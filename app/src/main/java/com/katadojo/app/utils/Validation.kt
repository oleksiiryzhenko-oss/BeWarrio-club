package com.katadojo.app.utils

object Validation {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val NAME = Regex("^[\\p{L} .'-]+$")
    private val PHONE_CHARS = Regex("^[+0-9 ()-]+$")

    fun fullName(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Full name is required"
            v.length < 3 -> "Name is too short"
            !NAME.matches(v) -> "Use letters, spaces, apostrophes or hyphens"
            v.split(' ').count { it.isNotBlank() } < 2 -> "Enter your first and last name"
            v.length > 60 -> "Name is too long"
            else -> null
        }
    }

    fun email(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Email is required"
            !EMAIL.matches(v) -> "Enter a valid email address"
            else -> null
        }
    }

    fun phone(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return "Phone number is required"
        if (!PHONE_CHARS.matches(v)) return "Use digits, spaces, +, ( ) and - only"
        val digits = v.count { it.isDigit() }
        return if (digits !in 7..15) "Enter 7 to 15 digits" else null
    }

    fun comment(value: String): String? = if (value.length > 300) "Keep your comment under 300 characters" else null

    fun displayName(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Display name is required"
            v.length > 24 -> "Keep it under 24 characters"
            else -> null
        }
    }
}
