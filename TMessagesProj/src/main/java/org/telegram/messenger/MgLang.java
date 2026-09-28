/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * MilliyGram qo'shimcha funksiyalari matnlarini ilova tiliga moslab beradi.
 * Asl matnlar o'zbekcha; rus va ingliz tarjimalari assets/mg_lang.tsv faylida (uz \t ru \t en).
 * Rus tiliga yaqin tillar (uk, be, kk, ky, tg) ruscha, qolgan hamma tillar inglizcha ko'radi.
 */
public final class MgLang {

    private static volatile HashMap<String, String[]> table;

    private MgLang() {
    }

    /** "uz", "ru" yoki "en" */
    public static String lang() {
        String code = null;
        try {
            LocaleController lc = LocaleController.getInstance();
            LocaleController.LocaleInfo info = lc.getCurrentLocaleInfo();
            if (info != null) {
                code = !TextUtils.isEmpty(info.pluralLangCode) ? info.pluralLangCode : info.shortName;
            }
            if (TextUtils.isEmpty(code) && lc.getCurrentLocale() != null) {
                code = lc.getCurrentLocale().getLanguage();
            }
        } catch (Throwable ignore) {
        }
        if (TextUtils.isEmpty(code)) {
            return "uz";
        }
        code = code.toLowerCase();
        int cut = code.indexOf('_');
        if (cut < 0) {
            cut = code.indexOf('-');
        }
        if (cut > 0) {
            code = code.substring(0, cut);
        }
        switch (code) {
            case "uz":
                return "uz";
            case "ru":
            case "uk":
            case "be":
            case "kk":
            case "ky":
            case "tg":
                return "ru";
            default:
                return "en";
        }
    }

    public static String t(String uz) {
        if (uz == null) {
            return null;
        }
        String l = lang();
        if ("uz".equals(l)) {
            return uz;
        }
        String[] v = table().get(uz);
        if (v == null) {
            return uz;
        }
        return "ru".equals(l) ? v[0] : v[1];
    }

    private static HashMap<String, String[]> table() {
        HashMap<String, String[]> t = table;
        if (t != null) {
            return t;
        }
        synchronized (MgLang.class) {
            if (table != null) {
                return table;
            }
            t = new HashMap<>(2048);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(ApplicationLoader.applicationContext.getAssets().open("mg_lang.tsv"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] p = line.split("\t", -1);
                    if (p.length >= 3) {
                        t.put(unescape(p[0]), new String[]{unescape(p[1]), unescape(p[2])});
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            table = t;
            return t;
        }
    }

    /** Java satr literalidagi \n, \", \\, \t, \' va \\uXXXX ketma-ketliklarini ochadi */
    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                                i += 4;
                                break;
                            } catch (NumberFormatException ignore) {
                            }
                        }
                        sb.append('u');
                        break;
                    default: sb.append(n); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
