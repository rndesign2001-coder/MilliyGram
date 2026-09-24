/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.TranslateAlert2;

import java.util.ArrayList;

/**
 * Tarjima yordamchisi: matnni (formatlash bilan) istalgan tilga tarjima qilish.
 */
public class MgTranslate {

    public static final String[] CODES = {"uz", "ru", "en", "tr", "kk", "ky", "tg", "ar", "de", "ko", "zh", "fr", "es", "fa"};
    public static final String[] NAMES = {"🇺🇿 O'zbekcha", "🇷🇺 Ruscha", "🇬🇧 Inglizcha", "🇹🇷 Turkcha", "🇰🇿 Qozoqcha", "🇰🇬 Qirg'izcha", "🇹🇯 Tojikcha", "🇸🇦 Arabcha", "🇩🇪 Nemischa", "🇰🇷 Koreyscha", "🇨🇳 Xitoycha", "🇫🇷 Fransuzcha", "🇪🇸 Ispancha", "🇮🇷 Forscha"};

    public static String nameOf(String code) {
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(code)) {
                return NAMES[i];
            }
        }
        return code;
    }

    /** Yozilgan matnni tarjima qilishda oxirgi tanlangan til */
    public static String lastOutgoingLang() {
        return MgConfig.getString("mg_translate_out", "en");
    }

    /** Birinchi ishga tushishda xabarlar tarjima tilini o'zbekcha qilib qo'yadi */
    public static void ensureDefaultTarget() {
        try {
            if (!MgConfig.getBool("mg_translate_default_set", false)) {
                MgConfig.setBool("mg_translate_default_set", true);
                if (!MessagesController.getGlobalMainSettings().contains("translate_to_language")) {
                    TranslateAlert2.setToLanguage("uz");
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static void chooseLanguage(BaseFragment fragment, String title, Utilities.Callback<String> onChosen) {
        chooseLanguage(fragment, title, false, onChosen);
    }

    /** overlay=true — boshqa dialog ustida ochiladi (uni yopmaydi) */
    public static void chooseLanguage(BaseFragment fragment, String title, boolean overlay, Utilities.Callback<String> onChosen) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        String last = lastOutgoingLang();
        CharSequence[] items = new CharSequence[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            items[i] = NAMES[i] + (CODES[i].equals(last) ? "  ✓" : "");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(title);
        builder.setItems(items, (dialog, which) -> {
            MgConfig.setString("mg_translate_out", CODES[which]);
            onChosen.run(CODES[which]);
        });
        if (overlay) {
            builder.show();
        } else {
            fragment.showDialog(builder.create());
        }
    }

    /** Tilni tanlash (saqlamasdan), joriy til belgilanadi */
    public static void chooseTarget(BaseFragment fragment, String title, String current, Utilities.Callback<String> onChosen) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            items[i] = NAMES[i] + (CODES[i].equals(current) ? "  ✓" : "");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(title);
        builder.setItems(items, (dialog, which) -> onChosen.run(CODES[which]));
        fragment.showDialog(builder.create());
    }

    public static final int ENGINE_AUTO = 0;
    public static final int ENGINE_GOOGLE = 1;
    public static final int ENGINE_TELEGRAM = 2;
    public static final String[] ENGINE_NAMES = {"Avtomatik (Telegram + Google)", "Google Tarjimon", "Telegram"};

    public static int getEngine() {
        return MgConfig.getInt("mg_translate_engine", ENGINE_AUTO);
    }

    /**
     * Matnni tarjima qiladi. Natija UI thread'da: (tarjima, xato matni).
     * Avtomatik rejimda avval Telegram, u ishlamasa yoki matnni o'zgartirmasa — Google.
     */
    public static void translate(int account, String text, ArrayList<TLRPC.MessageEntity> entities, String lang,
                                 Utilities.Callback2<TLRPC.TL_textWithEntities, String> done) {
        final String src = text == null ? "" : text;
        int engine = getEngine();
        if (engine == ENGINE_GOOGLE) {
            google(src, lang, done);
            return;
        }
        TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
        req.flags |= 2;
        TLRPC.TL_textWithEntities t = new TLRPC.TL_textWithEntities();
        t.text = src;
        if (entities != null) {
            t.entities = new ArrayList<>(entities);
        }
        req.text.add(t);
        req.to_lang = lang;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            TLRPC.TL_textWithEntities result = null;
            if (res instanceof TLRPC.TL_messages_translateResult
                    && !((TLRPC.TL_messages_translateResult) res).result.isEmpty()) {
                result = ((TLRPC.TL_messages_translateResult) res).result.get(0);
            }
            boolean unchanged = result == null || result.text == null || result.text.trim().equals(src.trim());
            if (!unchanged) {
                done.run(result, null);
            } else if (engine == ENGINE_AUTO) {
                google(src, lang, done);
            } else {
                done.run(null, err != null ? "Telegram: " + err.text : "Telegram tarjima qila olmadi");
            }
        }));
    }

    /** Google Tarjimon (ochiq gtx endpoint) — formatlashsiz oddiy matn */
    public static void google(String text, String lang, Utilities.Callback2<TLRPC.TL_textWithEntities, String> done) {
        Utilities.globalQueue.postRunnable(() -> {
            String out = null;
            String error = null;
            java.net.HttpURLConnection c = null;
            try {
                java.net.URL url = new java.net.URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                        + java.net.URLEncoder.encode(lang, "UTF-8") + "&dt=t&ie=UTF-8&oe=UTF-8");
                c = (java.net.HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setDoOutput(true);
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36");
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                byte[] body = ("q=" + java.net.URLEncoder.encode(text, "UTF-8")).getBytes("UTF-8");
                c.setFixedLengthStreamingMode(body.length);
                try (java.io.OutputStream os = c.getOutputStream()) {
                    os.write(body);
                }
                int code = c.getResponseCode();
                if (code != 200) {
                    error = "Google: HTTP " + code;
                } else {
                    StringBuilder sb = new StringBuilder();
                    try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream(), "UTF-8"))) {
                        char[] buf = new char[4096];
                        int n;
                        while ((n = r.read(buf)) > 0) {
                            sb.append(buf, 0, n);
                        }
                    }
                    org.json.JSONArray root = new org.json.JSONArray(sb.toString());
                    org.json.JSONArray parts = root.getJSONArray(0);
                    StringBuilder res = new StringBuilder();
                    for (int i = 0; i < parts.length(); i++) {
                        org.json.JSONArray part = parts.optJSONArray(i);
                        if (part != null && !part.isNull(0)) {
                            res.append(part.optString(0, ""));
                        }
                    }
                    out = res.toString();
                }
            } catch (Throwable e) {
                error = "Internet orqali tarjima qilib bo'lmadi";
            } finally {
                if (c != null) {
                    try {
                        c.disconnect();
                    } catch (Throwable ignore) {
                    }
                }
            }
            final String outF = out;
            final String errF = error;
            AndroidUtilities.runOnUIThread(() -> {
                if (outF != null && !outF.isEmpty()) {
                    TLRPC.TL_textWithEntities t = new TLRPC.TL_textWithEntities();
                    t.text = outF;
                    done.run(t, null);
                } else {
                    done.run(null, errF != null ? errF : "Tarjima qilib bo'lmadi");
                }
            });
        });
    }
}
