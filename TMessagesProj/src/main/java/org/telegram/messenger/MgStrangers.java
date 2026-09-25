/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * G'oya Novagram (VipAds LLC, GPL v2) "Stranger shield" funksiyasidan olingan va MilliyGram uchun qayta yozilgan.
 */

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notanishlardan himoya: kontaktda bo'lmagan odamlarning shaxsiy chatlari asosiy ro'yxatdan olinib,
 * alohida "Notanishlar" jildiga tushadi va ovozsiz bo'ladi. Har bir akkaunt uchun alohida yoqiladi.
 * "Notanish emas" deb belgilangan chatlar asosiy ro'yxatda qoladi.
 */
public class MgStrangers {

    public static final int FOLDER_ID = 909;

    private static final boolean[] enabled = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    @SuppressWarnings("unchecked")
    private static final Set<Long>[] allowed = new Set[UserConfig.MAX_ACCOUNT_COUNT];
    private static volatile boolean loaded;

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (MgStrangers.class) {
            if (loaded) {
                return;
            }
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                enabled[a] = MgConfig.getBool("stranger_on_" + a, false);
                Set<Long> set = ConcurrentHashMap.newKeySet();
                String raw = MgConfig.getString("stranger_allowed_" + a, "");
                if (raw != null && !raw.isEmpty()) {
                    for (String s : raw.split(",")) {
                        try {
                            set.add(Long.parseLong(s));
                        } catch (Throwable ignore) {
                        }
                    }
                }
                allowed[a] = set;
            }
            loaded = true;
        }
    }

    public static boolean isEnabled(int account) {
        ensureLoaded();
        return account >= 0 && account < enabled.length && enabled[account];
    }

    public static void setEnabled(int account, boolean value) {
        ensureLoaded();
        enabled[account] = value;
        MgConfig.setBool("stranger_on_" + account, value);
        if (value) {
            // Qayta yoqilganda — toza boshlanish
            allowed[account].clear();
            saveAllowed(account);
        }
        if (value && !MgLocalFolders.isEnabledFeature()) {
            MgConfig.setBool("local_folders", true);
        }
        MgLocalFolders.setEnabled(account, FOLDER_ID, value);
        refresh(account);
    }

    /** Jild ro'yxatlarini qayta hisoblash (UI oqimida) */
    public static void refresh(int account) {
        MessagesController mc = MessagesController.getInstance(account);
        MgLocalFolders.resetUnreadCache();
        mc.sortDialogs(null);
        mc.getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        mc.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    public static boolean isNotifyEnabled() {
        return MgConfig.getBool("stranger_notify", false);
    }

    /** Kontaktda yo'q, bot emas, o'zingiz emas, Telegram xizmati emas */
    public static boolean isStranger(TLRPC.User user) {
        return user != null && !user.contact && !user.bot && !user.support
                && !UserObject.isUserSelf(user) && !UserObject.isService(user.id);
    }

    public static boolean belongsInInbox(int account, TLRPC.User user, long dialogId) {
        if (!isEnabled(account) || dialogId <= 0) {
            return false;
        }
        if (!isStranger(user)) {
            return false;
        }
        return !allowed[account].contains(dialogId);
    }

    public static boolean belongsInInbox(int account, long dialogId) {
        if (!isEnabled(account) || dialogId <= 0 || DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        return belongsInInbox(account, MessagesController.getInstance(account).getUser(dialogId), dialogId);
    }

    /** "Notanish emas" — chatni asosiy ro'yxatga qaytarish */
    public static void trust(int account, long dialogId) {
        ensureLoaded();
        if (allowed[account].add(dialogId)) {
            saveAllowed(account);
        }
    }

    public static void untrust(int account, long dialogId) {
        ensureLoaded();
        if (allowed[account].remove(dialogId)) {
            saveAllowed(account);
        }
    }

    public static boolean isTrusted(int account, long dialogId) {
        ensureLoaded();
        return allowed[account].contains(dialogId);
    }

    private static void saveAllowed(int account) {
        StringBuilder sb = new StringBuilder();
        for (Long id : allowed[account]) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        MgConfig.setString("stranger_allowed_" + account, sb.toString());
    }

    /** Asosiy ro'yxatdan notanishlarni olib tashlaydi (hech narsa o'zgarmasa — o'sha ro'yxatning o'zi) */
    public static ArrayList<TLRPC.Dialog> filter(int account, ArrayList<TLRPC.Dialog> list) {
        if (list == null || !isEnabled(account)) {
            return list;
        }
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<TLRPC.Dialog> out = null;
        for (int i = 0; i < list.size(); i++) {
            TLRPC.Dialog d = list.get(i);
            boolean inbox = d != null && d.id > 0 && !DialogObject.isEncryptedDialog(d.id) && belongsInInbox(account, mc.getUser(d.id), d.id);
            if (inbox && out == null) {
                out = new ArrayList<>(list.size());
                for (int j = 0; j < i; j++) {
                    out.add(list.get(j));
                }
            }
            if (!inbox && out != null) {
                out.add(d);
            }
        }
        return out == null ? list : out;
    }

    public static int countInbox(int account) {
        if (!isEnabled(account)) {
            return 0;
        }
        MessagesController mc = MessagesController.getInstance(account);
        int c = 0;
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        for (int i = 0; i < all.size(); i++) {
            TLRPC.Dialog d = all.get(i);
            if (d != null && d.id > 0 && belongsInInbox(account, mc.getUser(d.id), d.id)) {
                c++;
            }
        }
        return c;
    }
}
