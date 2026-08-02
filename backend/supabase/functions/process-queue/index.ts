// supabase/functions/process-queue/index.ts
//
// Bildirim kuyruğunu boşaltır. pg_cron her dakika çağırır.
//
// Kuyruk yapısının sebebi: bildirim üretimi (SQL trigger'ları) ile
// bildirim gönderimi (FCM/SMS) birbirinden ayrılmalı. Trigger içinden
// dış servise istek atılamaz; ayrıca gönderim başarısız olursa yeniden
// denenebilir olması gerekir.
//
// Deploy:  supabase functions deploy process-queue --no-verify-jwt

import { createClient } from "jsr:@supabase/supabase-js@2";
import { sendPushToUser } from "../_shared/fcm.ts";

const admin = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

const MAX_ATTEMPTS = 3;
const BATCH_SIZE = 50;

Deno.serve(async (req) => {
  // Bu fonksiyonu yalnızca cron çağırmalı. Paylaşılan bir sır ile korunur.
  const secret = req.headers.get("x-queue-secret");
  if (secret !== Deno.env.get("QUEUE_SECRET")) {
    return new Response("Forbidden", { status: 403 });
  }

  const { data: items, error } = await admin
    .from("notification_queue")
    .select("id, channel, recipient, payload, attempts")
    .eq("status", "pending")
    .lt("attempts", MAX_ATTEMPTS)
    .order("created_at", { ascending: true })
    .limit(BATCH_SIZE);

  if (error) {
    console.error("Kuyruk okunamadı:", error);
    return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  }

  if (!items?.length) {
    return new Response(JSON.stringify({ processed: 0 }), {
      headers: { "Content-Type": "application/json" },
    });
  }

  let sent = 0, failed = 0, skipped = 0;

  for (const item of items) {
    // -----------------------------------------------------------------
    // PUSH — recipient bir user_id
    // -----------------------------------------------------------------
    if (item.channel === "push") {
      const payload: Record<string, string> = {};
      for (const [k, v] of Object.entries(item.payload ?? {})) {
        payload[k] = String(v);   // FCM data alanı yalnızca string kabul eder
      }

      const result = await sendPushToUser(admin, item.recipient, payload);

      if (result.sent > 0) {
        await admin.from("notification_queue")
          .update({ status: "sent", sent_at: new Date().toISOString() })
          .eq("id", item.id);
        sent++;
      } else {
        // Cihaz yoksa tekrar denemenin anlamı yok — kalıcı olarak işaretle
        const noDevice = result.sent === 0 && result.failed === 0;
        await admin.from("notification_queue")
          .update({
            attempts: item.attempts + 1,
            status: noDevice ? "failed" : "pending",
            last_error: noDevice ? "Kayıtlı cihaz yok" : "Gönderim başarısız",
          })
          .eq("id", item.id);
        failed++;
      }
      continue;
    }

    // -----------------------------------------------------------------
    // SMS — sağlayıcı entegrasyonu pilot fazında
    //
    // MVP'de gerçek gönderim yapılmıyor. Kayıt 'skipped' olarak
    // işaretleniyor ki kuyrukta birikmesin ama izlenebilir kalsın.
    // Demoda bu kayıtlar "bu noktada SMS gider" diye gösterilir.
    // -----------------------------------------------------------------
    if (item.channel === "sms") {
      console.log(`SMS (simüle): ${item.recipient} -> ${item.payload?.text ?? ""}`);

      await admin.from("notification_queue")
        .update({
          status: "skipped",
          last_error: "SMS sağlayıcı entegrasyonu pilot fazında",
          sent_at: new Date().toISOString(),
        })
        .eq("id", item.id);
      skipped++;
      continue;
    }

    // Bilinmeyen kanal
    await admin.from("notification_queue")
      .update({ status: "failed", last_error: `Bilinmeyen kanal: ${item.channel}` })
      .eq("id", item.id);
    failed++;
  }

  return new Response(
    JSON.stringify({ processed: items.length, sent, failed, skipped }),
    { headers: { "Content-Type": "application/json" } },
  );
});