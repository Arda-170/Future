// supabase/functions/verify-contact/index.ts
//
// Acil kişi doğrulama.
//
// NEDEN GEREKLİ: Yanlış girilmiş bir numaraya kriz bildirimi gitmesi
// hem mahremiyet ihlali hem de tanımadığı birini gereksiz kaygıya
// sokmak demektir. Doğrulanmamış kişiye bildirim GÖNDERİLMEZ.
//
// Akış:
//   POST { contact_id }        -> kod üretir, kuyruğa SMS düşer
//   POST { contact_id, code }  -> kodu doğrular, verified_at doldurur
//
// Deploy:  supabase functions deploy verify-contact

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

function generateCode(): string {
  // 6 haneli, kriptografik olarak güvenli
  const arr = new Uint32Array(1);
  crypto.getRandomValues(arr);
  return String(arr[0] % 1_000_000).padStart(6, "0");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return fail("METHOD_NOT_ALLOWED", "Yalnızca POST", 405);

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
  const contactId = body?.contact_id?.trim();
  const code = body?.code?.trim();

  if (!contactId) return fail("MISSING_ID", "contact_id gerekli");

  // Sahiplik kontrolü: başkasının acil kişisi doğrulanamaz
  const { data: contact, error: cErr } = await admin
    .from("emergency_contacts")
    .select("id, name, phone, verified_at")
    .eq("id", contactId)
    .eq("user_id", user.id)
    .single();

  if (cErr || !contact) return fail("NOT_FOUND", "Acil kişi bulunamadı", 404);

  // ===================================================================
  // AŞAMA 2 — kod doğrulama
  // ===================================================================
  if (code) {
    const { data: challenge } = await admin
      .from("contact_verifications")
      .select("id, code, attempts, expires_at")
      .eq("contact_id", contactId)
      .is("used_at", null)
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();

    if (!challenge) return fail("NO_CHALLENGE", "Önce doğrulama kodu isteyin", 409);

    if (new Date(challenge.expires_at) < new Date()) {
      return fail("CODE_EXPIRED", "Kodun süresi doldu, yenisini isteyin", 410);
    }

    // Kaba kuvvet koruması: 5 deneme
    if (challenge.attempts >= 5) {
      return fail("TOO_MANY_ATTEMPTS", "Çok fazla hatalı deneme", 429);
    }

    if (challenge.code !== code) {
      await admin
        .from("contact_verifications")
        .update({ attempts: challenge.attempts + 1 })
        .eq("id", challenge.id);
      return fail("INVALID_CODE", "Kod hatalı");
    }

    const now = new Date().toISOString();

    await admin
      .from("contact_verifications")
      .update({ used_at: now })
      .eq("id", challenge.id);

    await admin
      .from("emergency_contacts")
      .update({ verified_at: now })
      .eq("id", contactId);

    await admin.from("audit_log").insert({
      actor_id: user.id,
      action: "contact.verified",
      target: "emergency_contacts",
      target_id: contactId,
    });

    return json({
      status: "verified",
      contact_name: contact.name,
      message: `${contact.name} artık acil kişin olarak tanımlı.`,
    });
  }

  // ===================================================================
  // AŞAMA 1 — kod gönderimi
  // ===================================================================
  if (contact.verified_at) {
    return json({ status: "already_verified", contact_name: contact.name });
  }

  const newCode = generateCode();
  const expiresAt = new Date(Date.now() + 15 * 60_000).toISOString();

  await admin.from("contact_verifications").insert({
    contact_id: contactId,
    code: newCode,
    expires_at: expiresAt,
  });

  // SMS kuyruğuna düş. Mesajda kullanıcının durumu hakkında HİÇBİR
  // bilgi yok — sadece onay isteniyor. Acil kişi henüz neye onay
  // verdiğini uygulamadan değil, kullanıcının kendisinden öğrenmeli.
  await admin.from("notification_queue").insert({
    escalation_id: null,
    channel: "sms",
    recipient: contact.phone,
    payload: {
      template: "contact_verification",
      text: `Bir yakınınız sizi destek kişisi olarak eklemek istiyor. ` +
            `Onay kodu: ${newCode}`,
    },
  });

  return json({
    status: "code_sent",
    expires_at: expiresAt,
    // SMS sağlayıcı entegrasyonu pilot fazında. MVP'de kod yanıtta
    // dönüyor ki demo ve test yapılabilsin.
    demo_code: Deno.env.get("DEMO_MODE") === "true" ? newCode : undefined,
  });
});