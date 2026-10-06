package com.experimentos.mobile.authentication.domain

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationPasswordPolicyTest {
    @Test
    fun acceptsValidPasswordsIncludingAccentsAndSymbols() {
        listOf("Password1!", "Árbol123!", "SafeSpace1🔒").forEach {
            assertNull(RegistrationPasswordPolicy.validationError(it))
        }
    }

    @Test
    fun rejectsEveryMissingRequirementAndWhitespaceAsASpecialCharacter() {
        listOf("Ab1!", "password1!", "PASSWORD1!", "Password!", "Password1", "Password1 ", "Password1\u00a0").forEach {
            assertNotNull(RegistrationPasswordPolicy.validationError(it))
        }
    }

    @Test
    fun enforcesTheBcryptUtf8ByteLimit() {
        assertNull(RegistrationPasswordPolicy.validationError("Ab1!" + "a".repeat(68)))
        assertNotNull(RegistrationPasswordPolicy.validationError("Ab1!" + "a".repeat(69)))
        assertNotNull(RegistrationPasswordPolicy.validationError("Áb1!" + "é".repeat(34)))
    }
}
