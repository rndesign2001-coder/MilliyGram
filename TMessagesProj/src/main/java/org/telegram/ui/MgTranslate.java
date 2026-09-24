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
        fragment.showDialog(builder.create());
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

    /**
     * Matnni tarjima qiladi. Natija UI thread'da: (tarjima, xato matni)
     */
    public static void translate(int account, String text, ArrayList<TLRPC.MessageEntity> entities, String lang,
                                 Utilities.Callback2<TLRPC.TL_textWithEntities, String> done) {
        TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
        req.flags |= 2;
        TLRPC.TL_textWithEntities t = new TLRPC.TL_textWithEntities();
        t.text = text == null ? "" : text;
        if (entities != null) {
            t.entities = new ArrayList<>(entities);
        }
        req.text.add(t);
        req.to_lang = lang;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res instanceof TLRPC.TL_messages_translateResult
                    && !((TLRPC.TL_messages_translateResult) res).result.isEmpty()
                    && ((TLRPC.TL_messages_translateResult) res).result.get(0) != null) {
                done.run(((TLRPC.TL_messages_translateResult) res).result.get(0), null);
            } else {
                done.run(null, err != null ? err.text : "Tarjima qilib bo'lmadi");
            }
        }));
    }
}
