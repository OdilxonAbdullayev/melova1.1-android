package uz.melova.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Android Keystore: Supabase refresh/access tokens are not stored as plaintext. */
internal class Vault(context: Context) {
    private val prefs = context.getSharedPreferences("melova.secure.v1", Context.MODE_PRIVATE)
    private val alias = "melova-session-aes-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    fun set(name:String,value:String) {
        val c=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val encrypted=c.doFinal(value.toByteArray(Charsets.UTF_8))
        val full=c.iv + encrypted
        prefs.edit().putString(name,Base64.encodeToString(full,Base64.NO_WRAP)).apply()
    }
    fun get(name:String):String? {
        val value=prefs.getString(name,null) ?: return null
        return try {
            val data=Base64.decode(value,Base64.NO_WRAP)
            val c=Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,data.copyOfRange(0,12)))
            String(c.doFinal(data.copyOfRange(12,data.size)),Charsets.UTF_8)
        } catch (_:Exception) { prefs.edit().remove(name).apply();null }
    }
    fun remove(name:String) { prefs.edit().remove(name).apply() }
}
