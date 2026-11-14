package com.sadra.ai.billing

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Verifies the RSA (SHA1withRSA, IAB v3) signature Myket attaches to every purchase using your public key from the Myket panel. */
object SignatureVerifier {
    fun verify(publicKeyBase64: String, signedData: String, signatureBase64: String): Boolean {
        if (publicKeyBase64.isBlank() || signatureBase64.isBlank()) return false
        return try {
            val key = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicKeyBase64)))
            Signature.getInstance("SHA1withRSA").run {
                initVerify(key)
                update(signedData.toByteArray(Charsets.UTF_8))
                verify(Base64.getMimeDecoder().decode(signatureBase64))
            }
        } catch (e: Exception) { false }
    }
}
