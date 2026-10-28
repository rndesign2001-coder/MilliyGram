/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgSimple;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Random;

/**
 * Birinchi ochilishdagi tanishtiruv: logotip intro'si, so'ng MilliyGram imkoniyatlari sahifalari.
 * Motion: aurora fon + yulduzchalar (parallaks), aylanuvchi halqali qahramon belgi,
 * matnlarning navbatma-navbat kirishi, uzluksiz rang almashinuvi va yakunda konfetti.
 */
public class MgOnboarding {

    private static final String PREF = "onboard_v3";
    private static boolean showing;

    private static final class Page {
        final String icon; // null — MilliyGram logotipi
        final String title;
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
        list.add(new Page(null, MgLang.t("MilliyGram'ga xush kelibsiz!"), 0xFF1E6FD9, 0xFF050F33,
                MgLang.t("Telegram'ning barcha imkoniyatlari va o'zbek foydalanuvchilari uchun qo'shimchalar"),
                MgLang.t("Sozlamalar → MilliyGram bo'limida hammasi bir joyda"),
                MgLang.t("Ma'lumotlaringiz faqat telefoningizda qoladi")));
        list.add(new Page("👻", MgLang.t("Sharpa rejimi"), 0xFF3F4C6B, 0xFF0B1020,
                MgLang.t("Bosh ekrandagi 👻 tugma bilan bir bosishda yoqing"),
                MgLang.t("O'qilganlik belgisi, «yozmoqda…» va onlayn holat yuborilmaydi"),
                MgLang.t("Hikoyalarni sezdirmasdan ko'ring"),
                MgLang.t("Yangi qurilmadan kirilsa darhol ogohlantirish")));
        list.add(new Page("🔒", MgLang.t("Maxfiylik"), 0xFF6B3FD9, 0xFF1E0A4D,
                MgLang.t("Chatni PIN yoki grafik kalit bilan qulflash (chat → ⋮)"),
                MgLang.t("Yashirin bo'lim: qidiruv tugmasini uzoq bosing"),
                MgLang.t("Telefonni silkitsangiz, maxfiy chatlar darhol yopiladi"),
                MgLang.t("Firibgarlik va APK fayllardan ogohlantirish")));
        list.add(new Page("🕋", MgLang.t("Namoz, qibla va ob-havo"), 0xFF12825A, 0xFF03301F,
                MgLang.t("Chatlar ro'yxati tepasida keyingi namozgacha qolgan vaqt"),
                MgLang.t("Uni bossangiz — barcha vaqtlar, ob-havo va jonli qibla kompasi"),
                MgLang.t("Namoz vaqtini eslatish, lotin ↔ kirill o'girish, tarjima")));
        list.add(new Page("📢", MgLang.t("Kanal adminlari uchun"), 0xFFE0822A, 0xFF5A2304,
                MgLang.t("☁ Bulutcha — postni bir bosishda Saqlangan xabarlarga"),
                MgLang.t("Yuborish tugmasini uzoq bosing → reklama postini 24/48 soatdan keyin avtomatik o'chirish"),
                MgLang.t("Bitta postni ko'p kanalga tahrirlab uzatish"),
                MgLang.t("«So'nggi amallar»ni faylga eksport qilish")));
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

    private static int dp(float v) {
        return AndroidUtilities.dp(v);
    }

    /** Bir sahifaning animatsiya qilinadigan qismlari */
    private static final class PageHolder {
        View root;
        View heroWrap, hero, textWrap, title;
        final ArrayList<View> points = new ArrayList<>();
        boolean played;

        void hide() {
            hero.setAlpha(0f);
            hero.setScaleX(0.3f);
            hero.setScaleY(0.3f);
            hero.setRotation(-35f);
            title.setAlpha(0f);
            title.setTranslationY(dp(28));
            for (int i = 0; i < points.size(); i++) {
                View v = points.get(i);
                v.setAlpha(0f);
                v.setTranslationY(dp(36));
                v.setTranslationX(i % 2 == 0 ? -dp(24) : dp(24));
                v.setScaleX(0.94f);
                v.setScaleY(0.94f);
            }
        }

        void play() {
            if (played) {
                return;
            }
            played = true;
            hero.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f).setDuration(700)
                    .setInterpolator(new OvershootInterpolator(1.8f)).start();
            title.animate().alpha(1f).translationY(0).setStartDelay(140).setDuration(520)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start();
            for (int i = 0; i < points.size(); i++) {
                points.get(i).animate().alpha(1f).translationY(0).translationX(0).scaleX(1f).scaleY(1f)
                        .setStartDelay(260 + i * 85L).setDuration(560)
                        .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start();
            }
        }
    }

    public static void show(Activity act) {
        if (act == null || showing) {
            return;
        }
        showing = true;
        final ArrayList<Page> pages = pages();
        final PageHolder[] holders = new PageHolder[pages.size()];
        final boolean[] introDone = {false};

        Dialog dialog = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(act);
        AuroraView bg = new AuroraView(act);
        bg.setColors(pages.get(0).c1, pages.get(0).c2);
        root.addView(bg, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        FrameLayout content = new FrameLayout(act);
        content.setAlpha(0f);
        root.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        ViewPager pager = new ViewPager(act);
        pager.setOffscreenPageLimit(1);
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
                PageHolder h = pageView(act, pages.get(position), position);
                holders[position] = h;
                h.hide();
                if (position == pager.getCurrentItem() && introDone[0]) {
                    h.play();
                }
                container.addView(h.root);
                return h.root;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
                if (holders[position] != null && holders[position].root == object) {
                    holders[position] = null;
                }
            }
        });
        // parallaks: belgi sekinroq, matn tezroq siljiydi; chetga ketayotgan belgi aylanadi
        pager.setPageTransformer(false, (page, pos) -> {
            Object tag = page.getTag();
            if (!(tag instanceof PageHolder)) {
                return;
            }
            PageHolder h = (PageHolder) tag;
            float w = page.getWidth();
            float ap = Math.min(1f, Math.abs(pos));
            h.heroWrap.setTranslationX(pos * w * 0.45f);
            h.heroWrap.setRotation(pos * -28f);
            float s = 1f - 0.35f * ap;
            h.heroWrap.setScaleX(s);
            h.heroWrap.setScaleY(s);
            h.textWrap.setTranslationX(-pos * w * 0.18f);
            page.setAlpha(1f - 0.65f * ap);
        });
        content.addView(pager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 44, 0, 124));

        IndicatorView indicator = new IndicatorView(act, pages.size());
        content.addView(indicator, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.BOTTOM, 0, 0, 0, 94));

        ShineButton next = new ShineButton(act);
        content.addView(next, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 54, Gravity.BOTTOM, 24, 0, 24, 28));

        TextView skip = new TextView(act);
        skip.setText(MgLang.t("O'tkazib yuborish"));
        skip.setTextColor(0xCCFFFFFF);
        skip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        skip.setPadding(dp(16), dp(10), dp(16), dp(10));
        content.addView(skip, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 8, 8, 0));

        ConfettiView confetti = new ConfettiView(act);
        root.addView(confetti, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        Runnable updateLabels = () -> {
            int cur = pager.getCurrentItem();
            boolean last = cur == pages.size() - 1;
            next.setLabel(last ? MgLang.t("Boshlash 🚀") : MgLang.t("Keyingi"));
            next.setPulse(last);
            skip.animate().alpha(last ? 0f : 1f).setDuration(200).start();
            skip.setClickable(!last);
        };
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float offset, int offsetPx) {
                float p = position + offset;
                Page a = pages.get(position);
                Page b = pages.get(Math.min(pages.size() - 1, position + 1));
                bg.setColors(ColorUtils.blendARGB(a.c1, b.c1, offset), ColorUtils.blendARGB(a.c2, b.c2, offset));
                bg.setScroll(p);
                indicator.setProgress(p);
                if (!introDone[0]) {
                    return;
                }
                // kelayotgan sahifa yarim ko'ringanda kirish animatsiyasi boshlanadi
                if (offset > 0.3f && position + 1 < holders.length && holders[position + 1] != null) {
                    holders[position + 1].play();
                }
                if (offset < 0.7f && holders[position] != null) {
                    holders[position].play();
                }
            }

            @Override
            public void onPageSelected(int position) {
                updateLabels.run();
                if (introDone[0] && holders[position] != null) {
                    holders[position].play();
                }
                pager.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        });
        updateLabels.run();

        final boolean[] closing = {false};
        Runnable close = () -> {
            if (closing[0]) {
                return;
            }
            closing[0] = true;
            MgConfig.setBool(PREF, true);
            root.animate().alpha(0f).scaleX(1.06f).scaleY(1.06f).setDuration(320).withEndAction(() -> {
                try {
                    dialog.dismiss();
                } catch (Throwable ignore) {
                }
            }).start();
        };
        next.setOnClickListener(v -> {
            int cur = pager.getCurrentItem();
            if (cur < pages.size() - 1) {
                pager.setCurrentItem(cur + 1, true);
            } else if (!closing[0]) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                int[] loc = new int[2], rl = new int[2];
                v.getLocationOnScreen(loc);
                root.getLocationOnScreen(rl);
                confetti.burst(loc[0] - rl[0] + v.getWidth() / 2f, loc[1] - rl[1] + v.getHeight() / 2f);
                AndroidUtilities.runOnUIThread(close, 900);
            }
        });
        skip.setOnClickListener(v -> close.run());

        // ---- logotip intro'si ----
        FrameLayout intro = new FrameLayout(act);
        RingsView rings = new RingsView(act);
        intro.addView(rings, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        LinearLayout introBox = new LinearLayout(act);
        introBox.setOrientation(LinearLayout.VERTICAL);
        introBox.setGravity(Gravity.CENTER_HORIZONTAL);
        intro.addView(introBox, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        ImageView logo = new ImageView(act);
        logo.setImageResource(R.mipmap.ic_launcher_round);
        logo.setScaleX(0f);
        logo.setScaleY(0f);
        logo.setAlpha(0f);
        logo.setRotation(-120f);
        introBox.addView(logo, LayoutHelper.createLinear(128, 128, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 22));

        LinearLayout letters = new LinearLayout(act);
        letters.setOrientation(LinearLayout.HORIZONTAL);
        letters.setClipChildren(false);
        introBox.setClipChildren(false);
        intro.setClipChildren(false);
        String name = "MilliyGram";
        ArrayList<View> letterViews = new ArrayList<>();
        for (int i = 0; i < name.length(); i++) {
            TextView l = new TextView(act);
            l.setText(String.valueOf(name.charAt(i)));
            l.setTextColor(0xFFFFFFFF);
            l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 38);
            l.setTypeface(AndroidUtilities.bold());
            l.setAlpha(0f);
            l.setTranslationY(dp(26));
            l.setScaleX(0.4f);
            l.setScaleY(0.4f);
            letters.addView(l, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            letterViews.add(l);
        }
        introBox.addView(letters, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        TextView tagline = new TextView(act);
        tagline.setText(MgLang.t("O'zbekcha • Tez • Xavfsiz"));
        tagline.setTextColor(0xCCFFFFFF);
        tagline.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        tagline.setLetterSpacing(0.08f);
        tagline.setAlpha(0f);
        tagline.setTranslationY(dp(12));
        introBox.addView(tagline, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 10, 0, 0));
        root.addView(intro, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        Runnable finishIntro = () -> {
            if (introDone[0]) {
                return;
            }
            introDone[0] = true;
            intro.setClickable(false);
            intro.animate().alpha(0f).scaleX(1.35f).scaleY(1.35f).setDuration(450)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                    .withEndAction(() -> root.removeView(intro)).start();
            content.setScaleX(0.92f);
            content.setScaleY(0.92f);
            content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start();
            next.setTranslationY(dp(90));
            next.animate().translationY(0).setStartDelay(200).setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.4f)).start();
            int cur = pager.getCurrentItem();
            if (holders[cur] != null) {
                holders[cur].play();
            }
        };
        intro.setOnClickListener(v -> finishIntro.run());
        Runnable startIntro = () -> {
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f).setDuration(750)
                    .setInterpolator(new OvershootInterpolator(2.2f)).start();
            AndroidUtilities.runOnUIThread(() -> {
                int[] a = new int[2], b = new int[2];
                logo.getLocationOnScreen(a);
                rings.getLocationOnScreen(b);
                rings.start(a[0] - b[0] + logo.getWidth() / 2f, a[1] - b[1] + logo.getHeight() / 2f);
                logo.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }, 260);
            for (int i = 0; i < letterViews.size(); i++) {
                letterViews.get(i).animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                        .setStartDelay(480 + i * 50L).setDuration(460)
                        .setInterpolator(new OvershootInterpolator(2.4f)).start();
            }
            tagline.animate().alpha(1f).translationY(0).setStartDelay(1050).setDuration(500)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start();
            AndroidUtilities.runOnUIThread(finishIntro, 2200);
        };

        // tizim panellari ostida tugmalar qolib ketmasligi uchun
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        content.setPadding(0, AndroidUtilities.statusBarHeight, 0, AndroidUtilities.navigationBarHeight);

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
        root.post(startIntro);
    }

    private static PageHolder pageView(Context ctx, Page p, int position) {
        PageHolder h = new PageHolder();
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setTag(h);
        h.root = scroll;
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(28), dp(20), dp(28), dp(16));
        scroll.addView(box);

        FrameLayout heroWrap = new FrameLayout(ctx);
        HeroView hero = new HeroView(ctx, p.icon);
        heroWrap.addView(hero, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        box.addView(heroWrap, LayoutHelper.createLinear(176, 176, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 18));
        h.heroWrap = heroWrap;
        h.hero = hero;

        LinearLayout textWrap = new LinearLayout(ctx);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        box.addView(textWrap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        h.textWrap = textWrap;

        TextView title = new TextView(ctx);
        title.setText(p.title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 27);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(dp(12), 0, dp(2), 0x55000000);
        textWrap.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));
        h.title = title;

        for (String point : p.points) {
            TextView tv = new TextView(ctx);
            SpannableStringBuilder sb = new SpannableStringBuilder("✦  ");
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(point);
            tv.setText(sb);
            tv.setTextColor(0xF2FFFFFF);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            tv.setLineSpacing(dp(2), 1f);
            tv.setPadding(dp(16), dp(12), dp(16), dp(12));
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x2EFFFFFF, 0x12FFFFFF});
            g.setCornerRadius(dp(16));
            g.setStroke(1, 0x33FFFFFF);
            tv.setBackground(g);
            textWrap.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
            h.points.add(tv);
        }

        if (position == 0) {
            // keksalar uchun: oddiy rejimni shu yerning o'zida yoqish
            TextView simple = new TextView(ctx);
            simple.setTextColor(0xFFFFFFFF);
            simple.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            simple.setGravity(Gravity.CENTER);
            simple.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(24));
            g.setStroke(dp(1.5f), 0xAAFFFFFF);
            simple.setBackground(g);
            Runnable fill = () -> simple.setText(MgSimple.isEnabled() ? MgLang.t("👓 Oddiy rejim yoqilgan (katta yozuvlar) ✓") : MgLang.t("👓 Katta yozuvlar kerakmi? Oddiy rejimni yoqish"));
            fill.run();
            simple.setOnClickListener(v -> {
                MgSimple.setEnabled(!MgSimple.isEnabled());
                fill.run();
            });
            textWrap.addView(simple, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));
            h.points.add(simple);
        }
        return h;
    }

    /** Qahramon belgi: nafas oluvchi nur, aylanuvchi halqa, orbitadagi nuqtalar */
    private static class HeroView extends View {
        private final String emoji;
        private final Drawable logo;
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final long born = SystemClock.elapsedRealtime();

        HeroView(Context ctx, String emoji) {
            super(ctx);
            this.emoji = emoji;
            this.logo = emoji == null ? ctx.getResources().getDrawable(R.mipmap.ic_launcher_round) : null;
            tp.setTextAlign(Paint.Align.CENTER);
            disc.setColor(0x24FFFFFF);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeCap(Paint.Cap.ROUND);
            ring.setStrokeWidth(dp(3));
            dot.setColor(0xFFFFFFFF);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f;
            glow.setShader(new RadialGradient(cx, cy, r, new int[]{0x88FFFFFF, 0x22FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            ring.setShader(new SweepGradient(cx, cy, new int[]{0x00FFFFFF, 0x00FFFFFF, 0xEEFFFFFF}, new float[]{0f, 0.35f, 1f}));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f;
            float r = Math.min(w, h) / 2f;
            float t = (SystemClock.elapsedRealtime() - born) / 1000f;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.4f);

            glow.setAlpha((int) (150 + 105 * pulse));
            c.drawCircle(cx, cy, r, glow);
            c.drawCircle(cx, cy, r * (0.6f + 0.03f * pulse), disc);

            c.save();
            c.rotate(t * 110f, cx, cy);
            float rr = r * 0.78f;
            oval.set(cx - rr, cy - rr, cx + rr, cy + rr);
            c.drawArc(oval, 0, 300, false, ring);
            c.restore();

            for (int i = 0; i < 3; i++) {
                double ang = Math.toRadians(-t * (55 + i * 18) + i * 120);
                float orb = r * (0.86f + 0.05f * i);
                float x = cx + (float) Math.cos(ang) * orb, y = cy + (float) Math.sin(ang) * orb;
                dot.setAlpha(150 + 35 * i);
                c.drawCircle(x, y, dp(2.5f + i), dot);
            }

            float bob = (float) Math.sin(t * 1.8f) * dp(3);
            if (logo != null) {
                int lr = (int) (r * 0.52f);
                logo.setBounds((int) cx - lr, (int) (cy - lr + bob), (int) cx + lr, (int) (cy + lr + bob));
                logo.draw(c);
            } else {
                tp.setTextSize(r * 0.78f);
                c.drawText(emoji, cx, cy + r * 0.27f + bob, tp);
            }
            postInvalidateOnAnimation();
        }
    }

    /** Aurora fon: gradient + suzib yuruvchi nurli dog'lar + miltillovchi yulduzchalar */
    private static class AuroraView extends View {
        private static final int STARS = 48;
        private int c1, c2;
        private boolean dirty = true;
        private float scroll;
        private final Paint bgPaint = new Paint();
        private final Paint blob = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] sx = new float[STARS], sy = new float[STARS], sz = new float[STARS], sp = new float[STARS];
        private final long born = SystemClock.elapsedRealtime();
        private float blobR;

        AuroraView(Context ctx) {
            super(ctx);
            Random rnd = new Random(2026);
            for (int i = 0; i < STARS; i++) {
                sx[i] = rnd.nextFloat();
                sy[i] = rnd.nextFloat();
                sz[i] = 0.3f + rnd.nextFloat() * 0.7f;
                sp[i] = rnd.nextFloat() * 6.28f;
            }
            star.setColor(0xFFFFFFFF);
        }

        void setColors(int a, int b) {
            if (a != c1 || b != c2) {
                c1 = a;
                c2 = b;
                dirty = true;
                invalidate();
            }
        }

        void setScroll(float s) {
            scroll = s;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            blobR = Math.max(w, h) * 0.45f;
            blob.setShader(new RadialGradient(0, 0, blobR, new int[]{0x66FFFFFF, 0x1FFFFFFF, 0x00FFFFFF}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            dirty = true;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            if (dirty) {
                dirty = false;
                bgPaint.setShader(new LinearGradient(0, 0, w * 0.35f, h, c1, c2, Shader.TileMode.CLAMP));
            }
            c.drawRect(0, 0, w, h, bgPaint);
            float t = (SystemClock.elapsedRealtime() - born) / 1000f;

            for (int i = 0; i < 3; i++) {
                float bx = w * (0.15f + 0.7f * (0.5f + 0.5f * (float) Math.sin(t * 0.27f + i * 2.1f))) - scroll * w * 0.12f * (i + 1);
                bx = wrap(bx, w, blobR);
                float by = h * (0.12f + 0.36f * i) + h * 0.06f * (float) Math.sin(t * 0.41f + i);
                float s = 0.75f + 0.3f * (float) Math.sin(t * 0.5f + i * 1.3f);
                blob.setAlpha(i == 1 ? 110 : 170);
                c.save();
                c.translate(bx, by);
                c.scale(s, s * 0.8f);
                c.drawCircle(0, 0, blobR, blob);
                c.restore();
            }

            for (int i = 0; i < STARS; i++) {
                float y = frac(sy[i] - t * 0.012f * sz[i]) * h;
                float x = frac(sx[i] - scroll * 0.06f * sz[i]) * w;
                float tw = 0.5f + 0.5f * (float) Math.sin(t * 2.6f * sz[i] + sp[i]);
                star.setAlpha((int) (40 + 190 * tw * sz[i]));
                c.drawCircle(x, y, dp(0.8f + 1.4f * sz[i]), star);
            }
            postInvalidateOnAnimation();
        }

        private static float frac(float v) {
            v = v % 1f;
            return v < 0 ? v + 1f : v;
        }

        private static float wrap(float x, float w, float pad) {
            float span = w + pad * 2;
            float v = (x + pad) % span;
            if (v < 0) {
                v += span;
            }
            return v - pad;
        }
    }

    /** Pastdagi "kapsula" indikator: faol nuqta suzib cho'ziladi */
    private static class IndicatorView extends View {
        private final int count;
        private float progress;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        IndicatorView(Context ctx, int count) {
            super(ctx);
            this.count = count;
        }

        void setProgress(float v) {
            progress = v;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float base = dp(8), extra = dp(20), gap = dp(8);
            float total = count * base + extra + (count - 1) * gap;
            float x = (getWidth() - total) / 2f, cy = getHeight() / 2f;
            for (int i = 0; i < count; i++) {
                float a = Math.max(0f, 1f - Math.abs(progress - i));
                float wdt = base + extra * a;
                p.setColor(ColorUtils.setAlphaComponent(0xFFFFFFFF, (int) (85 + 170 * a)));
                rect.set(x, cy - base / 2f, x + wdt, cy + base / 2f);
                c.drawRoundRect(rect, base / 2f, base / 2f, p);
                x += wdt + gap;
            }
        }
    }

    /** Gradientli tugma: ustidan yaltiroq nur o'tib turadi, bosilganda siqiladi */
    @SuppressLint("ViewConstructor")
    private static class ShineButton extends TextView {
        private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix m = new Matrix();
        private final Path clip = new Path();
        private final RectF rect = new RectF();
        private final long born = SystemClock.elapsedRealtime();
        private boolean pulse;

        @SuppressLint("ClickableViewAccessibility")
        ShineButton(Context ctx) {
            super(ctx);
            setTextColor(0xFFFFFFFF);
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            setTypeface(AndroidUtilities.bold());
            setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF06B6D4, 0xFF3B82F6, 0xFF8B5CF6});
            bg.setCornerRadius(dp(27));
            setBackground(bg);
            setElevation(dp(6));
            shine.setShader(new LinearGradient(0, 0, dp(70), 0, new int[]{0x00FFFFFF, 0x99FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            setOnTouchListener((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).start();
                } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new OvershootInterpolator(3f)).start();
                }
                return false;
            });
        }

        void setLabel(CharSequence s) {
            if (s.toString().contentEquals(getText())) {
                return;
            }
            setText(s);
            setAlpha(0.4f);
            animate().alpha(1f).setDuration(220).start();
        }

        void setPulse(boolean v) {
            pulse = v;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            clip.reset();
            rect.set(0, 0, w, h);
            clip.addRoundRect(rect, dp(27), dp(27), Path.Direction.CW);
        }

        @Override
        protected void onDraw(Canvas c) {
            float t = (SystemClock.elapsedRealtime() - born) / 1000f;
            if (pulse) {
                float s = 1f + 0.025f * (float) Math.sin(t * 5f);
                c.save();
                c.scale(s, s, getWidth() / 2f, getHeight() / 2f);
                super.onDraw(c);
                c.restore();
            } else {
                super.onDraw(c);
            }
            float cycle = 2.6f, ph = (t % cycle) / 1.3f;
            if (ph <= 1f) {
                float band = dp(70);
                float x = -band + (getWidth() + band * 2) * ph;
                m.setTranslate(x, 0);
                m.postSkew(-0.3f, 0, x, getHeight() / 2f);
                shine.getShader().setLocalMatrix(m);
                c.save();
                c.clipPath(clip);
                c.drawRect(0, 0, getWidth(), getHeight(), shine);
                c.restore();
            }
            postInvalidateOnAnimation();
        }
    }

    /** Intro: logotipdan tarqaluvchi to'lqin halqalari va uchqunlar */
    private static class RingsView extends View {
        private float cx, cy;
        private long start = -1;
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint spark = new Paint(Paint.ANTI_ALIAS_FLAG);

        RingsView(Context ctx) {
            super(ctx);
            ring.setStyle(Paint.Style.STROKE);
            ring.setColor(0xFFFFFFFF);
            spark.setColor(0xFFFFFFFF);
            spark.setStrokeCap(Paint.Cap.ROUND);
        }

        void start(float x, float y) {
            cx = x;
            cy = y;
            start = SystemClock.elapsedRealtime();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            if (start < 0) {
                return;
            }
            float t = (SystemClock.elapsedRealtime() - start) / 1000f;
            float maxR = Math.max(getWidth(), getHeight()) * 0.75f;
            boolean alive = false;
            for (int i = 0; i < 3; i++) {
                float p = (t - i * 0.22f) / 1.1f;
                if (p < 0f || p > 1f) {
                    alive |= p < 0f;
                    continue;
                }
                alive = true;
                float e = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(p);
                ring.setStrokeWidth(dp(3) * (1f - p) + 1);
                ring.setAlpha((int) (200 * (1f - p)));
                c.drawCircle(cx, cy, dp(64) + maxR * e, ring);
            }
            float sp = t / 0.8f;
            if (sp <= 1f) {
                alive = true;
                float e = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(sp);
                spark.setAlpha((int) (255 * (1f - sp)));
                spark.setStrokeWidth(dp(3));
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30 + 15);
                    float r1 = dp(70) + dp(60) * e, r2 = r1 + dp(14) * (1f - sp);
                    c.drawLine(cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1,
                            cx + (float) Math.cos(a) * r2, cy + (float) Math.sin(a) * r2, spark);
                }
            }
            if (alive) {
                postInvalidateOnAnimation();
            }
        }
    }

    /** Yakuniy konfetti portlashi */
    private static class ConfettiView extends View {
        private static final int N = 120;
        private static final int[] COLORS = {0xFF22D3EE, 0xFF3B82F6, 0xFF8B5CF6, 0xFFF59E0B, 0xFF22C55E, 0xFFEF4444, 0xFFFFFFFF};
        private final float[] vx = new float[N], vy = new float[N], rot = new float[N], vr = new float[N], size = new float[N];
        private final int[] col = new int[N];
        private float ox, oy;
        private long start = -1;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        ConfettiView(Context ctx) {
            super(ctx);
        }

        void burst(float x, float y) {
            ox = x;
            oy = y;
            Random r = new Random();
            for (int i = 0; i < N; i++) {
                double a = Math.toRadians(-90 + (r.nextFloat() - 0.5f) * 150);
                float speed = dp(500 + r.nextFloat() * 900);
                vx[i] = (float) Math.cos(a) * speed;
                vy[i] = (float) Math.sin(a) * speed;
                rot[i] = r.nextFloat() * 360;
                vr[i] = (r.nextFloat() - 0.5f) * 900;
                size[i] = dp(5 + r.nextFloat() * 6);
                col[i] = COLORS[r.nextInt(COLORS.length)];
            }
            start = SystemClock.elapsedRealtime();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            if (start < 0) {
                return;
            }
            float t = (SystemClock.elapsedRealtime() - start) / 1000f;
            if (t > 1.6f) {
                return;
            }
            float g = dp(1400);
            int alpha = (int) (255 * Math.min(1f, Math.max(0f, (1.6f - t) / 0.5f)));
            for (int i = 0; i < N; i++) {
                float drag = 1f / (1f + t * 1.4f);
                float x = ox + vx[i] * t * drag;
                float y = oy + vy[i] * t * drag + 0.5f * g * t * t;
                p.setColor(col[i]);
                p.setAlpha(alpha);
                c.save();
                c.translate(x, y);
                c.rotate(rot[i] + vr[i] * t);
                float s = size[i];
                c.drawRect(-s / 2f, -s / 4f, s / 2f, s / 4f, p);
                c.restore();
            }
            postInvalidateOnAnimation();
        }
    }
}
