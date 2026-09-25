/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

/**
 * O'zbek tili: Kirill ↔ Lotin o'giruvchi (1995-yilgi lotin alifbosi qoidalari bo'yicha).
 * "Е" so'z boshida va unlidan keyin "ye", "Ц" undoshdan keyin "s", unlidan keyin "ts" bo'ladi va h.k.
 */
public final class MgTranslit {

    private static final String CYR_VOWELS = "аеёиоуэюяўАЕЁИОУЭЮЯЎ";
    private static final String LAT_VOWELS = "aeiouAEIOU";

    public static boolean isCyrillic(CharSequence s) {
        int cyr = 0, lat = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0400 && c <= 0x04FF) {
                cyr++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
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
        return isCyrillic(s) ? toLatin(s.toString()) : toCyrillic(s.toString());
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
                case 'ч': r = "ch"; break;
                case 'ш': r = "sh"; break;
                case 'щ': r = "sh"; break;
                case 'ъ': r = "ʼ"; break;
                case 'ы': r = "i"; break;
                case 'ь': r = ""; break;
                case 'э': r = "e"; break;
                case 'ю': r = "yu"; break;
                case 'я': r = "ya"; break;
                case 'ў': r = "oʻ"; break;
                case 'қ': r = "q"; break;
                case 'ғ': r = "gʻ"; break;
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
        StringBuilder sb = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char lc = Character.toLowerCase(c);
            boolean up = Character.isUpperCase(c);
            char next = i + 1 < n ? Character.toLowerCase(s.charAt(i + 1)) : 0;
            String r = null;
            int skip = 0;
            if ((lc == 'o' || lc == 'g') && i + 1 < n && isApos(s.charAt(i + 1))) {
                r = lc == 'o' ? "ў" : "ғ";
                skip = 1;
            } else if (lc == 's' && next == 'h') {
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
}
