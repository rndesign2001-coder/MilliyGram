/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * Tarjima qismi Novagram (VipAds LLC, GPL v2) kodidan olingan.
 */

package org.telegram.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.fenixuz.ui.voice_translate.VoiceTranslateSheet;
import org.fenixuz.utils.VoiceDictation;
import org.fenixuz.utils.VoiceTranslate;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

import java.util.ArrayList;

/**
 * Ovoz bilan yozish.
 * Uzoq diktovka: pauza qilsangiz ham tinglash davom etadi (jimlik ~25 soniyadan oshsa yoki "Tayyor" bosilsa tugaydi).
 * Har bir gap tugaganda tanish qayta boshlanadi, bu paytdagi tizim "dit" signali o'chirib turiladi.
 * Oyna yopilishi bilan mikrofon va tanish to'liq to'xtatiladi — fonda hech narsa qolmaydi.
 * Nutqni tanish xizmati bo'lmasa, tizimning ovoz oynasi ishlatiladi.
 */
public class MgVoiceTyping {

    public static final int REQUEST_CODE = 7719;
    private static final int MAX_SILENT_ROUNDS = 5;       // ketma-ket "hech narsa eshitilmadi" soni
    private static final long MAX_SESSION_MS = 5 * 60_000L;

    private static Intent buildIntent(Activity activity, boolean continuous) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        String lang = VoiceDictation.getSpeakLang(activity);
        if (!TextUtils.isEmpty(lang)) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        }
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // Pauzalarga sabrliroq bo'lish (xizmat qo'llasa)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 8000L);
        intent.putExtra("android.speech.extra.DICTATION_MODE", true);
        if (continuous) {
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName());
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Gapiring…");
        }
        return intent;
    }

    public static boolean isAvailable(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            return SpeechRecognizer.isRecognitionAvailable(activity)
                    || buildIntent(activity, false).resolveActivity(activity.getPackageManager()) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Chat menyusidan: tillarni tanlash, keyin tinglash */
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
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.chats_infotip, "Mikrofonga ruxsat bering va qayta bosing").show();
            return;
        }
        Runnable go = () -> AndroidUtilities.runOnUIThread(() -> {
            if (SpeechRecognizer.isRecognitionAvailable(activity)) {
                new Session(chat).start();
            } else {
                launchSystem(chat);
            }
        }, 150);
        try {
            chat.showDialog(new VoiceTranslateSheet(chat, go));
        } catch (Throwable e) {
            go.run();
        }
    }

    private static void launchSystem(ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) {
            return;
        }
        try {
            chat.startActivityForResult(buildIntent(activity, false), REQUEST_CODE);
        } catch (Throwable e) {
            BulletinFactory.of(chat).createErrorBulletin("Ovoz oynasini ochib bo'lmadi").show();
        }
    }

    /** ChatActivity.onActivityResultFragment dan chaqiriladi (tizim oynasi varianti) */
    public static boolean onResult(ChatActivity chat, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (res != null && !res.isEmpty() && !TextUtils.isEmpty(res.get(0))) {
                deliver(chat, res.get(0));
            }
        }
        return true;
    }

    private static void deliver(ChatActivity chat, String text) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(text.trim())) {
            return;
        }
        Activity activity = chat.getParentActivity();
        String target = activity == null ? "" : VoiceDictation.getTranslateLang(activity);
        if (TextUtils.isEmpty(target)) {
            put(chat, text);
            return;
        }
        BulletinFactory.of(chat).createSimpleBulletin(R.raw.chats_infotip, "Tarjima qilinmoqda…").show();
        VoiceTranslate.INSTANCE.translate(text, target, chat.getCurrentAccount(), (result, translated) -> {
            put(chat, result);
            if (!translated) {
                BulletinFactory.of(chat).createErrorBulletin("Tarjima qilinmadi — asl matn qo'yildi").show();
            }
        });
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

    // ================= Uzluksiz tinglash seansi =================

    private static class Session implements RecognitionListener {
        private final ChatActivity chat;
        private final Activity activity;
        private SpeechRecognizer recognizer;
        private Intent intent;
        private boolean active;
        private boolean delivered;
        private final StringBuilder committed = new StringBuilder();
        private String partial = "";
        private int silentRounds;
        private int errorRounds;
        private long startedAt;
        private AlertDialog dialog;
        private TextView textView, timerView;
        private RLottieImageView mic;
        private boolean mutedMusic, mutedSystem;

        private final Runnable restartRunnable = this::listen;
        private final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!active) {
                    return;
                }
                long s = (System.currentTimeMillis() - startedAt) / 1000;
                if (timerView != null) {
                    timerView.setText(String.format(java.util.Locale.US, "● %d:%02d", s / 60, s % 60));
                }
                if (System.currentTimeMillis() - startedAt > MAX_SESSION_MS) {
                    finish();
                    return;
                }
                AndroidUtilities.runOnUIThread(this, 1000);
            }
        };

        Session(ChatActivity chat) {
            this.chat = chat;
            this.activity = chat.getParentActivity();
        }

        void start() {
            if (activity == null) {
                return;
            }
            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
                recognizer.setRecognitionListener(this);
            } catch (Throwable e) {
                FileLog.e(e);
                launchSystem(chat);
                return;
            }
            intent = buildIntent(activity, true);
            active = true;
            startedAt = System.currentTimeMillis();
            showDialog();
            muteBeeps(true);
            listen();
            timerRunnable.run();
        }

        private void showDialog() {
            LinearLayout box = new LinearLayout(activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            mic = new RLottieImageView(activity);
            mic.setAnimation(R.raw.voip_record_start, 86, 86);
            mic.setAutoRepeat(true);
            mic.playAnimation();
            box.addView(mic, LayoutHelper.createLinear(86, 86, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));
            timerView = new TextView(activity);
            timerView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            timerView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
            timerView.setGravity(Gravity.CENTER);
            box.addView(timerView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 6));
            textView = new TextView(activity);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            textView.setGravity(Gravity.CENTER);
            textView.setMinHeight(AndroidUtilities.dp(56));
            textView.setText("Gapiring… Pauza qilsangiz ham kutaman.\nTugatgach «Tayyor»ni bosing.");
            box.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 22, 0, 22, 8));

            AlertDialog.Builder b = new AlertDialog.Builder(activity, chat.getResourceProvider());
            b.setTitle("Ovoz bilan yozish");
            b.setView(box);
            b.setPositiveButton("Tayyor", (d, w) -> finish());
            b.setNegativeButton("Bekor qilish", (d, w) -> stop());
            b.setOnDismissListener(d -> {
                if (active) {
                    finish();
                }
            });
            dialog = b.create();
            dialog.setCanceledOnTouchOutside(false);
            chat.showDialog(dialog);
        }

        private void listen() {
            if (!active || recognizer == null) {
                return;
            }
            try {
                recognizer.cancel();
                recognizer.startListening(intent);
            } catch (Throwable e) {
                FileLog.e(e);
                if (++errorRounds > 3) {
                    finish();
                } else {
                    AndroidUtilities.runOnUIThread(restartRunnable, 400);
                }
            }
        }

        private void restartSoon(long delay) {
            AndroidUtilities.cancelRunOnUIThread(restartRunnable);
            if (active) {
                AndroidUtilities.runOnUIThread(restartRunnable, delay);
            }
        }

        private void updateText() {
            if (textView == null) {
                return;
            }
            String t = (committed + (partial.isEmpty() ? "" : (committed.length() > 0 ? " " : "") + partial)).trim();
            if (!t.isEmpty()) {
                textView.setText(t);
            }
        }

        private static String best(Bundle b) {
            if (b == null) {
                return "";
            }
            ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            return r == null || r.isEmpty() || r.get(0) == null ? "" : r.get(0).trim();
        }

        /** Foydalanuvchi "Tayyor" bosdi yoki uzoq jimlik: yig'ilgan matnni yozish maydoniga qo'yish */
        void finish() {
            if (!active && delivered) {
                return;
            }
            String text = (committed + (partial.isEmpty() ? "" : " " + partial)).trim();
            end();
            if (!delivered) {
                delivered = true;
                if (!text.isEmpty()) {
                    deliver(chat, text);
                } else {
                    BulletinFactory.of(chat).createSimpleBulletin(R.raw.chats_infotip, "Hech narsa eshitilmadi").show();
                }
            }
        }

        /** Bekor qilish: hech narsa qo'yilmaydi */
        void stop() {
            delivered = true;
            end();
        }

        private void end() {
            active = false;
            AndroidUtilities.cancelRunOnUIThread(restartRunnable);
            AndroidUtilities.cancelRunOnUIThread(timerRunnable);
            if (recognizer != null) {
                try {
                    recognizer.cancel();
                } catch (Throwable ignore) {
                }
                try {
                    recognizer.destroy();
                } catch (Throwable ignore) {
                }
                recognizer = null;
            }
            // tizim signalini qaytarish biroz kechiktiriladi — to'xtash "dit"i eshitilmasin
            AndroidUtilities.runOnUIThread(() -> muteBeeps(false), 600);
            if (mic != null) {
                mic.stopAnimation();
            }
            if (dialog != null) {
                try {
                    dialog.dismiss();
                } catch (Throwable ignore) {
                }
            }
        }

        private void muteBeeps(boolean mute) {
            try {
                AudioManager am = (AudioManager) activity.getSystemService(android.content.Context.AUDIO_SERVICE);
                if (am == null || Build.VERSION.SDK_INT < 23) {
                    return;
                }
                if (mute) {
                    if (!am.isStreamMute(AudioManager.STREAM_MUSIC) && !am.isMusicActive()) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                        mutedMusic = true;
                    }
                    if (!am.isStreamMute(AudioManager.STREAM_SYSTEM)) {
                        am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
                        mutedSystem = true;
                    }
                } else {
                    if (mutedMusic) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                        mutedMusic = false;
                    }
                    if (mutedSystem) {
                        am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
                        mutedSystem = false;
                    }
                }
            } catch (Throwable ignore) {
            }
        }

        // ---- RecognitionListener ----

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
            silentRounds = 0;
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (!active) {
                return;
            }
            String p = best(partialResults);
            if (!p.isEmpty()) {
                partial = p;
                silentRounds = 0;
                updateText();
            }
        }

        @Override
        public void onResults(Bundle results) {
            if (!active) {
                return;
            }
            String r = best(results);
            if (r.isEmpty()) {
                r = partial;
            }
            if (!r.isEmpty()) {
                if (committed.length() > 0) {
                    committed.append(' ');
                }
                committed.append(r);
                silentRounds = 0;
            } else {
                silentRounds++;
            }
            partial = "";
            errorRounds = 0;
            updateText();
            if (silentRounds >= MAX_SILENT_ROUNDS) {
                finish();
            } else {
                restartSoon(80);
            }
        }

        @Override
        public void onError(int error) {
            if (!active) {
                return;
            }
            if (!partial.isEmpty()) {
                // Uzilishdan oldin eshitilgan qism yo'qolmasin
                if (committed.length() > 0) {
                    committed.append(' ');
                }
                committed.append(partial);
                partial = "";
                updateText();
            }
            switch (error) {
                case SpeechRecognizer.ERROR_NO_MATCH:
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    if (++silentRounds >= MAX_SILENT_ROUNDS) {
                        finish();
                    } else {
                        restartSoon(80);
                    }
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                case SpeechRecognizer.ERROR_CLIENT:
                    if (++errorRounds > 6) {
                        finish();
                    } else {
                        restartSoon(350);
                    }
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    BulletinFactory.of(chat).createErrorBulletin("Mikrofonga ruxsat yo'q").show();
                    finish();
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    if (++errorRounds > 2) {
                        BulletinFactory.of(chat).createErrorBulletin("Internet bilan muammo — ovoz tanilmadi").show();
                        finish();
                    } else {
                        restartSoon(800);
                    }
                    break;
                default:
                    if (++errorRounds > 3) {
                        finish();
                    } else {
                        restartSoon(400);
                    }
                    break;
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }
}
