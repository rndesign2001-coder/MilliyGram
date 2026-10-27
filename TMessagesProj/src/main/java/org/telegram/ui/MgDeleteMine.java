/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/**
 * Guruhdagi barcha o'z xabarlarimni o'chirish: Telegram qidiruvi orqali (faqat mening xabarlarim)
 * topiladi va 100 tadan bo'lib hamma uchun o'chiriladi.
 */
public final class MgDeleteMine {

    private static final int PAGE = 100;
    private static final int MAX = 20000;

    private MgDeleteMine() {
    }

    public static boolean canUse(TLRPC.Chat chat) {
        return chat != null && !ChatObject.isChannelAndNotMegaGroup(chat) && !ChatObject.isNotInChat(chat);
    }

    public static void start(BaseFragment f, int account, long dialogId) {
        if (f.getParentActivity() == null) {
            return;
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        if (!canUse(chat)) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(f.getParentActivity(), f.getResourceProvider());
        b.setTitle(MgLang.t("Barcha xabarlarimni o'chirish"));
        b.setMessage(MgLang.t("Shu guruhdagi BARCHA xabarlaringiz hamma uchun o'chiriladi. Bu amalni qaytarib bo'lmaydi.\n\nDavom etilsinmi?"));
        b.setPositiveButton(MgLang.t("O'chirish"), (d, w) -> run(f, account, dialogId));
        b.setNegativeButton(MgLang.t("Bekor qilish"), null);
        AlertDialog dialog = b.create();
        f.showDialog(dialog);
        android.view.View button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (button instanceof android.widget.TextView) {
            ((android.widget.TextView) button).setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_text_RedBold));
        }
    }

    private static void run(BaseFragment f, int account, long dialogId) {
        if (f.getParentActivity() == null) {
            return;
        }
        AlertDialog progress = new AlertDialog(f.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(true);
        final boolean[] cancelled = {false};
        progress.setOnCancelListener(d -> cancelled[0] = true);
        progress.show();
        collect(f, account, dialogId, 0, new ArrayList<>(), progress, cancelled);
    }

    /** Mening xabarlarimni sahifalab yig'ish (yangidan eskiga) */
    private static void collect(BaseFragment f, int account, long dialogId, int offsetId, ArrayList<Integer> ids, AlertDialog progress, boolean[] cancelled) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = mc.getInputPeer(dialogId);
        req.q = "";
        req.from_id = new TLRPC.TL_inputPeerSelf();
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.offset_id = offsetId;
        req.limit = PAGE;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (cancelled[0]) {
                return;
            }
            if (error != null || !(response instanceof TLRPC.messages_Messages)) {
                finish(f, progress, error != null ? error.text : null, 0);
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            int minId = Integer.MAX_VALUE;
            for (TLRPC.Message m : res.messages) {
                if (m != null && m.id > 0) {
                    ids.add(m.id);
                    minId = Math.min(minId, m.id);
                }
            }
            try {
                progress.setMessage(MgLang.t("Topildi: ") + ids.size());
            } catch (Throwable ignore) {
            }
            if (res.messages.size() < PAGE || minId == Integer.MAX_VALUE || ids.size() >= MAX) {
                if (ids.isEmpty()) {
                    finish(f, progress, null, 0);
                } else {
                    delete(f, account, dialogId, ids, 0, progress, cancelled);
                }
            } else {
                final int next = minId;
                AndroidUtilities.runOnUIThread(() -> collect(f, account, dialogId, next, ids, progress, cancelled), 300);
            }
        }));
    }

    /** 100 tadan bo'lib hamma uchun o'chirish */
    private static void delete(BaseFragment f, int account, long dialogId, ArrayList<Integer> ids, int from, AlertDialog progress, boolean[] cancelled) {
        if (cancelled[0]) {
            finish(f, progress, null, from);
            return;
        }
        if (from >= ids.size()) {
            finish(f, progress, null, ids.size());
            return;
        }
        int to = Math.min(ids.size(), from + PAGE);
        ArrayList<Integer> batch = new ArrayList<>(ids.subList(from, to));
        MessagesController.getInstance(account).deleteMessages(batch, null, null, dialogId, 0, true, 0);
        try {
            progress.setMessage(MgLang.t("O'chirilmoqda: ") + to + " / " + ids.size());
        } catch (Throwable ignore) {
        }
        AndroidUtilities.runOnUIThread(() -> delete(f, account, dialogId, ids, to, progress, cancelled), 700);
    }

    private static void finish(BaseFragment f, AlertDialog progress, String error, int count) {
        try {
            progress.dismiss();
        } catch (Throwable ignore) {
        }
        if (error != null) {
            BulletinFactory.of(f).createErrorBulletin(MgLang.t("Xatolik: ") + error).show();
        } else if (count == 0) {
            BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip, MgLang.t("Bu guruhda sizning xabaringiz topilmadi")).show();
        } else {
            BulletinFactory.of(f).createSimpleBulletin(R.raw.ic_delete, MgLang.t("O'chirildi: ") + count + MgLang.t(" ta xabar")).show();
        }
    }
}
