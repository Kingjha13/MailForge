package com.mailforge

actual fun createSmtpSocket(): SmtpSocket =
    throw NotImplementedError("SMTP socket not yet implemented for iOS")