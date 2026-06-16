# MailForge

A lightweight Kotlin Multiplatform library for sending emails directly over SMTP using your own email account or domain — no third-party email API, no SDK lock-in, no per-email pricing.

> **Status:** Android and JVM (Desktop) are working. Web (WasmJS/JS) and iOS are not yet implemented — see [Roadmap](#roadmap).

## Why MailForge

Most "send email from your app" solutions push you toward a paid API (SendGrid, Mailgun, Postmark, etc.). MailForge instead speaks raw SMTP directly to any mail server — Gmail, Outlook, your own domain's mail server, anything that accepts authenticated SMTP. You provide host/port/username/password; MailForge handles the protocol.

## Supported Platforms

| Platform | Status | Notes |
|---|---|---|
| Android | ✅ Working | Uses `java.net.Socket` / `SSLSocketFactory` |
| JVM (Desktop) | ✅ Working | Same socket implementation as Android |
| Web (WasmJS / JS) | ❌ Not implemented | Browsers have no raw TCP socket API — this is a browser security restriction, not a missing feature. A server-side relay will be needed; see Roadmap. |
| iOS | ❌ Not implemented | Requires a Kotlin/Native socket + TLS implementation (Network.framework bridge) |

## How It Works

MailForge opens a TCP connection to your SMTP server, performs `STARTTLS` to upgrade to an encrypted connection, authenticates with `AUTH LOGIN`, and sends a manually-constructed MIME message — all without any third-party email library.

```
Your app → MailForge → TCP socket → STARTTLS → AUTH LOGIN → MAIL FROM / RCPT TO / DATA → smtp.gmail.com (or any SMTP server)
```

## Getting Started

### 1. Add the INTERNET permission (Android)

In your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 2. Get credentials for your email provider

If you're using Gmail specifically, you cannot use your normal account password — Google requires an **App Password**.

1. Enable 2-Step Verification on your Google account (required for App Passwords).
2. Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
3. Generate a new App Password and copy the 16-character code (the spaces shown are not part of the password).

If you're using your own domain's mail server (e.g. via cPanel, Postfix, Zoho Mail, etc.), use the host/port/credentials your provider gives you instead.

### 3. Configure and send

```kotlin
val config = SmtpConfig(
    host = "smtp.gmail.com",
    port = 587,
    username = "youraddress@gmail.com",
    password = "your16charapppassword", // not your real Gmail password
    useTls = true
)

val client = EmailClient(config)

val email = Email(
    from = "youraddress@gmail.com",
    to = listOf("recipient@example.com"),
    subject = "Hello from MailForge",
    body = "This email was sent using my own SMTP credentials.",
    isHtml = false
)

val result = client.send(email)
if (result.success) {
    println("Sent!")
} else {
    println("Failed: ${result.error}")
}
```

Call `send()` from a coroutine (e.g. `lifecycleScope.launch { }` on Android) — it's a `suspend` function and performs network I/O on `Dispatchers.IO`.

## Sending Limits (Gmail)

If you're testing or using a free personal Gmail account as your sender, be aware of Google's limits so you don't get temporarily blocked:

- **Free Gmail account:** capped at roughly 100–500 emails per rolling 24-hour day, depending on sending method (the lower figure applies specifically to automated SMTP sending; general account caps run higher).
- **Per message:** up to 100 recipients total (To + Cc + Bcc combined).
- **Recommended rate:** no more than ~20 emails per hour, even though there's no hard per-hour limit — sending in quick bursts can trigger Google's abuse detection regardless of staying under the daily cap.
- **Google Workspace accounts:** higher cap, around 2,000 per day.
- Exceeding limits can result in a temporary block (commonly 24 hours) on outgoing mail from that account.

For testing MailForge itself, sending a handful of emails to yourself is more than enough — there's no need to send in bulk to verify the library works. If you eventually need to send at real volume (newsletters, transactional email at scale), use a dedicated SMTP relay provider instead of a personal Gmail account; Gmail's authenticated SMTP was not built for that use case.

## Project Structure

```
shared/
├── src/
│   ├── commonMain/kotlin/com/mailforge/
│   │   ├── Email.kt              — Email + EmailAttachment data classes
│   │   ├── EmailResult.kt        — success/error result type
│   │   ├── SmtpConfig.kt         — connection configuration
│   │   ├── SmtpSocket.kt         — expect interface for platform sockets
│   │   ├── SmtpConnection.kt     — SMTP protocol logic (EHLO/STARTTLS/AUTH/DATA)
│   │   ├── MimeBuilder.kt        — builds the raw MIME message
│   │   ├── Base64Util.kt         — base64 encoding for AUTH and attachments
│   │   └── EmailClient.kt        — public API: send() / sendBatch()
│   ├── jvmMain/kotlin/com/mailforge/
│   │   └── SmtpSocket.jvm.kt     — actual: java.net.Socket implementation
│   ├── androidMain/kotlin/com/mailforge/
│   │   ├── SmtpSocket.android.kt — actual: delegates to AndroidSmtpSocket
│   │   └── AndroidSmtpSocket.kt  — same socket logic as JVM (duplicated for now)
│   ├── iosMain/kotlin/com/mailforge/
│   │   └── SmtpSocket.ios.kt     — actual: throws NotImplementedError (placeholder)
│   ├── jsMain/kotlin/com/mailforge/
│   │   └── SmtpSocket.js.kt      — actual: throws NotImplementedError (placeholder)
│   └── wasmJsMain/kotlin/com/mailforge/
│       └── SmtpSocket.wasmJs.kt  — actual: throws NotImplementedError (placeholder)
```

> Note: `AndroidSmtpSocket` and the JVM socket implementation currently contain duplicated code, since `androidMain` and `jvmMain` are sibling source sets that don't share code by default. This will be consolidated into a shared `jvmAndAndroidMain` intermediate source set — see Roadmap.

## Known Limitations (current state)

- No connection pooling or retry logic — every `send()` opens a fresh connection and closes it.
- No delivery tracking. Raw SMTP only confirms the message was *accepted by the server*, not that it was delivered to the recipient's inbox. True delivery/bounce tracking requires parsing DSN (Delivery Status Notification) bounce emails, which isn't implemented.
- No OAuth2 support — only username/password (App Password) authentication via `AUTH LOGIN`.
- Email templates, scheduled sending, and batch optimization are not yet implemented.
- iOS has no working socket implementation yet (stub only).
- Web (WasmJS/JS) cannot use raw sockets — this is a browser platform limitation, not something fixable in this library. Web support will require a server-side HTTP relay.

## Roadmap

1. **iOS support** — implement a real TLS-capable socket, likely via a Kotlin/Native bridge to `Network.framework` (pure POSIX sockets can't do TLS without significant extra work).
2. **Web support** — build a small backend relay (e.g. Ktor server) that reuses the existing JVM SMTP code, exposing an HTTP endpoint (`POST /send-email`) that the web client calls instead of opening a socket directly.
3. **Shared Android/JVM source set** — eliminate the duplicated socket code between `androidMain` and `jvmMain`.
4. **Email templates** — placeholder-based template rendering (`{{variable}}` substitution).
5. **Retry logic** — automatic retry with backoff for transient SMTP errors (e.g. `421` temporary failures).
6. **OAuth2 authentication** — needed for Workspace/Outlook accounts that disable basic auth.

## License

Add your chosen license here (e.g. Apache 2.0, MIT) before publishing.