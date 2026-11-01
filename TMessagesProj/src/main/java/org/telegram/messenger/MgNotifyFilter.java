/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Bildirishnoma filtri: faqat tanlangan turdagi chatlardan (shaxsiy, bot, guruh, kanal) bildirishnoma keladi.
 * Istisno ro'yxatidagi chatlardan esa har doim keladi. Telegram'ning o'z ovozsiz qilish sozlamalari o'zgarmaydi.
 */
public final class MgNotifyFilter {

    public static final String KEY_ON = "nf_on";
    public static final String KEY_USERS = "nf_users";
    public static final String KEY_BOTS = "nf_bots";
    public static final String KEY_GROUPS = "nf_groups";
    public static final String KEY_CHANNELS = "nf_channels";

    private MgNotifyFilter() {
    }

    public static boolean isEnabled() {
        return MgConfig.getBool(KEY_ON, false);
    }

    /** true — bu chatdan bildirishnoma ko'rsatilmaydi */
    public static boolean blocks(int account, long dialogId) {
        if (!isEnabled() || dialogId == 0) {
            return false;
        }
        if (getExceptions(account).contains(dialogId)) {
            return false;
        }
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return !MgConfig.getBool(KEY_USERS, true);
        }
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
            boolean bot = user != null && user.bot;
            return !MgConfig.getBool(bot ? KEY_BOTS : KEY_USERS, true);
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        boolean channel = ChatObject.isChannelAndNotMegaGroup(chat);
        return !MgConfig.getBool(channel ? KEY_CHANNELS : KEY_GROUPS, true);
    }

    private static String exceptionsKey(int account) {
        return "nf_exc_" + account;
    }

    public static ArrayList<Long> getExceptions(int account) {
        ArrayList<Long> list = new ArrayList<>();
        String raw = MgConfig.getString(exceptionsKey(account), "");
        if (TextUtils.isEmpty(raw)) {
            return list;
        }
        for (String s : raw.split(",")) {
            try {
                list.add(Long.parseLong(s.trim()));
            } catch (Exception ignore) {
            }
        }
        return list;
    }

    private static void save(int account, ArrayList<Long> list) {
        StringBuilder sb = new StringBuilder();
        for (Long id : list) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        MgConfig.setString(exceptionsKey(account), sb.toString());
    }

    public static void addException(int account, long dialogId) {
        ArrayList<Long> list = getExceptions(account);
        if (!list.contains(dialogId)) {
            list.add(dialogId);
            save(account, list);
        }
    }

    public static void removeException(int account, long dialogId) {
        ArrayList<Long> list = getExceptions(account);
        if (list.remove(dialogId)) {
            save(account, list);
        }
    }
}
