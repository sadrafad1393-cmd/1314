package com.sadra.ai.billing

import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class SignatureVerifierTest {
    private val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val pub = Base64.getEncoder().encodeToString(kp.public.encoded)
    private fun sign(data: String) = Base64.getEncoder().encodeToString(
        Signature.getInstance("SHA1withRSA").run { initSign(kp.private); update(data.toByteArray()); sign() })

    @Test fun acceptsValidSignature() = assertTrue(SignatureVerifier.verify(pub, "hello", sign("hello")))
    @Test fun rejectsTamperedData() = assertFalse(SignatureVerifier.verify(pub, "hello!", sign("hello")))
    @Test fun rejectsWrongKey() {
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertFalse(SignatureVerifier.verify(Base64.getEncoder().encodeToString(other.public.encoded), "x", sign("x")))
    }
    @Test fun rejectsBlankKeyOrGarbage() {
        assertFalse(SignatureVerifier.verify("", "x", sign("x")))
        assertFalse(SignatureVerifier.verify(pub, "x", "not-base64!!"))
    }
}
