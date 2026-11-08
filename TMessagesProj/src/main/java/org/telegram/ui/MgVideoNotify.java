/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;

/**
 * Video ko'rilayotganda bildirishnomada boshqaruv: 10 s orqaga, pauza/davom, 10 s oldinga.
 * Android 13+ da media panelida ham ko'rinadi (MediaSession orqali).
 */
public class MgVideoNotify {

    public interface Controller {
        void toggle();
        void seekTo(long positionMs);
        long position();
        long duration();
        boolean isPlaying();
    }

    private static final int ID = 0x4D47_5644; // "MGVD"
    private static final String ACTION_TOGGLE = "org.milliygram.video.TOGGLE";
    private static final String ACTION_BACK = "org.milliygram.video.BACK";
    private static final String ACTION_FWD = "org.milliygram.video.FWD";

    private static Controller controller;
    private static MediaSession session;
    private static BroadcastReceiver receiver;
    private static String title;
    private static boolean shown;

    public static boolean isEnabled() {
        return MgConfig.getBool("video_notify", true);
    }

    private static Context ctx() {
        return ApplicationLoader.applicationContext;
    }

    /** O'ynash holati o'zgarganda chaqiriladi */
    public static void update(Controller c, String videoTitle) {
        if (!isEnabled() || c == null) {
            return;
        }
        try {
            controller = c;
            title = videoTitle;
            ensureSession();
            ensureReceiver();
            post();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void cancel() {
        controller = null;
        try {
            if (shown) {
                NotificationManager nm = (NotificationManager) ctx().getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(ID);
            }
            shown = false;
            if (session != null) {
                session.setActive(false);
                session.release();
                session = null;
            }
            if (receiver != null) {
                ctx().unregisterReceiver(receiver);
                receiver = null;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void ensureSession() {
        if (session != null) {
            return;
        }
        session = new MediaSession(ctx(), "MilliyGramVideo");
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                AndroidUtilities.runOnUIThread(() -> act(ACTION_TOGGLE, true));
            }

            @Override
            public void onPause() {
                AndroidUtilities.runOnUIThread(() -> act(ACTION_TOGGLE, false));
            }

            @Override
            public void onSeekTo(long pos) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (controller != null) {
                        controller.seekTo(pos);
                        post();
                    }
                });
            }

            @Override
            public void onRewind() {
                AndroidUtilities.runOnUIThread(() -> act(ACTION_BACK, false));
            }

            @Override
            public void onFastForward() {
                AndroidUtilities.runOnUIThread(() -> act(ACTION_FWD, false));
            }

            @Override
            public void onSkipToPrevious() {
                onRewind();
            }

            @Override
            public void onSkipToNext() {
                onFastForward();
            }
        });
        session.setActive(true);
    }

    private static void ensureReceiver() {
        if (receiver != null) {
            return;
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    act(intent.getAction(), null);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_TOGGLE);
        f.addAction(ACTION_BACK);
        f.addAction(ACTION_FWD);
        if (Build.VERSION.SDK_INT >= 33) {
            ctx().registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ctx().registerReceiver(receiver, f);
        }
    }

    /** wantPlay: null — almashtirish, true/false — aniq holat */
    private static void act(String action, Boolean wantPlay) {
        Controller c = controller;
        if (c == null) {
            return;
        }
        try {
            if (ACTION_TOGGLE.equals(action)) {
                if (wantPlay == null || wantPlay != c.isPlaying()) {
                    c.toggle();
                }
            } else if (ACTION_BACK.equals(action)) {
                c.seekTo(Math.max(0, c.position() - 10_000));
            } else if (ACTION_FWD.equals(action)) {
                long d = c.duration();
                c.seekTo(d > 0 ? Math.min(d - 500, c.position() + 10_000) : c.position() + 10_000);
            }
            AndroidUtilities.runOnUIThread(MgVideoNotify::post, 150);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static PendingIntent pi(String action, int req) {
        Intent i = new Intent(action).setPackage(ctx().getPackageName());
        return PendingIntent.getBroadcast(ctx(), req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void post() {
        Controller c = controller;
        if (c == null || session == null) {
            return;
        }
        boolean playing = c.isPlaying();
        long dur = Math.max(0, c.duration());
        String t = title != null && !title.isEmpty() ? title : org.telegram.messenger.MgLang.t("Video");

        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, t)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "MilliyGram")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, dur)
                .build());
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                        | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_REWIND | PlaybackState.ACTION_FAST_FORWARD
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, Math.max(0, c.position()), playing ? 1f : 0f)
                .build());

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationsController.checkOtherNotificationsChannel();
            b = new Notification.Builder(ctx(), NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
        } else {
            b = new Notification.Builder(ctx());
        }
        Intent open = new Intent(ctx(), LaunchActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        b.setSmallIcon(R.drawable.player)
                .setContentTitle(t)
                .setContentText(org.telegram.messenger.MgLang.t("Video"))
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(playing)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(ctx(), 0x4D47, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .addAction(new Notification.Action.Builder(R.drawable.ic_action_previous, org.telegram.messenger.MgLang.t("10 s orqaga"), pi(ACTION_BACK, 1)).build())
                .addAction(new Notification.Action.Builder(playing ? R.drawable.ic_action_pause : R.drawable.ic_action_play, playing ? "Pause" : "Play", pi(ACTION_TOGGLE, 2)).build())
                .addAction(new Notification.Action.Builder(R.drawable.ic_action_next, org.telegram.messenger.MgLang.t("10 s oldinga"), pi(ACTION_FWD, 3)).build())
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
        try {
            NotificationManager nm = (NotificationManager) ctx().getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(ID, b.build());
            shown = true;
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
