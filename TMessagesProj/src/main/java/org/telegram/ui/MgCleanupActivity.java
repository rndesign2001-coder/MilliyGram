/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Kanal va guruhlarni tozalash yordamchisi: o'qilmayotgan (ko'p o'qilmagan xabar) va
 * faolsiz (uzoq vaqt yangi post yo'q) kanal/guruhlarni ko'rsatadi, belgilanganlardan birdaniga chiqiladi.
 */
public class MgCleanupActivity extends UniversalFragment {

    private static final int ID_LEAVE = 1;
    private static final int ID_SELECT_ALL = 2;
    private static final int ID_BASE = 10000;
    private static final int UNREAD_MIN = 100;
    private static final int INACTIVE_DAYS = 30;

    private final ArrayList<TLRPC.Dialog> unread = new ArrayList<>();
    private final ArrayList<TLRPC.Dialog> inactive = new ArrayList<>();
    private final ArrayList<Long> ids = new ArrayList<>();
    private final HashSet<Long> selected = new HashSet<>();

    @Override
    protected CharSequence getTitle() {
        return "Kanallarni tozalash";
    }

    private void collect() {
        unread.clear();
        inactive.clear();
        ids.clear();
        MessagesController mc = getMessagesController();
        int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        for (int i = 0; i < all.size(); i++) {
            TLRPC.Dialog d = all.get(i);
            if (d == null || d.id >= 0 || d instanceof TLRPC.TL_dialogFolder) {
                continue;
            }
            TLRPC.Chat chat = mc.getChat(-d.id);
            if (chat == null || chat.creator || ChatObject.isNotInChat(chat) || d.pinned) {
                continue;
            }
            if (d.unread_count >= UNREAD_MIN) {
                unread.add(d);
            } else if (d.last_message_date > 0 && now - d.last_message_date > INACTIVE_DAYS * 86400) {
                inactive.add(d);
            }
        }
        unread.sort((a, b) -> Integer.compare(b.unread_count, a.unread_count));
        inactive.sort((a, b) -> Integer.compare(a.last_message_date, b.last_message_date));
    }

    private String title(TLRPC.Dialog d) {
        TLRPC.Chat chat = getMessagesController().getChat(-d.id);
        String kind = chat != null && ChatObject.isChannelAndNotMegaGroup(chat) ? "📢 " : "👥 ";
        return kind + (chat == null ? "?" : chat.title);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        collect();
        int n = selected.size();
        items.add(UItem.asShadow("Ko'p xabari o'qilmay yotgan va uzoq vaqt jim turgan kanal/guruhlar. Keraksizlarini belgilab, birdaniga chiqib keting. O'zingiz yaratganlar va mahkamlanganlar ko'rsatilmaydi."));
        items.add(UItem.asButton(ID_LEAVE, R.drawable.msg_leave, n > 0 ? "Belgilanganlardan chiqish (" + n + ")" : "Belgilanganlardan chiqish").red());
        items.add(UItem.asButton(ID_SELECT_ALL, R.drawable.msg_select, "Hammasini belgilash / bekor qilish"));
        int idx = 0;
        if (!unread.isEmpty()) {
            items.add(UItem.asHeader("O'qilmayotganlar (" + UNREAD_MIN + "+ o'qilmagan)"));
            for (TLRPC.Dialog d : unread) {
                ids.add(d.id);
                items.add(UItem.asCheck(ID_BASE + idx, title(d) + " · " + d.unread_count).setChecked(selected.contains(d.id)));
                idx++;
            }
        }
        if (!inactive.isEmpty()) {
            items.add(UItem.asHeader("Faolsizlar (" + INACTIVE_DAYS + "+ kun yangi xabar yo'q)"));
            int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            for (TLRPC.Dialog d : inactive) {
                ids.add(d.id);
                int days = (now - d.last_message_date) / 86400;
                items.add(UItem.asCheck(ID_BASE + idx, title(d) + " · " + days + " kun").setChecked(selected.contains(d.id)));
                idx++;
            }
        }
        if (unread.isEmpty() && inactive.isEmpty()) {
            items.add(UItem.asShadow("Tozalashga arziydigan kanal yoki guruh topilmadi — hammasi joyida!"));
        } else {
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_BASE) {
            int i = item.id - ID_BASE;
            if (i < 0 || i >= ids.size()) {
                return;
            }
            long did = ids.get(i);
            if (!selected.remove(did)) {
                selected.add(did);
            }
            listView.adapter.update(true);
        } else if (item.id == ID_SELECT_ALL) {
            if (selected.size() >= ids.size()) {
                selected.clear();
            } else {
                selected.addAll(ids);
            }
            listView.adapter.update(true);
        } else if (item.id == ID_LEAVE) {
            if (selected.isEmpty()) {
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Avval ro'yxatdan kanal/guruhlarni belgilang").show();
                return;
            }
            ArrayList<Long> list = new ArrayList<>(selected);
            MgBulkActions.leaveChats(this, currentAccount, list, () -> {
                selected.clear();
                AndroidUtilities.runOnUIThread(() -> {
                    if (listView != null) {
                        listView.adapter.update(true);
                    }
                }, (long) list.size() * 400 + 1500);
            });
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
