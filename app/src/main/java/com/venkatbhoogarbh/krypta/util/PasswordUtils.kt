package com.venkatbhoogarbh.krypta.util

import com.venkatbhoogarbh.krypta.R
import java.security.SecureRandom

enum class PasswordStrength(val score: Int, val labelResId: Int, val colorResId: Int) {
    WEAK(1, R.string.strength_weak, R.color.strength_weak),
    FAIR(2, R.string.strength_fair, R.color.strength_fair),
    GOOD(3, R.string.strength_good, R.color.strength_good),
    STRONG(4, R.string.strength_strong, R.color.strength_strong);

    companion object {
        fun fromScore(score: Int): PasswordStrength {
            return when {
                score <= 2 -> WEAK
                score == 3 -> FAIR
                score == 4 -> GOOD
                else -> STRONG
            }
        }
    }
}

object PasswordUtils {
    private val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private val DIGITS = "0123456789"
    private val SYMBOLS = "!@#$%^&*()-_=+[]{}\\|;:'\",.<>/?"

    fun evaluateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.WEAK

        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.containsSpecialChar() }) score++

        val cappedScore = score.coerceIn(1, 5)
        return PasswordStrength.fromScore(cappedScore)
    }

    private fun Char.containsSpecialChar(): Boolean {
        return SYMBOLS.contains(this)
    }

    fun generatePassword(
        length: Int,
        useUpper: Boolean,
        useLower: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean
    ): String {
        val random = SecureRandom()
        val categories = mutableListOf<String>()
        if (useUpper) categories.add(UPPER)
        if (useLower) categories.add(LOWER)
        if (useDigits) categories.add(DIGITS)
        if (useSymbols) categories.add(SYMBOLS)

        if (categories.isEmpty()) return ""

        val password = StringBuilder()
        
        // Ensure at least one character from each selected category
        for (category in categories) {
            password.append(category[random.nextInt(category.length)])
        }

        // Fill remaining length
        val allChars = categories.joinToString("")
        for (i in categories.size until length) {
            password.append(allChars[random.nextInt(allChars.length)])
        }

        // Shuffle the password characters to avoid predictable patterns (first few chars)
        val charList = password.toString().toList().shuffled(random)
        return charList.joinToString("")
    }
}
