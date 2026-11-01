/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import java.nio.charset.StandardCharsets;

/** API_ID / API_HASH build paytida XOR bilan yashiriladi; bu yerda xotirada ochiladi */
final class MgKeys {

    private MgKeys() {
    }

    static int appId() {
        return BuildConfig.MG_K1 ^ BuildConfig.MG_K2;
    }

    static String appHash() {
        byte[] data = hex(BuildConfig.MG_K3);
        byte[] key = hex(BuildConfig.MG_K4);
        if (key.length == 0) {
            return "";
        }
        for (int i = 0; i < data.length; i++) {
            data[i] ^= key[i % key.length];
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private static byte[] hex(String s) {
        int n = s.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
