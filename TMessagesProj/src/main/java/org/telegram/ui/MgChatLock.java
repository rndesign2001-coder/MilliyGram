/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Chatlarni PIN kod bilan qulflash.
 */
public class MgChatLock {

    public static final int MENU_ID = 9001;
    public static final int MENU_ID_HIDE = 9002;
    public static final int MENU_ID_COPY_ID = 9003;

    private static EditTextBoldCursor createPinField(Context context) {
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(22));
        editText.setCursorWidth(1.5f);
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        editText.setGravity(Gravity.CENTER);
        editText.setSingleLine(true);
        editText.setHint("• • • •");
        editText.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        return editText;
    }

    private static View wrap(Context context, View view) {
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(view, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));
        return frameLayout;
    }

    /** Qulf ekranini ko'rsatadi. Natija: true — to'g'ri, false — bekor qilindi */
    public static void askPin(BaseFragment fragment, String title, Utilities.Callback<Boolean> result) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            result.run(false);
            return;
        }
        MgLockScreen.verify(context, title, result);
    }

    /** Yangi qulf kodi yaratish (joriy qulf turi bilan) */
    public static void createPin(BaseFragment fragment, Runnable onDone) {
        createLock(fragment, MgConfig.getLockType(), onDone);
    }

    public static void createLock(BaseFragment fragment, String type, Runnable onDone) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        MgLockScreen.create(context, type, ok -> {
            if (ok) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, MgConfig.LOCK_PATTERN.equals(type) ? "Grafik kalit saqlandi" : "PIN kod saqlandi").show();
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
    }

    /** Chat menyusidagi "Qulflash / Qulfni olish" */
    public static void toggleLock(BaseFragment fragment, int account, long dialogId) {
        if (MgConfig.isDialogLocked(account, dialogId)) {
            askPin(fragment, "Qulfni olish", ok -> {
                if (ok) {
                    MgConfig.setDialogLocked(account, dialogId, false);
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Chat qulfdan chiqarildi").show();
                }
            });
        } else if (!MgConfig.hasPin()) {
            createPin(fragment, () -> {
                MgConfig.setDialogLocked(account, dialogId, true);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Chat qulflandi. Keyingi safar PIN so'raladi").show();
            });
        } else {
            MgConfig.setDialogLocked(account, dialogId, true);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Chat qulflandi. Keyingi safar PIN so'raladi").show();
        }
    }

    /**
     * Chat ochilganda chaqiriladi: qulflangan bo'lsa kontentni yopib, PIN so'raydi.
     */
    public static void checkOnOpen(BaseFragment fragment, View fragmentView, int account, long dialogId, boolean preview) {
        if (!(fragmentView instanceof ViewGroup) || !MgConfig.isDialogLocked(account, dialogId)) {
            return;
        }
        Context context = fragmentView.getContext();
        LinearLayout overlay = new LinearLayout(context);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        overlay.setClickable(true);
        ImageView lockIcon = new ImageView(context);
        lockIcon.setImageResource(R.drawable.msg_secret);
        lockIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        lockIcon.setScaleType(ImageView.ScaleType.CENTER);
        overlay.addView(lockIcon, LayoutHelper.createLinear(64, 64, Gravity.CENTER_HORIZONTAL));
        TextView text = new TextView(context);
        text.setText("Bu chat qulflangan");
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        text.setGravity(Gravity.CENTER);
        overlay.addView(text, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));
        ((ViewGroup) fragmentView).addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setOnClickListener(v -> ask(fragment, overlay));
        if (!preview) {
            AndroidUtilities.runOnUIThread(() -> ask(fragment, overlay), 300);
        }
    }

    private static void ask(BaseFragment fragment, View overlay) {
        if (fragment.getParentActivity() == null || overlay.getParent() == null) {
            return;
        }
        askPin(fragment, "Chat qulflangan", ok -> {
            if (ok) {
                if (overlay.getParent() instanceof ViewGroup) {
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                }
            } else {
                fragment.finishFragment();
            }
        });
    }
}
