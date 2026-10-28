/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

import static org.telegram.messenger.AndroidUtilities.dp;

/**
 * MilliyGram sozlamalari sahifasi uchun ko'rinishlar: logo bilan "hero" karta, tezkor plitkalar
 * va rangli ikonkali bo'lim kartalari.
 */
public final class MgSettingsUi {

    private MgSettingsUi() {
    }

    public static final class Row {
        final int icon;
        final int color;
        final CharSequence title;
        final CharSequence subtitle;
        final CharSequence value;
        final Runnable action;

        public Row(int icon, int color, CharSequence title, CharSequence subtitle, CharSequence value, Runnable action) {
            this.icon = icon;
            this.color = color;
            this.title = title;
            this.subtitle = subtitle;
            this.value = value;
            this.action = action;
        }
    }

    public static final class Tile {
        final int icon;
        final int color;
        final CharSequence title;
        final CharSequence state;
        final boolean active;
        final Runnable action;

        public Tile(int icon, int color, CharSequence title, CharSequence state, boolean active, Runnable action) {
            this.icon = icon;
            this.color = color;
            this.title = title;
            this.state = state;
            this.active = active;
            this.action = action;
        }
    }

    private static int color(int key, Theme.ResourcesProvider rp) {
        return Theme.getColor(key, rp);
    }

    private static Drawable selector(int radiusDp, Theme.ResourcesProvider rp) {
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(radiusDp));
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(color(Theme.key_listSelector, rp)), null, mask);
    }

    /** Rangli yumaloq kvadrat ichidagi oq ikonka */
    private static ImageView iconTile(Context ctx, int icon, int bg, int sizeDp, int radiusDp) {
        ImageView iv = new ImageView(ctx);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        Drawable d = ctx.getResources().getDrawable(icon).mutate();
        d.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iv.setImageDrawable(d);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{lighten(bg, 0.18f), bg});
        g.setCornerRadius(dp(radiusDp));
        iv.setBackground(g);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return iv;
    }

    private static int lighten(int c, float f) {
        int r = Color.red(c), g = Color.green(c), b = Color.blue(c);
        return Color.rgb(r + (int) ((255 - r) * f), g + (int) ((255 - g) * f), b + (int) ((255 - b) * f));
    }

    /** Tepadagi logo kartasi */
    public static View hero(Context ctx, CharSequence version, Runnable onNews, Runnable onChat, Runnable onUpdate) {
        FrameLayout wrap = new FrameLayout(ctx);
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF061445, 0xFF0B2A8A, 0xFF1F5BEA});
        bg.setCornerRadius(dp(22));
        card.setBackground(bg);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(ctx);
        logo.setImageResource(R.mipmap.ic_launcher_round);
        top.addView(logo, LayoutHelper.createLinear(60, 60));
        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setText("MilliyGram");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        texts.addView(title);
        TextView tag = new TextView(ctx);
        tag.setText(org.telegram.messenger.MgLang.t("O‘ZBEKCHA • TEZ • XAVFSIZ"));
        tag.setTextColor(0xCCFFFFFF);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tag.setLetterSpacing(0.06f);
        texts.addView(tag, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        TextView ver = new TextView(ctx);
        ver.setText(version);
        ver.setTextColor(Color.WHITE);
        ver.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(10));
        pill.setColor(0x33FFFFFF);
        ver.setBackground(pill);
        ver.setPadding(dp(8), dp(2), dp(8), dp(2));
        texts.addView(ver, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        top.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 14, 0, 0, 0));
        card.addView(top);

        LinearLayout pills = new LinearLayout(ctx);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.addView(heroButton(ctx, R.drawable.msg_channel, org.telegram.messenger.MgLang.t("Yangiliklar"), onNews), LayoutHelper.createLinear(0, 38, 1f, 0, 0, 4, 0));
        pills.addView(heroButton(ctx, R.drawable.msg_discussion, org.telegram.messenger.MgLang.t("Chat"), onChat), LayoutHelper.createLinear(0, 38, 1f, 4, 0, 4, 0));
        pills.addView(heroButton(ctx, R.drawable.msg_retry, org.telegram.messenger.MgLang.t("Yangilanish"), onUpdate), LayoutHelper.createLinear(0, 38, 1f, 4, 0, 0, 0));
        card.addView(pills, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        wrap.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 12, 12, 12, 4));
        return wrap;
    }

    private static View heroButton(Context ctx, int icon, CharSequence text, Runnable action) {
        LinearLayout b = new LinearLayout(ctx);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(19));
        g.setColor(0x26FFFFFF);
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(19));
        mask.setColor(Color.WHITE);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), g, mask));
        ImageView iv = new ImageView(ctx);
        Drawable d = ctx.getResources().getDrawable(icon).mutate();
        d.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iv.setImageDrawable(d);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.addView(iv, LayoutHelper.createLinear(18, 18));
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        b.addView(tv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 6, 0, 0, 0));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    /** "Ilova haqida" sahifasining tepa kartasi: logo, nom, shior va ikkita versiya kartasi */
    public static View aboutHeader(Context ctx, Theme.ResourcesProvider rp, CharSequence mgVersion, CharSequence tgVersion) {
        FrameLayout wrap = new FrameLayout(ctx);
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(22));
        bg.setColor(color(Theme.key_windowBackgroundWhite, rp));
        card.setBackground(bg);
        card.setPadding(dp(16), dp(24), dp(16), dp(16));

        ImageView logo = new ImageView(ctx);
        logo.setImageResource(R.mipmap.ic_launcher_round);
        card.addView(logo, LayoutHelper.createLinear(104, 104, Gravity.CENTER_HORIZONTAL));
        // logo yumshoq "nafas oladi"
        android.animation.ObjectAnimator sx = android.animation.ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.05f);
        android.animation.ObjectAnimator sy = android.animation.ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.05f);
        final android.animation.ObjectAnimator[] pulse = {sx, sy};
        for (android.animation.ObjectAnimator a : pulse) {
            a.setDuration(1800);
            a.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        }
        logo.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                for (android.animation.ObjectAnimator a : pulse) {
                    if (!a.isStarted()) {
                        a.start();
                    }
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                for (android.animation.ObjectAnimator a : pulse) {
                    a.cancel();
                }
            }
        });

        TextView name = new TextView(ctx);
        name.setText("MilliyGram");
        name.setTextColor(color(Theme.key_windowBackgroundWhiteBlackText, rp));
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        name.setTypeface(AndroidUtilities.bold());
        name.setGravity(Gravity.CENTER);
        card.addView(name, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 14, 0, 0));

        TextView tag = new TextView(ctx);
        tag.setText(org.telegram.messenger.MgLang.t("O'zbekcha • Tez • Xavfsiz"));
        tag.setTextColor(color(Theme.key_windowBackgroundWhiteGrayText, rp));
        tag.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tag.setGravity(Gravity.CENTER);
        card.addView(tag, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        LinearLayout chips = new LinearLayout(ctx);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(versionChip(ctx, rp, "MilliyGram", mgVersion), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 5, 0));
        chips.addView(versionChip(ctx, rp, "Telegram", tgVersion), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 5, 0, 0, 0));
        card.addView(chips, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 20, 0, 0));

        wrap.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 12, 12, 12, 6));
        return wrap;
    }

    private static View versionChip(Context ctx, Theme.ResourcesProvider rp, CharSequence label, CharSequence value) {
        LinearLayout chip = new LinearLayout(ctx);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(12), dp(8), dp(12));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(Theme.multAlpha(color(Theme.key_windowBackgroundWhiteBlackText, rp), 0.06f));
        chip.setBackground(g);
        TextView l = new TextView(ctx);
        l.setText(label);
        l.setTextColor(color(Theme.key_windowBackgroundWhiteGrayText, rp));
        l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        l.setGravity(Gravity.CENTER);
        chip.addView(l);
        TextView v = new TextView(ctx);
        v.setText(value);
        v.setTextColor(color(Theme.key_windowBackgroundWhiteBlueText4, rp));
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        v.setTypeface(AndroidUtilities.bold());
        v.setGravity(Gravity.CENTER);
        chip.addView(v, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return chip;
    }

    /** 4 ta tezkor plitka */
    public static View tiles(Context ctx, Theme.ResourcesProvider rp, ArrayList<Tile> tiles) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(4), dp(12), dp(4), dp(10));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(18));
            g.setColor(color(Theme.key_windowBackgroundWhite, rp));
            if (t.active) {
                g.setStroke(dp(1.5f), t.color);
            }
            GradientDrawable mask = new GradientDrawable();
            mask.setCornerRadius(dp(18));
            mask.setColor(Color.WHITE);
            cell.setBackground(new RippleDrawable(ColorStateList.valueOf(color(Theme.key_listSelector, rp)), g, mask));
            ImageView iv = iconTile(ctx, t.icon, t.color, 42, 14);
            cell.addView(iv, LayoutHelper.createLinear(42, 42));
            TextView title = new TextView(ctx);
            title.setText(t.title);
            title.setTextColor(color(Theme.key_windowBackgroundWhiteBlackText, rp));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            title.setTypeface(AndroidUtilities.bold());
            title.setGravity(Gravity.CENTER);
            title.setMaxLines(1);
            title.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
            TextView state = new TextView(ctx);
            state.setText(t.state);
            state.setTextColor(t.active ? t.color : color(Theme.key_windowBackgroundWhiteGrayText, rp));
            state.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            state.setGravity(Gravity.CENTER);
            state.setMaxLines(1);
            state.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(state, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            cell.setOnClickListener(v -> t.action.run());
            row.addView(cell, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, i == 0 ? 0 : 4, 0, i == tiles.size() - 1 ? 0 : 4, 0));
        }
        return row;
    }

    /** Sarlavhali bo'lim kartasi */
    public static View section(Context ctx, Theme.ResourcesProvider rp, CharSequence header, ArrayList<Row> rows) {
        FrameLayout wrap = new FrameLayout(ctx);
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(color(Theme.key_windowBackgroundWhite, rp));
        card.setBackground(bg);
        card.setPadding(0, dp(4), 0, dp(4));
        if (header != null) {
            TextView h = new TextView(ctx);
            h.setText(header);
            h.setTextColor(color(Theme.key_windowBackgroundWhiteBlueHeader, rp));
            h.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            h.setTypeface(AndroidUtilities.bold());
            h.setPadding(dp(16), dp(10), dp(16), dp(2));
            card.addView(h);
        }
        for (int i = 0; i < rows.size(); i++) {
            card.addView(row(ctx, rp, rows.get(i), i < rows.size() - 1));
        }
        wrap.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 12, 6, 12, 6));
        return wrap;
    }

    private static View row(Context ctx, Theme.ResourcesProvider rp, Row r, boolean divider) {
        final android.graphics.Paint line = new android.graphics.Paint();
        line.setColor(color(Theme.key_divider, rp));
        line.setStrokeWidth(Math.max(1, dp(0.5f)));
        LinearLayout row = new LinearLayout(ctx) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                if (divider) {
                    canvas.drawLine(dp(66), getHeight() - 1, getWidth() - dp(14), getHeight() - 1, line);
                }
            }
        };
        row.setWillNotDraw(!divider);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(62));
        row.setPadding(dp(14), dp(8), dp(12), dp(8));
        row.setBackground(selector(14, rp));
        row.addView(iconTile(ctx, r.icon, r.color, 36, 11), LayoutHelper.createLinear(36, 36));
        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setText(r.title);
        title.setTextColor(color(Theme.key_windowBackgroundWhiteBlackText, rp));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title);
        if (!TextUtils.isEmpty(r.subtitle)) {
            TextView sub = new TextView(ctx);
            sub.setText(r.subtitle);
            sub.setTextColor(color(Theme.key_windowBackgroundWhiteGrayText, rp));
            sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            sub.setMaxLines(2);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(sub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 0));
        }
        row.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 16, 0, 8, 0));
        if (!TextUtils.isEmpty(r.value)) {
            TextView val = new TextView(ctx);
            val.setText(r.value);
            val.setTextColor(color(Theme.key_windowBackgroundWhiteValueText, rp));
            val.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            val.setMaxLines(1);
            row.addView(val, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
        }
        ImageView arrow = new ImageView(ctx);
        Drawable d = ctx.getResources().getDrawable(R.drawable.msg_arrowright).mutate();
        d.setColorFilter(new PorterDuffColorFilter(color(Theme.key_windowBackgroundWhiteGrayIcon, rp), PorterDuff.Mode.SRC_IN));
        arrow.setImageDrawable(d);
        arrow.setAlpha(0.6f);
        row.addView(arrow, LayoutHelper.createLinear(20, 20));
        row.setOnClickListener(v -> r.action.run());
        return row;
    }
}
