/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Iborali yashirin chatlar (Telegram macOS Beta'dagi "Hidden Chats" kabi).
 *
 * - Har bir maxfiy ibora o'zining alohida chatlar to'plamini ("makon") ochadi; yangi ibora bo'sh boshlanadi.
 * - Ibora hech qayerda saqlanmaydi: faqat tasodifiy tuz va PBKDF2 natijasi (tekshiruvchi) saqlanadi.
 * - Makondagi chatlar hamma joyda yashirin: ro'yxat, jildlar, qidiruv, bildirishnomalar, ulashish oynasi.
 * - Rejimdan tashqarida bu chatlardan kelgan qo'ng'iroqlar jimgina rad etiladi.
 * - Rejim ekran o'chganda, ilova fonga o'tganda yoki 5 daqiqa harakatsizlikdan keyin o'zi yopiladi.
 * - Hammasi faqat shu telefonda saqlanadi va boshqa qurilmalar bilan sinxronlanmaydi.
 */
public final class MgHiddenSpaces {

    public static final int MIN_PHRASE_LENGTH = 8;
    private static final int ITERATIONS = 12000;
    private static final long IDLE_TIMEOUT_MS = 5 * 60_000L;

    private static final Object sync = new Object();
    private static final HashMap<String, HashSet<Long>> dialogCache = new HashMap<>();
    private static final HashMap<Integer, HashSet<Long>> unionCache = new HashMap<>();

    private static volatile String activeSpace;
    private static volatile int activeAccount = -1;
    private static volatile long lastInteraction;
    private static boolean screenReceiverRegistered;

    private MgHiddenSpaces() {
    }

    // ---------------------------------------------------------------- makonlar ro'yxati

    private static ArrayList<String> ids() {
        ArrayList<String> out = new ArrayList<>();
        String raw = MgConfig.getString("hs_list", "");
        if (!TextUtils.isEmpty(raw)) {
            for (String s : raw.split(",")) {
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    public static boolean hasAny() {
        return !ids().isEmpty();
    }

    public static int count() {
        return ids().size();
    }

    /** Qidiruvga yozilgan matn ibora bo'lishi mumkinmi (tez tekshiruv, PBKDF2'siz) */
    public static boolean mayBePhrase(String text) {
        return text != null && text.trim().length() >= MIN_PHRASE_LENGTH && hasAny();
    }

    private static String normalize(String phrase) {
        return phrase == null ? "" : phrase.trim();
    }

    private static byte[] derive(String phrase, byte[] salt) throws Exception {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(normalize(phrase).toCharArray(), salt, ITERATIONS, 256);
        try {
            return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    /**
     * Ibora qaysi makonni ochishini topadi (sekin — fon oqimida chaqiring).
     * Topilmasa null.
     */
    public static String match(String phrase) {
        String p = normalize(phrase);
        if (p.length() < MIN_PHRASE_LENGTH) {
            return null;
        }
        for (String id : ids()) {
            try {
                String salt = MgConfig.getString("hs_salt_" + id, null);
                String ver = MgConfig.getString("hs_ver_" + id, null);
                if (salt == null || ver == null) {
                    continue;
                }
                if (MessageDigest.isEqual(unhex(ver), derive(p, unhex(salt)))) {
                    return id;
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return null;
    }

    /** Yangi ibora qo'shadi (ibora allaqachon bor bo'lsa — o'sha makon). Sekin: fon oqimida chaqiring. */
    public static String create(String phrase) {
        String existing = match(phrase);
        if (existing != null) {
            return existing;
        }
        try {
            SecureRandom rnd = new SecureRandom();
            byte[] salt = new byte[16];
            rnd.nextBytes(salt);
            byte[] idb = new byte[6];
            rnd.nextBytes(idb);
            String id = hex(idb);
            MgConfig.setString("hs_salt_" + id, hex(salt));
            MgConfig.setString("hs_ver_" + id, hex(derive(phrase, salt)));
            ArrayList<String> list = ids();
            list.add(id);
            MgConfig.setString("hs_list", TextUtils.join(",", list));
            return id;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    // ---------------------------------------------------------------- makondagi chatlar

    private static String dlgKey(int account, String space) {
        return "hs_dlg_" + account + "_" + space;
    }

    public static HashSet<Long> dialogs(int account, String space) {
        if (space == null) {
            return new HashSet<>();
        }
        synchronized (sync) {
            String k = dlgKey(account, space);
            HashSet<Long> cached = dialogCache.get(k);
            if (cached != null) {
                return cached;
            }
            HashSet<Long> out = new HashSet<>();
            String raw = MgConfig.getString(k, null);
            String plain = raw == null ? null : (raw.startsWith("e1:") ? MgSecure.decrypt(raw) : raw);
            if (raw != null && plain == null) {
                return out; // Keystore vaqtincha ishlamadi — keshlamaymiz
            }
            if (!TextUtils.isEmpty(plain)) {
                for (String s : plain.split(",")) {
                    try {
                        out.add(Long.parseLong(s));
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            dialogCache.put(k, out);
            return out;
        }
    }

    private static void save(int account, String space, Collection<Long> set) {
        String plain = TextUtils.join(",", set);
        String enc = MgSecure.encrypt(plain);
        MgConfig.setString(dlgKey(account, space), enc != null ? enc : plain);
    }

    /** Barcha iboralardagi chatlar — hamma joyda yashirish uchun */
    public static HashSet<Long> all(int account) {
        synchronized (sync) {
            HashSet<Long> cached = unionCache.get(account);
            if (cached != null) {
                return cached;
            }
            HashSet<Long> out = new HashSet<>();
            for (String id : ids()) {
                out.addAll(dialogs(account, id));
            }
            unionCache.put(account, out);
            return out;
        }
    }

    public static boolean contains(int account, long dialogId) {
        return dialogId != 0 && all(account).contains(dialogId);
    }

    public static void add(int account, String space, Collection<Long> ids) {
        if (space == null || ids == null || ids.isEmpty()) {
            return;
        }
        synchronized (sync) {
            String k = dlgKey(account, space);
            if (dialogCache.get(k) == null && MgConfig.getString(k, null) != null && dialogs(account, space).isEmpty()
                    && dialogCache.get(k) == null) {
                FileLog.e("MgHiddenSpaces: space list unavailable, not overwriting");
                return;
            }
            HashSet<Long> set = new HashSet<>(dialogs(account, space));
            set.addAll(ids);
            save(account, space, set);
            dialogCache.put(k, set);
            unionCache.remove(account);
        }
        notifyChanged(account);
    }

    public static void remove(int account, String space, Collection<Long> ids) {
        if (space == null || ids == null || ids.isEmpty()) {
            return;
        }
        synchronized (sync) {
            HashSet<Long> set = new HashSet<>(dialogs(account, space));
            set.removeAll(ids);
            save(account, space, set);
            dialogCache.put(dlgKey(account, space), set);
            unionCache.remove(account);
        }
        notifyChanged(account);
    }

    /** Barcha iboralarni o'chiradi — chatlar yana oddiy ro'yxatda ko'rinadi */
    public static void deleteAll() {
        synchronized (sync) {
            for (String id : ids()) {
                MgConfig.setString("hs_salt_" + id, null);
                MgConfig.setString("hs_ver_" + id, null);
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    MgConfig.setString(dlgKey(a, id), null);
                }
            }
            MgConfig.setString("hs_list", null);
            dialogCache.clear();
            unionCache.clear();
        }
        leave();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.isValidAccount(a) && UserConfig.getInstance(a).isClientActivated()) {
                notifyChanged(a);
            }
        }
    }

    /** Akkauntdan chiqilganda — shu akkauntning yashirin chatlari tozalanadi */
    public static void clearAccount(int account) {
        synchronized (sync) {
            for (String id : ids()) {
                MgConfig.setString(dlgKey(account, id), null);
                dialogCache.remove(dlgKey(account, id));
            }
            unionCache.remove(account);
        }
        if (activeAccount == account) {
            leave();
        }
    }

    private static void notifyChanged(int account) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
            } catch (Throwable ignore) {
            }
        });
    }

    // ---------------------------------------------------------------- rejim

    public static String active() {
        return activeSpace;
    }

    public static void enter(int account, String space) {
        activeSpace = space;
        activeAccount = account;
        touch();
        ensureScreenReceiver();
        AndroidUtilities.cancelRunOnUIThread(idleCheck);
        AndroidUtilities.runOnUIThread(idleCheck, 30_000);
    }

    /** Rejimni yopadi: ochiq yashirin oynalar ham yopiladi */
    public static void leave() {
        if (activeSpace == null) {
            return;
        }
        activeSpace = null;
        activeAccount = -1;
        AndroidUtilities.cancelRunOnUIThread(idleCheck);
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mgHiddenSpaceClosed));
    }

    public static void touch() {
        lastInteraction = SystemClock.elapsedRealtime();
    }

    private static final Runnable backgroundLeave = MgHiddenSpaces::leave;

    /** Ilova fonga o'tdi: 1 daqiqadan keyin rejim yopiladi (galereya/kamera ochilganda darhol yopilmasligi uchun) */
    public static void onAppBackground() {
        if (activeSpace != null) {
            AndroidUtilities.cancelRunOnUIThread(backgroundLeave);
            AndroidUtilities.runOnUIThread(backgroundLeave, 60_000);
        }
    }

    public static void onAppForeground() {
        AndroidUtilities.cancelRunOnUIThread(backgroundLeave);
        touch();
    }

    private static final Runnable idleCheck = new Runnable() {
        @Override
        public void run() {
            if (activeSpace == null) {
                return;
            }
            if (SystemClock.elapsedRealtime() - lastInteraction >= IDLE_TIMEOUT_MS) {
                leave();
            } else {
                AndroidUtilities.runOnUIThread(this, 30_000);
            }
        }
    };

    private static void ensureScreenReceiver() {
        if (screenReceiverRegistered) {
            return;
        }
        try {
            IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            ApplicationLoader.applicationContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    leave();
                }
            }, f);
            screenReceiverRegistered = true;
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /** Rejimdan tashqarida yashirin chatdan kelgan qo'ng'iroq jimgina rad etiladi */
    public static boolean shouldDeclineCall(int account, long userId) {
        if (userId == 0 || !contains(account, userId)) {
            return false;
        }
        String a = activeSpace;
        return a == null || activeAccount != account || !dialogs(account, a).contains(userId);
    }
}
