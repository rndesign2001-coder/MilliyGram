/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yangi versiya bildirishnomasi.
 * Yangi APK Telegram kanalga "#mgupdate 31" (31 — build raqami) belgisi bilan joylanadi
 * (GitHub Actions buni avtomatik qiladi). Ilova ochilganda kanal oxirgi postlarini tekshiradi
 * va o'zidan yangi build topilsa, "Yangilab oling" oynasi hamda bildirishnoma chiqaradi.
 */
public final class MgUpdater {

    private static final Pattern TAG = Pattern.compile("#mgupdate\\s*b?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final long INTERVAL_MS = 6 * 3600_000L;
    private static final String CHANNEL_ID = "mg_update_v1";
    private static boolean checking;
    private static boolean shownThisSession;

    private MgUpdater() {
    }

    public static int currentBuild() {
        return BuildConfig.MG_BUILD;
    }

    public static String channel() {
        String custom = MgConfig.getString("upd_channel", "");
        if (!TextUtils.isEmpty(custom)) {
            return custom.replace("@", "").trim();
        }
        return BuildConfig.MG_UPDATE_CHANNEL == null ? "" : BuildConfig.MG_UPDATE_CHANNEL;
    }

    public static boolean isEnabled() {
        return MgConfig.getBool("upd_check", true) && !isFromPlayStore();
    }

    /** Google Play'dan o'rnatilgan bo'lsa yangilanishni Play o'zi beradi (Play qoidasi) */
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

    /** Ilova ochilganda: 6 soatda bir marta tekshiradi */
    public static void check(Activity activity) {
        check(activity, false, null);
    }

    /**
     * @param done natija: null — tekshirib bo'lmadi, 0 — yangi versiya yo'q, >0 — yangi build raqami
     */
    public static void check(Activity activity, boolean force, Utilities.Callback<Integer> done) {
        try {
            String ch = channel();
            if (TextUtils.isEmpty(ch) || currentBuild() <= 0 || !force && !isEnabled() || isFromPlayStore() || checking) {
                if (done != null) {
                    done.run(null);
                }
                return;
            }
            long now = System.currentTimeMillis();
            long last = MgConfig.getLong("upd_last", 0);
            if (!force && now - last < INTERVAL_MS) {
                if (done != null) {
                    done.run(null);
                }
                return;
            }
            int account = UserConfig.selectedAccount;
            if (!UserConfig.getInstance(account).isClientActivated()) {
                if (done != null) {
                    done.run(null);
                }
                return;
            }
            checking = true;
            MgConfig.setLong("upd_last", now);
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = ch;
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
                if (!(res instanceof TLRPC.TL_contacts_resolvedPeer) || ((TLRPC.TL_contacts_resolvedPeer) res).chats.isEmpty()) {
                    finish(done, null);
                    return;
                }
                TLRPC.Chat chat = ((TLRPC.TL_contacts_resolvedPeer) res).chats.get(0);
                TLRPC.TL_inputPeerChannel peer = new TLRPC.TL_inputPeerChannel();
                peer.channel_id = chat.id;
                peer.access_hash = chat.access_hash;
                TLRPC.TL_messages_getHistory h = new TLRPC.TL_messages_getHistory();
                h.peer = peer;
                h.limit = 30;
                ConnectionsManager.getInstance(account).sendRequest(h, (res2, err2) -> {
                    if (!(res2 instanceof TLRPC.messages_Messages)) {
                        finish(done, null);
                        return;
                    }
                    int best = 0;
                    TLRPC.Message bestMsg = null;
                    for (TLRPC.Message m : ((TLRPC.messages_Messages) res2).messages) {
                        if (m == null || TextUtils.isEmpty(m.message)) {
                            continue;
                        }
                        Matcher mt = TAG.matcher(m.message);
                        if (mt.find()) {
                            try {
                                int b = Integer.parseInt(mt.group(1));
                                if (b > best) {
                                    best = b;
                                    bestMsg = m;
                                }
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                    final int found = best;
                    final TLRPC.Message msg = bestMsg;
                    AndroidUtilities.runOnUIThread(() -> {
                        checking = false;
                        if (found > currentBuild() && msg != null) {
                            MgConfig.setInt("upd_latest", found);
                            String link = "https://t.me/" + ch + "/" + msg.id;
                            MgConfig.setString("upd_link", link);
                            notifyUpdate(activity, found, msg.message, link, force);
                            if (done != null) {
                                done.run(found);
                            }
                        } else if (done != null) {
                            done.run(0);
                        }
                    });
                });
            });
        } catch (Throwable e) {
            FileLog.e(e);
            checking = false;
            if (done != null) {
                done.run(null);
            }
        }
    }

    private static void finish(Utilities.Callback<Integer> done, Integer v) {
        AndroidUtilities.runOnUIThread(() -> {
            checking = false;
            if (done != null) {
                done.run(v);
            }
        });
    }

    private static String notes(String text) {
        if (text == null) {
            return "";
        }
        String t = TAG.matcher(text).replaceAll("").trim();
        return t.length() > 700 ? t.substring(0, 700) + "…" : t;
    }

    private static void notifyUpdate(Activity activity, int build, String text, String link, boolean force) {
        boolean skipped = MgConfig.getInt("upd_skip", 0) == build;
        if (!force && skipped) {
            return;
        }
        // tizim bildirishnomasi — har bir yangi build uchun bir marta
        if (MgConfig.getInt("upd_notified", 0) != build) {
            MgConfig.setInt("upd_notified", build);
            showNotification(build, link);
        }
        if (activity == null || activity.isFinishing() || shownThisSession && !force) {
            return;
        }
        shownThisSession = true;
        try {
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            b.setTitle(MgLang.t("🎉 Yangi versiya chiqdi (b") + build + ")");
            String n = notes(text);
            b.setMessage(MgLang.t("Sizda: b") + currentBuild() + MgLang.t(". Yangilab oling — xatolar tuzatilgan va yangi imkoniyatlar qo'shilgan.")
                    + (n.isEmpty() ? "" : "\n\n" + n));
            b.setPositiveButton(MgLang.t("Yangilash"), (d, w) -> openLink(activity, link));
            b.setNegativeButton(MgLang.t("Keyinroq"), null);
            b.setNeutralButton(MgLang.t("O'tkazib yuborish"), (d, w) -> MgConfig.setInt("upd_skip", build));
            b.show();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void openLink(Context ctx, String link) {
        try {
            org.telegram.messenger.browser.Browser.openUrl(ctx, link);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void showNotification(int build, String link) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel c = new NotificationChannel(CHANNEL_ID, MgLang.t("Ilova yangilanishlari"), NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(c);
            }
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            i.setPackage(ctx.getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(ctx, 770411, i, flags);
            NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(MgLang.t("MilliyGram yangilandi: b") + build)
                    .setContentText(MgLang.t("Yangi versiyani yuklab oling — bosing"))
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            nm.notify(770411, nb.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
