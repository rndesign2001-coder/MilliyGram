/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgLocalFolders;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/**
 * Jild ikonkasini to'r (grid) ko'rinishida tanlash oynasi.
 */
public class MgIconPicker {

    public interface Callback {
        /** key == null — standart ikonkaga qaytarish */
        void onPicked(String key);
    }

    public static void show(BaseFragment fragment, String title, String currentKey, Callback cb) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) {
            return;
        }
        final AlertDialog[] dialogRef = new AlertDialog[1];
        ArrayList<String> keys = new ArrayList<>(MgLocalFolders.ICONS.keySet());
        final int cols = 4;
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
        int iconColor = Theme.getColor(Theme.key_dialogTextBlack);
        int textColor = Theme.getColor(Theme.key_dialogTextGray3);

        LinearLayout grid = new LinearLayout(ctx);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        LinearLayout row = null;
        for (int i = 0; i < keys.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }
            final String key = keys.get(i);
            boolean selected = TextUtils.equals(key, currentKey);
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
            cell.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector), 12, 12));

            ImageView icon = new ImageView(ctx);
            icon.setScaleType(ImageView.ScaleType.CENTER);
            Integer res = MgLocalFolders.ICONS.get(key);
            if (res != null) {
                icon.setImageResource(res);
            }
            icon.setColorFilter(new PorterDuffColorFilter(selected ? 0xFFFFFFFF : iconColor, PorterDuff.Mode.SRC_IN));
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(selected ? accent : Theme.multAlpha(iconColor, 0.07f));
            icon.setBackground(circle);
            cell.addView(icon, LayoutHelper.createLinear(48, 48, Gravity.CENTER_HORIZONTAL));

            TextView name = new TextView(ctx);
            name.setText(MgLocalFolders.ICON_NAMES.get(key));
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            name.setTextColor(selected ? accent : textColor);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 2, 4, 2, 0));

            cell.setOnClickListener(v -> {
                if (dialogRef[0] != null) {
                    dialogRef[0].dismiss();
                }
                cb.onPicked(key);
            });
            row.addView(cell, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        }
        // oxirgi qatorni to'ldirish
        if (row != null) {
            int rest = keys.size() % cols;
            for (int i = rest == 0 ? cols : rest; i < cols; i++) {
                row.addView(new android.view.View(ctx), LayoutHelper.createLinear(0, 1, 1f));
            }
        }
        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(grid);

        AlertDialog.Builder b = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        b.setTitle(title);
        b.setView(scroll);
        b.setNeutralButton("Standart", (d, w) -> cb.onPicked(null));
        b.setNegativeButton("Bekor qilish", null);
        dialogRef[0] = b.create();
        fragment.showDialog(dialogRef[0]);
    }
}
