/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.URLSpanUserMention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Maxsus uzatish" — xabarni oddiy forward emas, balki tahrirlab, o'z nomingizdan uzatish.
 * Matn/izohni tahrirlash, formatlash, havolalarni olib tashlash, almashtirish,
 * imzo qo'shish, tarjima qilish, media yoki matnni alohida yuborish, albomlarni saqlash.
 */
public class MgCustomForward {

    private static final Pattern JUNK = Pattern.compile("(https?://\\S+|(?:www\\.)?t(?:elegram)?\\.me/\\S+|@[A-Za-z][A-Za-z0-9_]{3,31})");

    /** Tahrirlovchi holati: tanlash oynasidan qaytilganda saqlanib qoladi */
    private static class State {
        MessageObject primary;
        ArrayList<MessageObject> items = new ArrayList<>();
        CharSequence originalText;
        CharSequence text;
        boolean withMedia = true;
        boolean spoiler;
        boolean silent;
        boolean hasMedia;
        boolean albumOk;
    }

    public static void open(ChatActivity fragment, MessageObject selected, MessageObject.GroupedMessages group) {
        if (fragment == null || selected == null || fragment.getParentActivity() == null) {
            return;
        }
        State st = new State();
        if (group != null && group.messages != null && group.messages.size() > 1) {
            st.items.addAll(group.messages);
            java.util.Collections.sort(st.items, (a, b) -> Integer.compare(a.getId(), b.getId()));
        } else {
            st.items.add(selected);
        }
        MessageObject withText = null;
        for (MessageObject m : st.items) {
            if (m.messageOwner != null && !TextUtils.isEmpty(m.messageOwner.message)) {
                withText = m;
                break;
            }
        }
        st.primary = withText != null ? withText : st.items.get(0);
        st.hasMedia = hasSendableMedia(st.items.get(0));
        st.albumOk = true;
        for (MessageObject m : st.items) {
            if (!isPhotoOrDoc(m)) {
                st.albumOk = false;
            }
        }
        st.spoiler = st.items.get(0).hasMediaSpoilers();
        st.originalText = buildText(st.primary);
        st.text = st.originalText;
        showEditor(fragment, st);
    }

    private static boolean hasSendableMedia(MessageObject m) {
        TLRPC.MessageMedia media = m.messageOwner == null ? null : m.messageOwner.media;
        return media != null && !(media instanceof TLRPC.TL_messageMediaEmpty) && !(media instanceof TLRPC.TL_messageMediaWebPage);
    }

    private static boolean isPhotoOrDoc(MessageObject m) {
        TLRPC.MessageMedia media = m.messageOwner == null ? null : m.messageOwner.media;
        return media != null && (media.photo instanceof TLRPC.TL_photo || media.document instanceof TLRPC.TL_document);
    }

    /** Izoh qo'shib bo'lmaydigan media turlari (stiker, doira video, zar va h.k.) */
    private static boolean captionless(MessageObject m) {
        return m.isSticker() || m.isAnimatedSticker() || m.isRoundVideo() || m.isDice() || m.isPoll()
                || !isPhotoOrDoc(m);
    }

    private static CharSequence buildText(MessageObject m) {
        if (m == null || m.messageOwner == null || TextUtils.isEmpty(m.messageOwner.message)) {
            return "";
        }
        ArrayList<TLRPC.MessageEntity> ents = m.messageOwner.entities == null ? new ArrayList<>() : new ArrayList<>(m.messageOwner.entities);
        return fromEntities(m.messageOwner.message, ents);
    }

    private static CharSequence fromEntities(String text, ArrayList<TLRPC.MessageEntity> entities) {
        try {
            ArrayList<TLRPC.MessageEntity> ents = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
            CharSequence cs = ChatActivityEnterView.applyMessageEntities(ents, text, Theme.chat_msgTextPaint.getFontMetricsInt());
            return Emoji.replaceEmoji(cs, Theme.chat_msgTextPaint.getFontMetricsInt(), false);
        } catch (Throwable e) {
            FileLog.e(e);
            return text;
        }
    }

    private static TextView chip(Context ctx, String label, View.OnClickListener l) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.multAlpha(Theme.getColor(Theme.key_featuredStickers_addButton), 0.14f));
        tv.setBackground(bg);
        tv.setOnClickListener(l);
        return tv;
    }

    private static void showEditor(ChatActivity fragment, State st) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) {
            return;
        }
        final int account = fragment.getCurrentAccount();
        final boolean[] proceeding = {false};

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextCheckCell mediaCell = null;
        TextCheckCell spoilerCell = null;
        LinearLayout optionsBox = new LinearLayout(ctx);
        optionsBox.setOrientation(LinearLayout.VERTICAL);
        if (st.hasMedia) {
            FrameLayout preview = new FrameLayout(ctx);
            BackupImageView img = new BackupImageView(ctx);
            img.setRoundRadius(AndroidUtilities.dp(10));
            MessageObject first = st.items.get(0);
            try {
                TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(first.photoThumbs, AndroidUtilities.dp(320));
                TLRPC.PhotoSize small = FileLoader.getClosestPhotoSizeWithSize(first.photoThumbs, 40);
                if (size != null) {
                    img.setImage(ImageLocation.getForObject(size, first.photoThumbsObject), "320_180", ImageLocation.getForObject(small, first.photoThumbsObject), "40_40_b", null, first);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            preview.addView(img, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            TextView badge = new TextView(ctx);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            badge.setTextColor(0xFFFFFFFF);
            badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            GradientDrawable bb = new GradientDrawable();
            bb.setCornerRadius(AndroidUtilities.dp(10));
            bb.setColor(0x88000000);
            badge.setBackground(bb);
            badge.setText(mediaLabel(first) + (st.items.size() > 1 ? "  ·  albom: " + st.items.size() + " ta" : ""));
            preview.addView(badge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 8, 0, 0, 8));
            GradientDrawable pbg = new GradientDrawable();
            pbg.setCornerRadius(AndroidUtilities.dp(10));
            pbg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            preview.setBackground(pbg);
            boolean hasThumb = first.photoThumbs != null && !first.photoThumbs.isEmpty();
            root.addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, hasThumb ? 170 : 40, 24, 4, 24, 4));

            mediaCell = new TextCheckCell(ctx, 23, true, fragment.getResourceProvider());
            mediaCell.setTextAndCheck("Mediani ham yuborish", st.withMedia, false);
            optionsBox.addView(mediaCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
            if (isPhotoOrDoc(first) && !captionless(first)) {
                spoilerCell = new TextCheckCell(ctx, 23, true, fragment.getResourceProvider());
                spoilerCell.setTextAndCheck("Spoiler bilan yashirish", st.spoiler, false);
                optionsBox.addView(spoilerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
            }
        }
        TextCheckCell silentCell = new TextCheckCell(ctx, 23, true, fragment.getResourceProvider());
        silentCell.setTextAndCheck("Ovozsiz yuborish", st.silent, false);
        optionsBox.addView(silentCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));

        EditTextCaption edit = new EditTextCaption(ctx, fragment.getResourceProvider());
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setHint(st.hasMedia ? "Izoh (bo'sh qoldirish mumkin)" : "Xabar matni");
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setSingleLine(false);
        edit.setMinLines(3);
        edit.setMaxLines(12);
        edit.setGravity(Gravity.TOP | Gravity.LEFT);
        edit.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        GradientDrawable ebg = new GradientDrawable();
        ebg.setCornerRadius(AndroidUtilities.dp(12));
        ebg.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack), 0.2f));
        edit.setBackground(ebg);
        edit.setText(st.text);
        root.addView(sectionLabel(ctx, st.hasMedia ? "1. Izohni tahrirlang" : "1. Matnni tahrirlang"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 10, 24, 6));
        root.addView(edit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));

        // Formatlash paneli: matnni belgilab tugmani bosing (qayta bosish — olib tashlaydi)
        HorizontalScrollView fmtScroll = new HorizontalScrollView(ctx);
        fmtScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout fmt = new LinearLayout(ctx);
        fmt.setOrientation(LinearLayout.HORIZONTAL);
        fmt.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        fmtScroll.addView(fmt);
        root.addView(fmtScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 0, 8, 0, 0));
        final Runnable[] fmtSay = new Runnable[1];
        addFmt(ctx, fmt, "B", android.graphics.Typeface.DEFAULT_BOLD, 0, "Qalin", edit, () -> edit.toggleStyleForSelection(org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_BOLD), fmtSay);
        addFmt(ctx, fmt, "I", android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC), 0, "Qiya", edit, () -> edit.toggleStyleForSelection(org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_ITALIC), fmtSay);
        addFmt(ctx, fmt, "U", null, android.graphics.Paint.UNDERLINE_TEXT_FLAG, "Tagiga chizilgan", edit, () -> edit.toggleStyleForSelection(org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_UNDERLINE), fmtSay);
        addFmt(ctx, fmt, "S", null, android.graphics.Paint.STRIKE_THRU_TEXT_FLAG, "O'rtasiga chizilgan", edit, () -> edit.toggleStyleForSelection(org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_STRIKE), fmtSay);
        addFmt(ctx, fmt, "</>", android.graphics.Typeface.MONOSPACE, 0, "Monoshrift", edit, () -> edit.toggleStyleForSelection(org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_MONO), fmtSay);
        addFmt(ctx, fmt, "▒ Spoyler", null, 0, "Yashirin (spoyler)", edit, edit::makeSelectedSpoiler, fmtSay);
        addFmt(ctx, fmt, "❝ Iqtibos", null, 0, "Iqtibos", edit, edit::makeSelectedQuote, fmtSay);
        addFmt(ctx, fmt, "🔗 Havola", null, 0, "Havola", edit, edit::makeSelectedUrl, fmtSay);
        addFmt(ctx, fmt, "✕ Oddiy", null, 0, "Formatsiz", edit, edit::makeSelectedRegular, fmtSay);

        TextView counter = new TextView(ctx);
        counter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        counter.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        counter.setGravity(Gravity.RIGHT);
        root.addView(counter, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 4, 24, 0));
        TextView status = new TextView(ctx);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        status.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        status.setGravity(Gravity.CENTER);
        status.setVisibility(View.GONE);
        root.addView(status, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 6, 24, 0));
        final Runnable[] hideStatus = new Runnable[1];
        final Utilities.Callback<String> say = msg -> {
            status.setText(msg);
            status.setVisibility(View.VISIBLE);
            if (hideStatus[0] != null) {
                AndroidUtilities.cancelRunOnUIThread(hideStatus[0]);
            }
            AndroidUtilities.runOnUIThread(hideStatus[0] = () -> status.setVisibility(View.GONE), 3500);
        };
        fmtSay[0] = () -> say.run("Avval matnning bir qismini belgilang (bosib turing va suring)");
        final TextCheckCell mediaCellF = mediaCell;
        Runnable updateCounter = () -> {
            int len = edit.length();
            boolean asCaption = st.hasMedia && (mediaCellF == null || mediaCellF.isChecked());
            int limit = asCaption ? MessagesController.getInstance(account).getCaptionMaxLengthLimit() : MessagesController.getInstance(account).maxMessageLength;
            String extra = asCaption && len > limit ? "  · alohida xabar bo'lib ketadi" : "";
            counter.setText(len + " / " + limit + extra);
            counter.setTextColor(Theme.getColor(len > limit ? Theme.key_text_RedRegular : Theme.key_dialogTextGray3));
        };
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCounter.run();
            }
        });
        updateCounter.run();

        if (mediaCell != null) {
            mediaCell.setOnClickListener(v -> {
                ((TextCheckCell) v).setChecked(!((TextCheckCell) v).isChecked());
                updateCounter.run();
            });
        }
        if (spoilerCell != null) {
            spoilerCell.setOnClickListener(v -> ((TextCheckCell) v).setChecked(!((TextCheckCell) v).isChecked()));
        }
        silentCell.setOnClickListener(v -> ((TextCheckCell) v).setChecked(!((TextCheckCell) v).isChecked()));

        // Tezkor amallar (2 ustunli to'r)
        root.addView(sectionLabel(ctx, "2. Tezkor amallar"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 14, 24, 6));
        final LinearLayout tools = new ToolGrid(ctx);
        root.addView(tools, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 6));

        tools.addView(chip(ctx, "↺ Asl matn", v -> edit.setText(st.originalText)), null);
        tools.addView(chip(ctx, "🔗 Havolalarni o'chirish", v -> {
            Editable e = edit.getText();
            if (e == null) return;
            int removed = 0;
            for (URLSpanReplacement s : e.getSpans(0, e.length(), URLSpanReplacement.class)) {
                e.removeSpan(s);
                removed++;
            }
            say.run(removed > 0 ? "✓ Yashirin havolalar olib tashlandi: " + removed : "Yashirin havola topilmadi");
        }), null);
        tools.addView(chip(ctx, "🧽 @ va linklarni tozalash", v -> {
            Editable e = edit.getText();
            if (e == null) return;
            ArrayList<int[]> ranges = new ArrayList<>();
            Matcher m = JUNK.matcher(e.toString());
            while (m.find()) {
                ranges.add(new int[]{m.start(), m.end()});
            }
            for (int i = ranges.size() - 1; i >= 0; i--) {
                e.delete(ranges.get(i)[0], ranges.get(i)[1]);
            }
            cleanupWhitespace(e);
            say.run(ranges.isEmpty() ? "Tozalanadigan narsa topilmadi" : "✓ Tozalandi: " + ranges.size() + " ta");
        }), null);
        tools.addView(chip(ctx, "🧹 Formatsiz", v -> {
            Editable e = edit.getText();
            if (e == null) return;
            for (CharacterStyle s : e.getSpans(0, e.length(), CharacterStyle.class)) {
                if (s instanceof AnimatedEmojiSpan || s instanceof Emoji.EmojiSpan) {
                    continue;
                }
                e.removeSpan(s);
            }
        }), null);
        tools.addView(chip(ctx, "🔁 Almashtirish", v -> showReplace(fragment, edit, say)), null);
        tools.addView(chip(ctx, "✍️ Imzo", v -> addSignature(fragment, edit, say)), null);
        tools.addView(chip(ctx, "🌐 Tarjima", v -> translateInEditor(fragment, account, edit, say, false)), null);
        tools.addView(chip(ctx, "🌐+ Tarjimani qo'shish", v -> translateInEditor(fragment, account, edit, say, true)), null);

        root.addView(sectionLabel(ctx, "3. Yuborish"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 14, 24, 0));
        root.addView(optionsBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView hint = new TextView(ctx);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        hint.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        hint.setText("Xabar sizning nomingizdan, \"Forwarded from\" belgisisiz yuboriladi. Pastdagi \"Chatni tanlash\" tugmasini bosing.");
        root.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 8));

        final TextCheckCell spoilerCellF = spoilerCell;
        Runnable saveState = () -> {
            st.text = new SpannableStringBuilder(edit.getText());
            st.withMedia = mediaCellF == null || mediaCellF.isChecked();
            st.spoiler = spoilerCellF != null && spoilerCellF.isChecked();
            st.silent = silentCell.isChecked();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        builder.setTitle("Maxsus uzatish");
        builder.setView(scroll);
        builder.setPositiveButton("Chatni tanlash ➜", (dialog, which) -> {
            saveState.run();
            if (!st.hasMedia || !st.withMedia) {
                if (TextUtils.isEmpty(st.text.toString().trim())) {
                    BulletinFactory.of(fragment).createErrorBulletin("Yuborish uchun matn yo'q").show();
                    AndroidUtilities.runOnUIThread(() -> showEditor(fragment, st), 250);
                    return;
                }
            }
            proceeding[0] = true;
            AndroidUtilities.hideKeyboard(edit);
            openPicker(fragment, st);
        });
        builder.setNegativeButton("Bekor qilish", null);
        AlertDialog dialog = builder.create();
        fragment.showDialog(dialog);
        AndroidUtilities.runOnUIThread(() -> {
            edit.requestFocus();
            edit.setSelection(edit.length());
        }, 200);
    }

    private static TextView sectionLabel(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        return tv;
    }

    private static void addFmt(Context ctx, LinearLayout bar, String label, android.graphics.Typeface tf, int paintFlags, String desc,
                               EditTextCaption edit, Runnable action, Runnable[] noSelection) {
        TextView b = new TextView(ctx);
        b.setText(label);
        b.setContentDescription(desc);
        b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        if (tf != null) {
            b.setTypeface(tf);
        }
        if (paintFlags != 0) {
            b.setPaintFlags(b.getPaintFlags() | paintFlags);
        }
        b.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(AndroidUtilities.dp(40));
        b.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack), 0.07f));
        b.setBackground(bg);
        b.setFocusable(false);
        b.setOnClickListener(v -> {
            if (edit.getSelectionStart() == edit.getSelectionEnd()) {
                if (noSelection[0] != null) {
                    noSelection[0].run();
                }
                return;
            }
            try {
                action.run();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
        bar.addView(b, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 38, 3, 3, 3, 3));
    }

    /** Qo'shilgan tugmalarni 2 ustunli qatorlarga joylaydigan oddiy to'r */
    private static class ToolGrid extends LinearLayout {
        private LinearLayout row;

        ToolGrid(Context ctx) {
            super(ctx);
            setOrientation(VERTICAL);
        }

        @Override
        public void addView(android.view.View child, android.view.ViewGroup.LayoutParams params) {
            if (row == null || row.getChildCount() >= 2) {
                row = new LinearLayout(getContext());
                row.setOrientation(HORIZONTAL);
                super.addView(row, -1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44));
            }
            row.addView(child, LayoutHelper.createLinear(0, 40, 1f, 4, 2, 4, 2));
        }

        @Override
        public void addView(android.view.View child) {
            addView(child, (android.view.ViewGroup.LayoutParams) null);
        }
    }

    private static String mediaLabel(MessageObject m) {
        if (m.isPhoto()) return "🖼 Rasm";
        if (m.isRoundVideo()) return "⏺ Doira video";
        if (m.isVideo()) return "🎬 Video";
        if (m.isVoice()) return "🎙 Ovozli xabar";
        if (m.isMusic()) return "🎵 Musiqa";
        if (m.isSticker() || m.isAnimatedSticker()) return "😀 Stiker";
        if (m.isGif()) return "GIF";
        if (m.isPoll()) return "📊 So'rovnoma";
        if (m.isDice()) return "🎲 Zar";
        if (m.messageOwner != null && m.messageOwner.media != null && m.messageOwner.media.document != null) return "📎 Fayl";
        return "📎 Media";
    }

    private static void cleanupWhitespace(Editable e) {
        // Ortiqcha bo'sh joy va bo'sh qatorlarni yig'ishtirish
        String s = e.toString();
        for (int i = s.length() - 1; i > 0; i--) {
            char c = s.charAt(i), p = s.charAt(i - 1);
            if ((c == ' ' && (p == ' ' || p == '\n')) || (c == '\n' && i > 1 && p == '\n' && s.charAt(i - 2) == '\n')) {
                e.delete(i, i + 1);
                s = e.toString();
            }
        }
        while (e.length() > 0 && Character.isWhitespace(e.charAt(e.length() - 1))) {
            e.delete(e.length() - 1, e.length());
        }
        while (e.length() > 0 && Character.isWhitespace(e.charAt(0))) {
            e.delete(0, 1);
        }
    }

    private static void showReplace(ChatActivity fragment, EditTextCaption target, Utilities.Callback<String> say) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) return;
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        org.telegram.ui.Components.EditTextBoldCursor from = new org.telegram.ui.Components.EditTextBoldCursor(ctx);
        org.telegram.ui.Components.EditTextBoldCursor to = new org.telegram.ui.Components.EditTextBoldCursor(ctx);
        String lastFrom = MgConfig.getString("mg_cf_replace_from", "");
        String lastTo = MgConfig.getString("mg_cf_replace_to", "");
        org.telegram.ui.Components.EditTextBoldCursor[] fields = {from, to};
        String[] hints = {"Nimani (masalan: @eski_kanal)", "Nimaga (masalan: @mening_kanalim)"};
        String[] values = {lastFrom, lastTo};
        for (int i = 0; i < 2; i++) {
            org.telegram.ui.Components.EditTextBoldCursor f = fields[i];
            f.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            f.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            f.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
            f.setHint(hints[i]);
            f.setText(values[i]);
            f.setSingleLine(true);
            f.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
            f.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
            f.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
            ll.addView(f, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 24, 6, 24, 6));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        b.setTitle("🔁 Matnni almashtirish");
        b.setView(ll);
        b.setPositiveButton("Almashtirish", (d, w) -> {
            String f = from.getText() == null ? "" : from.getText().toString();
            String t = to.getText() == null ? "" : to.getText().toString();
            if (f.isEmpty()) return;
            MgConfig.setString("mg_cf_replace_from", f);
            MgConfig.setString("mg_cf_replace_to", t);
            Editable e = target.getText();
            if (e == null) return;
            int count = 0;
            String lower = e.toString().toLowerCase();
            String fl = f.toLowerCase();
            int idx = lower.lastIndexOf(fl);
            while (idx >= 0) {
                e.replace(idx, idx + f.length(), t);
                count++;
                if (idx == 0) break;
                idx = lower.lastIndexOf(fl, idx - 1);
            }
            say.run(count > 0 ? "✓ Almashtirildi: " + count + " ta" : "\"" + f + "\" topilmadi");
        });
        b.setNegativeButton("Bekor", null);
        b.show();
    }

    private static void addSignature(ChatActivity fragment, EditTextCaption target, Utilities.Callback<String> say) {
        String sig = MgConfig.getString("mg_cf_signature", "");
        if (TextUtils.isEmpty(sig)) {
            editSignature(fragment, () -> addSignature(fragment, target, say), true);
            return;
        }
        Editable e = target.getText();
        if (e == null) return;
        String cur = e.toString();
        if (cur.endsWith(sig)) {
            // Ikkinchi bosishda imzoni tahrirlash
            editSignature(fragment, null, true);
            return;
        }
        if (cur.trim().length() > 0) {
            e.append("\n\n");
        }
        e.append(sig);
        target.setSelection(target.length());
        say.run("✓ Imzo qo'shildi (qayta bossangiz — tahrirlash)");
    }

    public static void editSignature(org.telegram.ui.ActionBar.BaseFragment fragment, Runnable after) {
        editSignature(fragment, after, false);
    }

    public static void editSignature(org.telegram.ui.ActionBar.BaseFragment fragment, Runnable after, boolean overlay) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) return;
        org.telegram.ui.Components.EditTextBoldCursor f = new org.telegram.ui.Components.EditTextBoldCursor(ctx);
        f.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        f.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        f.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        f.setHint("Masalan: 👉 @mening_kanalim");
        f.setText(MgConfig.getString("mg_cf_signature", ""));
        f.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        f.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        f.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        FrameLayout fl = new FrameLayout(ctx);
        fl.addView(f, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 6));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        b.setTitle("✍️ Imzo");
        b.setMessage("Maxsus uzatishda \"Imzo\" tugmasi bosilganda xabar oxiriga qo'shiladi.");
        b.setView(fl);
        b.setPositiveButton("Saqlash", (d, w) -> {
            MgConfig.setString("mg_cf_signature", f.getText() == null ? "" : f.getText().toString().trim());
            if (after != null && !TextUtils.isEmpty(MgConfig.getString("mg_cf_signature", ""))) {
                after.run();
            }
        });
        b.setNegativeButton("Bekor", null);
        if (overlay) {
            b.show();
        } else {
            fragment.showDialog(b.create());
        }
    }

    /** Muharrir ichida tarjima (dialog yopilmaydi) */
    private static void translateInEditor(ChatActivity fragment, int account, EditTextCaption edit, Utilities.Callback<String> say, boolean append) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) {
            return;
        }
        MgTranslate.chooseLanguage(fragment, append ? "Tarjima qaysi tilda qo'shilsin?" : "Qaysi tilga tarjima qilinsin?", true, lang -> {
            CharSequence[] arr = {new SpannableStringBuilder(edit.getText())};
            ArrayList<TLRPC.MessageEntity> ents = MediaDataController.getInstance(account).getEntities(arr, true, false);
            String src = arr[0] == null ? "" : arr[0].toString();
            if (TextUtils.isEmpty(src.trim())) {
                say.run("Tarjima uchun matn yo'q");
                return;
            }
            say.run("⏳ Tarjima qilinmoqda…");
            MgTranslate.translate(account, src, ents, lang, (res, err) -> {
                if (res != null) {
                    CharSequence tr = fromEntities(res.text, res.entities);
                    if (append) {
                        Editable e = edit.getText();
                        if (e != null) {
                            e.append("\n\n");
                            e.append(tr);
                        }
                    } else {
                        edit.setText(tr);
                    }
                    edit.setSelection(edit.length());
                    say.run("✓ Tarjima qilindi: " + MgTranslate.nameOf(lang));
                } else {
                    say.run("⚠️ " + (err == null ? "Tarjima qilib bo'lmadi" : err));
                }
            });
        });
    }

    private static void openPicker(ChatActivity fragment, State st) {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        args.putInt("messagesCount", st.items.size());
        args.putBoolean("canSelectTopics", true);
        final boolean[] sent = {false};
        DialogsActivity picker = new DialogsActivity(args) {
            @Override
            public void onFragmentDestroy() {
                super.onFragmentDestroy();
                if (!sent[0]) {
                    // Tanlash bekor qilindi — tahrirlangan holat bilan qaytamiz
                    AndroidUtilities.runOnUIThread(() -> {
                        if (fragment.getParentActivity() != null && !fragment.isFinished && fragment.getFragmentView() != null) {
                            showEditor(fragment, st);
                        }
                    }, 300);
                }
            }
        };
        picker.setDelegate((dialogsFragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            sent[0] = true;
            boolean n = notify && !st.silent;
            int ok = 0;
            for (int i = 0; i < dids.size(); i++) {
                try {
                    if (send(fragment, st, dids.get(i), message, n, scheduleDate)) {
                        ok++;
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            if (topicsFragment != null) {
                topicsFragment.removeSelfFromStack();
            }
            dialogsFragment.finishFragment();
            final int okF = ok;
            final int total = dids.size();
            AndroidUtilities.runOnUIThread(() -> {
                if (okF > 0) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.forward, total == 1 ? "Maxsus uzatildi" : "Maxsus uzatildi: " + okF + " ta chatga").show();
                } else {
                    BulletinFactory.of(fragment).createErrorBulletin("Yuborib bo'lmadi").show();
                }
            }, 250);
            return true;
        });
        fragment.presentFragment(picker);
    }

    private static boolean send(ChatActivity fragment, State st, MessagesStorage.TopicKey key, CharSequence comment, boolean notify, int scheduleDate) {
        final int account = fragment.getCurrentAccount();
        final long did = key.dialogId;
        SendMessagesHelper helper = SendMessagesHelper.getInstance(account);
        MessagesController mc = MessagesController.getInstance(account);

        MessageObject replyTop = null;
        if (key.topicId != 0 && did < 0) {
            TLRPC.TL_forumTopic topic = mc.getTopicsController().findTopic(-did, key.topicId);
            if (topic != null && topic.topicStartMessage != null) {
                replyTop = new MessageObject(account, topic.topicStartMessage, false, false);
                replyTop.isTopicMainMessage = true;
            }
        }

        // Izoh (tanlash oynasida yozilgan qo'shimcha matn) — birinchi bo'lib
        if (!TextUtils.isEmpty(comment)) {
            SendMessagesHelper.SendMessageParams cp = SendMessagesHelper.SendMessageParams.of(comment.toString(), did, replyTop, replyTop, null, true, null, null, null, notify, scheduleDate, 0, null, false);
            helper.sendMessage(cp);
        }

        CharSequence[] arr = {new SpannableStringBuilder(st.text == null ? "" : st.text)};
        ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(account).getEntities(arr, true, false);
        String text = arr[0] == null ? "" : arr[0].toString();
        // Chetdagi bo'shliqlarni olib tashlash (entity ofsetlarini buzmasdan faqat oxiridan)
        while (text.length() > 0 && Character.isWhitespace(text.charAt(text.length() - 1))) {
            text = text.substring(0, text.length() - 1);
        }
        if (entities != null) {
            for (int i = entities.size() - 1; i >= 0; i--) {
                TLRPC.MessageEntity en = entities.get(i);
                if (en.offset >= text.length()) {
                    entities.remove(i);
                } else if (en.offset + en.length > text.length()) {
                    en.length = text.length() - en.offset;
                }
            }
        }
        final boolean hasText = !TextUtils.isEmpty(text.trim());

        if (DialogObject.isEncryptedDialog(did)) {
            // Maxfiy chatlarda: media asl holicha, matn alohida
            if (st.hasMedia && st.withMedia) {
                for (MessageObject m : st.items) {
                    helper.processForwardFromMyName(m, did, 0, 0, null);
                }
            }
            if (hasText && (!st.hasMedia || !st.withMedia || !TextUtils.equals(text, st.primary.messageOwner.message))) {
                helper.sendMessage(SendMessagesHelper.SendMessageParams.of(text, did, null, null, null, true, entities, null, null, notify, scheduleDate, 0, null, false));
            }
            return true;
        }

        if (!st.hasMedia || !st.withMedia) {
            if (!hasText) {
                return false;
            }
            sendLongText(helper, mc, text, entities, did, replyTop, notify, scheduleDate);
            return true;
        }

        int captionLimit = mc.getCaptionMaxLengthLimit();
        boolean textSeparately = text.length() > captionLimit || captionless(st.items.get(0));

        if (st.items.size() > 1 && st.albumOk) {
            long groupId = Utilities.random.nextLong();
            for (int i = 0; i < st.items.size(); i++) {
                MessageObject m = st.items.get(i);
                HashMap<String, String> params = new HashMap<>();
                params.put("groupId", "" + groupId);
                if (i == st.items.size() - 1) {
                    params.put("final", "1");
                }
                boolean withCaption = i == 0 && !textSeparately && hasText;
                sendMedia(helper, m, did, replyTop, withCaption ? text : "", withCaption ? entities : null, params, notify, scheduleDate, st.spoiler);
            }
        } else if (st.items.size() > 1) {
            // Aralash albom (masalan, musiqa) — o'z nomimdan uzatish, izohsiz
            ArrayList<MessageObject> list = new ArrayList<>(st.items);
            helper.sendMessage(list, did, true, true, notify, scheduleDate, replyTop, -1, 0);
            textSeparately = true;
        } else {
            MessageObject m = st.items.get(0);
            if (isPhotoOrDoc(m) && !captionless(m)) {
                sendMedia(helper, m, did, replyTop, textSeparately || !hasText ? "" : text, textSeparately || !hasText ? null : entities, null, notify, scheduleDate, st.spoiler);
            } else {
                ArrayList<MessageObject> list = new ArrayList<>();
                list.add(m);
                helper.sendMessage(list, did, true, true, notify, scheduleDate, replyTop, -1, 0);
                textSeparately = true;
            }
        }
        if (textSeparately && hasText) {
            sendLongText(helper, mc, text, entities, did, replyTop, notify, scheduleDate);
        }
        return true;
    }

    private static void sendMedia(SendMessagesHelper helper, MessageObject m, long did, MessageObject replyTop, String caption,
                                  ArrayList<TLRPC.MessageEntity> entities, HashMap<String, String> params, boolean notify, int scheduleDate, boolean spoiler) {
        TLRPC.MessageMedia media = m.messageOwner.media;
        int ttl = media.ttl_seconds;
        SendMessagesHelper.SendMessageParams p;
        if (media.photo instanceof TLRPC.TL_photo) {
            p = SendMessagesHelper.SendMessageParams.of((TLRPC.TL_photo) media.photo, null, did, replyTop, replyTop, caption, entities, null, params, notify, scheduleDate, 0, ttl, m, false, spoiler);
        } else {
            p = SendMessagesHelper.SendMessageParams.of((TLRPC.TL_document) media.document, null, m.messageOwner.attachPath, did, replyTop, replyTop, caption, entities, null, params, notify, scheduleDate, 0, ttl, m, null, false, spoiler);
        }
        helper.sendMessage(p);
    }

    /** Uzun matnni limitdan oshsa bo'laklab yuboradi */
    private static void sendLongText(SendMessagesHelper helper, MessagesController mc, String text, ArrayList<TLRPC.MessageEntity> entities,
                                     long did, MessageObject replyTop, boolean notify, int scheduleDate) {
        int limit = Math.max(1024, mc.maxMessageLength);
        if (text.length() <= limit) {
            helper.sendMessage(SendMessagesHelper.SendMessageParams.of(text, did, replyTop, replyTop, null, true, entities, null, null, notify, scheduleDate, 0, null, false));
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + limit);
            if (end < text.length()) {
                int nl = text.lastIndexOf('\n', end);
                if (nl > start + limit / 2) {
                    end = nl;
                }
            }
            ArrayList<TLRPC.MessageEntity> part = new ArrayList<>();
            if (entities != null) {
                for (TLRPC.MessageEntity en : entities) {
                    int s = Math.max(en.offset, start);
                    int e = Math.min(en.offset + en.length, end);
                    if (e > s) {
                        TLRPC.MessageEntity c = copyEntity(en);
                        if (c != null) {
                            c.offset = s - start;
                            c.length = e - s;
                            part.add(c);
                        }
                    }
                }
            }
            helper.sendMessage(SendMessagesHelper.SendMessageParams.of(text.substring(start, end), did, replyTop, replyTop, null, true, part, null, null, notify, scheduleDate, 0, null, false));
            start = end;
            while (start < text.length() && text.charAt(start) == '\n') {
                start++;
            }
        }
    }

    private static TLRPC.MessageEntity copyEntity(TLRPC.MessageEntity en) {
        try {
            org.telegram.tgnet.SerializedData data = new org.telegram.tgnet.SerializedData(en.getObjectSize());
            en.serializeToStream(data);
            org.telegram.tgnet.SerializedData in = new org.telegram.tgnet.SerializedData(data.toByteArray());
            TLRPC.MessageEntity c = TLRPC.MessageEntity.TLdeserialize(in, in.readInt32(false), false);
            data.cleanup();
            in.cleanup();
            return c;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }
}
