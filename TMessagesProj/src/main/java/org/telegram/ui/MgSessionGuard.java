/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.app.Activity;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgSessions;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Yangi qurilmadan kirish ogohlantirishi: ilova ochilganda faol seanslar ro'yxati tekshiriladi.
 * Avval ko'rilmagan qurilma paydo bo'lsa — qurilma, joy va vaqt ko'rsatiladi hamda bir bosishda
 * uni chiqarib yuborish mumkin.
 */
public final class MgSessionGuard {

    private static final long CHECK_INTERVAL = 10 * 60 * 1000L;
    private static boolean showing;

    private MgSessionGuard() {
    }

    public static boolean isEnabled() {
        return MgConfig.getBool("session_guard", true);
    }

    private static String knownKey(int account) {
        return "sess_known_" + account;
    }

    private static HashSet<Long> known(int account) {
        HashSet<Long> set = new HashSet<>();
        String raw = MgConfig.getString(knownKey(account), null);
        if (!TextUtils.isEmpty(raw)) {
            for (String p : raw.split(",")) {
                try {
                    set.add(Long.parseLong(p));
                } catch (Throwable ignore) {
                }
            }
        }
        return set;
    }

    private static void saveKnown(int account, HashSet<Long> set) {
        StringBuilder sb = new StringBuilder();
        for (Long h : set) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(h);
        }
        MgConfig.setString(knownKey(account), sb.toString());
    }

    /** Ilova ochilganda chaqiriladi (10 daqiqada bir martadan ko'p tekshirmaydi) */
    public static void check(Activity activity, int account) {
        if (!isEnabled() || activity == null || showing || !UserConfig.getInstance(account).isClientActivated()) {
            return;
        }
        long now = System.currentTimeMillis();
        String timeKey = "sess_check_" + account;
        if (Math.abs(now - MgConfig.getLong(timeKey, 0)) < CHECK_INTERVAL) {
            return;
        }
        MgConfig.setLong(timeKey, now);
        TL_account.getAuthorizations req = new TL_account.getAuthorizations();
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (!(response instanceof TL_account.authorizations)) {
                return;
            }
            ArrayList<TLRPC.TL_authorization> list = ((TL_account.authorizations) response).authorizations;
            HashSet<Long> knownSet = known(account);
            boolean firstRun = MgConfig.getString(knownKey(account), null) == null;
            TLRPC.TL_authorization newest = null;
            for (TLRPC.TL_authorization a : list) {
                if (a == null || a.current || a.hash == 0) {
                    continue;
                }
                if (firstRun) {
                    knownSet.add(a.hash);
                } else if (!knownSet.contains(a.hash) && (newest == null || a.date_created > newest.date_created)) {
                    newest = a;
                }
            }
            // eskirgan (tugatilgan) seanslarni ro'yxatdan tozalash
            HashSet<Long> alive = new HashSet<>();
            for (TLRPC.TL_authorization a : list) {
                if (a != null && knownSet.contains(a.hash)) {
                    alive.add(a.hash);
                }
            }
            saveKnown(account, alive);
            if (newest != null) {
                show(activity, account, newest);
            }
        }));
    }

    private static void show(Activity activity, int account, TLRPC.TL_authorization a) {
        if (activity.isFinishing()) {
            return;
        }
        showing = true;
        StringBuilder sb = new StringBuilder();
        sb.append(MgLang.t("Akkauntingizga yangi qurilmadan kirildi:")).append("\n\n");
        sb.append("📱 ").append(TextUtils.isEmpty(a.device_model) ? "?" : a.device_model).append("\n");
        sb.append("💬 ").append(MgSessions.appName(a)).append(" ").append(a.app_version == null ? "" : a.app_version).append("\n");
        if (!TextUtils.isEmpty(a.country)) {
            sb.append("📍 ").append(a.country);
            if (!TextUtils.isEmpty(a.ip)) {
                sb.append(" (").append(a.ip).append(")");
            }
            sb.append("\n");
        }
        sb.append("🕐 ").append(LocaleController.formatDateTime(a.date_created, true)).append("\n\n");
        sb.append(MgLang.t("Agar bu siz bo'lmasangiz — darhol chiqarib yuboring va ikki bosqichli parolni yoqing."));
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("⚠️ " + MgLang.t("Yangi qurilma"));
        b.setMessage(sb.toString());
        b.setPositiveButton(MgLang.t("Bu menman"), (d, w) -> {
            HashSet<Long> set = known(account);
            set.add(a.hash);
            saveKnown(account, set);
        });
        b.setNegativeButton(MgLang.t("Chiqarib yuborish"), (d, w) -> terminate(activity, account, a));
        b.setOnDismissListener(d -> showing = false);
        AlertDialog dialog = b.create();
        dialog.setCanCancel(false);
        dialog.show();
        android.view.View button = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
        if (button instanceof android.widget.TextView) {
            ((android.widget.TextView) button).setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private static void terminate(Activity activity, int account, TLRPC.TL_authorization a) {
        TL_account.resetAuthorization req = new TL_account.resetAuthorization();
        req.hash = a.hash;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (activity.isFinishing()) {
                return;
            }
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            if (error == null) {
                b.setTitle(MgLang.t("Qurilma chiqarib yuborildi"));
                b.setMessage(MgLang.t("Xavfsizlik uchun Sozlamalar → Maxfiylik va xavfsizlik → Ikki bosqichli tekshiruv bo'limida parol o'rnating."));
            } else {
                b.setTitle(MgLang.t("Chiqarib bo'lmadi"));
                b.setMessage(MgLang.t("Agar shu telefonda yaqinda (24 soat ichida) kirgan bo'lsangiz, Telegram bu seansga boshqa qurilmalarni chiqarib yuborishga ruxsat bermaydi. Eski qurilmangizdan Sozlamalar → Qurilmalar orqali tugating.") + "\n\n" + error.text);
            }
            b.setPositiveButton("OK", null);
            b.show();
        }));
    }
}
