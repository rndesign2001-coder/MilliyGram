/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgPlaces;
import org.telegram.messenger.MgPrayer;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/**
 * Qibla kompasi: Makkaga yo'nalish (tanlangan tuman yoki GPS bo'yicha), jonli kompas, 3 xil dizayn.
 */
public class MgQibla {

    public static final double KAABA_LAT = 21.422487;
    public static final double KAABA_LON = 39.826206;

    public static final int STYLE_GOLD = 0;
    public static final int STYLE_EMERALD = 1;
    public static final int STYLE_NIGHT = 2;

    public static String[] styleNames() {
        return new String[]{MgLang.t("Oltin"), MgLang.t("Zumrad naqsh"), MgLang.t("Tungi")};
    }

    public static int getStyle() {
        return Math.max(0, Math.min(2, MgConfig.getInt("qibla_style", STYLE_GOLD)));
    }

    public static void setStyle(int s) {
        MgConfig.setInt("qibla_style", s);
    }

    public static boolean isGpsEnabled() {
        return MgConfig.getBool("qibla_use_gps", false);
    }

    // ------------------------------------------------------------------ geometriya

    /** [lat, lon, fromGps(1/0)] */
    public static double[] location() {
        if (isGpsEnabled()) {
            String raw = MgConfig.getString("qibla_gps", "");
            if (raw != null && !raw.isEmpty()) {
                try {
                    String[] p = raw.split(",");
                    return new double[]{Double.parseDouble(p[0]), Double.parseDouble(p[1]), 1};
                } catch (Throwable ignore) {
                }
            }
        }
        MgPlaces.Place pl = MgPrayer.getPlace();
        return new double[]{pl.lat, pl.lon, 0};
    }

    /** Shimoldan soat yo'nalishida Qiblaga burchak (0..360) */
    public static double bearing(double lat, double lon) {
        double p1 = Math.toRadians(lat), p2 = Math.toRadians(KAABA_LAT);
        double dl = Math.toRadians(KAABA_LON - lon);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public static double distanceKm(double lat, double lon) {
        double r = 6371.0;
        double dLat = Math.toRadians(KAABA_LAT - lat), dLon = Math.toRadians(KAABA_LON - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(KAABA_LAT)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double qiblaBearing() {
        double[] l = location();
        return bearing(l[0], l[1]);
    }

    public static String directionName(double deg) {
        String[] n = {MgLang.t("Shimol"), MgLang.t("Shimoli-sharq"), MgLang.t("Sharq"), MgLang.t("Janubi-sharq"),
                MgLang.t("Janub"), MgLang.t("Janubi-g'arb"), MgLang.t("G'arb"), MgLang.t("Shimoli-g'arb")};
        return n[(int) Math.round(((deg % 360) + 360) % 360 / 45.0) % 8];
    }

    /** Qisqa matn: "Qibla 240° · Janubi-g'arb" */
    public static String shortText() {
        double b = qiblaBearing();
        return MgLang.t("Qibla ") + Math.round(b) + "° · " + directionName(b);
    }

    private static float wrap(float a) {
        a %= 360f;
        if (a > 180f) {
            a -= 360f;
        } else if (a < -180f) {
            a += 360f;
        }
        return a;
    }

    // ------------------------------------------------------------------ kompas sensori

    /** Telefonning haqiqiy shimolga nisbatan yo'nalishi (magnit og'ishi tuzatilgan) */
    public static class Heading implements SensorEventListener {
        public interface Listener {
            void onHeading(float degrees, int accuracy);
        }

        private final SensorManager sm;
        private final Listener listener;
        private final float[] rot = new float[9];
        private final float[] rot2 = new float[9];
        private final float[] orient = new float[3];
        private final float[] grav = new float[3];
        private final float[] geo = new float[3];
        private boolean hasGrav, hasGeo, useVector;
        private float sinF, cosF = 1;
        private boolean first = true;
        private int accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
        private final float declination;

        public Heading(Context ctx, Listener l) {
            sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            listener = l;
            double[] loc = location();
            float dec = 0;
            try {
                GeomagneticField f = new GeomagneticField((float) loc[0], (float) loc[1], 400f, System.currentTimeMillis());
                dec = f.getDeclination();
            } catch (Throwable ignore) {
            }
            declination = dec;
        }

        public boolean isAvailable() {
            return sm != null && (sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
                    || sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null && sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
        }

        public void start() {
            if (sm == null) {
                return;
            }
            Sensor rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            Sensor mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (rv != null) {
                useVector = true;
                sm.registerListener(this, rv, SensorManager.SENSOR_DELAY_GAME);
                if (mag != null) {
                    // faqat aniqlik holatini bilish uchun
                    sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI);
                }
            } else {
                Sensor acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (acc != null && mag != null) {
                    sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
                    sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);
                }
            }
        }

        public void stop() {
            try {
                if (sm != null) {
                    sm.unregisterListener(this);
                }
            } catch (Throwable ignore) {
            }
        }

        private int displayRotation() {
            try {
                WindowManager wm = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Context.WINDOW_SERVICE);
                return wm.getDefaultDisplay().getRotation();
            } catch (Throwable e) {
                return Surface.ROTATION_0;
            }
        }

        @Override
        public void onSensorChanged(SensorEvent e) {
            int type = e.sensor.getType();
            boolean ready = false;
            if (type == Sensor.TYPE_ROTATION_VECTOR) {
                try {
                    SensorManager.getRotationMatrixFromVector(rot, e.values);
                    ready = true;
                } catch (Throwable ignore) {
                }
            } else if (!useVector) {
                if (type == Sensor.TYPE_ACCELEROMETER) {
                    lowPass(e.values, grav, hasGrav ? 0.2f : 1f);
                    hasGrav = true;
                } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
                    lowPass(e.values, geo, hasGeo ? 0.2f : 1f);
                    hasGeo = true;
                }
                ready = hasGrav && hasGeo && SensorManager.getRotationMatrix(rot, null, grav, geo);
            }
            if (!ready) {
                return;
            }
            float[] m = rot;
            switch (displayRotation()) {
                case Surface.ROTATION_90:
                    SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rot2);
                    m = rot2;
                    break;
                case Surface.ROTATION_180:
                    SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, rot2);
                    m = rot2;
                    break;
                case Surface.ROTATION_270:
                    SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, rot2);
                    m = rot2;
                    break;
            }
            SensorManager.getOrientation(m, orient);
            double az = Math.toDegrees(orient[0]) + declination;
            // burchakni sin/cos orqali silliqlash (359° ↔ 0° sakrashsiz)
            float s = (float) Math.sin(Math.toRadians(az)), c = (float) Math.cos(Math.toRadians(az));
            float k = first ? 1f : 0.18f;
            first = false;
            sinF += (s - sinF) * k;
            cosF += (c - cosF) * k;
            float deg = (float) ((Math.toDegrees(Math.atan2(sinF, cosF)) + 360) % 360);
            if (listener != null) {
                listener.onHeading(deg, accuracy);
            }
        }

        private static void lowPass(float[] in, float[] out, float k) {
            for (int i = 0; i < 3; i++) {
                out[i] += (in[i] - out[i]) * k;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                accuracy = acc;
            }
        }
    }

    // ------------------------------------------------------------------ kichik jonli ko'rsatkich

    /** Namoz paneli uchun kichik kompas: o'q doim Qibla tomonni ko'rsatadi */
    public static class MiniView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path arrow = new Path();
        private Heading heading;
        private float target = Float.NaN, shown;
        private final float qibla;

        public MiniView(Context ctx) {
            super(ctx);
            qibla = (float) qiblaBearing();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            heading = new Heading(getContext(), (deg, acc) -> {
                target = wrap(qibla - deg);
                invalidate();
            });
            heading.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (heading != null) {
                heading.stop();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f - AndroidUtilities.dp(2);
            boolean live = !Float.isNaN(target);
            float t = live ? target : qibla;
            shown += wrap(t - shown) * 0.25f;
            if (Math.abs(wrap(t - shown)) > 0.3f) {
                invalidate();
            }
            boolean aligned = live && Math.abs(wrap(shown)) < 4f;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx, cy, r, aligned ? 0xFF1E9E5A : 0xFF12325E, aligned ? 0xFF0B5E34 : 0xFF081A33, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.5f));
            paint.setColor(0xFFD4AF37);
            canvas.drawCircle(cx, cy, r, paint);
            canvas.save();
            canvas.rotate(shown, cx, cy);
            arrow.reset();
            arrow.moveTo(cx, cy - r * 0.78f);
            arrow.lineTo(cx + r * 0.26f, cy + r * 0.12f);
            arrow.lineTo(cx, cy - r * 0.05f);
            arrow.lineTo(cx - r * 0.26f, cy + r * 0.12f);
            arrow.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFF5D76E);
            canvas.drawPath(arrow, paint);
            paint.setColor(0xFF111111);
            float ks = r * 0.22f;
            canvas.drawRect(cx - ks / 2, cy + r * 0.3f, cx + ks / 2, cy + r * 0.3f + ks, paint);
            paint.setColor(0xFFD4AF37);
            canvas.drawRect(cx - ks / 2, cy + r * 0.3f + ks * 0.25f, cx + ks / 2, cy + r * 0.3f + ks * 0.38f, paint);
            canvas.restore();
        }
    }

    // ------------------------------------------------------------------ katta kompas

    public static class CompassView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private int style;
        private float qibla;
        private float target;
        private float dial, velocity;
        private boolean hasHeading;
        private long lastFrame;
        private final long born = SystemClock.elapsedRealtime();
        private float glow;
        private boolean aligned;
        private Runnable onAligned;
        private final float[] stars;

        public CompassView(Context ctx) {
            super(ctx);
            style = getStyle();
            qibla = (float) qiblaBearing();
            text.setTextAlign(Paint.Align.CENTER);
            Random rnd = new Random(7);
            stars = new float[3 * 70];
            for (int i = 0; i < 70; i++) {
                stars[i * 3] = rnd.nextFloat();
                stars[i * 3 + 1] = rnd.nextFloat();
                stars[i * 3 + 2] = rnd.nextFloat() * 6.28f;
            }
        }

        public void setStyle(int s) {
            style = s;
            invalidate();
        }

        public void setQibla(float q) {
            qibla = q;
            invalidate();
        }

        public void setOnAligned(Runnable r) {
            onAligned = r;
        }

        public void setHeading(float deg) {
            if (!hasHeading) {
                dial = -deg;
            }
            hasHeading = true;
            target = -deg;
            invalidate();
        }

        public boolean isAligned() {
            return aligned;
        }

        /** Qibla telefon tepasiga nisbatan necha gradus (manfiy — chapda) */
        public float offset() {
            return wrap(qibla + dial);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            long now = SystemClock.elapsedRealtime();
            float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
            lastFrame = now;
            // prujinali silliq aylanish (kritik so'nish)
            float k = 60f, damp = 2f * (float) Math.sqrt(k) * 0.95f;
            float diff = wrap(target - dial);
            velocity += (diff * k - velocity * damp) * dt;
            dial += velocity * dt;
            dial = wrap(dial);

            float appear = Math.min(1f, (now - born) / 700f);
            float ease = 1f - (float) Math.pow(1f - appear, 3);
            float over = appear < 1f ? (float) Math.sin(appear * Math.PI) * 0.06f : 0f;

            boolean nowAligned = hasHeading && Math.abs(offset()) < 3f;
            if (nowAligned && !aligned && onAligned != null) {
                onAligned.run();
            }
            aligned = nowAligned;
            glow += ((aligned ? 1f : 0f) - glow) * Math.min(1f, dt * 6f);

            float w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h * 0.42f;
            float R = Math.min(w, h * 0.8f) * 0.40f;
            float t = (now - born) / 1000f;

            drawBackground(canvas, w, h, cx, cy, R, t);

            canvas.save();
            float scale = 0.7f + 0.3f * ease + over;
            canvas.scale(scale, scale, cx, cy);
            canvas.rotate(dial + (1f - ease) * 200f, cx, cy);
            drawDial(canvas, cx, cy, R, t);
            drawQiblaMarker(canvas, cx, cy, R, t);
            canvas.restore();

            drawTopPointer(canvas, cx, cy, R);
            drawCenter(canvas, cx, cy, R);
            postInvalidateOnAnimation();
        }

        private int accent() {
            switch (style) {
                case STYLE_EMERALD:
                    return 0xFFE9F5DB;
                case STYLE_NIGHT:
                    return 0xFF64E3FF;
                default:
                    return 0xFFD4AF37;
            }
        }

        private void drawBackground(Canvas c, float w, float h, float cx, float cy, float R, float t) {
            int top, bottom;
            switch (style) {
                case STYLE_EMERALD:
                    top = 0xFF0E5A3A;
                    bottom = 0xFF03261A;
                    break;
                case STYLE_NIGHT:
                    top = 0xFF05070C;
                    bottom = 0xFF000000;
                    break;
                default:
                    top = 0xFF0D2248;
                    bottom = 0xFF040A17;
                    break;
            }
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            if (style != STYLE_EMERALD) {
                // miltillovchi yulduzlar
                for (int i = 0; i < stars.length / 3; i++) {
                    float a = 0.25f + 0.75f * (0.5f + 0.5f * (float) Math.sin(t * 1.6f + stars[i * 3 + 2]));
                    p.setColor(Color.argb((int) (a * (style == STYLE_NIGHT ? 120 : 170)), 255, 255, 255));
                    c.drawCircle(stars[i * 3] * w, stars[i * 3 + 1] * h, AndroidUtilities.dp(i % 5 == 0 ? 1.6f : 1f), p);
                }
            } else {
                // yengil islomiy naqsh fon
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(AndroidUtilities.dp(1));
                p.setColor(0x14FFFFFF);
                float step = AndroidUtilities.dp(56);
                for (float y = -step; y < h + step; y += step) {
                    for (float x = -step; x < w + step; x += step) {
                        drawStar8(c, x + ((int) (y / step) % 2 == 0 ? 0 : step / 2), y, step * 0.32f);
                    }
                }
                p.setStyle(Paint.Style.FILL);
            }
            // aniq yo'nalishda — nurli halqa
            if (glow > 0.01f) {
                int col = style == STYLE_NIGHT ? 0x64E3FF : style == STYLE_EMERALD ? 0x9CFFB0 : 0xFFD76E;
                float pulse = 1f + 0.05f * (float) Math.sin(t * 5f);
                p.setShader(new RadialGradient(cx, cy, R * 1.45f * pulse, new int[]{((int) (glow * 150) << 24) | col, 0x00000000}, null, Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, R * 1.45f * pulse, p);
                p.setShader(null);
            }
        }

        private void drawStar8(Canvas c, float x, float y, float r) {
            c.drawRect(x - r, y - r, x + r, y + r, p);
            c.save();
            c.rotate(45, x, y);
            c.drawRect(x - r, y - r, x + r, y + r, p);
            c.restore();
        }

        private void drawDial(Canvas c, float cx, float cy, float R, float t) {
            int acc = accent();
            // halqalar
            p.setStyle(Paint.Style.FILL);
            if (style == STYLE_GOLD) {
                p.setShader(new RadialGradient(cx, cy, R, 0xFF16305E, 0xFF0A1830, Shader.TileMode.CLAMP));
            } else if (style == STYLE_EMERALD) {
                p.setShader(new RadialGradient(cx, cy, R, 0xFF137049, 0xFF0A3F29, Shader.TileMode.CLAMP));
            } else {
                p.setShader(new RadialGradient(cx, cy, R, 0xFF0B1116, 0xFF020304, Shader.TileMode.CLAMP));
            }
            c.drawCircle(cx, cy, R, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(acc);
            p.setStrokeWidth(AndroidUtilities.dp(style == STYLE_NIGHT ? 1.2f : 3f));
            c.drawCircle(cx, cy, R, p);
            p.setStrokeWidth(AndroidUtilities.dp(1));
            p.setColor(multAlpha(acc, 0.5f));
            c.drawCircle(cx, cy, R * 0.8f, p);
            if (style == STYLE_EMERALD) {
                p.setColor(multAlpha(acc, 0.35f));
                drawStar8(c, cx, cy, R * 0.42f);
                drawStar8(c, cx, cy, R * 0.30f);
            }
            // chiziqlar (har 5°) va raqamlar (har 30°)
            text.setColor(multAlpha(acc, 0.9f));
            text.setTextSize(AndroidUtilities.dp(11));
            text.setTypeface(Typeface.DEFAULT);
            for (int d = 0; d < 360; d += 5) {
                boolean major = d % 30 == 0;
                float len = major ? R * 0.1f : R * 0.05f;
                p.setStrokeWidth(AndroidUtilities.dp(major ? 2f : 1f));
                p.setColor(multAlpha(acc, major ? 0.95f : 0.5f));
                double a = Math.toRadians(d);
                float sx = cx + (float) Math.sin(a) * R, sy = cy - (float) Math.cos(a) * R;
                float ex = cx + (float) Math.sin(a) * (R - len), ey = cy - (float) Math.cos(a) * (R - len);
                c.drawLine(sx, sy, ex, ey, p);
                if (major && d % 90 != 0) {
                    float tx = cx + (float) Math.sin(a) * (R * 0.8f + AndroidUtilities.dp(10)), ty = cy - (float) Math.cos(a) * (R * 0.8f + AndroidUtilities.dp(10));
                    c.drawText(String.valueOf(d), tx, ty + AndroidUtilities.dp(4), text);
                }
            }
            // dunyo tomonlari
            String[] names = {MgLang.t("Sh"), MgLang.t("Shq"), MgLang.t("J"), MgLang.t("G'")};
            text.setTypeface(AndroidUtilities.bold());
            text.setTextSize(AndroidUtilities.dp(17));
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 90);
                float tx = cx + (float) Math.sin(a) * (R * 0.8f + AndroidUtilities.dp(12)), ty = cy - (float) Math.cos(a) * (R * 0.8f + AndroidUtilities.dp(12));
                text.setColor(i == 0 ? 0xFFFF5A5A : acc);
                c.save();
                c.rotate(i * 90, tx, ty);
                c.drawText(names[i], tx, ty + AndroidUtilities.dp(6), text);
                c.restore();
            }
        }

        private void drawQiblaMarker(Canvas c, float cx, float cy, float R, float t) {
            c.save();
            c.rotate(qibla, cx, cy);
            int acc = accent();
            // o'q
            path.reset();
            path.moveTo(cx, cy - R * 0.62f);
            path.lineTo(cx + R * 0.07f, cy - R * 0.1f);
            path.lineTo(cx, cy - R * 0.16f);
            path.lineTo(cx - R * 0.07f, cy - R * 0.1f);
            path.close();
            p.setStyle(Paint.Style.FILL);
            if (style == STYLE_NIGHT) {
                p.setShader(new LinearGradient(cx, cy - R * 0.62f, cx, cy, 0xFF9CF0FF, 0xFF1A7FA0, Shader.TileMode.CLAMP));
            } else if (style == STYLE_EMERALD) {
                p.setShader(new LinearGradient(cx, cy - R * 0.62f, cx, cy, 0xFFFFFFFF, 0xFF7ED9A4, Shader.TileMode.CLAMP));
            } else {
                p.setShader(new LinearGradient(cx, cy - R * 0.62f, cx, cy, 0xFFFFE9A3, 0xFFB8871F, Shader.TileMode.CLAMP));
            }
            c.drawPath(path, p);
            p.setShader(null);
            // Ka'ba belgisi — yengil "suzish" animatsiyasi
            float bob = (float) Math.sin(t * 2.2f) * AndroidUtilities.dp(1.5f);
            float ks = R * 0.17f;
            float ky = cy - R * 0.62f - ks * 1.25f + bob;
            if (glow > 0.01f) {
                p.setShader(new RadialGradient(cx, ky + ks / 2, ks * 1.6f, new int[]{((int) (glow * 200) << 24) | 0xFFE08A, 0}, null, Shader.TileMode.CLAMP));
                c.drawCircle(cx, ky + ks / 2, ks * 1.6f, p);
                p.setShader(null);
            }
            c.save();
            c.rotate(-qibla - dial, cx, ky + ks / 2); // belgi doim tik turadi
            drawKaaba(c, cx, ky + ks / 2, ks);
            c.restore();
            c.restore();
        }

        private void drawKaaba(Canvas c, float x, float y, float s) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF121212);
            rect.set(x - s / 2, y - s / 2, x + s / 2, y + s / 2);
            c.drawRoundRect(rect, s * 0.08f, s * 0.08f, p);
            p.setColor(0xFFD4AF37);
            c.drawRect(x - s / 2, y - s * 0.22f, x + s / 2, y - s * 0.1f, p);
            rect.set(x + s * 0.08f, y + s * 0.05f, x + s * 0.3f, y + s / 2);
            c.drawRect(rect, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(AndroidUtilities.dp(1));
            p.setColor(0x66FFFFFF);
            rect.set(x - s / 2, y - s / 2, x + s / 2, y + s / 2);
            c.drawRoundRect(rect, s * 0.08f, s * 0.08f, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawTopPointer(Canvas c, float cx, float cy, float R) {
            float y = cy - R - AndroidUtilities.dp(14);
            path.reset();
            path.moveTo(cx, y + AndroidUtilities.dp(14));
            path.lineTo(cx - AndroidUtilities.dp(10), y - AndroidUtilities.dp(4));
            path.lineTo(cx + AndroidUtilities.dp(10), y - AndroidUtilities.dp(4));
            path.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(aligned ? 0xFF3CE08A : 0xFFFFFFFF);
            c.drawPath(path, p);
        }

        private void drawCenter(Canvas c, float cx, float cy, float R) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(accent());
            c.drawCircle(cx, cy, AndroidUtilities.dp(7), p);
            p.setColor(0xFF000000);
            c.drawCircle(cx, cy, AndroidUtilities.dp(3), p);
        }

        private static int multAlpha(int color, float a) {
            return Color.argb((int) (Color.alpha(color) * a), Color.red(color), Color.green(color), Color.blue(color));
        }
    }

    // ------------------------------------------------------------------ oyna

    public static void open(BaseFragment f) {
        Activity act = f == null ? null : f.getParentActivity();
        if (act == null) {
            return;
        }
        Dialog dialog = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(act);
        CompassView compass = new CompassView(act);
        root.addView(compass, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout bottom = new LinearLayout(act);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(bottom, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 16, 0, 16, 24));

        TextView status = new TextView(act);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        status.setTypeface(AndroidUtilities.bold());
        status.setTextColor(0xFFFFFFFF);
        status.setGravity(Gravity.CENTER);
        bottom.addView(status, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView info = new TextView(act);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        info.setTextColor(0xCCFFFFFF);
        info.setGravity(Gravity.CENTER);
        info.setLineSpacing(AndroidUtilities.dp(2), 1f);
        bottom.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        LinearLayout chips = new LinearLayout(act);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER);
        bottom.addView(chips, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        String[] names = styleNames();
        ArrayList<TextView> chipViews = new ArrayList<>();
        Runnable paintChips = () -> {
            for (int i = 0; i < chipViews.size(); i++) {
                boolean sel = i == getStyle();
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(AndroidUtilities.dp(18));
                bg.setColor(sel ? 0x40FFFFFF : 0x14FFFFFF);
                bg.setStroke(AndroidUtilities.dp(1), sel ? 0xFFFFFFFF : 0x40FFFFFF);
                chipViews.get(i).setBackground(bg);
            }
        };
        for (int i = 0; i < names.length; i++) {
            TextView chip = new TextView(act);
            chip.setText(names[i]);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
            final int idx = i;
            chip.setOnClickListener(v -> {
                setStyle(idx);
                compass.setStyle(idx);
                paintChips.run();
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            });
            chipViews.add(chip);
            chips.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 0));
        }
        paintChips.run();

        TextView gps = new TextView(act);
        gps.setTextColor(0xFFFFFFFF);
        gps.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        gps.setGravity(Gravity.CENTER);
        gps.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(12));
        GradientDrawable gbg = new GradientDrawable();
        gbg.setCornerRadius(AndroidUtilities.dp(24));
        gbg.setColor(0x26FFFFFF);
        gps.setBackground(gbg);
        bottom.addView(gps, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView close = new TextView(act);
        close.setText("✕");
        close.setTextColor(0xFFFFFFFF);
        close.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close, LayoutHelper.createFrame(48, 48, Gravity.TOP | Gravity.RIGHT, 0, 28, 8, 0));

        TextView title = new TextView(act);
        title.setText(MgLang.t("🕋 Qibla"));
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        root.addView(title, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 48, Gravity.TOP | Gravity.LEFT, 20, 28, 0, 0));
        title.setGravity(Gravity.CENTER_VERTICAL);

        final int[] lastAcc = {SensorManager.SENSOR_STATUS_ACCURACY_HIGH};
        final boolean[] gotHeading = {false};
        Runnable fillInfo = () -> {
            double[] l = location();
            double b = bearing(l[0], l[1]);
            compass.setQibla((float) b);
            String where = l[2] > 0 ? MgLang.t("📍 GPS bo'yicha") : "📍 " + MgLang.t(MgPrayer.getPlace().name);
            StringBuilder sb = new StringBuilder();
            sb.append(MgLang.t("Qibla: ")).append(Math.round(b)).append("° ").append(directionName(b))
                    .append(" · ").append(MgLang.t("Makkagacha ")).append(String.format(Locale.US, "%,d", Math.round(distanceKm(l[0], l[1]))).replace(',', ' ')).append(MgLang.t(" km"))
                    .append("\n").append(where);
            if (gotHeading[0] && lastAcc[0] <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                sb.append("\n").append(MgLang.t("⚠️ Kompas aniqligi past: telefonni havoda ∞ shaklida 2–3 marta aylantiring va temir buyumlardan uzoqlashing."));
            }
            info.setText(sb);
            gps.setText(isGpsEnabled() ? MgLang.t("📡 GPS: yoqilgan (yangilash)") : MgLang.t("📡 GPS bilan aniqroq aniqlash"));
        };
        Runnable fillStatus = () -> {
            if (!gotHeading[0]) {
                status.setText(MgLang.t("Telefonni tekis ushlang…"));
                return;
            }
            float off = compass.offset();
            if (compass.isAligned()) {
                setIfChanged(status, MgLang.t("✅ Qibla aynan oldingizda"));
                status.setTextColor(0xFF7CF5AE);
            } else {
                setIfChanged(status, off > 0 ? MgLang.t("O'ngga buriling ") + Math.round(Math.abs(off)) + "° ➜" : "⬅ " + MgLang.t("Chapga buriling ") + Math.round(Math.abs(off)) + "°");
                status.setTextColor(0xFFFFFFFF);
            }
        };

        Heading heading = new Heading(act, (deg, acc) -> {
            boolean accChanged = acc != lastAcc[0];
            lastAcc[0] = acc;
            boolean first = !gotHeading[0];
            gotHeading[0] = true;
            compass.setHeading(deg);
            fillStatus.run();
            if (accChanged || first) {
                fillInfo.run();
            }
        });
        compass.setOnAligned(() -> {
            try {
                compass.performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            } catch (Throwable ignore) {
            }
        });
        if (!heading.isAvailable()) {
            status.setText(MgLang.t("Telefoningizda kompas sensori yo'q"));
            info.setText(MgLang.t("Qibla shimoldan soat mili bo'yicha ") + Math.round(qiblaBearing()) + "° (" + directionName(qiblaBearing()) + MgLang.t("). Quyosh yoki xarita yordamida yo'nalishni toping."));
        } else {
            fillInfo.run();
            fillStatus.run();
        }
        gps.setOnClickListener(v -> requestGps(act, ok -> {
            if (ok) {
                MgConfig.setBool("qibla_use_gps", true);
            }
            fillInfo.run();
        }, status));

        dialog.setContentView(root);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        dialog.setOnShowListener(d -> heading.start());
        dialog.setOnDismissListener(d -> heading.stop());
        dialog.show();
    }

    // ------------------------------------------------------------------ GPS

    @SuppressLint("MissingPermission")
    public static void requestGps(Activity act, Utilities.Callback<Boolean> done, TextView status) {
        if (Build.VERSION.SDK_INT >= 23 && !hasLocationPermission(act)) {
            act.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 7731);
            // ruxsat berilishini kutamiz (natija LaunchActivity'ga keladi)
            final int[] tries = {0};
            Runnable poll = new Runnable() {
                @Override
                public void run() {
                    if (hasLocationPermission(act)) {
                        requestGps(act, done, status);
                    } else if (++tries[0] < 60) {
                        AndroidUtilities.runOnUIThread(this, 500);
                    }
                }
            };
            AndroidUtilities.runOnUIThread(poll, 800);
            return;
        }
        LocationManager lm = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            done.run(false);
            return;
        }
        Location best = null;
        try {
            for (String prov : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(prov);
                if (l != null && (best == null || l.getTime() > best.getTime())) {
                    best = l;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 15 * 60_000L) {
            save(best);
            done.run(true);
            return;
        }
        if (status != null) {
            status.setText(MgLang.t("📡 Joylashuv aniqlanmoqda…"));
        }
        final Location fallback = best;
        final boolean[] finished = {false};
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (finished[0]) {
                    return;
                }
                finished[0] = true;
                try {
                    lm.removeUpdates(this);
                } catch (Throwable ignore) {
                }
                save(location);
                done.run(true);
            }

            @Override
            public void onStatusChanged(String provider, int st, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        try {
            boolean any = false;
            for (String prov : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                if (lm.isProviderEnabled(prov)) {
                    lm.requestLocationUpdates(prov, 0, 0, listener, Looper.getMainLooper());
                    any = true;
                }
            }
            if (!any) {
                finished[0] = true;
                if (fallback != null) {
                    save(fallback);
                }
                if (status != null) {
                    status.setText(MgLang.t("Joylashuv (GPS) o'chiq — telefon sozlamalaridan yoqing"));
                }
                done.run(fallback != null);
                return;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (!finished[0]) {
                finished[0] = true;
                try {
                    lm.removeUpdates(listener);
                } catch (Throwable ignore) {
                }
                if (fallback != null) {
                    save(fallback);
                }
                done.run(fallback != null);
            }
        }, 25_000);
    }

    private static boolean hasLocationPermission(Activity act) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return act.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || act.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static void setIfChanged(TextView tv, CharSequence t) {
        if (!android.text.TextUtils.equals(tv.getText(), t)) {
            tv.setText(t);
        }
    }

    private static void save(Location l) {
        MgConfig.setString("qibla_gps", String.format(Locale.US, "%.5f,%.5f,%d", l.getLatitude(), l.getLongitude(), System.currentTimeMillis()));
    }
}
