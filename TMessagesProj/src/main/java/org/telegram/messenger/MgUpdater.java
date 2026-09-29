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
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            b.setTitle(MgLang.t("🎉 Yangi versiya tayyor"));
            b.setMessage(MgLang.t("MilliyGram'ning yangi versiyasi Google Play'dan yuklab olindi. O'rnatish uchun ilova bir necha soniyaga qayta ishga tushadi."));
            b.setPositiveButton(MgLang.t("O'rnatish"), (d, w) -> {
                try {
                    manager().completeUpdate();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
            b.setNegativeButton(MgLang.t("Keyinroq"), null);
            AlertDialog dlg = b.create();
            dlg.setOnDismissListener(d -> promptShown = false);
            dlg.show();
        } catch (Throwable e) {
            promptShown = false;
            FileLog.e(e);
        }
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
