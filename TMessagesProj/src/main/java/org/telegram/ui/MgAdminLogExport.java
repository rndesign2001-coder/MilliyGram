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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MgLang;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Kanal/guruh "So'nggi amallar" jurnalini matn fayliga eksport qilish (yuklangan voqealar).
 * O'chirilgan xabarlar uchun matn, yozilgan va o'chirilgan vaqti ham yoziladi.
 */
public final class MgAdminLogExport {

    public static final int MENU_ID = 9701;

    private MgAdminLogExport() {
    }

    private static String time(int date) {
        return LocaleController.formatDateTime(date, true);
    }

    public static void export(BaseFragment f, TLRPC.Chat chat, ArrayList<MessageObject> messages) {
        if (f.getParentActivity() == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(MgLang.t("So'nggi amallar")).append(": ").append(chat != null ? chat.title : "").append('\n');
        sb.append(MgLang.t("Eksport vaqti")).append(": ").append(time((int) (System.currentTimeMillis() / 1000))).append('\n');
        sb.append("MilliyGram\n");
        sb.append("========================================\n\n");
        int count = 0;
        // ro'yxatda eng yangisi boshida — xronologik tartibda yozamiz
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageObject m = messages.get(i);
            if (m == null || m.isDateObject || m.currentEvent == null) {
                continue;
            }
            TLRPC.TL_channelAdminLogEvent e = m.currentEvent;
            boolean deletedContent = e.action instanceof TLRPC.TL_channelAdminLogEventActionDeleteMessage && m.realDate != 0;
            CharSequence text = m.messageText;
            if (TextUtils.isEmpty(text) && m.caption != null) {
                text = m.caption;
            }
            String body = text == null ? "" : text.toString().replace('\n', ' ').trim();
            if (TextUtils.isEmpty(body)) {
                body = mediaName(m);
            }
            if (deletedContent) {
                sb.append("    ↳ ").append(body)
                        .append("   [").append(MgLang.t("yozilgan")).append(": ").append(time(m.realDate))
                        .append(" · ").append(MgLang.t("o'chirilgan")).append(": ").append(time(e.date)).append("]\n");
            } else {
                sb.append('[').append(time(e.date)).append("] ").append(body).append('\n');
                count++;
            }
        }
        if (count == 0) {
            BulletinFactory.of(f).createErrorBulletin(MgLang.t("Eksport uchun voqealar hali yuklanmagan")).show();
            return;
        }
        sb.append("\n========================================\n");
        sb.append(MgLang.t("Jami voqealar")).append(": ").append(count).append('\n');
        try {
            File dir = AndroidUtilities.getSharingDirectory();
            dir.mkdirs();
            String safe = chat == null || chat.title == null ? "log" : chat.title.replaceAll("[^\\p{L}\\p{N}_ -]", "").trim();
            if (safe.isEmpty()) {
                safe = "log";
            }
            File file = new File(dir, "MilliyGram_amallar_" + safe + ".txt");
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                w.write(sb.toString());
            }
            Uri uri = FileProvider.getUriForFile(f.getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            f.getParentActivity().startActivity(Intent.createChooser(intent, MgLang.t("So'nggi amallar eksporti")));
        } catch (Throwable ex) {
            FileLog.e(ex);
            BulletinFactory.of(f).createErrorBulletin(MgLang.t("Eksport qilib bo'lmadi")).show();
        }
    }

    private static String mediaName(MessageObject m) {
        if (m.isPhoto()) {
            return "🖼 " + MgLang.t("Rasm");
        } else if (m.isVideo()) {
            return "🎬 " + MgLang.t("Video");
        } else if (m.isVoice()) {
            return "🎤 " + MgLang.t("Ovozli xabar");
        } else if (m.isSticker() || m.isAnimatedSticker()) {
            return "🙂 " + MgLang.t("Stiker");
        } else if (m.isDocument()) {
            return "📎 " + MgLang.t("Fayl");
        }
        return "—";
    }
}
