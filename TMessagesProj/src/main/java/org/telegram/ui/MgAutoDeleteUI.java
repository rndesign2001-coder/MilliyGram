/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgAutoDelete;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

/**
 * "Vaqt o'tgach o'chirish" uchun muddat va yuborish usulini tanlash oynalari.
 */
public class MgAutoDeleteUI {

    /** Chatga post yuborish va uni keyin o'chirish mumkinmi (kanal yoki guruh) */
    public static boolean isAvailable(int account, long dialogId) {
        if (dialogId >= 0) {
            return false;
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        return chat != null && !ChatObject.isNotInChat(chat) && (ChatObject.canSendMessages(chat) || ChatObject.canPost(chat));
    }

    /** Muddat tanlash; seconds > 0 — tanlangan muddat, 0 — o'chirish kerak emas */
    public static void pickDuration(Context ctx, Theme.ResourcesProvider rp, boolean allowNone, Utilities.Callback<Integer> onPicked) {
        if (ctx == null) {
            return;
        }
        int n = MgAutoDelete.PRESET_HOURS.length;
        CharSequence[] items = new CharSequence[n + 1 + (allowNone ? 1 : 0)];
        int[] icons = new int[items.length];
        for (int i = 0; i < n; i++) {
            items[i] = MgAutoDelete.durationName(MgAutoDelete.PRESET_HOURS[i] * 3600);
            icons[i] = R.drawable.msg_autodelete;
        }
        items[n] = MgLang.t("Boshqa muddat (soatda)…");
        icons[n] = R.drawable.msg_edit;
        if (allowNone) {
            items[n + 1] = MgLang.t("O'chirilmasin");
            icons[n + 1] = R.drawable.msg_cancel;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, rp);
        b.setTitle(MgLang.t("Post qancha vaqtdan keyin o'chirilsin?"));
        b.setItems(items, icons, (d, which) -> {
            if (which < n) {
                onPicked.run(MgAutoDelete.PRESET_HOURS[which] * 3600);
            } else if (which == n) {
                AndroidUtilities.runOnUIThread(() -> askCustom(ctx, rp, onPicked), 150);
            } else {
                onPicked.run(0);
            }
        });
        b.show();
    }

    private static void askCustom(Context ctx, Theme.ResourcesProvider rp, Utilities.Callback<Integer> onPicked) {
        EditTextBoldCursor et = new EditTextBoldCursor(ctx);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        et.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, rp));
        et.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, rp));
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(MgLang.t("masalan, 36"));
        et.setGravity(Gravity.CENTER);
        et.setBackground(null);
        et.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, rp), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, rp), Theme.getColor(Theme.key_text_RedRegular, rp));
        FrameLayout fl = new FrameLayout(ctx);
        fl.addView(et, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 8, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, rp);
        b.setTitle(MgLang.t("Necha soatdan keyin o'chirilsin?"));
        b.setView(fl);
        b.setPositiveButton(MgLang.t("Tayyor"), (d, w) -> {
            int h;
            try {
                h = Integer.parseInt(et.getText().toString().trim());
            } catch (Throwable e) {
                h = 0;
            }
            if (h > 0) {
                onPicked.run(Math.min(h, 24 * 60) * 3600);
            }
        });
        b.setNegativeButton(MgLang.t("Bekor qilish"), null);
        b.show();
        AndroidUtilities.runOnUIThread(() -> {
            et.requestFocus();
            AndroidUtilities.showKeyboard(et);
        }, 200);
    }

    public interface Sender {
        /** scheduleDate = 0 — darhol */
        void send(boolean notify, int scheduleDate, int scheduleRepeatPeriod);
    }

    /**
     * Yuborish tugmasi menyusidan: muddat → "Hozir" yoki "Rejalashtirish" → yuborish.
     * Muddat post kanalda paydo bo'lgan vaqtdan hisoblanadi.
     */
    public static void sendWithAutoDelete(Context ctx, Theme.ResourcesProvider rp, int account, long dialogId, Sender sender) {
        pickDuration(ctx, rp, false, seconds -> {
            if (seconds == null || seconds <= 0) {
                return;
            }
            AlertDialog.Builder b = new AlertDialog.Builder(ctx, rp);
            b.setTitle(MgLang.t("Avto-o'chirish: ") + MgAutoDelete.durationName(seconds));
            b.setItems(new CharSequence[]{
                    MgLang.t("Hozir yuborish"),
                    MgLang.t("Vaqtini rejalashtirib yuborish…")
            }, new int[]{R.drawable.msg_send, R.drawable.msg_calendar2}, (d, which) -> {
                if (which == 0) {
                    MgAutoDelete.arm(account, dialogId, seconds);
                    sender.send(true, 0, 0);
                } else {
                    AndroidUtilities.runOnUIThread(() -> AlertsCreator.createScheduleDatePickerDialog(ctx, dialogId, (notify, scheduleDate, repeat) -> {
                        MgAutoDelete.arm(account, dialogId, seconds);
                        sender.send(notify, scheduleDate, repeat);
                    }, rp), 150);
                }
            });
            b.show();
        });
    }
}
