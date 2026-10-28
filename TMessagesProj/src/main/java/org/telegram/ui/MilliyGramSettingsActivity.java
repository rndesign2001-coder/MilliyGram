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
    private static final int ID_TOUR = 502;
    private static final int ID_SIMPLE = 503;
    private static final int ID_INTRO = 504;
    private static final int ID_UPDATE = 505;
    private static final int ID_NEWS = 506;
    private static final int ID_CHAT = 507;
    public static final String NEWS_CHANNEL = "MilliyGramm";
    public static final String CHAT_GROUP = "MilliyGramChat";
    private static final int ID_RESULT_BASE = 10000;

    private String mgQuery;
    private ArrayList<Object[]> mgIndex; // {page, CharSequence text, int icon, String pageTitle}
    private final ArrayList<Integer> mgResultPages = new ArrayList<>();

    private static final int[] PAGES = {
            MgSettingsPage.PAGE_GENERAL,
            MgSettingsPage.PAGE_CHATLIST,
            MgSettingsPage.PAGE_STORIES,
            MgSettingsPage.PAGE_MESSAGES,
            MgSettingsPage.PAGE_AUTOMATION,
            MgSettingsPage.PAGE_THEMES,
            MgSettingsPage.PAGE_PROFILE,
            MgSettingsPage.PAGE_NOTIFY,
            MgSettingsPage.PAGE_PRIVACY,
            MgSettingsPage.PAGE_DATA,
            MgSettingsPage.PAGE_PRAYER,
    };

    private static final int[] ICONS = {
            R.drawable.msg_settings,
            R.drawable.msg_groups,
            R.drawable.msg_menu_stories,
            R.drawable.msg_discussion,
            R.drawable.msg_bots,
            R.drawable.msg_theme,
            R.drawable.msg_openprofile,
            R.drawable.msg_notifications,
            R.drawable.msg_secret,
            R.drawable.msg_download,
            R.drawable.msg_calendar2,
    };

    @Override
    protected CharSequence getTitle() {
        return org.telegram.messenger.MgLang.t("MilliyGram sozlamalari");
    }

    // Kartalar (tanishtiruv shu id'lar bo'yicha topadi)
    private static final int CARD_HERO = 600;
    private static final int CARD_TILES = 601;
    private static final int CARD_CHATS = 602;
    private static final int CARD_LOOK = 603;
    private static final int CARD_SAFE = 604;
    private static final int CARD_LIFE = 605;
    private static final int CARD_SYSTEM = 606;
    private static final int CARD_HELP = 607;
    private static final int CARD_SEARCH = 608;

    private org.telegram.ui.ActionBar.ActionBarMenuItem mgSearchItem;
    private final ArrayList<CharSequence> mgResultTexts = new ArrayList<>();

    private static String t(String s) {
        return org.telegram.messenger.MgLang.t(s);
    }

    private Runnable page(int page) {
        return () -> presentFragment(new MgSettingsPage(page));
    }

    /** Kanal yoki guruhni Telegram ichida to'g'ridan-to'g'ri ochadi (brauzersiz) */
    private void openLink(String user) {
        if (getParentActivity() == null) {
            return;
        }
        try {
            getMessagesController().openByUserName(user, this, 1);
        } catch (Throwable e) {
            org.telegram.messenger.browser.Browser.openUrl(getParentActivity(), "https://t.me/" + user);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (mgQuery != null && !mgQuery.trim().isEmpty()) {
            fillSearch(items);
            return;
        }
        android.content.Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        org.telegram.ui.ActionBar.Theme.ResourcesProvider rp = getResourceProvider();
        String version = "v" + org.telegram.messenger.BuildVars.BUILD_VERSION_STRING
                + (org.telegram.messenger.MgUpdater.currentBuild() > 0 ? "  ·  b" + org.telegram.messenger.MgUpdater.currentBuild() : "");
        items.add(UItem.asCustomShadow(CARD_HERO, MgSettingsUi.hero(ctx, version,
                () -> openLink(NEWS_CHANNEL), () -> openLink(CHAT_GROUP), this::checkUpdate)));
        items.add(UItem.asCustomShadow(CARD_SEARCH, MgSettingsUi.searchBar(ctx, rp, t("Sozlamalardan qidirish"), () -> {
            if (mgSearchItem != null) {
                mgSearchItem.openSearch(true);
            }
        })));

        // Tezkor plitkalar
        ArrayList<MgSettingsUi.Tile> tiles = new ArrayList<>();
        boolean simple = org.telegram.messenger.MgSimple.isEnabled();
        tiles.add(new MgSettingsUi.Tile(R.drawable.msg_zoomin, 0xFFF59E0B, t("Oddiy rejim"), simple ? t("yoqilgan") : t("o'chiq"), simple,
                () -> presentFragment(new MgSimpleModeActivity())));
        boolean ghost = MgGhostMode.isEnabled(currentAccount);
        tiles.add(new MgSettingsUi.Tile(R.drawable.ghost, 0xFF8B5CF6, t("Sharpa"), ghost ? t("yoqilgan") : t("o'chiq"), ghost, this::toggleGhost));
        tiles.add(new MgSettingsUi.Tile(R.drawable.msg_location, 0xFF10B981, t("Qibla"), t("kompas"), false, () -> MgQibla.open(this)));
        tiles.add(new MgSettingsUi.Tile(R.drawable.msg_theme, 0xFFEC4899, t("Mavzu"), MgThemesActivity.currentName(), false,
                () -> presentFragment(new MgThemesActivity())));
        items.add(UItem.asCustomShadow(CARD_TILES, MgSettingsUi.tiles(ctx, rp, tiles)));

        ArrayList<MgSettingsUi.Row> chats = new ArrayList<>();
        chats.add(new MgSettingsUi.Row(R.drawable.msg_folders, 0xFF3B82F6, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_CHATLIST), t("Jildlar, toifalar, arxiv va tablar"), null, page(MgSettingsPage.PAGE_CHATLIST)));
        chats.add(new MgSettingsUi.Row(R.drawable.msg_discussion, 0xFF06B6D4, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_MESSAGES), t("Tarjima, ikki marta bosish, shablonlar, uzatish"), null, page(MgSettingsPage.PAGE_MESSAGES)));
        chats.add(new MgSettingsUi.Row(R.drawable.msg_bots, 0xFF6366F1, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_AUTOMATION), t("Avto-javob, avto-tarjima, kanallarni kuzatish"), null, page(MgSettingsPage.PAGE_AUTOMATION)));
        chats.add(new MgSettingsUi.Row(R.drawable.msg_menu_stories, 0xFFF97316, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_STORIES), t("Hikoyalar panelini yashirish"), null, page(MgSettingsPage.PAGE_STORIES)));
        items.add(UItem.asCustomShadow(CARD_CHATS, MgSettingsUi.section(ctx, rp, t("Chatlar va xabarlar"), chats)));

        ArrayList<MgSettingsUi.Row> safe = new ArrayList<>();
        safe.add(new MgSettingsUi.Row(R.drawable.msg_secret, 0xFFEF4444, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_PRIVACY), t("Chat qulfi, sharpa rejimi, yashirin bo'lim, firibgarlikdan himoya"), null, page(MgSettingsPage.PAGE_PRIVACY)));
        safe.add(new MgSettingsUi.Row(R.drawable.msg_notifications, 0xFFF43F5E, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_NOTIFY), t("Fokus rejimi, javobsiz xabar eslatmasi"), null, page(MgSettingsPage.PAGE_NOTIFY)));
        items.add(UItem.asCustomShadow(CARD_SAFE, MgSettingsUi.section(ctx, rp, t("Xavfsizlik"), safe)));

        ArrayList<MgSettingsUi.Row> life = new ArrayList<>();
        life.add(new MgSettingsUi.Row(R.drawable.msg_calendar2, 0xFF10B981, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_PRAYER), t("Barcha tumanlar uchun vaqtlar, eslatma, qibla kompasi"), null, page(MgSettingsPage.PAGE_PRAYER)));
        items.add(UItem.asCustomShadow(CARD_LIFE, MgSettingsUi.section(ctx, rp, t("Kundalik hayot"), life)));

        ArrayList<MgSettingsUi.Row> look = new ArrayList<>();
        look.add(new MgSettingsUi.Row(R.drawable.msg_theme, 0xFFEC4899, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_THEMES), t("Milliy naqshli mavzular va chat foni"), null, page(MgSettingsPage.PAGE_THEMES)));
        look.add(new MgSettingsUi.Row(R.drawable.msg_palette, 0xFFA855F7, t("Dizayn"), t("Ranglar, burchaklar va shriftlar"), null, () -> presentFragment(new MgDesignActivity())));
        look.add(new MgSettingsUi.Row(R.drawable.msg_openprofile, 0xFF14B8A6, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_PROFILE), t("ID, yolg'on ism, profil kartasi (QR)"), null, page(MgSettingsPage.PAGE_PROFILE)));
        items.add(UItem.asCustomShadow(CARD_LOOK, MgSettingsUi.section(ctx, rp, t("Ko'rinish"), look)));

        ArrayList<MgSettingsUi.Row> sys = new ArrayList<>();
        sys.add(new MgSettingsUi.Row(R.drawable.msg_settings, 0xFF64748B, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_GENERAL), t("Silkitib yashirish, bayram tabriklari"), null, page(MgSettingsPage.PAGE_GENERAL)));
        sys.add(new MgSettingsUi.Row(R.drawable.msg_download, 0xFF0EA5E9, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_DATA), t("Trafik tejash, proksi, xotira"), null, page(MgSettingsPage.PAGE_DATA)));
        sys.add(new MgSettingsUi.Row(R.drawable.msg_copy, 0xFF22C55E, MgSettingsPage.pageTitle(MgSettingsPage.PAGE_BACKUP), t("Boshqa telefonga ko'chirish uchun nusxa"), null, page(MgSettingsPage.PAGE_BACKUP)));
        items.add(UItem.asCustomShadow(CARD_SYSTEM, MgSettingsUi.section(ctx, rp, t("Tizim"), sys)));

        ArrayList<MgSettingsUi.Row> help = new ArrayList<>();
        help.add(new MgSettingsUi.Row(R.drawable.msg_emoji_activities, 0xFFF59E0B, t("MilliyGram imkoniyatlari"), t("Qisqa tanishtiruv"), null, () -> MgOnboarding.show(getParentActivity())));
        help.add(new MgSettingsUi.Row(R.drawable.msg_help, 0xFF3B82F6, t("Sozlamalar bo'yicha sayohat"), t("Har bir bo'lim nima qilishini ko'rsatadi"), null, this::startTour));
        help.add(new MgSettingsUi.Row(R.drawable.msg_info, 0xFF64748B, t("MilliyGram haqida"), t("Versiya, kanallar va yordam xizmati"), null, () -> presentFragment(new MgAboutActivity())));
        items.add(UItem.asCustomShadow(CARD_HELP, MgSettingsUi.section(ctx, rp, t("Yordam"), help)));
    }

    private void toggleGhost() {
        final boolean enable = !MgGhostMode.isEnabled(currentAccount);
        MgGhostMode.setEnabled(currentAccount, enable, err -> org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
            if (err != null) {
                org.telegram.ui.Components.BulletinFactory.of(this).createErrorBulletin(err).show();
            } else {
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check,
                        enable ? t("Sharpa rejimi yoqildi: o'qiganingiz, yozayotganingiz va onlayn ekaningiz ko'rinmaydi") : t("Sharpa rejimi o'chirildi")).show();
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_NEWS || item.id == ID_CHAT) {
            openLink(item.id == ID_NEWS ? NEWS_CHANNEL : CHAT_GROUP);
        } else if (item.id == ID_SIMPLE) {
            presentFragment(new MgSimpleModeActivity());
        } else if (item.id == ID_INTRO) {
            MgOnboarding.show(getParentActivity());
        } else if (item.id == ID_UPDATE) {
            checkUpdate();
        } else if (item.id == ID_ABOUT) {
            presentFragment(new MgAboutActivity());
        } else if (item.id == ID_DESIGN) {
            presentFragment(new MgDesignActivity());
        } else if (item.id >= ID_RESULT_BASE) {
            int i = item.id - ID_RESULT_BASE;
            if (i >= 0 && i < mgResultPages.size()) {
                int page = mgResultPages.get(i);
                if (page == ID_DESIGN) {
                    presentFragment(new MgDesignActivity());
                } else if (page == ID_TOUR) {
                    startTour();
                } else if (page > 0) {
                    presentFragment(new MgSettingsPage(page).highlight(mgResultTexts.get(i)));
                }
            }
        } else if (item.id == ID_TOUR) {
            startTour();
        } else if (item.id >= MgSettingsPage.PAGE_GENERAL && item.id <= MgSettingsPage.PAGE_PRAYER) {
            presentFragment(new MgSettingsPage(item.id));
        }
    }

    @Override
    public android.view.View createView(android.content.Context context) {
        android.view.View v = super.createView(context);
        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        org.telegram.ui.ActionBar.ActionBarMenuItem searchItem = mgSearchItem = menu.addItem(0, R.drawable.outline_header_search).setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchCollapse() {
                        mgQuery = null;
                        if (listView != null) {
                            listView.adapter.update(true);
                        }
                    }

                    @Override
                    public void onTextChanged(android.widget.EditText editText) {
                        mgQuery = editText.getText() == null ? null : editText.getText().toString();
                        if (listView != null) {
                            listView.adapter.update(true);
                        }
                    }
                });
        searchItem.setSearchFieldHint(org.telegram.messenger.MgLang.t("Sozlamalardan qidirish"));
        searchItem.setContentDescription(org.telegram.messenger.MgLang.t("Sozlamalardan qidirish"));
        if (!org.telegram.messenger.MgConfig.getBool("tour_done", false)) {
            org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
                if (getParentActivity() != null && fragmentView != null && fragmentView.isAttachedToWindow()) {
                    startTour();
                }
            }, 700);
        }
        return v;
    }

    private void checkUpdate() {
        if (getParentActivity() == null) {
            return;
        }
        if (!org.telegram.messenger.MgUpdater.isFromPlayStore()) {
            // APK orqali o'rnatilgan — Play sahifasini ochamiz
            org.telegram.messenger.MgUpdater.openPlayPage(getParentActivity());
            return;
        }
        org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, org.telegram.messenger.MgLang.t("Tekshirilmoqda…")).show();
        org.telegram.messenger.MgUpdater.check(getParentActivity(), true, started -> {
            if (started == null) {
                org.telegram.ui.Components.BulletinFactory.of(this).createErrorBulletin(org.telegram.messenger.MgLang.t("Tekshirib bo'lmadi (internetni tekshiring)")).show();
            } else if (!started) {
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, org.telegram.messenger.MgLang.t("Sizda eng so'nggi versiya o'rnatilgan")).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    /** Yangi foydalanuvchi uchun bo'limlarni bittalab, strelka bilan ko'rsatib chiqish */
    private void startTour() {
        if (listView == null || !(fragmentView instanceof android.widget.FrameLayout) || getParentActivity() == null) {
            return;
        }
        org.telegram.messenger.MgConfig.setBool("tour_done", true);
        java.util.ArrayList<org.fenixuz.ui.onboarding.FenixTour.Step> steps = new java.util.ArrayList<>();
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(CARD_HERO, t("MilliyGram"),
                t("Yangiliklar kanali, loyiha chati va yangilanishni tekshirish shu yerda.")));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(CARD_TILES, t("Tezkor tugmalar"),
                t("Oddiy rejim, sharpa rejimi, qibla kompasi va mavzuni bir bosishda o'zgartiring.")));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(CARD_CHATS, t("Chatlar va xabarlar"),
                t("Jildlar, tarjima, ikki marta bosish amali, shablonlar va avto-javob.")));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(CARD_SAFE, t("Xavfsizlik"),
                t("Chat qulfi, yashirin bo'lim, notanishlardan himoya, APK blok va firibgarlikdan ogohlantirish.")));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(CARD_LIFE, t("Namoz vaqti va ob-havo"),
                t("Tumaningizni tanlang — chatlar ro'yxati tepasida keyingi namozgacha qolgan vaqt yoki ob-havo chiqadi.")));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(CARD_HELP, t("Yordam"),
                t("Bu tanishtiruvni istalgan vaqtda shu yerdan qayta ko'rishingiz mumkin.")));
        try {
            new org.fenixuz.ui.onboarding.FenixTour(getParentActivity(), (android.widget.FrameLayout) fragmentView, listView, steps).start();
        } catch (Throwable e) {
            org.telegram.messenger.FileLog.e(e);
        }
    }

    private static String norm(CharSequence cs) {
        String s = cs == null ? "" : cs.toString().toLowerCase();
        return s.replace('ʻ', '\'').replace('’', '\'').replace('‘', '\'').replace('`', '\'').replace('ʼ', '\'');
    }

    /** Barcha bo'limlardagi sozlamalar nomlari bo'yicha indeks (bir marta quriladi) */
    private void buildIndex() {
        mgIndex = new ArrayList<>();
        int[] pages = new int[PAGES.length + 1];
        System.arraycopy(PAGES, 0, pages, 0, PAGES.length);
        pages[PAGES.length] = MgSettingsPage.PAGE_BACKUP;
        for (int page : pages) {
            String title = MgSettingsPage.pageTitle(page);
            int pageIcon = R.drawable.msg_settings;
            for (int k = 0; k < PAGES.length; k++) {
                if (PAGES[k] == page) {
                    pageIcon = ICONS[k];
                }
            }
            mgIndex.add(new Object[]{page, title, pageIcon, org.telegram.messenger.MgLang.t("Bo'lim")});
            try {
                MgSettingsPage pg = new MgSettingsPage(page);
                ArrayList<UItem> tmp = new ArrayList<>();
                pg.fillItems(tmp, null);
                for (UItem u : tmp) {
                    if (u == null || u.text == null || u.viewType == UniversalAdapter.VIEW_TYPE_SHADOW || u.viewType == UniversalAdapter.VIEW_TYPE_HEADER) {
                        continue;
                    }
                    mgIndex.add(new Object[]{page, u.text, u.iconResId != 0 ? u.iconResId : pageIcon, title});
                }
            } catch (Throwable e) {
                org.telegram.messenger.FileLog.e(e);
            }
        }
        mgIndex.add(new Object[]{ID_DESIGN, org.telegram.messenger.MgLang.t("Dizayn (ranglarni sozlash)"), R.drawable.msg_palette, org.telegram.messenger.MgLang.t("Bo'lim")});
        mgIndex.add(new Object[]{ID_TOUR, org.telegram.messenger.MgLang.t("Qisqacha tanishtiruv"), R.drawable.msg_help, org.telegram.messenger.MgLang.t("Bo'lim")});
    }

    private void fillSearch(ArrayList<UItem> items) {
        if (mgIndex == null) {
            buildIndex();
        }
        mgResultPages.clear();
        mgResultTexts.clear();
        String q = norm(mgQuery.trim());
        String[] words = q.split("\\s+");
        for (Object[] e : mgIndex) {
            String hay = norm((CharSequence) e[1]) + " " + norm((String) e[3]);
            boolean ok = true;
            for (String w : words) {
                if (!w.isEmpty() && !hay.contains(w)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                mgResultPages.add((Integer) e[0]);
                mgResultTexts.add((CharSequence) e[1]);
                items.add(UItem.asButton(ID_RESULT_BASE + mgResultPages.size() - 1, (Integer) e[2], (CharSequence) e[1], (String) e[3]));
            }
        }
        if (mgResultPages.isEmpty()) {
            items.add(UItem.asShadow("\"" + mgQuery.trim() + org.telegram.messenger.MgLang.t("\" bo'yicha hech narsa topilmadi")));
        } else {
            items.add(UItem.asShadow(org.telegram.messenger.MgLang.t("Topildi: ") + mgResultPages.size() + org.telegram.messenger.MgLang.t(". Bosing — tegishli bo'lim ochiladi.")));
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
        builder.setMessage(org.telegram.messenger.MgLang.t("MilliyGram — milliy dizayndagi norasmiy Telegram klienti.\n\n") +
                org.telegram.messenger.MgLang.t("Ilova Telegram FZ-LLC tomonidan ishlab chiqilmagan. U Telegram'ning ochiq manba kodi (GPL v2) asosida qurilgan va Telegram API'dan foydalanadi.\n\n") +
                org.telegram.messenger.MgLang.t("Xabarlaringiz to'g'ridan-to'g'ri Telegram serverlari orqali yuboriladi. MilliyGram hech qanday ma'lumot yig'maydi.")
                + "\n\n" + org.telegram.messenger.MgLang.t("Versiya: ") + org.telegram.messenger.BuildVars.BUILD_VERSION_STRING
                + (org.telegram.messenger.MgUpdater.currentBuild() > 0 ? " (b" + org.telegram.messenger.MgUpdater.currentBuild() + ")" : ""));
        builder.setPositiveButton("OK", null);
        showDialog(builder.create());
    }
}
