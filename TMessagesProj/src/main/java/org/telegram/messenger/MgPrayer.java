/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * Hisoblash usuli PrayTimes.org (Hamid Zarrabi-Zadeh, LGPL) algoritmiga asoslangan, qayta yozilgan.
 */

package org.telegram.messenger;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Namoz vaqtlarini internetsiz, koordinata bo'yicha hisoblaydi.
 * O'zbekiston uchun: Bomdod va Xufton — 15.5° (O'zbekiston musulmonlari idorasi taqvimi bilan solishtirilgan), Asr — Hanafiy mazhabi (soya ikki barobar).
 * Har bir vaqtga foydalanuvchi o'z masjidiga moslab daqiqa qo'shishi/ayirishi mumkin.
 */
public final class MgPrayer {

    public static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    public static final String[] NAMES = {"Bomdod", "Quyosh", "Peshin", "Asr", "Shom", "Xufton"};
    /** Namoz bo'lmagan "Quyosh chiqishi" eslatma va "keyingi namoz" hisobidan chiqariladi */
    public static boolean isPrayer(int i) {
        return i != SUNRISE;
    }

    private static final double FAJR_ANGLE = 15.5;
    private static final double ISHA_ANGLE = 15.5;
    private static final double ASR_FACTOR = 2.0; // Hanafiy
    /** Ehtiyot daqiqalari (mahalliy taqvimlarga yaqinlashtirish uchun) */
    private static final int[] BASE_OFFSET = {0, 0, 0, 0, 5, 0};

    public static final TimeZone TZ = TimeZone.getTimeZone("Asia/Tashkent");

    // ============ sozlamalar ============

    public static String getRegionKey() {
        return MgConfig.getString("pr_region", "tashkent_city");
    }

    public static String getPlaceName() {
        return MgConfig.getString("pr_place", "");
    }

    public static MgPlaces.Place getPlace() {
        return MgPlaces.findPlace(getRegionKey(), getPlaceName());
    }

    public static void setPlace(String regionKey, String placeName) {
        MgConfig.setString("pr_region", regionKey);
        MgConfig.setString("pr_place", placeName);
        MgConfig.setString("wx_cache", null);
    }

    public static int getUserOffset(int i) {
        return MgConfig.getInt("pr_off_" + i, 0);
    }

    public static void setUserOffset(int i, int minutes) {
        MgConfig.setInt("pr_off_" + i, minutes);
    }

    // ============ hisoblash ============

    /** Bugungi (yoki berilgan kundagi) vaqtlar: kun boshidan daqiqalarda, Toshkent vaqti bilan */
    public static int[] times(Calendar day, MgPlaces.Place place) {
        return times(day, place, true);
    }

    /** Foydalanuvchi tuzatishisiz (asl hisob) vaqtlar */
    public static int[] baseTimes() {
        return times(Calendar.getInstance(TZ), getPlace(), false);
    }

    /** [quyosh chiqishi, quyosh botishi] — sof astronomik, ehtiyot va tuzatishlarsiz */
    public static int[] sunTimes() {
        int[] t = times(Calendar.getInstance(TZ), getPlace(), false);
        return new int[]{t[SUNRISE], ((t[MAGHRIB] - BASE_OFFSET[MAGHRIB]) % 1440 + 1440) % 1440};
    }

    public static int[] times(Calendar day, MgPlaces.Place place, boolean withUser) {
        int y = day.get(Calendar.YEAR), m = day.get(Calendar.MONTH) + 1, d = day.get(Calendar.DAY_OF_MONTH);
        double jDate = julian(y, m, d) - place.lon / (15.0 * 24.0);
        double tz = 5.0;
        double lat = place.lat, lng = place.lon;

        // Birinchi yaqinlashish
        double[] t = {5, 6, 12, 13, 18, 18};
        for (int iter = 0; iter < 2; iter++) {
            double[] dayPortion = new double[6];
            for (int i = 0; i < 6; i++) {
                dayPortion[i] = t[i] / 24.0;
            }
            double fajr = sunAngleTime(jDate, FAJR_ANGLE, dayPortion[0], lat, true);
            double sunrise = sunAngleTime(jDate, riseSetAngle(), dayPortion[1], lat, true);
            double dhuhr = midDay(jDate, dayPortion[2]);
            double asr = asrTime(jDate, ASR_FACTOR, dayPortion[3], lat);
            double sunset = sunAngleTime(jDate, riseSetAngle(), dayPortion[4], lat, false);
            double isha = sunAngleTime(jDate, ISHA_ANGLE, dayPortion[5], lat, false);
            t = new double[]{fajr, sunrise, dhuhr, asr, sunset, isha};
        }
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) {
            double h = t[i] + tz - lng / 15.0;
            if (Double.isNaN(h)) {
                h = i < 2 ? 5 : 20;
            }
            int minutes = (int) Math.round(h * 60.0) + BASE_OFFSET[i] + (withUser ? getUserOffset(i) : 0);
            out[i] = ((minutes % 1440) + 1440) % 1440;
        }
        return out;
    }

    public static int[] today() {
        return times(Calendar.getInstance(TZ), getPlace());
    }

    /** [indeks, qolgan daqiqa] — keyingi namoz (Quyosh chiqishi hisobga olinmaydi) */
    public static int[] next() {
        Calendar now = Calendar.getInstance(TZ);
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int[] t = times(now, getPlace());
        for (int i = 0; i < 6; i++) {
            if (!isPrayer(i)) {
                continue;
            }
            if (t[i] > cur) {
                return new int[]{i, t[i] - cur};
            }
        }
        Calendar tomorrow = (Calendar) now.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        int[] t2 = times(tomorrow, getPlace());
        return new int[]{FAJR, 1440 - cur + t2[FAJR]};
    }

    /** Hozirgi namoz vaqti indeksi (ro'yxatda ajratib ko'rsatish uchun) yoki -1 */
    public static int current(int[] t) {
        Calendar now = Calendar.getInstance(TZ);
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int res = -1;
        for (int i = 0; i < 6; i++) {
            if (t[i] <= cur) {
                res = i;
            }
        }
        return res == -1 ? ISHA : res;
    }

    public static String hhmm(int minutes) {
        return String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    public static String left(int minutes) {
        if (minutes < 60) {
            return minutes + " daq";
        }
        return (minutes / 60) + ":" + String.format(java.util.Locale.US, "%02d", minutes % 60);
    }

    // ============ astronomiya ============

    private static double riseSetAngle() {
        return 0.833;
    }

    private static double julian(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0;
        double eqt = q / 15.0 - fixHour(ra);
        double decl = darcsin(dsin(e) * dsin(l));
        return new double[]{decl, eqt};
    }

    private static double midDay(double jDate, double time) {
        double eqt = sunPosition(jDate + time)[1];
        return fixHour(12 - eqt);
    }

    private static double sunAngleTime(double jDate, double angle, double time, double lat, boolean ccw) {
        double decl = sunPosition(jDate + time)[0];
        double noon = midDay(jDate, time);
        double t = (1.0 / 15.0) * darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat)));
        return noon + (ccw ? -t : t);
    }

    private static double asrTime(double jDate, double factor, double time, double lat) {
        double decl = sunPosition(jDate + time)[0];
        double angle = -darccot(factor + dtan(Math.abs(lat - decl)));
        return sunAngleTime(jDate, angle, time, lat, false);
    }

    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double dtan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double darcsin(double x) { return Math.toDegrees(Math.asin(x)); }
    private static double darccos(double x) { return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, x)))); }
    private static double darctan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    private static double darccot(double x) { return Math.toDegrees(Math.atan(1.0 / x)); }
    private static double fixAngle(double a) { a = a - 360.0 * Math.floor(a / 360.0); return a < 0 ? a + 360 : a; }
    private static double fixHour(double a) { a = a - 24.0 * Math.floor(a / 24.0); return a < 0 ? a + 24 : a; }
}
