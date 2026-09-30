/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import java.util.ArrayList;

/**
 * Chat ⋮ menyusidagi MilliyGram bandlari: guruhlangan "MilliyGram vositalari" oynasi va
 * "Xabarga tez o'tish" oynasi.
 */
public class MgChatMenu {

    public static final int MENU_JUMP = 9050;
    public static final int MENU_TOOLS = 9051;

    public static final class Item {
        final int icon;
        final CharSequence text;
        final Runnable action;
        boolean red;

        public Item(int icon, CharSequence text, Runnable action) {
            this.icon = icon;
            this.text = text;
            this.action = action;
        }

        public Item red() {
            red = true;
            return this;
        }
    }

    public static final class Section {
        final String title;
        final ArrayList<Item> items = new ArrayList<>();

        public Section(String title) {
            this.title = title;
        }

        public Section add(int icon, CharSequence text, Runnable action) {
            items.add(new Item(icon, text, action));
            return this;
        }
    }

    /** Bo'limlarga ajratilgan vositalar oynasi (pastdan chiqadi) */
    public static void show(BaseFragment f, String title, ArrayList<Section> sections) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        Theme.ResourcesProvider rp = f.getResourceProvider();
        BottomSheet.Builder builder = new BottomSheet.Builder(ctx, false, rp);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

        TextView head = new TextView(ctx);
        head.setText(title);
        head.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        head.setTypeface(AndroidUtilities.bold());
        head.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, rp));
        box.addView(head, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 21, 8, 21, 6));

        final BottomSheet[] sheet = new BottomSheet[1];
        for (Section s : sections) {
            if (s.items.isEmpty()) {
                continue;
            }
            TextView sh = new TextView(ctx);
            sh.setText(s.title);
            sh.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            sh.setTypeface(AndroidUtilities.bold());
            sh.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2, rp));
            box.addView(sh, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 21, 14, 21, 2));
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(14));
            bg.setColor(Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, rp), 0.05f));
            card.setBackground(bg);
            for (int i = 0; i < s.items.size(); i++) {
                Item it = s.items.get(i);
                TextCell cell = new TextCell(ctx, rp);
                cell.setTextAndIcon(it.text, it.icon, i < s.items.size() - 1);
                if (it.red) {
                    cell.setColors(Theme.key_text_RedRegular, Theme.key_text_RedRegular);
                }
                cell.setBackground(Theme.getSelectorDrawable(false));
                cell.setOnClickListener(v -> {
                    if (sheet[0] != null) {
                        sheet[0].dismiss();
                    }
                    AndroidUtilities.runOnUIThread(it.action, 150);
                });
                card.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
            }
            box.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 4, 12, 0));
        }
        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(box);
        builder.setCustomView(scroll);
        sheet[0] = builder.create();
        f.showDialog(sheet[0]);
    }

    /** ⋮ menyusi ichida yon tomondan ochiladigan sahifa (Orqaga tugmasi bilan) */
    public static ActionBarPopupWindow.ActionBarPopupWindowLayout createSwipeLayout(Context ctx, Theme.ResourcesProvider rp) {
        ActionBarPopupWindow.ActionBarPopupWindowLayout layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(ctx, 0, rp);
        layout.setFitItems(true);
        return layout;
    }

    /**
     * Sahifani bo'limlar bilan to'ldiradi. dismiss — menyuni yopish; refill — amal bajarilgach
     * sahifani yangilash (masalan "Chatni qulflash" → "Qulfni olish").
     */
    public static void fillSwipe(ActionBarPopupWindow.ActionBarPopupWindowLayout layout, PopupSwipeBackLayout swipeBack, Theme.ResourcesProvider rp,
                                 ArrayList<Section> sections, Runnable dismiss, Runnable refill) {
        layout.removeInnerViews();
        Context ctx = layout.getContext();
        ActionBarMenuSubItem back = ActionBarMenuItem.addItem(layout, R.drawable.msg_arrow_back, LocaleController.getString(R.string.Back), false, rp);
        back.setOnClickListener(v -> {
            if (swipeBack != null) {
                swipeBack.closeForeground();
            }
        });
        if (sections == null) {
            return;
        }
        for (Section s : sections) {
            if (s.items.isEmpty()) {
                continue;
            }
            ActionBarPopupWindow.GapView gap = new ActionBarPopupWindow.GapView(ctx, rp, Theme.key_actionBarDefaultSubmenuSeparator);
            gap.setTag(R.id.fit_width_tag, 1);
            layout.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
            if (s.title != null) {
                TextView head = new TextView(ctx);
                head.setText(s.title);
                head.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                head.setTypeface(AndroidUtilities.bold());
                head.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, rp));
                head.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(8), AndroidUtilities.dp(18), AndroidUtilities.dp(2));
                head.setTag(R.id.fit_width_tag, 1);
                layout.addView(head, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }
            for (Item it : s.items) {
                ActionBarMenuSubItem cell = ActionBarMenuItem.addItem(layout, it.icon, it.text, false, rp);
                if (it.red) {
                    int red = Theme.getColor(Theme.key_text_RedRegular, rp);
                    cell.setTextColor(red);
                    cell.setIconColor(red);
                }
                cell.setOnClickListener(v -> {
                    if (dismiss != null) {
                        dismiss.run();
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        it.action.run();
                        if (refill != null) {
                            AndroidUtilities.runOnUIThread(refill, 700);
                        }
                    }, 150);
                });
            }
        }
    }

    /** "Xabarga tez o'tish" oynasi */
    public static void showQuickJump(BaseFragment f, Runnable first, Runnable up, Runnable down, Runnable byDate) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        Theme.ResourcesProvider rp = f.getResourceProvider();
        BottomSheet.Builder builder = new BottomSheet.Builder(ctx, false, rp);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        TextView head = new TextView(ctx);
        head.setText(MgLang.t("Xabarga tez o'tish"));
        head.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        head.setTypeface(AndroidUtilities.bold());
        head.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, rp));
        box.addView(head, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 6, 0, 6, 12));
        final BottomSheet[] sheet = new BottomSheet[1];
        String[] names = {MgLang.t("⏮  Eng birinchi xabarga o'tish"), MgLang.t("⏫  Tepaga tez chiqish (100 ta xabar)"),
                MgLang.t("⏬  Pastga tez tushish (eng oxirgisi)"), MgLang.t("📅  Sana bo'yicha o'tish")};
        Runnable[] actions = {first, up, down, byDate};
        for (int i = 0; i < names.length; i++) {
            if (actions[i] == null) {
                continue;
            }
            TextView b = new TextView(ctx);
            b.setText(names[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            b.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, rp));
            b.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(12));
            bg.setColor(Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, rp), 0.06f));
            b.setBackground(bg);
            final Runnable a = actions[i];
            final boolean keepOpen = i == 1; // tepaga chiqishni ketma-ket bosish mumkin
            b.setOnClickListener(v -> {
                if (!keepOpen && sheet[0] != null) {
                    sheet[0].dismiss();
                }
                a.run();
            });
            box.addView(b, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 0, 0, 0, 8));
        }
        builder.setCustomView(box);
        sheet[0] = builder.create();
        f.showDialog(sheet[0]);
    }

}
