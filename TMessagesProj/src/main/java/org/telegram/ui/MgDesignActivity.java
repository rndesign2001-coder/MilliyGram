/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.core.content.FileProvider;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.io.File;
import java.util.ArrayList;

/**
 * "Dizayn" — ilova ranglarini ekranma-ekran sozlash, mavzuni faylga saqlash va fayldan qo'llash.
 * screen = -1 — asosiy sahifa, 0..N — tanlangan ekran ranglari.
 */
public class MgDesignActivity extends UniversalFragment {

    private static final int ID_ENABLED = 1;
    private static final int ID_ACCENT = 2;
    private static final int ID_SAVE = 3;
    private static final int ID_APPLY_FILE = 4;
    private static final int ID_RESET = 5;
    private static final int ID_THEMES = 6;
    private static final int ID_DIALOG_BUTTONS = 7;
    private static final int ID_DIALOG_BG = 8;
    private static final int ID_DIALOG_TEXT = 9;
    private static final int ID_SCREEN_BASE = 100;
    private static final int ID_ITEM_BASE = 1000;

    private final int screen;

    public MgDesignActivity() {
        this(-1);
    }

    public MgDesignActivity(int screen) {
        this.screen = screen;
    }

    @Override
    protected CharSequence getTitle() {
        return screen >= 0 ? MgDesign.SCREENS[screen].title : org.telegram.messenger.MgLang.t("Dizayn");
    }

    private UItem colorRow(int id, MgDesign.Item item) {
        Integer custom = MgDesign.getCustom(item);
        int color = custom != null ? custom : MgDesign.getEffective(item);
        UItem it = UItem.asButton(id, new MgThemesActivity.ColorDot(color, 0, custom != null), item.title);
        it.textValue = custom != null ? org.telegram.messenger.MgLang.t("o'zgartirilgan") : null;
        return it;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        String mode = MgDesign.isNight() ? "tungi" : "kunduzgi";
        if (screen >= 0) {
            MgDesign.Screen s = MgDesign.SCREENS[screen];
            items.add(UItem.asHeader(s.title + " — " + mode + org.telegram.messenger.MgLang.t(" rejim")));
            for (int i = 0; i < s.items.length; i++) {
                items.add(colorRow(ID_ITEM_BASE + i, s.items[i]));
            }
            items.add(UItem.asShadow(org.telegram.messenger.MgLang.t("Rangni tanlash uchun bosing. \"Standart\" tugmasi rangni mavzudagi asl holiga qaytaradi. Kunduzgi va tungi rejim ranglari alohida saqlanadi.")));
            return;
        }
        items.add(UItem.asCheck(ID_ENABLED, org.telegram.messenger.MgLang.t("MilliyGram dizaynidan foydalanish")).setChecked(MgDesign.isEnabled()));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(org.telegram.messenger.MgLang.t("Asosiy")));
        items.add(colorRow(ID_ACCENT, MgDesign.ACCENT));
        items.add(UItem.asButton(ID_THEMES, R.drawable.msg_theme, org.telegram.messenger.MgLang.t("Milliy mavzular va chat fonlari")));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(org.telegram.messenger.MgLang.t("Ekranlar")));
        for (int i = 0; i < MgDesign.SCREENS.length; i++) {
            items.add(UItem.asButton(ID_SCREEN_BASE + i, (i + 1) + "  " + MgDesign.SCREENS[i].title));
        }
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(org.telegram.messenger.MgLang.t("Dialog")));
        items.add(colorRow(ID_DIALOG_BUTTONS, MgDesign.DIALOG_BUTTONS));
        items.add(colorRow(ID_DIALOG_BG, MgDesign.DIALOG_BG));
        items.add(colorRow(ID_DIALOG_TEXT, MgDesign.DIALOG_TEXT));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(org.telegram.messenger.MgLang.t("Dizayn")));
        items.add(UItem.asButton(ID_SAVE, R.drawable.msg_download, org.telegram.messenger.MgLang.t("Mavzuni saqlash"), ""));
        items.add(UItem.asButton(ID_APPLY_FILE, R.drawable.msg_openin, org.telegram.messenger.MgLang.t("Mavzu faylini qo'llash")));
        int count = MgDesign.customCount();
        items.add(UItem.asButton(ID_RESET, R.drawable.msg_reset, org.telegram.messenger.MgLang.t("Mavzu sozlamalarini tiklash"), count > 0 ? String.valueOf(count) : ""));
        items.add(UItem.asShadow(org.telegram.messenger.MgLang.t("Hozir ") + mode + org.telegram.messenger.MgLang.t(" rejim ranglari sozlanmoqda. Mavzu fayllari quyidagi papkada saqlanadi:\n") + shortPath()));
    }

    private String shortPath() {
        try {
            return MgDesign.getThemesDir().getAbsolutePath();
        } catch (Throwable e) {
            return "Android/data/…/MilliyGram/Themes";
        }
    }

    private void pick(MgDesign.Item item) {
        Integer custom = MgDesign.getCustom(item);
        int initial = custom != null ? custom : MgDesign.getEffective(item);
        MgColorPicker.show(this, item.title, initial, custom != null, color -> {
            MgDesign.set(item, color);
            if (!MgDesign.isEnabled()) {
                MgConfig.setBool("mg_design_on", true);
            }
            MgDesign.applyNow(this);
        });
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (screen >= 0) {
            int idx = item.id - ID_ITEM_BASE;
            MgDesign.Item[] list = MgDesign.SCREENS[screen].items;
            if (idx >= 0 && idx < list.length) {
                pick(list[idx]);
            }
            return;
        }
        if (item.id >= ID_SCREEN_BASE && item.id < ID_SCREEN_BASE + MgDesign.SCREENS.length) {
            presentFragment(new MgDesignActivity(item.id - ID_SCREEN_BASE));
            return;
        }
        switch (item.id) {
            case ID_ENABLED: {
                boolean v = !MgDesign.isEnabled();
                MgConfig.setBool("mg_design_on", v);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(v);
                }
                MgDesign.applyNow(this);
                break;
            }
            case ID_ACCENT:
                pick(MgDesign.ACCENT);
                break;
            case ID_THEMES:
                presentFragment(new MgThemesActivity());
                break;
            case ID_DIALOG_BUTTONS:
                pick(MgDesign.DIALOG_BUTTONS);
                break;
            case ID_DIALOG_BG:
                pick(MgDesign.DIALOG_BG);
                break;
            case ID_DIALOG_TEXT:
                pick(MgDesign.DIALOG_TEXT);
                break;
            case ID_SAVE:
                saveTheme();
                break;
            case ID_APPLY_FILE:
                applyFile();
                break;
            case ID_RESET: {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle(org.telegram.messenger.MgLang.t("Mavzu sozlamalarini tiklash"));
                b.setMessage(org.telegram.messenger.MgLang.t("Joriy rejimdagi barcha o'zgartirilgan ranglar asl holiga qaytadi."));
                b.setPositiveButton(org.telegram.messenger.MgLang.t("Tiklash"), (d, w) -> {
                    MgDesign.resetCurrent();
                    MgDesign.applyNow(this);
                });
                b.setNegativeButton(org.telegram.messenger.MgLang.t("Bekor qilish"), null);
                AlertDialog dialog = b.create();
                showDialog(dialog);
                dialog.redPositive();
                break;
            }
        }
    }

    private void saveTheme() {
        File file = MgDesign.exportToFile();
        if (file == null || getParentActivity() == null) {
            BulletinFactory.of(this).createErrorBulletin(org.telegram.messenger.MgLang.t("Mavzuni saqlab bo'lmadi")).show();
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle(org.telegram.messenger.MgLang.t("Mavzu saqlandi"));
        b.setMessage(file.getName() + "\n\n" + file.getParent());
        b.setPositiveButton(org.telegram.messenger.MgLang.t("Ulashish"), (d, w) -> {
            try {
                Uri uri = FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getParentActivity().startActivity(Intent.createChooser(intent, org.telegram.messenger.MgLang.t("Mavzuni ulashish")));
            } catch (Throwable e) {
                FileLog.e(e);
                BulletinFactory.of(this).createErrorBulletin(org.telegram.messenger.MgLang.t("Ulashib bo'lmadi")).show();
            }
        });
        b.setNegativeButton("OK", null);
        showDialog(b.create());
    }

    private void applyFile() {
        if (getParentActivity() == null) {
            return;
        }
        ArrayList<File> files = MgDesign.listThemeFiles();
        if (files.isEmpty()) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
            b.setTitle(org.telegram.messenger.MgLang.t("Mavzu fayllari yo'q"));
            b.setMessage(org.telegram.messenger.MgLang.t("Mavzu faylini (.mgtheme) quyidagi papkaga joylang va qayta urinib ko'ring:\n\n") + shortPath());
            b.setPositiveButton("OK", null);
            showDialog(b.create());
            return;
        }
        CharSequence[] names = new CharSequence[files.size()];
        for (int i = 0; i < files.size(); i++) {
            names[i] = files.get(i).getName();
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle(org.telegram.messenger.MgLang.t("Mavzu faylini qo'llash"));
        b.setItems(names, (d, which) -> {
            int count = MgDesign.importFromFile(files.get(which));
            if (count < 0) {
                BulletinFactory.of(this).createErrorBulletin(org.telegram.messenger.MgLang.t("Fayl noto'g'ri")).show();
            } else {
                MgDesign.applyNow(this);
                AndroidUtilitiesPost.later(() -> BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, org.telegram.messenger.MgLang.t("Qo'llanildi: ") + count + org.telegram.messenger.MgLang.t(" ta rang")).show());
            }
        });
        showDialog(b.create());
    }

    /** Kichik yordamchi: qayta chizilgandan keyin xabar ko'rsatish */
    private static class AndroidUtilitiesPost {
        static void later(Runnable r) {
            org.telegram.messenger.AndroidUtilities.runOnUIThread(r, 400);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
