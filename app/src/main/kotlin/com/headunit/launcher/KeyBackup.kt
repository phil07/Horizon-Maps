package com.headunit.launcher

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The API keys and account secrets in a separate backup file, protected by a password the driver chooses: the key is derived from
 * the password (PBKDF2, 200 000 rounds) and the contents are sealed with AES-GCM. The normal backup never holds these keys.
 */
object KeyBackup {
    private const val FORMAT = "horizon-nav-keys"
    private const val ROUNDS = 200_000

    private fun derive(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ROUNDS, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }

    /** The backup text (JSON) for the keys that are set. The password is needed to restore them. */
    fun export(prefs: Prefs, password: String): String {
        require(password.length >= 6) { Lang.t("Das Passwort braucht mindestens 6 Zeichen", "The password needs at least 6 characters") }
        val keys = JSONObject()
        for (k in prefs.secretKeys()) { val v = prefs.s(k, ""); if (v.isNotBlank()) keys.put(k, v) }
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
        val sealed = c.doFinal(keys.toString().toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("format", FORMAT).put("v", 1)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(sealed, Base64.NO_WRAP))
            .toString(2)
    }

    /** Restores the keys from [export] output. Returns how many were restored; a wrong password changes nothing. */
    fun import(prefs: Prefs, text: String, password: String): Int {
        val o = JSONObject(text)
        require(o.optString("format") == FORMAT) { Lang.t("Das ist keine Schlüssel-Sicherung", "This is not a key backup") }
        val salt = Base64.decode(o.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(o.getString("iv"), Base64.NO_WRAP)
        val data = Base64.decode(o.getString("data"), Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
        val keys = try { JSONObject(String(c.doFinal(data), Charsets.UTF_8)) } catch (e: Exception) {
            throw IllegalArgumentException(Lang.t("Passwort falsch oder Datei beschädigt", "Wrong password or damaged file"))
        }
        // only the known key names, and nothing blank: a file can't write other settings through this
        var n = 0
        for (k in prefs.secretKeys()) {
            val v = keys.optString(k, "")
            if (v.isNotBlank()) { prefs.put(k, v); n++ }
        }
        return n
    }
}
