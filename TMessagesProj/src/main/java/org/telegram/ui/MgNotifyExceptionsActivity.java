/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgNotifyFilter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Bildirishnoma filtri istisnolari: bu chatlardan bildirishnoma har doim keladi */
public class MgNotifyExceptionsActivity extends UniversalFragment {

    private static final int ID_ADD = 1;
    private static final int ID_ITEM_BASE = 1000;

    private final ArrayList<Long> dids = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return MgLang.t("Istisno chatlar");
    }

    private String name(long did) {
        MessagesController mc = getMessagesController();
        if (did > 0) {
            TLRPC.User u = mc.getUser(did);
            return u != null ? UserObject.getUserName(u) : String.valueOf(did);
        }
        TLRPC.Chat c = mc.getChat(-did);
        return c != null ? c.title : String.valueOf(did);
    }

    private int icon(long did) {
        if (did > 0) {
            TLRPC.User u = getMessagesController().getUser(did);
            return u != null && u.bot ? R.drawable.msg_bots : R.drawable.msg_openprofile;
        }
        TLRPC.Chat c = getMessagesController().getChat(-did);
        return org.telegram.messenger.ChatObject.isChannelAndNotMegaGroup(c) ? R.drawable.msg_channel : R.drawable.msg_groups;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        dids.clear();
        dids.addAll(MgNotifyFilter.getExceptions(currentAccount));
        items.add(UItem.asButton(ID_ADD, R.drawable.msg_add, MgLang.t("Chat qo'shish")).accent());
        items.add(UItem.asShadow(MgLang.t("Bu chatlardan bildirishnoma har doim keladi, filtr ularni o'chirmaydi.")));
        if (!dids.isEmpty()) {
            items.add(UItem.asHeader(MgLang.t("Istisnolar") + " (" + dids.size() + ")"));
            for (int i = 0; i < dids.size(); i++) {
                items.add(UItem.asButton(ID_ITEM_BASE + i, icon(dids.get(i)), name(dids.get(i)), MgLang.t("o'chirish")));
            }
            items.add(UItem.asShadow(MgLang.t("Ro'yxatdan olib tashlash uchun chatni bosing.")));
        }
    }

    private void openPicker() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        args.putBoolean("allowGlobalSearch", false);
        DialogsActivity picker = new DialogsActivity(args);
        picker.setDelegate((fragment, selected, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            for (int i = 0; i < selected.size(); i++) {
                MgNotifyFilter.addException(currentAccount, selected.get(i).dialogId);
            }
            fragment.finishFragment();
            if (listView != null) {
                listView.adapter.update(true);
            }
            return true;
        });
        presentFragment(picker);
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ADD) {
            openPicker();
        } else if (item.id >= ID_ITEM_BASE) {
            int i = item.id - ID_ITEM_BASE;
            if (i >= 0 && i < dids.size()) {
                MgNotifyFilter.removeException(currentAccount, dids.get(i));
                listView.adapter.update(true);
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }
}
