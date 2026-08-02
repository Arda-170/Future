// supabase/functions/checkin-respond/index.ts
//
// Kullanıcı check-in bildirimine cevap verdiğinde çağrılır.
// "iyiyim"      -> eskalasyon kapanır, puan verilir
// "zorlanıyorum"-> destek kaynakları sunulur, eskalasyon açık kalır
//
// Deploy:  supabase functions deploy checkin-respond

import { createClient } from "jsr:@supabase/supabase-js@2";

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
  if (req.method !== "POST") return fail("METHOD_NOT_ALLOWED", "Yalnızca POST", 405);

  // --- Kimlik --------------------------------------------------------
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return fail("UNAUTHORIZED", "Authorization başlığı yok", 401);

  const userClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const { data: { user }, error: authError } = await userClient.auth.getUser();
  if (authError || !user) return fail("UNAUTHORIZED", "Geçersiz oturum", 401);

  // --- Girdi ---------------------------------------------------------
  let body: { escalation_id?: string; response?: string };
  try {
    body = await req.json();
  } catch {
    return fail("INVALID_JSON", "Gövde okunamadı");
  }

  const escalationId = body.escalation_id?.trim();
  const answer = body.response?.trim();

  if (!escalationId) return fail("MISSING_ID", "escalation_id gerekli");
  if (!["ok", "struggling"].includes(answer ?? "")) {
    return fail("INVALID_RESPONSE", "response 'ok' veya 'struggling' olmalı");
  }

  // --- Eskalasyonu doğrula -------------------------------------------
  // Sahiplik kontrolü şart: başkasının eskalasyonunu kapatamamalı.
  const { data: esc, error: escError } = await admin
    .from("escalation_events")
    .select("id, user_id, state")
    .eq("id", escalationId)
    .eq("user_id", user.id)
    .single();

  if (escError || !esc) {
    return fail("NOT_FOUND", "Eskalasyon kaydı bulunamadı", 404);
  }

  // Zaten kapanmış bir eskalasyona tekrar cevap verilemez.
  if (["resolved", "expired"].includes(esc.state)) {
    return fail("ALREADY_CLOSED", "Bu kayıt kapanmış", 409);
  }

  const now = new Date().toISOString();

  // ===================================================================
  // "İyiyim"
  // ===================================================================
  if (answer === "ok") {
    await admin
      .from("escalation_events")
      .update({ state: "resolved", responded_at: now })
      .eq("id", escalationId);

    // Cevap vermek ödüllendirilir — sistemle etkileşimi teşvik eder.
    const { data: awarded } = await admin.rpc("award_points", {
      p_user: user.id,
      p_delta: 10,
      p_reason: "daily_checkin",
      p_ref_id: escalationId,
    });

    await admin.from("audit_log").insert({
      actor_id: user.id,
      action: "checkin.respond",
      target: "escalation_events",
      target_id: escalationId,
      meta: { response: "ok" },
    });

    return json({
      state: "resolved",
      points_awarded: awarded ?? 0,
      message: "İyi olduğunu duymak güzel.",
    });
  }

  // ===================================================================
  // "Zorlanıyorum"
  //
  // Eskalasyon KAPANMAZ — support_offered durumunda kalır.
  // Kullanıcı destek aldıktan sonra ayrıca kapatılır.
  // ===================================================================
  await admin
    .from("escalation_events")
    .update({ state: "support_offered", responded_at: now })
    .eq("id", escalationId);

  // Doğrulanmış acil kişiyi getir — doğrulanmamışa yönlendirme yapılmaz.
  const { data: contact } = await admin
    .from("emergency_contacts")
    .select("name, phone")
    .eq("user_id", user.id)
    .not("verified_at", "is", null)
    .order("priority", { ascending: true })
    .limit(1)
    .maybeSingle();

  const resources: Array<Record<string, string>> = [
    {
      type: "hotline",
      label: "Yeşilay Danışmanlık Merkezi — ALO 115",
      value: "115",
      note: "7/24, ücretsiz ve gizli",
    },
  ];

  if (contact) {
    resources.push({
      type: "contact",
      label: `${contact.name} — acil kişin`,
      value: contact.phone,
    });
  }

  // Destek istemek de ödüllendirilir. Yardım istemeyi teşvik etmek,
  // bağımlılık müdahalesinde kanıtlanmış bir yaklaşımdır.
  await admin.rpc("award_points", {
    p_user: user.id,
    p_delta: 15,
    p_reason: "reached_out",
    p_ref_id: escalationId,
  });

  await admin.from("audit_log").insert({
    actor_id: user.id,
    action: "checkin.respond",
    target: "escalation_events",
    target_id: escalationId,
    meta: { response: "struggling" },
  });

  return json({
    state: "support_offered",
    resources,
    message: "Bunu söylemek cesaret ister. Yalnız değilsin.",
  });
});