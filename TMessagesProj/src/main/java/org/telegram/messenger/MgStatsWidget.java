/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.widget.RemoteViews;

import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Bosh ekran vidjeti: kuzatilayotgan kanalning obunachilari soni, 24 soat va 7 kunlik o'sish,
 * hamda kichik grafik. "Keyingi" tugmasi bilan kanallar almashtiriladi.
 */
public class MgStatsWidget extends AppWidgetProvider {

    private static final String ACTION_NEXT = "org.milliygram.statswidget.NEXT";
    private static final String LIST_KEY = "gw_list";

    // ---- kuzatilayotgan kanallar ro'yxati ("akkaunt:chatId") ----

    static ArrayList<long[]> entries() {
        ArrayList<long[]> out = new ArrayList<>();
        String raw = MgConfig.getString(LIST_KEY, "");
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String e : raw.split(",")) {
            int i = e.indexOf(':');
            if (i <= 0) {
                continue;
            }
            try {
                long acc = Long.parseLong(e.substring(0, i));
                long chat = Long.parseLong(e.substring(i + 1));
                if (!MgGrowth.points((int) acc, chat).isEmpty()) {
                    out.add(new long[]{acc, chat});
                }
            } catch (Throwable ignore) {
            }
        }
        return out;
    }

    static void remember(int account, long chatId) {
        String item = account + ":" + chatId;
        String raw = MgConfig.getString(LIST_KEY, "");
        if (raw == null) {
            raw = "";
        }
        for (String e : raw.split(",")) {
            if (e.equals(item)) {
                return;
            }
        }
        MgConfig.setString(LIST_KEY, raw.isEmpty() ? item : raw + "," + item);
    }

    public static void updateAll(Context ctx) {
        if (ctx == null) {
            return;
        }
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, MgStatsWidget.class));
            if (ids != null && ids.length > 0) {
                for (int id : ids) {
                    update(ctx, mgr, id);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            update(context, mgr, id);
        }
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            MgConfig.setInt("gw_pos_" + id, 0);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_NEXT.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                MgConfig.setInt("gw_pos_" + id, MgConfig.getInt("gw_pos_" + id, 0) + 1);
                update(context, AppWidgetManager.getInstance(context), id);
            }
        }
    }

    private static String fmt(int n) {
        return String.format(Locale.US, "%,d", n).replace(',', ' ');
    }

    private static String delta(int d) {
        return (d > 0 ? "+" : d < 0 ? "−" : "±") + fmt(Math.abs(d));
    }

    /** Berilgan vaqtdan oldingi eng yaqin nuqtaga nisbatan o'zgarish (ma'lumot yetmasa — eng eskisiga) */
    private static int change(ArrayList<MgGrowth.Point> pts, int sinceTime) {
        MgGrowth.Point last = pts.get(pts.size() - 1);
        MgGrowth.Point base = pts.get(0);
        for (int i = pts.size() - 1; i >= 0; i--) {
            if (pts.get(i).time <= sinceTime) {
                base = pts.get(i);
                break;
            }
        }
        return last.count - base.count;
    }

    private static void update(Context ctx, AppWidgetManager mgr, int widgetId) {
        try {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.mg_stats_widget);
            ArrayList<long[]> list = entries();

            Intent open = new Intent(ctx, LaunchActivity.class);
            open.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
            open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            open.addCategory(Intent.CATEGORY_LAUNCHER);

            if (list.isEmpty()) {
                rv.setTextViewText(R.id.mg_sw_title, MgLang.t("Kanal statistikasi"));
                rv.setTextViewText(R.id.mg_sw_count, "—");
                rv.setTextViewText(R.id.mg_sw_day, MgLang.t("Kanal ma'lumotida \"O'sish statistikasi\"ni yoqing"));
                rv.setTextViewText(R.id.mg_sw_week, "");
                rv.setViewVisibility(R.id.mg_sw_graph, View.GONE);
                rv.setViewVisibility(R.id.mg_sw_next, View.GONE);
            } else {
                int pos = Math.abs(MgConfig.getInt("gw_pos_" + widgetId, 0)) % list.size();
                int account = (int) list.get(pos)[0];
                long chatId = list.get(pos)[1];
                ArrayList<MgGrowth.Point> pts = MgGrowth.points(account, chatId);
                MgGrowth.Point last = pts.get(pts.size() - 1);
                String name = MgConfig.getString("gr_name_" + account + "_" + chatId, MgLang.t("Kanal"));
                rv.setTextViewText(R.id.mg_sw_title, name);
                rv.setTextViewText(R.id.mg_sw_count, fmt(last.count) + " " + MgLang.t("obunachi"));
                int d1 = change(pts, last.time - 86400);
                int d7 = change(pts, last.time - 7 * 86400);
                rv.setTextViewText(R.id.mg_sw_day, MgLang.t("24 soat: ") + delta(d1));
                rv.setTextColor(R.id.mg_sw_day, d1 < 0 ? 0xFFE5484D : 0xFF2EB872);
                rv.setTextViewText(R.id.mg_sw_week, MgLang.t("7 kun: ") + delta(d7));
                rv.setTextColor(R.id.mg_sw_week, d7 < 0 ? 0xFFE5484D : 0xFF2EB872);
                Bitmap graph = sparkline(pts, last.time - 7 * 86400);
                if (graph != null) {
                    rv.setImageViewBitmap(R.id.mg_sw_graph, graph);
                    rv.setViewVisibility(R.id.mg_sw_graph, View.VISIBLE);
                } else {
                    rv.setViewVisibility(R.id.mg_sw_graph, View.GONE);
                }
                rv.setViewVisibility(R.id.mg_sw_next, list.size() > 1 ? View.VISIBLE : View.GONE);
                open.putExtra("chatId", chatId);
                open.putExtra("currentAccount", account);

                Intent next = new Intent(ctx, MgStatsWidget.class).setAction(ACTION_NEXT);
                next.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                rv.setOnClickPendingIntent(R.id.mg_sw_next, PendingIntent.getBroadcast(ctx, widgetId, next,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
            rv.setOnClickPendingIntent(R.id.mg_sw_root, PendingIntent.getActivity(ctx, 0x4D5300 + widgetId, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            mgr.updateAppWidget(widgetId, rv);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /** Oxirgi 7 kunlik obunachilar grafigi */
    private static Bitmap sparkline(ArrayList<MgGrowth.Point> all, int since) {
        ArrayList<MgGrowth.Point> pts = new ArrayList<>();
        for (MgGrowth.Point p : all) {
            if (p.time >= since) {
                pts.add(p);
            }
        }
        if (pts.size() < 2) {
            if (all.size() < 2) {
                return null;
            }
            pts = new ArrayList<>(all.subList(Math.max(0, all.size() - 30), all.size()));
        }
        final int w = 600, h = 140, pad = 8;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (MgGrowth.Point p : pts) {
            min = Math.min(min, p.count);
            max = Math.max(max, p.count);
        }
        if (max == min) {
            max = min + 1;
        }
        float t0 = pts.get(0).time, t1 = Math.max(t0 + 1, pts.get(pts.size() - 1).time);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Path line = new Path();
        for (int i = 0; i < pts.size(); i++) {
            MgGrowth.Point p = pts.get(i);
            float x = pad + (w - 2 * pad) * (p.time - t0) / (t1 - t0);
            float y = h - pad - (h - 2 * pad) * (p.count - min) / (float) (max - min);
            if (i == 0) {
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
        }
        final int accent = 0xFF3B82F6;
        Path fill = new Path(line);
        fill.lineTo(w - pad, h);
        fill.lineTo(pad, h);
        fill.close();
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setShader(new LinearGradient(0, 0, 0, h, 0x553B82F6, 0x003B82F6, Shader.TileMode.CLAMP));
        c.drawPath(fill, fp);
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setStyle(Paint.Style.STROKE);
        lp.setStrokeWidth(5f);
        lp.setStrokeJoin(Paint.Join.ROUND);
        lp.setStrokeCap(Paint.Cap.ROUND);
        lp.setColor(accent);
        c.drawPath(line, lp);
        return bmp;
    }
}
