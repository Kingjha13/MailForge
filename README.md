# MailForge

> A lightweight Kotlin Multiplatform library for sending emails directly over raw SMTP — no third-party email API, no SDK lock-in, no per-email pricing.


---

## Why MailForge

Most "send email from your app" solutions push you toward a paid API (SendGrid, Mailgun, Postmark, etc.). MailForge instead speaks raw SMTP directly to any mail server — Gmail, Outlook, your own domain's mail server, anything that accepts authenticated SMTP. You provide host, port, username, and password; MailForge handles the rest.

---

## Supported Platforms

| Platform | Status    | Notes |
|---|-----------|---|
| Android | ✅ Working | Uses `java.net.Socket` / `SSLSocketFactory` |
| JVM (Desktop) | ✅ Working | Same socket implementation as Android |
| Web (WasmJS / JS) | ❌ Not yet | Browsers have no raw TCP socket API — a server-side relay is needed |
| iOS | ❌ Not yet | Requires a Kotlin/Native socket + TLS implementation |

---

## How It  Works

MailForge opens a TCP connection to your SMTP server, performs `STARTTLS` to upgrade to an encrypted connection, authenticates with `AUTH LOGIN`, and sends a manually constructed MIME message — all without any third-party email library.

```
Your app → MailForge → TCP socket → STARTTLS → AUTH LOGIN → MAIL FROM / RCPT TO / DATA → smtp.gmail.com
```

---

## Getting Started

### 1. Add the dependency

Add JitPack to your repositories:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add MailForge:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.YOUR_GITHUB_USERNAME:mailforge:1.0.0")
}
```

### 2. Add the INTERNET permission (Android only)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

### 3. Get credentials for your email provider

If you are using Gmail, you cannot use your normal account password — Google requires an **App Password**.

1. Enable 2-Step Verification on your Google account.
2. Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
3. Generate a new App Password and copy the 16-character code.

For your own domain's mail server (cPanel, Postfix, Zoho Mail, etc.), use the host, port, and credentials your provider supplies instead.

### 4. Send an email

```kotlin
val config = SmtpConfig(
    host = "smtp.gmail.com",
    port = 587,
    username = "youraddress@gmail.com",
    password = "your16charapppassword",
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

// Call from a coroutine — send() is a suspend function
val result = client.send(email)

if (result.success) {
    println("Sent!")
} else {
    println("Failed: ${result.error}")
}
```

Call `send()` from a coroutine scope — it is a `suspend` function and performs all network I/O on `Dispatchers.IO`.

---

## Sending Limits (Gmail)

| Account type | Daily limit | Notes |
|---|---|---|
| Free Gmail | ~100–500 emails | Lower figure applies to automated SMTP |
| Google Workspace | ~2,000 emails | Higher cap for business accounts |
| Per message | Up to 100 recipients | To + Cc + Bcc combined |

**Recommended:** no more than ~20 emails per hour to avoid triggering abuse detection, even if you stay under the daily cap.

> For real-volume sending (newsletters, transactional email at scale), use a dedicated SMTP relay provider. Gmail's authenticated SMTP is not built for that use case.

---

## Project Structure

```
shared/
├── src/
│   ├── commonMain/kotlin/com/mailforge/
│   │   ├── Email.kt              — Email + EmailAttachment data classes
│   │   ├── EmailResult.kt        — success/error result type
│   │   ├── SmtpConfig.kt         — connection configuration
│   │   ├── SmtpSocket.kt         — expect interface for platform sockets
│   │   ├── SmtpConnection.kt     — SMTP protocol logic (EHLO / STARTTLS / AUTH / DATA)
│   │   ├── MimeBuilder.kt        — builds the raw MIME message
│   │   ├── Base64Util.kt         — base64 encoding for AUTH and attachments
│   │   └── EmailClient.kt        — public API: send() / sendBatch()
│   ├── jvmMain/kotlin/com/mailforge/
│   │   └── SmtpSocket.jvm.kt     — actual: java.net.Socket implementation
│   ├── androidMain/kotlin/com/mailforge/
│   │   ├── SmtpSocket.android.kt — actual: delegates to AndroidSmtpSocket
│   │   └── AndroidSmtpSocket.kt  — same socket logic as JVM (to be consolidated)
│   ├── iosMain/kotlin/com/mailforge/
│   │   └── SmtpSocket.ios.kt     — actual: placeholder (not yet implemented)
│   ├── jsMain/kotlin/com/mailforge/
│   │   └── SmtpSocket.js.kt      — actual: placeholder (not yet implemented)
│   └── wasmJsMain/kotlin/com/mailforge/
│       └── SmtpSocket.wasmJs.kt  — actual: placeholder (not yet implemented)
```

---

## Known Limitations

- No connection pooling or retry logic — every `send()` opens a fresh connection and closes it.
- No delivery tracking — raw SMTP only confirms the server accepted the message, not that it reached the inbox. True bounce tracking requires parsing DSN emails.
- No OAuth2 support — only `AUTH LOGIN` (username + App Password) for now.
- No email templates or scheduled sending yet.
- iOS socket implementation is a stub only.
- Web (WasmJS/JS) cannot use raw TCP sockets — this is a browser platform restriction, not a missing library feature.

---

## Roadmap

- **iOS support** — real TLS-capable socket via a Kotlin/Native bridge to `Network.framework`
- **Web support** — a small Ktor relay server exposing a `POST /send-email` endpoint for the JS/WASM targets
- **Shared Android/JVM source set** — eliminate the duplicated socket code between `androidMain` and `jvmMain`
- **Email templates** — `{{variable}}` placeholder substitution
- **Retry logic** — automatic backoff for transient SMTP errors (e.g. `421`)
- **OAuth2 authentication** — for Workspace and Outlook accounts that disable basic auth

---

## License

MIT License — see [LICENSE](LICENSE) for details.