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
            TLRPC.ChatFull full = null;
            if (res instanceof TLRPC.TL_messages_chatFull) {
                full = ((TLRPC.TL_messages_chatFull) res).full_chat;
                if (full != null && full.participants_count > 0) {
                    record(account, chatId, full.participants_count, force);
                }
            }
            final TLRPC.ChatFull ff = full;
            AndroidUtilities.runOnUIThread(() -> fetchFlows(account, chatId, ff, done));
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

    // ================= Kirganlar / chiqqanlar =================

    /** Kunlik oqim: kalit yyyymmdd → {kirdi, chiqdi} */
    public static java.util.TreeMap<Integer, int[]> flows(int account, long chatId) {
        java.util.TreeMap<Integer, int[]> map = new java.util.TreeMap<>();
        String raw = MgConfig.getString("grd_" + account + "_" + chatId, "");
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String p : raw.split(";")) {
            String[] a = p.split(":");
            if (a.length == 3) {
                try {
                    map.put(Integer.parseInt(a[0]), new int[]{Integer.parseInt(a[1]), Integer.parseInt(a[2])});
                } catch (Throwable ignore) {
                }
            }
        }
        return map;
    }

    private static void saveFlows(int account, long chatId, java.util.TreeMap<Integer, int[]> map) {
        while (map.size() > 400) {
            map.remove(map.firstKey());
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<Integer, int[]> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append(':').append(e.getValue()[0]).append(':').append(e.getValue()[1]);
        }
        MgConfig.setString("grd_" + account + "_" + chatId, sb.toString());
    }

    public static int dayKey(long unixSeconds) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(unixSeconds * 1000L);
        return c.get(java.util.Calendar.YEAR) * 10000 + (c.get(java.util.Calendar.MONTH) + 1) * 100 + c.get(java.util.Calendar.DAY_OF_MONTH);
    }

    /** Ma'lumot manbai: "stats" — Telegram statistikasi, "log" — admin jurnali, "" — faqat umumiy son */
    public static String flowSource(int account, long chatId) {
        return MgConfig.getString("grd_src_" + account + "_" + chatId, "");
    }

    private static void fetchFlows(int account, long chatId, TLRPC.ChatFull full, Runnable done) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.Chat c = mc.getChat(chatId);
        if (full != null && full.can_view_stats && c != null) {
            loadStats(account, chatId, full.stats_dc, c.megagroup, ok -> {
                if (!ok && c != null && (c.creator || c.admin_rights != null)) {
                    loadAdminLog(account, chatId, done);
                } else if (done != null) {
                    done.run();
                }
            });
        } else if (c != null && (c.creator || c.admin_rights != null) && ChatObject.isChannel(c)) {
            loadAdminLog(account, chatId, done);
        } else if (done != null) {
            done.run();
        }
    }

    private static void loadStats(int account, long chatId, int dc, boolean megagroup, Utilities.Callback<Boolean> cb) {
        MessagesController mc = MessagesController.getInstance(account);
        org.telegram.tgnet.TLObject req;
        if (megagroup) {
            org.telegram.tgnet.tl.TL_stats.TL_getMegagroupStats r = new org.telegram.tgnet.tl.TL_stats.TL_getMegagroupStats();
            r.channel = mc.getInputChannel(chatId);
            req = r;
        } else {
            org.telegram.tgnet.tl.TL_stats.TL_getBroadcastStats r = new org.telegram.tgnet.tl.TL_stats.TL_getBroadcastStats();
            r.channel = mc.getInputChannel(chatId);
            req = r;
        }
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
            org.telegram.tgnet.tl.TL_stats.StatsGraph graph = null;
            if (res instanceof org.telegram.tgnet.tl.TL_stats.TL_broadcastStats) {
                graph = ((org.telegram.tgnet.tl.TL_stats.TL_broadcastStats) res).followers_graph;
            } else if (res instanceof org.telegram.tgnet.tl.TL_stats.TL_megagroupStats) {
                graph = ((org.telegram.tgnet.tl.TL_stats.TL_megagroupStats) res).members_graph;
            }
            handleGraph(account, chatId, dc, graph, cb);
        }, null, null, 0, dc, ConnectionsManager.ConnectionTypeGeneric, true);
    }

    private static void handleGraph(int account, long chatId, int dc, org.telegram.tgnet.tl.TL_stats.StatsGraph graph, Utilities.Callback<Boolean> cb) {
        if (graph instanceof org.telegram.tgnet.tl.TL_stats.TL_statsGraph) {
            boolean ok = parseGraph(account, chatId, ((org.telegram.tgnet.tl.TL_stats.TL_statsGraph) graph).json.data);
            AndroidUtilities.runOnUIThread(() -> cb.run(ok));
        } else if (graph instanceof org.telegram.tgnet.tl.TL_stats.TL_statsGraphAsync) {
            org.telegram.tgnet.tl.TL_stats.TL_loadAsyncGraph r = new org.telegram.tgnet.tl.TL_stats.TL_loadAsyncGraph();
            r.token = ((org.telegram.tgnet.tl.TL_stats.TL_statsGraphAsync) graph).token;
            ConnectionsManager.getInstance(account).sendRequest(r, (res, err) -> {
                if (res instanceof org.telegram.tgnet.tl.TL_stats.TL_statsGraph) {
                    handleGraph(account, chatId, dc, (org.telegram.tgnet.tl.TL_stats.StatsGraph) res, cb);
                } else {
                    AndroidUtilities.runOnUIThread(() -> cb.run(false));
                }
            }, null, null, 0, dc, ConnectionsManager.ConnectionTypeGeneric, true);
        } else {
            AndroidUtilities.runOnUIThread(() -> cb.run(false));
        }
    }

    /** Telegram statistikasi grafigi (JSON): x — vaqt (ms), y0/y1 — kirganlar va chiqqanlar */
    private static boolean parseGraph(int account, long chatId, String json) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(json);
            org.json.JSONArray cols = o.getJSONArray("columns");
            org.json.JSONObject names = o.optJSONObject("names");
            org.json.JSONArray xs = null, joined = null, left = null;
            java.util.ArrayList<org.json.JSONArray> ys = new java.util.ArrayList<>();
            for (int i = 0; i < cols.length(); i++) {
                org.json.JSONArray col = cols.getJSONArray(i);
                String id = col.getString(0);
                if ("x".equals(id)) {
                    xs = col;
                    continue;
                }
                ys.add(col);
                String name = names == null ? "" : names.optString(id, "").toLowerCase();
                if (name.contains("left") || name.contains("покин") || name.contains("отпис") || name.contains("chiq") || name.contains("unsub")) {
                    left = col;
                } else if (name.contains("join") || name.contains("присоед") || name.contains("подпис") || name.contains("qo'sh") || name.contains("new")) {
                    joined = col;
                }
            }
            if (joined == null && !ys.isEmpty()) {
                joined = ys.get(0);
            }
            if (left == null && ys.size() > 1) {
                left = ys.get(1) == joined ? ys.get(0) : ys.get(1);
            }
            if (xs == null || joined == null) {
                return false;
            }
            java.util.TreeMap<Integer, int[]> map = flows(account, chatId);
            for (int i = 1; i < xs.length(); i++) {
                long ms = xs.getLong(i);
                int key = dayKey(ms / 1000);
                int j = i < joined.length() ? joined.optInt(i) : 0;
                int l = left != null && i < left.length() ? left.optInt(i) : 0;
                map.put(key, new int[]{j, l});
            }
            saveFlows(account, chatId, map);
            MgConfig.setString("grd_src_" + account + "_" + chatId, "stats");
            return true;
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    /** Admin jurnali (oxirgi 48 soat): kirganlar/chiqqanlar voqealarini kunlar bo'yicha sanaydi */
    private static void loadAdminLog(int account, long chatId, Runnable done) {
        long lastSeen = Long.parseLong(MgConfig.getString("grd_last_" + account + "_" + chatId, "0"));
        java.util.TreeMap<Integer, int[]> add = new java.util.TreeMap<>();
        long[] maxSeen = {lastSeen};
        pageAdminLog(account, chatId, 0, lastSeen, add, maxSeen, 0, () -> {
            java.util.TreeMap<Integer, int[]> map = flows(account, chatId);
            for (java.util.Map.Entry<Integer, int[]> e : add.entrySet()) {
                int[] cur = map.get(e.getKey());
                if (cur == null) {
                    map.put(e.getKey(), e.getValue());
                } else {
                    cur[0] += e.getValue()[0];
                    cur[1] += e.getValue()[1];
                }
            }
            saveFlows(account, chatId, map);
            MgConfig.setString("grd_last_" + account + "_" + chatId, String.valueOf(maxSeen[0]));
            if (!"stats".equals(flowSource(account, chatId))) {
                MgConfig.setString("grd_src_" + account + "_" + chatId, "log");
            }
            if (done != null) {
                done.run();
            }
        });
    }

    private static void pageAdminLog(int account, long chatId, long maxId, long lastSeen, java.util.TreeMap<Integer, int[]> add, long[] maxSeen, int page, Runnable finish) {
        TLRPC.TL_channels_getAdminLog req = new TLRPC.TL_channels_getAdminLog();
        req.channel = MessagesController.getInstance(account).getInputChannel(chatId);
        req.q = "";
        req.limit = 100;
        req.max_id = maxId;
        req.min_id = lastSeen;
        req.flags |= 1;
        req.events_filter = new TLRPC.TL_channelAdminLogEventsFilter();
        req.events_filter.join = true;
        req.events_filter.leave = true;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (!(res instanceof TLRPC.TL_channels_adminLogResults)) {
                finish.run();
                return;
            }
            ArrayList<TLRPC.TL_channelAdminLogEvent> ev = ((TLRPC.TL_channels_adminLogResults) res).events;
            long minId = Long.MAX_VALUE;
            for (TLRPC.TL_channelAdminLogEvent e : ev) {
                if (e.id <= lastSeen) {
                    continue;
                }
                maxSeen[0] = Math.max(maxSeen[0], e.id);
                minId = Math.min(minId, e.id);
                int key = dayKey(e.date);
                int[] v = add.get(key);
                if (v == null) {
                    v = new int[2];
                    add.put(key, v);
                }
                if (e.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantLeave) {
                    v[1]++;
                } else if (e.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantJoin
                        || e.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantJoinByInvite
                        || e.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantJoinByRequest) {
                    v[0]++;
                }
            }
            if (ev.size() >= 100 && minId != Long.MAX_VALUE && page < 20) {
                final long nextMaxId = minId;
                AndroidUtilities.runOnUIThread(() -> pageAdminLog(account, chatId, nextMaxId, lastSeen, add, maxSeen, page + 1, finish), 700);
            } else {
                finish.run();
            }
        }));
    }
}
