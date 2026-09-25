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
        items.add(UItem.asButton(ID_TOUR, R.drawable.msg_help, "Qisqacha tanishtiruv"));
        items.add(UItem.asButton(ID_ABOUT, R.drawable.msg_info, "MilliyGram haqida"));
        items.add(UItem.asShadow("MilliyGram — Telegram'ning ochiq manba kodi asosida qurilgan norasmiy klient."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ABOUT) {
            showAboutDialog();
        } else if (item.id == ID_DESIGN) {
            presentFragment(new MgDesignActivity());
        } else if (item.id == ID_TOUR) {
            startTour();
        } else if (item.id >= MgSettingsPage.PAGE_GENERAL && item.id <= MgSettingsPage.PAGE_PRAYER) {
            presentFragment(new MgSettingsPage(item.id));
        }
    }

    @Override
    public android.view.View createView(android.content.Context context) {
        android.view.View v = super.createView(context);
        if (!org.telegram.messenger.MgConfig.getBool("tour_done", false)) {
            org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
                if (getParentActivity() != null && fragmentView != null && fragmentView.isAttachedToWindow()) {
                    startTour();
                }
            }, 700);
        }
        return v;
    }

    /** Yangi foydalanuvchi uchun bo'limlarni bittalab, strelka bilan ko'rsatib chiqish */
    private void startTour() {
        if (listView == null || !(fragmentView instanceof android.widget.FrameLayout) || getParentActivity() == null) {
            return;
        }
        org.telegram.messenger.MgConfig.setBool("tour_done", true);
        java.util.ArrayList<org.fenixuz.ui.onboarding.FenixTour.Step> steps = new java.util.ArrayList<>();
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_GENERAL, "Asosiy",
                "Katta shriftli oddiy rejim, silkitib yashirish va @username tekshirish shu yerda."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_CHATLIST, "Chat ro'yxati",
                "Jildlar, ikonkalar, toifalar va arxiv. Chatlarni ✓✓ bilan oraliqda, ☰ bilan hammasini belgilash mumkin."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_MESSAGES, "Xabarlar va tarjima",
                "Tarjima, maxsus uzatish, shablonlar, standart matn uslubi va yuborishdan oldin so'rash."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_AUTOMATION, "Avtomatlashtirish",
                "Band bo'lganingizda avto-javob, avto-tarjima, avto-imzo va qo'shilish so'rovlarini avtomatik qabul qilish."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_THEMES, "Mavzular",
                "Milliy naqshli mavzular va chat foni."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_NOTIFY, "Bildirishnomalar",
                "Fokus rejimi, akkauntlar bo'yicha bildirishnoma va javobsiz xabar eslatmasi."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_PRIVACY, "Maxfiylik va xavfsizlik",
                "Chat qulfi, yashirin bo'lim, notanishlardan himoya, APK blok va firibgarlikdan ogohlantirish."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(MgSettingsPage.PAGE_PRAYER, "Namoz vaqti va ob-havo",
                "Tumaningizni tanlang — chatlar ro'yxati tepasida keyingi namozgacha qolgan vaqt yoki ob-havo chiqadi."));
        steps.add(new org.fenixuz.ui.onboarding.FenixTour.Step(ID_TOUR, "Tanishtiruv",
                "Bu tanishtiruvni istalgan vaqtda shu yerdan qayta ko'rishingiz mumkin."));
        try {
            new org.fenixuz.ui.onboarding.FenixTour(getParentActivity(), (android.widget.FrameLayout) fragmentView, listView, steps).start();
        } catch (Throwable e) {
            org.telegram.messenger.FileLog.e(e);
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
