// supabase/functions/register-device/index.ts
//
// FCM cihaz token'ı kaydeder / günceller.
// Uygulama her açılışta ve token yenilendiğinde çağırır.
//
// Deploy:  supabase functions deploy register-device

import { createClient } from "jsr:@supabase/supabase-js@2";

// Ortak CORS başlıkları — mobil için şart değil ama web test arayüzü
// ve tarayıcıdan deneme yaparken gerekiyor.
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

Deno.serve(async (req) => {
  // Tarayıcı preflight isteği
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: cors });
  }

  if (req.method !== "POST") {
    return fail("METHOD_NOT_ALLOWED", "Yalnızca POST kabul edilir", 405);
  }

  // -------------------------------------------------------------------
  // 1. Kimliği doğrula
  //    Kullanıcının JWT'siyle çalışan geçici bir istemci kuruyoruz.
  //    Bu istemci RLS'e TABİDİR — sadece kim olduğunu öğrenmek için.
  // -------------------------------------------------------------------
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return fail("UNAUTHORIZED", "Authorization başlığı yok", 401);
  }

  const userClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const { data: { user }, error: authError } = await userClient.auth.getUser();

  if (authError || !user) {
    return fail("UNAUTHORIZED", "Geçersiz oturum", 401);
  }

  // -------------------------------------------------------------------
  // 2. Girdiyi doğrula
  //    İstemciden gelen HİÇBİR şeye güvenilmez. user_id'yi istemciden
  //    almıyoruz — JWT'den okuduğumuz user.id'yi kullanıyoruz.
  // -------------------------------------------------------------------
  let body: { fcm_token?: string; platform?: string };
  try {
    body = await req.json();
  } catch {
    return fail("INVALID_JSON", "Gövde okunamadı");
  }

  const fcmToken = body.fcm_token?.trim();
  const platform = body.platform?.trim() ?? "android";

  if (!fcmToken || fcmToken.length < 20) {
    return fail("INVALID_TOKEN", "fcm_token geçersiz");
  }

  if (!["android", "ios"].includes(platform)) {
    return fail("INVALID_PLATFORM", "platform 'android' veya 'ios' olmalı");
  }

  // -------------------------------------------------------------------
  // 3. Yaz
  //    Burada service_role kullanıyoruz — RLS'i bypass eder.
  //    Bu anahtar ASLA istemciye gitmez, sadece sunucuda yaşar.
  // -------------------------------------------------------------------
  const admin = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  // fcm_token unique. Aynı telefon başka bir hesaba giriş yaptıysa
  // kayıt yeni kullanıcıya devredilir — eski hesaba bildirim gitmez.
  const { data, error } = await admin
    .from("device_tokens")
    .upsert(
      {
        user_id: user.id,
        fcm_token: fcmToken,
        platform,
        last_seen_at: new Date().toISOString(),
      },
      { onConflict: "fcm_token" },
    )
    .select("id")
    .single();

  if (error) {
    console.error("device_tokens upsert hatası:", error);
    return fail("DB_ERROR", "Cihaz kaydedilemedi", 500);
  }

  // Denetim kaydı — KVKK "kim hangi veriye erişti" gereksinimi
  await admin.from("audit_log").insert({
    actor_id: user.id,
    action: "device.register",
    target: "device_tokens",
    target_id: data.id,
    meta: { platform },
  });

  return json({ ok: true });
});