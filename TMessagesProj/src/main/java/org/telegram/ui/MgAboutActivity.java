/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgUpdater;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * "MilliyGram haqida": logo, versiyalar, rasmiy kanallar, yordam xizmati va ilovani baholash.
 */
public class MgAboutActivity extends UniversalFragment {

    public static final String SUPPORT_USER = "Rustam_Nigmatov";
    private static final String PRIVACY_URL = "https://github.com/rndesign2001-coder/MilliyGram/blob/claude/zen-hypatia-ngf4ae/docs/play-market/privacy-policy.md";

    private static final int CARD_HEADER = 700;
    private static final int CARD_CHANNELS = 701;
    private static final int CARD_HELP = 702;

    @Override
    protected CharSequence getTitle() {
        return MgLang.t("Ilova haqida");
    }

    private void openUser(String username) {
        if (getParentActivity() == null) {
            return;
        }
        try {
            getMessagesController().openByUserName(username, this, 1);
        } catch (Throwable e) {
            Browser.openUrl(getParentActivity(), "https://t.me/" + username);
        }
    }

    private void rateApp() {
        Context ctx = getParentActivity();
        if (ctx == null) {
            return;
        }
        String pkg = ApplicationLoader.applicationContext.getPackageName();
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
            intent.setPackage("com.android.vending");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Throwable e) {
            Browser.openUrl(ctx, "https://play.google.com/store/apps/details?id=" + pkg);
        }
    }

    private void checkUpdate() {
        if (getParentActivity() == null) {
            return;
        }
        if (!MgUpdater.isFromPlayStore()) {
            MgUpdater.openPlayPage(getParentActivity());
            return;
        }
        BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, MgLang.t("Tekshirilmoqda…")).show();
        MgUpdater.check(getParentActivity(), true, started -> {
            if (started == null) {
                BulletinFactory.of(this).createErrorBulletin(MgLang.t("Tekshirib bo'lmadi (internetni tekshiring)")).show();
            } else if (!started) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, MgLang.t("Sizda eng so'nggi versiya o'rnatilgan")).show();
            }
        });
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        String mgVersion = MgUpdater.versionName();
        items.add(UItem.asCustomShadow(CARD_HEADER, MgSettingsUi.aboutHeader(ctx, getResourceProvider(), mgVersion, BuildVars.BUILD_VERSION_STRING)));

        ArrayList<MgSettingsUi.Row> channels = new ArrayList<>();
        channels.add(new MgSettingsUi.Row(R.drawable.msg_channel, 0xFF2F8CFF, MgLang.t("Yangiliklar kanali"), "@" + MilliyGramSettingsActivity.NEWS_CHANNEL, null,
                () -> openUser(MilliyGramSettingsActivity.NEWS_CHANNEL)));
        channels.add(new MgSettingsUi.Row(R.drawable.msg_groups, 0xFF8B5CF6, MgLang.t("Loyiha chati"), "@" + MilliyGramSettingsActivity.CHAT_GROUP, null,
                () -> openUser(MilliyGramSettingsActivity.CHAT_GROUP)));
        items.add(UItem.asCustomShadow(CARD_CHANNELS, MgSettingsUi.section(ctx, getResourceProvider(), MgLang.t("Bizning kanallarimiz"), channels)));

        ArrayList<MgSettingsUi.Row> help = new ArrayList<>();
        help.add(new MgSettingsUi.Row(R.drawable.msg_openprofile, 0xFF22C55E, MgLang.t("Yordam xizmati bilan bog'lanish"), MgLang.t("Adminga to'g'ridan-to'g'ri yozing"), null,
                () -> openUser(SUPPORT_USER)));
        help.add(new MgSettingsUi.Row(R.drawable.msg_fave, 0xFFF59E0B, MgLang.t("Ilovani baholang"), MgLang.t("Google Play'da sharh qoldiring"), null, this::rateApp));
        help.add(new MgSettingsUi.Row(R.drawable.msg_retry, 0xFF0EA5E9, MgLang.t("Yangilanishni tekshirish"), MgLang.t("Eng so'nggi versiyani o'rnating"), null, this::checkUpdate));
        items.add(UItem.asCustomShadow(CARD_HELP, MgSettingsUi.section(ctx, getResourceProvider(), MgLang.t("Yordam"), help)));

        // Google Play qoidasi: ilova mustaqil ekanligi va ma'lumotlardan foydalanish aniq yozilgan
        // (Play talabi: maxfiylik siyosati ilova ichida ham ochilishi kerak — shu yerda havola sifatida)
        CharSequence policy = org.telegram.messenger.AndroidUtilities.replaceSingleTag("**" + MgLang.t("Maxfiylik siyosati") + "**",
                () -> Browser.openUrl(getParentActivity(), PRIVACY_URL));
        items.add(UItem.asShadow(new android.text.SpannableStringBuilder(MgLang.t("MilliyGram — Telegram API asosida ishlaydigan mustaqil ilova. Biz shaxsiy ma'lumotlaringizni yig'maymiz, saqlamaymiz va uchinchi shaxslarga bermaymiz. Xabarlaringiz to'g'ridan-to'g'ri Telegram serverlari orqali shifrlangan holda yuboriladi."))
                .append("\n\n").append(policy)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
