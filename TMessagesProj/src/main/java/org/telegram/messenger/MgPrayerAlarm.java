/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

/**
 * Namoz vaqti eslatmasi: har bir namoz kirishidan (yoki tanlangan daqiqa oldin) bitta bildirishnoma.
 * Har safar faqat eng yaqin eslatma rejalashtiriladi, u ishlagach keyingisi qo'yiladi.
 */
public class MgPrayerAlarm extends BroadcastReceiver {

    public static final String ACTION = "uz.milliygram.PRAYER_ALARM";
    private static final String CHANNEL = "mg_prayer_v1";
    private static final int REQ = 770210;

    public static boolean isEnabled() {
        return MgConfig.getBool("pr_notify", false);
    }

    public static void setEnabled(boolean v) {
        MgConfig.setBool("pr_notify", v);
        schedule(ApplicationLoader.applicationContext);
    }

    public static int getBefore() {
        return MgConfig.getInt("pr_before", 0);
    }

    public static void setBefore(int minutes) {
        MgConfig.setInt("pr_before", minutes);
        schedule(ApplicationLoader.applicationContext);
    }

    /** Qaysi namozlar uchun eslatish (bit maska, standart — hammasi) */
    public static boolean isPrayerEnabled(int i) {
        return (MgConfig.getInt("pr_mask", 0b111101) & (1 << i)) != 0;
    }

    public static void setPrayerEnabled(int i, boolean v) {
        int m = MgConfig.getInt("pr_mask", 0b111101);
        m = v ? (m | (1 << i)) : (m & ~(1 << i));
        MgConfig.setInt("pr_mask", m);
        schedule(ApplicationLoader.applicationContext);
    }

    private static PendingIntent pending(Context ctx, int prayer) {
        Intent i = new Intent(ctx, MgPrayerAlarm.class);
        i.setAction(ACTION);
        i.putExtra("p", prayer);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(ctx, REQ, i, flags);
    }

    public static void schedule(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }
            am.cancel(pending(ctx, 0));
            if (!isEnabled()) {
                return;
            }
            int before = getBefore();
            Calendar now = Calendar.getInstance(MgPrayer.TZ);
            long nowMs = now.getTimeInMillis();
            long best = Long.MAX_VALUE;
            int bestIdx = -1;
            for (int dayAdd = 0; dayAdd < 2 && bestIdx < 0; dayAdd++) {
                Calendar day = (Calendar) now.clone();
                day.add(Calendar.DAY_OF_MONTH, dayAdd);
                int[] t = MgPrayer.times(day, MgPrayer.getPlace());
                for (int i = 0; i < 6; i++) {
                    if (!MgPrayer.isPrayer(i) || !isPrayerEnabled(i)) {
                        continue;
                    }
                    Calendar at = (Calendar) day.clone();
                    at.set(Calendar.HOUR_OF_DAY, 0);
                    at.set(Calendar.MINUTE, 0);
                    at.set(Calendar.SECOND, 0);
                    at.set(Calendar.MILLISECOND, 0);
                    long ms = at.getTimeInMillis() + (t[i] - before) * 60_000L;
                    if (ms > nowMs + 15_000 && ms < best) {
                        best = ms;
                        bestIdx = i;
                    }
                }
            }
            if (bestIdx < 0) {
                return;
            }
            PendingIntent pi = pending(ctx, bestIdx);
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, best, pi);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) {
            return;
        }
        int p = intent.getIntExtra("p", MgPrayer.DHUHR);
        try {
            show(context, p);
        } catch (Throwable e) {
            FileLog.e(e);
        }
        schedule(context);
    }

    private static void show(Context ctx, int p) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Namoz vaqtlari", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Namoz vaqti kirganini eslatish");
                nm.createNotificationChannel(ch);
            }
        }
        int before = getBefore();
        int[] t = MgPrayer.today();
        MgPlaces.Place place = MgPrayer.getPlace();
        String title = before > 0
                ? MgPrayer.NAMES[p] + " namoziga " + before + " daqiqa qoldi"
                : MgPrayer.NAMES[p] + " namozi vaqti kirdi";
        String text = MgPrayer.NAMES[p] + ": " + MgPrayer.hhmm(t[p]) + " · " + place.name;
        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        PendingIntent content = null;
        if (launch != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            content = PendingIntent.getActivity(ctx, REQ, launch, flags);
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("🕌 " + title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true);
        if (content != null) {
            b.setContentIntent(content);
        }
        try {
            NotificationManagerCompat.from(ctx).notify(REQ + p, b.build());
        } catch (SecurityException ignore) {
        }
    }
}
