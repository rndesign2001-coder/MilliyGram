/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * Tarjima qismi Novagram (VipAds LLC, GPL v2) kodidan olingan.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.text.TextUtils;

import org.fenixuz.ui.voice_translate.VoiceTranslateSheet;
import org.fenixuz.utils.VoiceDictation;
import org.fenixuz.utils.VoiceTranslate;
import org.telegram.messenger.R;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;

import java.util.ArrayList;

/**
 * Ovoz bilan yozish — telefonning o'z (Google) nutqni tanish oynasi orqali.
 * Bitta gapni tinglaydi, jim bo'lganda o'zi to'xtaydi: fonda mikrofon qolmaydi, takroriy signal chalinmaydi.
 * Natija yozish maydoniga qo'yiladi (xohlasa tarjima qilinib), yuborishni foydalanuvchi o'zi bosadi.
 */
public class MgVoiceTyping {

    public static final int REQUEST_CODE = 7719;

    private static Intent buildIntent(Activity activity) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        String lang = VoiceDictation.getSpeakLang(activity);
        if (!TextUtils.isEmpty(lang)) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        }
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Gapiring…");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        return intent;
    }

    public static boolean isAvailable(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            return buildIntent(activity).resolveActivity(activity.getPackageManager()) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Chat menyusidan: tillarni tanlash, keyin tizimning ovoz oynasi */
    public static void open(ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) {
            return;
        }
        if (!isAvailable(activity)) {
            BulletinFactory.of(chat).createErrorBulletin("Bu telefonda ovozni tanish xizmati yo'q (Google ilovasini o'rnating)").show();
            return;
        }
        try {
            VoiceTranslateSheet sheet = new VoiceTranslateSheet(chat, () -> launch(chat));
            chat.showDialog(sheet);
        } catch (Throwable e) {
            launch(chat);
        }
    }

    private static void launch(ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) {
            return;
        }
        try {
            chat.startActivityForResult(buildIntent(activity), REQUEST_CODE);
        } catch (Throwable e) {
            BulletinFactory.of(chat).createErrorBulletin("Ovoz oynasini ochib bo'lmadi").show();
        }
    }

    /** ChatActivity.onActivityResultFragment dan chaqiriladi */
    public static boolean onResult(ChatActivity chat, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            return false;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            return true;
        }
        ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (res == null || res.isEmpty() || TextUtils.isEmpty(res.get(0))) {
            BulletinFactory.of(chat).createErrorBulletin("Ovoz tanilmadi, qayta urinib ko'ring").show();
            return true;
        }
        String text = res.get(0);
        Activity activity = chat.getParentActivity();
        String target = activity == null ? "" : VoiceDictation.getTranslateLang(activity);
        if (TextUtils.isEmpty(target)) {
            put(chat, text);
            return true;
        }
        BulletinFactory.of(chat).createSimpleBulletin(R.raw.chats_infotip, "Tarjima qilinmoqda…").show();
        VoiceTranslate.INSTANCE.translate(text, target, chat.getCurrentAccount(), (result, translated) -> {
            put(chat, result);
            if (!translated) {
                BulletinFactory.of(chat).createErrorBulletin("Tarjima qilinmadi — asl matn qo'yildi").show();
            }
        });
        return true;
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
