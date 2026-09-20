package com.example.keykeep.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager(private val context: Context) {

    private val keystoreProvider = "AndroidKeyStore"
    private val alias = "KeyKeepVaultKey"
    private val prefsName = "secure_prefs"
    private val prefEncryptedKey = "encrypted_db_key"
    private val prefIv = "db_key_iv"

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun getHardwareKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keystoreProvider).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keystoreProvider)
        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    fun initializeAndSaveDatabaseKey(): ByteArray {
        val dbKey = ByteArray(32)
        SecureRandom().nextBytes(dbKey)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getHardwareKey())

        val iv = cipher.iv
        val encryptedDbKey = cipher.doFinal(dbKey)

        // Using the recommended KTX extension to resolve the SharedPreferences warning
        prefs.edit {
            putString(prefEncryptedKey, Base64.encodeToString(encryptedDbKey, Base64.DEFAULT))
            putString(prefIv, Base64.encodeToString(iv, Base64.DEFAULT))
        }

        return dbKey
    }

    fun getSavedDatabaseKey(): ByteArray? {
        val encryptedKeyB64 = prefs.getString(prefEncryptedKey, null)
        val ivB64 = prefs.getString(prefIv, null)

        if (encryptedKeyB64 == null || ivB64 == null) return null

        val encryptedKey = Base64.decode(encryptedKeyB64, Base64.DEFAULT)
        val iv = Base64.decode(ivB64, Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getHardwareKey(), spec)

        return cipher.doFinal(encryptedKey)
    }
}