/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * MilliyGram sozlamalari va yordamchi funksiyalari.
 */
public class MgConfig {

    public static final String PREFS = "milliygram";

    public static final int SIMPLE_MODE_FONT_SIZE = 21;
    public static final int NORMAL_FONT_SIZE = 16;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Umumiy ----------

    public static boolean getBool(String key, boolean def) {
        try {
            return prefs().getBoolean(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void setBool(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static String getString(String key, String def) {
        try {
            return prefs().getString(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void setString(String key, String value) {
        prefs().edit().putString(key, value).apply();
    }

    public static int getInt(String key, int def) {
        try {
            return prefs().getInt(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void setInt(String key, int value) {
        prefs().edit().putInt(key, value).apply();
    }

    // ---------- Oddiy rejim (katta shrift) ----------

    public static boolean isSimpleMode() {
        return getBool("simple_mode", false);
    }

    public static void setSimpleMode(boolean enabled) {
        setBool("simple_mode", enabled);
        int size = enabled ? SIMPLE_MODE_FONT_SIZE : NORMAL_FONT_SIZE;
        SharedConfig.fontSize = size;
        SharedConfig.fontSizeIsDefault = false;
        SharedPreferences main = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        main.edit().putInt("fons_size", size).commit();
        try {
            org.telegram.ui.ActionBar.Theme.createCommonMessageResources();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ---------- Trafik tejash ----------

    public static boolean isTrafficSaver() {
        return getBool("traffic_saver", false);
    }

    public static void setTrafficSaver(boolean enabled) {
        setBool("traffic_saver", enabled);
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            try {
                DownloadController dc = DownloadController.getInstance(a);
                // 0 = kam (low), 1 = o'rtacha (medium)
                int preset = enabled ? 0 : 1;
                dc.currentMobilePreset = preset;
                SharedPreferences.Editor editor = MessagesController.getMainSettings(a).edit();
                editor.putInt("currentMobilePreset", preset);
                editor.commit();
                dc.checkAutodownloadSettings();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    // ---------- Fokus rejimi ----------

    public static boolean isFocusEnabled() {
        return getBool("focus_enabled", false);
    }

    /** Daqiqalarda, 00:00 dan boshlab. Standart: 22:00 - 07:00 */
    public static int getFocusStart() {
        return getInt("focus_start", 22 * 60);
    }

    public static int getFocusEnd() {
        return getInt("focus_end", 7 * 60);
    }

    public static boolean isFocusActiveNow() {
        if (!isFocusEnabled()) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int start = getFocusStart();
        int end = getFocusEnd();
        if (start == end) {
            return true;
        }
        if (start < end) {
            return now >= start && now < end;
        }
        return now >= start || now < end;
    }

    public static String formatMinutes(int minutes) {
        return String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    // ---------- Bayramlar ----------

    public static boolean isHolidayDecorEnabled() {
        return getBool("holiday_decor", true);
    }

    /** Bugun bayram bo'lsa tabrik matni, aks holda null */
    public static String getHolidayGreeting() {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        if (month == 1 && day == 1) {
            return "Yangi yil muborak! 🎄";
        } else if (month == 1 && day == 14) {
            return "Vatan himoyachilari kuni 🎖";
        } else if (month == 3 && day == 8) {
            return "8-mart muborak! 🌷";
        } else if (month == 3 && (day >= 20 && day <= 22)) {
            return "Navro'z muborak! 🌱";
        } else if (month == 5 && day == 9) {
            return "Xotira va qadrlash kuni 🕊";
        } else if (month == 9 && day == 1) {
            return "Mustaqillik kuni muborak! 🇺🇿";
        } else if (month == 10 && day == 1) {
            return "Ustoz va murabbiylar kuni 📚";
        } else if (month == 10 && day == 21) {
            return "O'zbek tili bayrami 📖";
        } else if (month == 12 && day == 8) {
            return "Konstitutsiya kuni 📜";
        } else if (month == 12 && day == 31) {
            return "Yangi yil arafasi 🎆";
        }
        return null;
    }

    public static CharSequence getMainTitle() {
        String title = "MilliyGram";
        if (isHolidayDecorEnabled()) {
            String greeting = getHolidayGreeting();
            if (greeting != null) {
                return title + " · " + greeting;
            }
        }
        return title;
    }

    // ---------- Jildlar ----------

    public static boolean isFolderIconTabs() {
        return getBool("folder_icon_tabs", true);
    }

    // ---------- Chatlarni PIN bilan qulflash ----------

    private static String lockedKey(int account) {
        return "locked_dialogs_" + account;
    }

    public static boolean hasPin() {
        return prefs().contains("chat_pin_hash");
    }

    public static void setPin(String pin) {
        prefs().edit().putString("chat_pin_hash", hash(pin)).apply();
    }

    public static boolean checkPin(String pin) {
        String h = prefs().getString("chat_pin_hash", null);
        return h != null && h.equals(hash(pin));
    }

    public static void removePinAndLocks() {
        SharedPreferences.Editor editor = prefs().edit();
        editor.remove("chat_pin_hash");
        editor.remove("lock_type");
        editor.remove("chat_pin_len");
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            editor.remove(lockedKey(a));
        }
        editor.apply();
    }

    public static Set<String> getLockedDialogs(int account) {
        try {
            return new HashSet<>(prefs().getStringSet(lockedKey(account), new HashSet<>()));
        } catch (Throwable e) {
            return new HashSet<>();
        }
    }

    public static int getLockedCount() {
        int count = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            count += getLockedDialogs(a).size();
        }
        return count;
    }

    public static boolean isDialogLocked(int account, long dialogId) {
        if (dialogId == 0 || !hasPin()) {
            return false;
        }
        return getLockedDialogs(account).contains(String.valueOf(dialogId));
    }

    public static void setDialogLocked(int account, long dialogId, boolean locked) {
        Set<String> set = getLockedDialogs(account);
        if (locked) {
            set.add(String.valueOf(dialogId));
        } else {
            set.remove(String.valueOf(dialogId));
        }
        prefs().edit().putStringSet(lockedKey(account), set).apply();
    }

    private static String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(("milliygram:" + pin).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable e) {
            return pin;
        }
    }

    // ---------- Qulf turi (PIN / grafik kalit) ----------

    public static final String LOCK_PIN = "pin";
    public static final String LOCK_PATTERN = "pattern";
    public static final String PATTERN_PREFIX = "pat:";

    public static String getLockType() {
        try {
            String t = prefs().getString("lock_type", LOCK_PIN);
            return LOCK_PATTERN.equals(t) ? LOCK_PATTERN : LOCK_PIN;
        } catch (Throwable e) {
            return LOCK_PIN;
        }
    }

    /** Yangi qulf kodini saqlaydi (PIN yoki grafik kalit ketma-ketligi) */
    public static void setLock(String type, String secret) {
        boolean pattern = LOCK_PATTERN.equals(type);
        setPin(pattern ? PATTERN_PREFIX + secret : secret);
        prefs().edit()
                .putString("lock_type", pattern ? LOCK_PATTERN : LOCK_PIN)
                .putInt("chat_pin_len", pattern ? 0 : secret.length())
                .apply();
    }

    public static int getPinLength() {
        return getInt("chat_pin_len", 0);
    }

    public static void setPinLength(int len) {
        setInt("chat_pin_len", len);
    }

    public static boolean isFingerprintEnabled() {
        return getBool("lock_fingerprint", true);
    }

    public static boolean isLockVibrate() {
        return getBool("lock_vibrate", true);
    }

    public static boolean isPatternInvisible() {
        return getBool("pattern_invisible", false);
    }

    /** Yashirin bo'limga parolsiz kirish */
    public static boolean isHiddenNoPin() {
        return getBool("hidden_no_pin", false);
    }

    public static boolean isHiddenInSettings() {
        return getBool("hidden_in_settings", true);
    }

    // ---------- Akkauntlarni yashirish ----------

    public static boolean isAccountHidden(int account) {
        return getBool("hidden_account_" + account, false);
    }

    public static void setAccountHidden(int account, boolean hidden) {
        setBool("hidden_account_" + account, hidden);
    }

    public static boolean isHiddenAccountNotify() {
        return getBool("hidden_account_notify", false);
    }

    // ---------- Yolg'on ism ----------

    /** O'z profilingiz uchun faqat shu qurilmada ko'rinadigan boshqa ism (bo'sh bo'lsa o'chirilgan) */
    private static volatile String fakeNameCache;

    public static String getFakeName() {
        String cached = fakeNameCache;
        if (cached != null) {
            return cached;
        }
        String n = "";
        try {
            if (ApplicationLoader.applicationContext != null) {
                n = prefs().getString("fake_name", "");
                n = n == null ? "" : n.trim();
                fakeNameCache = n;
            }
        } catch (Throwable ignore) {
        }
        return n;
    }

    public static void setFakeName(String name) {
        String n = name == null ? "" : name.trim();
        prefs().edit().putString("fake_name", n).apply();
        fakeNameCache = n;
    }

    // ---------- Yashirin bo'lim ----------

    private static final Object hiddenSync = new Object();
    private static final java.util.HashMap<Integer, HashSet<Long>> hiddenCache = new java.util.HashMap<>();

    private static String hiddenKey(int account) {
        return "hidden_dialogs_" + account;
    }

    public static HashSet<Long> getHiddenDialogs(int account) {
        synchronized (hiddenSync) {
            HashSet<Long> cached = hiddenCache.get(account);
            if (cached != null) {
                return cached;
            }
            HashSet<Long> result = new HashSet<>();
            try {
                for (String s : prefs().getStringSet(hiddenKey(account), new HashSet<>())) {
                    try {
                        result.add(Long.parseLong(s));
                    } catch (NumberFormatException ignore) {
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            hiddenCache.put(account, result);
            return result;
        }
    }

    public static boolean isDialogHidden(int account, long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        HashSet<Long> set = getHiddenDialogs(account);
        return !set.isEmpty() && set.contains(dialogId);
    }

    public static void setDialogHidden(int account, long dialogId, boolean hidden) {
        synchronized (hiddenSync) {
            HashSet<Long> set = new HashSet<>(getHiddenDialogs(account));
            if (hidden) {
                set.add(dialogId);
            } else {
                set.remove(dialogId);
            }
            HashSet<String> strings = new HashSet<>();
            for (Long id : set) {
                strings.add(String.valueOf(id));
            }
            prefs().edit().putStringSet(hiddenKey(account), strings).apply();
            hiddenCache.put(account, set);
        }
    }

    public static boolean isHiddenNotifyEnabled() {
        return getBool("hidden_notify", false);
    }

    /** Ro'yxatdan yashirin chatlarni olib tashlaydi (asl ro'yxat o'zgarmaydi) */
    public static java.util.ArrayList<org.telegram.tgnet.TLRPC.Dialog> filterHidden(int account, java.util.ArrayList<org.telegram.tgnet.TLRPC.Dialog> dialogs) {
        if (dialogs == null) {
            return null;
        }
        HashSet<Long> hidden = getHiddenDialogs(account);
        if (hidden.isEmpty()) {
            return dialogs;
        }
        boolean found = false;
        for (int i = 0, n = dialogs.size(); i < n; i++) {
            org.telegram.tgnet.TLRPC.Dialog d = dialogs.get(i);
            if (d != null && hidden.contains(d.id)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return dialogs;
        }
        java.util.ArrayList<org.telegram.tgnet.TLRPC.Dialog> result = new java.util.ArrayList<>(dialogs.size());
        for (int i = 0, n = dialogs.size(); i < n; i++) {
            org.telegram.tgnet.TLRPC.Dialog d = dialogs.get(i);
            if (d == null || !hidden.contains(d.id)) {
                result.add(d);
            }
        }
        return result;
    }

    // ---------- Sozlamalar zaxirasi ----------

    /** Sozlamalarni JSON matn ko'rinishida (PIN va qulflangan chatlarsiz) */
    public static String exportSettings() {
        try {
            JSONObject json = new JSONObject();
            json.put("app", "MilliyGram");
            json.put("version", 1);
            JSONObject values = new JSONObject();
            for (Map.Entry<String, ?> e : prefs().getAll().entrySet()) {
                String key = e.getKey();
                if (key.startsWith("chat_pin") || key.startsWith("locked_dialogs_") || key.startsWith("hidden_dialogs_") || key.startsWith("hidden_account_") || key.startsWith("ghost_") || key.equals("lock_type") || key.equals("fake_name")) {
                    continue;
                }
                Object v = e.getValue();
                if (v instanceof Boolean || v instanceof Integer || v instanceof String || v instanceof Long || v instanceof Float) {
                    JSONObject item = new JSONObject();
                    item.put("t", v.getClass().getSimpleName());
                    item.put("v", v);
                    values.put(key, item);
                }
            }
            json.put("values", values);
            return json.toString();
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** @return import qilingan sozlamalar soni, xato bo'lsa -1 */
    public static int importSettings(String text) {
        try {
            JSONObject json = new JSONObject(text.trim());
            if (!"MilliyGram".equals(json.optString("app"))) {
                return -1;
            }
            JSONObject values = json.getJSONObject("values");
            SharedPreferences.Editor editor = prefs().edit();
            int count = 0;
            Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith("chat_pin") || key.startsWith("locked_dialogs_") || key.startsWith("hidden_dialogs_") || key.startsWith("hidden_account_") || key.startsWith("ghost_") || key.equals("lock_type") || key.equals("fake_name")) {
                    continue;
                }
                JSONObject item = values.getJSONObject(key);
                String t = item.optString("t");
                switch (t) {
                    case "Boolean":
                        editor.putBoolean(key, item.getBoolean("v"));
                        break;
                    case "Integer":
                        editor.putInt(key, item.getInt("v"));
                        break;
                    case "Long":
                        editor.putLong(key, item.getLong("v"));
                        break;
                    case "Float":
                        editor.putFloat(key, (float) item.getDouble("v"));
                        break;
                    case "String":
                        editor.putString(key, item.getString("v"));
                        break;
                    default:
                        continue;
                }
                count++;
            }
            editor.commit();
            // bog'liq sozlamalarni qo'llash
            setSimpleMode(isSimpleMode());
            setTrafficSaver(isTrafficSaver());
            return count;
        } catch (Throwable e) {
            FileLog.e(e);
            return -1;
        }
    }
}
