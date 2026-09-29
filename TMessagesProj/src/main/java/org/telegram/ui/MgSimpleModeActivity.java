/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgSimple;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Oddiy rejim — keksalar va ko'zi zaif foydalanuvchilar uchun: katta shrift, katta interfeys,
 * keng chatlar ro'yxati va soddalashtirilgan menyu.
 */
public class MgSimpleModeActivity extends UniversalFragment {

    private static final int ID_ON = 1;
    private static final int ID_FONT = 2;
    private static final int ID_SCALE = 3;
    private static final int ID_THREE_LINES = 4;
    private static final int ID_MENU = 5;
    private static final int ID_STORIES = 6;

    @Override
    protected CharSequence getTitle() {
        return MgLang.t("Oddiy rejim");
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context ctx = getContext();
        boolean on = MgSimple.isEnabled();
        if (ctx != null) {
            TextView preview = new TextView(ctx);
            preview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, on ? MgSimple.getFontSize() : MgConfig.NORMAL_FONT_SIZE);
            preview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            preview.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(16), AndroidUtilities.dp(21), AndroidUtilities.dp(16));
            preview.setText(MgLang.t("👴👵 Assalomu alaykum! Oddiy rejimda yozuvlar kattaroq, tugmalar kengroq va menyular soddaroq bo'ladi."));
            items.add(UItem.asCustom(preview));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asCheck(ID_ON, MgLang.t("Oddiy rejimni yoqish")).setChecked(on));
        items.add(UItem.asShadow(MgLang.t("Bir bosishda: katta shrift, katta interfeys, keng chatlar ro'yxati va soddalashtirilgan menyu yoqiladi. Quyida har birini alohida sozlash mumkin.")));

        items.add(UItem.asHeader(MgLang.t("Ko'rinish")));
        items.add(UItem.asButton(ID_FONT, R.drawable.msg_text_outlined, MgLang.t("Xabarlar shrifti"), MgSimple.getFontSize() + " pt"));
        items.add(UItem.asButton(ID_SCALE, R.drawable.msg_zoomin, MgLang.t("Tugmalar va yozuvlar kattaligi"), MgSimple.getUiScale() + "%"));
        items.add(UItem.asCheck(ID_THREE_LINES, MgLang.t("Keng chatlar ro'yxati (katta rasm)")).setChecked(SharedConfig.useThreeLinesLayout));
        items.add(UItem.asShadow(MgLang.t("Shrift va kattalik faqat oddiy rejim yoqilganda qo'llanadi. Kattalik o'zgarganda ilova bir marta qayta ochiladi.")));

        items.add(UItem.asHeader(MgLang.t("Soddalashtirish")));
        items.add(UItem.asCheck(ID_MENU, MgLang.t("Soddalashtirilgan xabar menyusi")).setChecked(MgConfig.getBool("simple_menu", true)));
        items.add(UItem.asCheck(ID_STORIES, MgLang.t("Hikoyalar panelini yashirish")).setChecked(MgConfig.getBool("hide_stories", false)));
        items.add(UItem.asShadow(MgLang.t("Soddalashtirilgan menyuda xabarni uzoq bosganda faqat asosiy amallar qoladi: javob berish, nusxalash, uzatish va o'chirish.")));
    }

    private void setCheck(View view, boolean v) {
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(v);
        }
    }

    private void refresh() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void askRestart() {
        Context ctx = getParentActivity();
        if (ctx == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, getResourceProvider());
        b.setTitle(MgLang.t("Qayta ochish"));
        b.setMessage(MgLang.t("O'zgarish to'liq qo'llanishi uchun ekran qayta ochiladi."));
        b.setPositiveButton(MgLang.t("Qayta ochish"), (d, w) -> {
            if (LaunchActivity.instance != null) {
                LaunchActivity.instance.recreate();
            }
        });
        b.setNegativeButton(MgLang.t("Keyinroq"), null);
        showDialog(b.create());
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_ON: {
                boolean v = !MgSimple.isEnabled();
                MgSimple.setEnabled(v);
                setCheck(view, v);
                refresh();
                if (MgSimple.getUiScale() != 100) {
                    askRestart();
                }
                break;
            }
            case ID_FONT: {
                String[] names = new String[MgSimple.FONT_SIZES.length];
                for (int i = 0; i < names.length; i++) {
                    int s = MgSimple.FONT_SIZES[i];
                    names[i] = s + " pt" + (s == 16 ? MgLang.t(" — odatiy") : s >= 24 ? MgLang.t(" — juda katta") : "") + (s == MgSimple.getFontSize() ? "  ✓" : "");
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle(MgLang.t("Xabarlar shrifti"));
                b.setItems(names, (d, w) -> {
                    MgSimple.setFontSize(MgSimple.FONT_SIZES[w]);
                    refresh();
                });
                showDialog(b.create());
                break;
            }
            case ID_SCALE: {
                String[] names = new String[MgSimple.UI_SCALES.length];
                for (int i = 0; i < names.length; i++) {
                    int s = MgSimple.UI_SCALES[i];
                    names[i] = s + "%" + (s == 100 ? MgLang.t(" — odatiy") : "") + (s == MgSimple.getUiScale() ? "  ✓" : "");
                }
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle(MgLang.t("Tugmalar va yozuvlar kattaligi"));
                b.setItems(names, (d, w) -> {
                    int old = MgSimple.getUiScale();
                    MgConfig.setInt("simple_ui_scale", MgSimple.UI_SCALES[w]);
                    refresh();
                    if (old != MgSimple.UI_SCALES[w] && MgSimple.isEnabled()) {
                        askRestart();
                    }
                });
                showDialog(b.create());
                break;
            }
            case ID_THREE_LINES: {
                boolean v = !SharedConfig.useThreeLinesLayout;
                SharedConfig.setUseThreeLinesLayout(v);
                setCheck(view, v);
                break;
            }
            case ID_MENU: {
                boolean v = !MgConfig.getBool("simple_menu", true);
                MgConfig.setBool("simple_menu", v);
                setCheck(view, v);
                break;
            }
            case ID_STORIES: {
                boolean v = !MgConfig.getBool("hide_stories", false);
                MgConfig.setBool("hide_stories", v);
                setCheck(view, v);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                break;
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        MgSimple.stop();
    }
}
