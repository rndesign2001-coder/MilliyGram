/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Oddiy rejim (keksalar va ko'zi zaif foydalanuvchilar uchun):
 * katta interfeys, katta shrift, keng chatlar ro'yxati, soddalashtirilgan menyu va ovozli o'qish.
 */
public final class MgSimple {

    public static final int[] FONT_SIZES = {16, 18, 21, 24, 27};
    public static final int[] UI_SCALES = {100, 110, 120, 130};
    public static final float[] SPEECH_RATES = {0.7f, 0.85f, 1f, 1.2f};

    private static TextToSpeech tts;
    private static boolean ttsReady;
    private static String pendingText;

    private MgSimple() {
    }

    // ---------------------------------------------------------------- sozlamalar

    public static boolean isEnabled() {
        return MgConfig.isSimpleMode();
    }

    public static int getFontSize() {
        return MgConfig.getInt("simple_font", MgConfig.SIMPLE_MODE_FONT_SIZE);
    }

    /** Interfeys kattaligi, foizda (100 — odatiy). Faqat oddiy rejim yoqilganda ishlaydi */
    public static int getUiScale() {
        return MgConfig.getInt("simple_ui_scale", 110);
    }

    /** AndroidUtilities.checkDisplaySize dan chaqiriladi */
    public static float densityFactor() {
        try {
            if (!isEnabled()) {
                return 1f;
            }
            int s = getUiScale();
            return s < 100 || s > 150 ? 1f : s / 100f;
        } catch (Throwable e) {
            return 1f;
        }
    }

    public static boolean isSimpleMenu() {
        try {
            return isEnabled() && MgConfig.getBool("simple_menu", true);
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isSpeakButton() {
        return MgConfig.getBool("simple_speak_btn", true);
    }

    public static boolean isAutoRead() {
        return isEnabled() && MgConfig.getBool("simple_auto_read", false);
    }

    public static int getSpeechRateIndex() {
        return Math.max(0, Math.min(SPEECH_RATES.length - 1, MgConfig.getInt("simple_rate", 1)));
    }

    /** Oddiy rejimni yoqish/o'chirish; yoqilganda tavsiya etilgan sozlamalar birga qo'llanadi */
    public static void setEnabled(boolean on) {
        MgConfig.setBool("simple_mode", on);
        applyFont();
        if (on) {
            if (!SharedConfig.useThreeLinesLayout) {
                MgConfig.setBool("simple_prev_three_lines", false);
                SharedConfig.setUseThreeLinesLayout(true);
            } else {
                MgConfig.setBool("simple_prev_three_lines", true);
            }
        } else if (!MgConfig.getBool("simple_prev_three_lines", false) && SharedConfig.useThreeLinesLayout) {
            SharedConfig.setUseThreeLinesLayout(false);
        }
    }

    public static void setFontSize(int size) {
        MgConfig.setInt("simple_font", size);
        applyFont();
    }

    /** Xabarlar shriftini joriy holatga moslaydi */
    public static void applyFont() {
        int size = isEnabled() ? getFontSize() : MgConfig.NORMAL_FONT_SIZE;
        SharedConfig.fontSize = size;
        SharedConfig.fontSizeIsDefault = false;
        SharedPreferences main = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", android.app.Activity.MODE_PRIVATE);
        main.edit().putInt("fons_size", size).apply();
        try {
            org.telegram.ui.ActionBar.Theme.createCommonMessageResources();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ---------------------------------------------------------------- ovozli o'qish

    private static Locale pickLocale(String text) {
        boolean cyr = MgTranslit.isCyrillic(text);
        Locale[] tries = cyr
                ? new Locale[]{new Locale("uz", "UZ"), new Locale("ru", "RU")}
                : new Locale[]{new Locale("uz", "UZ"), new Locale("tr", "TR"), Locale.getDefault()};
        for (Locale l : tries) {
            try {
                int r = tts.isLanguageAvailable(l);
                if (r >= TextToSpeech.LANG_AVAILABLE) {
                    return l;
                }
            } catch (Throwable ignore) {
            }
        }
        return Locale.getDefault();
    }

    /** Matnni ovoz chiqarib o'qiydi (lotin yozuvidagi o'zbekcha matn kirillga o'girib o'qitiladi, agar o'zbek ovozi bo'lmasa) */
    public static void speak(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        String clean = text.replaceAll("https?://\\S+", "").trim();
        if (clean.length() > 3000) {
            clean = clean.substring(0, 3000);
        }
        if (tts == null) {
            pendingText = clean;
            try {
                tts = new TextToSpeech(ApplicationLoader.applicationContext, status -> {
                    ttsReady = status == TextToSpeech.SUCCESS;
                    String p = pendingText;
                    pendingText = null;
                    if (ttsReady && p != null) {
                        AndroidUtilities.runOnUIThread(() -> speakNow(p));
                    }
                });
            } catch (Throwable e) {
                FileLog.e(e);
                tts = null;
            }
            return;
        }
        if (!ttsReady) {
            pendingText = clean;
            return;
        }
        speakNow(clean);
    }

    private static void speakNow(String text) {
        try {
            Locale l = pickLocale(text);
            String toSay = text;
            // o'zbek ovozi yo'q telefonlarda lotin matnini rus ovozi kirill orqali tushunarliroq o'qiydi
            if (!"uz".equals(l.getLanguage()) && !MgTranslit.isCyrillic(text) && isUzbekLatin(text)) {
                Locale ru = new Locale("ru", "RU");
                if (tts.isLanguageAvailable(ru) >= TextToSpeech.LANG_AVAILABLE) {
                    toSay = MgTranslit.toCyrillic(text);
                    l = ru;
                }
            }
            tts.setLanguage(l);
            tts.setSpeechRate(SPEECH_RATES[getSpeechRateIndex()]);
            tts.speak(toSay, TextToSpeech.QUEUE_FLUSH, null, "mg_speak");
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static boolean isUzbekLatin(String t) {
        String s = t.toLowerCase(Locale.ROOT);
        return s.contains("o'") || s.contains("g'") || s.contains("oʻ") || s.contains("gʻ") || s.contains("sh") && s.contains("ch")
                || s.matches("(?s).*\\b(va|bilan|uchun|emas|yaxshi|salom|rahmat|qanday|nima|bu|men|siz)\\b.*");
    }

    public static void stop() {
        try {
            if (tts != null) {
                tts.stop();
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean isSpeaking() {
        try {
            return tts != null && tts.isSpeaking();
        } catch (Throwable e) {
            return false;
        }
    }

    /** Ochiq chatga kelgan yangi xabarlarni o'qib beradi (oddiy rejim + avto-o'qish yoqilgan bo'lsa) */
    public static void autoRead(int account, ArrayList<MessageObject> arr, boolean paused) {
        if (paused || arr == null || arr.isEmpty() || !isAutoRead()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (MessageObject m : arr) {
            if (m == null || m.isOut() || m.messageOwner == null || TextUtils.isEmpty(m.messageOwner.message)) {
                continue;
            }
            String name = "";
            try {
                long from = m.getFromChatId();
                if (from > 0) {
                    org.telegram.tgnet.TLRPC.User u = MessagesController.getInstance(account).getUser(from);
                    if (u != null) {
                        name = UserObject.getFirstName(u);
                    }
                }
            } catch (Throwable ignore) {
            }
            if (sb.length() > 0) {
                sb.append(". ");
            }
            if (!TextUtils.isEmpty(name)) {
                sb.append(name).append(": ");
            }
            sb.append(m.messageOwner.message);
        }
        if (sb.length() > 0) {
            speak(sb.toString());
        }
    }
}
