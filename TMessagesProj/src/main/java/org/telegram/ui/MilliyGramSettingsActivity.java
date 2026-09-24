/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * MilliyGram sozlamalari — bo'limlarga ajratilgan asosiy sahifa.
 */
public class MilliyGramSettingsActivity extends UniversalFragment {

    private static final int ID_ABOUT = 500;
    private static final int ID_DESIGN = 501;

    private static final int[] PAGES = {
            MgSettingsPage.PAGE_GENERAL,
            MgSettingsPage.PAGE_CHATLIST,
            MgSettingsPage.PAGE_STORIES,
            MgSettingsPage.PAGE_MESSAGES,
            MgSettingsPage.PAGE_THEMES,
            MgSettingsPage.PAGE_PROFILE,
            MgSettingsPage.PAGE_NOTIFY,
            MgSettingsPage.PAGE_PRIVACY,
            MgSettingsPage.PAGE_DATA,
    };

    private static final int[] ICONS = {
            R.drawable.msg_settings,
            R.drawable.msg_groups,
            R.drawable.msg_menu_stories,
            R.drawable.msg_discussion,
            R.drawable.msg_theme,
            R.drawable.msg_openprofile,
            R.drawable.msg_notifications,
            R.drawable.msg_secret,
            R.drawable.msg_download,
    };

    @Override
    protected CharSequence getTitle() {
        return "MilliyGram sozlamalari";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(null));
        for (int i = 0; i < PAGES.length; i++) {
            items.add(UItem.asButton(PAGES[i], ICONS[i], MgSettingsPage.pageTitle(PAGES[i])));
        }
        items.add(UItem.asButton(ID_DESIGN, R.drawable.msg_palette, "Dizayn"));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(MgSettingsPage.PAGE_BACKUP, R.drawable.msg_copy, "Sozlamalarni saqlash"));
        items.add(UItem.asShadow("Sozlamalarni (sevimlilar va jildlar bilan) nusxalab, boshqa telefonda tiklash mumkin."));
        items.add(UItem.asButton(ID_ABOUT, R.drawable.msg_info, "MilliyGram haqida"));
        items.add(UItem.asShadow("MilliyGram — Telegram'ning ochiq manba kodi asosida qurilgan norasmiy klient."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ABOUT) {
            showAboutDialog();
        } else if (item.id == ID_DESIGN) {
            presentFragment(new MgDesignActivity());
        } else if (item.id >= MgSettingsPage.PAGE_GENERAL && item.id <= MgSettingsPage.PAGE_BACKUP) {
            presentFragment(new MgSettingsPage(item.id));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void showAboutDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("MilliyGram");
        builder.setMessage("MilliyGram — milliy dizayndagi norasmiy Telegram klienti.\n\n" +
                "Ilova Telegram FZ-LLC tomonidan ishlab chiqilmagan. U Telegram'ning ochiq manba kodi (GPL v2) asosida qurilgan va Telegram API'dan foydalanadi.\n\n" +
                "Xabarlaringiz to'g'ridan-to'g'ri Telegram serverlari orqali yuboriladi. MilliyGram hech qanday ma'lumot yig'maydi.");
        builder.setPositiveButton("OK", null);
        showDialog(builder.create());
    }
}
