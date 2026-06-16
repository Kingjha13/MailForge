package com.mailforge

interface SmtpSocket {
    suspend fun connect(host: String, port: Int, useTls: Boolean)
    suspend fun startTls()
    suspend fun writeLine(line: String)
    suspend fun readLine(): String?
    suspend fun close()
}

expect fun createSmtpSocket(): SmtpSocket
