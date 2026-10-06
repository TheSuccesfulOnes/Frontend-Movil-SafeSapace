package com.experimentos.mobile.authentication.domain

/** Mirrors the registration policy enforced by the backend; passwords are never normalized. */
object RegistrationPasswordPolicy {
    const val REQUIREMENTS_MESSAGE = "Incluye al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial."
    const val TOO_LONG_MESSAGE = "La contraseña es demasiado larga. Usa menos caracteres."

    private val uppercase = Regex("\\p{Lu}")
    private val lowercase = Regex("\\p{Ll}")
    private val digit = Regex("\\p{Nd}")
    private val special = Regex("[^\\p{L}\\p{N}\\p{Z}\\s\\p{C}]")

    fun requirements(password: String): List<Pair<String, Boolean>> = listOf(
        "Al menos 8 caracteres" to (password.codePointCount(0, password.length) >= 8),
        "Una letra mayúscula" to uppercase.containsMatchIn(password),
        "Una letra minúscula" to lowercase.containsMatchIn(password),
        "Un número" to digit.containsMatchIn(password),
        "Un carácter especial, como !, @ o #" to special.containsMatchIn(password),
    )

    fun validationError(password: String): String? = when {
        password.toByteArray(Charsets.UTF_8).size > 72 -> TOO_LONG_MESSAGE
        requirements(password).any { !it.second } -> REQUIREMENTS_MESSAGE
        else -> null
    }
}
