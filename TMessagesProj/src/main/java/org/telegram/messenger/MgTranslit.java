/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

/**
 * O'zbek tili yozuvlari o'rtasida o'giruvchi:
 *  - Kirill;
 *  - Lotin (1995-yilgi alifbo: oʻ, gʻ, sh, ch);
 *  - Yangi lotin (2026-yil 21-oktyabr qonuni: Ö, Ğ, Ş, Ç; "ng" alohida harf emas).
 * "Е" so'z boshida va unlidan keyin "ye", "Ц" undoshdan keyin "s", unlidan keyin "ts" bo'ladi va h.k.
 * Havolalar, @username, #heshteg, /buyruq va e-pochtalar o'zgartirilmaydi.
 */
public final class MgTranslit {

    public static final int CYR = 0;
    public static final int LAT = 1;
    public static final int LAT_NEW = 2;

    private static final String CYR_VOWELS = "аеёиоуэюяўАЕЁИОУЭЮЯЎ";
    private static final String LAT_VOWELS = "aeiouöAEIOUÖ";

    /** Yangi lotin alifbosidagi maxsus harflar */
    public static boolean isNewLetter(char c) {
        switch (c) {
            case 'ş': case 'Ş': case 'ç': case 'Ç': case 'ğ': case 'Ğ': case 'ö': case 'Ö':
                return true;
        }
        return false;
    }

    /** Matn qaysi yozuvda: CYR, LAT (eski lotin) yoki LAT_NEW (yangi lotin) */
    public static int detect(CharSequence cs) {
        if (cs == null) {
            return LAT;
        }
        if (isCyrillic(cs)) {
            return CYR;
        }
        String s = cs.toString();
        int fresh = 0, old = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isNewLetter(c)) {
                fresh++;
                continue;
            }
            char lc = Character.toLowerCase(c);
            if (i + 1 < s.length()) {
                char nx = s.charAt(i + 1);
                if ((lc == 's' || lc == 'c') && Character.toLowerCase(nx) == 'h') {
                    old++;
                } else if ((lc == 'o' || lc == 'g') && isApos(nx) && isLetter(s, i + 2)) {
                    old++;
                }
            }
        }
        return fresh > 0 && fresh >= old ? LAT_NEW : LAT;
    }

    /** Matnni aniqlangan yozuvdan {@code target} yozuviga o'giradi */
    public static String convert(CharSequence s, int target) {
        return s == null ? "" : convert(s, detect(s), target);
    }

    public static String convert(CharSequence cs, int from, int target) {
        if (cs == null) {
            return "";
        }
        String s = cs.toString();
        if (from == target || s.isEmpty()) {
            return s;
        }
        // havola va shu kabi bo'laklar o'zgarmaydi
        StringBuilder out = new StringBuilder(s.length() + 16);
        int n = s.length(), plainStart = 0, i = 0;
        while (i < n) {
            if (Character.isWhitespace(s.charAt(i))) {
                i++;
                continue;
            }
            int e = i;
            while (e < n && !Character.isWhitespace(s.charAt(e))) {
                e++;
            }
            if (isProtected(s, i, e)) {
                out.append(convertRaw(s.substring(plainStart, i), from, target));
                out.append(s, i, e);
                plainStart = e;
            }
            i = e;
        }
        out.append(convertRaw(s.substring(plainStart), from, target));
        return out.toString();
    }

    private static boolean isProtected(String s, int a, int b) {
        // bosh tinish belgilari ("(" va qo'shtirnoq) e'tiborga olinmaydi
        while (a < b && "([{\"«“'".indexOf(s.charAt(a)) >= 0) {
            a++;
        }
        if (a >= b) {
            return false;
        }
        char c = s.charAt(a);
        if ((c == '@' || c == '#' || c == '$' || c == '/') && a + 1 < b && (Character.isLetterOrDigit(s.charAt(a + 1)) || s.charAt(a + 1) == '_')) {
            return true;
        }
        String w = s.substring(a, b).toLowerCase(java.util.Locale.ROOT);
        if (w.contains("://") || w.startsWith("www.") || w.startsWith("t.me/") || w.startsWith("tg:")) {
            return true;
        }
        int at = w.indexOf('@');
        if (at > 0 && w.indexOf('.', at) > at + 1) {
            return true; // e-pochta
        }
        // domen.uz/yo'l ko'rinishidagi havolalar
        int dot = w.indexOf('.');
        if (dot > 0 && dot < w.length() - 2 && w.indexOf('/') > dot) {
            return true;
        }
        return false;
    }

    private static String convertRaw(String s, int from, int target) {
        if (s.isEmpty()) {
            return s;
        }
        if (from == CYR) {
            return toLatin(s, target == LAT_NEW);
        }
        if (target == CYR) {
            return toCyrillic(s, from == LAT_NEW);
        }
        return target == LAT_NEW ? oldToNew(s) : newToOld(s);
    }

    /** Formatlash (qalin, kursiv, havola…) saqlangan holda o'giradi */
    public static CharSequence convertSpanned(CharSequence src, int target) {
        if (src == null) {
            return "";
        }
        final int from = detect(src);
        if (!(src instanceof android.text.Spanned)) {
            return convert(src, from, target);
        }
        android.text.Spanned sp = (android.text.Spanned) src;
        Object[] spans = sp.getSpans(0, sp.length(), Object.class);
        java.util.TreeSet<Integer> cuts = new java.util.TreeSet<>();
        cuts.add(0);
        cuts.add(sp.length());
        for (Object o : spans) {
            int a = sp.getSpanStart(o), b = sp.getSpanEnd(o);
            if (a >= 0 && b >= 0) {
                cuts.add(a);
                cuts.add(b);
            }
        }
        Integer[] pts = cuts.toArray(new Integer[0]);
        int[] map = new int[sp.length() + 1];
        StringBuilder text = new StringBuilder(sp.length() + 16);
        for (int k = 0; k + 1 < pts.length; k++) {
            int a = pts[k], b = pts[k + 1];
            String seg = sp.subSequence(a, b).toString();
            map[a] = text.length();
            text.append(keepAsIs(sp, a, b) ? seg : convert(seg, from, target));
        }
        map[sp.length()] = text.length();
        android.text.SpannableStringBuilder res = new android.text.SpannableStringBuilder(text);
        for (Object o : spans) {
            int a = sp.getSpanStart(o), b = sp.getSpanEnd(o);
            if (a < 0 || b < 0) {
                continue;
            }
            int na = map[a], nb = map[b];
            if (nb < na) {
                continue;
            }
            int fl = sp.getSpanFlags(o);
            if (na == nb && (fl & android.text.Spanned.SPAN_POINT_MARK_MASK) == android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                continue;
            }
            try {
                res.setSpan(o, na, nb, fl);
            } catch (Exception ignore) {
            }
        }
        return res;
    }

    /** Kod, emoji va yashirin havolali bo'laklar o'zgarmaydi */
    private static boolean keepAsIs(android.text.Spanned sp, int a, int b) {
        if (a >= b) {
            return true;
        }
        for (Object o : sp.getSpans(a, b, Object.class)) {
            if (sp.getSpanStart(o) > a || sp.getSpanEnd(o) < b) {
                continue;
            }
            if (o instanceof org.telegram.ui.Components.URLSpanMono || o instanceof org.telegram.ui.Components.AnimatedEmojiSpan
                    || o.getClass().getName().contains("CodeHighlighting")) {
                return true;
            }
            if (o instanceof org.telegram.ui.Components.TextStyleSpan) {
                int f = ((org.telegram.ui.Components.TextStyleSpan) o).getStyleFlags();
                if ((f & (org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_MONO | org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_CODE)) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Yozuv nomi (menyular uchun) */
    public static String scriptName(int script) {
        switch (script) {
            case CYR: return MgLang.t("Kirill yozuvi");
            case LAT_NEW: return MgLang.t("Yangi lotin yozuvi (2026)");
            default: return MgLang.t("Eski lotin yozuvi (1995)");
        }
    }

    /** Namuna (menyuda ko'rsatish uchun) */
    public static String scriptSample(int script) {
        switch (script) {
            case CYR: return "Ўзбекистон, ғалаба, шаҳар, чироқ";
            case LAT_NEW: return "Özbekiston, ğalaba, şahar, çiroq";
            default: return "Oʻzbekiston, gʻalaba, shahar, chiroq";
        }
    }

    public static boolean isCyrillic(CharSequence s) {
        int cyr = 0, lat = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0400 && c <= 0x04FF) {
                cyr++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || isNewLetter(c)) {
                lat++;
            }
        }
        return cyr > lat;
    }

    public static boolean hasLetters(CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Matn qaysi yozuvda bo'lsa, ikkinchisiga o'giradi */
    public static String flip(CharSequence s) {
        int from = detect(s);
        return convert(s, from, from == CYR ? LAT : CYR);
    }

    private static boolean isUpper(String s, int i) {
        return i >= 0 && i < s.length() && Character.isUpperCase(s.charAt(i));
    }

    private static boolean isLetter(String s, int i) {
        return i >= 0 && i < s.length() && Character.isLetter(s.charAt(i));
    }

    /** Ikki harfli lotin mosligi uchun registr: "Ш" → "Sh", "ШАХАР" → "SHAHAR" */
    private static String cased(String lat, String src, int i) {
        if (!Character.isUpperCase(src.charAt(i))) {
            return lat;
        }
        boolean wordUpper = isUpper(src, i + 1) || (!isLetter(src, i + 1) && isUpper(src, i - 1));
        if (lat.length() == 1 || wordUpper) {
            return lat.toUpperCase();
        }
        return Character.toUpperCase(lat.charAt(0)) + lat.substring(1);
    }

    public static String toLatin(String s) {
        return toLatin(s, false);
    }

    public static String toLatin(String s, boolean newAlphabet) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char lc = Character.toLowerCase(c);
            String r;
            switch (lc) {
                case 'а': r = "a"; break;
                case 'б': r = "b"; break;
                case 'в': r = "v"; break;
                case 'г': r = "g"; break;
                case 'д': r = "d"; break;
                case 'е': {
                    char prev = i > 0 ? s.charAt(i - 1) : ' ';
                    boolean start = !Character.isLetter(prev) || CYR_VOWELS.indexOf(prev) >= 0 || prev == 'ъ' || prev == 'Ъ' || prev == 'ь' || prev == 'Ь';
                    r = start ? "ye" : "e";
                    break;
                }
                case 'ё': r = "yo"; break;
                case 'ж': r = "j"; break;
                case 'з': r = "z"; break;
                case 'и': r = "i"; break;
                case 'й': r = "y"; break;
                case 'к': r = "k"; break;
                case 'л': r = "l"; break;
                case 'м': r = "m"; break;
                case 'н': r = "n"; break;
                case 'о': r = "o"; break;
                case 'п': r = "p"; break;
                case 'р': r = "r"; break;
                case 'с': r = "s"; break;
                case 'т': r = "t"; break;
                case 'у': r = "u"; break;
                case 'ф': r = "f"; break;
                case 'х': r = "x"; break;
                case 'ц': {
                    char prev = i > 0 ? s.charAt(i - 1) : ' ';
                    r = Character.isLetter(prev) && CYR_VOWELS.indexOf(prev) < 0 ? "s" : (Character.isLetter(prev) ? "ts" : "s");
                    break;
                }
                case 'ч': r = newAlphabet ? "ç" : "ch"; break;
                case 'ш': r = newAlphabet ? "ş" : "sh"; break;
                case 'щ': r = newAlphabet ? "ş" : "sh"; break;
                case 'ъ': r = "ʼ"; break;
                case 'ы': r = "i"; break;
                case 'ь': r = ""; break;
                case 'э': r = "e"; break;
                case 'ю': r = "yu"; break;
                case 'я': r = "ya"; break;
                case 'ў': r = newAlphabet ? "ö" : "oʻ"; break;
                case 'қ': r = "q"; break;
                case 'ғ': r = newAlphabet ? "ğ" : "gʻ"; break;
                case 'ҳ': r = "h"; break;
                default:
                    sb.append(c);
                    continue;
            }
            if (r.isEmpty()) {
                continue;
            }
            if (r.equals("oʻ") || r.equals("gʻ")) {
                sb.append(Character.isUpperCase(c) ? Character.toUpperCase(r.charAt(0)) : r.charAt(0)).append('ʻ');
            } else {
                sb.append(cased(r, s, i));
            }
        }
        return sb.toString();
    }

    private static boolean isApos(char c) {
        return c == '\'' || c == '‘' || c == '’' || c == 'ʻ' || c == 'ʼ' || c == '`';
    }

    public static String toCyrillic(String s) {
        return toCyrillic(s, false);
    }

    /** @param newAlphabet manba yangi lotinda: "sh"/"ch" ikki alohida harf, "o'" esa o + tutuq */
    public static String toCyrillic(String s, boolean newAlphabet) {
        StringBuilder sb = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char lc = Character.toLowerCase(c);
            boolean up = Character.isUpperCase(c);
            char next = i + 1 < n ? Character.toLowerCase(s.charAt(i + 1)) : 0;
            String r = null;
            int skip = 0;
            if (lc == 'ş' || lc == 'ç' || lc == 'ö' || lc == 'ğ') {
                r = lc == 'ş' ? "ш" : lc == 'ç' ? "ч" : lc == 'ö' ? "ў" : "ғ";
            } else if (!newAlphabet && (lc == 'o' || lc == 'g') && i + 1 < n && isApos(s.charAt(i + 1))) {
                r = lc == 'o' ? "ў" : "ғ";
                skip = 1;
            } else if (!newAlphabet && lc == 's' && next == 'h') {
                r = "ш";
                skip = 1;
            } else if (lc == 'c' && next == 'h') {
                r = "ч";
                skip = 1;
            } else if (lc == 'y' && (next == 'o' || next == 'u' || next == 'a' || next == 'e')
                    && !(next == 'o' && i + 2 < n && isApos(s.charAt(i + 2)))) {
                r = next == 'o' ? "ё" : next == 'u' ? "ю" : next == 'a' ? "я" : "е";
                skip = 1;
            } else {
                switch (lc) {
                    case 'a': r = "а"; break;
                    case 'b': r = "б"; break;
                    case 'c': r = "с"; break;
                    case 'd': r = "д"; break;
                    case 'e': {
                        char prev = i > 0 ? s.charAt(i - 1) : ' ';
                        r = !Character.isLetter(prev) || LAT_VOWELS.indexOf(prev) >= 0 ? "э" : "е";
                        break;
                    }
                    case 'f': r = "ф"; break;
                    case 'g': r = "г"; break;
                    case 'h': r = "ҳ"; break;
                    case 'i': r = "и"; break;
                    case 'j': r = "ж"; break;
                    case 'k': r = "к"; break;
                    case 'l': r = "л"; break;
                    case 'm': r = "м"; break;
                    case 'n': r = "н"; break;
                    case 'o': r = "о"; break;
                    case 'p': r = "п"; break;
                    case 'q': r = "қ"; break;
                    case 'r': r = "р"; break;
                    case 's': r = "с"; break;
                    case 't': r = "т"; break;
                    case 'u': r = "у"; break;
                    case 'v': r = "в"; break;
                    case 'w': r = "в"; break;
                    case 'x': r = "х"; break;
                    case 'y': r = "й"; break;
                    case 'z': r = "з"; break;
                    default:
                        if (isApos(c) && i > 0 && Character.isLetter(s.charAt(i - 1)) && i + 1 < n && Character.isLetter(s.charAt(i + 1))) {
                            r = "ъ";
                        }
                        break;
                }
            }
            if (r == null) {
                sb.append(c);
                continue;
            }
            sb.append(up ? r.toUpperCase() : r);
            i += skip;
        }
        return sb.toString();
    }

    /** Eski lotin → yangi lotin: oʻ→ö, gʻ→ğ, sh→ş, ch→ç */
    public static String oldToNew(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char lc = Character.toLowerCase(c);
            boolean up = Character.isUpperCase(c);
            char next = i + 1 < n ? Character.toLowerCase(s.charAt(i + 1)) : 0;
            if ((lc == 'o' || lc == 'g') && i + 1 < n && isApos(s.charAt(i + 1)) && (isLetter(s, i - 1) || isLetter(s, i + 2))) {
                char r = lc == 'o' ? 'ö' : 'ğ';
                sb.append(up ? Character.toUpperCase(r) : r);
                i++;
            } else if ((lc == 's' || lc == 'c') && next == 'h') {
                char r = lc == 's' ? 'ş' : 'ç';
                sb.append(up ? Character.toUpperCase(r) : r);
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Yangi lotin → eski lotin: ö→oʻ, ğ→gʻ, ş→sh, ç→ch */
    public static String newToOld(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (Character.toLowerCase(c)) {
                case 'ö':
                    sb.append(Character.isUpperCase(c) ? 'O' : 'o').append('ʻ');
                    break;
                case 'ğ':
                    sb.append(Character.isUpperCase(c) ? 'G' : 'g').append('ʻ');
                    break;
                case 'ş':
                    sb.append(cased("sh", s, i));
                    break;
                case 'ç':
                    sb.append(cased("ch", s, i));
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
