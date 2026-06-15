package com.mailforge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform