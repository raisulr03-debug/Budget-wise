package com.example.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SHA_ALGORITHM = "SHA-256"

    /**
     * Derives a 256-bit AES key from a text password using SHA-256.
     */
    private fun deriveKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance(KEY_SHA_ALGORITHM)
        val keyBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts the input message with the given password.
     * Returns a Base64 encoded string containing [IV (12 bytes)] + [Ciphertext].
     */
    fun encrypt(message: String, password: String): String {
        if (password.isEmpty() || message.isEmpty()) return message
        try {
            val key = deriveKey(password)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val iv = cipher.iv // 12 bytes for GCM
            val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            return Base64.encodeToString(combined, Base64.DEFAULT or Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Decrypts a Base64 string that has been encrypted using encrypt.
     */
    fun decrypt(encryptedBase64: String, password: String): String {
        if (password.isEmpty() || encryptedBase64.isEmpty()) return encryptedBase64
        try {
            val key = deriveKey(password)
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            if (combined.size < 12) return ""
            
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
