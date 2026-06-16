package com.mailforge

actual fun createSmtpSocket(): SmtpSocket = AndroidSmtpSocket()