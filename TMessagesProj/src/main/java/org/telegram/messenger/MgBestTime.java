/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Eng yaxshi post vaqti: obunachilar qaysi soatlarda eng faol ekanini aniqlaydi.
 * Manba 1 — Telegram kanal statistikasidagi "soatlar bo'yicha ko'rishlar" grafigi (katta kanallarda).
 * Manba 2 — statistika yopiq bo'lsa, oxirgi 100 ta postning ko'rishlari joylangan soati bo'yicha o'rtachasi.
 * Natija faqat telefonda saqlanadi.
 */
public final class MgBestTime {

    public static final String SRC_STATS = "stats";
    public static final String SRC_POSTS = "posts";
    private static final long CACHE_MS = 6 * 3600_000L;

    public static final class Result {
        public final float[] hours = new float[24];
        public String source = "";
        public int samples;
        public long time;

        public boolean isEmpty() {
            for (float v : hours) {
                if (v > 0) {
                    return false;
                }
            }
            return true;
        }

        /** Eng faol soatlar (kamayish tartibida) */
        public int[] top(int n) {
            int[] out = new int[Math.min(n, 24)];
            boolean[] used = new boolean[24];
            for (int k = 0; k < out.length; k++) {
                int best = -1;
                for (int h = 0; h < 24; h++) {
                    if (!used[h] && (best < 0 || hours[h] > hours[best])) {
                        best = h;
                    }
                }
                used[best] = true;
                out[k] = best;
            }
            return out;
        }

        public float max() {
            float m = 0;
            for (float v : hours) {
                m = Math.max(m, v);
            }
            return m;
        }
    }

    private MgBestTime() {
    }

    private static String key(int account, long chatId) {
        return "gbt_" + account + "_" + chatId;
    }

    /** Saqlangan natija (bo'lmasa null) */
    public static Result cached(int account, long chatId) {
        String raw = MgConfig.getString(key(account, chatId), null);
        if (raw == null) {
            return null;
        }
        try {
            String[] p = raw.split("\\|");
            if (p.length < 4) {
                return null;
            }
            Result r = new Result();
            r.source = p[0];
            r.time = Long.parseLong(p[1]);
            r.samples = Integer.parseInt(p[2]);
            String[] v = p[3].split(",");
            for (int i = 0; i < 24 && i < v.length; i++) {
                r.hours[i] = Float.parseFloat(v[i]);
            }
            return r;
        } catch (Throwable e) {
            return null;
        }
    }

    private static void save(int account, long chatId, Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.source).append('|').append(r.time).append('|').append(r.samples).append('|');
        for (int i = 0; i < 24; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.US, "%.1f", r.hours[i]));
        }
        MgConfig.setString(key(account, chatId), sb.toString());
    }

    public static String hourRange(int h) {
        return String.format(Locale.US, "%02d:00–%02d:00", h, (h + 1) % 24);
    }

    /** Hisoblaydi; natija UI oqimida (null — aniqlab bo'lmadi) */
    public static void load(int account, long chatId, boolean force, Utilities.Callback<Result> done) {
        Result c = cached(account, chatId);
        if (!force && c != null && System.currentTimeMillis() - c.time < CACHE_MS) {
            done.run(c);
            return;
        }
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.Chat chat = mc.getChat(chatId);
        if (chat == null || !ChatObject.isChannel(chat)) {
            done.run(c);
            return;
        }
        TLRPC.TL_channels_getFullChannel req = new TLRPC.TL_channels_getFullChannel();
        req.channel = mc.getInputChannel(chatId);
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            TLRPC.ChatFull full = res instanceof TLRPC.TL_messages_chatFull ? ((TLRPC.TL_messages_chatFull) res).full_chat : null;
            Utilities.Callback<Result> finish = r -> {
                if (r != null && !r.isEmpty()) {
                    r.time = System.currentTimeMillis();
                    save(account, chatId, r);
                    done.run(r);
                } else {
                    done.run(c);
                }
            };
            if (full != null && full.can_view_stats) {
                loadStats(account, chatId, full.stats_dc, chat.megagroup, r -> {
                    if (r != null && !r.isEmpty()) {
                        finish.run(r);
                    } else if (!chat.megagroup) {
                        loadPosts(account, chatId, finish);
                    } else {
                        finish.run(null);
                    }
                });
            } else if (!chat.megagroup) {
                loadPosts(account, chatId, finish);
            } else {
                finish.run(null);
            }
        }));
    }

    private static void loadStats(int account, long chatId, int dc, boolean megagroup, Utilities.Callback<Result> cb) {
        MessagesController mc = MessagesController.getInstance(account);
        TLObject req;
        if (megagroup) {
            TL_stats.TL_getMegagroupStats r = new TL_stats.TL_getMegagroupStats();
            r.channel = mc.getInputChannel(chatId);
            req = r;
        } else {
            TL_stats.TL_getBroadcastStats r = new TL_stats.TL_getBroadcastStats();
            r.channel = mc.getInputChannel(chatId);
            req = r;
        }
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
            TL_stats.StatsGraph graph = null;
            if (res instanceof TL_stats.TL_broadcastStats) {
                graph = ((TL_stats.TL_broadcastStats) res).top_hours_graph;
            } else if (res instanceof TL_stats.TL_megagroupStats) {
                graph = ((TL_stats.TL_megagroupStats) res).top_hours_graph;
            }
            handleGraph(account, dc, graph, cb);
        }, null, null, 0, dc, ConnectionsManager.ConnectionTypeGeneric, true);
    }

    private static void handleGraph(int account, int dc, TL_stats.StatsGraph graph, Utilities.Callback<Result> cb) {
        if (graph instanceof TL_stats.TL_statsGraph) {
            Result r = parseGraph(((TL_stats.TL_statsGraph) graph).json.data);
            AndroidUtilities.runOnUIThread(() -> cb.run(r));
        } else if (graph instanceof TL_stats.TL_statsGraphAsync) {
            TL_stats.TL_loadAsyncGraph r = new TL_stats.TL_loadAsyncGraph();
            r.token = ((TL_stats.TL_statsGraphAsync) graph).token;
            ConnectionsManager.getInstance(account).sendRequest(r, (res, err) -> {
                if (res instanceof TL_stats.StatsGraph) {
                    handleGraph(account, dc, (TL_stats.StatsGraph) res, cb);
                } else {
                    AndroidUtilities.runOnUIThread(() -> cb.run(null));
                }
            }, null, null, 0, dc, ConnectionsManager.ConnectionTypeGeneric, true);
        } else {
            AndroidUtilities.runOnUIThread(() -> cb.run(null));
        }
    }

    /** "Soatlar bo'yicha ko'rishlar" grafigi: x — vaqt (ms), birinchi y — joriy hafta */
    private static Result parseGraph(String json) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(json);
            org.json.JSONArray cols = o.getJSONArray("columns");
            org.json.JSONArray xs = null, ys = null;
            for (int i = 0; i < cols.length(); i++) {
                org.json.JSONArray col = cols.getJSONArray(i);
                String id = col.getString(0);
                if ("x".equals(id)) {
                    xs = col;
                } else if (ys == null) {
                    ys = col;
                }
            }
            if (xs == null || ys == null) {
                return null;
            }
            Result r = new Result();
            r.source = SRC_STATS;
            Calendar c = Calendar.getInstance();
            int n = Math.min(xs.length(), ys.length());
            for (int i = 1; i < n; i++) {
                long x = xs.getLong(i);
                int hour;
                if (x >= 0 && x < 24) {
                    hour = (int) x; // ba'zi grafiklarda x to'g'ridan-to'g'ri soat
                } else {
                    c.setTimeInMillis(x);
                    hour = c.get(Calendar.HOUR_OF_DAY);
                }
                r.hours[hour] += (float) ys.optDouble(i, 0);
                r.samples++;
            }
            return r;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** Statistika yopiq kanallar: oxirgi postlarning ko'rishlari joylangan soati bo'yicha */
    private static void loadPosts(int account, long chatId, Utilities.Callback<Result> cb) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = mc.getInputPeer(-chatId);
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
            Result r = null;
            if (res instanceof TLRPC.messages_Messages) {
                ArrayList<TLRPC.Message> msgs = ((TLRPC.messages_Messages) res).messages;
                int now = ConnectionsManager.getInstance(account).getCurrentTime();
                float[] sum = new float[24];
                int[] cnt = new int[24];
                Calendar c = Calendar.getInstance();
                long lastGroup = 0;
                int total = 0;
                for (TLRPC.Message m : msgs) {
                    if (m == null || m instanceof TLRPC.TL_messageService || (m.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) == 0 || m.views <= 0) {
                        continue;
                    }
                    int age = now - m.date;
                    // juda yangi postlar hali ko'rish yig'ib ulgurmagan; juda eskilari bugungi auditoriyani aks ettirmaydi
                    if (age < 20 * 3600 || age > 60 * 86400) {
                        continue;
                    }
                    if (m.grouped_id != 0) {
                        if (m.grouped_id == lastGroup) {
                            continue;
                        }
                        lastGroup = m.grouped_id;
                    }
                    c.setTimeInMillis(m.date * 1000L);
                    int h = c.get(Calendar.HOUR_OF_DAY);
                    sum[h] += m.views;
                    cnt[h]++;
                    total++;
                }
                if (total >= 5) {
                    r = new Result();
                    r.source = SRC_POSTS;
                    r.samples = total;
                    for (int h = 0; h < 24; h++) {
                        r.hours[h] = cnt[h] > 0 ? sum[h] / cnt[h] : 0;
                    }
                }
            }
            final Result fr = r;
            AndroidUtilities.runOnUIThread(() -> cb.run(fr));
        });
    }
}
