package com.mailforge

data class EmailResult(
    val success : Boolean,
    val error : String? = null
)