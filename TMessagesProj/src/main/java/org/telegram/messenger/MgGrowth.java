/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Obunachilar kundaligi: kuzatilayotgan kanal/guruhlarning obunachi sonini belgilangan oraliqda
 * (3/6/12/24 soat) yozib boradi. Har bir tekshiruv — bitta kichik so'rov, ketma-ket va oraliq bilan,
 * shuning uchun ilova tezligiga ta'sir qilmaydi. Ma'lumot faqat telefonda saqlanadi.
 */
public final class MgGrowth {

    public static final int[] INTERVALS = {3, 6, 12, 24};
    private static final int MAX_POINTS = 1500;
    private static long lastTick;
    private static boolean ticking;

    public static final class Point {
        public final int time, count;

        Point(int time, int count) {
            this.time = time;
            this.count = count;
        }
    }

    public static int getIntervalHours() {
        return MgConfig.getInt("gr_interval_h", 12);
    }

    public static void setIntervalHours(int h) {
        MgConfig.setInt("gr_interval_h", h);
    }

    public static boolean isAutoAdmin() {
        return MgConfig.getBool("gr_auto", true);
    }

    // ---- kuzatuv ro'yxati ----

    private static LinkedHashSet<Long> manual(int account) {
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        String raw = MgConfig.getString("gr_track_" + account, "");
        if (raw != null) {
            for (String s : raw.split(",")) {
                try {
                    if (!s.isEmpty()) {
                        set.add(Long.parseLong(s));
                    }
                } catch (Throwable ignore) {
                }
            }
        }
        return set;
    }

    private static void saveManual(int account, LinkedHashSet<Long> set) {
        StringBuilder sb = new StringBuilder();
        for (Long id : set) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        MgConfig.setString("gr_track_" + account, sb.toString());
    }

    public static boolean isTracked(int account, long chatId) {
        if (manual(account).contains(chatId)) {
            return !MgConfig.getBool("gr_off_" + account + "_" + chatId, false);
        }
        if (isAutoAdmin()) {
            TLRPC.Chat c = MessagesController.getInstance(account).getChat(chatId);
            if (c != null && (c.creator || c.admin_rights != null) && ChatObject.isChannel(c)) {
                return !MgConfig.getBool("gr_off_" + account + "_" + chatId, false);
            }
        }
        return false;
    }

    public static void setTracked(int account, long chatId, boolean on) {
        LinkedHashSet<Long> set = manual(account);
        if (on) {
            set.add(chatId);
            MgConfig.setBool("gr_off_" + account + "_" + chatId, false);
        } else {
            set.remove(chatId);
            MgConfig.setBool("gr_off_" + account + "_" + chatId, true);
        }
        saveManual(account, set);
    }

    /** Kuzatilayotgan barcha chatlar (qo'lda qo'shilganlar + admin bo'lgan kanal/guruhlar) */
    public static ArrayList<Long> trackedChats(int account) {
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        for (Long id : manual(account)) {
            if (isTracked(account, id)) {
                out.add(id);
            }
        }
        if (isAutoAdmin()) {
            ArrayList<TLRPC.Dialog> all = MessagesController.getInstance(account).getAllDialogs();
            for (int i = 0; i < all.size(); i++) {
                TLRPC.Dialog d = all.get(i);
                if (d != null && d.id < 0 && isTracked(account, -d.id)) {
                    out.add(-d.id);
                }
            }
        }
        return new ArrayList<>(out);
    }

    // ---- ma'lumot ----

    private static String key(int account, long chatId) {
        return "gr_" + account + "_" + chatId;
    }

    public static ArrayList<Point> points(int account, long chatId) {
        ArrayList<Point> list = new ArrayList<>();
        String raw = MgConfig.getString(key(account, chatId), "");
        if (raw == null || raw.isEmpty()) {
            return list;
        }
        for (String p : raw.split(";")) {
            int i = p.indexOf(':');
            if (i <= 0) {
                continue;
            }
            try {
                list.add(new Point(Integer.parseInt(p.substring(0, i)), Integer.parseInt(p.substring(i + 1))));
            } catch (Throwable ignore) {
            }
        }
        return list;
    }

    public static void clear(int account, long chatId) {
        MgConfig.setString(key(account, chatId), null);
    }

    /** Yangi qiymat: oxirgi yozuvdan keyin oraliq o'tgan bo'lsa (yoki force) qo'shiladi */
    public static synchronized void record(int account, long chatId, int count, boolean force) {
        if (count <= 0) {
            return;
        }
        int now = ConnectionsManager.getInstance(account).getCurrentTime();
        ArrayList<Point> list = points(account, chatId);
        if (!list.isEmpty()) {
            Point last = list.get(list.size() - 1);
            if (!force && now - last.time < getIntervalHours() * 3600 - 300) {
                return;
            }
            if (force && now - last.time < 600 && last.count == count) {
                return;
            }
        }
        list.add(new Point(now, count));
        while (list.size() > MAX_POINTS) {
            list.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for (Point p : list) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(p.time).append(':').append(p.count);
        }
        MgConfig.setString(key(account, chatId), sb.toString());
    }

    private static boolean due(int account, long chatId) {
        ArrayList<Point> list = points(account, chatId);
        if (list.isEmpty()) {
            return true;
        }
        int now = ConnectionsManager.getInstance(account).getCurrentTime();
        return now - list.get(list.size() - 1).time >= getIntervalHours() * 3600 - 300;
    }

    /** Bitta chatni hozir tekshirish */
    public static void fetch(int account, long chatId, boolean force, Runnable done) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.Chat c = mc.getChat(chatId);
        if (c == null || !ChatObject.isChannel(c)) {
            if (c != null && c.participants_count > 0) {
                record(account, chatId, c.participants_count, force);
            }
            if (done != null) {
                AndroidUtilities.runOnUIThread(done);
            }
            return;
        }
        TLRPC.TL_channels_getFullChannel req = new TLRPC.TL_channels_getFullChannel();
        req.channel = mc.getInputChannel(chatId);
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
            if (res instanceof TLRPC.TL_messages_chatFull) {
                TLRPC.ChatFull full = ((TLRPC.TL_messages_chatFull) res).full_chat;
                if (full != null && full.participants_count > 0) {
                    record(account, chatId, full.participants_count, force);
                }
            }
            if (done != null) {
                AndroidUtilities.runOnUIThread(done);
            }
        });
    }

    /** Ilova ochilganda: vaqti kelgan chatlarni ketma-ket, sekin tekshiradi (ko'pi bilan 25 ta) */
    public static void tick(int account) {
        long now = System.currentTimeMillis();
        if (ticking || now - lastTick < 10 * 60_000L) {
            return;
        }
        lastTick = now;
        ArrayList<Long> ids = trackedChats(account);
        ArrayList<Long> dueIds = new ArrayList<>();
        for (Long id : ids) {
            if (due(account, id)) {
                dueIds.add(id);
                if (dueIds.size() >= 25) {
                    break;
                }
            }
        }
        if (dueIds.isEmpty()) {
            return;
        }
        ticking = true;
        next(account, dueIds, 0);
    }

    private static void next(int account, ArrayList<Long> ids, int index) {
        if (index >= ids.size()) {
            ticking = false;
            return;
        }
        fetch(account, ids.get(index), false, () -> AndroidUtilities.runOnUIThread(() -> next(account, ids, index + 1), 1500));
    }
}
