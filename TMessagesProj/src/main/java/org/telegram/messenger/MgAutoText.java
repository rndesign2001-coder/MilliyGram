/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * G'oya Novagram (VipAds LLC, GPL v2) "Auto text" funksiyasidan olingan va MilliyGram uchun qayta yozilgan.
 */

package org.telegram.messenger;

import android.text.TextUtils;
import android.util.Base64;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Avto-imzo: tanlangan chatga yuboriladigan har bir xabar (va media izohi) oxiriga formatli imzo qo'shadi.
 * Imzo yuborish paytida qo'shiladi — xabar keyin tahrirlanmaydi, "tahrirlangan" belgisi chiqmaydi.
 */
public class MgAutoText {

    public static class Entry {
        public final boolean active;
        public final String text;
        public final ArrayList<TLRPC.MessageEntity> entities;

        Entry(boolean active, String text, ArrayList<TLRPC.MessageEntity> entities) {
            this.active = active;
            this.text = text == null ? "" : text;
            this.entities = entities == null ? new ArrayList<>() : entities;
        }
    }

    private static final Entry EMPTY = new Entry(false, "", null);
    private static final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    /** Avto-javob kabi xizmat xabarlariga imzo qo'shilmasligi uchun */
    public static boolean suppress;

    private static String key(int account, long dialogId) {
        return "autotext_" + account + "_" + dialogId;
    }

    public static Entry get(int account, long dialogId) {
        String k = key(account, dialogId);
        Entry e = cache.get(k);
        if (e != null) {
            return e;
        }
        e = load(k);
        cache.put(k, e);
        return e;
    }

    public static boolean isActive(int account, long dialogId) {
        Entry e = get(account, dialogId);
        return e.active && !TextUtils.isEmpty(e.text);
    }

    private static Entry load(String k) {
        String raw = MgConfig.getString(k, null);
        if (raw == null) {
            return EMPTY;
        }
        SerializedData data = null;
        try {
            data = new SerializedData(Base64.decode(raw, Base64.DEFAULT));
            boolean active = data.readInt32(false) == 1;
            String text = data.readString(false);
            int count = data.readInt32(false);
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                TLRPC.MessageEntity en = TLRPC.MessageEntity.TLdeserialize(data, data.readInt32(false), false);
                if (en != null) {
                    entities.add(en);
                }
            }
            return new Entry(active, text, entities);
        } catch (Throwable e) {
            FileLog.e(e);
            return EMPTY;
        } finally {
            if (data != null) {
                data.cleanup();
            }
        }
    }

    private static void write(SerializedData data, boolean active, String text, ArrayList<TLRPC.MessageEntity> entities) {
        data.writeInt32(active ? 1 : 0);
        data.writeString(text);
        data.writeInt32(entities.size());
        for (TLRPC.MessageEntity e : entities) {
            e.serializeToStream(data);
        }
    }

    public static void save(int account, long dialogId, boolean active, String text, ArrayList<TLRPC.MessageEntity> entities) {
        String k = key(account, dialogId);
        ArrayList<TLRPC.MessageEntity> ents = entities == null ? new ArrayList<>() : entities;
        if (TextUtils.isEmpty(text)) {
            MgConfig.setString(k, null);
            cache.put(k, EMPTY);
            return;
        }
        try {
            SerializedData calc = new SerializedData(true);
            write(calc, active, text, ents);
            SerializedData data = new SerializedData(calc.length());
            write(data, active, text, ents);
            MgConfig.setString(k, Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP));
            data.cleanup();
            calc.cleanup();
        } catch (Throwable e) {
            FileLog.e(e);
        }
        cache.put(k, new Entry(active, text, ents));
    }

    public static void setActive(int account, long dialogId, boolean active) {
        Entry e = get(account, dialogId);
        save(account, dialogId, active, e.text, e.entities);
    }

    private static TLRPC.MessageEntity copy(TLRPC.MessageEntity en) {
        SerializedData calc = null, data = null, in = null;
        try {
            calc = new SerializedData(true);
            en.serializeToStream(calc);
            data = new SerializedData(calc.length());
            en.serializeToStream(data);
            in = new SerializedData(data.toByteArray());
            return TLRPC.MessageEntity.TLdeserialize(in, in.readInt32(false), false);
        } catch (Throwable e) {
            return null;
        } finally {
            if (calc != null) calc.cleanup();
            if (data != null) data.cleanup();
            if (in != null) in.cleanup();
        }
    }

    private static boolean isSkippedDocument(TLRPC.Document d) {
        if (d == null || d.attributes == null) {
            return false;
        }
        if (MessageObject.isStickerDocument(d) || MessageObject.isAnimatedStickerDocument(d, true)) {
            return true;
        }
        for (TLRPC.DocumentAttribute a : d.attributes) {
            if (a instanceof TLRPC.TL_documentAttributeAudio && a.voice) {
                return true;
            }
            if (a instanceof TLRPC.TL_documentAttributeVideo && a.round_message) {
                return true;
            }
        }
        return false;
    }

    /** SendMessagesHelper.sendMessage() boshida chaqiriladi: kerak bo'lsa imzoni qo'shadi */
    public static void apply(int account, SendMessagesHelper.SendMessageParams p) {
        try {
            if (!suppress && p != null && p.retryMessageObject == null && p.peer > 0) {
                MgAutoAnswer.onUserReplied(account, p.peer);
            }
            if (suppress || p == null || p.retryMessageObject != null || p.sendingStory != null
                    || p.quick_reply_shortcut != null || p.poll != null || p.todo != null || p.location != null
                    || p.user != null || p.game != null || p.invoice != null) {
                return;
            }
            if (p.sendMessageChatArguments != null && p.sendMessageChatArguments.quickReplyShortcut != null) {
                return;
            }
            if (!isActive(account, p.peer)) {
                return;
            }
            Entry e = get(account, p.peer);
            MessagesController mc = MessagesController.getInstance(account);
            boolean media = p.photo != null || p.document != null;
            String base;
            int limit;
            if (media) {
                if (p.document != null && isSkippedDocument(p.document)) {
                    return;
                }
                base = p.caption == null ? "" : p.caption;
                boolean grouped = p.params != null && p.params.containsKey("groupId") && !"0".equals(p.params.get("groupId"));
                if (TextUtils.isEmpty(base) && grouped) {
                    return; // albomda izoh faqat bitta elementda
                }
                limit = mc.getCaptionMaxLengthLimit();
            } else {
                base = p.message;
                if (TextUtils.isEmpty(base) || base.startsWith("/")) {
                    return;
                }
                limit = mc.maxMessageLength;
            }
            if (base.endsWith(e.text)) {
                return; // allaqachon bor
            }
            String sep = base.isEmpty() || Character.isWhitespace(e.text.charAt(0)) ? "" : "\n\n";
            String result = base + sep + e.text;
            if (result.length() > limit) {
                return;
            }
            int offset = base.length() + sep.length();
            ArrayList<TLRPC.MessageEntity> ents = new ArrayList<>();
            if (p.entities != null) {
                ents.addAll(p.entities);
            }
            for (TLRPC.MessageEntity en : e.entities) {
                TLRPC.MessageEntity c = copy(en);
                if (c != null) {
                    c.offset += offset;
                    ents.add(c);
                }
            }
            if (media) {
                p.caption = result;
            } else {
                p.message = result;
            }
            p.entities = ents;
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
