/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kanal / guruh a'zolarini ommaviy boshqarish (faqat a'zolarni chiqarish huquqi bor adminlar uchun):
 * ro'yxatni yuklaydi, tez belgilash filtrlari (o'chirilgan hisoblar, botlar, yaqinda qo'shilganlar,
 * rasmsiz va username'siz) va belgilanganlarni ketma-ket, Telegram cheklovlariga rioya qilib chiqaradi.
 */
public class MgMembersActivity extends UniversalFragment {

    private static final int ID_QUICK = 1;
    private static final int ID_DELETED = 2;
    private static final int ID_KICK = 3;
    private static final int ID_RELOAD = 4;
    private static final int ID_BASE = 100000;
    private static final int PAGE = 200;
    private static final int MAX = 10000;

    private static class Member {
        TLRPC.User user;
        int date;
    }

    private final long chatId;
    private TLRPC.Chat chat;
    private final ArrayList<Member> members = new ArrayList<>();
    private final HashSet<Long> selected = new HashSet<>();
    private boolean loading;
    private int serverCount;
    private boolean cancelKick;

    public MgMembersActivity(long chatId) {
        super();
        this.chatId = chatId;
    }

    public static boolean canUse(TLRPC.Chat chat) {
        return chat != null && ChatObject.canBlockUsers(chat) && !chat.left;
    }

    @Override
    public boolean onFragmentCreate() {
        chat = getMessagesController().getChat(chatId);
        load();
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return "A'zolarni tozalash";
    }

    // ================= Yuklash =================

    private void load() {
        if (chat == null || loading) {
            return;
        }
        loading = true;
        members.clear();
        selected.clear();
        if (ChatObject.isChannel(chat)) {
            loadPage(0);
        } else {
            loadBasicGroup();
        }
    }

    private void loadPage(int offset) {
        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = getMessagesController().getInputChannel(chatId);
        req.filter = new TLRPC.TL_channelParticipantsRecent();
        req.offset = offset;
        req.limit = PAGE;
        getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (err != null) {
                if (err.text != null && err.text.startsWith("FLOOD_WAIT_")) {
                    int sec = parseWait(err.text);
                    AndroidUtilities.runOnUIThread(() -> loadPage(offset), (sec + 1) * 1000L);
                    return;
                }
                finishLoad();
                BulletinFactory.of(this).createErrorBulletin("Ro'yxatni olib bo'lmadi: " + err.text).show();
                return;
            }
            if (res instanceof TLRPC.TL_channels_channelParticipants) {
                TLRPC.TL_channels_channelParticipants r = (TLRPC.TL_channels_channelParticipants) res;
                getMessagesController().putUsers(r.users, false);
                getMessagesController().putChats(r.chats, false);
                serverCount = r.count;
                long self = UserConfig.getInstance(currentAccount).getClientUserId();
                for (TLRPC.ChannelParticipant p : r.participants) {
                    if (p instanceof TLRPC.TL_channelParticipantAdmin || p instanceof TLRPC.TL_channelParticipantCreator) {
                        continue;
                    }
                    long uid = p.peer != null ? MessageObject.getPeerId(p.peer) : p.user_id;
                    if (uid <= 0 || uid == self) {
                        continue;
                    }
                    TLRPC.User u = getMessagesController().getUser(uid);
                    if (u == null) {
                        continue;
                    }
                    Member m = new Member();
                    m.user = u;
                    m.date = p.date;
                    members.add(m);
                }
                if (r.participants.size() >= PAGE && offset + PAGE < MAX) {
                    if (listView != null) {
                        listView.adapter.update(false);
                    }
                    AndroidUtilities.runOnUIThread(() -> loadPage(offset + PAGE), 350);
                    return;
                }
            }
            finishLoad();
        }));
    }

    private void loadBasicGroup() {
        TLRPC.ChatFull full = getMessagesController().getChatFull(chatId);
        if (full != null && full.participants != null) {
            long self = UserConfig.getInstance(currentAccount).getClientUserId();
            for (TLRPC.ChatParticipant p : full.participants.participants) {
                if (p instanceof TLRPC.TL_chatParticipantAdmin || p instanceof TLRPC.TL_chatParticipantCreator || p.user_id == self) {
                    continue;
                }
                TLRPC.User u = getMessagesController().getUser(p.user_id);
                if (u == null) {
                    continue;
                }
                Member m = new Member();
                m.user = u;
                m.date = p.date;
                members.add(m);
            }
            serverCount = full.participants.participants.size();
        }
        finishLoad();
    }

    private void finishLoad() {
        loading = false;
        members.sort((a, b) -> Integer.compare(b.date, a.date));
        if (listView != null) {
            listView.adapter.update(true);
        }
    }

    private static int parseWait(String t) {
        Matcher m = Pattern.compile("(\\d+)").matcher(t);
        return m.find() ? Math.min(Integer.parseInt(m.group(1)), 300) : 5;
    }

    // ================= Ro'yxat =================

    private static String fmtDate(int date) {
        if (date <= 0) {
            return "";
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date * 1000L);
        return String.format(java.util.Locale.US, "%02d.%02d.%d %02d:%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.YEAR) % 100, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private String label(Member m) {
        TLRPC.User u = m.user;
        StringBuilder sb = new StringBuilder();
        if (u.deleted) {
            sb.append("👻 O'chirilgan hisob");
        } else {
            sb.append(UserObject.getUserName(u));
            if (u.bot) {
                sb.append(" 🤖");
            }
            String un = UserObject.getPublicUsername(u);
            if (un != null) {
                sb.append("  @").append(un);
            }
        }
        if (m.date > 0) {
            sb.append("  · ").append(fmtDate(m.date));
        }
        return sb.toString();
    }

    private int countDeleted() {
        int c = 0;
        for (Member m : members) {
            if (m.user.deleted) {
                c++;
            }
        }
        return c;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (loading) {
            items.add(UItem.asShadow("Ro'yxat yuklanmoqda… " + members.size() + (serverCount > 0 ? " / " + serverCount : "")));
        } else {
            String note = chat != null && ChatObject.isChannelAndNotMegaGroup(chat)
                    ? "Yuklandi: " + members.size() + " ta obunachi. Telegram kanallarda adminlarga faqat oxirgi ~200 obunachini ko'rsatadi — tozalagach \"Qayta yuklash\"ni bosing, keyingilari chiqadi."
                    : "Yuklandi: " + members.size() + " ta a'zo" + (serverCount > members.size() ? " (jami " + serverCount + ", Telegram ko'pi bilan 10 000 tasini beradi)" : "") + ". Adminlar ro'yxatga kirmaydi.";
            items.add(UItem.asShadow(note));
        }
        int n = selected.size();
        int del = countDeleted();
        items.add(UItem.asButton(ID_DELETED, R.drawable.msg_delete, "O'chirilgan hisoblarni tozalash", String.valueOf(del)));
        items.add(UItem.asButton(ID_QUICK, R.drawable.mg_select_all, "Tez belgilash…"));
        items.add(UItem.asButton(ID_KICK, R.drawable.msg_leave, n > 0 ? "Belgilanganlarni chiqarish (" + n + ")" : "Belgilanganlarni chiqarish").red());
        items.add(UItem.asButton(ID_RELOAD, R.drawable.msg_retry, "Qayta yuklash"));
        items.add(UItem.asShadow("Chiqarilganlar \"Chetlatilganlar\" ro'yxatiga tushadi va havola orqali qayta kira olmaydi (nakrutka qaytib kelmasligi uchun). Istasangiz, u yerdan blokdan chiqarishingiz mumkin."));
        if (!members.isEmpty()) {
            items.add(UItem.asHeader("A'zolar (yangi qo'shilganlar tepada)"));
            for (int i = 0; i < members.size(); i++) {
                Member m = members.get(i);
                items.add(UItem.asCheck(ID_BASE + i, label(m)).setChecked(selected.contains(m.user.id)));
            }
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_BASE) {
            int i = item.id - ID_BASE;
            if (i >= 0 && i < members.size()) {
                long id = members.get(i).user.id;
                if (!selected.remove(id)) {
                    selected.add(id);
                }
                listView.adapter.update(true);
            }
            return;
        }
        switch (item.id) {
            case ID_RELOAD:
                load();
                listView.adapter.update(true);
                break;
            case ID_QUICK:
                showQuickSelect();
                break;
            case ID_DELETED: {
                ArrayList<Long> ids = new ArrayList<>();
                for (Member m : members) {
                    if (m.user.deleted) {
                        ids.add(m.user.id);
                    }
                }
                if (ids.isEmpty()) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Yuklangan ro'yxatda o'chirilgan hisob yo'q").show();
                    return;
                }
                confirmKick(ids, ids.size() + " ta o'chirilgan hisob chiqarib yuborilsinmi?");
                break;
            }
            case ID_KICK:
                if (selected.isEmpty()) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Avval a'zolarni belgilang yoki \"Tez belgilash\"dan foydalaning").show();
                    return;
                }
                confirmKick(new ArrayList<>(selected), selected.size() + " ta a'zo chiqarib yuborilsinmi?");
                break;
        }
    }

    private void showQuickSelect() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] opts = {
                "👻 O'chirilgan hisoblar",
                "🤖 Botlar",
                "🕐 Oxirgi 1 soatda qo'shilganlar",
                "📅 Oxirgi 24 soatda qo'shilganlar",
                "📆 Oxirgi 7 kunda qo'shilganlar",
                "🎭 Rasmsiz va username'siz",
                "☑️ Hammasi",
                "✖️ Belgilashni bekor qilish"
        };
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle("Tez belgilash");
        b.setItems(opts, (d, w) -> {
            int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            int before = selected.size();
            if (w == 7) {
                selected.clear();
            } else {
                for (Member m : members) {
                    TLRPC.User u = m.user;
                    boolean pick;
                    switch (w) {
                        case 0: pick = u.deleted; break;
                        case 1: pick = u.bot; break;
                        case 2: pick = m.date > 0 && now - m.date <= 3600; break;
                        case 3: pick = m.date > 0 && now - m.date <= 86400; break;
                        case 4: pick = m.date > 0 && now - m.date <= 7 * 86400; break;
                        case 5: pick = !u.deleted && u.photo == null && UserObject.getPublicUsername(u) == null; break;
                        default: pick = true; break;
                    }
                    if (pick) {
                        selected.add(u.id);
                    }
                }
            }
            listView.adapter.update(true);
            if (w != 7) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Qo'shildi: " + (selected.size() - before) + ". Jami belgilangan: " + selected.size()).show();
            }
        });
        showDialog(b.create());
    }

    // ================= Chiqarish =================

    private void confirmKick(ArrayList<Long> ids, String text) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle("Chiqarish");
        b.setMessage(text + "\n\nTelegram cheklovlari tufayli ular birma-bir chiqariladi (taxminan " + Math.max(1, ids.size() * 4 / 10 / 60) + " daqiqa). Jarayonni istalgan payt to'xtatish mumkin.");
        b.setPositiveButton("Chiqarish", (d, w) -> startKick(ids));
        b.setNegativeButton("Bekor qilish", null);
        AlertDialog dialog = b.create();
        showDialog(dialog);
        TextView btn = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void startKick(ArrayList<Long> ids) {
        Context ctx = getParentActivity();
        if (ctx == null) {
            return;
        }
        cancelKick = false;
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView status = new TextView(ctx);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        status.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        status.setGravity(Gravity.CENTER);
        status.setText("0 / " + ids.size());
        box.addView(status, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 8, 24, 8));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, getResourceProvider());
        b.setTitle("Chiqarilmoqda…");
        b.setView(box);
        b.setNegativeButton("To'xtatish", (d, w) -> cancelKick = true);
        AlertDialog progress = b.create();
        progress.setCanceledOnTouchOutside(false);
        showDialog(progress);
        kickNext(ids, 0, 0, status, progress);
    }

    private void kickNext(ArrayList<Long> ids, int index, int done, TextView status, AlertDialog progress) {
        if (cancelKick || index >= ids.size()) {
            try {
                progress.dismiss();
            } catch (Throwable ignore) {
            }
            onKickFinished(done, ids.size());
            return;
        }
        long uid = ids.get(index);
        TLObject request;
        MessagesController mc = getMessagesController();
        if (ChatObject.isChannel(chat)) {
            TLRPC.TL_channels_editBanned req = new TLRPC.TL_channels_editBanned();
            req.channel = mc.getInputChannel(chatId);
            req.participant = mc.getInputPeer(uid);
            req.banned_rights = new TLRPC.TL_chatBannedRights();
            req.banned_rights.view_messages = true;
            req.banned_rights.send_messages = true;
            req.banned_rights.send_media = true;
            req.banned_rights.send_stickers = true;
            req.banned_rights.send_gifs = true;
            req.banned_rights.send_games = true;
            req.banned_rights.send_inline = true;
            req.banned_rights.embed_links = true;
            req.banned_rights.send_polls = true;
            req.banned_rights.invite_users = true;
            req.banned_rights.change_info = true;
            req.banned_rights.pin_messages = true;
            request = req;
        } else {
            TLRPC.TL_messages_deleteChatUser req = new TLRPC.TL_messages_deleteChatUser();
            req.chat_id = chatId;
            req.user_id = mc.getInputUser(uid);
            request = req;
        }
        getConnectionsManager().sendRequest(request, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (err != null && err.text != null && err.text.startsWith("FLOOD_WAIT_")) {
                int sec = parseWait(err.text);
                status.setText(done + " / " + ids.size() + "\nTelegram cheklovi: " + sec + " soniya kutilmoqda…");
                AndroidUtilities.runOnUIThread(() -> kickNext(ids, index, done, status, progress), (sec + 1) * 1000L);
                return;
            }
            int nd = done;
            if (err == null) {
                nd++;
                if (res instanceof TLRPC.Updates) {
                    mc.processUpdates((TLRPC.Updates) res, false);
                }
                selected.remove(uid);
                for (int i = 0; i < members.size(); i++) {
                    if (members.get(i).user.id == uid) {
                        members.remove(i);
                        break;
                    }
                }
            }
            status.setText(nd + " / " + ids.size());
            final int fnd = nd;
            AndroidUtilities.runOnUIThread(() -> kickNext(ids, index + 1, fnd, status, progress), 400);
        }));
    }

    private void onKickFinished(int done, int total) {
        if (listView != null) {
            listView.adapter.update(true);
        }
        getMessagesController().loadFullChat(chatId, 0, true);
        BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Chiqarildi: " + done + " / " + total).show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
