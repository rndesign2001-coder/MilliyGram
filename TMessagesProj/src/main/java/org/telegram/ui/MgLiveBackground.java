/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgPrayer;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.Calendar;

/**
 * Jonli chat foni: Registon yoki Toshkent Siti manzarasi kun vaqtiga qarab o'zgaradi —
 * tong, kun, shom, tun. Vaqtlar tanlangan hududning quyosh chiqishi/botishi bo'yicha hisoblanadi.
 */
public class MgLiveBackground {

    public static final String[] SCENES = {"Registon", "Toshkent Siti", "Almashib (kunma-kun)"};
    public static final String[] MODES = {"Avtomatik (quyosh bo'yicha)", "Doim tong", "Doim kun", "Doim shom", "Doim tun"};
    public static final String[] SLOT_NAMES = {"Tong", "Kun", "Shom", "Tun"};
    public static final int[] DIMS = {0, 10, 20, 30, 45};

    private static final int[][] RES = {
            {R.drawable.mg_live_registon_tong, R.drawable.mg_live_registon_kun, R.drawable.mg_live_registon_shom, R.drawable.mg_live_registon_tun},
            {R.drawable.mg_live_city_tong, R.drawable.mg_live_city_kun, R.drawable.mg_live_city_shom, R.drawable.mg_live_city_tun},
    };

    private static String loadedKey;
    private static BitmapDrawable drawable;
    private static volatile boolean enabledCache;
    private static volatile boolean cacheReady;

    public static boolean isEnabled() {
        if (!cacheReady) {
            enabledCache = MgConfig.getBool("live_bg", false);
            cacheReady = true;
        }
        return enabledCache;
    }

    public static void setEnabled(boolean v) {
        MgConfig.setBool("live_bg", v);
        enabledCache = v;
        cacheReady = true;
        onSettingsChanged();
    }

    public static int getScene() {
        return Math.max(0, Math.min(2, MgConfig.getInt("live_scene", 0)));
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

    public static int currentScene() {
        int scene = getScene();
        if (scene == 2) {
            return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % 2;
        }
        return scene;
    }

    private static String computeKey() {
        return currentScene() + "_" + currentSlot() + "_" + getDim();
    }

    /** Theme.getCachedWallpaperNonBlocking() dan chaqiriladi; o'chiq bo'lsa null */
    public static synchronized Drawable getDrawable() {
        if (!isEnabled()) {
            return null;
        }
        String key = computeKey();
        if (drawable != null && key.equals(loadedKey)) {
            return drawable;
        }
        boolean changed = loadedKey != null && !key.equals(loadedKey);
        BitmapDrawable d = load(currentScene(), currentSlot(), getDim());
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

    public static BitmapDrawable load(int scene, int slot, int dim) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            o.inMutable = true;
            Bitmap b = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(), RES[scene][slot], o);
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

    /** Vaqt oralig'i almashgan bo'lsa fonni yangilaydi (ilova ochilganda va har bir necha daqiqada) */
    public static void check() {
        if (!isEnabled()) {
            return;
        }
        String key = computeKey();
        if (!key.equals(loadedKey)) {
            Utils.run(() -> getDrawable());
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

    private static class Utils {
        static void run(Runnable r) {
            org.telegram.messenger.Utilities.globalQueue.postRunnable(r);
        }
    }
}
