/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgSimple;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/**
 * Birinchi ochilishdagi tanishtiruv: 4 ekranda MilliyGram'ning asosiy imkoniyatlari.
 */
public class MgOnboarding {

    private static final String PREF = "onboard_v2";
    private static boolean showing;

    private static final class Page {
        final String icon, title;
        final String[] points;
        final int c1, c2;

        Page(String icon, String title, int c1, int c2, String... points) {
            this.icon = icon;
            this.title = title;
            this.c1 = c1;
            this.c2 = c2;
            this.points = points;
        }
    }

    private static ArrayList<Page> pages() {
        ArrayList<Page> list = new ArrayList<>();
        list.add(new Page("🇺🇿", MgLang.t("MilliyGram'ga xush kelibsiz!"), 0xFF1E6FD9, 0xFF0B2F6B,
                MgLang.t("Telegram'ning barcha imkoniyatlari va o'zbek foydalanuvchilari uchun qo'shimchalar"),
                MgLang.t("Sozlamalar → MilliyGram bo'limida hammasi bir joyda"),
                MgLang.t("Ma'lumotlaringiz faqat telefoningizda qoladi")));
        list.add(new Page("🔒", MgLang.t("Maxfiylik"), 0xFF6B3FD9, 0xFF2A1260,
                MgLang.t("Chatni PIN yoki grafik kalit bilan qulflash (chat → ⋮)"),
                MgLang.t("Yashirin bo'lim: qidiruv tugmasini uzoq bosing"),
                MgLang.t("Telefonni silkitsangiz, maxfiy chatlar darhol yopiladi"),
                MgLang.t("Firibgarlik va APK fayllardan ogohlantirish")));
        list.add(new Page("🕋", MgLang.t("Namoz, qibla va ob-havo"), 0xFF12825A, 0xFF053A27,
                MgLang.t("Chatlar ro'yxati tepasida keyingi namozgacha qolgan vaqt"),
                MgLang.t("Uni bossangiz — barcha vaqtlar, ob-havo va jonli qibla kompasi"),
                MgLang.t("Namoz vaqtini eslatish, lotin ↔ kirill o'girish, tarjima")));
        list.add(new Page("📢", MgLang.t("Kanal adminlari uchun"), 0xFFE0822A, 0xFF6B2F05,
                MgLang.t("☁ Bulutcha — postni bir bosishda Saqlangan xabarlarga"),
                MgLang.t("Yuborish tugmasini uzoq bosing → reklama postini 24/48 soatdan keyin avtomatik o'chirish"),
                MgLang.t("Bitta postni ko'p kanalga tahrirlab uzatish"),
                MgLang.t("Obunachilar kundaligi va eng yaxshi post vaqti")));
        return list;
    }

    /** LaunchActivity.onResume'dan: login qilingan va hali ko'rsatilmagan bo'lsa */
    public static void maybeShow(Activity act) {
        if (act == null || showing || MgConfig.getBool(PREF, false)) {
            return;
        }
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (showing || MgConfig.getBool(PREF, false) || act.isFinishing()) {
                return;
            }
            BaseFragment last = LaunchActivity.getLastFragment();
            if (!(last instanceof DialogsActivity) && !(last instanceof MainTabsActivity)) {
                return;
            }
            show(act);
        }, 1500);
    }

    public static void show(Activity act) {
        if (act == null || showing) {
            return;
        }
        showing = true;
        ArrayList<Page> pages = pages();
        Dialog dialog = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        BackgroundView bg = new BackgroundView(act);
        FrameLayout root = new FrameLayout(act);
        root.addView(bg, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        ViewPager pager = new ViewPager(act);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return pages.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View v = pageView(act, pages.get(position), position);
                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }
        });
        pager.setPageTransformer(false, (page, pos) -> {
            float a = Math.max(0f, 1f - Math.abs(pos));
            page.setAlpha(0.2f + 0.8f * a);
            float s = 0.9f + 0.1f * a;
            page.setScaleX(s);
            page.setScaleY(s);
        });
        root.addView(pager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 40, 0, 120));

        // nuqtalar
        LinearLayout dots = new LinearLayout(act);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        ArrayList<View> dotViews = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            View d = new View(act);
            dotViews.add(d);
            dots.addView(d, LayoutHelper.createLinear(8, 8, 4, 0, 4, 0));
        }
        root.addView(dots, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.BOTTOM, 0, 0, 0, 92));

        TextView next = new TextView(act);
        next.setTextColor(0xFF0B2F6B);
        next.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        next.setTypeface(AndroidUtilities.bold());
        next.setGravity(Gravity.CENTER);
        GradientDrawable nb = new GradientDrawable();
        nb.setCornerRadius(AndroidUtilities.dp(26));
        nb.setColor(0xFFFFFFFF);
        next.setBackground(nb);
        root.addView(next, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.BOTTOM, 24, 0, 24, 28));

        TextView skip = new TextView(act);
        skip.setText(MgLang.t("O'tkazib yuborish"));
        skip.setTextColor(0xCCFFFFFF);
        skip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        skip.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        root.addView(skip, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 28, 8, 0));

        Runnable update = () -> {
            int cur = pager.getCurrentItem();
            for (int i = 0; i < dotViews.size(); i++) {
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(i == cur ? 0xFFFFFFFF : 0x55FFFFFF);
                dotViews.get(i).setBackground(g);
            }
            boolean last = cur == pages.size() - 1;
            next.setText(last ? MgLang.t("Boshlash 🚀") : MgLang.t("Keyingi"));
            skip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            Page p = pages.get(cur);
            bg.setColors(p.c1, p.c2);
        };
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                update.run();
                View iv = pager.findViewWithTag("icon" + position);
                if (iv != null) {
                    iv.setScaleX(0.4f);
                    iv.setScaleY(0.4f);
                    iv.animate().scaleX(1f).scaleY(1f).setDuration(500).setInterpolator(new OvershootInterpolator(2.2f)).start();
                }
            }
        });
        update.run();
        Runnable done = () -> {
            MgConfig.setBool(PREF, true);
            dialog.dismiss();
        };
        next.setOnClickListener(v -> {
            int cur = pager.getCurrentItem();
            if (cur < pages.size() - 1) {
                pager.setCurrentItem(cur + 1, true);
            } else {
                done.run();
            }
        });
        skip.setOnClickListener(v -> done.run());

        dialog.setContentView(root);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            win.setWindowAnimations(android.R.style.Animation_Dialog);
            win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setOnDismissListener(d -> {
            showing = false;
            MgConfig.setBool(PREF, true);
        });
        dialog.show();
    }

    private static View pageView(Context ctx, Page p, int position) {
        boolean first = position == 0;
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(24), AndroidUtilities.dp(28), AndroidUtilities.dp(16));
        scroll.addView(box);

        IconView icon = new IconView(ctx, p.icon);
        icon.setTag("icon" + position);
        box.addView(icon, LayoutHelper.createLinear(150, 150, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 20));

        TextView title = new TextView(ctx);
        title.setText(p.title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        box.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        for (String point : p.points) {
            TextView tv = new TextView(ctx);
            SpannableStringBuilder sb = new SpannableStringBuilder("✓  ");
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(point);
            tv.setText(sb);
            tv.setTextColor(0xEEFFFFFF);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            tv.setLineSpacing(AndroidUtilities.dp(2), 1f);
            tv.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(AndroidUtilities.dp(14));
            g.setColor(0x1FFFFFFF);
            tv.setBackground(g);
            box.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        }

        if (first) {
            // keksalar uchun: oddiy rejimni shu yerning o'zida yoqish
            TextView simple = new TextView(ctx);
            simple.setTextColor(0xFFFFFFFF);
            simple.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            simple.setGravity(Gravity.CENTER);
            simple.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(AndroidUtilities.dp(24));
            g.setStroke(AndroidUtilities.dp(1.5f), 0xAAFFFFFF);
            simple.setBackground(g);
            Runnable fill = () -> simple.setText(MgSimple.isEnabled() ? MgLang.t("👓 Oddiy rejim yoqilgan (katta yozuvlar) ✓") : MgLang.t("👓 Katta yozuvlar kerakmi? Oddiy rejimni yoqish"));
            fill.run();
            simple.setOnClickListener(v -> {
                MgSimple.setEnabled(!MgSimple.isEnabled());
                fill.run();
            });
            box.addView(simple, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));
        }
        return scroll;
    }

    /** Katta emoji — yumshoq nur halqasi bilan "nafas oladi" */
    private static class IconView extends View {
        private final String emoji;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long born = SystemClock.elapsedRealtime();

        IconView(Context ctx, String emoji) {
            super(ctx);
            this.emoji = emoji;
            tp.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f;
            float t = (SystemClock.elapsedRealtime() - born) / 1000f;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.4f);
            float r = Math.min(w, h) / 2f;
            p.setShader(new RadialGradient(cx, cy, r, new int[]{(int) (0x40 + 0x30 * pulse) << 24 | 0xFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r, p);
            p.setShader(null);
            p.setColor(0x26FFFFFF);
            c.drawCircle(cx, cy, r * (0.62f + 0.03f * pulse), p);
            tp.setTextSize(r * 0.8f);
            float bob = (float) Math.sin(t * 1.8f) * AndroidUtilities.dp(3);
            c.drawText(emoji, cx, cy + r * 0.28f + bob, tp);
            postInvalidateOnAnimation();
        }
    }

    /** Sahifaga qarab silliq almashadigan gradient fon */
    private static class BackgroundView extends View {
        private int c1 = 0xFF1E6FD9, c2 = 0xFF0B2F6B, f1 = c1, f2 = c2;
        private float progress = 1f;
        private final Paint p = new Paint();
        private ValueAnimator anim;

        BackgroundView(Context ctx) {
            super(ctx);
        }

        void setColors(int a, int b) {
            if (a == c1 && b == c2) {
                return;
            }
            f1 = blend(f1, c1, progress);
            f2 = blend(f2, c2, progress);
            c1 = a;
            c2 = b;
            progress = 0f;
            if (anim != null) {
                anim.cancel();
            }
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(450);
            anim.addUpdateListener(v -> {
                progress = (float) v.getAnimatedValue();
                invalidate();
            });
            anim.start();
        }

        private static int blend(int from, int to, float t) {
            return androidx.core.graphics.ColorUtils.blendARGB(from, to, t);
        }

        @Override
        protected void onDraw(Canvas c) {
            int a = blend(f1, c1, progress), b = blend(f2, c2, progress);
            p.setShader(new LinearGradient(0, 0, getWidth() * 0.3f, getHeight(), a, b, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), getHeight(), p);
        }
    }
}
