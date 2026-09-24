/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.util.SparseIntArray;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeColors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * "Dizayn" bo'limi: har bir ekran uchun foydalanuvchi ranglari (kunduzgi va tungi rejim alohida).
 * Ranglar Theme.getColor() ichida ustun turadi, shuning uchun istalgan mavzu bilan ishlaydi.
 */
public class MgDesign {

    public static final String FILE_EXT = ".mgtheme";

    /** Bitta sozlanadigan rang */
    public static class Item {
        public final String title;
        public final int[] keys;

        public Item(String title, int... keys) {
            this.title = title;
            this.keys = keys;
        }
    }

    public static class Screen {
        public final String title;
        public final Item[] items;

        public Screen(String title, Item... items) {
            this.title = title;
            this.items = items;
        }
    }

    /** "Mavzu rangi" — asosiy urg'u rangi ishlatiladigan joylar */
    public static final Item ACCENT = new Item("Mavzu rangi",
            Theme.key_chats_actionBackground, Theme.key_windowBackgroundWhiteBlueHeader, Theme.key_switchTrackChecked,
            Theme.key_chats_unreadCounter, Theme.key_chat_messagePanelSend, Theme.key_dialogButton, Theme.key_dialogTextBlue,
            Theme.key_featuredStickers_addButton, Theme.key_windowBackgroundWhiteBlueText4, Theme.key_windowBackgroundWhiteBlueButton,
            Theme.key_windowBackgroundWhiteBlueText, Theme.key_chats_tabUnreadActiveBackground, Theme.key_actionBarTabActiveText,
            Theme.key_actionBarTabLine, Theme.key_checkboxSquareBackground, Theme.key_radioBackgroundChecked,
            Theme.key_windowBackgroundWhiteValueText, Theme.key_chats_onlineCircle);

    public static final Screen[] SCREENS = {
            new Screen("Asosiy ekran",
                    new Item("Sarlavha foni", Theme.key_actionBarDefault),
                    new Item("Sarlavha matni", Theme.key_actionBarDefaultTitle),
                    new Item("Sarlavha ikonkalari", Theme.key_actionBarDefaultIcon),
                    new Item("Chatlar ro'yxati foni", Theme.key_windowBackgroundWhite),
                    new Item("Chat nomi", Theme.key_chats_name),
                    new Item("Oxirgi xabar matni", Theme.key_chats_message),
                    new Item("Vaqt", Theme.key_chats_date),
                    new Item("O'qilmaganlar belgisi", Theme.key_chats_unreadCounter),
                    new Item("Ovozsiz chatlar belgisi", Theme.key_chats_unreadCounterMuted),
                    new Item("Jild ikonkalari (faol)", Theme.key_actionBarTabActiveText, Theme.key_actionBarTabLine),
                    new Item("Jild ikonkalari (nofaol)", Theme.key_actionBarTabUnactiveText),
                    new Item("Yangi xabar tugmasi", Theme.key_chats_actionBackground),
                    new Item("Mahkamlangan chat foni", Theme.key_chats_pinnedOverlay)),
            new Screen("Chat oynasi",
                    new Item("Mening xabarim foni", Theme.key_chat_outBubble, Theme.key_chat_outBubbleGradient1),
                    new Item("Kelgan xabar foni", Theme.key_chat_inBubble),
                    new Item("Mening xabarim matni", Theme.key_chat_messageTextOut),
                    new Item("Kelgan xabar matni", Theme.key_chat_messageTextIn),
                    new Item("Mening xabarim vaqti", Theme.key_chat_outTimeText),
                    new Item("Kelgan xabar vaqti", Theme.key_chat_inTimeText),
                    new Item("Yozish paneli foni", Theme.key_chat_messagePanelBackground),
                    new Item("Yozish paneli matni", Theme.key_chat_messagePanelText),
                    new Item("Yozish paneli ikonkalari", Theme.key_chat_messagePanelIcons),
                    new Item("Yuborish tugmasi", Theme.key_chat_messagePanelSend),
                    new Item("Pastga tushish tugmasi", Theme.key_chat_goDownButton)),
            new Screen("Kontaktlar ekrani",
                    new Item("Fon", Theme.key_windowBackgroundWhite),
                    new Item("Ism", Theme.key_windowBackgroundWhiteBlackText),
                    new Item("Holat (oxirgi marta)", Theme.key_windowBackgroundWhiteGrayText),
                    new Item("Onlayn holati", Theme.key_windowBackgroundWhiteBlueText),
                    new Item("Onlayn doirasi", Theme.key_chats_onlineCircle)),
            new Screen("Pastki menyu",
                    new Item("Faol bo'lim", Theme.key_featuredStickers_addButton),
                    new Item("Nofaol belgilar", Theme.key_chats_tabUnreadUnactiveBackground),
                    new Item("Yon menyu foni", Theme.key_chats_menuBackground),
                    new Item("Yon menyu matni", Theme.key_chats_menuItemText),
                    new Item("Yon menyu ikonkalari", Theme.key_chats_menuItemIcon)),
            new Screen("Profil",
                    new Item("Profil sarlavhasi foni", Theme.key_avatar_backgroundActionBarBlue),
                    new Item("Ism", Theme.key_profile_title),
                    new Item("Holat", Theme.key_profile_status),
                    new Item("Ikonkalar", Theme.key_avatar_actionBarIconBlue)),
            new Screen("Sozlamalar menyusi",
                    new Item("Umumiy fon", Theme.key_windowBackgroundGray),
                    new Item("Bo'limlar foni", Theme.key_windowBackgroundWhite),
                    new Item("Asosiy matn", Theme.key_windowBackgroundWhiteBlackText),
                    new Item("Izoh matni", Theme.key_windowBackgroundWhiteGrayText2),
                    new Item("Bo'lim sarlavhalari", Theme.key_windowBackgroundWhiteBlueHeader),
                    new Item("Ikonkalar", Theme.key_windowBackgroundWhiteGrayIcon),
                    new Item("Qiymatlar", Theme.key_windowBackgroundWhiteValueText),
                    new Item("Yoqish tugmasi", Theme.key_switchTrackChecked)),
    };

    public static final Item DIALOG_BUTTONS = new Item("Dialog sarlavhasi / tugmalarining rangi", Theme.key_dialogButton, Theme.key_dialogTextBlue, Theme.key_dialogTextBlue2);
    public static final Item DIALOG_BG = new Item("Dialog matnining fon rangi", Theme.key_dialogBackground);
    public static final Item DIALOG_TEXT = new Item("Dialog matnining rangi", Theme.key_dialogTextBlack);

    // ---------- Saqlash ----------

    public static boolean isEnabled() {
        return MgConfig.getBool("mg_design_on", true);
    }

    public static boolean isNight() {
        Theme.ThemeInfo t = Theme.getActiveTheme();
        return t != null && t.isDark();
    }

    private static String pref(boolean night) {
        return night ? "mg_design_night" : "mg_design_day";
    }

    public static HashMap<String, Integer> load(boolean night) {
        HashMap<String, Integer> map = new HashMap<>();
        try {
            String json = MgConfig.getString(pref(night), null);
            if (json != null) {
                JSONObject o = new JSONObject(json);
                Iterator<String> it = o.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    map.put(k, (int) o.getLong(k));
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return map;
    }

    private static void save(boolean night, HashMap<String, Integer> map) {
        try {
            JSONObject o = new JSONObject();
            for (HashMap.Entry<String, Integer> e : map.entrySet()) {
                o.put(e.getKey(), e.getValue() & 0xFFFFFFFFL);
            }
            MgConfig.setString(pref(night), map.isEmpty() ? null : o.toString());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static SparseIntArray build(boolean night) {
        HashMap<String, Integer> map = load(night);
        if (map.isEmpty()) {
            return null;
        }
        SparseIntArray arr = new SparseIntArray();
        for (HashMap.Entry<String, Integer> e : map.entrySet()) {
            int key = ThemeColors.stringKeyToInt(e.getKey());
            if (key >= 0) {
                arr.put(key, e.getValue());
            }
        }
        return arr.size() == 0 ? null : arr;
    }

    /** Saqlangan ranglarni Theme'ga yuklaydi (getColor ularni ishlatadi) */
    public static void reloadIntoTheme() {
        if (ApplicationLoader.applicationContext == null) {
            throw new IllegalStateException("no context");
        }
        if (!isEnabled()) {
            Theme.mgDesignDay = null;
            Theme.mgDesignNight = null;
            return;
        }
        Theme.mgDesignDay = build(false);
        Theme.mgDesignNight = build(true);
    }

    /** Joriy rejimdagi foydalanuvchi rangi (yo'q bo'lsa null) */
    public static Integer getCustom(Item item) {
        HashMap<String, Integer> map = load(isNight());
        String name = ThemeColors.getStringName(item.keys[0]);
        return name == null ? null : map.get(name);
    }

    /** Hozir ekranda ko'rinayotgan rang */
    public static int getEffective(Item item) {
        return Theme.getColor(item.keys[0]);
    }

    public static void set(Item item, Integer color) {
        boolean night = isNight();
        HashMap<String, Integer> map = load(night);
        for (int key : item.keys) {
            String name = ThemeColors.getStringName(key);
            if (name == null) {
                continue;
            }
            if (color == null) {
                map.remove(name);
            } else {
                map.put(name, color);
            }
        }
        save(night, map);
    }

    public static void resetCurrent() {
        save(isNight(), new HashMap<>());
    }

    public static int customCount() {
        return load(isNight()).size();
    }

    /** O'zgarishni darhol butun ilovaga qo'llash */
    public static void applyNow(BaseFragment fragment) {
        try {
            reloadIntoTheme();
        } catch (Throwable ignore) {
        }
        Theme.refreshThemeColors();
        AndroidUtilities.runOnUIThread(() -> {
            if (fragment != null && fragment.getParentLayout() != null) {
                fragment.getParentLayout().rebuildAllFragmentViews(true, true);
            }
        }, 150);
    }

    // ---------- Fayl sifatida saqlash / qo'llash ----------

    public static File getThemesDir() {
        File base = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (base == null) {
            base = ApplicationLoader.getFilesDirFixed();
        }
        File dir = new File(base, "MilliyGram/Themes");
        dir.mkdirs();
        return dir;
    }

    /** @return saqlangan fayl yoki null */
    public static File exportToFile() {
        try {
            JSONObject root = new JSONObject();
            root.put("format", "milliygram-theme");
            root.put("version", 1);
            root.put("enabled", isEnabled());
            for (int n = 0; n < 2; n++) {
                boolean night = n == 1;
                JSONObject o = new JSONObject();
                for (HashMap.Entry<String, Integer> e : load(night).entrySet()) {
                    o.put(e.getKey(), String.format(Locale.US, "#%08X", e.getValue()));
                }
                root.put(night ? "night" : "day", o);
            }
            String name = "MilliyGram_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + FILE_EXT;
            File file = new File(getThemesDir(), name);
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                w.write(root.toString(2));
            }
            return file;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public static ArrayList<File> listThemeFiles() {
        ArrayList<File> list = new ArrayList<>();
        File[] files = getThemesDir().listFiles();
        if (files != null) {
            for (File f : files) {
                String n = f.getName().toLowerCase(Locale.US);
                if (f.isFile() && (n.endsWith(FILE_EXT) || n.endsWith(".json") || n.endsWith(".xml"))) {
                    list.add(f);
                }
            }
        }
        java.util.Collections.sort(list, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return list;
    }

    /** @return qo'llangan ranglar soni yoki -1 (xato) */
    public static int importFromFile(File file) {
        try {
            StringBuilder sb = new StringBuilder();
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
            }
            String text = sb.toString().trim();
            int count = 0;
            if (text.startsWith("{")) {
                JSONObject root = new JSONObject(text);
                for (int m = 0; m < 2; m++) {
                    boolean night = m == 1;
                    JSONObject o = root.optJSONObject(night ? "night" : "day");
                    if (o == null) {
                        continue;
                    }
                    HashMap<String, Integer> map = new HashMap<>();
                    Iterator<String> it = o.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        Integer c = parseColor(o.optString(k, null));
                        if (c != null && ThemeColors.stringKeyToInt(k) >= 0) {
                            map.put(k, c);
                            count++;
                        }
                    }
                    save(night, map);
                }
            } else {
                // Oddiy "kalit=#AARRGGBB" qatorlari (.attheme / xml uslubi) — joriy rejimga
                HashMap<String, Integer> map = load(isNight());
                java.util.regex.Matcher mt = java.util.regex.Pattern.compile("([A-Za-z0-9_]+)\\s*[=:\"'>\\s]+\\s*(#?[0-9A-Fa-f]{6,8}|-?\\d{5,})").matcher(text);
                while (mt.find()) {
                    String k = mt.group(1);
                    Integer c = parseColor(mt.group(2));
                    if (c != null && ThemeColors.stringKeyToInt(k) >= 0) {
                        map.put(k, c);
                        count++;
                    }
                }
                save(isNight(), map);
            }
            MgConfig.setBool("mg_design_on", true);
            return count;
        } catch (Throwable e) {
            FileLog.e(e);
            return -1;
        }
    }

    public static Integer parseColor(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        try {
            if (s.startsWith("#")) {
                s = s.substring(1);
                if (s.length() == 6) {
                    return (int) (0xFF000000L | Long.parseLong(s, 16));
                } else if (s.length() == 8) {
                    return (int) Long.parseLong(s, 16);
                }
                return null;
            }
            if (s.matches("[0-9A-Fa-f]{6}")) {
                return (int) (0xFF000000L | Long.parseLong(s, 16));
            }
            if (s.matches("[0-9A-Fa-f]{8}")) {
                return (int) Long.parseLong(s, 16);
            }
            return (int) Long.parseLong(s);
        } catch (Throwable e) {
            return null;
        }
    }
}
