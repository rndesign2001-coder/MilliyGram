/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Xabarni o'chirishda yuklangan fayl bilan ishlash:
 *  - "Xabar faylini ham o'chirish" o'chirilgan bo'lsa — xabar o'chadi, telefondagi fayl qoladi;
 *  - "Faqat faylni o'chirish" — xabar qoladi, telefondagi fayl o'chiriladi (keyin qayta yuklash mumkin).
 */
public final class MgMediaDelete {

    private static final long KEEP_WINDOW_MS = 2 * 60_000L;
    /** media ID -> qachongacha faylni saqlab qolish kerak */
    private static final HashMap<Long, Long> keep = new HashMap<>();

    private MgMediaDelete() {
    }

    private static long mediaId(TLRPC.Message m) {
        if (m == null || m.media == null) {
            return 0;
        }
        TLRPC.Document d = MessageObject.getDocument(m);
        if (d != null) {
            return d.id;
        }
        TLRPC.Photo p = MessageObject.getPhoto(m);
        return p != null ? p.id : 0;
    }

    /** Xabarda yuklab olinadigan media bormi (stiker/emoji emas) */
    public static boolean hasMedia(MessageObject mo) {
        if (mo == null || mo.messageOwner == null || mo.messageOwner.media == null) {
            return false;
        }
        if (mo.isSticker() || mo.isAnimatedSticker() || mo.isAnimatedEmoji() || mo.type == MessageObject.TYPE_EMOJIS) {
            return false;
        }
        return mediaId(mo.messageOwner) != 0;
    }

    /** Telefonda shu xabarning yuklangan fayli bormi */
    public static boolean hasLocalFile(int account, MessageObject mo) {
        if (!hasMedia(mo)) {
            return false;
        }
        for (File f : files(account, mo)) {
            if (f != null && f.isFile()) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<File> files(int account, MessageObject mo) {
        ArrayList<File> out = new ArrayList<>();
        FileLoader fl = FileLoader.getInstance(account);
        TLRPC.Message m = mo.messageOwner;
        TLRPC.Document d = MessageObject.getDocument(m);
        if (d != null) {
            out.add(fl.getPathToAttach(d));
            out.add(fl.getPathToAttach(d, true));
        } else {
            TLRPC.Photo p = MessageObject.getPhoto(m);
            if (p != null && p.sizes != null) {
                for (TLRPC.PhotoSize s : p.sizes) {
                    if (s instanceof TLRPC.TL_photoStrippedSize || s instanceof TLRPC.TL_photoCachedSize) {
                        continue;
                    }
                    out.add(fl.getPathToAttach(s));
                    out.add(fl.getPathToAttach(s, true));
                }
            }
        }
        // o'zimiz yuborgan fayl: faqat Telegram keshidagi nusxa (galereyadagi asl fayl o'chirilmaydi)
        String attach = m.attachPath;
        File cache = FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE);
        if (!TextUtils.isEmpty(attach) && cache != null && attach.startsWith(cache.getAbsolutePath())) {
            out.add(new File(attach));
        }
        return out;
    }

    /** Keyingi o'chirishda shu xabarlarning fayllari telefonda qolsin */
    public static void keepFilesOf(List<MessageObject> list) {
        long until = System.currentTimeMillis() + KEEP_WINDOW_MS;
        synchronized (keep) {
            for (MessageObject mo : list) {
                long id = mo == null ? 0 : mediaId(mo.messageOwner);
                if (id != 0) {
                    keep.put(id, until);
                }
            }
        }
    }

    /** MessagesStorage: bu xabarning faylini o'chirmaslik kerakmi */
    public static boolean shouldKeep(TLRPC.Message m) {
        long id = mediaId(m);
        if (id == 0) {
            return false;
        }
        synchronized (keep) {
            if (keep.isEmpty()) {
                return false;
            }
            Long until = keep.get(id);
            if (until == null) {
                return false;
            }
            if (until < System.currentTimeMillis()) {
                keep.remove(id);
                return false;
            }
            return true;
        }
    }

    /** Xabarlarni qoldirib, faqat telefondagi fayllarini o'chiradi */
    public static void deleteFilesOnly(int account, List<MessageObject> list, Utilities.Callback<Integer> done) {
        final ArrayList<MessageObject> targets = new ArrayList<>();
        final ArrayList<File> toDelete = new ArrayList<>();
        for (MessageObject mo : list) {
            if (!hasMedia(mo) || mo.isSending()) {
                continue;
            }
            targets.add(mo);
            TLRPC.Document d = mo.getDocument();
            if (d != null) {
                FileLoader.getInstance(account).cancelLoadFile(d);
            }
            toDelete.addAll(files(account, mo));
        }
        Utilities.globalQueue.postRunnable(() -> {
            int removed = 0;
            for (File f : toDelete) {
                try {
                    if (f != null && f.isFile() && f.delete()) {
                        removed++;
                    }
                } catch (Throwable ignore) {
                }
            }
            final int count = removed;
            AndroidUtilities.runOnUIThread(() -> {
                File cache = FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE);
                for (MessageObject mo : targets) {
                    mo.mediaExists = false;
                    String attach = mo.messageOwner.attachPath;
                    if (!TextUtils.isEmpty(attach) && cache != null && attach.startsWith(cache.getAbsolutePath())) {
                        mo.messageOwner.attachPath = "";
                        mo.attachPathExists = false;
                    }
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateMessageMedia, mo.messageOwner);
                }
                if (done != null) {
                    done.run(count);
                }
            });
        });
    }
}
