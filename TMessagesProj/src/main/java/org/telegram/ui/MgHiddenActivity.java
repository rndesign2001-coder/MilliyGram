/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Yashirin bo'lim: yashirilgan chatlar, akkauntlar va qulf sozlamalari.
 */
public class MgHiddenActivity extends UniversalFragment {

    private static final int ID_NOTIFY = 1;
    private static final int ID_ACC_NOTIFY = 2;
    private static final int ID_LOCK_TYPE = 3;
    private static final int ID_CHANGE_CODE = 4;
    private static final int ID_FINGERPRINT = 5;
    private static final int ID_VIBRATE = 6;
    private static final int ID_INVISIBLE = 7;
    private static final int ID_NO_PIN = 8;
    private static final int ID_IN_SETTINGS = 9;
    private static final int ID_FAKE_NAME = 10;
    private static final int ID_BASE = 1000;
    private static final int ID_ACC_BASE = 2000;

    private final ArrayList<Long> dialogIds = new ArrayList<>();

    /** Yashirin bo'limni ochadi (kerak bo'lsa qulf so'raydi) */
    public static void open(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (!MgConfig.hasPin()) {
            MgChatLock.createPin(fragment, () -> fragment.presentFragment(new MgHiddenActivity()));
            return;
        }
        if (MgConfig.isHiddenNoPin()) {
            fragment.presentFragment(new MgHiddenActivity());
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
                        "Chat yashirildi. Ochish: qidiruv tugmasini uzoq bosing").show();
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

    private String getAccountName(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        if (user == null) {
            return "Akkaunt " + (account + 1);
        }
        String name = org.telegram.messenger.ContactsController.formatName(user.first_name, user.last_name);
        if (user.bot) {
            name = "🤖 " + name;
        }
        return account == currentAccount ? name + " (joriy)" : name;
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

        items.add(UItem.asHeader("Yashirilgan akkauntlar"));
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                items.add(UItem.asCheck(ID_ACC_BASE + a, getAccountName(a)).setChecked(MgConfig.isAccountHidden(a)));
            }
        }
        items.add(UItem.asShadow("Belgilangan akkauntlar akkauntlar ro'yxatida ko'rinmaydi. Yashirilgan akkauntga o'tish uchun shu yerda uning nomini uzoq bosing."));

        boolean pattern = MgConfig.LOCK_PATTERN.equals(MgConfig.getLockType());
        items.add(UItem.asHeader("Qulf"));
        items.add(UItem.asButton(ID_LOCK_TYPE, R.drawable.msg_secret, "Qulf turi", pattern ? "Grafik kalit" : "PIN kod"));
        items.add(UItem.asButton(ID_CHANGE_CODE, R.drawable.msg_edit, pattern ? "Grafik kalitni o'zgartirish" : "PIN kodni o'zgartirish"));
        items.add(UItem.asCheck(ID_FINGERPRINT, "Barmoq izi bilan ochish").setChecked(MgConfig.isFingerprintEnabled()));
        items.add(UItem.asCheck(ID_VIBRATE, "Kiritishda tebranish").setChecked(MgConfig.isLockVibrate()));
        if (pattern) {
            items.add(UItem.asCheck(ID_INVISIBLE, "Ko'rinmas grafik kalit").setChecked(MgConfig.isPatternInvisible()));
        }
        items.add(UItem.asCheck(ID_NO_PIN, "Parolsiz kirish").setChecked(MgConfig.isHiddenNoPin()));
        items.add(UItem.asCheck(ID_IN_SETTINGS, "Sozlamalarda ko'rsatish").setChecked(MgConfig.isHiddenInSettings()));
        items.add(UItem.asShadow("Kirish usuli: bosh ekranda qidiruv tugmasini uzoq bosing. \"Sozlamalarda ko'rsatish\" o'chirilsa, bo'lim faqat shu usul bilan ochiladi."));

        items.add(UItem.asHeader("Yolg'on ism"));
        String fake = MgConfig.getFakeName();
        items.add(UItem.asButton(ID_FAKE_NAME, R.drawable.msg_openprofile, "Yolg'on ism", fake.isEmpty() ? "O'chirilgan" : fake));
        items.add(UItem.asShadow("Ilovada o'z ismingiz o'rniga shu ism ko'rinadi (masalan, skrinshot olganda). Boshqalar sizning haqiqiy ismingizni ko'raveradi."));

        items.add(UItem.asHeader("Bildirishnomalar"));
        items.add(UItem.asCheck(ID_NOTIFY, "Yashirin chatlardan").setChecked(MgConfig.isHiddenNotifyEnabled()));
        items.add(UItem.asCheck(ID_ACC_NOTIFY, "Yashirilgan akkauntlardan").setChecked(MgConfig.isHiddenAccountNotify()));
        items.add(UItem.asShadow(null));
    }

    private void toggleCheck(View view, String key, boolean newValue) {
        MgConfig.setBool(key, newValue);
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(newValue);
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_NOTIFY:
                toggleCheck(view, "hidden_notify", !MgConfig.isHiddenNotifyEnabled());
                return;
            case ID_ACC_NOTIFY:
                toggleCheck(view, "hidden_account_notify", !MgConfig.isHiddenAccountNotify());
                return;
            case ID_FINGERPRINT:
                toggleCheck(view, "lock_fingerprint", !MgConfig.isFingerprintEnabled());
                return;
            case ID_VIBRATE:
                toggleCheck(view, "lock_vibrate", !MgConfig.isLockVibrate());
                return;
            case ID_INVISIBLE:
                toggleCheck(view, "pattern_invisible", !MgConfig.isPatternInvisible());
                return;
            case ID_NO_PIN:
                toggleCheck(view, "hidden_no_pin", !MgConfig.isHiddenNoPin());
                return;
            case ID_IN_SETTINGS:
                toggleCheck(view, "hidden_in_settings", !MgConfig.isHiddenInSettings());
                return;
            case ID_LOCK_TYPE:
                showLockTypeDialog();
                return;
            case ID_CHANGE_CODE:
                MgChatLock.createLock(this, MgConfig.getLockType(), () -> listView.adapter.update(true));
                return;
            case ID_FAKE_NAME:
                showFakeNameDialog();
                return;
        }
        if (item.id >= ID_ACC_BASE && item.id < ID_ACC_BASE + UserConfig.MAX_ACCOUNT_COUNT) {
            int account = item.id - ID_ACC_BASE;
            boolean value = !MgConfig.isAccountHidden(account);
            MgConfig.setAccountHidden(account, value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
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
        if (getParentActivity() == null) {
            return false;
        }
        if (item.id >= ID_ACC_BASE && item.id < ID_ACC_BASE + UserConfig.MAX_ACCOUNT_COUNT) {
            int account = item.id - ID_ACC_BASE;
            if (account != currentAccount && LaunchActivity.instance != null) {
                LaunchActivity.instance.switchToAccount(account, true);
            }
            return true;
        }
        int index = item.id - ID_BASE;
        if (index < 0 || index >= dialogIds.size()) {
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

    private void showLockTypeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("Qulf turi");
        builder.setItems(new CharSequence[]{"PIN kod (4 raqam)", "Grafik kalit (tasvirli kod)"}, (dialog, which) -> {
            String type = which == 1 ? MgConfig.LOCK_PATTERN : MgConfig.LOCK_PIN;
            MgChatLock.createLock(this, type, () -> listView.adapter.update(true));
        });
        showDialog(builder.create());
    }

    private void showFakeNameDialog() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setSingleLine(true);
        editText.setHint("Masalan: Foydalanuvchi");
        editText.setText(MgConfig.getFakeName());
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle("Yolg'on ism");
        builder.setView(frameLayout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            MgConfig.setFakeName(editText.getText().toString());
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listView.adapter.update(true);
        });
        builder.setNeutralButton("O'chirish", (dialog, which) -> {
            MgConfig.setFakeName("");
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listView.adapter.update(true);
        });
        builder.setNegativeButton("Bekor qilish", null);
        showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 250);
    }
}
