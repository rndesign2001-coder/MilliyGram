/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Ob-havo: Open-Meteo (bepul, kalitsiz) xizmatidan tanlangan tuman koordinatasi bo'yicha.
 * Natija 30 daqiqa keshlanadi; internet bo'lmasa oxirgi ma'lumot ko'rsatiladi.
 */
public final class MgWeather {

    public static final class Data {
        public double temp, feels, wind;
        public int code, humidity;
        public int[] dayCode = new int[3];
        public double[] dayMax = new double[3], dayMin = new double[3];
        public long time;
    }

    private static final long TTL = 30 * 60 * 1000L;
    private static volatile boolean loading;
    private static Data cached;

    public interface Callback {
        void onLoaded(Data data);
    }

    public static Data getCached() {
        if (cached == null) {
            cached = parse(MgConfig.getString("wx_cache", null));
        }
        return cached;
    }

    /** Kerak bo'lsa yangilaydi; callback UI oqimida chaqiriladi */
    public static void refresh(boolean force, Callback cb) {
        Data c = getCached();
        if (!force && c != null && System.currentTimeMillis() - c.time < TTL) {
            if (cb != null) {
                cb.onLoaded(c);
            }
            return;
        }
        if (loading) {
            return;
        }
        loading = true;
        final MgPlaces.Place place = MgPrayer.getPlace();
        Utilities.globalQueue.postRunnable(() -> {
            String json = null;
            try {
                String url = String.format(Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f"
                                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                                + "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia%%2FTashkent&forecast_days=3&wind_speed_unit=ms",
                        place.lat, place.lon);
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                if (con.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    JSONObject o = new JSONObject(sb.toString());
                    o.put("_t", System.currentTimeMillis());
                    json = o.toString();
                }
                con.disconnect();
            } catch (Throwable e) {
                FileLog.e(e);
            }
            final String result = json;
            AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                if (result != null) {
                    MgConfig.setString("wx_cache", result);
                    cached = parse(result);
                }
                if (cb != null) {
                    cb.onLoaded(getCached());
                }
            });
        });
    }

    private static Data parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            JSONObject cur = o.getJSONObject("current");
            Data d = new Data();
            d.temp = cur.getDouble("temperature_2m");
            d.feels = cur.optDouble("apparent_temperature", d.temp);
            d.humidity = cur.optInt("relative_humidity_2m", 0);
            d.code = cur.optInt("weather_code", 0);
            d.wind = cur.optDouble("wind_speed_10m", 0);
            JSONObject daily = o.optJSONObject("daily");
            if (daily != null) {
                JSONArray c = daily.getJSONArray("weather_code");
                JSONArray mx = daily.getJSONArray("temperature_2m_max");
                JSONArray mn = daily.getJSONArray("temperature_2m_min");
                for (int i = 0; i < 3 && i < c.length(); i++) {
                    d.dayCode[i] = c.optInt(i);
                    d.dayMax[i] = mx.optDouble(i);
                    d.dayMin[i] = mn.optDouble(i);
                }
            }
            d.time = o.optLong("_t", 0);
            return d;
        } catch (Throwable e) {
            return null;
        }
    }

    public static String temp(double t) {
        long r = Math.round(t);
        return (r > 0 ? "+" : "") + r + "°";
    }

    /** WMO ob-havo kodi → belgi */
    public static String icon(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫";
        if (code >= 51 && code <= 57) return "🌦";
        if (code >= 61 && code <= 67 || code >= 80 && code <= 82) return "🌧";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "❄️";
        if (code >= 95) return "⛈";
        return "🌡";
    }

    /** WMO ob-havo kodi → o'zbekcha tavsif */
    public static String describe(int code) {
        if (code == 0) return "Ochiq osmon";
        if (code == 1) return "Asosan ochiq";
        if (code == 2) return "Qisman bulutli";
        if (code == 3) return "Bulutli";
        if (code == 45 || code == 48) return "Tuman";
        if (code >= 51 && code <= 57) return "Mayda yomg'ir";
        if (code >= 61 && code <= 65) return "Yomg'ir";
        if (code == 66 || code == 67) return "Muzlovchi yomg'ir";
        if (code >= 71 && code <= 77) return "Qor";
        if (code >= 80 && code <= 82) return "Jala";
        if (code == 85 || code == 86) return "Qor yog'ishi";
        if (code >= 95) return "Momaqaldiroq";
        return "—";
    }
}
