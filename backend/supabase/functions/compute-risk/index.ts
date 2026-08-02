// supabase/functions/compute-risk/index.ts
//
// Günlük özeti üretir, risk skorunu hesaplar, gerekiyorsa eskalasyon başlatır.
// Normalde pg_cron ile otomatik çalışır; istemci "yenile" için de çağırabilir.
//
// Deploy:  supabase functions deploy compute-risk

import { createClient } from "jsr:@supabase/supabase-js@2";
import { sendPushToUser } from "../_shared/fcm.ts";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}

function fail(code: string, message: string, status = 400) {
  return json({ error: { code, message } }, status);
}

const admin = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") {
    return fail("METHOD_NOT_ALLOWED", "Yalnızca POST", 405);
  }

  // -------------------------------------------------------------------
  // Kimlik
  // -------------------------------------------------------------------
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return fail("UNAUTHORIZED", "Authorization başlığı yok", 401);

  const userClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const { data: { user }, error: authError } = await userClient.auth.getUser();
  if (authError || !user) return fail("UNAUTHORIZED", "Geçersiz oturum", 401);

  // -------------------------------------------------------------------
  // Rıza kontrolü — sağlık verisi işlemenin ön koşulu (KVKK md.6)
  // -------------------------------------------------------------------
  const { data: consentOk } = await admin.rpc("has_active_consent", {
    p_user: user.id,
    p_kind: "health_data",
  });

  if (!consentOk) {
    return fail("CONSENT_REQUIRED", "Sağlık verisi rızası bulunamadı", 403);
  }

  // -------------------------------------------------------------------
  // Gün belirleme
  // -------------------------------------------------------------------
  let day: string;
  try {
    const body = await req.json().catch(() => ({}));
    day = body?.day ?? new Date().toISOString().slice(0, 10);
  } catch {
    day = new Date().toISOString().slice(0, 10);
  }

  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) {
    return fail("INVALID_DAY", "day 'YYYY-MM-DD' formatında olmalı");
  }

  // -------------------------------------------------------------------
  // 1. Günlük özeti üret
  // -------------------------------------------------------------------
  const { error: aggError } = await admin.rpc("build_daily_aggregate", {
    p_user: user.id,
    p_day: day,
  });

  if (aggError) {
    console.error("build_daily_aggregate hatası:", aggError);
    return fail("AGGREGATION_FAILED", "Günlük özet üretilemedi", 500);
  }

  // -------------------------------------------------------------------
  // 2. Risk skorunu hesapla
  // -------------------------------------------------------------------
  const { data: result, error: riskError } = await admin.rpc("compute_risk_score", {
    p_user: user.id,
    p_day: day,
  });

  if (riskError) {
    console.error("compute_risk_score hatası:", riskError);
    return fail("SCORING_FAILED", "Risk skoru hesaplanamadı", 500);
  }

  // Yeterli veri yoksa dürüst ol — uydurma skor gösterme.
  if (result.status !== "ok") {
    return json({
      status: result.status,
      days_available: result.days_available ?? null,
      days_required: result.days_required ?? null,
      message: result.status === "insufficient_data"
        ? "Kişisel temel çizgi için yeterli veri henüz toplanmadı"
        : "Bu gün için değerlendirme yapılamadı",
    });
  }

  // -------------------------------------------------------------------
  // 3. Yüksek riskse eskalasyon başlat
  // -------------------------------------------------------------------
  let escalationId: string | null = null;

  if (result.level === "high") {
    const { data: scoreRow } = await admin
      .from("risk_scores")
      .select("id")
      .eq("user_id", user.id)
      .eq("for_day", day)
      .order("computed_at", { ascending: false })
      .limit(1)
      .single();

    const { data: escId } = await admin.rpc("maybe_start_escalation", {
      p_user: user.id,
      p_score_id: scoreRow?.id ?? null,
      p_source: "risk_score",
    });

    escalationId = escId;

    // null dönerse 24 saatlik soğuma süresi içindeyiz — bildirim gitmez.
    if (escalationId) {
      await sendCheckinPush(user.id, escalationId);
    }
  }

  await admin.from("audit_log").insert({
    actor_id: user.id,
    action: "risk.compute",
    target: "risk_scores",
    meta: { day, score: result.score, level: result.level },
  });

  return json({
    status: "ok",
    score: result.score,
    level: result.level,
    factors: result.factors,
    baseline_days: result.baseline_days,
    escalation_started: escalationId !== null,
  });
});


// ---------------------------------------------------------------------
// Check-in bildirimi gönder
//
// Dil önemli: suçlayıcı değil, meraklı. "Riskli davranış tespit edildi"
// değil, "nasılsın?". Kullanıcı zaten zorlanıyor olabilir.
// ---------------------------------------------------------------------
async function sendCheckinPush(userId: string, escalationId: string) {
  const { sent } = await sendPushToUser(admin, userId, {
    type: "checkin",
    escalation_id: escalationId,
    title: "Nasılsın?",
    body: "Son birkaç gündür verilerinde değişiklik var. Bir dakikan var mı?",
  });

  if (sent > 0) {
    await admin
      .from("escalation_events")
      .update({ state: "checkin_sent", checkin_sent_at: new Date().toISOString() })
      .eq("id", escalationId);
  }
}