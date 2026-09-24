/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * MilliyGram mavzulari: milliy ranglar + milliy naqshli chat fonlari (kunduzgi va tungi).
 */
public class MgThemesActivity extends UniversalFragment {

    private static final int ID_DAY_BASE = 100;
    private static final int ID_NIGHT_BASE = 200;
    private static final int ID_CHAT_SETTINGS = 1;
    private static final int ID_WALLPAPERS = 2;

    @Override
    protected CharSequence getTitle() {
        return "🎨 MilliyGram mavzulari";
    }

    private Drawable dot(int color, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        if (selected) {
            d.setStroke(AndroidUtilities.dp(3), Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        }
        d.setSize(AndroidUtilities.dp(26), AndroidUtilities.dp(26));
        return d;
    }

    private boolean isCurrent(boolean night, int idx) {
        Theme.ThemeInfo active = Theme.getActiveTheme();
        if (active == null) {
            return false;
        }
        String key = night ? "Dark Blue" : "Blue";
        int accentId = night ? Theme.MG_THEME_NIGHT_IDS[idx] : Theme.MG_THEME_DAY_IDS[idx];
        return key.equals(active.getKey()) && active.currentAccentId == accentId;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("☀️ Kunduzgi mavzular"));
        for (int i = 0; i < Theme.MG_THEME_NAMES.length; i++) {
            if (i == Theme.MG_THEME_NAMES.length - 1 && Build.VERSION.SDK_INT < 31) {
                continue;
            }
            boolean cur = isCurrent(false, i);
            items.add(UItem.asButton(ID_DAY_BASE + i, dot(Theme.MG_THEME_COLORS[i], cur), Theme.MG_THEME_NAMES[i] + (cur ? "  ✓" : "")));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("🌙 Tungi mavzular"));
        for (int i = 0; i < Theme.MG_THEME_NAMES.length; i++) {
            if (i == Theme.MG_THEME_NAMES.length - 1 && Build.VERSION.SDK_INT < 31) {
                continue;
            }
            boolean cur = isCurrent(true, i);
            items.add(UItem.asButton(ID_NIGHT_BASE + i, dot(Theme.MG_THEME_COLORS[i], cur), Theme.MG_THEME_NAMES[i] + (cur ? "  ✓" : "")));
        }
        items.add(UItem.asShadow("Har bir mavzuning o'z milliy naqshli chat foni bor: Ko'k osmon — milliy naqsh, Feruza — girih, Oltin — suzani, Anor — anor, Paxta — paxta gullari, Atlas — abr naqshi. Material You ranglari telefon fon rasmidan olinadi (Android 12+)."));
        items.add(UItem.asButton(ID_WALLPAPERS, R.drawable.msg_background, "Boshqa chat foni tanlash"));
        items.add(UItem.asButton(ID_CHAT_SETTINGS, R.drawable.msg_palette, "Chat sozlamalari (shrift, burchaklar)"));
        items.add(UItem.asShadow(null));
    }

    private void apply(boolean night, int idx) {
        Theme.ThemeInfo info = Theme.getTheme(night ? "Dark Blue" : "Blue");
        if (info == null) {
            return;
        }
        int accentId = night ? Theme.MG_THEME_NIGHT_IDS[idx] : Theme.MG_THEME_DAY_IDS[idx];
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, info, false, null, accentId);
        ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE).edit()
                .putString(night ? "lastDarkTheme" : "lastDayTheme", info.getKey())
                .commit();
        Theme.turnOffAutoNight(this);
        AndroidUtilities.runOnUIThread(() -> {
            if (listView != null) {
                listView.adapter.update(true);
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Mavzu qo'llanildi: " + Theme.MG_THEME_NAMES[idx]).show();
        }, 400);
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_DAY_BASE && item.id < ID_DAY_BASE + Theme.MG_THEME_NAMES.length) {
            apply(false, item.id - ID_DAY_BASE);
        } else if (item.id >= ID_NIGHT_BASE && item.id < ID_NIGHT_BASE + Theme.MG_THEME_NAMES.length) {
            apply(true, item.id - ID_NIGHT_BASE);
        } else if (item.id == ID_WALLPAPERS) {
            presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL));
        } else if (item.id == ID_CHAT_SETTINGS) {
            presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
