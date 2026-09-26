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
    public static final int PAGE_PRAYER = 12;

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
    private static final int ID_LOCK_ANIM = 112;
    private static final int ID_TEXT_STYLE = 120;
    private static final int ID_CONFIRM_STICKER = 121;
    private static final int ID_CONFIRM_VOICE = 122;
    private static final int ID_CONFIRM_GIF = 123;
    private static final int ID_ROUND_FRONT = 124;
    private static final int ID_APK_BLOCK = 125;
    private static final int ID_REMIND_ON = 126;
    private static final int ID_REMIND_DELAY = 127;
    private static final int ID_REMIND_SOUND = 128;
    private static final int ID_CHAT_FINDER = 129;
    private static final int ID_TEMPLATES = 130;
    private static final int ID_SCAM = 131;
    private static final int ID_CLEANUP = 132;
    private static final int ID_CHIP_MODE = 140;
    private static final int ID_CHIP_STYLE = 141;
    private static final int ID_PR_REGION = 142;
    private static final int ID_PR_PLACE = 143;
    private static final int ID_PR_TODAY = 144;
    private static final int ID_PR_NOTIFY = 145;
    private static final int ID_PR_BEFORE = 146;
    private static final int ID_PR_WHICH = 147;
    private static final int ID_PR_ADJUST = 148;
    private static final int ID_WX_REFRESH = 149;
    private static final int ID_SAVE_BTN = 150;
    private static final int ID_LIVE_ON = 151;
    private static final int ID_LIVE_SCENE = 152;
    private static final int ID_LIVE_MODE = 153;
    private static final int ID_LIVE_DIM = 154;
    private static final int ID_LIVE_PREVIEW = 155;
    private static final int ID_MY_CARD = 156;
    private static final int ID_GR_INTERVAL = 157;
    private static final int ID_GR_AUTO = 158;
    private static final int ID_GR_LIST = 159;
    private static final int ID_ACC_NOTIFY_BASE = 2000;
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
            case PAGE_PRAYER: return "Namoz vaqti va ob-havo";
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
                items.add(UItem.asHeader("Qidiruv"));
                items.add(UItem.asButton(ID_CHAT_FINDER, R.drawable.msg_search, "Foydalanuvchi nomini tekshirish"));
                items.add(UItem.asShadow("@username yozing — band yoki bo'shligini darhol ko'rsatadi va mavjud bo'lsa, chatni ochadi."));
                break;
            case PAGE_CHATLIST: {
                items.add(UItem.asHeader("Jildlar"));
                items.add(UItem.asCheck(ID_ICON_TABS, "Jildlarni ikonkada ko'rsatish").setChecked(MgConfig.isFolderIconTabs()));
                items.add(UItem.asButton(ID_FOLDERS, R.drawable.msg_folders, "Lokal jildlar va ikonkalar"));
                items.add(UItem.asButton(ID_CATEGORIES, R.drawable.msg_addfolder, "Toifalar", String.valueOf(MgLocalFolders.getCategories(currentAccount).size())));
                items.add(UItem.asButton(ID_CLOUD_FOLDERS, R.drawable.msg_customize, "Bulut jildlarini tahrirlash"));
                items.add(UItem.asShadow("Jildni uzoq bossangiz: tahrirlash, ikonka tanlash, tabni yashirish. Chatlarni belgilab ⋮ → \"Toifaga qo'shish\" orqali o'z toifalaringizni yarating."));
                items.add(UItem.asButton(ID_CLEANUP, R.drawable.msg_clear, "Kanal va guruhlarni tozalash"));
                items.add(UItem.asShadow("O'qilmay yotgan va faolsiz kanal/guruhlarni topib, birdaniga chiqib ketish."));
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
                items.add(UItem.asHeader("Xabarlar yonida"));
                items.add(UItem.asCheck(ID_SAVE_BTN, "Bulutcha — tez saqlash tugmasi").setChecked(MgConfig.getBool("mg_save_btn", true)));
                items.add(UItem.asShadow("Har bir xabar yonidagi ☁ tugmasi xabarni bir bosishda \"Saqlangan xabarlar\"ga saqlaydi. Uzatish taqiqlangan chatlarda chiqmaydi."));
                items.add(UItem.asHeader("Tezkor shablonlar"));
                items.add(UItem.asButton(ID_TEMPLATES, R.drawable.msg_saved, "Shablonlarni boshqarish", String.valueOf(MgMessageTools.templates().size())));
                items.add(UItem.asShadow("Ko'p yoziladigan gaplarni saqlang: chat → ⋮ → \"Tezkor shablonlar\" orqali bir bosishda qo'yiladi. Xabarni uzoq bosib \"Lotinga/Kirillga o'girish\" va \"Keyin eslatish\" ham mavjud."));
                items.add(UItem.asHeader("Yozish"));
                items.add(UItem.asButton(ID_TEXT_STYLE, R.drawable.msg_text_outlined, "Standart matn uslubi", org.telegram.messenger.MgAutoText.STYLE_NAMES[org.telegram.messenger.MgAutoText.getDefaultStyle()]));
                items.add(UItem.asShadow("Tanlangan uslub har bir oddiy matnli xabaringizga avtomatik qo'llanadi (buyruqlar, kod va faqat emojidan iborat xabarlar bundan mustasno)."));
                items.add(UItem.asHeader("Yuborishdan oldin so'rash"));
                items.add(UItem.asCheck(ID_CONFIRM_STICKER, "Stiker yuborishda").setChecked(org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.getConfirmSticker()));
                items.add(UItem.asCheck(ID_CONFIRM_GIF, "GIF yuborishda").setChecked(org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.getConfirmGif()));
                items.add(UItem.asCheck(ID_CONFIRM_VOICE, "Ovozli xabarda (avval tinglash)").setChecked(org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.getConfirmVoice()));
                items.add(UItem.asShadow("Tasodifan yuborib yuborishning oldini oladi. Ovozli xabar yozib bo'lingach darhol ketmaydi — avval tinglab, keyin yuborasiz."));
                items.add(UItem.asHeader("Doira video"));
                items.add(UItem.asCheck(ID_ROUND_FRONT, "Old kamera bilan boshlash").setChecked(org.fenixuz.utils.CameraSituation.INSTANCE.isFront()));
                items.add(UItem.asShadow("O'chirilsa, doira video orqa kamera bilan boshlanadi. Galereyadagi videoni doira qilib yuborish: videoni tanlang → pastdagi ⏺ (kamera) tugmasini bosing. Bir martalik ovozli xabar: chat → ⋮ → \"Bir martalik ovoz\". Gapirib yozish (tarjima bilan): chat → ⋮ → \"Ovoz bilan yozish\"."));
                break;
            case PAGE_THEMES:
                items.add(UItem.asButton(ID_THEMES, R.drawable.msg_theme, "MilliyGram mavzulari", MgThemesActivity.currentName()));
                items.add(UItem.asButton(ID_CHAT_BG, R.drawable.msg_background, "Chat foni"));
                items.add(UItem.asButton(ID_DESIGN, R.drawable.msg_palette, "Dizayn (ranglarni sozlash)"));
                items.add(UItem.asButton(ID_CHAT_SETTINGS, R.drawable.msg_msgbubble3, "Chat sozlamalari (shrift, burchaklar)"));
                items.add(UItem.asShadow("Har bir milliy mavzuning o'z naqshli chat foni bor. \"Dizayn\" bo'limida har bir ekran rangini alohida o'zgartirish mumkin."));
                items.add(UItem.asHeader("Jonli fon"));
                items.add(UItem.asCheck(ID_LIVE_ON, "Jonli chat foni").setChecked(MgLiveBackground.isEnabled()));
                items.add(UItem.asButton(ID_LIVE_SCENE, R.drawable.msg_background, "Manzara", MgLiveBackground.SCENES[MgLiveBackground.getScene()]));
                items.add(UItem.asButton(ID_LIVE_MODE, R.drawable.msg_recent, "Vaqt", MgLiveBackground.MODES[MgLiveBackground.getMode()]));
                items.add(UItem.asButton(ID_LIVE_DIM, R.drawable.msg_brightness_low, "Qoraytirish", MgLiveBackground.getDim() + "%"));
                items.add(UItem.asButton(ID_LIVE_PREVIEW, R.drawable.msg_views, "Ko'rib chiqish"));
                items.add(UItem.asShadow("Chat foni kun vaqtiga qarab o'zgaradi: tong, kun, shom, tun. Vaqtlar tanlangan hududingizning quyosh chiqishi va botishi bo'yicha hisoblanadi (Namoz vaqti va ob-havo bo'limi). Hozir: " + MgLiveBackground.SLOT_NAMES[MgLiveBackground.currentSlot()] + "."));
                break;
            case PAGE_PROFILE: {
                String fake = MgConfig.getFakeName();
                items.add(UItem.asCheck(ID_SHOW_ID, "Profilda ID ko'rsatish").setChecked(MgConfig.getBool("show_profile_id", true)));
                items.add(UItem.asButton(ID_FAKE_NAME, R.drawable.msg_openprofile, "Yolg'on ism", fake == null || fake.isEmpty() ? "o'chirilgan" : fake));
                items.add(UItem.asShadow("ID qatorini bossangiz, nusxalanadi. Yolg'on ism faqat sizning ekraningizda ko'rinadi (skrinshotlar uchun)."));
                items.add(UItem.asButton(ID_MY_CARD, R.drawable.msg_qrcode, "Profil kartam (QR bilan)"));
                items.add(UItem.asShadow("Profilingizni chiroyli vizitka rasmiga aylantiring va ulashing. Kanal yoki guruh kartasi: chat → ⋮ → \"Profil kartasi (QR)\"."));
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
                items.add(UItem.asHeader("Javobsiz xabar eslatmasi"));
                items.add(UItem.asCheck(ID_REMIND_ON, "Eslatib turish").setChecked(org.fenixuz.utils.MessageReminder.INSTANCE.isEnabled()));
                items.add(UItem.asButton(ID_REMIND_DELAY, R.drawable.msg_recent, "Qancha vaqtdan keyin", org.fenixuz.utils.MessageReminder.INSTANCE.getDelayMin() + " daqiqa"));
                items.add(UItem.asButton(ID_REMIND_SOUND, R.drawable.msg_filled_data_music, "Ovoz", org.fenixuz.utils.MessageReminder.INSTANCE.getSound() == 1 ? "Budilnik" : "Bildirishnoma"));
                items.add(UItem.asShadow("Shaxsiy chatga kelgan xabarni belgilangan vaqt ichida o'qimasangiz, telefon bir marta jiringlab eslatadi. Xabarni o'qishingiz bilan eslatma bekor bo'ladi. Ovozi o'chirilgan, yashirin va notanish chatlar hisobga olinmaydi."));
                if (org.telegram.messenger.UserConfig.getActivatedAccountsCount() > 1) {
                    items.add(UItem.asHeader("Akkauntlar bo'yicha"));
                    for (int a = 0; a < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        org.telegram.messenger.UserConfig uc = org.telegram.messenger.UserConfig.getInstance(a);
                        if (!uc.isClientActivated()) {
                            continue;
                        }
                        String alias = MgConfig.getAccountAlias(a);
                        String name = alias != null && !alias.isEmpty() ? alias : org.telegram.messenger.UserObject.getUserName(uc.getCurrentUser());
                        items.add(UItem.asCheck(ID_ACC_NOTIFY_BASE + a, name).setChecked(MgConfig.isAccountNotifyEnabled(a)));
                    }
                    items.add(UItem.asShadow("O'chirilgan akkauntdan bildirishnoma kelmaydi. Akkauntlar ro'yxatida ⚙ tugmasi orqali ham o'zgartirish mumkin."));
                }
                break;
            case PAGE_PRIVACY: {
                items.add(UItem.asCheck(ID_GHOST, "Sharpa rejimi").setChecked(MgGhostMode.isEnabled(currentAccount)));
                items.add(UItem.asButton(ID_HIDDEN, R.drawable.msg_stories_myhide, "Yashirin bo'lim"));
                int locked = MgConfig.getLockedCount();
                items.add(UItem.asButton(ID_LOCK, R.drawable.msg_secret, "Chat qulfi", locked > 0 ? String.valueOf(locked) : ""));
                items.add(UItem.asShadow(null));
                items.add(UItem.asCheck(ID_LOCK_ANIM, "Qulf ekranida animatsiyali fon").setChecked(MgConfig.getBool("lock_animated_bg", true)));
                items.add(UItem.asShadow("Chatni qulflashda umumiy parol yoki shu chatga alohida PIN / grafik kalit tanlash mumkin: chat → ⋮ → \"Chatni qulflash\"."));
                items.add(UItem.asHeader("Xavfsizlik"));
                items.add(UItem.asCheck(ID_APK_BLOCK, "APK fayllarni bloklash").setChecked(org.fenixuz.utils.ApkShield.isEnabled()));
                items.add(UItem.asCheck(ID_SCAM, "Firibgarlikdan ogohlantirish").setChecked(MgMessageTools.isScamGuardEnabled()));
                items.add(UItem.asShadow("APK blok: chatlardagi .apk (Android ilova) fayllari ko'rsatilmaydi va ochilmaydi. Ogohlantirish: kontaktingizda yo'q odam karta raqami, SMS kod, pul o'tkazish yoki shubhali havola bilan yozsa, chatni ochganingizda ogohlantiriladi."));
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
                items.add(UItem.asHeader("Obunachilar kundaligi"));
                items.add(UItem.asButton(ID_GR_LIST, R.drawable.msg_stats, "Kuzatilayotgan kanallar", String.valueOf(org.telegram.messenger.MgGrowth.trackedChats(currentAccount).size())));
                items.add(UItem.asButton(ID_GR_INTERVAL, R.drawable.msg_recent, "Qanchalik tez-tez yozilsin", "har " + org.telegram.messenger.MgGrowth.getIntervalHours() + " soatda"));
                items.add(UItem.asCheck(ID_GR_AUTO, "Admin bo'lgan kanal/guruhlarni avtomatik kuzatish").setChecked(org.telegram.messenger.MgGrowth.isAutoAdmin()));
                items.add(UItem.asShadow("Obunachi soni muntazam yozib boriladi va grafik chiziladi: reklama bergan yoki nakrutka kirgan kunlar ko'rinib turadi. Istalgan kanalni qo'shish: kanal → ⋮ → \"Obunachilar kundaligi\". Har tekshiruv bitta kichik so'rov, ilova tezligiga ta'sir qilmaydi."));
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
            case PAGE_PRAYER: {
                org.telegram.messenger.MgPlaces.Place pl = org.telegram.messenger.MgPrayer.getPlace();
                items.add(UItem.asHeader("Chatlar ro'yxatida ko'rsatish"));
                items.add(UItem.asButton(ID_CHIP_MODE, R.drawable.msg_views, "Nimani ko'rsatish", MgInfoChip.MODE_NAMES[Math.max(0, Math.min(3, MgInfoChip.getMode()))]));
                items.add(UItem.asButton(ID_CHIP_STYLE, R.drawable.msg_palette, "Ko'rinish uslubi", MgInfoChip.STYLE_NAMES[Math.max(0, Math.min(MgInfoChip.STYLE_NAMES.length - 1, MgInfoChip.getStyleIndex()))]));
                items.add(UItem.asShadow("Chatlar ro'yxati tepasida, ⋮ tugmasining chap tomonida kichik belgi chiqadi: keyingi namozgacha qolgan vaqt yoki ob-havo. Uni bossangiz, bugungi barcha vaqtlar va 3 kunlik ob-havo ochiladi."));
                items.add(UItem.asHeader("Joylashuv"));
                items.add(UItem.asButton(ID_PR_REGION, R.drawable.msg_map, "Viloyat", org.telegram.messenger.MgPlaces.findRegion(org.telegram.messenger.MgPrayer.getRegionKey()).name));
                items.add(UItem.asButton(ID_PR_PLACE, R.drawable.msg_location, "Shahar / tuman", pl.name));
                items.add(UItem.asHeader("Namoz vaqtlari"));
                items.add(UItem.asButton(ID_PR_TODAY, R.drawable.msg_calendar2, "Bugungi vaqtlar"));
                items.add(UItem.asCheck(ID_PR_NOTIFY, "Namoz vaqtini eslatish").setChecked(org.telegram.messenger.MgPrayerAlarm.isEnabled()));
                int before = org.telegram.messenger.MgPrayerAlarm.getBefore();
                items.add(UItem.asButton(ID_PR_BEFORE, R.drawable.msg_recent, "Qachon", before == 0 ? "Vaqt kirganda" : before + " daqiqa oldin"));
                items.add(UItem.asButton(ID_PR_WHICH, R.drawable.msg_list, "Qaysi namozlar"));
                items.add(UItem.asButton(ID_PR_ADJUST, R.drawable.msg_customize, "Vaqtlarni tuzatish (daqiqa)"));
                items.add(UItem.asShadow("Vaqtlar internetsiz, tanlangan tuman koordinatasi bo'yicha O'zbekiston musulmonlari idorasi taqvimi uslubida hisoblanadi. Masjidingiz jadvalidan farq qilsa, \"Vaqtlarni tuzatish\"da moslang."));
                items.add(UItem.asHeader("Ob-havo"));
                {
                    org.telegram.messenger.MgWeather.Data wd = org.telegram.messenger.MgWeather.getCached();
                    items.add(UItem.asButton(ID_WX_REFRESH, R.drawable.msg_retry, "Ob-havoni yangilash",
                            wd == null ? "" : org.telegram.messenger.MgWeather.icon(wd.code) + " " + org.telegram.messenger.MgWeather.temp(wd.temp)));
                }
                items.add(UItem.asShadow("Ob-havo Open-Meteo xizmatidan olinadi (internet kerak), har 30 daqiqada yangilanadi."));
                break;
            }
            case PAGE_BACKUP:
                items.add(UItem.asButton(ID_EXPORT, R.drawable.msg_copy, "Sozlamalarni nusxalash"));
                items.add(UItem.asButton(ID_IMPORT, R.drawable.msg_download, "Sozlamalarni tiklash"));
                items.add(UItem.asShadow("Sozlamalar matn ko'rinishida nusxalanadi (sevimlilar va jildlar bilan). Uni Saqlangan xabarlarga yuborib qo'ying va yangi telefonda qayta joylang. Maxfiy kodlar nusxalanmaydi."));
                break;
        }
    }

    private static void setChecked(View view, boolean v) {
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(v);
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
        if (item.id >= ID_ACC_NOTIFY_BASE && mgHandleAccountNotify(item, view)) {
            return;
        }
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
            case ID_LOCK_ANIM:
                toggle(view, "lock_animated_bg", true);
                break;
            case ID_TEXT_STYLE: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Standart matn uslubi");
                b.setItems(org.telegram.messenger.MgAutoText.STYLE_NAMES, (d, w) -> {
                    org.telegram.messenger.MgAutoText.setDefaultStyle(w);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_CONFIRM_STICKER:
                org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.changeConfirmStickerMode();
                setChecked(view, org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.getConfirmSticker());
                break;
            case ID_CONFIRM_GIF:
                org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.changeConfirmGifMode();
                setChecked(view, org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.getConfirmGif());
                break;
            case ID_CONFIRM_VOICE:
                org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.changeConfirmVoiceMode();
                setChecked(view, org.fenixuz.utils.ConfirmDialogsPref.INSTANCE.getConfirmVoice());
                break;
            case ID_ROUND_FRONT: {
                boolean v = !org.fenixuz.utils.CameraSituation.INSTANCE.isFront();
                org.fenixuz.utils.CameraSituation.INSTANCE.setFront(v);
                setChecked(view, v);
                break;
            }
            case ID_APK_BLOCK: {
                boolean v = !org.fenixuz.utils.ApkShield.isEnabled();
                org.fenixuz.utils.ApkShield.setEnabled(v);
                setChecked(view, v);
                break;
            }
            case ID_LIVE_ON: {
                boolean v = !MgLiveBackground.isEnabled();
                MgLiveBackground.setEnabled(v);
                setChecked(view, v);
                break;
            }
            case ID_LIVE_SCENE:
                pick("Manzara", MgLiveBackground.SCENES, w -> MgLiveBackground.set("live_scene", w));
                break;
            case ID_LIVE_MODE:
                pick("Vaqt", MgLiveBackground.MODES, w -> MgLiveBackground.set("live_mode", w));
                break;
            case ID_LIVE_DIM: {
                String[] names = new String[MgLiveBackground.DIMS.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = MgLiveBackground.DIMS[i] == 0 ? "Yo'q" : MgLiveBackground.DIMS[i] + "%";
                }
                pick("Qoraytirish (matn o'qilishi uchun)", names, w -> MgLiveBackground.set("live_dim", MgLiveBackground.DIMS[w]));
                break;
            }
            case ID_LIVE_PREVIEW:
                showLivePreview();
                break;
            case ID_MY_CARD:
                MgProfileCard.show(this, getUserConfig().getClientUserId());
                break;
            case ID_GR_INTERVAL: {
                String[] names = new String[org.telegram.messenger.MgGrowth.INTERVALS.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = "Har " + org.telegram.messenger.MgGrowth.INTERVALS[i] + " soatda";
                }
                pick("Qanchalik tez-tez yozilsin", names, w -> org.telegram.messenger.MgGrowth.setIntervalHours(org.telegram.messenger.MgGrowth.INTERVALS[w]));
                break;
            }
            case ID_GR_AUTO:
                toggle(view, "gr_auto", true);
                break;
            case ID_GR_LIST: {
                java.util.ArrayList<Long> ids = org.telegram.messenger.MgGrowth.trackedChats(currentAccount);
                if (ids.isEmpty()) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Hozircha yo'q. Kanal → ⋮ → \"Obunachilar kundaligi\" orqali qo'shing").show();
                    break;
                }
                String[] names = new String[ids.size()];
                for (int i = 0; i < ids.size(); i++) {
                    org.telegram.tgnet.TLRPC.Chat c = getMessagesController().getChat(ids.get(i));
                    java.util.ArrayList<org.telegram.messenger.MgGrowth.Point> pts = org.telegram.messenger.MgGrowth.points(currentAccount, ids.get(i));
                    names[i] = (c == null ? String.valueOf(ids.get(i)) : c.title) + (pts.isEmpty() ? "" : " · " + pts.get(pts.size() - 1).count);
                }
                pick("Kuzatilayotgan kanallar", names, w -> presentFragment(new MgGrowthActivity(ids.get(w))));
                break;
            }
            case ID_SAVE_BTN:
                toggle(view, "mg_save_btn", true);
                break;
            case ID_TEMPLATES:
                MgMessageTools.showTemplates(this, null);
                break;
            case ID_SCAM:
                toggle(view, "scam_guard", true);
                break;
            case ID_CLEANUP:
                presentFragment(new MgCleanupActivity());
                break;
            case ID_CHIP_MODE: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Chatlar ro'yxatida ko'rsatish");
                b.setItems(MgInfoChip.MODE_NAMES, (d, w) -> {
                    MgConfig.setInt("chip_mode", w);
                    if (w == MgInfoChip.MODE_WEATHER || w == MgInfoChip.MODE_BOTH) {
                        org.telegram.messenger.MgWeather.refresh(false, null);
                    }
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_CHIP_STYLE: {
                CharSequence[] names = new CharSequence[MgInfoChip.STYLE_NAMES.length];
                String[] samples = {"( Asr · 1:24 )", "[ Asr · 1:24 ]", "Asr · 1:24", "( 🕌 1:24 )", "( Asr · 1:24 ) rangli"};
                for (int i = 0; i < names.length; i++) {
                    names[i] = MgInfoChip.STYLE_NAMES[i] + "   " + samples[i];
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Ko'rinish uslubi");
                b.setItems(names, (d, w) -> {
                    MgConfig.setInt("chip_style", w);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_PR_REGION: {
                org.telegram.messenger.MgPlaces.Region[] rs = org.telegram.messenger.MgPlaces.REGIONS;
                CharSequence[] names = new CharSequence[rs.length];
                for (int i = 0; i < rs.length; i++) {
                    names[i] = rs[i].name;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Viloyatni tanlang");
                b.setItems(names, (d, w) -> {
                    org.telegram.messenger.MgPrayer.setPlace(rs[w].key, rs[w].places[0].name);
                    listView.adapter.update(true);
                    AndroidUtilities.runOnUIThread(() -> showPlacePicker(), 250);
                });
                showDialog(b.create());
                break;
            }
            case ID_PR_PLACE:
                showPlacePicker();
                break;
            case ID_PR_TODAY:
                MgInfoChip.showDetails(this);
                break;
            case ID_PR_NOTIFY: {
                boolean v = !org.telegram.messenger.MgPrayerAlarm.isEnabled();
                org.telegram.messenger.MgPrayerAlarm.setEnabled(v);
                setChecked(view, v);
                if (v && android.os.Build.VERSION.SDK_INT >= 33 && getParentActivity() != null
                        && getParentActivity().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
                break;
            }
            case ID_PR_BEFORE: {
                final int[] opts = {0, 5, 10, 15, 20, 30};
                CharSequence[] names = new CharSequence[opts.length];
                for (int i = 0; i < opts.length; i++) {
                    names[i] = opts[i] == 0 ? "Vaqt kirganda" : opts[i] + " daqiqa oldin";
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Qachon eslatilsin?");
                b.setItems(names, (d, w) -> {
                    org.telegram.messenger.MgPrayerAlarm.setBefore(opts[w]);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_PR_WHICH:
                showWhichDialog(new int[]{0, 2, 3, 4, 5}, 0);
                break;
            case ID_PR_ADJUST:
                showAdjustDialog();
                break;
            case ID_WX_REFRESH:
                org.telegram.messenger.MgWeather.refresh(true, d -> {
                    if (listView != null) {
                        listView.adapter.update(true);
                    }
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, d == null ? "Ob-havo olinmadi — internetni tekshiring" : "Yangilandi: " + org.telegram.messenger.MgWeather.icon(d.code) + " " + org.telegram.messenger.MgWeather.temp(d.temp) + ", " + org.telegram.messenger.MgWeather.describe(d.code)).show();
                });
                break;
            case ID_CHAT_FINDER:
                presentFragment(new org.fenixuz.ui.chat_finder.ChatFinder());
                break;
            case ID_REMIND_ON: {
                boolean v = !org.fenixuz.utils.MessageReminder.INSTANCE.isEnabled();
                org.fenixuz.utils.MessageReminder.INSTANCE.setEnabled(v);
                setChecked(view, v);
                break;
            }
            case ID_REMIND_DELAY: {
                final int[] opts = {2, 5, 10, 15, 30, 59};
                CharSequence[] names = new CharSequence[opts.length];
                for (int i = 0; i < opts.length; i++) {
                    names[i] = opts[i] + " daqiqa";
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Eslatma vaqti");
                b.setItems(names, (d, w) -> {
                    org.fenixuz.utils.MessageReminder.INSTANCE.setDelayMin(opts[w]);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_REMIND_SOUND: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle("Eslatma ovozi");
                b.setItems(new CharSequence[]{"Bildirishnoma ovozi (yumshoq)", "Budilnik ovozi (baland)"}, (d, w) -> {
                    org.fenixuz.utils.MessageReminder.INSTANCE.setSound(w);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_STRANGER_NOTIFY:
                toggle(view, "stranger_notify", false);
                break;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private boolean mgHandleAccountNotify(UItem item, View view) {
        int a = item.id - ID_ACC_NOTIFY_BASE;
        if (a < 0 || a >= org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT) {
            return false;
        }
        boolean v = !MgConfig.isAccountNotifyEnabled(a);
        MgConfig.setAccountNotifyEnabled(a, v);
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(v);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    private void pick(String title, String[] names, org.telegram.messenger.Utilities.Callback<Integer> cb) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle(title);
        b.setItems(names, (d, w) -> {
            cb.run(w);
            if (listView != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(b.create());
    }

    private void showLivePreview() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        int scene = MgLiveBackground.currentScene();
        android.widget.LinearLayout row = new android.widget.LinearLayout(context);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        int cur = MgLiveBackground.currentSlot();
        for (int i = 0; i < 4; i++) {
            android.widget.LinearLayout col = new android.widget.LinearLayout(context);
            col.setOrientation(android.widget.LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            android.widget.ImageView iv = new android.widget.ImageView(context);
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inSampleSize = 4;
            try {
                int[] res = scene == 0
                        ? new int[]{R.drawable.mg_live_registon_tong, R.drawable.mg_live_registon_kun, R.drawable.mg_live_registon_shom, R.drawable.mg_live_registon_tun}
                        : new int[]{R.drawable.mg_live_city_tong, R.drawable.mg_live_city_kun, R.drawable.mg_live_city_shom, R.drawable.mg_live_city_tun};
                iv.setImageBitmap(android.graphics.BitmapFactory.decodeResource(context.getResources(), res[i], o));
            } catch (Throwable ignore) {
            }
            android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable();
            frame.setCornerRadius(AndroidUtilities.dp(8));
            frame.setStroke(AndroidUtilities.dp(i == cur ? 3 : 0), Theme.getColor(Theme.key_featuredStickers_addButton));
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                iv.setForeground(frame);
            }
            iv.setClipToOutline(true);
            col.addView(iv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 130));
            android.widget.TextView tv = new android.widget.TextView(context);
            tv.setText(MgLiveBackground.SLOT_NAMES[i] + (i == cur ? " •" : ""));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tv.setTextColor(Theme.getColor(i == cur ? Theme.key_featuredStickers_addButton : Theme.key_dialogTextBlack));
            tv.setGravity(Gravity.CENTER);
            col.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
            row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 3, 0, 3, 0));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(context, getResourceProvider());
        b.setTitle(MgLiveBackground.SCENES[scene] + " — kun davomida");
        b.setView(row);
        b.setPositiveButton("Yopish", null);
        showDialog(b.create());
    }

    private void showPlacePicker() {
        if (getParentActivity() == null) {
            return;
        }
        org.telegram.messenger.MgPlaces.Region r = org.telegram.messenger.MgPlaces.findRegion(org.telegram.messenger.MgPrayer.getRegionKey());
        CharSequence[] names = new CharSequence[r.places.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = r.places[i].name;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle(r.name);
        b.setItems(names, (d, w) -> {
            org.telegram.messenger.MgPrayer.setPlace(r.key, r.places[w].name);
            org.telegram.messenger.MgPrayerAlarm.schedule(getParentActivity());
            org.telegram.messenger.MgWeather.refresh(true, x -> {
                if (listView != null) {
                    listView.adapter.update(true);
                }
            });
            listView.adapter.update(true);
        });
        showDialog(b.create());
    }

    /** Har bir namozni navbat bilan yoqish/o'chirish oynasi */
    private void showWhichDialog(int[] idx, int unused) {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] names = new CharSequence[idx.length];
        for (int i = 0; i < idx.length; i++) {
            names[i] = (org.telegram.messenger.MgPrayerAlarm.isPrayerEnabled(idx[i]) ? "✅  " : "⬜  ") + org.telegram.messenger.MgPrayer.NAMES[idx[i]];
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle("Qaysi namozlar eslatilsin?");
        b.setItems(names, (d, w) -> {
            org.telegram.messenger.MgPrayerAlarm.setPrayerEnabled(idx[w], !org.telegram.messenger.MgPrayerAlarm.isPrayerEnabled(idx[w]));
            AndroidUtilities.runOnUIThread(() -> showWhichDialog(idx, 0), 150);
        });
        b.setPositiveButton("Tayyor", null);
        showDialog(b.create());
    }

    private void showAdjustDialog() {
        if (getParentActivity() == null) {
            return;
        }
        int[] t = org.telegram.messenger.MgPrayer.today();
        CharSequence[] names = new CharSequence[6];
        for (int i = 0; i < 6; i++) {
            int off = org.telegram.messenger.MgPrayer.getUserOffset(i);
            names[i] = org.telegram.messenger.MgPrayer.NAMES[i] + " — " + org.telegram.messenger.MgPrayer.hhmm(t[i]) + (off != 0 ? "  (" + (off > 0 ? "+" : "") + off + " daq)" : "");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle("Qaysi vaqtni tuzatasiz?");
        b.setItems(names, (d, w) -> AndroidUtilities.runOnUIThread(() -> showAdjustInput(w), 150));
        b.setNeutralButton("Hammasini tiklash", (d, w) -> {
            for (int i = 0; i < 6; i++) {
                org.telegram.messenger.MgPrayer.setUserOffset(i, 0);
            }
            org.telegram.messenger.MgPrayerAlarm.schedule(getParentActivity());
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Tuzatishlar olib tashlandi").show();
        });
        b.setPositiveButton("Tayyor", null);
        showDialog(b.create());
    }

    /** Istalgan tuzatish: "+35", "-12" yoki aniq vaqt "05:12" kiritiladi, natija darhol ko'rsatiladi */
    private void showAdjustInput(int prayer) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final int base = org.telegram.messenger.MgPrayer.baseTimes()[prayer];
        android.widget.LinearLayout box = new android.widget.LinearLayout(context);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.TextView info = new android.widget.TextView(context);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        info.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        info.setText("Hisoblangan vaqt: " + org.telegram.messenger.MgPrayer.hhmm(base) + "\nDaqiqa kiriting (masalan +35 yoki -12) yoki masjidingizdagi aniq vaqtni yozing (masalan 05:12).");
        box.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 4, 24, 8));
        EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setBackground(null);
        edit.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        edit.setInputType(InputType.TYPE_CLASS_PHONE);
        edit.setGravity(Gravity.CENTER);
        edit.setHint("+0");
        int cur = org.telegram.messenger.MgPrayer.getUserOffset(prayer);
        if (cur != 0) {
            edit.setText((cur > 0 ? "+" : "") + cur);
        }
        box.addView(edit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));
        android.widget.TextView result = new android.widget.TextView(context);
        result.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        result.setGravity(Gravity.CENTER);
        result.setTypeface(AndroidUtilities.bold());
        result.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        box.addView(result, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 10, 24, 4));
        final int[] parsed = {cur};
        Runnable recalc = () -> {
            Integer off = parseOffset(edit.getText() == null ? "" : edit.getText().toString(), base);
            if (off == null) {
                result.setText("Noto'g'ri qiymat");
                result.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
                parsed[0] = Integer.MIN_VALUE;
            } else {
                parsed[0] = off;
                int res = ((base + off) % 1440 + 1440) % 1440;
                result.setText("Natija: " + org.telegram.messenger.MgPrayer.NAMES[prayer] + " " + org.telegram.messenger.MgPrayer.hhmm(res)
                        + (off != 0 ? "  (" + (off > 0 ? "+" : "") + off + " daq)" : ""));
                result.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            }
        };
        edit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                recalc.run();
            }
        });
        recalc.run();
        AlertDialog.Builder b = new AlertDialog.Builder(context, getResourceProvider());
        b.setTitle(org.telegram.messenger.MgPrayer.NAMES[prayer] + " vaqtini tuzatish");
        b.setView(box);
        b.setPositiveButton("Saqlash", (d, w) -> {
            if (parsed[0] == Integer.MIN_VALUE) {
                BulletinFactory.of(this).createErrorBulletin("Qiymat noto'g'ri — saqlanmadi").show();
                return;
            }
            org.telegram.messenger.MgPrayer.setUserOffset(prayer, parsed[0]);
            org.telegram.messenger.MgPrayerAlarm.schedule(context);
            AndroidUtilities.runOnUIThread(this::showAdjustDialog, 150);
        });
        b.setNegativeButton("Bekor qilish", null);
        showDialog(b.create());
        AndroidUtilities.runOnUIThread(() -> {
            edit.requestFocus();
            AndroidUtilities.showKeyboard(edit);
        }, 250);
    }

    /** "+35" / "-12" / "35" → daqiqa; "05:12" / "5.12" → hisoblangan vaqtdan farq. Chegara: ±12 soat */
    private static Integer parseOffset(String raw, int base) {
        String s = raw.trim().replace(" ", "");
        if (s.isEmpty() || s.equals("+") || s.equals("-")) {
            return 0;
        }
        try {
            if (s.contains(":") || s.contains(".") || s.contains(",")) {
                String[] p = s.split("[:.,]");
                if (p.length != 2) {
                    return null;
                }
                int h = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]);
                if (h < 0 || h > 23 || m < 0 || m > 59) {
                    return null;
                }
                int diff = h * 60 + m - base;
                if (diff > 720) diff -= 1440;
                if (diff < -720) diff += 1440;
                return diff;
            }
            int v = Integer.parseInt(s.startsWith("+") ? s.substring(1) : s);
            return Math.abs(v) <= 720 ? v : null;
        } catch (Throwable e) {
            return null;
        }
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
