/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Chat vositalari: eksport (TXT) va statistika.
 */
public class MgChatTools {

    public static final int MENU_EXPORT = 9010;
    public static final int MENU_STATS = 9011;

    private static String senderName(MessagesController mc, MessageObject m) {
        long from = m.getFromChatId();
        if (from > 0) {
            TLRPC.User u = mc.getUser(from);
            if (u != null) {
                return UserObject.getUserName(u);
            }
        } else if (from < 0) {
            TLRPC.Chat c = mc.getChat(-from);
            if (c != null && c.title != null) {
                return c.title;
            }
        }
        return m.isOut() ? "Siz" : "Noma'lum";
    }

    private static String mediaLabel(MessageObject m) {
        if (m.isPhoto()) return "[Rasm]";
        if (m.isRoundVideo()) return "[Doira video]";
        if (m.isVideo()) return "[Video]";
        if (m.isVoice()) return "[Ovozli xabar]";
        if (m.isMusic()) return "[Musiqa]";
        if (m.isSticker()) return "[Stiker]";
        if (m.isGif()) return "[GIF]";
        if (m.messageOwner != null && m.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) return "[Fayl]";
        return null;
    }

    private static ArrayList<MessageObject> chronological(ArrayList<MessageObject> loaded) {
        ArrayList<MessageObject> list = new ArrayList<>();
        for (MessageObject m : loaded) {
            if (m != null && m.messageOwner != null && m.getId() != 0 && m.messageOwner.date > 0) {
                list.add(m);
            }
        }
        Collections.sort(list, (a, b) -> Integer.compare(a.messageOwner.date, b.messageOwner.date));
        return list;
    }

    /** Yuklangan xabarlarni TXT faylga eksport qilib ulashish */
    public static void export(BaseFragment fragment, String chatTitle, ArrayList<MessageObject> loaded) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        ArrayList<MessageObject> list = chronological(loaded);
        if (list.isEmpty()) {
            BulletinFactory.of(fragment).createErrorBulletin("Eksport uchun xabar yo'q").show();
            return;
        }
        MessagesController mc = fragment.getMessagesController();
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US);
        StringBuilder sb = new StringBuilder();
        sb.append("MilliyGram — chat eksporti\n");
        sb.append("Chat: ").append(chatTitle).append('\n');
        sb.append("Xabarlar: ").append(list.size()).append(" ta (ekranda yuklanganlari)\n");
        sb.append("Sana: ").append(df.format(new Date())).append("\n");
        sb.append("========================================\n\n");
        for (MessageObject m : list) {
            sb.append('[').append(df.format(new Date(m.messageOwner.date * 1000L))).append("] ");
            sb.append(senderName(mc, m)).append(": ");
            String media = mediaLabel(m);
            String text = m.messageOwner.message;
            if (media != null) {
                sb.append(media);
                if (!TextUtils.isEmpty(text)) {
                    sb.append(' ').append(text);
                }
            } else if (!TextUtils.isEmpty(text)) {
                sb.append(text);
            } else if (m.messageText != null) {
                sb.append(m.messageText);
            }
            sb.append('\n');
        }
        try {
            File dir = AndroidUtilities.getSharingDirectory();
            dir.mkdirs();
            String safe = chatTitle == null ? "chat" : chatTitle.replaceAll("[^\\p{L}\\p{N}_ -]", "").trim();
            if (safe.isEmpty()) {
                safe = "chat";
            }
            File file = new File(dir, "MilliyGram_" + safe + ".txt");
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                w.write(sb.toString());
            }
            Uri uri = FileProvider.getUriForFile(fragment.getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fragment.getParentActivity().startActivity(Intent.createChooser(intent, "Chat eksporti"));
        } catch (Throwable e) {
            FileLog.e(e);
            BulletinFactory.of(fragment).createErrorBulletin("Eksport qilib bo'lmadi").show();
        }
    }

    /** Yuklangan xabarlar bo'yicha statistika */
    public static void stats(BaseFragment fragment, ArrayList<MessageObject> loaded) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        ArrayList<MessageObject> list = chronological(loaded);
        if (list.isEmpty()) {
            BulletinFactory.of(fragment).createErrorBulletin("Statistika uchun xabar yo'q").show();
            return;
        }
        MessagesController mc = fragment.getMessagesController();
        HashMap<String, Integer> bySender = new HashMap<>();
        int[] hours = new int[24];
        int photos = 0, videos = 0, voices = 0, stickers = 0, files = 0, links = 0, words = 0, out = 0;
        for (MessageObject m : list) {
            String name = senderName(mc, m);
            Integer c = bySender.get(name);
            bySender.put(name, c == null ? 1 : c + 1);
            hours[new Date(m.messageOwner.date * 1000L).getHours()]++;
            if (m.isOut()) out++;
            if (m.isPhoto()) photos++;
            else if (m.isVideo() || m.isRoundVideo() || m.isGif()) videos++;
            else if (m.isVoice()) voices++;
            else if (m.isSticker()) stickers++;
            else if (m.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) files++;
            String t = m.messageOwner.message;
            if (!TextUtils.isEmpty(t)) {
                words += t.trim().split("\\s+").length;
                if (t.contains("http://") || t.contains("https://") || t.contains("t.me/")) links++;
            }
        }
        int bestHour = 0;
        for (int h = 1; h < 24; h++) {
            if (hours[h] > hours[bestHour]) bestHour = h;
        }
        ArrayList<Map.Entry<String, Integer>> top = new ArrayList<>(bySender.entrySet());
        Collections.sort(top, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        StringBuilder sb = new StringBuilder();
        sb.append("📅 ").append(df.format(new Date(list.get(0).messageOwner.date * 1000L)))
                .append(" — ").append(df.format(new Date(list.get(list.size() - 1).messageOwner.date * 1000L))).append("\n\n");
        sb.append("💬 Jami xabarlar: ").append(list.size()).append("\n");
        sb.append("📤 Siz yozganlar: ").append(out).append("\n");
        sb.append("📝 So'zlar: ").append(words).append("\n\n");
        sb.append("🖼 Rasmlar: ").append(photos).append("   🎬 Videolar: ").append(videos).append("\n");
        sb.append("🎙 Ovozli: ").append(voices).append("   😀 Stikerlar: ").append(stickers).append("\n");
        sb.append("📎 Fayllar: ").append(files).append("   🔗 Havolalar: ").append(links).append("\n\n");
        sb.append("⏰ Eng faol vaqt: ").append(String.format(Locale.US, "%02d:00–%02d:00", bestHour, (bestHour + 1) % 24)).append("\n\n");
        sb.append("🏆 Eng faollar:\n");
        String[] medals = {"🥇", "🥈", "🥉", "4.", "5."};
        for (int i = 0; i < Math.min(5, top.size()); i++) {
            sb.append(medals[i]).append(' ').append(top.get(i).getKey()).append(" — ").append(top.get(i).getValue()).append('\n');
        }
        sb.append("\nStatistika ekranda yuklangan xabarlar bo'yicha. Ko'proq xabar uchun chatni yuqoriga aylantiring.");
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle("📊 Chat statistikasi");
        builder.setMessage(sb.toString());
        builder.setPositiveButton("OK", null);
        builder.setNeutralButton("Nusxalash", (dialog, which) -> {
            AndroidUtilities.addToClipboard(sb.toString());
            BulletinFactory.of(fragment).createCopyBulletin("Statistika nusxalandi").show();
        });
        fragment.showDialog(builder.create());
    }
}
