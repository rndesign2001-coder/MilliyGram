/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Yashirin bo'lim: yashirilgan chatlar ro'yxati.
 */
public class MgHiddenActivity extends UniversalFragment {

    private static final int ID_NOTIFY = 1;
    private static final int ID_BASE = 1000;

    private final ArrayList<Long> dialogIds = new ArrayList<>();

    /** PIN so'rab yashirin bo'limni ochadi */
    public static void open(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (!MgConfig.hasPin()) {
            MgChatLock.createPin(fragment, () -> fragment.presentFragment(new MgHiddenActivity()));
            return;
        }
        MgChatLock.askPin(fragment, "Yashirin bo'lim", ok -> {
            if (ok) {
                fragment.presentFragment(new MgHiddenActivity());
            }
        });
    }

    /** Chat menyusidagi "Yashirish / Ko'rsatish" */
    public static void toggleHidden(BaseFragment fragment, int account, long dialogId) {
        boolean hidden = MgConfig.isDialogHidden(account, dialogId);
        if (hidden) {
            MgConfig.setDialogHidden(account, dialogId, false);
            notifyChanged(account);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Chat yana ro'yxatda ko'rinadi").show();
        } else {
            Runnable doHide = () -> {
                MgConfig.setDialogHidden(account, dialogId, true);
                notifyChanged(account);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check,
                        "Chat yashirildi. Ochish: qidiruv tugmasini uzoq bosing yoki Sozlamalar → MilliyGram → Yashirin bo'lim").show();
            };
            if (!MgConfig.hasPin()) {
                MgChatLock.createPin(fragment, doHide);
            } else {
                doHide.run();
            }
        }
    }

    private static void notifyChanged(int account) {
        try {
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected CharSequence getTitle() {
        return "Yashirin bo'lim";
    }

    private String getDialogName(long dialogId) {
        MessagesController mc = getMessagesController();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            TLRPC.EncryptedChat enc = mc.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
            if (enc != null) {
                TLRPC.User user = mc.getUser(enc.user_id);
                if (user != null) {
                    return "🔒 " + UserObject.getUserName(user);
                }
            }
            return "Maxfiy chat";
        } else if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = mc.getUser(dialogId);
            if (user != null) {
                return UserObject.isUserSelf(user) ? "Saqlangan xabarlar" : UserObject.getUserName(user);
            }
        } else {
            TLRPC.Chat chat = mc.getChat(-dialogId);
            if (chat != null && chat.title != null) {
                return chat.title;
            }
        }
        return "ID: " + dialogId;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        dialogIds.clear();
        dialogIds.addAll(MgConfig.getHiddenDialogs(currentAccount));

        items.add(UItem.asHeader("Yashirilgan chatlar"));
        if (dialogIds.isEmpty()) {
            items.add(UItem.asShadow("Hozircha yashirilgan chat yo'q. Chatni yashirish uchun chatni oching va ⋮ menyudan \"Chatni yashirish\" ni tanlang."));
        } else {
            for (int i = 0; i < dialogIds.size(); i++) {
                items.add(UItem.asButton(ID_BASE + i, R.drawable.msg_openprofile, getDialogName(dialogIds.get(i))));
            }
            items.add(UItem.asShadow("Chatni ochish uchun bosing. Ro'yxatga qaytarish uchun uzoq bosing."));
        }

        items.add(UItem.asHeader("Sozlamalar"));
        items.add(UItem.asCheck(ID_NOTIFY, "Yashirin chatlardan bildirishnoma").setChecked(MgConfig.isHiddenNotifyEnabled()));
        items.add(UItem.asShadow("O'chirilgan bo'lsa, yashirin chatlardan bildirishnoma kelmaydi va ular ro'yxatda ham, qidiruvda ham ko'rinmaydi."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_NOTIFY) {
            boolean value = !MgConfig.isHiddenNotifyEnabled();
            MgConfig.setBool("hidden_notify", value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            return;
        }
        int index = item.id - ID_BASE;
        if (index < 0 || index >= dialogIds.size()) {
            return;
        }
        long dialogId = dialogIds.get(index);
        Bundle args = new Bundle();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
        } else if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else {
            args.putLong("chat_id", -dialogId);
        }
        presentFragment(new ChatActivity(args));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int index = item.id - ID_BASE;
        if (index < 0 || index >= dialogIds.size() || getParentActivity() == null) {
            return false;
        }
        long dialogId = dialogIds.get(index);
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getDialogName(dialogId));
        builder.setMessage("Bu chatni yana umumiy ro'yxatda ko'rsataylikmi?");
        builder.setPositiveButton("Ko'rsatish", (dialog, which) -> {
            MgConfig.setDialogHidden(currentAccount, dialogId, false);
            notifyChanged(currentAccount);
            listView.adapter.update(true);
        });
        builder.setNegativeButton("Bekor qilish", null);
        showDialog(builder.create());
        return true;
    }
}
