/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Admin bo'lmagan kanal/guruhdagi xabarni faqat o'zidan (shu qurilmadan) o'chirish.
 * Xabar keshdan o'chiriladi va ID'si eslab qolinadi — serverdan qayta yuklanganda ham ko'rinmaydi.
 */
public class MgLocalDelete {

    private static final int MAX_PER_DIALOG = 3000;
    private static final HashMap<String, HashSet<Integer>> cache = new HashMap<>();

    private static String key(int account, long dialogId) {
        return "lmdel_" + account + "_" + dialogId;
    }

    public static synchronized HashSet<Integer> get(int account, long dialogId) {
        String k = key(account, dialogId);
        HashSet<Integer> set = cache.get(k);
        if (set == null) {
            set = new HashSet<>();
            String s = MgConfig.getString(k, null);
            if (s != null && !s.isEmpty()) {
                for (String p : s.split(",")) {
                    try {
                        set.add(Integer.parseInt(p));
                    } catch (Exception ignore) {
                    }
                }
            }
            cache.put(k, set);
        }
        return set;
    }

    private static synchronized void add(int account, long dialogId, Collection<Integer> ids) {
        HashSet<Integer> set = get(account, dialogId);
        set.addAll(ids);
        ArrayList<Integer> list = new ArrayList<>(set);
        java.util.Collections.sort(list);
        if (list.size() > MAX_PER_DIALOG) {
            list = new ArrayList<>(list.subList(list.size() - MAX_PER_DIALOG, list.size()));
            set.clear();
            set.addAll(list);
        }
        MgConfig.setString(key(account, dialogId), android.text.TextUtils.join(",", list));
    }

    /** Xabar(lar)ni faqat shu qurilmadan o'chiradi */
    public static void delete(int account, long dialogId, ArrayList<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        add(account, dialogId, ids);
        MessagesController mc = MessagesController.getInstance(account);
        org.milliygram.utils.By prev = mc.whoDeleted;
        mc.whoDeleted = org.milliygram.utils.By.Me;
        try {
            mc.deleteMessages(new ArrayList<>(ids), null, null, dialogId, 0, false, 0, true);
        } finally {
            mc.whoDeleted = prev;
        }
    }

    /** Serverdan kelgan ro'yxatdan o'zidan o'chirilganlarni olib tashlaydi */
    public static void filter(int account, long dialogId, ArrayList<TLRPC.Message> messages) {
        if (messages == null || messages.isEmpty() || dialogId >= 0) {
            return;
        }
        HashSet<Integer> set = get(account, dialogId);
        if (set.isEmpty()) {
            return;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            TLRPC.Message m = messages.get(i);
            if (m != null && set.contains(m.id)) {
                messages.remove(i);
            }
        }
    }

    /** Chatlar ro'yxati (server yoki kesh) uchun oxirgi xabarlardan o'zidan o'chirilganlarini olib tashlaydi */
    public static void filterTop(int account, ArrayList<TLRPC.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        try {
            for (int i = messages.size() - 1; i >= 0; i--) {
                TLRPC.Message m = messages.get(i);
                if (m == null || m.peer_id == null) {
                    continue;
                }
                long did = MessageObject.getDialogId(m);
                if (did >= 0) {
                    continue;
                }
                HashSet<Integer> set = get(account, did);
                if (!set.isEmpty() && set.contains(m.id)) {
                    messages.remove(i);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /** Admin huquqi bo'lmagan kanal/guruhdagi xabarni o'zidan o'chirish mumkinmi */
    public static boolean canDeleteLocally(MessageObject message, TLRPC.Chat chat) {
        return message != null && chat != null && message.getId() > 0 && !message.isSponsored()
                && message.getDialogId() < 0;
    }
}
