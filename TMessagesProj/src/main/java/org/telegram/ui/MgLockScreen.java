/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

import java.util.ArrayList;

/**
 * MilliyGram qulf ekrani: PIN klaviatura yoki grafik kalit, barmoq izi, o'rdak stikeri.
 */
public class MgLockScreen extends Dialog {

    public static final int MODE_VERIFY = 0;
    public static final int MODE_CREATE = 1;

    private static final int PIN_LENGTH = 4;

    private final int mode;
    private final String scope;
    private final String lockType;
    private final Utilities.Callback<Boolean> callback;
    private boolean resultSent;

    private final FrameLayout windowView;
    private final RLottieImageView duckView;
    private final TextView titleView;
    private final TextView subtitleView;
    private LinearLayout dotsLayout;
    private PatternView patternView;

    private final StringBuilder input = new StringBuilder();
    private String firstEntry;

    /** Tekshirish (mavjud kod bilan) */
    public static void verify(Context context, String scope, String title, Utilities.Callback<Boolean> callback) {
        if (context == null) {
            callback.run(false);
            return;
        }
        new MgLockScreen(context, MODE_VERIFY, scope, MgConfig.getLockType(scope), title, callback).show();
    }

    /** Yangi kod yaratish */
    public static void create(Context context, String scope, String type, Utilities.Callback<Boolean> callback) {
        if (context == null) {
            callback.run(false);
            return;
        }
        new MgLockScreen(context, MODE_CREATE, scope, type, null, callback).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private MgLockScreen(Context context, int mode, String scope, String lockType, String title, Utilities.Callback<Boolean> callback) {
        super(context, R.style.TransparentDialog);
        this.mode = mode;
        this.scope = scope;
        this.lockType = lockType;
        this.callback = callback;

        setCancelable(false);
        windowView = new FrameLayout(context);
        windowView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        windowView.setClickable(true);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        windowView.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 0, 36, 0, 16));

        View topSpace = new View(context);
        content.addView(topSpace, LayoutHelper.createLinear(1, 0, 1f));

        duckView = new RLottieImageView(context);
        duckView.setAnimation(R.raw.utyan_passcode, 130, 130);
        duckView.setScaleType(ImageView.ScaleType.CENTER);
        duckView.setOnClickListener(v -> duckView.playAnimation());
        content.addView(duckView, LayoutHelper.createLinear(130, 130, Gravity.CENTER_HORIZONTAL));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 21);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setGravity(Gravity.CENTER);
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 24, 12, 24, 0));

        subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setGravity(Gravity.CENTER);
        content.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 32, 6, 32, 0));

        if (mode == MODE_VERIFY) {
            titleView.setText(TextUtils.isEmpty(title) ? "Qulf" : title);
            subtitleView.setText(isPattern() ? "Grafik kalitni chizing" : "PIN kodni kiriting");
        } else {
            titleView.setText(isPattern() ? "Grafik kalit yarating" : "PIN kod yarating");
            subtitleView.setText(isPattern() ? "Kamida 4 ta nuqtani birlashtiring" : "4 xonali PIN kod o'ylab toping");
        }

        if (isPattern()) {
            patternView = new PatternView(context);
            content.addView(patternView, LayoutHelper.createLinear(280, 280, Gravity.CENTER_HORIZONTAL, 0, 28, 0, 0));
        } else {
            dotsLayout = new LinearLayout(context);
            dotsLayout.setOrientation(LinearLayout.HORIZONTAL);
            dotsLayout.setGravity(Gravity.CENTER);
            content.addView(dotsLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 24, Gravity.CENTER_HORIZONTAL, 0, 24, 0, 0));
            updateDots();
            content.addView(createKeypad(context), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 24, 0, 0));
        }

        View bottomSpace = new View(context);
        content.addView(bottomSpace, LayoutHelper.createLinear(1, 0, 1f));

        LinearLayout bottom = new LinearLayout(context);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        TextView cancel = new TextView(context);
        cancel.setText("Bekor qilish");
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        cancel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        cancel.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        cancel.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ROUNDRECT_6DP));
        cancel.setOnClickListener(v -> finish(false));
        bottom.addView(cancel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        if (isPattern() && canUseFingerprint()) {
            TextView finger = new TextView(context);
            finger.setText("Barmoq izi");
            finger.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            finger.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
            finger.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
            finger.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ROUNDRECT_6DP));
            finger.setOnClickListener(v -> showBiometric());
            bottom.addView(finger, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 0, 0, 0));
        }
        content.addView(bottom, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 8));
    }

    private boolean isPattern() {
        return MgConfig.LOCK_PATTERN.equals(lockType);
    }

    private boolean canUseFingerprint() {
        if (mode != MODE_VERIFY || !MgConfig.isFingerprintEnabled(scope) || Build.VERSION.SDK_INT < 23 || LaunchActivity.instance == null) {
            return false;
        }
        try {
            return BiometricManager.from(getContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable e) {
            return false;
        }
    }

    private void showBiometric() {
        if (!canUseFingerprint()) {
            return;
        }
        try {
            BiometricPrompt prompt = new BiometricPrompt(LaunchActivity.instance, ContextCompat.getMainExecutor(getContext()), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    finish(true);
                }
            });
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("MilliyGram")
                    .setSubtitle("Barmoq izi bilan ochish")
                    .setNegativeButtonText(isPattern() ? "Grafik kalit" : "PIN kod")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
            prompt.authenticate(info);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    // ---------- PIN klaviatura ----------

    private View createKeypad(Context context) {
        LinearLayout keypad = new LinearLayout(context);
        keypad.setOrientation(LinearLayout.VERTICAL);
        String[] letters = {"", "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ"};
        int[][] rows = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {-1, 0, -2}};
        for (int[] row : rows) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int key : row) {
                View button;
                if (key >= 0) {
                    button = createDigitButton(context, key, letters[key]);
                } else if (key == -2) {
                    button = createIconButton(context, R.drawable.filled_clear, v -> {
                        if (input.length() > 0) {
                            input.deleteCharAt(input.length() - 1);
                            haptic(v);
                            updateDots();
                        }
                    });
                } else if (canUseFingerprint()) {
                    button = createIconButton(context, R.drawable.fingerprint, v -> showBiometric());
                } else {
                    button = new View(context);
                }
                rowLayout.addView(button, LayoutHelper.createLinear(76, 76, 10, 6, 10, 6));
            }
            keypad.addView(rowLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }
        return keypad;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private View createDigitButton(Context context, int digit, String letters) {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(76), Theme.getColor(Theme.key_windowBackgroundGray), Theme.getColor(Theme.key_listSelector)));
        TextView d = new TextView(context);
        d.setText(String.valueOf(digit));
        d.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        d.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        d.setGravity(Gravity.CENTER);
        button.addView(d, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        if (!TextUtils.isEmpty(letters)) {
            TextView l = new TextView(context);
            l.setText(letters);
            l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            l.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            l.setGravity(Gravity.CENTER);
            button.addView(l, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, -4, 0, 0));
        }
        button.setOnClickListener(v -> {
            if (input.length() >= 8) {
                return;
            }
            haptic(v);
            input.append(digit);
            updateDots();
            onPinChanged();
        });
        return button;
    }

    private View createIconButton(Context context, int icon, View.OnClickListener listener) {
        ImageView button = new ImageView(context);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setImageResource(icon);
        button.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        button.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(76), 0, Theme.getColor(Theme.key_listSelector)));
        button.setOnClickListener(listener);
        return button;
    }

    private int expectedLength() {
        if (mode == MODE_CREATE) {
            return PIN_LENGTH;
        }
        int len = MgConfig.getPinLength(scope);
        return len > 0 ? len : 0;
    }

    private void updateDots() {
        if (dotsLayout == null) {
            return;
        }
        dotsLayout.removeAllViews();
        int expected = expectedLength();
        int count = Math.max(expected > 0 ? expected : 4, input.length());
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
        int empty = Theme.getColor(Theme.key_windowBackgroundWhiteHintText);
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            dot.setBackground(circle(i < input.length() ? accent : empty));
            dotsLayout.addView(dot, LayoutHelper.createLinear(14, 14, 9, 0, 9, 0));
        }
    }

    private void onPinChanged() {
        int expected = expectedLength();
        String pin = input.toString();
        if (mode == MODE_VERIFY) {
            if (expected > 0) {
                if (pin.length() == expected) {
                    submit(pin);
                }
            } else if (pin.length() >= 4) {
                // eski versiyada saqlangan PIN (uzunligi noma'lum)
                if (MgConfig.checkLock(scope, pin)) {
                    MgConfig.setPinLength(scope, pin.length());
                    finish(true);
                } else if (pin.length() >= 8) {
                    submit(pin);
                }
            }
        } else if (pin.length() == PIN_LENGTH) {
            submit(pin);
        }
    }

    private void submit(String secret) {
        if (mode == MODE_VERIFY) {
            String stored = isPattern() ? MgConfig.PATTERN_PREFIX + secret : secret;
            if (MgConfig.checkLock(scope, stored)) {
                finish(true);
            } else {
                onError("Noto'g'ri. Qayta urinib ko'ring");
            }
        } else {
            if (firstEntry == null) {
                firstEntry = secret;
                clearInput();
                subtitleView.setText(isPattern() ? "Grafik kalitni yana bir marta chizing" : "PIN kodni takrorlang");
                duckView.playAnimation();
            } else if (firstEntry.equals(secret)) {
                MgConfig.setLock(scope, lockType, secret);
                finish(true);
            } else {
                firstEntry = null;
                onError("Mos kelmadi. Qaytadan boshlang");
            }
        }
    }

    private void clearInput() {
        input.setLength(0);
        updateDots();
        if (patternView != null) {
            patternView.reset();
        }
    }

    private void onError(String text) {
        subtitleView.setText(text);
        subtitleView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        View target = dotsLayout != null ? dotsLayout : patternView;
        if (target != null) {
            AndroidUtilities.shakeViewSpring(target, 5, () -> {
                clearInput();
                subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            });
        } else {
            clearInput();
        }
        try {
            windowView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } catch (Throwable ignore) {
        }
    }

    private void haptic(View v) {
        if (MgConfig.isLockVibrate()) {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            } catch (Throwable ignore) {
            }
        }
    }

    private void finish(boolean ok) {
        if (resultSent) {
            return;
        }
        resultSent = true;
        try {
            super.dismiss();
        } catch (Throwable ignore) {
        }
        callback.run(ok);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setWindowAnimations(R.style.DialogNoAnimation);
        setContentView(windowView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.FILL;
        params.dimAmount = 0;
        params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        params.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        window.setAttributes(params);
    }

    @Override
    public void show() {
        super.show();
        windowView.setAlpha(0f);
        windowView.setScaleX(1.05f);
        windowView.setScaleY(1.05f);
        windowView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        AndroidUtilities.runOnUIThread(duckView::playAnimation, 150);
        if (canUseFingerprint()) {
            AndroidUtilities.runOnUIThread(this::showBiometric, 350);
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                finish(false);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    // ---------- Grafik kalit ----------

    private class PatternView extends View {

        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Integer> selected = new ArrayList<>();
        private float touchX, touchY;
        private boolean tracking;

        PatternView(Context context) {
            super(context);
            dotPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
            activePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
            linePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
            linePaint.setAlpha(170);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(AndroidUtilities.dp(5));
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setStrokeJoin(Paint.Join.ROUND);
        }

        void reset() {
            selected.clear();
            tracking = false;
            invalidate();
        }

        private float cx(int i) {
            float cell = getWidth() / 3f;
            return cell * (i % 3) + cell / 2f;
        }

        private float cy(int i) {
            float cell = getHeight() / 3f;
            return cell * (i / 3) + cell / 2f;
        }

        private int hit(float x, float y) {
            float r = Math.min(getWidth(), getHeight()) / 3f * 0.36f;
            for (int i = 0; i < 9; i++) {
                float dx = x - cx(i), dy = y - cy(i);
                if (dx * dx + dy * dy <= r * r) {
                    return i;
                }
            }
            return -1;
        }

        private void add(int i) {
            if (i < 0 || selected.contains(i)) {
                return;
            }
            if (!selected.isEmpty()) {
                // oraliqdagi nuqtani avtomatik qo'shish (masalan 0 -> 2 da 1)
                int last = selected.get(selected.size() - 1);
                int lr = last / 3, lc = last % 3, r = i / 3, c = i % 3;
                if ((lr + r) % 2 == 0 && (lc + c) % 2 == 0) {
                    int mid = ((lr + r) / 2) * 3 + (lc + c) / 2;
                    if (!selected.contains(mid)) {
                        selected.add(mid);
                    }
                }
            }
            selected.add(i);
            haptic(this);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            touchX = event.getX();
            touchY = event.getY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    selected.clear();
                    tracking = true;
                    add(hit(touchX, touchY));
                    getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (tracking) {
                        add(hit(touchX, touchY));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    tracking = false;
                    if (selected.size() >= 4) {
                        StringBuilder sb = new StringBuilder();
                        for (int i : selected) {
                            sb.append(i);
                        }
                        submit(sb.toString());
                    } else if (!selected.isEmpty()) {
                        onError("Kamida 4 ta nuqtani birlashtiring");
                    }
                    break;
            }
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            boolean invisible = mode == MODE_VERIFY && MgConfig.isPatternInvisible(scope);
            if (!invisible && !selected.isEmpty()) {
                Path path = new Path();
                for (int k = 0; k < selected.size(); k++) {
                    int i = selected.get(k);
                    if (k == 0) {
                        path.moveTo(cx(i), cy(i));
                    } else {
                        path.lineTo(cx(i), cy(i));
                    }
                }
                if (tracking) {
                    path.lineTo(touchX, touchY);
                }
                canvas.drawPath(path, linePaint);
            }
            float r = AndroidUtilities.dp(7);
            float rActive = AndroidUtilities.dp(11);
            for (int i = 0; i < 9; i++) {
                boolean active = !invisible && selected.contains(i);
                canvas.drawCircle(cx(i), cy(i), active ? rActive : r, active ? activePaint : dotPaint);
            }
        }
    }
}
