/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
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
 * Yashirin bo'lim sozlamalari (o'z qulfi bilan, chat qulfidan alohida).
 */
public class MgHiddenSettingsActivity extends UniversalFragment {

    private static final int ID_OPEN = 1;
    private static final int ID_LOCK_TYPE = 2;
    private static final int ID_CHANGE_CODE = 3;
    private static final int ID_FINGERPRINT = 4;
    private static final int ID_INVISIBLE = 5;
    private static final int ID_NO_PIN = 6;
    private static final int ID_FAKE_NAME = 7;
    private static final int ID_NOTIFY = 8;
    private static final int ID_ACC_NOTIFY = 9;
    private static final int ID_SHAKE = 10;
    private static final int ID_ACC_BASE = 2000;

    private static final String S = MgConfig.SCOPE_HIDDEN;

    public static void open(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (!MgConfig.hasLock(S)) {
            MgChatLock.createLock(fragment, S, MgConfig.LOCK_PIN, () -> fragment.presentFragment(new MgHiddenSettingsActivity()));
            return;
        }
        MgChatLock.askLock(fragment, S, "Yashirin bo'lim sozlamalari", ok -> {
            if (ok) {
                fragment.presentFragment(new MgHiddenSettingsActivity());
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return "Yashirin bo'lim sozlamalari";
    }

    private String accountName(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        if (user == null) {
            return "Akkaunt " + (account + 1);
        }
        String alias = MgConfig.getAccountAlias(account);
        String name = !alias.isEmpty() ? alias : ContactsController.formatName(user.first_name, user.last_name);
        return (user.bot ? "🤖 " : "") + name + (account == currentAccount ? " (joriy)" : "");
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asTopViewStatic("Yashirin chatlar va akkauntlar faqat shu yerda boshqariladi. Yashirin bo'limga kirish: bosh ekranda qidiruv 🔍 tugmasini uzoq bosing.", R.drawable.msg_archive));
        items.add(UItem.asButton(ID_OPEN, R.drawable.msg_archive, "Yashirin chatlarni ochish",
                MgConfig.getHiddenDialogs(currentAccount).isEmpty() ? "" : String.valueOf(MgConfig.getHiddenDialogs(currentAccount).size())));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader("Yashirilgan akkauntlar"));
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                items.add(UItem.asCheck(ID_ACC_BASE + a, accountName(a)).setChecked(MgConfig.isAccountHidden(a)));
            }
        }
        items.add(UItem.asShadow("Belgilangan akkauntlar ro'yxatlarda ko'rinmaydi. Ularga o'tish: Profil → akkauntlar ro'yxatida \"Hisob qo'shish\" ni uzoq bosing."));

        boolean pattern = MgConfig.LOCK_PATTERN.equals(MgConfig.getLockType(S));
        items.add(UItem.asHeader("Kirish kodi"));
        items.add(UItem.asButton(ID_LOCK_TYPE, R.drawable.msg_secret, "Kod turi", pattern ? "Grafik kalit" : "PIN kod"));
        items.add(UItem.asButton(ID_CHANGE_CODE, R.drawable.msg_edit, pattern ? "Grafik kalitni o'zgartirish" : "PIN kodni o'zgartirish"));
        items.add(UItem.asCheck(ID_FINGERPRINT, "Barmoq izi bilan ochish").setChecked(MgConfig.isFingerprintEnabled(S)));
        if (pattern) {
            items.add(UItem.asCheck(ID_INVISIBLE, "Ko'rinmas grafik kalit").setChecked(MgConfig.isPatternInvisible(S)));
        }
        items.add(UItem.asCheck(ID_NO_PIN, "Parolsiz kirish").setChecked(MgConfig.isHiddenNoPin()));
        items.add(UItem.asShadow("Bu kod chat qulfi kodidan alohida. Parolsiz kirish yoqilsa, qidiruvni uzoq bosganda kod so'ralmaydi."));

        items.add(UItem.asHeader("Xavfsizlik"));
        items.add(UItem.asCheck(ID_SHAKE, "Silkitib yashirish").setChecked(MgConfig.isShakeToHide()));
        items.add(UItem.asShadow("Telefonni keskin silkitsangiz, ochiq yashirin bo'lim va qulflangan chatlar darhol yopiladi, yashirilgan akkauntdan asosiy akkauntga o'tiladi."));

        items.add(UItem.asHeader("Yolg'on ism"));
        String fake = MgConfig.getFakeName();
        items.add(UItem.asButton(ID_FAKE_NAME, R.drawable.msg_openprofile, "Yolg'on ism", fake.isEmpty() ? "O'chirilgan" : fake));
        items.add(UItem.asShadow("Ilovada o'z ismingiz o'rniga shu ism ko'rinadi (masalan, skrinshot uchun). Boshqalar haqiqiy ismingizni ko'raveradi."));

        items.add(UItem.asHeader("Bildirishnomalar"));
        items.add(UItem.asCheck(ID_NOTIFY, "Yashirin chatlardan").setChecked(MgConfig.isHiddenNotifyEnabled()));
        items.add(UItem.asCheck(ID_ACC_NOTIFY, "Yashirilgan akkauntlardan").setChecked(MgConfig.isHiddenAccountNotify()));
        items.add(UItem.asShadow(null));
    }

    private void toggle(View view, String key, boolean value) {
        MgConfig.setBool(key, value);
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_OPEN:
                presentFragment(new MgHiddenActivity());
                return;
            case ID_LOCK_TYPE:
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                builder.setTitle("Kod turi");
                builder.setItems(new CharSequence[]{"PIN kod (4 raqam)", "Grafik kalit (tasvirli kod)"}, (dialog, which) ->
                        MgChatLock.createLock(this, S, which == 1 ? MgConfig.LOCK_PATTERN : MgConfig.LOCK_PIN, () -> listView.adapter.update(true)));
                showDialog(builder.create());
                return;
            case ID_CHANGE_CODE:
                MgChatLock.createLock(this, S, MgConfig.getLockType(S), () -> listView.adapter.update(true));
                return;
            case ID_FINGERPRINT:
                toggle(view, MgConfig.fingerprintKey(S), !MgConfig.isFingerprintEnabled(S));
                return;
            case ID_INVISIBLE:
                toggle(view, MgConfig.patternInvisibleKey(S), !MgConfig.isPatternInvisible(S));
                return;
            case ID_NO_PIN:
                toggle(view, "hidden_no_pin", !MgConfig.isHiddenNoPin());
                return;
            case ID_SHAKE:
                toggle(view, "shake_to_hide", !MgConfig.isShakeToHide());
                return;
            case ID_NOTIFY:
                toggle(view, "hidden_notify", !MgConfig.isHiddenNotifyEnabled());
                return;
            case ID_ACC_NOTIFY:
                toggle(view, "hidden_account_notify", !MgConfig.isHiddenAccountNotify());
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
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_ACC_BASE && item.id < ID_ACC_BASE + UserConfig.MAX_ACCOUNT_COUNT) {
            int account = item.id - ID_ACC_BASE;
            if (account != currentAccount && LaunchActivity.instance != null) {
                LaunchActivity.instance.switchToAccount(account, true);
            }
            return true;
        }
        return false;
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
