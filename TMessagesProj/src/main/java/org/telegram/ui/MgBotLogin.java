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

    public static void show(BaseFragment fragment, int account, Utilities.Callback<TLRPC.TL_auth_authorization> onSuccess) {
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
        editText.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 4, 24, 0));

        TextView info = new TextView(context);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setText("Tokenni @BotFather dan olasiz.\n\n" +
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
            String token = editText.getText().toString().trim();
            if (!token.matches("^\\d+:[A-Za-z0-9_-]{20,}$")) {
                showError(fragment, "Token noto'g'ri ko'rinishda. Masalan: 123456789:AAE...");
                return;
            }
            login(fragment, account, token, onSuccess);
        });
        builder.setNegativeButton("Bekor qilish", null);
        fragment.showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 250);
    }

    private static void login(BaseFragment fragment, int account, String token, Utilities.Callback<TLRPC.TL_auth_authorization> onSuccess) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.show();

        MgTL.TL_auth_importBotAuthorization req = new MgTL.TL_auth_importBotAuthorization();
        req.flags = 0;
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        req.bot_auth_token = token;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {
            }
            if (response instanceof TLRPC.TL_auth_authorization) {
                onSuccess.run((TLRPC.TL_auth_authorization) response);
            } else {
                String text = error != null ? error.text : "Noma'lum xato";
                if (text != null && text.contains("ACCESS_TOKEN_INVALID")) {
                    text = "Token noto'g'ri yoki bekor qilingan";
                } else if (text != null && text.contains("ACCESS_TOKEN_EXPIRED")) {
                    text = "Token eskirgan. @BotFather dan yangisini oling";
                }
                showError(fragment, text);
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin);
    }

    private static void showError(BaseFragment fragment, String text) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle("Kirib bo'lmadi");
        builder.setMessage(text);
        builder.setPositiveButton("OK", null);
        fragment.showDialog(builder.create());
    }
}
