/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgPlaces;
import org.telegram.messenger.MgPrayer;
import org.telegram.messenger.MgWeather;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Chatlar ro'yxati tepasida (⋮ tugmasi yonida) kichik "chip": keyingi namozgacha qolgan vaqt va/yoki ob-havo.
 * Sozlamalar → Namoz vaqti va ob-havo bo'limida yoqiladi, uslubi tanlanadi.
 */
public class MgInfoChip extends TextView {

    public static final int MODE_OFF = 0, MODE_PRAYER = 1, MODE_WEATHER = 2, MODE_BOTH = 3;
    public static final String[] MODE_NAMES = {"O'chirilgan", "Namoz vaqti", "Ob-havo", "Ikkalasi (almashib)"};
    public static final String[] STYLE_NAMES = {"Kapsula", "Chegarali", "Oddiy matn", "Ixcham", "Rangli"};

    public static int getMode() {
        return MgConfig.getInt("chip_mode", MODE_OFF);
    }

    public static int getStyleIndex() {
        return MgConfig.getInt("chip_style", 0);
    }

    private final BaseFragment fragment;
    private boolean attached;
    private int tick;
    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            tick++;
            update();
            if (attached) {
                AndroidUtilities.runOnUIThread(this, 5000);
            }
        }
    };

    public MgInfoChip(Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;
        setSingleLine(true);
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        setTypeface(AndroidUtilities.bold());
        setOnClickListener(v -> showDetails(fragment));
    }

    /** DialogsActivity menyusiga ⋮ tugmasidan oldin qo'shadi */
    public static ActionBarMenuItem attach(BaseFragment fragment, ActionBarMenu menu, Context context) {
        ActionBarMenuItem holder = menu.addItemWithWidth(9060, 0, AndroidUtilities.dp(10));
        MgInfoChip chip = new MgInfoChip(context, fragment);
        holder.addView(chip, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 28, Gravity.CENTER, 2, 0, 2, 0));
        holder.getLayoutParams().width = LayoutHelper.WRAP_CONTENT;
        holder.setOnClickListener(v -> showDetails(fragment));
        holder.setContentDescription("Namoz vaqti va ob-havo");
        holder.setTag(chip);
        holder.setVisibility(getMode() == MODE_OFF ? View.GONE : View.VISIBLE);
        chip.setVisibility(getMode() == MODE_OFF ? View.GONE : View.VISIBLE);
        return holder;
    }

    /** Sozlamalar o'zgarganda chaqiriladi */
    public static void refreshHolder(ActionBarMenuItem holder) {
        if (holder == null) {
            return;
        }
        holder.setVisibility(getMode() == MODE_OFF ? View.GONE : View.VISIBLE);
        if (holder.getTag() instanceof MgInfoChip) {
            ((MgInfoChip) holder.getTag()).setVisibility(getMode() == MODE_OFF ? View.GONE : View.VISIBLE);
            ((MgInfoChip) holder.getTag()).applyStyle();
            ((MgInfoChip) holder.getTag()).update();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        applyStyle();
        AndroidUtilities.cancelRunOnUIThread(updater);
        updater.run();
        if (getMode() == MODE_WEATHER || getMode() == MODE_BOTH) {
            MgWeather.refresh(false, d -> update());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attached = false;
        AndroidUtilities.cancelRunOnUIThread(updater);
    }

    private void applyStyle() {
        int fg = Theme.getColor(Theme.key_actionBarDefaultIcon);
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        int style = getStyleIndex();
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(14));
        int padH = AndroidUtilities.dp(10);
        switch (style) {
            case 1:
                bg.setColor(0);
                bg.setStroke(AndroidUtilities.dp(1.2f), Theme.multAlpha(fg, 0.45f));
                setTextColor(fg);
                break;
            case 2:
                bg = null;
                padH = AndroidUtilities.dp(4);
                setTextColor(fg);
                break;
            case 3:
                bg.setColor(Theme.multAlpha(fg, 0.10f));
                padH = AndroidUtilities.dp(7);
                setTextColor(fg);
                break;
            case 4:
                bg.setColor(accent);
                setTextColor(0xFFFFFFFF);
                break;
            default:
                bg.setColor(Theme.multAlpha(fg, 0.12f));
                setTextColor(fg);
                break;
        }
        setBackground(bg);
        setPadding(padH, 0, padH, 0);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, style == 3 ? 12 : 13);
    }

    private void update() {
        int mode = getMode();
        if (mode == MODE_OFF) {
            return;
        }
        boolean compact = getStyleIndex() == 3;
        boolean showWeather = mode == MODE_WEATHER || (mode == MODE_BOTH && (tick / 2) % 2 == 1);
        String text = null;
        if (showWeather) {
            MgWeather.Data d = MgWeather.getCached();
            if (d != null) {
                text = MgWeather.icon(d.code) + (compact ? "" : " ") + MgWeather.temp(d.temp);
            } else if (mode == MODE_WEATHER) {
                text = "🌡 …";
                MgWeather.refresh(false, x -> update());
            }
        }
        if (text == null) {
            int[] n = MgPrayer.next();
            text = compact ? "🕌 " + MgPrayer.left(n[1]) : MgPrayer.NAMES[n[0]] + " · " + MgPrayer.left(n[1]);
        }
        if (!TextUtils.equals(getText(), text)) {
            setText(text);
            requestLayout();
        }
    }

    // ================= Batafsil oyna =================

    public static void showDetails(BaseFragment f) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        MgPlaces.Place place = MgPrayer.getPlace();
        int[] t = MgPrayer.today();
        int cur = MgPrayer.current(t);
        int[] next = MgPrayer.next();
        int black = Theme.getColor(Theme.key_dialogTextBlack);
        int gray = Theme.getColor(Theme.key_dialogTextGray3);
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(8));

        TextView loc = new TextView(ctx);
        loc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        loc.setTextColor(gray);
        loc.setText("📍 " + place.name + " · " + MgPlaces.findRegion(MgPrayer.getRegionKey()).name);
        box.addView(loc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView nx = new TextView(ctx);
        nx.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        nx.setTextColor(accent);
        nx.setTypeface(AndroidUtilities.bold());
        nx.setText(MgPrayer.NAMES[next[0]] + " namozigacha " + MgPrayer.left(next[1]) + " qoldi");
        box.addView(nx, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        for (int i = 0; i < 6; i++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            if (i == cur) {
                GradientDrawable hl = new GradientDrawable();
                hl.setCornerRadius(AndroidUtilities.dp(10));
                hl.setColor(Theme.multAlpha(accent, 0.14f));
                row.setBackground(hl);
            }
            TextView name = new TextView(ctx);
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            name.setTextColor(i == cur ? accent : black);
            name.setText(MgPrayer.NAMES[i] + (i == MgPrayer.SUNRISE ? " chiqishi" : ""));
            if (i == cur) {
                name.setTypeface(AndroidUtilities.bold());
            }
            row.addView(name, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
            TextView time = new TextView(ctx);
            time.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            time.setTextColor(i == cur ? accent : black);
            time.setTypeface(AndroidUtilities.bold());
            time.setText(MgPrayer.hhmm(t[i]));
            row.addView(time, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            box.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        TextView wx = new TextView(ctx);
        wx.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        wx.setTextColor(black);
        wx.setLineSpacing(AndroidUtilities.dp(3), 1f);
        box.addView(wx, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 0, 0));
        Runnable fillWeather = () -> {
            MgWeather.Data d = MgWeather.getCached();
            if (d == null) {
                wx.setText("Ob-havo yuklanmoqda… (internet kerak)");
                return;
            }
            SpannableStringBuilder sb = new SpannableStringBuilder();
            int s0 = sb.length();
            sb.append(MgWeather.icon(d.code)).append(" ").append(MgWeather.temp(d.temp)).append("  ").append(MgWeather.describe(d.code));
            sb.setSpan(new StyleSpan(Typeface.BOLD), s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(1.15f), s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            int s1 = sb.length();
            sb.append("\nSezilishi ").append(MgWeather.temp(d.feels)).append(" · Namlik ").append(String.valueOf(d.humidity)).append("% · Shamol ")
                    .append(String.valueOf(Math.round(d.wind))).append(" m/s");
            sb.setSpan(new ForegroundColorSpan(gray), s1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String[] days = {"Bugun", "Ertaga", "Indinga"};
            for (int i = 0; i < 3; i++) {
                sb.append("\n").append(days[i]).append(":  ").append(MgWeather.icon(d.dayCode[i])).append(" ")
                        .append(MgWeather.temp(d.dayMax[i])).append(" / ").append(MgWeather.temp(d.dayMin[i]));
            }
            wx.setText(sb);
        };
        fillWeather.run();
        MgWeather.refresh(false, d -> fillWeather.run());

        TextView note = new TextView(ctx);
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        note.setTextColor(gray);
        note.setText("Vaqtlar O'zbekiston musulmonlari idorasi taqvimi uslubida hisoblangan; mahalliy masjid jadvali bilan 1–2 daqiqa farq qilishi mumkin. Sozlamalarda tuzatish mumkin.");
        box.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(box);
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Namoz vaqtlari va ob-havo");
        b.setView(scroll);
        b.setPositiveButton("Yopish", null);
        b.setNeutralButton("Sozlamalar", (d, w) -> f.presentFragment(new MgSettingsPage(MgSettingsPage.PAGE_PRAYER)));
        f.showDialog(b.create());
    }
}
