/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/**
 * Akkauntlar ro'yxatidagi ⚙ menyu: asosiy akkaunt, bosh ekranga yorliq, taxallus, yashirish, chiqish.
 * "Hisob qo'shish" ni uzoq bosish — yashirilgan akkauntlar.
 */
public class MgAccountMenu {

    public static String displayName(int account) {
        String alias = MgConfig.getAccountAlias(account);
        if (!alias.isEmpty()) {
            return alias;
        }
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        if (user == null) {
            return org.telegram.messenger.MgLang.t("Akkaunt ") + (account + 1);
        }
        return ContactsController.formatName(user.first_name, user.last_name);
    }

    /** Akkaunt qatoriga ⚙ tugmasini qo'shadi */
    public static void decorateRow(BaseFragment fragment, ItemOptions o, LinearLayout row, int account) {
        try {
            String alias = MgConfig.getAccountAlias(account);
            if (!alias.isEmpty()) {
                for (int i = 0; i < row.getChildCount(); i++) {
                    if (row.getChildAt(i) instanceof TextView) {
                        ((TextView) row.getChildAt(i)).setText(alias);
                    }
                }
            }
            Context context = row.getContext();
            ImageView gear = new ImageView(context);
            gear.setImageResource(R.drawable.msg_settings);
            gear.setScaleType(ImageView.ScaleType.CENTER);
            gear.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));
            gear.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP));
            gear.setOnClickListener(v -> openAccountOptions(fragment, o, account));
            row.addView(gear, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void openAccountOptions(BaseFragment fragment, ItemOptions o, int account) {
        ItemOptions sub = o.makeSwipeback();
        sub.add(R.drawable.ic_ab_back, org.telegram.messenger.MgLang.t("Orqaga"), o::closeSwipeback);
        sub.addGap();
        sub.addText(displayName(account), 15);
        sub.addGap();
        boolean isMain = MgConfig.getMainAccount() == account;
        sub.add(R.drawable.msg_home, isMain ? org.telegram.messenger.MgLang.t("✓ Asosiy akkaunt") : org.telegram.messenger.MgLang.t("Asosiy akkaunt sifatida tanlash"), () -> {
            o.dismiss();
            MgConfig.setMainAccount(isMain ? -1 : account);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check,
                    isMain ? org.telegram.messenger.MgLang.t("Asosiy akkaunt bekor qilindi") : org.telegram.messenger.MgLang.t("Ilova endi shu akkaunt bilan ochiladi")).show();
        });
        sub.add(R.drawable.msg_addbot, org.telegram.messenger.MgLang.t("Bosh ekranga chiqarish"), () -> {
            o.dismiss();
            pinShortcut(fragment, account);
        });
        sub.add(R.drawable.msg_edit, org.telegram.messenger.MgLang.t("Akkaunt nomini o'zgartirish"), () -> {
            o.dismiss();
            showAliasDialog(fragment, account);
        });
        boolean notifyOn = org.telegram.messenger.MgConfig.isAccountNotifyEnabled(account);
        sub.add(notifyOn ? R.drawable.msg_mute : R.drawable.msg_unmute, notifyOn ? org.telegram.messenger.MgLang.t("Bildirishnomalarni o'chirish") : org.telegram.messenger.MgLang.t("Bildirishnomalarni yoqish"), () -> {
            org.telegram.messenger.MgConfig.setAccountNotifyEnabled(account, !notifyOn);
            o.dismiss();
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, !notifyOn ? org.telegram.messenger.MgLang.t("Bu akkaunt bildirishnomalari yoqildi") : org.telegram.messenger.MgLang.t("Bu akkauntdan bildirishnoma kelmaydi")).show();
        });
        sub.add(R.drawable.msg_archive, org.telegram.messenger.MgLang.t("Yashirish"), () -> {
            o.dismiss();
            hideAccount(fragment, account);
        });
        sub.add(R.drawable.msg_leave, org.telegram.messenger.MgLang.t("Chiqish"), true, () -> {
            o.dismiss();
            confirmLogout(fragment, account);
        });
        o.openSwipeback(sub);
    }

    private static void hideAccount(BaseFragment fragment, int account) {
        Runnable doHide = () -> {
            MgConfig.setAccountHidden(account, true);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
            if (account == UserConfig.selectedAccount) {
                switchToVisibleAccount(account);
            }
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, org.telegram.messenger.MgLang.t("Akkaunt yashirildi. Ko'rish: \"Hisob qo'shish\" ni uzoq bosing")).show();
        };
        if (!MgConfig.hasLock(MgConfig.SCOPE_HIDDEN)) {
            MgChatLock.createLock(fragment, MgConfig.SCOPE_HIDDEN, MgConfig.LOCK_PIN, doHide);
        } else {
            doHide.run();
        }
    }

    /** Yashirilmagan (asosiy yoki birinchi) akkauntga o'tadi */
    public static void switchToVisibleAccount(int except) {
        if (LaunchActivity.instance == null) {
            return;
        }
        int target = -1;
        int main = MgConfig.getMainAccount();
        if (main >= 0 && main != except && UserConfig.getInstance(main).isClientActivated() && !MgConfig.isAccountHidden(main)) {
            target = main;
        } else {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (a != except && UserConfig.getInstance(a).isClientActivated() && !MgConfig.isAccountHidden(a)) {
                    target = a;
                    break;
                }
            }
        }
        if (target >= 0) {
            LaunchActivity.instance.switchToAccount(target, true);
        }
    }

    /** "Hisob qo'shish" bandini uzoq bosganda yashirilgan akkauntlar */
    public static void attachHiddenAccounts(BaseFragment fragment, ItemOptions o, ActionBarMenuSubItem addItem) {
        if (addItem == null) {
            return;
        }
        addItem.setOnLongClickListener(v -> {
            o.dismiss();
            Runnable show = () -> showHiddenAccounts(fragment);
            if (!MgConfig.hasLock(MgConfig.SCOPE_HIDDEN)) {
                show.run();
            } else if (MgConfig.isHiddenNoPin()) {
                show.run();
            } else {
                MgChatLock.askLock(fragment, MgConfig.SCOPE_HIDDEN, org.telegram.messenger.MgLang.t("Yashirilgan akkauntlar"), ok -> {
                    if (ok) {
                        show.run();
                    }
                });
            }
            return true;
        });
    }

    private static void showHiddenAccounts(BaseFragment fragment) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        ArrayList<Integer> hidden = new ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated() && MgConfig.isAccountHidden(a)) {
                hidden.add(a);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(org.telegram.messenger.MgLang.t("Yashirilgan akkauntlar"));
        if (hidden.isEmpty()) {
            builder.setMessage(org.telegram.messenger.MgLang.t("Yashirilgan akkaunt yo'q. Akkauntni yashirish: akkauntlar ro'yxatida ⚙ → \"Yashirish\"."));
            builder.setPositiveButton("OK", null);
        } else {
            CharSequence[] names = new CharSequence[hidden.size()];
            int[] icons = new int[hidden.size()];
            for (int i = 0; i < hidden.size(); i++) {
                TLRPC.User user = UserConfig.getInstance(hidden.get(i)).getCurrentUser();
                names[i] = (user != null && user.bot ? "🤖 " : "") + displayName(hidden.get(i)) + (hidden.get(i) == UserConfig.selectedAccount ? org.telegram.messenger.MgLang.t(" (joriy)") : "");
                icons[i] = R.drawable.msg_openprofile;
            }
            builder.setItems(names, icons, (dialog, which) -> {
                int account = hidden.get(which);
                if (account != UserConfig.selectedAccount && LaunchActivity.instance != null) {
                    LaunchActivity.instance.switchToAccount(account, true);
                }
            });
            builder.setNegativeButton(org.telegram.messenger.MgLang.t("Hammasini ko'rsatish"), (dialog, which) -> {
                for (int a : hidden) {
                    MgConfig.setAccountHidden(a, false);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
            });
        }
        fragment.showDialog(builder.create());
    }

    private static void pinShortcut(BaseFragment fragment, int account) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        try {
            Intent intent = new Intent(context, LaunchActivity.class);
            intent.setAction("uz.milliygram.OPEN_ACCOUNT_" + account);
            intent.putExtra("currentAccount", account);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            String name = displayName(account);
            ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(context, "mg_account_" + account)
                    .setShortLabel(name)
                    .setLongLabel("MilliyGram · " + name)
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .build();
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                ShortcutManagerCompat.requestPinShortcut(context, info, null);
            } else {
                BulletinFactory.of(fragment).createErrorBulletin(org.telegram.messenger.MgLang.t("Bu telefon yorliq qo'shishni qo'llab-quvvatlamaydi")).show();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void showAliasDialog(BaseFragment fragment, int account) {
        Context context = fragment.getParentActivity();
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
        editText.setHint(org.telegram.messenger.MgLang.t("Masalan: Ish akkaunti"));
        editText.setText(MgConfig.getAccountAlias(account));
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle(org.telegram.messenger.MgLang.t("Akkaunt nomi"));
        builder.setMessage(org.telegram.messenger.MgLang.t("Bu nom faqat shu telefondagi akkauntlar ro'yxatida ko'rinadi."));
        builder.setView(frameLayout);
        builder.setPositiveButton(org.telegram.messenger.MgLang.t("Saqlash"), (dialog, which) -> {
            MgConfig.setAccountAlias(account, editText.getText().toString());
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
        });
        builder.setNeutralButton(org.telegram.messenger.MgLang.t("Asl nom"), (dialog, which) -> MgConfig.setAccountAlias(account, ""));
        builder.setNegativeButton(org.telegram.messenger.MgLang.t("Bekor qilish"), null);
        fragment.showDialog(builder.create());
    }

    private static void confirmLogout(BaseFragment fragment, int account) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(org.telegram.messenger.MgLang.t("Akkauntdan chiqish"));
        builder.setMessage("\"" + displayName(account) + org.telegram.messenger.MgLang.t("\" akkauntidan chiqasizmi?"));
        builder.setPositiveButton(org.telegram.messenger.MgLang.t("Chiqish"), (dialog, which) -> {
            if (MgConfig.getMainAccount() == account) {
                MgConfig.setMainAccount(-1);
            }
            MgConfig.setAccountHidden(account, false);
            MgConfig.setAccountAlias(account, "");
            MessagesController.getInstance(account).performLogout(1);
        });
        builder.setNegativeButton(org.telegram.messenger.MgLang.t("Bekor qilish"), null);
        builder.makeRed(AlertDialog.BUTTON_POSITIVE);
        fragment.showDialog(builder.create());
    }
}
