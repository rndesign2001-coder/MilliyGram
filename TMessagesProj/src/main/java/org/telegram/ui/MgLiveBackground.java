/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgPrayer;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Kun vaqtiga qarab o'zgaradigan chat foni: foydalanuvchi tong, kun, shom va tun uchun
 * o'z suratlarini tanlaydi (masalan, Registon yoki Toshkent Siti). Vaqtlar tanlangan hududning
 * quyosh chiqishi va botishi bo'yicha hisoblanadi.
 */
public class MgLiveBackground {

    public static String[] MODES() {
        return new String[]{org.telegram.messenger.MgLang.t("Avtomatik (quyosh bo'yicha)"), org.telegram.messenger.MgLang.t("Doim tong"), org.telegram.messenger.MgLang.t("Doim kun"), org.telegram.messenger.MgLang.t("Doim shom"), org.telegram.messenger.MgLang.t("Doim tun")};
    }
    public static String[] SLOT_NAMES() {
        return new String[]{org.telegram.messenger.MgLang.t("Tong"), org.telegram.messenger.MgLang.t("Kun"), org.telegram.messenger.MgLang.t("Shom"), org.telegram.messenger.MgLang.t("Tun")};
    }
    public static final int[] DIMS = {0, 10, 20, 30, 45};
    public static final int REQUEST_BASE = 7700;

    private static String loadedKey;
    private static BitmapDrawable drawable;
    private static volatile Boolean enabledCache;

    public static boolean isEnabled() {
        if (enabledCache == null) {
            enabledCache = MgConfig.getBool("live_bg", false);
        }
        return enabledCache && hasAny();
    }

    public static boolean isSwitchOn() {
        return MgConfig.getBool("live_bg", false);
    }

    public static void setEnabled(boolean v) {
        MgConfig.setBool("live_bg", v);
        enabledCache = v;
        onSettingsChanged();
    }

    public static int getMode() {
        return Math.max(0, Math.min(4, MgConfig.getInt("live_mode", 0)));
    }

    public static int getDim() {
        return MgConfig.getInt("live_dim", 0);
    }

    public static void set(String key, int value) {
        MgConfig.setInt(key, value);
        onSettingsChanged();
    }

    public static File slotFile(int slot) {
        return new File(ApplicationLoader.applicationContext.getFilesDir(), "mg_live_" + slot + ".jpg");
    }

    public static boolean hasSlot(int slot) {
        return slotFile(slot).exists();
    }

    private static volatile Boolean hasAnyCache;

    public static boolean hasAny() {
        Boolean c = hasAnyCache;
        if (c != null) {
            return c;
        }
        boolean r = false;
        for (int i = 0; i < 4; i++) {
            if (hasSlot(i)) {
                r = true;
                break;
            }
        }
        hasAnyCache = r;
        return r;
    }

    private static boolean hasAnyRaw() {
        for (int i = 0; i < 4; i++) {
            if (hasSlot(i)) {
                return true;
            }
        }
        return false;
    }

    /** 0 tong, 1 kun, 2 shom, 3 tun */
    public static int currentSlot() {
        int mode = getMode();
        if (mode > 0) {
            return mode - 1;
        }
        try {
            Calendar now = Calendar.getInstance(MgPrayer.TZ);
            int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int[] sun = MgPrayer.sunTimes();
            int sr = sun[0], ss = sun[1];
            if (cur >= sr - 60 && cur < sr + 75) {
                return 0;
            } else if (cur >= sr + 75 && cur < ss - 60) {
                return 1;
            } else if (cur >= ss - 60 && cur < ss + 50) {
                return 2;
            }
            return 3;
        } catch (Throwable e) {
            return 1;
        }
    }

    /** Joriy vaqt uchun surat bo'lmasa, eng yaqin mavjudi */
    private static int effectiveSlot() {
        int s = currentSlot();
        int[][] order = {{0, 1, 2, 3}, {1, 0, 2, 3}, {2, 3, 1, 0}, {3, 2, 0, 1}};
        for (int c : order[s]) {
            if (hasSlot(c)) {
                return c;
            }
        }
        return -1;
    }

    private static String computeKey() {
        int s = effectiveSlot();
        return s + "_" + getDim() + "_" + (s >= 0 ? slotFile(s).lastModified() : 0);
    }

    /** Theme.getCachedWallpaperNonBlocking() dan chaqiriladi; o'chiq bo'lsa null */
    public static synchronized Drawable getDrawable() {
        if (!isEnabled()) {
            return null;
        }
        int slot = effectiveSlot();
        if (slot < 0) {
            return null;
        }
        String key = computeKey();
        if (drawable != null && key.equals(loadedKey)) {
            return drawable;
        }
        boolean changed = loadedKey != null && !key.equals(loadedKey);
        BitmapDrawable d = load(slot, getDim());
        if (d == null) {
            return drawable;
        }
        drawable = d;
        loadedKey = key;
        if (changed) {
            final Drawable fd = d;
            AndroidUtilities.runOnUIThread(() -> {
                Theme.applyChatServiceMessageColor(null, fd);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewWallpapper);
            });
        }
        return drawable;
    }

    public static BitmapDrawable load(int slot, int dim) {
        try {
            File f = slotFile(slot);
            if (!f.exists()) {
                return null;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            o.inMutable = true;
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            if (b == null) {
                return null;
            }
            if (dim > 0) {
                if (!b.isMutable()) {
                    b = b.copy(Bitmap.Config.ARGB_8888, true);
                }
                new Canvas(b).drawColor(Color.argb(dim * 255 / 100, 0, 0, 0));
            }
            return new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), b);
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public static Bitmap thumb(int slot) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;
            return BitmapFactory.decodeFile(slotFile(slot).getAbsolutePath(), o);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Galereyadan tanlangan suratni ekran o'lchamiga keltirib saqlaydi (asl sifatda, JPEG 92%) */
    public static boolean importImage(Context ctx, Uri uri, int slot) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, bounds);
            }
            int maxSide = Math.max(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            maxSide = Math.max(1280, Math.min(maxSide, 2560));
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) {
                sample *= 2;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            Bitmap b;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                b = BitmapFactory.decodeStream(is, null, o);
            }
            if (b == null) {
                return false;
            }
            // kamerada olingan suratlar EXIF bo'yicha buriladi (aks holda fon yonboshlab chiqadi)
            int rotation = 0;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    rotation = AndroidUtilities.getImageOrientation(is).first;
                }
            } catch (Throwable ignore) {
            }
            float scale = Math.min(1f, maxSide / (float) Math.max(b.getWidth(), b.getHeight()));
            if (scale < 1f || rotation != 0) {
                android.graphics.Matrix m = new android.graphics.Matrix();
                m.postScale(scale, scale);
                m.postRotate(rotation);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            }
            try (FileOutputStream os = new FileOutputStream(slotFile(slot))) {
                b.compress(Bitmap.CompressFormat.JPEG, 92, os);
            }
            onSettingsChanged();
            return true;
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    public static void removeImage(int slot) {
        //noinspection ResultOfMethodCallIgnored
        slotFile(slot).delete();
        onSettingsChanged();
    }

    /** Vaqt oralig'i almashgan bo'lsa fonni yangilaydi */
    public static void check() {
        if (!isEnabled()) {
            return;
        }
        if (!computeKey().equals(loadedKey)) {
            org.telegram.messenger.Utilities.globalQueue.postRunnable(MgLiveBackground::getDrawable);
        }
    }

    private static final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            check();
            AndroidUtilities.runOnUIThread(this, 5 * 60_000L);
        }
    };
    private static boolean tickerStarted;

    public static void startTicker() {
        if (tickerStarted) {
            check();
            return;
        }
        tickerStarted = true;
        ticker.run();
    }

    public static void onSettingsChanged() {
        synchronized (MgLiveBackground.class) {
            loadedKey = null;
            drawable = null;
            hasAnyCache = null;
        }
        AndroidUtilities.runOnUIThread(() -> {
            Drawable d = getDrawable();
            if (d != null) {
                Theme.applyChatServiceMessageColor(null, d);
            } else {
                Theme.applyChatServiceMessageColor();
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewWallpapper);
        });
    }
}
