/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MgHiddenSpaces;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Sozlamalar → Maxfiylik va xavfsizlik → Yashirin chatlar.
 * Maxfiy ibora o'rnatiladi; uni chatlar qidiruviga yozganda yashirin chatlar rejimi ochiladi.
 */
public class MgHiddenPhraseActivity extends BaseFragment {

    private EditTextBoldCursor phraseField;
    private EditTextBoldCursor repeatField;
    private TextView countView;
    private boolean saving;

    private int color(int key) {
        return Theme.getColor(key, getResourceProvider());
    }

    private EditTextBoldCursor field(Context ctx, CharSequence hint) {
        EditTextBoldCursor e = new EditTextBoldCursor(ctx);
        e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        e.setTextColor(color(Theme.key_windowBackgroundWhiteBlackText));
        e.setHintColor(color(Theme.key_windowBackgroundWhiteHintText));
        e.setHintText(hint);
        e.setBackground(null);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        e.setCursorColor(color(Theme.key_windowBackgroundWhiteBlackText));
        e.setCursorSize(AndroidUtilities.dp(20));
        e.setCursorWidth(1.5f);
        e.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));
        return e;
    }

    private TextView note(Context ctx, CharSequence text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        t.setTextColor(color(Theme.key_windowBackgroundWhiteGrayText4));
        t.setLineSpacing(AndroidUtilities.dp(2), 1f);
        t.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(4));
        return t;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MgLang.t("Yashirin chatlar"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    save();
                }
            }
        });
        actionBar.createMenu().addItem(1, R.drawable.ic_ab_done);

        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(color(Theme.key_windowBackgroundGray));
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(24));
        scroll.addView(list, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0));

        LinearLayout card = MgUi.card(context, getResourceProvider());
        phraseField = field(context, MgLang.t("Ibora"));
        repeatField = field(context, MgLang.t("Iborani takrorlang"));
        card.addView(phraseField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        View divider = new View(context);
        divider.setBackgroundColor(color(Theme.key_divider));
        card.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 18, 0, 0, 0));
        card.addView(repeatField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        list.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));

        SpannableStringBuilder info = new SpannableStringBuilder(MgLang.t("Yashirin chatlarni ochish uchun iborani chatlar qidiruviga yozing. Har bir ibora o'zining alohida chatlar to'plamini ochadi: yangi ibora bo'sh boshlanadi, avvalgi iboralar ishlashda davom etadi. Yashirin chatlar faqat shu telefondagi MilliyGram'da ishlaydi va boshqa qurilmalarga sinxronlanmaydi. "));
        int start = info.length();
        info.append(MgLang.t("Qanday ishlaydi"));
        info.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showHowItWorks();
            }
        }, start, info.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView infoView = note(context, info);
        infoView.setMovementMethod(LinkMovementMethod.getInstance());
        infoView.setLinkTextColor(color(Theme.key_windowBackgroundWhiteLinkText));
        list.addView(infoView);
        list.addView(note(context, MgLang.t("Kamida 8 ta belgi ishlating, yaxshisi bir necha so'z. Iborani unutmang: u hech qayerda saqlanmaydi va uni tiklab bo'lmaydi. Unutsangiz, bu chatlar shu telefonda yashirin qoladi — akkauntdan chiqib, qayta kirguningizcha.")));

        LinearLayout actions = MgUi.card(context, getResourceProvider());
        countView = new TextView(context);
        countView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        countView.setTextColor(color(Theme.key_windowBackgroundWhiteBlackText));
        countView.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(10));
        actions.addView(countView);
        TextView deleteAll = MgUi.tile(context, getResourceProvider(), MgLang.t("Barcha iboralarni o'chirish"), this::confirmDeleteAll);
        deleteAll.setTextColor(color(Theme.key_text_RedBold));
        actions.addView(deleteAll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 12, 4, 12, 8));
        list.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 16, 12, 0));
        updateCount();

        fragmentView = scroll;
        return fragmentView;
    }

    private void updateCount() {
        if (countView == null) {
            return;
        }
        int n = MgHiddenSpaces.count();
        countView.setText(n == 0 ? MgLang.t("Hozircha ibora o'rnatilmagan") : MgLang.t("O'rnatilgan iboralar: ") + n);
        View parent = (View) countView.getParent();
        if (parent != null) {
            parent.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private void shake(View v) {
        AndroidUtilities.shakeViewSpring(v, 5);
        try {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Throwable ignore) {
        }
    }

    private void save() {
        if (saving || phraseField == null) {
            return;
        }
        final String phrase = phraseField.getText() == null ? "" : phraseField.getText().toString().trim();
        final String repeat = repeatField.getText() == null ? "" : repeatField.getText().toString().trim();
        if (phrase.length() < MgHiddenSpaces.MIN_PHRASE_LENGTH) {
            shake(phraseField);
            BulletinFactory.of(this).createErrorBulletin(MgLang.t("Ibora kamida 8 ta belgidan iborat bo'lsin")).show();
            return;
        }
        if (!phrase.equals(repeat)) {
            shake(repeatField);
            BulletinFactory.of(this).createErrorBulletin(MgLang.t("Iboralar mos kelmadi")).show();
            return;
        }
        saving = true;
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.showDelayed(300);
        Utilities.globalQueue.postRunnable(() -> {
            final boolean existed = MgHiddenSpaces.match(phrase) != null;
            final String space = MgHiddenSpaces.create(phrase);
            AndroidUtilities.runOnUIThread(() -> {
                saving = false;
                progress.dismiss();
                if (getParentActivity() == null) {
                    return;
                }
                if (space == null) {
                    BulletinFactory.of(this).createErrorBulletin(MgLang.t("Iborani saqlab bo'lmadi")).show();
                    return;
                }
                phraseField.setText("");
                repeatField.setText("");
                AndroidUtilities.hideKeyboard(phraseField);
                updateCount();
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                b.setTitle(existed ? MgLang.t("Bu ibora allaqachon bor") : MgLang.t("Ibora o'rnatildi"));
                b.setMessage(MgLang.t("Endi bosh ekrandagi qidiruvga shu iborani yozsangiz, yashirin chatlar ochiladi. U yerda + tugmasi bilan chat qo'shing."));
                b.setPositiveButton(MgLang.t("Hozir ochish"), (d, w) -> {
                    finishFragment();
                    BaseFragment last = org.telegram.ui.LaunchActivity.getSafeLastFragment();
                    if (last != null) {
                        DialogsActivity.mgOpenSpace(last, currentAccount, space);
                    }
                });
                b.setNegativeButton(MgLang.t("Yopish"), null);
                showDialog(b.create());
            });
        });
    }

    private void confirmDeleteAll() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        b.setTitle(MgLang.t("Barcha iboralarni o'chirish"));
        b.setMessage(MgLang.t("Barcha iboralar o'chiriladi va ulardagi chatlar yana oddiy ro'yxatda ko'rinadi. Davom etamizmi?"));
        b.setPositiveButton(MgLang.t("O'chirish"), (d, w) -> {
            MgHiddenSpaces.deleteAll();
            updateCount();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, MgLang.t("Iboralar o'chirildi, chatlar ro'yxatga qaytdi")).show();
        });
        b.setNegativeButton(MgLang.t("Bekor qilish"), null);
        AlertDialog dialog = b.create();
        showDialog(dialog);
        TextView button = (TextView) dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(color(Theme.key_text_RedBold));
        }
    }

    /** "Qanday ishlaydi" — funksiya haqida batafsil */
    private void showHowItWorks() {
        Context ctx = getParentActivity();
        if (ctx == null) {
            return;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(ctx, false, getResourceProvider());
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(12));

        TextView title = new TextView(ctx);
        title.setText(MgLang.t("Yashirin chatlar"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        title.setTextColor(color(Theme.key_dialogTextBlack));
        box.addView(title);
        TextView sub = new TextView(ctx);
        sub.setText(MgLang.t("Ba'zi chatlarni shu telefonda ko'zdan yashiring. Ular faqat iborangizni chatlar qidiruviga yozganingizdan keyin ko'rinadi."));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(color(Theme.key_dialogTextGray3));
        box.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 10));

        addPoint(ctx, box, R.drawable.msg_views, MgLang.t("Ko'zdan yashirin"), MgLang.t("Yashirin chatlar ro'yxat, jildlar, qidiruv, bildirishnomalar va ulashish oynasidan yo'qoladi."));
        addPoint(ctx, box, R.drawable.msg_permissions, MgLang.t("Ibora bilan ochiladi"), MgLang.t("Iborani chatlar qidiruviga yozing. U hech qachon saqlanmaydi va qidiruv tekshiruv tugaguncha kutadi — ibora serverga yuborilmaydi."));
        addPoint(ctx, box, R.drawable.msg_secret, MgLang.t("Faqat shu telefonda"), MgLang.t("Iboralar va yashirin chatlar faqat shu telefondagi MilliyGram'da saqlanadi. Boshqa qurilmalarda bu chatlar odatdagidek ko'rinadi."));
        addPoint(ctx, box, R.drawable.msg_folders, MgLang.t("Har bir iboraga alohida makon"), MgLang.t("Har bir ibora o'z chatlar to'plamini ochadi. Yangi ibora bo'sh boshlanadi va boshqa iboradagi chatlarni ko'rsatmaydi."));
        addPoint(ctx, box, R.drawable.msg_calls, MgLang.t("Jim qo'ng'iroqlar"), MgLang.t("Rejimdan tashqarida yashirin chatlardan kelgan qo'ng'iroqlar jimgina rad etiladi."));
        addPoint(ctx, box, R.drawable.msg_report, MgLang.t("Iborani unutmang"), MgLang.t("U hech qayerda saqlanmaydi va tiklab bo'lmaydi. Unutsangiz, chatlar akkauntdan chiqib qayta kirguningizcha yashirin qoladi."));
        addPoint(ctx, box, R.drawable.msg_mini_lock2, MgLang.t("O'zi yopiladi"), MgLang.t("Qulf tugmasi bilan darhol chiqasiz. Ekran o'chsa, ilova 1 daqiqa fonda tursa yoki 5 daqiqa harakatsiz qolsa — rejim o'zi yopiladi."));

        TextView foot = new TextView(ctx);
        foot.setText(MgLang.t("Yashirin chatlar akkauntingizning bir qismi bo'lib qoladi, Telegram serverlari ularni odatdagidek saqlaydi. Shu telefonda ular MilliyGram'ning shifrlangan xotirasida turadi."));
        foot.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        foot.setGravity(Gravity.CENTER);
        foot.setTextColor(color(Theme.key_dialogTextGray3));
        box.addView(foot, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 12));

        final BottomSheet[] sheet = new BottomSheet[1];
        TextView ok = MgUi.accentTile(ctx, getResourceProvider(), MgLang.t("Tushunarli"), () -> {
            if (sheet[0] != null) {
                sheet[0].dismiss();
            }
        });
        box.addView(ok, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        builder.setCustomView(sv);
        sheet[0] = builder.create();
        showDialog(sheet[0]);
    }

    private void addPoint(Context ctx, LinearLayout box, int icon, CharSequence head, CharSequence text) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        ImageView iv = new ImageView(ctx);
        iv.setImageResource(icon);
        iv.setColorFilter(new android.graphics.PorterDuffColorFilter(color(Theme.key_featuredStickers_addButton), android.graphics.PorterDuff.Mode.SRC_IN));
        row.addView(iv, LayoutHelper.createLinear(28, 28, Gravity.TOP, 0, 2, 14, 0));
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView h = new TextView(ctx);
        h.setText(head);
        h.setTypeface(AndroidUtilities.bold());
        h.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        h.setTextColor(color(Theme.key_dialogTextBlack));
        col.addView(h);
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        t.setTextColor(color(Theme.key_dialogTextGray3));
        col.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        box.addView(row);
    }
}
