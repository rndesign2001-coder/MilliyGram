/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgGrowth;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Obunachilar kundaligi: grafik, kunlik o'zgarishlar va keskin sakrashlar (nakrutka / reklama kunlari).
 */
public class MgGrowthActivity extends UniversalFragment {

    private static final int ID_REFRESH = 1;
    private static final int ID_TRACK = 2;
    private static final int ID_CLEAR = 3;

    private final long chatId;

    public MgGrowthActivity(long chatId) {
        super();
        this.chatId = chatId;
    }

    @Override
    public boolean onFragmentCreate() {
        if (!MgGrowth.isTracked(currentAccount, chatId)) {
            MgGrowth.setTracked(currentAccount, chatId, true);
        }
        MgGrowth.fetch(currentAccount, chatId, true, this::refresh);
        return super.onFragmentCreate();
    }

    private void refresh() {
        if (listView != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected CharSequence getTitle() {
        TLRPC.Chat c = getMessagesController().getChat(chatId);
        return c != null ? c.title : org.telegram.messenger.MgLang.t("Obunachilar kundaligi");
    }

    private static class Day {
        int time, count, delta;
        boolean first;
    }

    /** Har kunning oxirgi qiymati */
    private static ArrayList<Day> days(ArrayList<MgGrowth.Point> pts) {
        ArrayList<Day> out = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        int lastKey = -1;
        for (MgGrowth.Point p : pts) {
            c.setTimeInMillis(p.time * 1000L);
            int key = c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
            if (key == lastKey && !out.isEmpty()) {
                Day d = out.get(out.size() - 1);
                d.count = p.count;
                d.time = p.time;
            } else {
                Day d = new Day();
                d.time = p.time;
                d.count = p.count;
                out.add(d);
                lastKey = key;
            }
        }
        for (int i = 0; i < out.size(); i++) {
            out.get(i).first = i == 0;
            out.get(i).delta = i == 0 ? 0 : out.get(i).count - out.get(i - 1).count;
        }
        return out;
    }

    private static int changeSince(ArrayList<MgGrowth.Point> pts, int seconds) {
        if (pts.size() < 2) {
            return 0;
        }
        MgGrowth.Point last = pts.get(pts.size() - 1);
        MgGrowth.Point ref = pts.get(0);
        for (int i = pts.size() - 1; i >= 0; i--) {
            if (last.time - pts.get(i).time >= seconds) {
                ref = pts.get(i);
                break;
            }
        }
        return last.count - ref.count;
    }

    private static String signed(int v) {
        return (v > 0 ? "+" : "") + String.format(Locale.US, "%,d", v).replace(',', ' ');
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context ctx = getContext();
        ArrayList<MgGrowth.Point> pts = MgGrowth.points(currentAccount, chatId);
        if (ctx == null) {
            return;
        }
        int black = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        int gray = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        int green = 0xFF2EAD5B, red = Theme.getColor(Theme.key_text_RedRegular);

        TextView head = new TextView(ctx);
        head.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        head.setTextColor(black);
        head.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(14), AndroidUtilities.dp(21), AndroidUtilities.dp(6));
        head.setLineSpacing(AndroidUtilities.dp(3), 1f);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (pts.isEmpty()) {
            sb.append(org.telegram.messenger.MgLang.t("Ma'lumot yig'ilmoqda… Birinchi yozuv hozir olinadi, grafik bir necha kundan keyin to'liq ko'rinadi."));
        } else {
            MgGrowth.Point last = pts.get(pts.size() - 1);
            int s0 = sb.length();
            sb.append(String.format(Locale.US, "%,d", last.count).replace(',', ' ')).append(org.telegram.messenger.MgLang.t(" obunachi"));
            sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.RelativeSizeSpan(1.4f), s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            int[][] per = {{86400, 0}, {7 * 86400, 0}, {30 * 86400, 0}};
            String[] names = {org.telegram.messenger.MgLang.t("24 soat"), org.telegram.messenger.MgLang.t("7 kun"), org.telegram.messenger.MgLang.t("30 kun")};
            sb.append("\n");
            for (int i = 0; i < 3; i++) {
                int ch = changeSince(pts, per[i][0]);
                sb.append(names[i]).append(": ");
                int s = sb.length();
                sb.append(signed(ch));
                sb.setSpan(new ForegroundColorSpan(ch > 0 ? green : ch < 0 ? red : gray), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (i < 2) {
                    sb.append("   ");
                }
            }
            int sg = sb.length();
            sb.append(org.telegram.messenger.MgLang.t("\nYozuvlar: ")).append(String.valueOf(pts.size())).append(org.telegram.messenger.MgLang.t(" · har ")).append(String.valueOf(MgGrowth.getIntervalHours())).append(org.telegram.messenger.MgLang.t(" soatda"));
            sb.setSpan(new ForegroundColorSpan(gray), sg, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        java.util.TreeMap<Integer, int[]> hf = MgGrowth.flows(currentAccount, chatId);
        if (!hf.isEmpty()) {
            int today = MgGrowth.dayKey(System.currentTimeMillis() / 1000);
            int[][] sums = new int[2][2];
            int[] spans = {7, 30};
            for (int k = 0; k < 2; k++) {
                int from = MgGrowth.dayKey(System.currentTimeMillis() / 1000 - (spans[k] - 1) * 86400L);
                for (java.util.Map.Entry<Integer, int[]> e : hf.entrySet()) {
                    if (e.getKey() >= from && e.getKey() <= today) {
                        sums[k][0] += e.getValue()[0];
                        sums[k][1] += e.getValue()[1];
                    }
                }
                sb.append(k == 0 ? "\n" : "   ").append(spans[k] + org.telegram.messenger.MgLang.t(" kun: "));
                int a = sb.length();
                sb.append("+").append(String.valueOf(sums[k][0]));
                sb.setSpan(new ForegroundColorSpan(green), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(" / ");
                int b2 = sb.length();
                sb.append("−").append(String.valueOf(sums[k][1]));
                sb.setSpan(new ForegroundColorSpan(red), b2, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        head.setText(sb);
        items.add(UItem.asCustom(head));
        if (pts.size() >= 2) {
            items.add(UItem.asCustom(new ChartView(ctx, pts), 220));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(ID_REFRESH, R.drawable.msg_retry, org.telegram.messenger.MgLang.t("Hozir yangilash")));
        items.add(UItem.asCheck(ID_TRACK, org.telegram.messenger.MgLang.t("Avtomatik kuzatish")).setChecked(MgGrowth.isTracked(currentAccount, chatId)));
        items.add(UItem.asButton(ID_CLEAR, R.drawable.msg_delete, org.telegram.messenger.MgLang.t("Tarixni tozalash")).red());
        items.add(UItem.asShadow(org.telegram.messenger.MgLang.t("Obunachi soni har ") + MgGrowth.getIntervalHours() + org.telegram.messenger.MgLang.t(" soatda avtomatik yoziladi (Sozlamalar → Avtomatlashtirish). Ma'lumot faqat telefoningizda saqlanadi.")));

        // ---- Kunlar bo'yicha: kirganlar / chiqqanlar ----
        java.util.TreeMap<Integer, int[]> flows = MgGrowth.flows(currentAccount, chatId);
        java.util.TreeMap<Integer, Integer> net = new java.util.TreeMap<>();
        java.util.TreeMap<Integer, Integer> totals = new java.util.TreeMap<>();
        ArrayList<Day> ds = days(pts);
        for (Day d : ds) {
            int key = MgGrowth.dayKey(d.time);
            totals.put(key, d.count);
            if (!d.first) {
                net.put(key, d.delta);
            }
        }
        java.util.TreeSet<Integer> keys = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
        keys.addAll(flows.keySet());
        keys.addAll(net.keySet());
        if (!keys.isEmpty()) {
            // keskin kunlarni aniqlash (kirganlar bo'yicha)
            double mean = 0, sq = 0;
            int n = 0;
            for (int[] v : flows.values()) {
                mean += v[0];
                n++;
            }
            mean = n > 0 ? mean / n : 0;
            for (int[] v : flows.values()) {
                sq += (v[0] - mean) * (v[0] - mean);
            }
            double std = n > 1 ? Math.sqrt(sq / (n - 1)) : 0;
            double lMean = 0, lSq = 0;
            for (int[] v : flows.values()) {
                lMean += v[1];
            }
            lMean = n > 0 ? lMean / n : 0;
            for (int[] v : flows.values()) {
                lSq += (v[1] - lMean) * (v[1] - lMean);
            }
            double lStd = n > 1 ? Math.sqrt(lSq / (n - 1)) : 0;

            TextView list = new TextView(ctx);
            list.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            list.setTextColor(black);
            list.setLineSpacing(AndroidUtilities.dp(6), 1f);
            list.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(10), AndroidUtilities.dp(21), AndroidUtilities.dp(12));
            SpannableStringBuilder lb = new SpannableStringBuilder();
            int shown = 0;
            for (Integer key : keys) {
                if (shown++ >= 90) {
                    break;
                }
                lb.append(String.format(Locale.US, "%02d.%02d.%02d", key % 100, (key / 100) % 100, (key / 10000) % 100)).append("   ");
                int[] f = flows.get(key);
                if (f != null) {
                    int s1 = lb.length();
                    lb.append("+").append(String.valueOf(f[0]));
                    lb.setSpan(new ForegroundColorSpan(green), s1, lb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    lb.append("  ");
                    int s2 = lb.length();
                    lb.append("−").append(String.valueOf(f[1]));
                    lb.setSpan(new ForegroundColorSpan(red), s2, lb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    int d = f[0] - f[1];
                    lb.append("  = ");
                    int s3 = lb.length();
                    lb.append(signed(d));
                    lb.setSpan(new ForegroundColorSpan(d > 0 ? green : d < 0 ? red : gray), s3, lb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (n >= 4 && f[0] > mean + 2.5 * std && f[0] >= 20) {
                        lb.append("  ⚡");
                    }
                    if (n >= 4 && f[1] > lMean + 2.5 * lStd && f[1] >= 20) {
                        lb.append("  📉");
                    }
                } else {
                    Integer d = net.get(key);
                    int s3 = lb.length();
                    lb.append(d == null ? "—" : signed(d));
                    lb.setSpan(new ForegroundColorSpan(d == null ? gray : d > 0 ? green : d < 0 ? red : gray), s3, lb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    int sg = lb.length();
                    lb.append(org.telegram.messenger.MgLang.t("  (sof)"));
                    lb.setSpan(new ForegroundColorSpan(gray), sg, lb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                Integer total = totals.get(key);
                if (total != null) {
                    int sg = lb.length();
                    lb.append("   (").append(String.format(Locale.US, "%,d", total).replace(',', ' ')).append(")");
                    lb.setSpan(new ForegroundColorSpan(gray), sg, lb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                lb.append("\n");
            }
            list.setText(lb);
            items.add(UItem.asHeader(org.telegram.messenger.MgLang.t("Kunlar bo'yicha: kirdi / chiqdi")));
            items.add(UItem.asCustom(list));
            String src = MgGrowth.flowSource(currentAccount, chatId);
            String note;
            if ("stats".equals(src)) {
                note = org.telegram.messenger.MgLang.t("Kirganlar va chiqqanlar Telegram'ning rasmiy kanal statistikasidan olingan. ⚡ — odatdagidan keskin ko'p kirgan kun (reklama yoki nakrutka), 📉 — keskin ko'p chiqqan kun.");
            } else if ("log".equals(src)) {
                note = org.telegram.messenger.MgLang.t("Kirganlar va chiqqanlar admin jurnalidan yig'iladi. Telegram jurnalni faqat 48 soat saqlaydi, shuning uchun ma'lumot kuzatish yoqilgan kundan boshlab to'planadi.");
            } else {
                note = org.telegram.messenger.MgLang.t("Siz bu kanalda admin emassiz, shuning uchun faqat obunachilar sonining sof o'zgarishi ko'rsatiladi (kirgan va chiqqanlarni alohida faqat adminlar ko'ra oladi).");
            }
            items.add(UItem.asShadow(note));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_REFRESH) {
            MgGrowth.fetch(currentAccount, chatId, true, () -> {
                refresh();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, org.telegram.messenger.MgLang.t("Yangilandi")).show();
            });
        } else if (item.id == ID_TRACK) {
            boolean v = !MgGrowth.isTracked(currentAccount, chatId);
            MgGrowth.setTracked(currentAccount, chatId, v);
            refresh();
        } else if (item.id == ID_CLEAR) {
            MgGrowth.clear(currentAccount, chatId);
            refresh();
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    /** Oddiy chiziqli grafik */
    private static class ChartView extends View {
        private final ArrayList<MgGrowth.Point> pts;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        ChartView(Context ctx, ArrayList<MgGrowth.Point> pts) {
            super(ctx);
            this.pts = pts;
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            line.setColor(accent);
            line.setStrokeWidth(AndroidUtilities.dp(2.2f));
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeJoin(Paint.Join.ROUND);
            grid.setColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), 0.25f));
            grid.setStrokeWidth(1);
            text.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            text.setTextSize(AndroidUtilities.dp(11));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            float l = AndroidUtilities.dp(21), r = w - AndroidUtilities.dp(21), t = AndroidUtilities.dp(16), b = h - AndroidUtilities.dp(24);
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (MgGrowth.Point p : pts) {
                min = Math.min(min, p.count);
                max = Math.max(max, p.count);
            }
            if (max == min) {
                max = min + 1;
            }
            int t0 = pts.get(0).time, t1 = pts.get(pts.size() - 1).time;
            if (t1 == t0) {
                t1 = t0 + 1;
            }
            for (int i = 0; i <= 3; i++) {
                float y = t + (b - t) * i / 3f;
                canvas.drawLine(l, y, r, y, grid);
            }
            path.reset();
            for (int i = 0; i < pts.size(); i++) {
                MgGrowth.Point p = pts.get(i);
                float x = l + (r - l) * (p.time - t0) / (float) (t1 - t0);
                float y = b - (b - t) * (p.count - min) / (float) (max - min);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            Path area = new Path(path);
            area.lineTo(r, b);
            area.lineTo(l, b);
            area.close();
            int accent = line.getColor();
            fill.setShader(new LinearGradient(0, t, 0, b, Theme.multAlpha(accent, 0.35f), Theme.multAlpha(accent, 0f), Shader.TileMode.CLAMP));
            canvas.drawPath(area, fill);
            canvas.drawPath(path, line);
            canvas.drawText(String.format(Locale.US, "%,d", max).replace(',', ' '), l, t - AndroidUtilities.dp(3), text);
            canvas.drawText(String.format(Locale.US, "%,d", min).replace(',', ' '), l, b + AndroidUtilities.dp(14), text);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(t0 * 1000L);
            String d0 = String.format(Locale.US, "%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1);
            c.setTimeInMillis(t1 * 1000L);
            String d1 = String.format(Locale.US, "%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1);
            float d1w = text.measureText(d1);
            canvas.drawText(d0, r - d1w - text.measureText(d0) - AndroidUtilities.dp(20) < l + AndroidUtilities.dp(60) ? l + AndroidUtilities.dp(60) : l + AndroidUtilities.dp(60), b + AndroidUtilities.dp(14), text);
            canvas.drawText(d1, r - d1w, b + AndroidUtilities.dp(14), text);
        }
    }
}
