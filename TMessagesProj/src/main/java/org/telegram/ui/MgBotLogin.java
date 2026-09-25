/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.MgTL;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Bot tokeni orqali akkauntga kirish (auth.importBotAuthorization).
 */
public class MgBotLogin {

    private static boolean loggingIn;

    public static void show(BaseFragment fragment, int account, Utilities.Callback<TLRPC.TL_auth_authorization> onSuccess) {
        show(fragment, account, onSuccess, "", null);
    }

    /** Tokenni tozalash: bo'shliqlar, "bot" prefiksi, api.telegram.org havolasi */
    static String cleanToken(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().replaceAll("\\s+", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{5,}:[A-Za-z0-9_-]{30,})").matcher(t);
        if (m.find()) {
            return m.group(1);
        }
        return t;
    }

    static boolean isValidToken(String t) {
        return t != null && t.matches("^\\d{5,}:[A-Za-z0-9_-]{30,}$");
    }

    private static void show(BaseFragment fragment, int account, Utilities.Callback<TLRPC.TL_auth_authorization> onSuccess, String prefill, String errorText) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        editText.setSingleLine(true);
        editText.setHint("123456789:AAE...");
        editText.setText(prefill);
        editText.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        if (errorText != null) {
            editText.setErrorText(errorText);
        }
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 4, 24, 0));

        if (errorText != null) {
            TextView err = new TextView(context);
            err.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            err.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
            err.setText(errorText);
            layout.addView(err, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 6, 24, 0));
        }

        TextView info = new TextView(context);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setText("Tokenni @BotFather dan olasiz (/mybots → bot → API Token). To'liq havolani ham joylashingiz mumkin — token o'zi ajratib olinadi.\n\n" +
                "Bot sifatida kirganda cheklovlar bor:\n" +
                "• kontaktlar va eski chatlar ko'rinmaydi;\n" +
                "• chatlar ro'yxati botga kimdir yozgandan keyin to'ladi;\n" +
                "• faqat botga /start bosgan odamlarga yoza olasiz;\n" +
                "• guruh va kanallarga o'zingiz qo'shila olmaysiz.");
        layout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 12, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle("Bot tokeni orqali kirish");
        builder.setView(layout);
        builder.setPositiveButton("Kirish", (dialog, which) -> {
            String token = cleanToken(editText.getText() == null ? "" : editText.getText().toString());
            if (!isValidToken(token)) {
                // Oyna qayta ochiladi, kiritilgan matn yo'qolmaydi
                AndroidUtilities.runOnUIThread(() -> show(fragment, account, onSuccess, token,
                        token.isEmpty() ? "Tokenni kiriting" : "Token noto'g'ri ko'rinishda. To'g'risi: 123456789:AAE…(35 ta belgi)"), 150);
                return;
            }
            login(fragment, account, token, onSuccess);
        });
        builder.setNeutralButton("Joylash", (dialog, which) -> {
            String clip = "";
            try {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(context);
                    clip = cs == null ? "" : cs.toString();
                }
            } catch (Throwable ignore) {
            }
            final String token = cleanToken(clip);
            final boolean clipEmpty = clip.isEmpty();
            AndroidUtilities.runOnUIThread(() -> show(fragment, account, onSuccess, token,
                    clipEmpty ? "Bufer bo'sh" : (isValidToken(token) ? null : "Buferdagi matnda token topilmadi")), 150);
        });
        builder.setNegativeButton("Bekor qilish", null);
        fragment.showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            editText.setSelection(editText.length());
            AndroidUtilities.showKeyboard(editText);
        }, 250);
    }

    private static String mapError(TLRPC.TL_error error) {
        if (error == null) {
            return "Noma'lum xato";
        }
        String t = error.text == null ? "" : error.text;
        if (t.contains("ACCESS_TOKEN_INVALID")) {
            return "Token noto'g'ri yoki bekor qilingan. @BotFather dan tokenni tekshiring.";
        }
        if (t.contains("ACCESS_TOKEN_EXPIRED")) {
            return "Token eskirgan (bekor qilingan). @BotFather → /revoke orqali yangisini oling.";
        }
        if (t.startsWith("FLOOD_WAIT")) {
            int secs = 0;
            try {
                secs = Integer.parseInt(t.substring(t.lastIndexOf('_') + 1));
            } catch (Throwable ignore) {
            }
            String when = secs >= 3600 ? (secs / 3600) + " soat " + (secs % 3600 / 60) + " daqiqa" : secs >= 60 ? (secs / 60) + " daqiqa" : secs + " soniya";
            return "Juda ko'p urinish bo'ldi. " + when + "dan keyin qayta urinib ko'ring.";
        }
        if (t.contains("API_ID")) {
            return "Ilova API kaliti Telegram tomonidan rad etildi (" + t + ").";
        }
        if (t.contains("USER_DEACTIVATED") || t.contains("BOT_") && t.contains("BAN")) {
            return "Bu bot Telegram tomonidan bloklangan.";
        }
        if (error.code == -1000 || t.isEmpty()) {
            return "Internetga ulanib bo'lmadi. Aloqani tekshirib, qayta urinib ko'ring.";
        }
        return "Xato: " + t;
    }

    private static void login(BaseFragment fragment, int account, String token, Utilities.Callback<TLRPC.TL_auth_authorization> onSuccess) {
        Context context = fragment.getParentActivity();
        if (context == null || loggingIn) {
            return;
        }
        loggingIn = true;
        AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        final int[] reqId = {0};
        final boolean[] cancelled = {false};
        progress.setCanCancel(true);
        progress.setOnCancelListener(d -> {
            cancelled[0] = true;
            loggingIn = false;
            if (reqId[0] != 0) {
                ConnectionsManager.getInstance(account).cancelRequest(reqId[0], true);
            }
        });
        progress.show();

        MgTL.TL_auth_importBotAuthorization req = new MgTL.TL_auth_importBotAuthorization();
        req.flags = 0;
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        req.bot_auth_token = token;
        reqId[0] = ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            loggingIn = false;
            if (cancelled[0]) {
                return;
            }
            try {
                progress.dismiss();
            } catch (Exception ignore) {
            }
            if (fragment.getParentActivity() == null) {
                return;
            }
            if (response instanceof TLRPC.TL_auth_authorization) {
                onSuccess.run((TLRPC.TL_auth_authorization) response);
            } else {
                show(fragment, account, onSuccess, token, mapError(error));
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagTryDifferentDc | ConnectionsManager.RequestFlagEnableUnauthorized);
    }
}
