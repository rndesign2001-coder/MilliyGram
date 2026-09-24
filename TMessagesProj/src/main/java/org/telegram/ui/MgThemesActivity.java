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
        return "MilliyGram mavzulari";
    }

    /** Rangli doira: TextCell ikonkaga rang filtri qo'yganda ham o'z rangini saqlaydi */
    public static class ColorDot extends Drawable {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint ring = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int color, color2;
        private final boolean selected;

        public ColorDot(int color, int color2, boolean selected) {
            this.color = color;
            this.color2 = color2;
            this.selected = selected;
            ring.setStyle(android.graphics.Paint.Style.STROKE);
            ring.setStrokeWidth(AndroidUtilities.dp(2));
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float cx = b.exactCenterX(), cy = b.exactCenterY();
            float r = Math.min(b.width(), b.height()) / 2f - AndroidUtilities.dp(selected ? 4 : 1);
            paint.setColor(color);
            if (color2 != 0 && color2 != color) {
                android.graphics.RectF rf = new android.graphics.RectF(cx - r, cy - r, cx + r, cy + r);
                canvas.drawArc(rf, 90, 180, true, paint);
                paint.setColor(color2);
                canvas.drawArc(rf, 270, 180, true, paint);
            } else {
                canvas.drawCircle(cx, cy, r, paint);
            }
            if (selected) {
                ring.setColor(color);
                canvas.drawCircle(cx, cy, r + AndroidUtilities.dp(3), ring);
            }
        }

        @Override
        public int getIntrinsicWidth() {
            return AndroidUtilities.dp(26);
        }

        @Override
        public int getIntrinsicHeight() {
            return AndroidUtilities.dp(26);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            // atayin e'tiborsiz: rang o'zgarmasin
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    /** Joriy MilliyGram mavzusi nomi (bo'lmasa bo'sh) */
    public static String currentName() {
        try {
            Theme.ThemeInfo t = Theme.getActiveTheme();
            if (t != null) {
                boolean night = "Dark Blue".equals(t.getKey());
                if (night || "Blue".equals(t.getKey())) {
                    int[] ids = night ? Theme.MG_THEME_NIGHT_IDS : Theme.MG_THEME_DAY_IDS;
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i] == t.currentAccentId) {
                            return Theme.MG_THEME_NAMES[i];
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    private Drawable dot(int color, boolean selected) {
        return new ColorDot(color, 0, selected);
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
        items.add(UItem.asHeader("Kunduzgi mavzular"));
        for (int i = 0; i < Theme.MG_THEME_NAMES.length; i++) {
            if (i == Theme.MG_THEME_NAMES.length - 1 && Build.VERSION.SDK_INT < 31) {
                continue;
            }
            boolean cur = isCurrent(false, i);
            items.add(UItem.asButton(ID_DAY_BASE + i, dot(Theme.MG_THEME_COLORS[i], cur), Theme.MG_THEME_NAMES[i] + (cur ? "  ✓" : "")));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Tungi mavzular"));
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
        final String name = Theme.MG_THEME_NAMES[idx];
        // Mavzu animatsiyasidan keyin barcha ekranlarni qayta chizish (eski ranglar qolib ketmasin)
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentLayout() != null) {
                getParentLayout().rebuildAllFragmentViews(true, true);
            }
        }, 650);
        AndroidUtilities.runOnUIThread(() -> {
            if (listView != null) {
                listView.adapter.update(true);
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Mavzu qo'llanildi: " + name).show();
        }, 900);
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
