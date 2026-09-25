/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * Ovozni tanish va tarjima dvigateli Novagram (VipAds LLC, GPL v2) kodidan olingan.
 */

package org.telegram.ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.fenixuz.ui.voice_translate.VoiceTranslateSheet;
import org.fenixuz.utils.VoiceDictation;
import org.fenixuz.utils.VoiceTranslate;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

/**
 * Ovoz bilan yozish: gapirasiz — matn yozish maydoniga tushadi (xohlasangiz boshqa tilga tarjima qilinib).
 * Qurilmadagi Android nutqni tanish xizmati ishlatiladi, hech narsa yuborilmaydi — matnni tekshirib o'zingiz yuborasiz.
 */
public class MgVoiceTyping {

    public static boolean isAvailable(Activity activity) {
        return activity != null && VoiceDictation.hasRecognizer(activity);
    }

    /** Chat menyusidan: tillarni tanlash oynasi, keyin tinglash */
    public static void open(ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) {
            return;
        }
        if (!isAvailable(activity)) {
            BulletinFactory.of(chat).createErrorBulletin("Bu telefonda ovozni tanish xizmati yo'q (Google ilovasini o'rnating)").show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.chats_infotip, "Mikrofonga ruxsat bering va qayta urinib ko'ring").show();
            return;
        }
        try {
            VoiceTranslateSheet sheet = new VoiceTranslateSheet(chat, () -> AndroidUtilities.runOnUIThread(() -> listen(chat), 200));
            chat.showDialog(sheet);
        } catch (Throwable e) {
            listen(chat);
        }
    }

    private static void listen(ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        ChatActivityEnterView enter = chat.getChatActivityEnterView();
        if (activity == null || enter == null) {
            return;
        }

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        RLottieImageView mic = new RLottieImageView(activity);
        mic.setAnimation(R.raw.voip_record_start, 90, 90);
        mic.setAutoRepeat(true);
        mic.playAnimation();
        box.addView(mic, LayoutHelper.createLinear(90, 90, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 4));

        TextView text = new TextView(activity);
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        text.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        text.setGravity(Gravity.CENTER);
        text.setMinHeight(AndroidUtilities.dp(60));
        text.setText("Gapiring… Pauza qilsangiz ham tinglash davom etadi.");
        box.addView(text, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 4, 24, 8));

        final String target = VoiceDictation.getTranslateLang(activity);
        if (!TextUtils.isEmpty(target)) {
            TextView hint = new TextView(activity);
            hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            hint.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            hint.setGravity(Gravity.CENTER);
            hint.setText("Tugagach matn tarjima qilinadi: " + target.toUpperCase());
            box.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 8));
        }

        final boolean[] finishing = {false};
        final VoiceDictation[] ref = new VoiceDictation[1];
        final AlertDialog[] dlg = new AlertDialog[1];

        VoiceDictation dictation = new VoiceDictation(activity, new VoiceDictation.Listener() {
            @Override
            public void onText(CharSequence t) {
                if (t != null && t.length() > 0) {
                    text.setText(t);
                }
            }

            @Override
            public void onListeningChanged(boolean listening) {
                if (!listening) {
                    mic.stopAnimation();
                }
            }

            @Override
            public void onFinished(CharSequence finalText) {
                if (TextUtils.isEmpty(target)) {
                    put(chat, finalText);
                    return;
                }
                BulletinFactory.of(chat).createSimpleBulletin(R.raw.chats_infotip, "Tarjima qilinmoqda…").show();
                VoiceTranslate.INSTANCE.translate(finalText, target, chat.getCurrentAccount(), (result, translated) -> {
                    put(chat, result);
                    if (!translated) {
                        BulletinFactory.of(chat).createErrorBulletin("Tarjima qilinmadi — asl matn qo'yildi").show();
                    }
                });
            }

            @Override
            public void onUnavailable() {
                BulletinFactory.of(chat).createErrorBulletin("Ovozni tanib bo'lmadi. Internet va mikrofon ruxsatini tekshiring").show();
                if (dlg[0] != null) {
                    dlg[0].dismiss();
                }
            }
        });
        ref[0] = dictation;

        AlertDialog.Builder b = new AlertDialog.Builder(activity, chat.getResourceProvider());
        b.setTitle("Ovoz bilan yozish");
        FrameLayout wrap = new FrameLayout(activity);
        wrap.addView(box, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        b.setView(wrap);
        b.setPositiveButton("Tayyor", (d, w) -> {
            finishing[0] = true;
            ref[0].finish();
            AndroidUtilities.runOnUIThread(() -> ref[0].destroy(), 4000);
        });
        b.setNegativeButton("Bekor qilish", null);
        b.setOnDismissListener(d -> {
            if (!finishing[0]) {
                ref[0].stop();
                ref[0].destroy();
            }
        });
        dlg[0] = b.create();
        chat.showDialog(dlg[0]);
        dictation.start("");
    }

    private static void put(ChatActivity chat, CharSequence t) {
        ChatActivityEnterView enter = chat.getChatActivityEnterView();
        if (enter == null || TextUtils.isEmpty(t)) {
            return;
        }
        CharSequence cur = enter.getFieldText();
        String add = t.toString().trim();
        if (cur != null && cur.toString().trim().length() > 0) {
            enter.setFieldText(cur.toString().trim() + " " + add);
        } else {
            enter.setFieldText(add);
        }
        enter.openKeyboard();
    }
}
