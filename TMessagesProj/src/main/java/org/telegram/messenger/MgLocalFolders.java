/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.os.SystemClock;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Lokal (serverga sinxronlanmaydigan) jildlar: O'qilmagan, Guruhlar, Profillar, Kanallar, Botlar,
 * hamda standart holatda yashirin Admin, Mening kanallarim, Mening guruhlarim.
 * Shuningdek har qanday jild uchun ikonka tanlash va jild tabini yashirish.
 */
public class MgLocalFolders {

    public static final int ID_MIN = 900;
    public static final int ID_MAX = 999;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_ADMIN = 1;
    public static final int TYPE_ADMIN_CHANNELS = 2;
    public static final int TYPE_ADMIN_GROUPS = 3;
    public static final int TYPE_CUSTOM = 4; // foydalanuvchi toifasi (faqat tanlangan chatlar)
    public static final int TYPE_STRANGERS = 5; // notanishlar (kontaktda yo'q shaxsiy chatlar)
    public static final int TYPE_MOD_CHANNELS = 6; // admin (lekin egasi emas) kanallar
    public static final int TYPE_MOD_GROUPS = 7;   // admin (lekin egasi emas) guruhlar

    public static final int CUSTOM_MIN = 920;
    public static final int CUSTOM_MAX = 989;

    private static final int EXCL_ARCH = MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;

    // ---------- Ikonkalar ----------

    public static final LinkedHashMap<String, Integer> ICONS = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> ICON_NAMES = new LinkedHashMap<>();

    private static void icon(String key, int res, String name) {
        ICONS.put(key, res);
        ICON_NAMES.put(key, name);
    }

    static {
        icon("all", R.drawable.msg_media, "Barcha chatlar");
        icon("private", R.drawable.msg_contacts, "Profillar");
        icon("groups", R.drawable.msg_groups, "Guruhlar");
        icon("channels", R.drawable.msg_channel, "Kanallar");
        icon("bots", R.drawable.msg_bots, "Botlar");
        icon("fave", R.drawable.msg_fave, "Tanlanganlar");
        icon("admin", R.drawable.msg_admins, "Admin");
        icon("unread", R.drawable.msg_markunread, "O'qilmagan");
        icon("admin_channels", R.drawable.msg_channel_create, "Mening kanallarim");
        icon("admin_groups", R.drawable.msg_groups_create, "Mening guruhlarim");
        icon("mod_channels", R.drawable.msg_channel, "Admin kanallar");
        icon("mod_groups", R.drawable.msg_groups, "Admin guruhlar");
        icon("archive", R.drawable.msg_archive, "Arxiv");
        icon("chat", R.drawable.msg_discussion, "Suhbat");
        icon("category", R.drawable.msg_folders, "Toifa");
        icon("strangers", R.drawable.msg_usersearch, "Notanishlar");
        icon("folder", R.drawable.msg_folders, "Jild");
        icon("home", R.drawable.msg_home, "Uy");
        icon("work", R.drawable.msg_work, "Ish");
        icon("secret", R.drawable.msg2_secret, "Maxfiy");
        icon("gift", R.drawable.msg_gift_premium, "Sovg'a");
        icon("like", R.drawable.msg_input_like, "Yoqtirilgan");
        icon("cat", R.drawable.msg_emoji_cat, "Hayvonlar");
        icon("travel", R.drawable.msg_emoji_travel, "Sayohat");
        icon("flag", R.drawable.msg_emoji_flags, "Bayroq");
        icon("music", R.drawable.msg_filled_data_music, "Musiqa");
        icon("link", R.drawable.msg_link_folder, "Havola");
        icon("location", R.drawable.msg_location, "Joy");
        icon("palette", R.drawable.msg_palette, "Dizayn");
        icon("mention", R.drawable.msg_mention, "Eslatmalar");
        icon("muted", R.drawable.msg_folders_muted, "Ovozsiz");
        icon("read", R.drawable.msg_folders_read, "O'qilgan");
    }

    // ---------- Standart lokal jildlar ----------

    private static class Def {
        final int id, flags, type, pos;
        final String key, name, icon;
        final boolean enabled;

        Def(int id, String key, String name, int flags, int type, String icon, boolean enabled, int pos) {
            this.id = id;
            this.key = key;
            this.name = name;
            this.flags = flags;
            this.type = type;
            this.icon = icon;
            this.enabled = enabled;
            this.pos = pos;
        }
    }

    private static final Def[] DEFS = {
            new Def(903, "private", "Profillar", MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS | EXCL_ARCH, TYPE_NONE, "private", true, 1),
            new Def(902, "groups", "Guruhlar", MessagesController.DIALOG_FILTER_FLAG_GROUPS | EXCL_ARCH, TYPE_NONE, "groups", true, 2),
            new Def(904, "channels", "Kanallar", MessagesController.DIALOG_FILTER_FLAG_CHANNELS | EXCL_ARCH, TYPE_NONE, "channels", true, 3),
            new Def(905, "bots", "Botlar", MessagesController.DIALOG_FILTER_FLAG_BOTS | EXCL_ARCH, TYPE_NONE, "bots", true, 4),
            new Def(901, "unread", "O'qilmagan", MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ | EXCL_ARCH, TYPE_NONE, "unread", true, 5),
            new Def(906, "admin", "Admin", MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS, TYPE_ADMIN, "admin", false, 6),
            new Def(907, "admin_channels", "Mening kanallarim", MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS, TYPE_ADMIN_CHANNELS, "admin_channels", false, 7),
            new Def(908, "admin_groups", "Mening guruhlarim", MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS, TYPE_ADMIN_GROUPS, "admin_groups", false, 8),
            new Def(910, "mod_channels", "Admin kanallar", MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS, TYPE_MOD_CHANNELS, "mod_channels", false, 10),
            new Def(911, "mod_groups", "Admin guruhlar", MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS, TYPE_MOD_GROUPS, "mod_groups", false, 11),
            new Def(909, "strangers", "Notanishlar", MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS, TYPE_STRANGERS, "strangers", false, 9),
    };

    public static class Entry {
        public int id, flags, type, pos;
        public String key, name, icon;
        public boolean enabled;
        public ArrayList<Long> always = new ArrayList<>();
        public ArrayList<Long> never = new ArrayList<>();
    }

    public static boolean isLocal(int filterId) {
        return filterId >= ID_MIN && filterId <= ID_MAX;
    }

    public static boolean isLocal(MessagesController.DialogFilter filter) {
        return filter != null && isLocal(filter.id);
    }

    public static boolean isEnabledFeature() {
        return MgConfig.getBool("local_folders", true);
    }

    private static String prefKey(int account) {
        return "mg_local_folders_" + account;
    }

    private static JSONArray longs(ArrayList<Long> list) {
        JSONArray a = new JSONArray();
        for (Long l : list) {
            a.put(l);
        }
        return a;
    }

    private static void readLongs(JSONArray a, ArrayList<Long> out) {
        if (a == null) {
            return;
        }
        for (int i = 0; i < a.length(); i++) {
            out.add(a.optLong(i));
        }
    }

    /** Hisobning lokal jildlari (saqlanganlari + yetishmayotgan standartlari) */
    public static ArrayList<Entry> load(int account) {
        HashMap<Integer, Entry> saved = new HashMap<>();
        try {
            String json = MgConfig.getString(prefKey(account), null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Entry e = new Entry();
                    e.id = o.getInt("id");
                    e.key = o.optString("key", "");
                    e.name = o.optString("name", "");
                    e.flags = o.optInt("flags", 0);
                    e.type = o.optInt("type", 0);
                    e.pos = o.optInt("pos", 99);
                    e.icon = o.optString("icon", "folder");
                    e.enabled = o.optBoolean("enabled", false);
                    readLongs(o.optJSONArray("always"), e.always);
                    readLongs(o.optJSONArray("never"), e.never);
                    saved.put(e.id, e);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        // v2 migratsiyasi: yangi tartib va Plus uslubidagi ikonkalar
        final boolean migrate = !MgConfig.getBool("mg_lf_v2_" + account, false);
        if (migrate) {
            for (Def d : DEFS) {
                Entry e = saved.get(d.id);
                if (e != null) {
                    e.pos = d.pos;
                    e.icon = d.icon;
                }
                MgConfig.setString("folder_icon_" + account + "_" + d.id, null);
            }
        }
        ArrayList<Entry> result = new ArrayList<>();
        for (Def d : DEFS) {
            Entry e = saved.get(d.id);
            if (e == null) {
                e = new Entry();
                e.id = d.id;
                e.key = d.key;
                e.name = d.name;
                e.flags = d.flags;
                e.icon = d.icon;
                e.enabled = d.enabled;
                e.pos = d.pos;
            }
            e.type = d.type;
            result.add(e);
        }
        // Foydalanuvchi toifalari
        for (Entry e : saved.values()) {
            if (e.id >= CUSTOM_MIN && e.id <= CUSTOM_MAX) {
                e.type = TYPE_CUSTOM;
                e.flags = 0;
                result.add(e);
            }
        }
        Collections.sort(result, (a, b) -> Integer.compare(a.pos, b.pos));
        if (migrate) {
            MgConfig.setBool("mg_lf_v2_" + account, true);
            if (!saved.isEmpty()) {
                save(account, result);
            }
        }
        return result;
    }

    public static void save(int account, ArrayList<Entry> entries) {
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("key", e.key);
                o.put("name", e.name);
                o.put("flags", e.flags);
                o.put("type", e.type);
                o.put("pos", e.pos);
                o.put("icon", e.icon);
                o.put("enabled", e.enabled);
                o.put("always", longs(e.always));
                o.put("never", longs(e.never));
                arr.put(o);
            }
            MgConfig.setString(prefKey(account), arr.toString());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static MessagesController.DialogFilter toFilter(Entry e) {
        MessagesController.DialogFilter f = new MessagesController.DialogFilter();
        f.id = e.id;
        f.name = e.name;
        f.flags = e.flags;
        f.mgLocalType = e.type;
        f.localId = 100000 + e.id; // barqaror identifikator (tab tanlovi saqlanishi uchun)
        f.alwaysShow.addAll(e.always);
        f.neverShow.addAll(e.never);
        return f;
    }

    /** Server jildlari yuklangandan keyin lokal jildlarni ro'yxatga qo'shadi (UI thread) */
    public static void inject(MessagesController mc) {
        ArrayList<MessagesController.DialogFilter> list = mc.dialogFilters;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (isLocal(list.get(i))) {
                mc.dialogFiltersById.remove(list.get(i).id);
                list.remove(i);
            }
        }
        if (!isEnabledFeature()) {
            return;
        }
        ArrayList<Entry> entries = load(mc.currentAccount);
        boolean any = false;
        for (Entry e : entries) {
            if (e.enabled) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        boolean hasDefault = false;
        for (MessagesController.DialogFilter f : list) {
            if (f.isDefault()) {
                hasDefault = true;
                break;
            }
        }
        if (!hasDefault) {
            MessagesController.DialogFilter def = new MessagesController.DialogFilter();
            def.id = 0;
            def.name = "";
            def.order = -1;
            list.add(0, def);
            mc.dialogFiltersById.put(0, def);
        }
        for (Entry e : entries) {
            if (!e.enabled) {
                continue;
            }
            MessagesController.DialogFilter f = toFilter(e);
            int index = Math.max(0, Math.min(e.pos, list.size()));
            list.add(index, f);
            mc.dialogFiltersById.put(f.id, f);
        }
    }

    /** Xotiradagi lokal jildlar holatini saqlaydi (tartib, nom, turlar, chatlar) */
    public static void persist(int account) {
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<Entry> entries = load(account);
        HashMap<Integer, Entry> byId = new HashMap<>();
        for (Entry e : entries) {
            byId.put(e.id, e);
            e.enabled = false;
        }
        ArrayList<MessagesController.DialogFilter> list = mc.dialogFilters;
        for (int i = 0; i < list.size(); i++) {
            MessagesController.DialogFilter f = list.get(i);
            Entry e = byId.get(f.id);
            if (e == null) {
                continue;
            }
            e.enabled = true;
            e.pos = i;
            e.name = f.name;
            e.flags = f.flags;
            e.always.clear();
            e.always.addAll(f.alwaysShow);
            e.never.clear();
            e.never.addAll(f.neverShow);
        }
        save(account, entries);
    }

    private static void persistSoon(int account) {
        AndroidUtilities.runOnUIThread(() -> persist(account), 400);
    }

    /** Lokal jildni yoqish / o'chirish */
    public static void setEnabled(int account, int id, boolean enabled) {
        ArrayList<Entry> entries = load(account);
        MessagesController mc = MessagesController.getInstance(account);
        for (Entry e : entries) {
            if (e.id == id) {
                e.enabled = enabled;
                if (enabled) {
                    e.pos = Math.max(1, mc.dialogFilters.size());
                }
            }
        }
        // xotiradagi tartibni saqlab qolish
        for (int i = 0; i < mc.dialogFilters.size(); i++) {
            MessagesController.DialogFilter f = mc.dialogFilters.get(i);
            for (Entry e : entries) {
                if (e.id == f.id && e.id != id) {
                    e.pos = i;
                }
            }
        }
        save(account, entries);
        inject(mc);
        mc.lockFiltersInternal();
        mc.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    public static void renameLocal(int account, int id, String name) {
        MessagesController mc = MessagesController.getInstance(account);
        MessagesController.DialogFilter f = mc.dialogFiltersById.get(id);
        if (f != null) {
            f.name = name;
            persist(account);
        } else {
            ArrayList<Entry> entries = load(account);
            for (Entry e : entries) {
                if (e.id == id) {
                    e.name = name;
                }
            }
            save(account, entries);
        }
        mc.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    // ---------- Toifalar (foydalanuvchi yaratgan lokal jildlar) ----------

    public static ArrayList<Entry> getCategories(int account) {
        ArrayList<Entry> list = new ArrayList<>();
        for (Entry e : load(account)) {
            if (e.type == TYPE_CUSTOM) {
                list.add(e);
            }
        }
        return list;
    }

    private static void applyAndNotify(int account) {
        MessagesController mc = MessagesController.getInstance(account);
        inject(mc);
        mc.lockFiltersInternal();
        resetUnreadCache();
        mc.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
        for (int a = 0; a < 2; a++) {
            if (mc.selectedDialogFilter[a] != null) {
                MessagesController.DialogFilter f = mc.dialogFiltersById.get(mc.selectedDialogFilter[a].id);
                if (f != null && f != mc.selectedDialogFilter[a]) {
                    mc.selectDialogFilter(f, a);
                }
            }
        }
        mc.sortDialogs(null);
        mc.getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
    }

    /** Yangi toifa yaratadi, id qaytaradi (0 — joy qolmagan) */
    public static int createCategory(int account, String name, ArrayList<Long> dialogs) {
        if (!isEnabledFeature()) {
            MgConfig.setBool("local_folders", true);
        }
        ArrayList<Entry> entries = load(account);
        int id = 0;
        for (int candidate = CUSTOM_MIN; candidate <= CUSTOM_MAX; candidate++) {
            boolean used = false;
            for (Entry e : entries) {
                if (e.id == candidate) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                id = candidate;
                break;
            }
        }
        if (id == 0) {
            return 0;
        }
        MessagesController mc = MessagesController.getInstance(account);
        // xotiradagi tartibni saqlab qolish
        for (int i = 0; i < mc.dialogFilters.size(); i++) {
            for (Entry e : entries) {
                if (e.id == mc.dialogFilters.get(i).id) {
                    e.pos = i;
                }
            }
        }
        Entry e = new Entry();
        e.id = id;
        e.key = "custom";
        e.name = name;
        e.flags = 0;
        e.type = TYPE_CUSTOM;
        e.icon = "category";
        e.enabled = true;
        e.pos = Math.max(1, mc.dialogFilters.size());
        if (dialogs != null) {
            for (Long d : dialogs) {
                if (!e.always.contains(d)) {
                    e.always.add(d);
                }
            }
        }
        entries.add(e);
        save(account, entries);
        applyAndNotify(account);
        return id;
    }

    /** Chatlarni toifaga qo'shadi; qo'shilganlar sonini qaytaradi */
    public static int addToCategory(int account, int id, ArrayList<Long> dialogs) {
        MessagesController mc = MessagesController.getInstance(account);
        persist(account);
        ArrayList<Entry> entries = load(account);
        int added = 0;
        for (Entry e : entries) {
            if (e.id != id) {
                continue;
            }
            for (Long d : dialogs) {
                e.never.remove(d);
                if (!e.always.contains(d)) {
                    e.always.add(d);
                    added++;
                }
            }
            e.enabled = true;
        }
        save(account, entries);
        applyAndNotify(account);
        return added;
    }

    public static void removeFromCategory(int account, int id, ArrayList<Long> dialogs) {
        persist(account);
        ArrayList<Entry> entries = load(account);
        for (Entry e : entries) {
            if (e.id == id) {
                e.always.removeAll(dialogs);
            }
        }
        save(account, entries);
        applyAndNotify(account);
    }

    public static void deleteCategory(int account, int id) {
        persist(account);
        ArrayList<Entry> entries = load(account);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).id == id) {
                entries.remove(i);
            }
        }
        save(account, entries);
        MgConfig.setString("folder_icon_" + account + "_" + id, null);
        applyAndNotify(account);
    }

    // ---------- So'rovlarni ushlash (lokal jildlar serverga yuborilmaydi) ----------

    /** @return true bo'lsa so'rov serverga yuborilmaydi (javob lokal beriladi) */
    public static boolean interceptRequest(int account, TLObject object, RequestDelegate onComplete) {
        try {
            if (object instanceof TLRPC.TL_messages_updateDialogFiltersOrder) {
                TLRPC.TL_messages_updateDialogFiltersOrder req = (TLRPC.TL_messages_updateDialogFiltersOrder) object;
                boolean hadLocal = false;
                for (int i = req.order.size() - 1; i >= 0; i--) {
                    Integer id = req.order.get(i);
                    if (id != null && isLocal(id)) {
                        req.order.remove(i);
                        hadLocal = true;
                    }
                }
                if (hadLocal) {
                    persistSoon(account);
                }
                return false;
            }
            if (object instanceof TLRPC.TL_messages_updateDialogFilter) {
                TLRPC.TL_messages_updateDialogFilter req = (TLRPC.TL_messages_updateDialogFilter) object;
                if (isLocal(req.id)) {
                    persistSoon(account);
                    if (onComplete != null) {
                        AndroidUtilities.runOnUIThread(() -> onComplete.run(new TLRPC.TL_boolTrue(), null));
                    }
                    return true;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return false;
    }

    // ---------- Admin jildlari ----------

    public static boolean includesByType(MessagesController.DialogFilter filter, MessagesController mc, long dialogId) {
        if (filter.mgLocalType == TYPE_STRANGERS) {
            return MgStrangers.belongsInInbox(mc.currentAccount, dialogId);
        }
        if (dialogId >= 0) {
            return false;
        }
        TLRPC.Chat chat = mc.getChat(-dialogId);
        if (chat == null || ChatObject.isNotInChat(chat) || chat.deactivated || chat.migrated_to != null && !(chat.migrated_to instanceof TLRPC.TL_inputChannelEmpty)) {
            return false;
        }
        boolean broadcast = ChatObject.isChannelAndNotMegaGroup(chat);
        boolean owner = chat.creator;
        boolean admin = !owner && chat.admin_rights != null;
        switch (filter.mgLocalType) {
            case TYPE_ADMIN_CHANNELS:
                return owner && broadcast;
            case TYPE_ADMIN_GROUPS:
                return owner && !broadcast;
            case TYPE_MOD_CHANNELS:
                return admin && broadcast;
            case TYPE_MOD_GROUPS:
                return admin && !broadcast;
            case TYPE_ADMIN:
                return owner || admin;
        }
        return false;
    }

    // ---------- O'qilmaganlar soni (lokal jild tablari uchun) ----------

    private static final SparseArray<long[]> unreadCache = new SparseArray<>();

    /** {ovozli o'qilmagan chatlar, ovozsiz o'qilmagan chatlar} — 1.5 soniya keshlanadi */
    public static int[] getUnreadCounts(int account, MessagesController.DialogFilter filter) {
        int key = account * 100000 + (filter.id & 0xFFFF);
        long now = SystemClock.elapsedRealtime();
        long[] cached = unreadCache.get(key);
        if (cached != null && now - cached[0] < 1500) {
            return new int[]{(int) cached[1], (int) cached[2]};
        }
        MessagesController mc = MessagesController.getInstance(account);
        AccountInstance ai = AccountInstance.getInstance(account);
        int count = 0, muted = 0;
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        for (int i = 0, n = all.size(); i < n; i++) {
            TLRPC.Dialog d = all.get(i);
            if (d == null || d.folder_id != 0 && (filter.flags & EXCL_ARCH) != 0) {
                continue;
            }
            if (!(d.unread_count > 0 || d.unread_mark)) {
                continue;
            }
            if (MgConfig.isDialogHidden(account, d.id)) {
                continue;
            }
            try {
                if (!filter.includesDialog(ai, d.id, d)) {
                    continue;
                }
            } catch (Throwable e) {
                continue;
            }
            if (mc.isDialogMuted(d.id, 0)) {
                muted++;
            } else {
                count++;
            }
        }
        unreadCache.put(key, new long[]{now, count, muted});
        return new int[]{count, muted};
    }

    /** Tab hisoblagichi: ovozli bo'lsa ular soni, aks holda ovozsizlar soni */
    public static int getUnreadCount(int account, MessagesController.DialogFilter filter) {
        int[] c = getUnreadCounts(account, filter);
        return c[0] > 0 ? c[0] : c[1];
    }

    public static boolean isMutedOnly(int account, MessagesController.DialogFilter filter) {
        int[] c = getUnreadCounts(account, filter);
        return c[0] == 0 && c[1] > 0;
    }

    public static void resetUnreadCache() {
        unreadCache.clear();
    }

    // ---------- Jild ikonkasi va tabni yashirish (barcha jildlar uchun) ----------

    public static String getIconKey(int account, MessagesController.DialogFilter filter) {
        String custom = MgConfig.getString("folder_icon_" + account + "_" + filter.id, null);
        if (custom != null && ICONS.containsKey(custom)) {
            return custom;
        }
        if (isLocal(filter)) {
            for (Entry e : load(account)) {
                if (e.id == filter.id && ICONS.containsKey(e.icon)) {
                    return e.icon;
                }
            }
        }
        return null;
    }

    public static void setIconKey(int account, int filterId, String key) {
        MgConfig.setString("folder_icon_" + account + "_" + filterId, key);
        MessagesController.getInstance(account).getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    public static boolean isTabHidden(int account, int filterId) {
        return MgConfig.getBool("folder_tab_hidden_" + account + "_" + filterId, false);
    }

    public static void setTabHidden(int account, int filterId, boolean hidden) {
        MgConfig.setBool("folder_tab_hidden_" + account + "_" + filterId, hidden);
        MessagesController.getInstance(account).getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }
}
