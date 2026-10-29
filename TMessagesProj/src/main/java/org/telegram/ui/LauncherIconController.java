package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.mipmap.mg_icon_background, R.mipmap.mg_icon_foreground, R.string.AppIconDefault),
        VINTAGE("VintageIcon", R.mipmap.mg_icon_atlas_fg, android.R.color.transparent, R.string.AppIconVintage, "Atlas"),
        AQUA("AquaIcon", R.mipmap.mg_icon_oq_fg, android.R.color.transparent, R.string.AppIconAqua, "Oq"),
        PREMIUM("PremiumIcon", R.mipmap.mg_icon_shisha_fg, android.R.color.transparent, R.string.AppIconPremium, "Shisha"),
        TURBO("TurboIcon", R.mipmap.mg_icon_neon_fg, android.R.color.transparent, R.string.AppIconTurbo, "Neon"),
        NOX("NoxIcon", R.mipmap.mg_icon_binafsha_fg, android.R.color.transparent, R.string.AppIconNox, "Binafsha"),
        FLAG("FlagIcon", R.mipmap.mg_icon_bayroq_fg, android.R.color.transparent, R.string.AppIconDefault, "Bayroq");

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        private final String mgName; // MilliyGram: ikonka nomi (MgLang orqali tarjima qilinadi)

        public String getTitle() {
            if (mgName != null) {
                return org.telegram.messenger.MgLang.t(mgName);
            }
            return this == DEFAULT ? org.telegram.messenger.MgLang.t("Asosiy") : org.telegram.messenger.LocaleController.getString(title);
        }

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, String mgName) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = false;
            this.mgName = mgName;
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
            this.mgName = null;
        }
    }
}
