// supabase/functions/delete-my-data/index.ts
//
// KVKK md. 7 — kişisel verilerin silinmesi hakkı.
//
// Kullanıcının tüm verisini ve hesabını siler. Geri alınamaz.
// Ayarlar ekranında bu düğme MUTLAKA bulunmalı; jüri raporda arayacak.
//
// Deploy:  supabase functions deploy delete-my-data

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

  // --- Kasıtlı onay ---------------------------------------------------
  // Yanlışlıkla tetiklenmeyi engellemek için açık bir onay metni isteniyor.
  // Mobil tarafta kullanıcı bu kelimeyi yazarak onaylar.
  const body = await req.json().catch(() => ({}));
  if (body?.confirm !== "SIL") {
    return fail(
      "CONFIRMATION_REQUIRED",
      "Silme işlemi için confirm alanı 'SIL' olmalı",
      428,
    );
  }

  // --- Silme öncesi sayım ---------------------------------------------
  // Kullanıcıya neyin silindiğini raporlamak için (şeffaflık).
  const counts: Record<string, number> = {};
  const tables = [
    "health_samples", "daily_aggregates", "risk_scores",
    "geofence_zones", "geofence_events", "points_ledger",
    "user_badges", "emergency_contacts", "escalation_events",
    "device_tokens", "consents",
  ];

  for (const t of tables) {
    const { count } = await admin
      .from(t)
      .select("*", { count: "exact", head: true })
      .eq("user_id", user.id);
    counts[t] = count ?? 0;
  }

  // --- Silme ----------------------------------------------------------
  // profiles.id ve tüm user_id alanları auth.users'a
  // ON DELETE CASCADE ile bağlı. Auth kaydını silmek her şeyi götürür.
  const { error: delError } = await admin.auth.admin.deleteUser(user.id);

  if (delError) {
    console.error("Kullanıcı silinemedi:", delError);
    return fail("DELETE_FAILED", "Silme işlemi tamamlanamadı", 500);
  }

  // --- Anonim denetim kaydı -------------------------------------------
  // actor_id NULL: silinen kullanıcıyı işaret eden bir bağ bırakılmaz.
  // Yalnızca "bir silme talebi karşılandı" bilgisi tutulur — bu bilgi
  // KVKK yükümlülüğünün yerine getirildiğini kanıtlamak için gerekli.
  await admin.from("audit_log").insert({
    actor_id: null,
    action: "account.deleted",
    target: "auth.users",
    target_id: null,
    meta: { records_removed: counts },
  });

  return json({
    status: "deleted",
    records_removed: counts,
    message: "Tüm verilerin silindi. Bu işlem geri alınamaz.",
  });
});