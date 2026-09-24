/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Yashirin bo'lim — yashirilgan chat, guruh, kanal va botlar (turlari bo'yicha katalog).
 * Kirish: bosh ekranda qidiruv tugmasini uzoq bosish.
 * Sozlamalar alohida: Sozlamalar → MilliyGram → Yashirin bo'lim sozlamalari.
 */
public class MgHiddenActivity extends UniversalFragment {

    private static final int ID_ENCRYPTED_BASE = 5000;

    private final ArrayList<Long> encryptedIds = new ArrayList<>();

    /** Yashirin bo'limni ochadi (yashirin bo'lim qulfi bilan) */
    public static void open(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (!MgConfig.hasLock(MgConfig.SCOPE_HIDDEN)) {
            MgChatLock.createLock(fragment, MgConfig.SCOPE_HIDDEN, MgConfig.LOCK_PIN, () -> fragment.presentFragment(new MgHiddenActivity()));
            return;
        }
        if (MgConfig.isHiddenNoPin()) {
            fragment.presentFragment(new MgHiddenActivity());
            return;
        }
        MgChatLock.askLock(fragment, MgConfig.SCOPE_HIDDEN, "Yashirin bo'lim", ok -> {
            if (ok) {
                fragment.presentFragment(new MgHiddenActivity());
            }
        });
    }

    /** Chat menyusidagi "Yashirish / Ko'rsatish" */
    public static void toggleHidden(BaseFragment fragment, int account, long dialogId) {
        if (MgConfig.isDialogHidden(account, dialogId)) {
            MgConfig.setDialogHidden(account, dialogId, false);
            notifyChanged(account);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Chat yana ro'yxatda ko'rinadi").show();
        } else {
            ArrayList<Long> ids = new ArrayList<>();
            ids.add(dialogId);
            hideDialogs(fragment, account, ids);
        }
    }

    /** Bir nechta chatni yashirish (kerak bo'lsa avval yashirin bo'lim kodini yaratadi) */
    public static void hideDialogs(BaseFragment fragment, int account, List<Long> dialogIds) {
        Runnable doHide = () -> {
            for (Long id : dialogIds) {
                MgConfig.setDialogHidden(account, id, true);
            }
            notifyChanged(account);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check,
                    (dialogIds.size() > 1 ? dialogIds.size() + " ta chat yashirildi" : "Chat yashirildi") + ". Ko'rish: qidiruv tugmasini uzoq bosing").show();
        };
        if (!MgConfig.hasLock(MgConfig.SCOPE_HIDDEN)) {
            MgChatLock.createLock(fragment, MgConfig.SCOPE_HIDDEN, MgConfig.LOCK_PIN, doHide);
        } else {
            doHide.run();
        }
    }

    static void notifyChanged(int account) {
        try {
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected CharSequence getTitle() {
        return "🙈 Yashirin bo'lim";
    }

    private String encryptedName(long dialogId) {
        MessagesController mc = getMessagesController();
        TLRPC.EncryptedChat enc = mc.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
        if (enc != null) {
            TLRPC.User user = mc.getUser(enc.user_id);
            if (user != null) {
                return "🔒 " + UserObject.getUserName(user);
            }
        }
        return "🔒 Maxfiy chat";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        encryptedIds.clear();
        MessagesController mc = getMessagesController();
        ArrayList<Long> users = new ArrayList<>();
        ArrayList<Long> groups = new ArrayList<>();
        ArrayList<Long> channels = new ArrayList<>();
        ArrayList<Long> bots = new ArrayList<>();
        for (Long id : MgConfig.getHiddenDialogs(currentAccount)) {
            if (DialogObject.isEncryptedDialog(id)) {
                encryptedIds.add(id);
            } else if (id > 0) {
                TLRPC.User user = mc.getUser(id);
                if (user != null && user.bot) {
                    bots.add(id);
                } else {
                    users.add(id);
                }
            } else {
                TLRPC.Chat chat = mc.getChat(-id);
                if (ChatObject.isChannelAndNotMegaGroup(chat)) {
                    channels.add(id);
                } else {
                    groups.add(id);
                }
            }
        }
        boolean empty = users.isEmpty() && groups.isEmpty() && channels.isEmpty() && bots.isEmpty() && encryptedIds.isEmpty();
        if (empty) {
            items.add(UItem.asTopViewStatic("Hozircha yashirilgan chat yo'q.\n\nYashirish uchun: chatni belgilab ⋮ → \"Yashirish\" yoki chat ichida ⋮ → \"Chatni yashirish\".", R.drawable.msg_archive));
            return;
        }
        addSection(items, "👤 Profillar", users);
        addSection(items, "👥 Guruhlar", groups);
        addSection(items, "📢 Kanallar", channels);
        addSection(items, "🤖 Botlar", bots);
        if (!encryptedIds.isEmpty()) {
            items.add(UItem.asHeader("🔒 Maxfiy chatlar"));
            for (int i = 0; i < encryptedIds.size(); i++) {
                items.add(UItem.asButton(ID_ENCRYPTED_BASE + i, R.drawable.msg_secret, encryptedName(encryptedIds.get(i))));
            }
            items.add(UItem.asShadow(null));
        }
        items.add(UItem.asShadow("Chatni ochish uchun bosing. Umumiy ro'yxatga qaytarish uchun uzoq bosing."));
    }

    private void addSection(ArrayList<UItem> items, String title, ArrayList<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        items.add(UItem.asHeader(title + " · " + ids.size()));
        for (Long id : ids) {
            items.add(UItem.asFilterChat(true, id));
        }
        items.add(UItem.asShadow(null));
    }

    private long dialogIdOf(UItem item) {
        if (item.viewType == UniversalAdapter.VIEW_TYPE_FILTER_CHAT) {
            return item.dialogId;
        }
        int index = item.id - ID_ENCRYPTED_BASE;
        if (index >= 0 && index < encryptedIds.size()) {
            return encryptedIds.get(index);
        }
        return 0;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        long dialogId = dialogIdOf(item);
        if (dialogId == 0) {
            return;
        }
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
        long dialogId = dialogIdOf(item);
        if (dialogId == 0 || getParentActivity() == null) {
            return false;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("Yashirishdan chiqarish");
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
