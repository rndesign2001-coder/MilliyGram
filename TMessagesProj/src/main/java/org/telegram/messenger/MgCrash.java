/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import org.telegram.ui.ActionBar.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Ilova kutilmaganda yopilsa, xato matnini (faqat texnik stack trace, shaxsiy ma'lumotsiz)
 * telefonda saqlaydi va keyingi ochilishda uni nusxalab yuborishni taklif qiladi.
 * Hech narsa avtomatik ravishda tashqariga yuborilmaydi.
 */
public final class MgCrash {

    private static final String FILE = "mg_last_crash.txt";
    private static boolean installed;

    private MgCrash() {
    }

    private static File file() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx == null ? null : new File(ctx.getFilesDir(), FILE);
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        final Thread.UncaughtExceptionHandler past = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            try {
                write(e, true);
            } catch (Throwable ignore) {
            }
            if (past != null) {
                past.uncaughtException(thread, e);
            }
        });
    }

    /** Ushlab qolingan (ilovani yopmagan) xatoni ham keyin ko'rish uchun saqlaydi */
    public static void remember(Throwable e) {
        try {
            write(e, false);
        } catch (Throwable ignore) {
        }
    }

    private static void write(Throwable e, boolean fatal) throws Exception {
        File f = file();
        if (f == null || e == null) {
            return;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("MilliyGram " + BuildVars.BUILD_VERSION_STRING + (fatal ? " — crash" : " — error"));
        pw.println("Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + "), " + Build.MANUFACTURER + " " + Build.MODEL);
        pw.println("Thread: " + Thread.currentThread().getName());
        e.printStackTrace(pw);
        pw.flush();
        String text = sw.toString();
        if (text.length() > 12000) {
            text = text.substring(0, 12000);
        }
        try (FileOutputStream out = new FileOutputStream(f, false)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read() {
        File f = file();
        if (f == null || !f.exists()) {
            return null;
        }
        try {
            byte[] data = new byte[(int) Math.min(f.length(), 16000)];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                int n = in.read(data);
                return n > 0 ? new String(data, 0, n, StandardCharsets.UTF_8) : null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    /** Oldingi ishga tushirishda xato bo'lgan bo'lsa — bir marta ko'rsatadi */
    public static void showIfAny(Activity activity) {
        if (activity == null) {
            return;
        }
        final String text = read();
        File f = file();
        if (f != null) {
            f.delete();
        }
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (activity.isFinishing()) {
                    return;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(activity);
                b.setTitle(MgLang.t("Xatolik qayd etildi"));
                b.setMessage(MgLang.t("Oldingi safar ilovada xatolik yuz berdi. Uni tuzatishimiz uchun xato matnini nusxalab, dasturchiga yuboring. Matnda faqat texnik ma'lumot bor."));
                b.setPositiveButton(MgLang.t("Nusxalash"), (d, w) -> {
                    try {
                        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("MilliyGram crash", text));
                        android.widget.Toast.makeText(activity, MgLang.t("Nusxalandi"), android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignore) {
                    }
                });
                b.setNegativeButton(MgLang.t("Yopish"), null);
                b.show();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }, 1200);
    }
}
