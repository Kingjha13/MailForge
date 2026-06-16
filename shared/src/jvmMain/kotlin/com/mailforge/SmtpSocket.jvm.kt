package com.mailforge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

class JvmSmtpSocket : SmtpSocket {
    private lateinit var socket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: OutputStream

    override suspend fun connect(host: String, port: Int, useTls: Boolean) = withContext(Dispatchers.IO) {
        socket = if (useTls && port == 465) {
            SSLSocketFactory.getDefault().createSocket(host, port)
        } else {
            Socket().apply {
                connect(InetSocketAddress(host, port), 15_000)
            }
        }
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        writer = socket.getOutputStream()
    }

    override suspend fun startTls() = withContext(Dispatchers.IO) {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val sslSocket = factory.createSocket(
            socket, socket.inetAddress.hostAddress, socket.port, true
        )
        sslSocket.startHandshake()
        socket = sslSocket
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        writer = socket.getOutputStream()
    }

    override suspend fun writeLine(line: String) = withContext(Dispatchers.IO) {
        writer.write("$line\r\n".toByteArray(Charsets.UTF_8))
        writer.flush()
    }

    override suspend fun readLine(): String? = withContext(Dispatchers.IO) {
        reader.readLine()
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        runCatching { socket.close() }
    }
}

actual fun createSmtpSocket(): SmtpSocket = JvmSmtpSocket()