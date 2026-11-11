/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.telegram.ui.ActionBar.AlertDialog;

/**
 * Yangi versiya: Google Play'ning rasmiy "In-App Updates" mexanizmi orqali.
 * Play'da yangi versiya chiqsa, ilova buni o'zi aniqlaydi, fonda yuklab oladi va
 * "Qayta ishga tushirib o'rnatish" deb so'raydi. Muhim yangilanishlar (Play Console'da
 * prioritet 4–5 yoki 7 kundan beri o'rnatilmagan) to'liq ekranli oyna bilan taklif qilinadi.
 * Ilova Play'dan o'rnatilmagan bo'lsa (APK), avtomatik tekshirilmaydi.
 */
public final class MgUpdater {

    private static final int REQ = 770511;
    private static final long INTERVAL_MS = 6 * 3600_000L;
    private static AppUpdateManager manager;
    private static InstallStateUpdatedListener listener;
    private static boolean promptShown;

    private MgUpdater() {
    }

    public static int currentBuild() {
        return BuildConfig.MG_BUILD;
    }

    /** MilliyGram versiyasi, masalan "1.0.0" (Telegram asos versiyasi — BuildVars.BUILD_VERSION_STRING) */
    public static String versionName() {
        return BuildConfig.MG_VERSION_NAME;
    }

    /** Google Play'dan o'rnatilganmi */
    public static boolean isFromPlayStore() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            String installer;
            if (Build.VERSION.SDK_INT >= 30) {
                installer = ctx.getPackageManager().getInstallSourceInfo(ctx.getPackageName()).getInstallingPackageName();
            } else {
                installer = ctx.getPackageManager().getInstallerPackageName(ctx.getPackageName());
            }
            return "com.android.vending".equals(installer);
        } catch (Throwable e) {
            return false;
        }
    }

    private static AppUpdateManager manager() {
        if (manager == null) {
            manager = AppUpdateManagerFactory.create(ApplicationLoader.applicationContext);
        }
        return manager;
    }

    /** LaunchActivity.onResume'dan */
    public static void check(Activity activity) {
        check(activity, false, null);
    }

    /**
     * @param done natija: null — tekshirib bo'lmadi, false — yangi versiya yo'q, true — yangilanish boshlandi
     */
    public static void check(Activity activity, boolean force, Utilities.Callback<Boolean> done) {
        if (activity == null || !isFromPlayStore()) {
            if (done != null) {
                done.run(null);
            }
            return;
        }
        try {
            AppUpdateManager m = manager();
            m.getAppUpdateInfo().addOnSuccessListener(info -> {
                // oldin yuklab olingan, lekin o'rnatilmagan yangilanish — har safar eslatamiz
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    askInstall(activity);
                    if (done != null) {
                        done.run(true);
                    }
                    return;
                }
                // to'liq ekranli yangilanish yarim yo'lda qolgan bo'lsa — davom ettiramiz
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    start(m, info, activity, AppUpdateType.IMMEDIATE);
                    if (done != null) {
                        done.run(true);
                    }
                    return;
                }
                long now = System.currentTimeMillis();
                if (!force && now - MgConfig.getLong("upd_last", 0) < INTERVAL_MS) {
                    if (done != null) {
                        done.run(false);
                    }
                    return;
                }
                MgConfig.setLong("upd_last", now);
                if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                    if (done != null) {
                        done.run(false);
                    }
                    return;
                }
                Integer stale = info.clientVersionStalenessDays();
                boolean important = info.updatePriority() >= 4 || stale != null && stale >= 7;
                int type = important && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ? AppUpdateType.IMMEDIATE : AppUpdateType.FLEXIBLE;
                if (!info.isUpdateTypeAllowed(type)) {
                    if (done != null) {
                        done.run(false);
                    }
                    return;
                }
                start(m, info, activity, type);
                if (done != null) {
                    done.run(true);
                }
            }).addOnFailureListener(e -> {
                FileLog.e(e);
                if (done != null) {
                    done.run(null);
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
            if (done != null) {
                done.run(null);
            }
        }
    }

    private static void start(AppUpdateManager m, AppUpdateInfo info, Activity activity, int type) {
        try {
            if (type == AppUpdateType.FLEXIBLE && listener == null) {
                listener = state -> {
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        AndroidUtilities.runOnUIThread(() -> {
                            Activity a = org.telegram.ui.LaunchActivity.instance;
                            askInstall(a != null ? a : activity);
                        });
                    }
                };
                m.registerListener(listener);
            }
            m.startUpdateFlowForResult(info, activity, AppUpdateOptions.newBuilder(type).build(), REQ);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /** Yangilanish yuklab olindi — o'rnatish uchun ilovani qayta ishga tushirishni so'raydi */
    private static void askInstall(Activity activity) {
        if (activity == null || activity.isFinishing() || promptShown) {
            return;
        }
        promptShown = true;
        try {
            AlertDialog dlg = styledDialog(activity,
                    MgLang.t("Yangi versiya tayyor"),
                    MgLang.t("MilliyGram'ning yangi versiyasi Google Play'dan yuklab olindi. O'rnatish uchun ilova bir necha soniyaga qayta ishga tushadi."),
                    null,
                    MgLang.t("O'rnatish"), () -> {
                        try {
                            manager().completeUpdate();
                        } catch (Throwable e) {
                            FileLog.e(e);
                        }
                    },
                    MgLang.t("Keyinroq"));
            dlg.setOnDismissListener(d -> promptShown = false);
            dlg.show();
        } catch (Throwable e) {
            promptShown = false;
            FileLog.e(e);
        }
    }

    /**
     * MilliyGram uslubidagi dialog: logo, sarlavha, matn, ixtiyoriy ro'yxat (punktlar),
     * asosiy (urg'u rangli) va ikkinchi darajali plitka tugmalar.
     */
    public static AlertDialog styledDialog(Activity activity, CharSequence title, CharSequence text, String[] bullets,
                                           CharSequence primary, Runnable onPrimary, CharSequence secondary) {
        final AlertDialog[] dialog = new AlertDialog[1];
        android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(22), AndroidUtilities.dp(22), AndroidUtilities.dp(14));
        android.widget.ImageView logo = new android.widget.ImageView(activity);
        logo.setImageResource(R.mipmap.ic_launcher_round);
        box.addView(logo, org.telegram.ui.Components.LayoutHelper.createLinear(68, 68, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));
        android.widget.TextView t = new android.widget.TextView(activity);
        t.setText(title);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 20);
        t.setTypeface(AndroidUtilities.bold());
        t.setGravity(android.view.Gravity.CENTER);
        t.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlack));
        box.addView(t, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        if (text != null) {
            android.widget.TextView m = new android.widget.TextView(activity);
            m.setText(text);
            m.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 15);
            m.setGravity(android.view.Gravity.CENTER);
            m.setLineSpacing(AndroidUtilities.dp(2), 1f);
            m.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextGray3));
            box.addView(m, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        }
        if (bullets != null && bullets.length > 0) {
            android.widget.LinearLayout list = new android.widget.LinearLayout(activity);
            list.setOrientation(android.widget.LinearLayout.VERTICAL);
            int fg = org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlack);
            list.setBackground(org.telegram.ui.MgUi.round(org.telegram.ui.ActionBar.Theme.multAlpha(fg, 0.06f), 14));
            list.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
            for (String b : bullets) {
                android.widget.TextView row = new android.widget.TextView(activity);
                row.setText("\u2022  " + b);
                row.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 15);
                row.setTextColor(fg);
                row.setLineSpacing(AndroidUtilities.dp(1), 1f);
                list.addView(row, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 3, 0, 3));
            }
            box.addView(list, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));
        }
        if (primary != null) {
            box.addView(org.telegram.ui.MgUi.accentTile(activity, null, primary, () -> {
                if (dialog[0] != null) {
                    dialog[0].dismiss();
                }
                if (onPrimary != null) {
                    onPrimary.run();
                }
            }), org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, 50, 0, 0, 0, 8));
        }
        if (secondary != null) {
            box.addView(org.telegram.ui.MgUi.tile(activity, null, secondary, () -> {
                if (dialog[0] != null) {
                    dialog[0].dismiss();
                }
            }), org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, 50));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setView(box);
        dialog[0] = b.create();
        return dialog[0];
    }

    /** Joriy versiyadagi yangiliklar ("Yangi versiyada nimalar bor" oynasi uchun) */
    private static final String[] WHATS_NEW = {
            "Bosh ekran uchun kanal statistikasi vidjeti",
            "PIN-kod himoyasi kuchaytirildi, yashirin chatlar ro'yxati shifrlanadi",
            "Yashirin chatlar endi Ulashish oynasida ham ko'rinmaydi",
            "\"Faqat o'zimdan o'chirish\" qilingan post qaytib chiqmaydi",
            "Video bildirishnomasi endi surib o'chiriladi",
            "Tasdiqlash oynalari va menyular yangi yumaloq uslubda",
    };

    /** Ilova yangilangandan keyingi birinchi ochilishda yangiliklarni bir marta ko'rsatadi */
    public static void maybeShowWhatsNew(Activity activity) {
        final String ver = versionName();
        if (activity == null || ver == null || ver.equals(MgConfig.getString("whatsnew_ver", null))) {
            return;
        }
        // Yangi o'rnatishda (tanishtiruv hali ko'rsatilmagan) — faqat belgilab qo'yamiz
        if (!MgConfig.getBool("onboard_v3", false)
                || !UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            MgConfig.setString("whatsnew_ver", ver);
            return;
        }
        MgConfig.setString("whatsnew_ver", ver);
        AndroidUtilities.runOnUIThread(() -> {
            if (activity.isFinishing()) {
                return;
            }
            try {
                String[] items = new String[WHATS_NEW.length];
                for (int i = 0; i < items.length; i++) {
                    items[i] = MgLang.t(WHATS_NEW[i]);
                }
                styledDialog(activity,
                        MgLang.t("MilliyGram yangilandi") + " · " + ver,
                        MgLang.t("Yangi versiyada nimalar bor:"),
                        items,
                        MgLang.t("Zo'r!"), null,
                        null).show();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }, 1500);
    }

    /** Ilovaning Google Play sahifasini ochadi */
    public static void openPlayPage(Context ctx) {
        String pkg = ctx.getPackageName();
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
            i.setPackage("com.android.vending");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Throwable e) {
            org.telegram.messenger.browser.Browser.openUrl(ctx, "https://play.google.com/store/apps/details?id=" + pkg);
        }
    }
}
