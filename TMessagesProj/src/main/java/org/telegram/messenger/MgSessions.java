/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import org.telegram.tgnet.TLRPC;

import java.util.Locale;

/**
 * Faol seanslar: qurilma nomini aniq ko'rsatish ("Samsung Galaxy A56 5G"),
 * MilliyGram orqali kirilgan seanslarda ilova nomi va logotipi.
 */
public final class MgSessions {

    private static String cachedName;
    private static Bitmap logo;

    private MgSessions() {
    }

    /** Bu seans MilliyGram orqali ochilganmi */
    public static boolean isOurs(TLRPC.TL_authorization s) {
        return s != null && BuildVars.APP_ID != 0 && s.api_id == BuildVars.APP_ID;
    }

    public static String appName(TLRPC.TL_authorization s) {
        return isOurs(s) ? "MilliyGram" : s.app_name;
    }

    /** Dumaloq MilliyGram logotipi */
    public static Drawable logo() {
        try {
            if (logo == null) {
                Drawable d = ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.mipmap.ic_launcher_round);
                if (d == null) {
                    return null;
                }
                int size = 144;
                Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                Path clip = new Path();
                clip.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW);
                c.clipPath(clip);
                d.setBounds(0, 0, size, size);
                d.draw(c);
                logo = b;
            }
            BitmapDrawable bd = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), logo);
            bd.setBounds(0, 0, logo.getWidth(), logo.getHeight());
            return bd;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** Tizim versiyasi: "Android 15 (SDK 35)" — boshqa ilovalarda ham Android belgisi chiqadi */
    public static String systemVersion() {
        return "Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")";
    }

    /** Qurilmaning tijorat nomi, masalan "Samsung Galaxy A56 5G" */
    public static String deviceName() {
        if (cachedName != null) {
            return cachedName;
        }
        String brand = capitalize(Build.MANUFACTURER);
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        String market = null;
        for (String key : new String[]{"ro.product.marketname", "ro.product.vendor.marketname", "ro.config.marketing_name", "ro.vendor.oplus.market.name", "ro.oppo.market.name", "ro.vivo.market.name"}) {
            market = sysProp(key);
            if (!TextUtils.isEmpty(market)) {
                break;
            }
        }
        if (TextUtils.isEmpty(market) && Build.VERSION.SDK_INT >= 25) {
            try {
                String n = Settings.Global.getString(ApplicationLoader.applicationContext.getContentResolver(), Settings.Global.DEVICE_NAME);
                // foydalanuvchi o'zi qo'ygan nom ("Alining telefoni") emas, balki model nomi bo'lsa
                if (looksLikeModel(n)) {
                    market = n;
                }
            } catch (Throwable ignore) {
            }
        }
        String name = TextUtils.isEmpty(market) ? model : market.trim();
        if (TextUtils.isEmpty(name)) {
            name = "Android";
        }
        if (!TextUtils.isEmpty(brand) && !name.toLowerCase(Locale.US).startsWith(brand.toLowerCase(Locale.US))) {
            name = brand + " " + name;
        }
        if (name.length() > 60) {
            name = name.substring(0, 60);
        }
        cachedName = name;
        return name;
    }

    private static boolean looksLikeModel(String n) {
        if (TextUtils.isEmpty(n) || n.length() > 40) {
            return false;
        }
        if (n.contains("'") || n.contains("’") || n.contains("`")) {
            return false;
        }
        boolean digit = false;
        for (int i = 0; i < n.length(); i++) {
            if (Character.isDigit(n.charAt(i))) {
                digit = true;
                break;
            }
        }
        if (!digit) {
            return false;
        }
        String l = n.toLowerCase(Locale.US);
        String[] known = {"galaxy", "redmi", "poco", "xiaomi", "mi ", "pixel", "honor", "huawei", "nova", "oppo", "reno", "vivo", "realme", "oneplus", "infinix", "tecno", "spark", "camon", "moto", "nokia", "zte", "blade", "itel", "note", "sony", "xperia", "asus", "zenfone", "rog", "nothing", "lenovo", "tab", "samsung", "iqoo", "narzo", "hot "};
        for (String k : known) {
            if (l.startsWith(k) || l.contains(" " + k.trim())) {
                return true;
            }
        }
        String m = Build.MODEL == null ? "" : Build.MODEL.toLowerCase(Locale.US);
        return !m.isEmpty() && l.contains(m);
    }

    private static String sysProp(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Object v = c.getMethod("get", String.class).invoke(null, key);
            return v instanceof String ? ((String) v).trim() : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static String capitalize(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        s = s.trim();
        if (s.equalsIgnoreCase("oneplus")) {
            return "OnePlus";
        }
        if (s.equalsIgnoreCase("hmd global")) {
            return "Nokia";
        }
        return s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1).toLowerCase(Locale.US);
    }
}
