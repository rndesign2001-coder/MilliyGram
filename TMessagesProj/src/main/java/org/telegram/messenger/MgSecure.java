/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Maxfiy sozlamalarni (masalan, yashirin chatlar ro'yxati) Android Keystore kaliti bilan
 * AES-GCM orqali shifrlaydi. Kalit qurilmadan chiqarib bo'lmaydi.
 */
@android.annotation.TargetApi(23)
public final class MgSecure {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "milliygram_prefs_v1";
    private static final String PREFIX = "e1:";

    private MgSecure() {
    }

    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= 23;
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        gen.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return gen.generateKey();
    }

    /** Shifrlangan matn yoki xatolik bo'lsa null */
    public static String encrypt(String plain) {
        if (!isAvailable() || plain == null) {
            return null;
        }
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[1 + iv.length + ct.length];
            out[0] = (byte) iv.length;
            System.arraycopy(iv, 0, out, 1, iv.length);
            System.arraycopy(ct, 0, out, 1 + iv.length, ct.length);
            return PREFIX + Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** Ochilgan matn yoki xatolik bo'lsa null */
    public static String decrypt(String data) {
        if (!isAvailable() || data == null || !data.startsWith(PREFIX)) {
            return null;
        }
        try {
            byte[] in = Base64.decode(data.substring(PREFIX.length()), Base64.NO_WRAP);
            int ivLen = in[0] & 0xFF;
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, in, 1, ivLen));
            byte[] pt = c.doFinal(in, 1 + ivLen, in.length - 1 - ivLen);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }
}
