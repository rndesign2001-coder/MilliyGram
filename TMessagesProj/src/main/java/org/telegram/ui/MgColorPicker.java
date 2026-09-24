/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Locale;

/**
 * Oddiy va qulay rang tanlash oynasi: tayyor ranglar, rang tusi, yorqinlik va HEX kod.
 */
public class MgColorPicker {

    public static final int[] PALETTE = {
            0xFF0B63CE, 0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1, 0xFF0E8C94, 0xFF00897B,
            0xFF43A047, 0xFF3A9A4A, 0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFB8862B,
            0xFFFFB300, 0xFFFB8C00, 0xFFF4511E, 0xFFE53935, 0xFFC0392B, 0xFFD81B60,
            0xFF8E24AA, 0xFF8E3AA8, 0xFF5E35B1, 0xFF3949AB, 0xFF6D4C41, 0xFF546E7A,
            0xFFFFFFFF, 0xFFF5F5F5, 0xFFE0E0E0, 0xFF9E9E9E, 0xFF424242, 0xFF212121,
            0xFF17212B, 0xFF0E1621, 0xFF000000, 0xFFEFEBE9, 0xFFFFF8E1, 0xFFE8F5E9,
    };

    public static void show(BaseFragment fragment, String title, int initial, boolean hasCustom, Utilities.Callback<Integer> onPick) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) {
            return;
        }
        final int[] color = {initial | 0xFF000000};

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), 0);

        // Namuna
        LinearLayout previewRow = new LinearLayout(ctx);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        View preview = new View(ctx);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setCornerRadius(AndroidUtilities.dp(12));
        preview.setBackground(pbg);
        previewRow.addView(preview, LayoutHelper.createLinear(56, 40));

        EditTextBoldCursor hex = new EditTextBoldCursor(ctx);
        hex.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        hex.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        hex.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        hex.setHint("#RRGGBB");
        hex.setSingleLine(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
        hex.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        hex.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        hex.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        previewRow.addView(hex, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 14, 0, 0, 0));
        root.addView(previewRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 10));

        // Tayyor ranglar
        final int cols = 6;
        final View[] swatches = new View[PALETTE.length];
        LinearLayout row = null;
        for (int i = 0; i < PALETTE.length; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                root.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44));
            }
            FrameLayout cell = new FrameLayout(ctx);
            View sw = new View(ctx);
            swatches[i] = sw;
            cell.addView(sw, LayoutHelper.createFrame(34, 34, Gravity.CENTER));
            final int c = PALETTE[i];
            row.addView(cell, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f));
            cell.setOnClickListener(v -> {
                color[0] = c;
                hex.setTag(1);
                hex.setText(String.format(Locale.US, "#%06X", c & 0xFFFFFF));
                hex.setTag(null);
            });
        }

        // Rang tusi va yorqinlik
        TextView hueLabel = label(ctx, "Rang tusi");
        root.addView(hueLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));
        SeekBar hue = new SeekBar(ctx);
        hue.setMax(360);
        root.addView(hue, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        TextView satLabel = label(ctx, "To'yinganlik");
        root.addView(satLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        SeekBar sat = new SeekBar(ctx);
        sat.setMax(100);
        root.addView(sat, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        TextView valLabel = label(ctx, "Yorqinlik");
        root.addView(valLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        SeekBar val = new SeekBar(ctx);
        val.setMax(100);
        root.addView(val, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 0, 0, 8));

        final boolean[] fromUser = {false};
        Runnable refresh = () -> {
            pbg.setColor(color[0]);
            pbg.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack), 0.15f));
            for (int i = 0; i < swatches.length; i++) {
                GradientDrawable d = new GradientDrawable();
                d.setShape(GradientDrawable.OVAL);
                d.setColor(PALETTE[i]);
                boolean sel = (PALETTE[i] | 0xFF000000) == color[0];
                d.setStroke(AndroidUtilities.dp(sel ? 3 : 1), sel ? Theme.getColor(Theme.key_dialogTextBlack) : Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack), 0.15f));
                swatches[i].setBackground(d);
            }
            if (!fromUser[0]) {
                float[] hsv = new float[3];
                Color.colorToHSV(color[0], hsv);
                hue.setProgress(Math.round(hsv[0]));
                sat.setProgress(Math.round(hsv[1] * 100));
                val.setProgress(Math.round(hsv[2] * 100));
            }
        };
        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean user) {
                if (!user) {
                    return;
                }
                color[0] = Color.HSVToColor(new float[]{hue.getProgress(), sat.getProgress() / 100f, val.getProgress() / 100f});
                fromUser[0] = true;
                hex.setTag(1);
                hex.setText(String.format(Locale.US, "#%06X", color[0] & 0xFFFFFF));
                hex.setTag(null);
                refresh.run();
                fromUser[0] = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        hue.setOnSeekBarChangeListener(l);
        sat.setOnSeekBarChangeListener(l);
        val.setOnSeekBarChangeListener(l);
        hex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (hex.getTag() == null) {
                    Integer c = MgDesign.parseColor(s.toString().startsWith("#") ? s.toString() : "#" + s);
                    if (c != null) {
                        color[0] = c | 0xFF000000;
                        refresh.run();
                    }
                } else {
                    refresh.run();
                }
            }
        });
        hex.setTag(1);
        hex.setText(String.format(Locale.US, "#%06X", color[0] & 0xFFFFFF));
        hex.setTag(null);
        refresh.run();

        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.addView(root);

        AlertDialog.Builder b = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        b.setTitle(title);
        b.setView(scroll);
        b.setPositiveButton("Saqlash", (d, w) -> onPick.run(color[0]));
        if (hasCustom) {
            b.setNeutralButton("Standart", (d, w) -> onPick.run(null));
        }
        b.setNegativeButton("Bekor", null);
        fragment.showDialog(b.create());
    }

    private static TextView label(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        return tv;
    }
}
