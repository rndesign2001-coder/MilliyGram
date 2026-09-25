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
import org.telegram.messenger.MgLocalFolders;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * MilliyGram sozlamalarining bo'limlari (Asosiy, Chat ro'yxati, Hikoyalar, Xabarlar, Mavzular,
 * Profil, Bildirishnomalar, Maxfiylik, Yuklamalar, Zaxira).
 */
public class MgSettingsPage extends UniversalFragment {

    public static final int PAGE_GENERAL = 1;
    public static final int PAGE_CHATLIST = 2;
    public static final int PAGE_STORIES = 3;
    public static final int PAGE_MESSAGES = 4;
    public static final int PAGE_THEMES = 5;
    public static final int PAGE_PROFILE = 6;
    public static final int PAGE_NOTIFY = 7;
    public static final int PAGE_PRIVACY = 8;
    public static final int PAGE_DATA = 9;
    public static final int PAGE_BACKUP = 10;
    public static final int PAGE_AUTOMATION = 11;

    // Element identifikatorlari
    private static final int ID_SIMPLE_MODE = 1;
    private static final int ID_HOLIDAY = 2;
    private static final int ID_SHAKE = 3;
    private static final int ID_ICON_TABS = 10;
    private static final int ID_ARCHIVE_TABS = 11;
    private static final int ID_ARCHIVE_HIDDEN = 12;
    private static final int ID_FOLDERS = 13;
    private static final int ID_CLOUD_FOLDERS = 14;
    private static final int ID_ARCHIVE_SETTINGS = 15;
    private static final int ID_CATEGORIES = 16;
    private static final int ID_HIDE_STORIES = 20;
    private static final int ID_TR_IN = 30;
    private static final int ID_TR_OUT = 31;
    private static final int ID_TR_ENGINE = 32;
    private static final int ID_SIGNATURE = 33;
    private static final int ID_THEMES = 40;
    private static final int ID_CHAT_BG = 41;
    private static final int ID_DESIGN = 42;
    private static final int ID_CHAT_SETTINGS = 43;
    private static final int ID_SHOW_ID = 50;
    private static final int ID_FAKE_NAME = 51;
    private static final int ID_FOCUS = 60;
    private static final int ID_FOCUS_TIME = 61;
    private static final int ID_NOTIFY_SETTINGS = 62;
    private static final int ID_GHOST = 70;
    private static final int ID_HIDDEN = 71;
    private static final int ID_LOCK = 72;
    private static final int ID_TRAFFIC = 80;
    private static final int ID_AUTODOWNLOAD = 81;
    private static final int ID_PROXY = 82;
    private static final int ID_STORAGE = 83;
    private static final int ID_AA_ON = 100;
    private static final int ID_AA_TEXT = 101;
    private static final int ID_AA_SCOPE = 102;
    private static final int ID_AA_COOLDOWN = 103;
    private static final int ID_AA_RESET = 104;
    private static final int ID_AT_PREVIEW = 105;
    private static final int ID_AJ_ALL = 106;
    private static final int ID_STRANGER_ON = 110;
    private static final int ID_STRANGER_NOTIFY = 111;
    private static final int ID_EXPORT = 90;
    private static final int ID_IMPORT = 91;

    private static final int[][] FOCUS_PRESETS = {
            {22 * 60, 7 * 60},
            {23 * 60, 8 * 60},
            {0, 8 * 60},
            {9 * 60, 18 * 60},
    };

    private final int page;

    public MgSettingsPage(int page) {
        this.page = page;
    }

    public static String pageTitle(int page) {
        switch (page) {
            case PAGE_GENERAL: return "Asosiy";
            case PAGE_CHATLIST: return "Chat ro'yxati";
            case PAGE_STORIES: return "Hikoyalar";
            case PAGE_MESSAGES: return "Xabarlar va tarjima";
            case PAGE_THEMES: return "Mavzular";
            case PAGE_PROFILE: return "Profil";
            case PAGE_NOTIFY: return "Bildirishnomalar";
            case PAGE_PRIVACY: return "Maxfiylik va xavfsizlik";
            case PAGE_DATA: return "Yuklamalar va trafik";
            case PAGE_BACKUP: return "Sozlamalarni saqlash";
            case PAGE_AUTOMATION: return "Avtomatlashtirish";
        }
        return "MilliyGram";
    }

    @Override
    protected CharSequence getTitle() {
        return pageTitle(page);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        switch (page) {
            case PAGE_GENERAL:
                items.add(UItem.asHeader("Qulaylik"));
                items.add(UItem.asCheck(ID_SIMPLE_MODE, "Oddiy rejim (katta shrift)").setChecked(MgConfig.isSimpleMode()));
                items.add(UItem.asCheck(ID_HOLIDAY, "Bayram tabriklari").setChecked(MgConfig.isHolidayDecorEnabled()));
                items.add(UItem.asCheck(ID_SHAKE, "Silkitib yashirish").setChecked(MgConfig.isShakeToHide()));
                items.add(UItem.asShadow("Oddiy rejimda xabarlar kattaroq shriftda ko'rinadi. Bayram kunlari sarlavhada tabrik chiqadi. Telefonni silkitsangiz, yashirin bo'lim va qulflangan chatlar darhol yopiladi."));
                break;
            case PAGE_CHATLIST: {
                items.add(UItem.asHeader("Jildlar"));
                items.add(UItem.asCheck(ID_ICON_TABS, "Jildlarni ikonkada ko'rsatish").setChecked(MgConfig.isFolderIconTabs()));
                items.add(UItem.asButton(ID_FOLDERS, R.drawable.msg_folders, "Lokal jildlar va ikonkalar"));
                items.add(UItem.asButton(ID_CATEGORIES, R.drawable.msg_addfolder, "Toifalar", String.valueOf(MgLocalFolders.getCategories(currentAccount).size())));
                items.add(UItem.asButton(ID_CLOUD_FOLDERS, R.drawable.msg_customize, "Bulut jildlarini tahrirlash"));
                items.add(UItem.asShadow("Jildni uzoq bossangiz: tahrirlash, ikonka tanlash, tabni yashirish. Chatlarni belgilab ⋮ → \"Toifaga qo'shish\" orqali o'z toifalaringizni yarating."));
                items.add(UItem.asHeader("Arxiv"));
                items.add(UItem.asCheck(ID_ARCHIVE_TABS, "Arxivni barcha jildlarda ko'rsatish").setChecked(MgConfig.isArchiveInAllTabs()));
                items.add(UItem.asCheck(ID_ARCHIVE_HIDDEN, "Arxivni yashirish (pastga tortib ochiladi)").setChecked(SharedConfig.archiveHidden));
                items.add(UItem.asButton(ID_ARCHIVE_SETTINGS, R.drawable.msg_archive, "Arxiv sozlamalari"));
                items.add(UItem.asShadow("Yashirin arxivni ko'rish uchun chatlar ro'yxatini tepadan pastga torting. \"Arxivlangan chatlar\" qatorini uzoq bosib yashirish yoki mahkamlash mumkin. Arxiv ichidagi ⊞ tugmasi chatlarni turlarga ajratadi."));
                break;
            }
            case PAGE_STORIES:
                items.add(UItem.asCheck(ID_HIDE_STORIES, "Hikoyalar panelini yashirish").setChecked(MgConfig.getBool("hide_stories", false)));
                items.add(UItem.asShadow("Chatlar ro'yxati tepasidagi hikoyalar qatori ko'rinmaydi. Hikoyalarni profil orqali ko'rish mumkin."));
                break;
            case PAGE_MESSAGES:
                items.add(UItem.asHeader("Tarjima"));
                items.add(UItem.asButton(ID_TR_IN, R.drawable.msg_translate, "Xabarlarni tarjima qilish tili", MgTranslate.nameOf(TranslateAlert2.getToLanguage())));
                items.add(UItem.asButton(ID_TR_OUT, R.drawable.msg_language, "Yozganimni tarjima qilish tili", MgTranslate.nameOf(MgTranslate.lastOutgoingLang())));
                items.add(UItem.asButton(ID_TR_ENGINE, R.drawable.msg_customize, "Tarjima xizmati", MgTranslate.ENGINE_NAMES[Math.max(0, Math.min(2, MgTranslate.getEngine()))]));
                items.add(UItem.asShadow("Yozgan matningizni tarjima qilish: yuborish tugmasini uzoq bosing → \"Tarjima qilib yozish\". Avtomatik rejimda Telegram tarjimoni ishlamasa, Google Tarjimon ishlatiladi."));
                items.add(UItem.asHeader("Maxsus uzatish"));
                items.add(UItem.asButton(ID_SIGNATURE, R.drawable.msg_edit, "Imzo", MgConfig.getString("mg_cf_signature", "").isEmpty() ? "yo'q" : "bor"));
                items.add(UItem.asShadow("Xabarni uzoq bosing → \"Maxsus uzatish\": matnni tahrirlab, havola va @larni tozalab, tarjima qilib, o'z nomingizdan uzating."));
                break;
            case PAGE_THEMES:
                items.add(UItem.asButton(ID_THEMES, R.drawable.msg_theme, "MilliyGram mavzulari", MgThemesActivity.currentName()));
                items.add(UItem.asButton(ID_CHAT_BG, R.drawable.msg_background, "Chat foni"));
                items.add(UItem.asButton(ID_DESIGN, R.drawable.msg_palette, "Dizayn (ranglarni sozlash)"));
                items.add(UItem.asButton(ID_CHAT_SETTINGS, R.drawable.msg_msgbubble3, "Chat sozlamalari (shrift, burchaklar)"));
                items.add(UItem.asShadow("Har bir milliy mavzuning o'z naqshli chat foni bor. \"Dizayn\" bo'limida har bir ekran rangini alohida o'zgartirish mumkin."));
                break;
            case PAGE_PROFILE: {
                String fake = MgConfig.getFakeName();
                items.add(UItem.asCheck(ID_SHOW_ID, "Profilda ID ko'rsatish").setChecked(MgConfig.getBool("show_profile_id", true)));
                items.add(UItem.asButton(ID_FAKE_NAME, R.drawable.msg_openprofile, "Yolg'on ism", fake == null || fake.isEmpty() ? "o'chirilgan" : fake));
                items.add(UItem.asShadow("ID qatorini bossangiz, nusxalanadi. Yolg'on ism faqat sizning ekraningizda ko'rinadi (skrinshotlar uchun)."));
                break;
            }
            case PAGE_NOTIFY:
                items.add(UItem.asHeader("Fokus rejimi"));
                items.add(UItem.asCheck(ID_FOCUS, "Fokus rejimi").setChecked(MgConfig.isFocusEnabled()));
                items.add(UItem.asButton(ID_FOCUS_TIME, R.drawable.msg_recent, "Vaqt oralig'i",
                        MgConfig.formatMinutes(MgConfig.getFocusStart()) + " – " + MgConfig.formatMinutes(MgConfig.getFocusEnd())));
                items.add(UItem.asShadow("Belgilangan vaqtda bildirishnomalar kelmaydi. Xabarlar yo'qolmaydi."));
                items.add(UItem.asButton(ID_NOTIFY_SETTINGS, R.drawable.msg_notifications, "Telegram bildirishnomalari"));
                items.add(UItem.asShadow(null));
                break;
            case PAGE_PRIVACY: {
                items.add(UItem.asCheck(ID_GHOST, "Sharpa rejimi").setChecked(MgGhostMode.isEnabled(currentAccount)));
                items.add(UItem.asButton(ID_HIDDEN, R.drawable.msg_stories_myhide, "Yashirin bo'lim"));
                int locked = MgConfig.getLockedCount();
                items.add(UItem.asButton(ID_LOCK, R.drawable.msg_secret, "Chat qulfi", locked > 0 ? String.valueOf(locked) : ""));
                items.add(UItem.asShadow(null));
                items.add(UItem.asHeader("Notanishlardan himoya"));
                items.add(UItem.asCheck(ID_STRANGER_ON, "Notanishlardan himoya").setChecked(org.telegram.messenger.MgStrangers.isEnabled(currentAccount)));
                items.add(UItem.asCheck(ID_STRANGER_NOTIFY, "Notanishlardan bildirishnoma").setChecked(org.telegram.messenger.MgStrangers.isNotifyEnabled()));
                items.add(UItem.asShadow("Kontaktingizda bo'lmagan odamlarning shaxsiy chatlari asosiy ro'yxatdan olinib, \"Notanishlar\" jildiga tushadi va ovozsiz bo'ladi. Chatni asosiy ro'yxatga qaytarish: belgilab ⋮ → \"Notanish emas\". Faqat joriy akkaunt uchun."));
                items.add(UItem.asShadow("Yashirin bo'lim va chat qulfi alohida kodlarga ega. Yashirin chatlarni ochish: bosh ekranda qidiruv tugmasini uzoq bosing."));
                break;
            }
            case PAGE_DATA:
                items.add(UItem.asCheck(ID_TRAFFIC, "Trafik tejash").setChecked(MgConfig.isTrafficSaver()));
                items.add(UItem.asButton(ID_AUTODOWNLOAD, R.drawable.msg_download, "Avto-yuklash sozlamalari"));
                items.add(UItem.asButton(ID_STORAGE, R.drawable.msg_clearcache, "Xotira va kesh"));
                items.add(UItem.asButton(ID_PROXY, R.drawable.msg2_data, "Proksi menejeri"));
                items.add(UItem.asShadow("Trafik tejash yoqilsa, mobil internetda rasm va videolar kamroq avtomatik yuklanadi. Bitta chat keshini tozalash: chatni belgilang → ⋮ → \"Keshni tozalash\"."));
                break;
            case PAGE_AUTOMATION: {
                items.add(UItem.asHeader("Avto-javob"));
                items.add(UItem.asCheck(ID_AA_ON, "Avto-javobni yoqish").setChecked(org.telegram.messenger.MgAutoAnswer.isEnabled()));
                String t = org.telegram.messenger.MgAutoAnswer.getText();
                items.add(UItem.asButton(ID_AA_TEXT, R.drawable.msg_edit, "Javob matni", t.isEmpty() ? "kiritilmagan" : (t.length() > 18 ? t.substring(0, 18) + "…" : t)));
                items.add(UItem.asButton(ID_AA_SCOPE, R.drawable.msg_contacts, "Kimlarga", org.telegram.messenger.MgAutoAnswer.SCOPE_NAMES[Math.max(0, Math.min(2, org.telegram.messenger.MgAutoAnswer.getScope()))]));
                items.add(UItem.asButton(ID_AA_COOLDOWN, R.drawable.msg_recent, "Qanchalik tez-tez", org.telegram.messenger.MgAutoAnswer.cooldownName()));
                items.add(UItem.asButton(ID_AA_RESET, R.drawable.msg_reset, "Javob berilganlar ro'yxatini tozalash"));
                items.add(UItem.asShadow("Shaxsiy chatga kelgan xabarga avtomatik javob yuboriladi. Siz o'zingiz yozgan chatga tanlangan vaqt ichida qayta avto-javob ketmaydi. Botlar va Telegram xizmat akkauntlariga javob berilmaydi. Ilova ishlab turgan paytda ishlaydi."));
                items.add(UItem.asHeader("Avto-tarjima va avto-imzo"));
                items.add(UItem.asCheck(ID_AT_PREVIEW, "Tarjimani yuborishdan oldin ko'rsatish").setChecked(MgChatFeatures.isAutoTranslatePreview()));
                items.add(UItem.asShadow("Chatni oching → ⋮ → \"Avto-tarjima\" — shu chatga yozganlaringiz tanlangan tilga o'girilib yuboriladi. ⋮ → \"Avto-imzo\" — har bir xabar oxiriga imzo qo'shiladi."));
                items.add(UItem.asHeader("Qo'shilish so'rovlari"));
                items.add(UItem.asCheck(ID_AJ_ALL, "Barcha chatlarda avtomatik qabul qilish").setChecked(MgChatFeatures.isAutoAcceptAll()));
                items.add(UItem.asShadow("Siz admin bo'lgan (taklif qilish huquqi bor) kanal va guruhlarga kelgan qo'shilish so'rovlari avtomatik qabul qilinadi. Bitta chat uchun: chatni oching → ⋮ → \"Qo'shilish so'rovlari\"."));
                break;
            }
            case PAGE_BACKUP:
                items.add(UItem.asButton(ID_EXPORT, R.drawable.msg_copy, "Sozlamalarni nusxalash"));
                items.add(UItem.asButton(ID_IMPORT, R.drawable.msg_download, "Sozlamalarni tiklash"));
                items.add(UItem.asShadow("Sozlamalar matn ko'rinishida nusxalanadi (sevimlilar va jildlar bilan). Uni Saqlangan xabarlarga yuborib qo'ying va yangi telefonda qayta joylang. Maxfiy kodlar nusxalanmaydi."));
                break;
        }
    }

    private void toggle(View view, String key, boolean def) {
        boolean v = !MgConfig.getBool(key, def);
        MgConfig.setBool(key, v);
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(v);
        }
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
            case ID_HOLIDAY:
                toggle(view, "holiday_decor", MgConfig.isHolidayDecorEnabled());
                break;
            case ID_SHAKE:
                toggle(view, "shake_to_hide", true);
                break;
            case ID_ICON_TABS:
                toggle(view, "folder_icon_tabs", MgConfig.isFolderIconTabs());
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                break;
            case ID_ARCHIVE_TABS:
                toggle(view, "archive_all_tabs", true);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                break;
            case ID_ARCHIVE_HIDDEN:
                SharedConfig.toggleArchiveHidden();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.archiveHidden);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                break;
            case ID_ARCHIVE_SETTINGS:
                presentFragment(new ArchiveSettingsActivity());
                break;
            case ID_FOLDERS:
            case ID_CATEGORIES:
                presentFragment(new MgFoldersActivity());
                break;
            case ID_CLOUD_FOLDERS:
                presentFragment(new FiltersSetupActivity());
                break;
            case ID_HIDE_STORIES:
                toggle(view, "hide_stories", false);
                getNotificationCenter().postNotificationName(NotificationCenter.storiesUpdated);
                break;
            case ID_TR_IN:
                MgTranslate.chooseTarget(this, "Xabarlar qaysi tilga tarjima qilinsin?", TranslateAlert2.getToLanguage(), lang -> {
                    TranslateAlert2.setToLanguage(lang);
                    listView.adapter.update(true);
                });
                break;
            case ID_TR_OUT:
                MgTranslate.chooseLanguage(this, "Yozganingiz qaysi tilga tarjima qilinsin?", lang -> listView.adapter.update(true));
                break;
            case ID_TR_ENGINE: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Tarjima xizmati");
                CharSequence[] names = new CharSequence[MgTranslate.ENGINE_NAMES.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = MgTranslate.ENGINE_NAMES[i] + (i == MgTranslate.getEngine() ? "  ✓" : "");
                }
                b.setItems(names, (d, which) -> {
                    MgConfig.setInt("mg_translate_engine", which);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_SIGNATURE:
                MgCustomForward.editSignature(this, () -> listView.adapter.update(true));
                break;
            case ID_THEMES:
                presentFragment(new MgThemesActivity());
                break;
            case ID_CHAT_BG:
                presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL));
                break;
            case ID_DESIGN:
                presentFragment(new MgDesignActivity());
                break;
            case ID_CHAT_SETTINGS:
                presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
                break;
            case ID_SHOW_ID:
                toggle(view, "show_profile_id", true);
                break;
            case ID_FAKE_NAME:
                MgHiddenSettingsActivity.open(this);
                break;
            case ID_FOCUS:
                toggle(view, "focus_enabled", MgConfig.isFocusEnabled());
                break;
            case ID_FOCUS_TIME:
                showFocusTimeDialog();
                break;
            case ID_NOTIFY_SETTINGS:
                presentFragment(new NotificationsSettingsActivity());
                break;
            case ID_GHOST:
                toggleGhost(view);
                break;
            case ID_HIDDEN:
                MgHiddenSettingsActivity.open(this);
                break;
            case ID_LOCK:
                MgChatLockSettingsActivity.open(this);
                break;
            case ID_TRAFFIC: {
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
            case ID_STORAGE:
                presentFragment(new CacheControlActivity());
                break;
            case ID_PROXY:
                presentFragment(new ProxyListActivity());
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
            case ID_AA_ON: {
                boolean v = !org.telegram.messenger.MgAutoAnswer.isEnabled();
                if (v && org.telegram.messenger.MgAutoAnswer.getText().isEmpty()) {
                    editAutoAnswerText(true);
                    break;
                }
                org.telegram.messenger.MgAutoAnswer.setEnabled(v);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(v);
                }
                break;
            }
            case ID_AA_TEXT:
                editAutoAnswerText(false);
                break;
            case ID_AA_SCOPE: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Kimlarga javob berilsin?");
                CharSequence[] names = new CharSequence[org.telegram.messenger.MgAutoAnswer.SCOPE_NAMES.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = org.telegram.messenger.MgAutoAnswer.SCOPE_NAMES[i] + (i == org.telegram.messenger.MgAutoAnswer.getScope() ? "  ✓" : "");
                }
                b.setItems(names, (d, w) -> {
                    MgConfig.setInt("aa_scope", w);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_AA_COOLDOWN: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Bitta chatga qanchalik tez-tez javob berilsin?");
                CharSequence[] names = new CharSequence[org.telegram.messenger.MgAutoAnswer.COOLDOWN_NAMES.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = org.telegram.messenger.MgAutoAnswer.COOLDOWN_NAMES[i] + (org.telegram.messenger.MgAutoAnswer.COOLDOWNS[i] == org.telegram.messenger.MgAutoAnswer.getCooldownHours() ? "  ✓" : "");
                }
                b.setItems(names, (d, w) -> {
                    MgConfig.setInt("aa_cooldown", org.telegram.messenger.MgAutoAnswer.COOLDOWNS[w]);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_AA_RESET:
                org.telegram.messenger.MgAutoAnswer.clearAnswered();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Tozalandi: endi hamma chatga yana javob beriladi").show();
                break;
            case ID_AT_PREVIEW:
                toggle(view, "at_preview", false);
                break;
            case ID_AJ_ALL:
                toggle(view, "aj_all", false);
                break;
            case ID_STRANGER_ON: {
                boolean v = !org.telegram.messenger.MgStrangers.isEnabled(currentAccount);
                org.telegram.messenger.MgStrangers.setEnabled(currentAccount, v);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(v);
                }
                if (v) {
                    int n = org.telegram.messenger.MgStrangers.countInbox(currentAccount);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, n > 0 ? n + " ta notanish chat \"Notanishlar\" jildiga o'tkazildi" : "Himoya yoqildi").show();
                }
                break;
            }
            case ID_STRANGER_NOTIFY:
                toggle(view, "stranger_notify", false);
                break;
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

    private void editAutoAnswerText(boolean enableAfter) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setMaxLines(8);
        editText.setHint("Masalan: Salom! Hozir band edim, tez orada javob beraman.");
        editText.setText(org.telegram.messenger.MgAutoAnswer.getText());
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle("Avto-javob matni");
        builder.setView(frameLayout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String t = editText.getText() == null ? "" : editText.getText().toString().trim();
            org.telegram.messenger.MgAutoAnswer.setText(t);
            if (t.isEmpty()) {
                org.telegram.messenger.MgAutoAnswer.setEnabled(false);
            } else if (enableAfter) {
                org.telegram.messenger.MgAutoAnswer.setEnabled(true);
            }
            listView.adapter.update(true);
        });
        builder.setNegativeButton("Bekor qilish", null);
        showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 250);
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
        builder.setTitle("Sharpa rejimi");
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
}
