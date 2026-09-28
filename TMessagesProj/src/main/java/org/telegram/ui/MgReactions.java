/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Xabarga kim qanday reaksiya bildirganini ko'rsatadi (ID, ism, @username, vaqt) — nusxalash mumkin.
 * Telegram kanallarda (broadcast) reaksiya bildirganlarni hech kimga ko'rsatmaydi: u yerda faqat
 * reaksiyalar soni va Stars bilan reaksiya qilganlar (top) chiqadi.
 */
public class MgReactions {

    public static boolean hasReactions(MessageObject m) {
        return m != null && m.messageOwner != null && m.messageOwner.reactions != null && !m.messageOwner.reactions.results.isEmpty();
    }

    private static String reactionText(TLRPC.Reaction r) {
        if (r instanceof TLRPC.TL_reactionEmoji) {
            return ((TLRPC.TL_reactionEmoji) r).emoticon;
        } else if (r instanceof TLRPC.TL_reactionPaid) {
            return "⭐";
        } else if (r instanceof TLRPC.TL_reactionCustomEmoji) {
            return "✨";
        }
        return "•";
    }

    private static String peerText(MessagesController mc, TLRPC.Peer p) {
        long id = MessageObject.getPeerId(p);
        if (id > 0) {
            TLRPC.User u = mc.getUser(id);
            String un = u == null ? null : UserObject.getPublicUsername(u);
            return (u == null ? "?" : UserObject.getUserName(u)) + (un != null ? " @" + un : "") + " · ID " + id;
        }
        TLRPC.Chat c = mc.getChat(-id);
        String un = c == null ? null : ChatObject.getPublicUsername(c);
        return (c == null ? "?" : c.title) + (un != null ? " @" + un : "") + " · ID " + (-id);
    }

    private static String time(int date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date * 1000L);
        return String.format(java.util.Locale.US, "%02d.%02d %02d:%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static void show(ChatActivity f, MessageObject m) {
        if (!hasReactions(m) || f.getParentActivity() == null) {
            return;
        }
        int account = f.getCurrentAccount();
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.MessageReactions r = m.messageOwner.reactions;
        StringBuilder head = new StringBuilder();
        int total = 0;
        for (TLRPC.ReactionCount rc : r.results) {
            head.append(reactionText(rc.reaction)).append(" ").append(rc.count).append("   ");
            total += rc.count;
        }
        head.append("\n").append(MgLang.t("Jami")).append(": ").append(total).append("\n\n");
        TLRPC.Chat chat = f.getCurrentChat();
        boolean broadcast = chat != null && ChatObject.isChannelAndNotMegaGroup(chat);
        if (!broadcast && !r.can_see_list && r.recent_reactions != null && !r.recent_reactions.isEmpty()) {
            // shaxsiy chatlarda to'liq ro'yxat so'rovi yo'q — oxirgi reaksiyalar xabarning o'zida keladi
            StringBuilder sb = new StringBuilder(head);
            for (TLRPC.MessagePeerReaction pr : r.recent_reactions) {
                sb.append(reactionText(pr.reaction)).append("  ").append(peerText(mc, pr.peer_id))
                        .append(pr.date > 0 ? "  · " + time(pr.date) : "").append("\n");
            }
            showText(f, MgLang.t("Reaksiya bildirganlar") + " (" + r.recent_reactions.size() + ")", sb.toString().trim());
            return;
        }
        if (broadcast || !r.can_see_list) {
            StringBuilder sb = new StringBuilder(head);
            if (!r.top_reactors.isEmpty()) {
                sb.append(MgLang.t("⭐ Stars bilan reaksiya qilganlar:")).append("\n");
                for (TLRPC.MessageReactor mr : r.top_reactors) {
                    sb.append("• ").append(mr.anonymous || mr.peer_id == null ? MgLang.t("Yashirin foydalanuvchi") : peerText(mc, mr.peer_id))
                            .append(" — ").append(mr.count).append(" ⭐\n");
                }
                sb.append("\n");
            }
            sb.append(MgLang.t("Telegram kanallarda oddiy reaksiya bildirganlarni hech kimga, hatto egasiga ham ko'rsatmaydi — bu Telegram serverining qoidasi. Guruhlarda esa to'liq ro'yxat ko'rinadi."));
            showText(f, MgLang.t("Reaksiyalar"), sb.toString());
            return;
        }
        final ArrayList<String> lines = new ArrayList<>();
        load(f, account, m, "", lines, head.toString(), 0);
    }

    private static void load(ChatActivity f, int account, MessageObject m, String offset, ArrayList<String> lines, String head, int page) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
        req.peer = mc.getInputPeer(m.getDialogId());
        req.id = m.getId();
        req.limit = 100;
        if (!TextUtils.isEmpty(offset)) {
            req.offset = offset;
            req.flags |= 2;
        }
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res instanceof TLRPC.TL_messages_messageReactionsList) {
                TLRPC.TL_messages_messageReactionsList list = (TLRPC.TL_messages_messageReactionsList) res;
                mc.putUsers(list.users, false);
                mc.putChats(list.chats, false);
                for (TLRPC.MessagePeerReaction pr : list.reactions) {
                    lines.add(reactionText(pr.reaction) + "  " + peerText(mc, pr.peer_id) + (pr.date > 0 ? "  · " + time(pr.date) : ""));
                }
                if (!TextUtils.isEmpty(list.next_offset) && page < 20) {
                    load(f, account, m, list.next_offset, lines, head, page + 1);
                    return;
                }
            } else if (err != null && lines.isEmpty()) {
                BulletinFactory.of(f).createErrorBulletin(MgLang.t("Ro'yxatni olib bo'lmadi") + ": " + err.text).show();
                return;
            }
            showText(f, MgLang.t("Reaksiya bildirganlar") + " (" + lines.size() + ")", head + TextUtils.join("\n", lines));
        }));
    }

    private static void showText(ChatActivity f, String title, String text) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        TextView tv = new TextView(ctx);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tv.setLineSpacing(AndroidUtilities.dp(3), 1f);
        tv.setTextIsSelectable(true);
        tv.setText(text);
        ScrollView scroll = new ScrollView(ctx);
        scroll.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        scroll.addView(tv);
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle(title);
        b.setView(scroll);
        b.setPositiveButton(MgLang.t("Yopish"), null);
        b.setNeutralButton(MgLang.t("Nusxalash"), (d, w) -> {
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(f).createCopyBulletin(MgLang.t("Nusxalandi")).show();
        });
        f.showDialog(b.create());
    }
}
