// supabase/functions/simulate-risk/index.ts
//
// DEMO ENDPOINT — sahne sigortası.
//
// Canlı demoda saat bağlanmazsa veya veri akmazsa sunum çöker.
// Bu fonksiyon yapay bir risk skoru üretip eskalasyon zincirini
// tetikler, böylece akış her koşulda gösterilebilir.
//
// ÜRETİM ORTAMINDA KAPATILACAK:
//   supabase secrets set DEMO_MODE=false
//
// Deploy:  supabase functions deploy simulate-risk

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

// Demo seviyelerine karşılık gelen gerçekçi faktör tabloları.
// Gerçek algoritmanın ürettiğiyle aynı yapıda — sunumda fark edilmez,
// ama veritabanında model_version 'demo-sim' olarak işaretlenir.
const PRESETS: Record<string, { score: number; level: string; factors: unknown }> = {
  moderate: {
    score: 52,
    level: "moderate",
    factors: {
      sleep_drop: {
        z: 1.6, weight: 0.30, value: 402, baseline: 428.5,
        label: "Uyku süresi normalinin altında",
      },
      hrv_drop: {
        z: 1.5, weight: 0.25, value: 52.1, baseline: 55.3,
        label: "Kalp atım değişkenliği düşmüş",
      },
      night_screen: {
        z: 1.4, weight: 0.25, value: 17, baseline: 11.7,
        label: "Gece ekran süresi artmış",
      },
    },
  },
  high: {
    score: 84,
    level: "high",
    factors: {
      sleep_drop: {
        z: 2.9, weight: 0.30, value: 293, baseline: 428.5,
        label: "Uyku süresi normalinin altında",
      },
      hrv_drop: {
        z: 2.7, weight: 0.25, value: 39.8, baseline: 55.3,
        label: "Kalp atım değişkenliği düşmüş",
      },
      rhr_rise: {
        z: 2.4, weight: 0.20, value: 71.5, baseline: 62.5,
        label: "Dinlenme nabzı yükselmiş",
      },
      night_screen: {
        z: 2.8, weight: 0.25, value: 74, baseline: 11.7,
        label: "Gece ekran süresi artmış",
      },
    },
  },
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return fail("METHOD_NOT_ALLOWED", "Yalnızca POST", 405);

  // --- Özellik bayrağı -----------------------------------------------
  // Varsayılan KAPALI. Açıkça açılmadıysa çalışmaz.
  if (Deno.env.get("DEMO_MODE") !== "true") {
    return fail("DEMO_DISABLED", "Demo modu kapalı", 403);
  }

  // --- Kimlik ---------------------------------------------------------
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return fail("UNAUTHORIZED", "Authorization başlığı yok", 401);

  const userClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const { data: { user }, error: authError } = await userClient.auth.getUser();
  if (authError || !user) return fail("UNAUTHORIZED", "Geçersiz oturum", 401);

  // --- Girdi ----------------------------------------------------------
  const body = await req.json().catch(() => ({}));
  const level = (body?.level ?? "high") as string;

  const preset = PRESETS[level];
  if (!preset) return fail("INVALID_LEVEL", "level 'moderate' veya 'high' olmalı");

  // --- Skoru yaz ------------------------------------------------------
  const { data: scoreRow, error: insError } = await admin
    .from("risk_scores")
    .insert({
      user_id: user.id,
      for_day: new Date().toISOString().slice(0, 10),
      score: preset.score,
      level: preset.level,
      factors: preset.factors,
      model_version: "demo-sim",   // gerçek skorlardan ayırt edilebilir
    })
    .select("id")
    .single();

  if (insError) {
    console.error("simulate insert hatası:", insError);
    return fail("DB_ERROR", "Skor yazılamadı", 500);
  }

  // --- Eskalasyon -----------------------------------------------------
  let escalationId: string | null = null;

  if (preset.level === "high") {
    // Demo'da soğuma süresini atlamak gerekebilir (arka arkaya prova).
    if (body?.bypass_cooldown === true) {
      const { data } = await admin
        .from("escalation_events")
        .insert({
          user_id: user.id,
          risk_score_id: scoreRow.id,
          trigger_source: "demo",
          state: "pending",
          timeout_at: new Date(Date.now() + 15 * 60_000).toISOString(),
        })
        .select("id")
        .single();
      escalationId = data?.id ?? null;
    } else {
      const { data } = await admin.rpc("maybe_start_escalation", {
        p_user: user.id,
        p_score_id: scoreRow.id,
        p_source: "demo",
      });
      escalationId = data;
    }

    if (escalationId) {
      const { sent } = await sendPushToUser(admin, user.id, {
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
  }

  await admin.from("audit_log").insert({
    actor_id: user.id,
    action: "demo.simulate",
    target: "risk_scores",
    target_id: scoreRow.id,
    meta: { level, simulated: true },
  });

  return json({
    status: "ok",
    simulated: true,
    score: preset.score,
    level: preset.level,
    factors: preset.factors,
    escalation_started: escalationId !== null,
    escalation_id: escalationId,
  });
});