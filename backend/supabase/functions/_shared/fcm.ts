// supabase/functions/_shared/fcm.ts
//
// FCM HTTP v1 istemcisi.
//
// NOT: Eski "server key" yöntemi (fcm.googleapis.com/fcm/send) Google
// tarafından 22 Temmuz 2024'te kapatıldı. Artık servis hesabıyla
// OAuth2 erişim token'ı üretmek zorunlu.
//
// Akış:
//   1. Servis hesabının private key'iyle imzalı bir JWT üret (RS256)
//   2. JWT'yi Google'ın token endpoint'inde erişim token'ıyla değiştir
//   3. Erişim token'ıyla FCM v1 endpoint'ine mesaj gönder
//
// Erişim token'ı 1 saat geçerli; bellekte önbelleğe alınıyor.

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

let cachedToken: { value: string; expiresAt: number } | null = null;

// ---------------------------------------------------------------------
// Base64URL kodlama (JWT standardı — normal base64'ten farklı)
// ---------------------------------------------------------------------
function base64url(input: string | Uint8Array): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

// ---------------------------------------------------------------------
// PEM formatındaki private key'i Web Crypto'nun anlayacağı hale getir
// ---------------------------------------------------------------------
async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const clean = pem
    .replace(/\\n/g, "\n")                   // ortam değişkeninden gelen kaçışlar
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s/g, "");

  const der = Uint8Array.from(atob(clean), (c) => c.charCodeAt(0));

  return await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

// ---------------------------------------------------------------------
// Erişim token'ı al (önbellekli)
// ---------------------------------------------------------------------
async function getAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);

  // 60 sn güvenlik payı
  if (cachedToken && cachedToken.expiresAt > now + 60) {
    return cachedToken.value;
  }

  const clientEmail = Deno.env.get("FCM_CLIENT_EMAIL");
  const privateKey = Deno.env.get("FCM_PRIVATE_KEY");

  if (!clientEmail || !privateKey) {
    throw new Error("FCM_CLIENT_EMAIL veya FCM_PRIVATE_KEY tanımlı değil");
  }

  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64url(JSON.stringify({
    iss: clientEmail,
    scope: SCOPE,
    aud: TOKEN_URL,
    iat: now,
    exp: now + 3600,
  }));

  const key = await importPrivateKey(privateKey);
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(`${header}.${claims}`),
  );

  const jwt = `${header}.${claims}.${base64url(new Uint8Array(signature))}`;

  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  if (!res.ok) {
    throw new Error(`Token alınamadı: ${res.status} ${await res.text()}`);
  }

  const data = await res.json();
  cachedToken = {
    value: data.access_token,
    expiresAt: now + (data.expires_in ?? 3600),
  };

  return cachedToken.value;
}

// ---------------------------------------------------------------------
// Tek bir cihaza bildirim gönder
//
// Dönüş: { ok: true } | { ok: false, invalidToken: boolean, error: string }
// invalidToken true ise çağıran taraf token'ı veritabanından silmeli.
// ---------------------------------------------------------------------
export async function sendPush(
  fcmToken: string,
  payload: Record<string, string>,
): Promise<{ ok: boolean; invalidToken?: boolean; error?: string }> {
  const projectId = Deno.env.get("FCM_PROJECT_ID");
  if (!projectId) return { ok: false, error: "FCM_PROJECT_ID tanımlı değil" };

  let accessToken: string;
  try {
    accessToken = await getAccessToken();
  } catch (e) {
    return { ok: false, error: String(e) };
  }

  // data-only mesaj gönderiyoruz (notification bloğu yok).
  // Sebebi: uygulama arka plandayken bile bildirimin GÖRÜNÜMÜNÜ ve
  // davranışını mobil taraf kontrol etsin. Kriz anı arayüzü için önemli.
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: fcmToken,
          data: payload,
          android: {
            priority: "high",   // kriz bildirimi Doze modunu aşmalı
          },
        },
      }),
    },
  );

  if (res.ok) return { ok: true };

  const errText = await res.text();

  // 404 UNREGISTERED / 400 INVALID_ARGUMENT → token artık geçersiz
  const invalidToken = res.status === 404 ||
    errText.includes("UNREGISTERED") ||
    errText.includes("INVALID_ARGUMENT");

  return { ok: false, invalidToken, error: `${res.status} ${errText}` };
}

// ---------------------------------------------------------------------
// Bir kullanıcının tüm cihazlarına gönder, ölü token'ları temizle
// ---------------------------------------------------------------------
export async function sendPushToUser(
  admin: any,
  userId: string,
  payload: Record<string, string>,
): Promise<{ sent: number; failed: number }> {
  const { data: devices } = await admin
    .from("device_tokens")
    .select("id, fcm_token")
    .eq("user_id", userId);

  if (!devices?.length) {
    console.warn("Kayıtlı cihaz yok:", userId);
    return { sent: 0, failed: 0 };
  }

  let sent = 0, failed = 0;
  const deadTokens: string[] = [];

  for (const d of devices) {
    const result = await sendPush(d.fcm_token, payload);
    if (result.ok) {
      sent++;
    } else {
      failed++;
      console.error("FCM hatası:", result.error);
      if (result.invalidToken) deadTokens.push(d.id);
    }
  }

  // Ölü token'ları temizle — birikirse her gönderimde boşuna beklenir
  if (deadTokens.length) {
    await admin.from("device_tokens").delete().in("id", deadTokens);
  }

  return { sent, failed };
}
