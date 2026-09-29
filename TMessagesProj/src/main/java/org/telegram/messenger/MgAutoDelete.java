/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Postni vaqt o'tgach avtomatik o'chirish (reklama narxlaridagi "1/24", "1/48" formati).
 *
 * Ishlash tartibi:
 *  1. Foydalanuvchi yuborishdan oldin chatni "qurollantiradi" ({@link #arm}) — muddat tanlanadi.
 *  2. Shu chatga ketgan keyingi post (matn, media, albom yoki uzatish; darhol yoki rejalashtirilgan)
 *     SendMessagesHelper orqali {@link #onSend} ga tushadi va yozuv yaratiladi.
 *  3. Darhol yuborilganda server bergan haqiqiy ID lar {@link #onReceivedByServer} orqali yoziladi;
 *     rejalashtirilgan postda esa ID keyin o'zgaradi, shuning uchun o'chirish paytida post
 *     chiqqan vaqti va matni bo'yicha kanal tarixidan topiladi.
 *  4. Muddat post kanalda paydo bo'lgan vaqtdan hisoblanadi. AlarmManager ilova yopiq bo'lsa ham
 *     uyg'otadi; ilova ochilganda va har 5 daqiqada ham tekshiriladi.
 * Hammasi faqat shu telefonda saqlanadi.
 */
public class MgAutoDelete extends BroadcastReceiver {

    public static final String ACTION = "uz.milliygram.AUTO_DELETE";
    private static final int REQ = 770311;
    private static final String PREF = "mg_ad_recs";
    /** Qurollantirilgan holat yuborilmasa shuncha vaqtdan keyin bekor bo'ladi */
    private static final long ARM_TTL_MS = 15 * 60_000L;
    /** Albom qismlari ketma-ket keladi — birinchisidan keyin shuncha vaqt davomida bitta yozuvga qo'shiladi */
    private static final long ARM_GRACE_MS = 10_000L;

    public static final int[] PRESET_HOURS = {1, 3, 6, 12, 24, 48, 72, 168};

    private static final class Arm {
        boolean mode; // doimiy rejimdan yaratilgan
        int seconds;
        long armedAt;
        long firstUse;
        Rec rec;
    }

    static final class Rec {
        String id;
        int account;
        long did;
        long accessHash;
        boolean channel;
        int seconds;
        int postDate;
        boolean scheduled;
        String textKey = "";
        final ArrayList<Integer> ids = new ArrayList<>();
        int expected;
        long groupedId;
        int deleteAt;
        long createdMs;
        int tries;
        String title = "";

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("a", account);
            o.put("d", did);
            o.put("h", accessHash);
            o.put("c", channel);
            o.put("s", seconds);
            o.put("p", postDate);
            o.put("sc", scheduled);
            o.put("t", textKey);
            JSONArray arr = new JSONArray();
            for (Integer i : ids) {
                arr.put(i);
            }
            o.put("i", arr);
            o.put("e", expected);
            o.put("g", groupedId);
            o.put("at", deleteAt);
            o.put("cm", createdMs);
            o.put("tr", tries);
            o.put("ti", title);
            return o;
        }

        static Rec fromJson(JSONObject o) {
            Rec r = new Rec();
            r.id = o.optString("id");
            r.account = o.optInt("a");
            r.did = o.optLong("d");
            r.accessHash = o.optLong("h");
            r.channel = o.optBoolean("c");
            r.seconds = o.optInt("s");
            r.postDate = o.optInt("p");
            r.scheduled = o.optBoolean("sc");
            r.textKey = o.optString("t", "");
            JSONArray arr = o.optJSONArray("i");
            if (arr != null) {
                for (int k = 0; k < arr.length(); k++) {
                    r.ids.add(arr.optInt(k));
                }
            }
            r.expected = o.optInt("e");
            r.groupedId = o.optLong("g");
            r.deleteAt = o.optInt("at");
            r.createdMs = o.optLong("cm");
            r.tries = o.optInt("tr");
            r.title = o.optString("ti", "");
            return r;
        }
    }

    private static final Object sync = new Object();
    private static final HashMap<String, Arm> arms = new HashMap<>();
    private static ArrayList<Rec> recs;
    private static final HashSet<String> inFlight = new HashSet<>();

    private static String armKey(int account, long did) {
        return account + "_" + did;
    }

    // ------------------------------------------------------------------ holat

    private static ArrayList<Rec> recs() {
        if (recs == null) {
            recs = new ArrayList<>();
            try {
                String raw = MgConfig.getString(PREF, null);
                if (!TextUtils.isEmpty(raw)) {
                    JSONArray arr = new JSONArray(raw);
                    for (int i = 0; i < arr.length(); i++) {
                        recs.add(Rec.fromJson(arr.getJSONObject(i)));
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return recs;
    }

    private static void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Rec r : recs()) {
                arr.put(r.toJson());
            }
            MgConfig.setString(PREF, arr.toString());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static int now(int account) {
        try {
            return ConnectionsManager.getInstance(account).getCurrentTime();
        } catch (Throwable e) {
            return (int) (System.currentTimeMillis() / 1000);
        }
    }

    /** Matnni solishtirish uchun qisqa kalit: bo'shliqlarsiz, kichik harflarda, 48 belgigacha */
    public static String textKey(CharSequence text) {
        if (text == null) {
            return "";
        }
        String s = text.toString().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return s.length() > 48 ? s.substring(0, 48) : s;
    }

    // ------------------------------------------------------------------ qurollantirish

    public static void arm(int account, long did, int seconds) {
        synchronized (sync) {
            if (seconds <= 0) {
                arms.remove(armKey(account, did));
                return;
            }
            Arm a = new Arm();
            a.seconds = seconds;
            a.armedAt = System.currentTimeMillis();
            arms.put(armKey(account, did), a);
        }
    }

    public static void disarm(int account, long did) {
        synchronized (sync) {
            arms.remove(armKey(account, did));
        }
    }

    /** Chat uchun qurollantirilgan muddat (soniya), yo'q bo'lsa 0 */
    public static int armedSeconds(int account, long did) {
        synchronized (sync) {
            Arm a = arms.get(armKey(account, did));
            if (a == null || a.firstUse == 0 && System.currentTimeMillis() - a.armedAt > ARM_TTL_MS) {
                return 0;
            }
            return a.firstUse == 0 ? a.seconds : 0;
        }
    }

    // ------------------------------------------------------------------ doimiy rejim

    /** Kanal/guruh uchun doimiy avto-o'chirish muddati (soniya), 0 — o'chiq */
    public static int getChatMode(int account, long did) {
        return MgConfig.getInt("ad_mode_" + account + "_" + did, 0);
    }

    public static void setChatMode(int account, long did, int seconds) {
        MgConfig.setInt("ad_mode_" + account + "_" + did, Math.max(0, seconds));
    }

    /** SendMessagesHelper'dan: chatga post ketmoqda (count — xabarlar soni) */
    public static void onSend(int account, long did, int scheduleDate, CharSequence text, int count) {
        if (did == 0 || count <= 0) {
            return;
        }
        synchronized (sync) {
            String key = armKey(account, did);
            Arm a = arms.get(key);
            long nowMs = System.currentTimeMillis();
            if (a != null && (a.firstUse == 0 && nowMs - a.armedAt > ARM_TTL_MS || a.firstUse != 0 && nowMs - a.firstUse > ARM_GRACE_MS)) {
                arms.remove(key);
                a = null;
            }
            if (a == null) {
                // bir martalik belgi yo'q — chatning doimiy rejimi bormi?
                int mode = did < 0 ? getChatMode(account, did) : 0;
                if (mode <= 0) {
                    return;
                }
                a = new Arm();
                a.mode = true;
                a.seconds = mode;
                a.armedAt = nowMs;
                arms.put(key, a);
            }
            boolean scheduled = scheduleDate > 0 && scheduleDate != 0x7FFFFFFE;
            String tk = textKey(text);
            Rec r = a.rec;
            if (r == null || r.scheduled != scheduled || scheduled && r.postDate != scheduleDate) {
                r = new Rec();
                r.id = Long.toHexString(nowMs) + "_" + Math.abs(did % 100000);
                r.account = account;
                r.did = did;
                r.seconds = a.seconds;
                r.scheduled = scheduled;
                r.postDate = scheduled ? scheduleDate : now(account);
                r.deleteAt = r.postDate + a.seconds;
                r.createdMs = nowMs;
                try {
                    MessagesController mc = MessagesController.getInstance(account);
                    if (did < 0) {
                        TLRPC.Chat chat = mc.getChat(-did);
                        if (chat != null) {
                            r.channel = ChatObject.isChannel(chat);
                            r.accessHash = chat.access_hash;
                            r.title = chat.title == null ? "" : chat.title;
                        }
                    } else {
                        TLRPC.User user = mc.getUser(did);
                        if (user != null) {
                            r.accessHash = user.access_hash;
                            r.title = UserObject.getUserName(user);
                        }
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
                recs().add(r);
                a.rec = r;
            }
            r.expected += count;
            if (TextUtils.isEmpty(r.textKey) && !TextUtils.isEmpty(tk)) {
                r.textKey = tk;
            }
            if (a.firstUse == 0) {
                a.firstUse = nowMs;
            }
            save();
        }
        scheduleAlarm(ApplicationLoader.applicationContext);
    }

    /** SendMessagesHelper'dan: server xabarni qabul qildi (haqiqiy ID) */
    public static void onReceivedByServer(int account, int newId, TLRPC.Message msg, long did, long groupedId, boolean scheduled) {
        if (scheduled || newId <= 0) {
            return;
        }
        boolean changed = false;
        synchronized (sync) {
            ArrayList<Rec> list = recs();
            if (list.isEmpty()) {
                return;
            }
            long nowMs = System.currentTimeMillis();
            for (Rec r : list) {
                if (r.account != account || r.did != did || r.scheduled || r.ids.size() >= r.expected || nowMs - r.createdMs > 60 * 60_000L) {
                    continue;
                }
                if (r.ids.contains(newId)) {
                    return;
                }
                if (r.ids.isEmpty() && msg != null && msg.date > 0) {
                    r.postDate = msg.date;
                    r.deleteAt = msg.date + r.seconds;
                }
                if (groupedId != 0) {
                    r.groupedId = groupedId;
                }
                r.ids.add(newId);
                changed = true;
                break;
            }
            if (changed) {
                save();
            }
        }
        if (changed) {
            scheduleAlarm(ApplicationLoader.applicationContext);
        }
    }

    /** Kutilayotgan o'chirishlar soni */
    public static int pendingCount() {
        synchronized (sync) {
            return recs().size();
        }
    }

    /** Sozlamalarda ko'rsatish uchun ro'yxat: "Kanal — 12.10 21:00" */
    public static ArrayList<String> describePending() {
        ArrayList<String> out = new ArrayList<>();
        synchronized (sync) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            for (Rec r : recs()) {
                c.setTimeInMillis(r.deleteAt * 1000L);
                out.add((TextUtils.isEmpty(r.title) ? String.valueOf(r.did) : r.title) + " — "
                        + String.format(Locale.US, "%02d.%02d %02d:%02d", c.get(java.util.Calendar.DAY_OF_MONTH), c.get(java.util.Calendar.MONTH) + 1,
                        c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
                        + (r.scheduled ? MgLang.t(" (rejalashtirilgan post)") : ""));
            }
        }
        return out;
    }

    /** Bitta yozuvni bekor qiladi (describePending tartibidagi indeks) */
    public static void cancel(int index) {
        synchronized (sync) {
            if (index >= 0 && index < recs().size()) {
                recs().remove(index);
                save();
            }
        }
        scheduleAlarm(ApplicationLoader.applicationContext);
    }

    public static void cancelAll() {
        synchronized (sync) {
            recs().clear();
            arms.clear();
            save();
        }
        scheduleAlarm(ApplicationLoader.applicationContext);
    }

    public static String durationName(int seconds) {
        int h = seconds / 3600;
        if (h >= 24 && h % 24 == 0) {
            int d = h / 24;
            return d == 1 ? MgLang.t("24 soat (1/24)") : d == 2 ? MgLang.t("48 soat (1/48)") : d == 3 ? MgLang.t("72 soat (1/72)") : d + MgLang.t(" kun");
        }
        if (h > 0) {
            return h + MgLang.t(" soat");
        }
        return Math.max(1, seconds / 60) + MgLang.t(" daqiqa");
    }

    // ------------------------------------------------------------------ o'chirish

    /** Vaqti kelgan postlarni o'chiradi (istalgan oqimdan chaqirish mumkin) */
    public static void check() {
        ArrayList<Rec> due = new ArrayList<>();
        synchronized (sync) {
            for (Rec r : recs()) {
                if (inFlight.contains(r.id)) {
                    continue;
                }
                if (r.deleteAt <= now(r.account) && UserConfig.getInstance(r.account).isClientActivated()) {
                    due.add(r);
                    inFlight.add(r.id);
                }
            }
        }
        for (Rec r : due) {
            execute(r);
        }
        scheduleAlarm(ApplicationLoader.applicationContext);
    }

    private static TLRPC.InputPeer inputPeer(Rec r) {
        if (r.did < 0) {
            if (r.channel) {
                TLRPC.TL_inputPeerChannel p = new TLRPC.TL_inputPeerChannel();
                p.channel_id = -r.did;
                p.access_hash = r.accessHash;
                return p;
            }
            TLRPC.TL_inputPeerChat p = new TLRPC.TL_inputPeerChat();
            p.chat_id = -r.did;
            return p;
        }
        TLRPC.TL_inputPeerUser p = new TLRPC.TL_inputPeerUser();
        p.user_id = r.did;
        p.access_hash = r.accessHash;
        return p;
    }

    private static void execute(Rec r) {
        if (!r.ids.isEmpty()) {
            deleteIds(r, new ArrayList<>(r.ids));
            return;
        }
        // ID noma'lum (rejalashtirilgan post yoki ilova yuborish paytida yopilgan) — tarixdan topamiz
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = inputPeer(r);
        req.offset_date = r.postDate + (r.scheduled ? 180 : 1800);
        req.limit = 40;
        ConnectionsManager.getInstance(r.account).sendRequest(req, (res, err) -> {
            if (!(res instanceof TLRPC.messages_Messages)) {
                failed(r, err);
                return;
            }
            ArrayList<TLRPC.Message> msgs = ((TLRPC.messages_Messages) res).messages;
            int from = r.postDate - 5;
            int to = r.postDate + (r.scheduled ? 180 : 1800);
            ArrayList<TLRPC.Message> cand = new ArrayList<>();
            for (TLRPC.Message m : msgs) {
                if (m != null && m.out && !(m instanceof TLRPC.TL_messageService) && m.date >= from && m.date <= to) {
                    cand.add(m);
                }
            }
            ArrayList<Integer> ids = new ArrayList<>();
            TLRPC.Message anchor = null;
            if (!TextUtils.isEmpty(r.textKey)) {
                for (TLRPC.Message m : cand) {
                    String k = textKey(m.message);
                    if (!k.isEmpty() && (k.startsWith(r.textKey) || r.textKey.startsWith(k))) {
                        anchor = m;
                        break;
                    }
                }
            } else {
                int bestDiff = Integer.MAX_VALUE;
                for (TLRPC.Message m : cand) {
                    int diff = Math.abs(m.date - r.postDate);
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        anchor = m;
                    }
                }
            }
            if (anchor != null) {
                if (anchor.grouped_id != 0) {
                    for (TLRPC.Message m : cand) {
                        if (m.grouped_id == anchor.grouped_id) {
                            ids.add(m.id);
                        }
                    }
                } else {
                    ids.add(anchor.id);
                    // bir yuborishda bir nechta xabar ketgan bo'lsa (uzun matn bo'laklari), yonidagilarni ham olamiz
                    for (TLRPC.Message m : cand) {
                        if (ids.size() >= Math.max(1, r.expected)) {
                            break;
                        }
                        if (m.id != anchor.id && Math.abs(m.date - anchor.date) <= 3 && m.grouped_id == 0) {
                            ids.add(m.id);
                        }
                    }
                }
            }
            if (ids.isEmpty()) {
                // post topilmadi: ehtimol qo'lda o'chirilgan yoki hali chiqmagan
                if (now(r.account) - r.deleteAt > 6 * 3600) {
                    finish(r, false);
                } else {
                    failed(r, null);
                }
                return;
            }
            deleteIds(r, ids);
        });
    }

    private static void deleteIds(Rec r, ArrayList<Integer> ids) {
        if (r.did < 0 && r.channel) {
            TLRPC.TL_channels_deleteMessages req = new TLRPC.TL_channels_deleteMessages();
            TLRPC.TL_inputChannel ch = new TLRPC.TL_inputChannel();
            ch.channel_id = -r.did;
            ch.access_hash = r.accessHash;
            req.channel = ch;
            req.id = ids;
            ConnectionsManager.getInstance(r.account).sendRequest(req, (res, err) -> onDeleted(r, ids, res, err));
        } else {
            TLRPC.TL_messages_deleteMessages req = new TLRPC.TL_messages_deleteMessages();
            req.revoke = true;
            req.id = ids;
            ConnectionsManager.getInstance(r.account).sendRequest(req, (res, err) -> onDeleted(r, ids, res, err));
        }
    }

    private static void onDeleted(Rec r, ArrayList<Integer> ids, Object res, TLRPC.TL_error err) {
        if (err == null) {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    MessagesController.getInstance(r.account).deleteMessages(ids, null, null, r.did, 0, false, 0, true);
                } catch (Throwable ignore) {
                }
            });
            finish(r, true);
        } else if (err.text != null && (err.text.contains("FORBIDDEN") || err.text.contains("MESSAGE_ID_INVALID") || err.text.contains("CHANNEL_PRIVATE") || err.text.contains("CHAT_ADMIN_REQUIRED"))) {
            finish(r, false);
        } else {
            failed(r, err);
        }
    }

    private static void failed(Rec r, TLRPC.TL_error err) {
        synchronized (sync) {
            inFlight.remove(r.id);
            r.tries++;
            // tarmoq bo'lmasa keyinroq qayta urinadi; 3 kundan keyin voz kechadi
            if (r.tries > 40 || now(r.account) - r.deleteAt > 3 * 86400) {
                recs().remove(r);
            } else {
                r.deleteAt = Math.max(r.deleteAt, now(r.account) + Math.min(30 * 60, 60 * r.tries));
            }
            save();
        }
        scheduleAlarm(ApplicationLoader.applicationContext);
    }

    private static void finish(Rec r, boolean ok) {
        synchronized (sync) {
            inFlight.remove(r.id);
            recs().remove(r);
            save();
        }
        scheduleAlarm(ApplicationLoader.applicationContext);
    }

    // ------------------------------------------------------------------ budilnik

    private static PendingIntent pending(Context ctx) {
        Intent i = new Intent(ctx, MgAutoDelete.class);
        i.setAction(ACTION);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(ctx, REQ, i, flags);
    }

    public static void scheduleAlarm(Context ctx) {
        if (ctx == null) {
            return;
        }
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }
            long next = Long.MAX_VALUE;
            synchronized (sync) {
                for (Rec r : recs()) {
                    next = Math.min(next, r.deleteAt * 1000L);
                }
            }
            PendingIntent pi = pending(ctx);
            if (next == Long.MAX_VALUE) {
                am.cancel(pi);
                return;
            }
            next = Math.max(next, System.currentTimeMillis() + 5_000);
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, next, pi);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static boolean tickerStarted;

    /** Ilova ochilganda chaqiriladi: kechikkanlarni o'chiradi va har 5 daqiqada tekshiradi */
    public static void start() {
        if (tickerStarted) {
            return;
        }
        tickerStarted = true;
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
                AndroidUtilities.runOnUIThread(this, 5 * 60_000L);
            }
        };
        AndroidUtilities.runOnUIThread(tick, 5_000);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) {
            return;
        }
        final PendingResult result = goAsync();
        AndroidUtilities.runOnUIThread(() -> {
            try {
                ApplicationLoader.postInitApplication();
                // ulanish o'rnatilishi uchun biroz kutib, keyin o'chiramiz
                AndroidUtilities.runOnUIThread(MgAutoDelete::check, 3_000);
            } catch (Throwable e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    result.finish();
                } catch (Throwable ignore) {
                }
            }, 25_000);
        });
    }
}
