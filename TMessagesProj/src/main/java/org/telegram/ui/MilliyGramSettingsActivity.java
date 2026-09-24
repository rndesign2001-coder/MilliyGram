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
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
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
 * MilliyGram qo'shimcha sozlamalari.
 */
public class MilliyGramSettingsActivity extends UniversalFragment {

    private static final int ID_SIMPLE_MODE = 1;
    private static final int ID_TRAFFIC_SAVER = 2;
    private static final int ID_AUTODOWNLOAD = 3;
    private static final int ID_PROXY = 4;
    private static final int ID_FOCUS = 5;
    private static final int ID_FOCUS_TIME = 6;
    private static final int ID_HOLIDAY = 7;
    private static final int ID_PIN_RESET = 8;
    private static final int ID_EXPORT = 9;
    private static final int ID_IMPORT = 10;
    private static final int ID_ABOUT = 11;
    private static final int ID_HIDDEN = 12;
    private static final int ID_FOLDER_ICONS = 13;
    private static final int ID_GHOST = 14;
    private static final int ID_FOLDERS = 15;
    private static final int ID_THEMES = 16;
    private static final int ID_CHAT_BG = 17;
    private static final int ID_TR_IN = 18;
    private static final int ID_TR_OUT = 19;
    private static final int ID_SIGNATURE = 20;

    private static final int[][] FOCUS_PRESETS = {
            {22 * 60, 7 * 60},
            {23 * 60, 8 * 60},
            {21 * 60, 6 * 60},
            {0, 8 * 60},
            {9 * 60, 18 * 60},
    };

    @Override
    protected CharSequence getTitle() {
        return "MilliyGram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        MgGhostMode.isReady(currentAccount); // maxfiylik sozlamalarini oldindan yuklash
        items.add(UItem.asHeader("Qulaylik"));
        items.add(UItem.asCheck(ID_SIMPLE_MODE, "Oddiy rejim (katta shrift)").setChecked(MgConfig.isSimpleMode()));
        items.add(UItem.asShadow("Xabarlar kattaroq shriftda ko'rinadi. Keksalar va ko'rishi zaif foydalanuvchilar uchun qulay."));

        items.add(UItem.asHeader("Internet va trafik"));
        items.add(UItem.asCheck(ID_TRAFFIC_SAVER, "Trafik tejash").setChecked(MgConfig.isTrafficSaver()));
        items.add(UItem.asButton(ID_AUTODOWNLOAD, R.drawable.msg_download, "Avto-yuklash sozlamalari"));
        items.add(UItem.asButton(ID_PROXY, R.drawable.msg2_data, "Proksi menejeri"));
        items.add(UItem.asShadow("Trafik tejash yoqilsa, mobil internetda rasm va videolar kamroq avtomatik yuklanadi."));

        items.add(UItem.asHeader("Fokus rejimi"));
        items.add(UItem.asCheck(ID_FOCUS, "Fokus rejimi").setChecked(MgConfig.isFocusEnabled()));
        items.add(UItem.asButton(ID_FOCUS_TIME, R.drawable.msg_recent, "Vaqt oralig'i",
                MgConfig.formatMinutes(MgConfig.getFocusStart()) + " – " + MgConfig.formatMinutes(MgConfig.getFocusEnd())));
        items.add(UItem.asShadow("Belgilangan vaqtda bildirishnomalar kelmaydi. Xabarlar yo'qolmaydi: ilovani ochganingizda hammasi joyida bo'ladi."));

        items.add(UItem.asHeader("Maxfiylik"));
        items.add(UItem.asCheck(ID_GHOST, "👻 Sharpa rejimi").setChecked(MgGhostMode.isEnabled(currentAccount)));
        items.add(UItem.asButton(ID_HIDDEN, R.drawable.msg_archive, "🙈 Yashirin bo'lim sozlamalari"));
        int locked = MgConfig.getLockedCount();
        items.add(UItem.asButton(ID_PIN_RESET, R.drawable.msg_secret, "🔒 Chat qulfi sozlamalari", locked > 0 ? String.valueOf(locked) : ""));
        items.add(UItem.asShadow("Yashirin bo'lim va chat qulfi alohida kodlarga ega. Yashirin chatlarni ko'rish: bosh ekranda qidiruv tugmasini uzoq bosing."));

        items.add(UItem.asHeader("Dizayn"));
        items.add(UItem.asButton(ID_THEMES, R.drawable.msg_theme, "🎨 MilliyGram mavzulari", MgThemeCurrentName()));
        items.add(UItem.asButton(ID_CHAT_BG, R.drawable.msg_background, "🖼 Chat foni"));
        items.add(UItem.asCheck(ID_HOLIDAY, "Bayram tabriklari").setChecked(MgConfig.isHolidayDecorEnabled()));
        items.add(UItem.asButton(ID_FOLDERS, R.drawable.msg_folders, "📁 Jildlar (ikonkalar, lokal jildlar)"));
        items.add(UItem.asShadow("Ikonkali jildlarda tanlangan jild nomi tepada yoziladi. Jildni uzoq bosib tartiblash va tahrirlash mumkin. Bayram kunlari sarlavhada tabrik ko'rinadi."));

        items.add(UItem.asHeader("Tarjima va uzatish"));
        items.add(UItem.asButton(ID_TR_IN, R.drawable.msg_translate, "Xabarlarni tarjima qilish tili", MgTranslate.nameOf(org.telegram.ui.Components.TranslateAlert2.getToLanguage())));
        items.add(UItem.asButton(ID_TR_OUT, R.drawable.msg_language, "Yozganimni tarjima qilish tili", MgTranslate.nameOf(MgTranslate.lastOutgoingLang())));
        items.add(UItem.asButton(ID_SIGNATURE, R.drawable.msg_edit, "✍️ Maxsus uzatish imzosi", org.telegram.messenger.MgConfig.getString("mg_cf_signature", "").isEmpty() ? "yo'q" : "bor"));
        items.add(UItem.asShadow("Xabarni uzoq bosing → \"Maxsus uzatish\": matnni tahrirlab, havolalarsiz, tarjima qilib, o'z nomingizdan uzating. Yozgan matningizni tarjima qilish: yuborish tugmasini uzoq bosing → \"Tarjima qilib yozish\"."));

        items.add(UItem.asHeader("Zaxira"));
        items.add(UItem.asButton(ID_EXPORT, R.drawable.msg_copy, "Sozlamalarni nusxalash"));
        items.add(UItem.asButton(ID_IMPORT, R.drawable.msg_download, "Sozlamalarni tiklash"));
        items.add(UItem.asShadow("Sozlamalar matn ko'rinishida nusxalanadi. Uni Saqlangan xabarlarga yuborib qo'ying va yangi telefonda qayta joylang."));

        items.add(UItem.asButton(ID_ABOUT, R.drawable.msg_info, "MilliyGram haqida"));
        items.add(UItem.asShadow("MilliyGram — Telegram'ning ochiq manba kodi asosida qurilgan norasmiy klient."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_SIMPLE_MODE: {
                boolean value = !MgConfig.isSimpleMode();
                MgConfig.setSimpleMode(value);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(value);
                }
                break;
            }
            case ID_TRAFFIC_SAVER: {
                boolean value = !MgConfig.isTrafficSaver();
                MgConfig.setTrafficSaver(value);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(value);
                }
                break;
            }
            case ID_AUTODOWNLOAD:
                presentFragment(new DataSettingsActivity());
                break;
            case ID_PROXY:
                presentFragment(new ProxyListActivity());
                break;
            case ID_FOCUS: {
                boolean value = !MgConfig.isFocusEnabled();
                MgConfig.setBool("focus_enabled", value);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(value);
                }
                break;
            }
            case ID_FOCUS_TIME:
                showFocusTimeDialog();
                break;
            case ID_HOLIDAY: {
                boolean value = !MgConfig.isHolidayDecorEnabled();
                MgConfig.setBool("holiday_decor", value);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(value);
                }
                break;
            }
            case ID_PIN_RESET:
                MgChatLockSettingsActivity.open(this);
                break;
            case ID_FOLDERS:
                presentFragment(new MgFoldersActivity());
                break;
            case ID_THEMES:
                presentFragment(new MgThemesActivity());
                break;
            case ID_CHAT_BG:
                presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL));
                break;
            case ID_TR_IN:
                MgTranslate.chooseTarget(this, "Xabarlar qaysi tilga tarjima qilinsin?", org.telegram.ui.Components.TranslateAlert2.getToLanguage(), lang -> {
                    org.telegram.ui.Components.TranslateAlert2.setToLanguage(lang);
                    if (listView != null) listView.adapter.update(true);
                });
                break;
            case ID_TR_OUT:
                MgTranslate.chooseLanguage(this, "Yozganingiz qaysi tilga tarjima qilinsin?", lang -> {
                    if (listView != null) listView.adapter.update(true);
                });
                break;
            case ID_SIGNATURE:
                MgCustomForward.editSignature(this, null);
                break;
            case ID_EXPORT: {
                String json = MgConfig.exportSettings();
                if (json != null && AndroidUtilities.addToClipboard(json)) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Sozlamalar nusxalandi").show();
                }
                break;
            }
            case ID_IMPORT:
                showImportDialog();
                break;
            case ID_ABOUT:
                showAboutDialog();
                break;
            case ID_HIDDEN:
                MgHiddenSettingsActivity.open(this);
                break;
            case ID_GHOST:
                toggleGhost(view);
                break;
            case ID_FOLDER_ICONS: {
                boolean value = !MgConfig.isFolderIconTabs();
                MgConfig.setBool("folder_icon_tabs", value);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(value);
                }
                getNotificationCenter().postNotificationName(org.telegram.messenger.NotificationCenter.dialogFiltersUpdated);
                break;
            }
        }
    }

    private static String MgThemeCurrentName() {
        try {
            org.telegram.ui.ActionBar.Theme.ThemeInfo t = org.telegram.ui.ActionBar.Theme.getActiveTheme();
            if (t != null) {
                boolean night = "Dark Blue".equals(t.getKey());
                if (night || "Blue".equals(t.getKey())) {
                    int[] ids = night ? org.telegram.ui.ActionBar.Theme.MG_THEME_NIGHT_IDS : org.telegram.ui.ActionBar.Theme.MG_THEME_DAY_IDS;
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i] == t.currentAccentId) {
                            return org.telegram.ui.ActionBar.Theme.MG_THEME_NAMES[i];
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void toggleGhost(View view) {
        boolean enable = !MgGhostMode.isEnabled(currentAccount);
        Runnable apply = () -> MgGhostMode.setEnabled(currentAccount, enable, error -> {
            if (error == null) {
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enable);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, enable ? "Sharpa rejimi yoqildi" : "Avvalgi maxfiylik sozlamalari tiklandi").show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(error).show();
            }
        });
        if (!enable || getParentActivity() == null) {
            apply.run();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("👻 Sharpa rejimi");
        builder.setMessage("Telegram maxfiylik sozlamalarida quyidagilar \"Hech kim\" ga o'zgaradi:\n\n" +
                "• oxirgi marta onlayn bo'lgan vaqtingiz va onlayn holatingiz;\n" +
                "• telefon raqamingiz;\n" +
                "• uzatilgan xabarlaringizdagi profilingizga havola.\n\n" +
                "Eslatma: Telegram qoidasiga ko'ra, o'z vaqtingizni yashirsangiz, boshqalarning ham \"oxirgi marta onlayn\" vaqtini ko'ra olmaysiz.\n\n" +
                "O'chirganingizda avvalgi sozlamalaringiz aynan tiklanadi.");
        builder.setPositiveButton("Yoqish", (dialog, which) -> apply.run());
        builder.setNegativeButton("Bekor qilish", null);
        showDialog(builder.create());
    }

    private void showFocusTimeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] titles = new CharSequence[FOCUS_PRESETS.length];
        for (int i = 0; i < FOCUS_PRESETS.length; i++) {
            titles[i] = MgConfig.formatMinutes(FOCUS_PRESETS[i][0]) + " – " + MgConfig.formatMinutes(FOCUS_PRESETS[i][1]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("Fokus vaqti");
        builder.setItems(titles, (dialog, which) -> {
            MgConfig.setInt("focus_start", FOCUS_PRESETS[which][0]);
            MgConfig.setInt("focus_end", FOCUS_PRESETS[which][1]);
            listView.adapter.update(true);
        });
        showDialog(builder.create());
    }

    private void showImportDialog() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMaxLines(6);
        editText.setHint("Nusxalangan matnni shu yerga joylang");
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle("Sozlamalarni tiklash");
        builder.setView(frameLayout);
        builder.setPositiveButton("Tiklash", (dialog, which) -> {
            int count = MgConfig.importSettings(editText.getText().toString());
            if (count < 0) {
                BulletinFactory.of(this).createErrorBulletin("Matn noto'g'ri. MilliyGram'dan nusxalangan matnni joylang").show();
            } else {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Tiklandi: " + count + " ta sozlama").show();
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        showDialog(builder.create());
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
