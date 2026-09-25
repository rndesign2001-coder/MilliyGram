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
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLocalFolders;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Jildlar boshqaruvi: lokal jildlar (O'qilmagan, Guruhlar, Profillar, Kanallar, Botlar, Admin...),
 * ikonka tanlash, yashirilgan tablar.
 */
public class MgFoldersActivity extends UniversalFragment {

    private static final int ID_ICON_TABS = 1;
    private static final int ID_LOCAL = 2;
    private static final int ID_EDIT_CLOUD = 3;
    private static final int ID_NEW_CATEGORY = 4;
    private static final int ID_LOCAL_BASE = 1000;
    private static final int ID_HIDDEN_BASE = 3000;

    private final ArrayList<MgLocalFolders.Entry> entries = new ArrayList<>();
    private final ArrayList<MessagesController.DialogFilter> hiddenTabs = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return "Jildlar";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        entries.clear();
        entries.addAll(MgLocalFolders.load(currentAccount));
        items.add(UItem.asHeader("Ko'rinish"));
        items.add(UItem.asCheck(ID_ICON_TABS, "Jildlarni ikonkada ko'rsatish").setChecked(MgConfig.isFolderIconTabs()));
        items.add(UItem.asCheck(ID_LOCAL, "Lokal jildlar").setChecked(MgLocalFolders.isEnabledFeature()));
        items.add(UItem.asButton(ID_EDIT_CLOUD, R.drawable.msg_folders, "Bulut jildlarini tahrirlash"));
        items.add(UItem.asShadow("Lokal jildlar faqat shu telefonda ishlaydi, serverga yuborilmaydi va Telegram jild limitiga kirmaydi."));

        if (MgLocalFolders.isEnabledFeature()) {
            items.add(UItem.asHeader("Lokal jildlar"));
            for (int i = 0; i < entries.size(); i++) {
                MgLocalFolders.Entry e = entries.get(i);
                Integer icon = MgLocalFolders.ICONS.get(e.icon);
                String custom = MgConfig.getString("folder_icon_" + currentAccount + "_" + e.id, null);
                if (custom != null && MgLocalFolders.ICONS.containsKey(custom)) {
                    icon = MgLocalFolders.ICONS.get(custom);
                }
                String sub = e.type == MgLocalFolders.TYPE_CUSTOM ? "Toifa · " + e.always.size() + " ta chat · " + (e.enabled ? "yoqilgan" : "yashirilgan") : (e.enabled ? "Yoqilgan" : "Yashirilgan");
                items.add(UItem.asButtonCheck(ID_LOCAL_BASE + i, e.name, sub).setChecked(e.enabled));
            }
            items.add(UItem.asButton(ID_NEW_CATEGORY, R.drawable.msg_add, "Yangi toifa yaratish"));
            items.add(UItem.asShadow("Yoqish/yashirish uchun bosing. Nomini yoki ikonkasini o'zgartirish uchun uzoq bosing.\n\n• Admin — siz egasi yoki admini bo'lgan barcha guruh va kanallar\n• Mening kanallarim / guruhlarim — o'zingiz egasi bo'lganlar\n• Admin kanallar / guruhlar — siz admin, lekin egasi boshqa odam\n• Notanishlar — kontaktda yo'q odamlar (himoya yoqilganda)"));
        }

        hiddenTabs.clear();
        for (MessagesController.DialogFilter f : getMessagesController().getDialogFilters()) {
            if (!f.isDefault() && MgLocalFolders.isTabHidden(currentAccount, f.id)) {
                hiddenTabs.add(f);
            }
        }
        if (!hiddenTabs.isEmpty()) {
            items.add(UItem.asHeader("Yashirilgan tablar"));
            for (int i = 0; i < hiddenTabs.size(); i++) {
                items.add(UItem.asButton(ID_HIDDEN_BASE + i, R.drawable.msg_folders, hiddenTabs.get(i).name, "Ko'rsatish"));
            }
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ICON_TABS) {
            boolean v = !MgConfig.isFolderIconTabs();
            MgConfig.setBool("folder_icon_tabs", v);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(v);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            return;
        }
        if (item.id == ID_LOCAL) {
            boolean v = !MgLocalFolders.isEnabledFeature();
            MgConfig.setBool("local_folders", v);
            MgLocalFolders.inject(getMessagesController());
            getMessagesController().lockFiltersInternal();
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            listView.adapter.update(true);
            return;
        }
        if (item.id == ID_EDIT_CLOUD) {
            presentFragment(new FiltersSetupActivity());
            return;
        }
        if (item.id == ID_NEW_CATEGORY) {
            MgDialogActions.askName(this, "Yangi toifa", "", name -> {
                MgLocalFolders.createCategory(currentAccount, name, new ArrayList<>());
                listView.adapter.update(true);
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Toifa yaratildi. Chatlarni belgilab ⋮ → \"Toifaga qo'shish\"").show();
            });
            return;
        }
        int local = item.id - ID_LOCAL_BASE;
        if (local >= 0 && local < entries.size()) {
            MgLocalFolders.Entry e = entries.get(local);
            MgLocalFolders.setEnabled(currentAccount, e.id, !e.enabled);
            listView.adapter.update(true);
            return;
        }
        int hidden = item.id - ID_HIDDEN_BASE;
        if (hidden >= 0 && hidden < hiddenTabs.size()) {
            MgLocalFolders.setTabHidden(currentAccount, hiddenTabs.get(hidden).id, false);
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int local = item.id - ID_LOCAL_BASE;
        if (local < 0 || local >= entries.size()) {
            return false;
        }
        MgLocalFolders.Entry e = entries.get(local);
        ItemOptions o = ItemOptions.makeOptions(this, view)
                .add(R.drawable.msg_edit, "Nomini o'zgartirish", () -> showRename(e))
                .add(R.drawable.msg_palette, "Ikonka tanlash", () -> showIconPicker(e.id));
        if (e.type == MgLocalFolders.TYPE_CUSTOM) {
            o.add(R.drawable.msg_delete, "Toifani o'chirish", true, () -> {
                MgLocalFolders.deleteCategory(currentAccount, e.id);
                listView.adapter.update(true);
            });
        }
        o.show();
        return true;
    }

    private void showIconPicker(int filterId) {
        if (getParentActivity() == null) {
            return;
        }
        String cur = MgConfig.getString("folder_icon_" + currentAccount + "_" + filterId, null);
        if (cur == null) {
            for (MgLocalFolders.Entry e : entries) {
                if (e.id == filterId) {
                    cur = e.icon;
                }
            }
        }
        MgIconPicker.show(this, "Jild ikonkasi", cur, key -> {
            MgLocalFolders.setIconKey(currentAccount, filterId, key);
            listView.adapter.update(true);
        });
    }

    private void showRename(MgLocalFolders.Entry e) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setSingleLine(true);
        editText.setText(e.name);
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle("Jild nomi");
        builder.setView(frameLayout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String name = editText.getText().toString().trim();
            if (!name.isEmpty()) {
                MgLocalFolders.renameLocal(currentAccount, e.id, name);
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 250);
    }
}
