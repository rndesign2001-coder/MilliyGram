/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MilliyGram interfeysi uchun umumiy bloklar: yumaloq kartochka, ikki qatorli ma'lumot qatori,
 * plitka tugma va rangli kod bloki (HTML).
 */
public final class MgUi {

    private MgUi() {
    }

    private static int color(int key, Theme.ResourcesProvider rp) {
        return Theme.getColor(key, rp);
    }

    public static GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(radiusDp));
        g.setColor(color);
        return g;
    }

    /** Oq (mavzu foni) yumaloq kartochka */
    public static LinearLayout card(Context ctx, Theme.ResourcesProvider rp) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(color(Theme.key_windowBackgroundWhite, rp), 18));
        card.setPadding(0, dp(6), 0, dp(6));
        return card;
    }

    /** Kartochka sarlavhasi (ko'k, qalin) */
    public static TextView header(Context ctx, Theme.ResourcesProvider rp, CharSequence text) {
        TextView h = new TextView(ctx);
        h.setText(text);
        h.setTextColor(color(Theme.key_windowBackgroundWhiteBlueHeader, rp));
        h.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        h.setTypeface(AndroidUtilities.bold());
        h.setPadding(dp(18), dp(10), dp(18), dp(4));
        return h;
    }

    /** Sarlavha + qiymat; bosilganda onClick (masalan, nusxalash) */
    public static View infoRow(Context ctx, Theme.ResourcesProvider rp, CharSequence title, CharSequence value, Runnable onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(18), dp(9), dp(18), dp(9));
        if (onClick != null) {
            row.setBackground(Theme.createSelectorDrawable(color(Theme.key_listSelector, rp), Theme.RIPPLE_MASK_ALL));
            row.setOnClickListener(v -> onClick.run());
        }
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(color(Theme.key_windowBackgroundWhiteBlackText, rp));
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        row.addView(t);
        TextView v = new TextView(ctx);
        v.setText(value);
        v.setTextColor(color(Theme.key_windowBackgroundWhiteGrayText, rp));
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        v.setMaxLines(12);
        v.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return row;
    }

    /** Dialog ichidagi yumaloq plitka tugma */
    public static TextView tile(Context ctx, Theme.ResourcesProvider rp, CharSequence text, Runnable onClick) {
        TextView b = new TextView(ctx);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        int fg = color(Theme.key_dialogTextBlack, rp);
        b.setTextColor(fg);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(14),
                Theme.multAlpha(fg, 0.06f), Theme.multAlpha(fg, 0.14f)));
        if (onClick != null) {
            b.setOnClickListener(v -> onClick.run());
        }
        return b;
    }

    /** Asosiy (urg'u rangli) plitka tugma */
    public static TextView accentTile(Context ctx, Theme.ResourcesProvider rp, CharSequence text, Runnable onClick) {
        TextView b = tile(ctx, rp, text, onClick);
        int accent = color(Theme.key_featuredStickers_addButton, rp);
        b.setTextColor(Color.WHITE);
        b.setTypeface(AndroidUtilities.bold());
        b.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(14), accent,
                Theme.blendOver(accent, Theme.multAlpha(Color.WHITE, 0.2f))));
        return b;
    }

    // ---------- Kod bloki (HTML) ----------

    private static final int CODE_BG = 0xFF0D1117;
    private static final int CODE_HEAD_BG = 0xFF161B22;
    private static final int CODE_TEXT = 0xFFADBAC7;
    private static final int CODE_TAG = 0xFFFF7B72;
    private static final int CODE_ATTR = 0xFFFFA657;
    private static final int CODE_VALUE = 0xFF79C0FF;

    private static final Pattern TAG = Pattern.compile("</?[a-zA-Z][a-zA-Z0-9-]*[^>]*>");
    private static final Pattern ATTR = Pattern.compile("\\s([a-zA-Z-]+)(=)?");
    private static final Pattern VALUE = Pattern.compile("(\"[^\"]*\"|'[^']*')");

    /** HTML matnni GitHub uslubidagi ranglar bilan bo'yaydi */
    public static CharSequence highlightHtml(String html) {
        SpannableStringBuilder sb = new SpannableStringBuilder(html);
        Matcher tag = TAG.matcher(html);
        while (tag.find()) {
            int start = tag.start();
            int end = tag.end();
            sb.setSpan(new ForegroundColorSpan(CODE_TAG), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String inner = html.substring(start, end);
            Matcher attr = ATTR.matcher(inner);
            while (attr.find()) {
                sb.setSpan(new ForegroundColorSpan(CODE_ATTR), start + attr.start(1), start + attr.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            Matcher value = VALUE.matcher(inner);
            while (value.find()) {
                sb.setSpan(new ForegroundColorSpan(CODE_VALUE), start + value.start(1), start + value.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }

    /** Sarlavhali ("html") qorong'i kod bloki; nusxalash tugmasi bilan */
    public static View codeBlock(Context ctx, String language, String code, Runnable onCopy) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(round(CODE_BG, 14));
        box.setClipToOutline(true);

        FrameLayout head = new FrameLayout(ctx);
        GradientDrawable headBg = new GradientDrawable();
        float r = dp(14);
        headBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        headBg.setColor(CODE_HEAD_BG);
        head.setBackground(headBg);
        TextView lang = new TextView(ctx);
        lang.setText(language);
        lang.setTextColor(0xFF8B949E);
        lang.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        lang.setTypeface(Typeface.MONOSPACE);
        head.addView(lang, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));
        ImageView copy = new ImageView(ctx);
        copy.setScaleType(ImageView.ScaleType.CENTER);
        Drawable d = ctx.getResources().getDrawable(R.drawable.msg_copy).mutate();
        d.setColorFilter(new PorterDuffColorFilter(0xFF8B949E, PorterDuff.Mode.SRC_IN));
        copy.setImageDrawable(d);
        copy.setBackground(Theme.createSelectorDrawable(0x22FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        if (onCopy != null) {
            copy.setOnClickListener(v -> onCopy.run());
        }
        head.addView(copy, LayoutHelper.createFrame(40, 36, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 4, 0));
        box.addView(head, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));

        TextView body = new TextView(ctx);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        body.setTextColor(CODE_TEXT);
        body.setLineSpacing(dp(2), 1f);
        body.setTextIsSelectable(true);
        body.setText(highlightHtml(code));
        body.setPadding(dp(14), dp(10), dp(14), dp(12));
        box.addView(body, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return box;
    }
}
