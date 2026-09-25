/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/**
 * Belgilangan chatlar bilan ommaviy amallar:
 *  - kanal va guruhlardan birdaniga chiqish;
 *  - botlarni to'xtatish (bloklash), suhbatni tozalash va ro'yxatdan olib tashlash.
 * So'rovlar Telegram cheklovlariga tushmasligi uchun oraliq bilan ketma-ket yuboriladi.
 */
public class MgBulkActions {

    private static final int STEP_MS = 400;

    public static void leaveChats(BaseFragment f, int account, ArrayList<Long> dids, Runnable onStarted) {
        MessagesController mc = MessagesController.getInstance(account);
        final ArrayList<Long> targets = new ArrayList<>();
        int channels = 0, groups = 0, owned = 0;
        for (int i = 0; i < dids.size(); i++) {
            long did = dids.get(i);
            if (did >= 0) {
                continue;
            }
            TLRPC.Chat chat = mc.getChat(-did);
            if (chat == null) {
                continue;
            }
            if (chat.creator) {
                owned++; // o'zingiz yaratgan kanal/guruhni tasodifan tashlab ketmaslik uchun
                continue;
            }
            targets.add(did);
            if (ChatObject.isChannelAndNotMegaGroup(chat)) {
                channels++;
            } else {
                groups++;
            }
        }
        if (targets.isEmpty()) {
            BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip, owned > 0
                    ? "Belgilanganlar faqat o'zingiz yaratgan kanal/guruhlar — ular tashlab ketilmaydi"
                    : "Belgilanganlar orasida kanal yoki guruh yo'q").show();
            return;
        }
        StringBuilder msg = new StringBuilder();
        if (channels > 0) {
            msg.append(channels).append(" ta kanal");
        }
        if (groups > 0) {
            if (msg.length() > 0) {
                msg.append(" va ");
            }
            msg.append(groups).append(" ta guruh");
        }
        msg.append("dan chiqasizmi? Ular chatlar ro'yxatidan o'chadi.");
        if (owned > 0) {
            msg.append("\n\nO'zingiz yaratgan ").append(owned).append(" ta kanal/guruh tegilmaydi.");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(f.getParentActivity(), f.getResourceProvider());
        b.setTitle("Chiqish");
        b.setMessage(msg.toString());
        b.setPositiveButton("Chiqish", (d, w) -> {
            if (onStarted != null) {
                onStarted.run();
            }
            TLRPC.User self = mc.getUser(UserConfig.getInstance(account).getClientUserId());
            for (int i = 0; i < targets.size(); i++) {
                final long did = targets.get(i);
                AndroidUtilities.runOnUIThread(() -> {
                    TLRPC.Chat chat = mc.getChat(-did);
                    if (chat == null || ChatObject.isNotInChat(chat)) {
                        mc.deleteDialog(did, 0, false);
                    } else {
                        mc.deleteParticipantFromChat(-did, self, null, false, false);
                    }
                }, (long) i * STEP_MS);
            }
            final int n = targets.size();
            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
                try {
                    BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, n + " ta kanal/guruhdan chiqildi").show();
                } catch (Throwable ignore) {
                }
            }, (long) n * STEP_MS + 300);
        });
        b.setNegativeButton("Bekor qilish", null);
        AlertDialog dialog = b.create();
        f.showDialog(dialog);
        TextView button = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    public static void stopBots(BaseFragment f, int account, ArrayList<Long> dids, Runnable onStarted) {
        MessagesController mc = MessagesController.getInstance(account);
        final ArrayList<Long> targets = new ArrayList<>();
        for (int i = 0; i < dids.size(); i++) {
            long did = dids.get(i);
            if (did <= 0) {
                continue;
            }
            TLRPC.User user = mc.getUser(did);
            if (user != null && user.bot) {
                targets.add(did);
            }
        }
        if (targets.isEmpty()) {
            BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip, "Belgilanganlar orasida bot yo'q").show();
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(f.getParentActivity(), f.getResourceProvider());
        b.setTitle("Botlarni to'xtatish");
        b.setMessage(targets.size() + " ta bot to'xtatiladi (bloklanadi), ular bilan yozishmalar tozalanadi va ro'yxatdan o'chadi. Bot sizga boshqa xabar yubora olmaydi.\n\nKeyinroq botni qayta ishga tushirish uchun uni ochib \"Qayta ishga tushirish\"ni bosing.");
        b.setPositiveButton("To'xtatish", (d, w) -> {
            if (onStarted != null) {
                onStarted.run();
            }
            for (int i = 0; i < targets.size(); i++) {
                final long did = targets.get(i);
                AndroidUtilities.runOnUIThread(() -> {
                    mc.deleteDialog(did, 0, false);
                    mc.blockPeer(did);
                }, (long) i * STEP_MS);
            }
            final int n = targets.size();
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, n + " ta bot to'xtatildi va tozalandi").show();
                } catch (Throwable ignore) {
                }
            }, (long) n * STEP_MS + 300);
        });
        b.setNegativeButton("Bekor qilish", null);
        AlertDialog dialog = b.create();
        f.showDialog(dialog);
        TextView button = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }
}
