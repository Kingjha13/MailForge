package com.mailforge

actual fun createSmtpSocket(): SmtpSocket =
    throw NotImplementedError("Raw SMTP sockets are not available in browser JS")