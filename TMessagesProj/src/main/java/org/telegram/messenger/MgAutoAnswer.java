/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * G'oya Novagram (VipAds LLC, GPL v2) "Auto-answer" funksiyasidan olingan va MilliyGram uchun qayta yozilgan.
 */

package org.telegram.messenger;

import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Avto-javob: shaxsiy chatga kelgan xabarga saqlangan matn bilan avtomatik javob beradi.
 * Har bir chatga belgilangan vaqt oralig'ida faqat bir marta javob beriladi.
 * Ilova ishlab turgan (Telegram serveriga ulangan) paytda ishlaydi.
 */
public class MgAutoAnswer {

    public static final int SCOPE_ALL = 0;
    public static final int SCOPE_STRANGERS = 1;
    public static final int SCOPE_CONTACTS = 2;
    public static final String[] SCOPE_NAMES = {"Barcha shaxsiy chatlar", "Faqat kontaktda yo'qlar", "Faqat kontaktlar"};

    /** Soatlarda; 0 — har chatga faqat bir marta (ro'yxat tozalanguncha) */
    public static final int[] COOLDOWNS = {0, 1, 3, 6, 12, 24};
    public static final String[] COOLDOWN_NAMES = {"Har chatga bir marta", "1 soatda bir marta", "3 soatda bir marta", "6 soatda bir marta", "12 soatda bir marta", "Kuniga bir marta"};

    public static boolean isEnabled() {
        return MgConfig.getBool("aa_on", false);
    }

    public static void setEnabled(boolean v) {
        MgConfig.setBool("aa_on", v);
        if (v) {
            clearAnswered();
        }
    }

    public static String getText() {
        return MgConfig.getString("aa_text", "");
    }

    public static void setText(String t) {
        MgConfig.setString("aa_text", t == null ? "" : t.trim());
    }

    public static int getScope() {
        return MgConfig.getInt("aa_scope", SCOPE_ALL);
    }

    public static int getCooldownHours() {
        return MgConfig.getInt("aa_cooldown", 12);
    }

    public static String cooldownName() {
        int h = getCooldownHours();
        for (int i = 0; i < COOLDOWNS.length; i++) {
            if (COOLDOWNS[i] == h) {
                return COOLDOWN_NAMES[i];
            }
        }
        return h + " soatda bir marta";
    }

    private static String doneKey(int account) {
        return "aa_done_" + account;
    }

    private static JSONObject loadDone(int account) {
        try {
            String s = MgConfig.getString(doneKey(account), null);
            return s == null ? new JSONObject() : new JSONObject(s);
        } catch (Throwable e) {
            return new JSONObject();
        }
    }

    public static void clearAnswered() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            MgConfig.setString(doneKey(a), null);
        }
    }

    /** Siz chatga o'zingiz yozganingizda chaqiriladi */
    public static void onUserReplied(int account, long dialogId) {
        if (!isEnabled()) {
            return;
        }
        if (dialogId <= 0) {
            return;
        }
        // Siz suhbatda faol bo'lsangiz, avto-javob sizning gapingizni to'xtatmasin: hisob qaytadan boshlanadi
        JSONObject done = loadDone(account);
        try {
            done.put(String.valueOf(dialogId), System.currentTimeMillis());
            MgConfig.setString(doneKey(account), done.toString());
        } catch (Throwable ignore) {
        }
    }

    /** NotificationsController.processNewMessages dan chaqiriladi (fon oqimi) */
    public static void onNewMessages(int account, ArrayList<MessageObject> messages) {
        if (!isEnabled() || messages == null || messages.isEmpty()) {
            return;
        }
        final String text = getText();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        final ArrayList<Long> targets = new ArrayList<>();
        final ArrayList<Integer> replyIds = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject m = messages.get(i);
            if (m == null || m.messageOwner == null || m.isOut() || m.messageOwner instanceof TLRPC.TL_messageService) {
                continue;
            }
            long did = m.getDialogId();
            if (did <= 0 || DialogObject.isEncryptedDialog(did) || targets.contains(did)) {
                continue;
            }
            // Eski xabarlarga (uzoq oflayndan keyin) javob bermaymiz
            int now = ConnectionsManager_now(account);
            if (now - m.messageOwner.date > 60 * 60) {
                continue;
            }
            targets.add(did);
            replyIds.add(m.getId());
        }
        if (targets.isEmpty()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            MessagesController mc = MessagesController.getInstance(account);
            long self = UserConfig.getInstance(account).getClientUserId();
            JSONObject done = loadDone(account);
            long nowMs = System.currentTimeMillis();
            int cooldown = getCooldownHours();
            boolean changed = false;
            for (int i = 0; i < targets.size(); i++) {
                long did = targets.get(i);
                TLRPC.User u = mc.getUser(did);
                if (u == null || u.bot || u.support || u.id == self || UserObject.isService(u.id) || UserObject.isDeleted(u)) {
                    continue;
                }
                int scope = getScope();
                if (scope == SCOPE_STRANGERS && u.contact || scope == SCOPE_CONTACTS && !u.contact) {
                    continue;
                }
                long last = done.optLong(String.valueOf(did), 0);
                if (last != 0 && (cooldown == 0 || nowMs - last < cooldown * 3600_000L)) {
                    continue;
                }
                try {
                    done.put(String.valueOf(did), nowMs);
                    changed = true;
                } catch (Throwable ignore) {
                }
                send(account, did, text);
            }
            if (changed) {
                pruneAndSave(account, done, nowMs);
            }
        });
    }

    private static int ConnectionsManager_now(int account) {
        return org.telegram.tgnet.ConnectionsManager.getInstance(account).getCurrentTime();
    }

    private static void pruneAndSave(int account, JSONObject done, long nowMs) {
        try {
            if (done.length() > 500) {
                Iterator<String> it = done.keys();
                ArrayList<String> old = new ArrayList<>();
                while (it.hasNext()) {
                    String k = it.next();
                    if (nowMs - done.optLong(k, 0) > 7 * 24 * 3600_000L) {
                        old.add(k);
                    }
                }
                for (String k : old) {
                    done.remove(k);
                }
            }
            MgConfig.setString(doneKey(account), done.toString());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void send(int account, long did, String text) {
        try {
            CharSequence[] arr = {text};
            ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(account).getEntities(arr, true);
            String msg = arr[0] == null ? text : arr[0].toString();
            int max = MessagesController.getInstance(account).maxMessageLength;
            if (msg.length() > max) {
                msg = msg.substring(0, max);
            }
            SendMessagesHelper.SendMessageParams p = SendMessagesHelper.SendMessageParams.of(msg, did, null, null, null, true, entities, null, null, true, 0, 0, null, false);
            MgAutoText.suppress = true;
            try {
                SendMessagesHelper.getInstance(account).sendMessage(p);
            } finally {
                MgAutoText.suppress = false;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
