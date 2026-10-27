/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.PhoneFormat.PhoneFormat;

/**
 * Boshqa norasmiy klientlardagi qulayliklar: xabar vaqtida soniyalar, aylantirganda klaviaturani
 * yashirish, telefon raqamini yashirish, ikki marta bosish amali va qo'ng'iroqdan oldin so'rash.
 */
public final class MgExtras {

    public static final int TAP_OFF = 0;
    public static final int TAP_REPLY = 1;
    public static final int TAP_COPY = 2;
    public static final int TAP_EDIT = 3;
    public static final int TAP_SAVE = 4;
    public static final int TAP_FORWARD = 5;

    private MgExtras() {
    }

    public static String[] doubleTapNames() {
        return new String[]{
                MgLang.t("O'chiq"),
                MgLang.t("Javob berish"),
                MgLang.t("Nusxalash"),
                MgLang.t("Tahrirlash (o'z xabarim)"),
                MgLang.t("Saqlangan xabarlarga saqlash"),
                MgLang.t("Uzatish")
        };
    }

    public static int doubleTapMode() {
        int v = MgConfig.getInt("dbl_tap", TAP_OFF);
        return v < TAP_OFF || v > TAP_FORWARD ? TAP_OFF : v;
    }

    public static boolean showSeconds() {
        return MgConfig.getBool("time_seconds", false);
    }

    public static boolean hideKeyboardOnScroll() {
        return MgConfig.getBool("kb_hide_scroll", false);
    }

    public static boolean hidePhone() {
        return MgConfig.getBool("hide_phone", false);
    }

    public static boolean confirmCalls() {
        return MgConfig.getBool("confirm_call", false);
    }

    /** Xabar vaqti: sozlamaga qarab soniyalar bilan */
    public static String formatTime(long millis) {
        LocaleController lc = LocaleController.getInstance();
        return showSeconds() ? lc.getFormatterDayWithSeconds().format(millis) : lc.getFormatterDay().format(millis);
    }

    /**
     * O'z telefon raqamim (sozlamalar va profilda). Yashirish yoqilgan bo'lsa — mamlakat kodi va oxirgi
     * 2 ta raqam qoladi: "+998 •• ••• •• 12". Skrinshot ulashganda qulay.
     */
    public static String selfPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return phone;
        }
        String formatted = PhoneFormat.getInstance().format("+" + phone);
        if (!hidePhone()) {
            return formatted;
        }
        StringBuilder sb = new StringBuilder();
        int digitsTotal = 0;
        for (int i = 0; i < formatted.length(); i++) {
            if (Character.isDigit(formatted.charAt(i))) {
                digitsTotal++;
            }
        }
        int firstSpace = formatted.indexOf(' ');
        int digitsSeen = 0;
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (Character.isDigit(c)) {
                digitsSeen++;
                boolean countryCode = firstSpace > 0 && i < firstSpace;
                boolean tail = digitsSeen > digitsTotal - 2;
                sb.append(countryCode || tail ? c : '•');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
