package com.mailforge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Base64

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Text("Testing SMTP...")
        }

        lifecycleScope.launch {
            try {
                val socket = createSmtpSocket()

                socket.connect(host = "smtp.gmail.com", port = 587, useTls = false)
                Log.d("SMTP_TEST", "Banner: ${socket.readLine()}")

                socket.writeLine("EHLO localhost")
                while (true) {
                    val line = socket.readLine() ?: break
                    Log.d("SMTP_TEST", "EHLO: $line")
                    if (line.length > 3 && line[3] == ' ') break
                }

                socket.writeLine("STARTTLS")
                Log.d("SMTP_TEST", "STARTTLS: ${socket.readLine()}")
                socket.startTls()

                socket.writeLine("EHLO localhost")
                while (true) {
                    val line = socket.readLine() ?: break
                    Log.d("SMTP_TEST", "EHLO2: $line")
                    if (line.length > 3 && line[3] == ' ') break
                }

                val myEmail = "example@gmail.com"       // <-- your real Gmail address
                val appPassword = ""   // <-- the App Password, no spaces

                socket.writeLine("AUTH LOGIN")
                Log.d("SMTP_TEST", "AUTH: ${socket.readLine()}")

                socket.writeLine(Base64.getEncoder().encodeToString(myEmail.toByteArray()))
                Log.d("SMTP_TEST", "USER: ${socket.readLine()}")

                socket.writeLine(Base64.getEncoder().encodeToString(appPassword.toByteArray()))
                val authResult = socket.readLine()
                Log.d("SMTP_TEST", "PASS: $authResult")

                if (authResult?.startsWith("235") != true) {
                    Log.e("SMTP_TEST", "Auth failed, stopping here")
                    socket.close()
                    return@launch
                }

                socket.writeLine("MAIL FROM:<$myEmail>")
                Log.d("SMTP_TEST", "MAIL FROM: ${socket.readLine()}")

                val toAddress = "example2@gmail.com" // send to yourself for the test
                socket.writeLine("RCPT TO:<$toAddress>")
                Log.d("SMTP_TEST", "RCPT TO: ${socket.readLine()}")

                socket.writeLine("DATA")
                Log.d("SMTP_TEST", "DATA: ${socket.readLine()}")

                val message = buildString {
                    append("From: $myEmail\r\n")
                    append("To: $toAddress\r\n")
                    append("Subject: MailForge test\r\n")
                    append("\r\n")
                    append("Hello from my own KMP SMTP library!\r\n")
                }
                socket.writeLine(message)
                socket.writeLine(".")
                Log.d("SMTP_TEST", "SEND: ${socket.readLine()}")

                socket.writeLine("QUIT")
                socket.close()

                Log.d("SMTP_TEST", "Done — check your inbox!")

            } catch (e: Exception) {
                Log.e("SMTP_TEST", "Failed", e)
            }
        }
    }
}