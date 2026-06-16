package com.mailforge

data class SmtpConfig(
    val host: String,
    val port: Int=587,
    val username: String,
    val password: String,
    val useTls: Boolean=true,
    val useSSL: Boolean=false,
    val connectionTimeoutMs : Long = 15_000L
)