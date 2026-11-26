package com.caumarket.app.core.validation
import com.caumarket.app.core.error.*
import java.text.Normalizer
import java.util.Locale

object Validation {
    fun text(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
    fun length(value: String): Int = value.codePointCount(0, value.length)
    fun email(value: String): String {
        val email = value.trim().lowercase(Locale.ROOT)
        if (!Regex("^[^@\\s]+@cau[.]ac[.]kr$").matches(email)) invalid("email")
        return email
    }
    fun nickname(value: String): String = text(value).also {
        if (length(it) !in 2..10) invalid("nickname")
    }
    fun password(value: String) { if (length(value) < 8) invalid("password") }
    fun validatePasswordConfirmation(password: String, confirmation: String): Result<Unit> =
        if (password == confirmation) Result.success(Unit)
        else Result.failure(MarketException(MarketErrorCode.VALIDATION, "passwordConfirmation"))
    private fun invalid(field: String): Nothing = throw MarketException(MarketErrorCode.VALIDATION, field)
}
