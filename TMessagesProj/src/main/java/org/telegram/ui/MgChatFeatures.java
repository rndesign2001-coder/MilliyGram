/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * Avto-imzo, chat avto-tarjimasi, qo'shilish so'rovlari va "barcha akkauntlardan qo'shilish" g'oyalari
 * Novagram (VipAds LLC, GPL v2) dan olingan va MilliyGram uchun Java'da qayta yozilgan.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgAutoText;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgStrangers;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chat ichidagi qo'shimcha amallar (sarlavhadagi ⋮ menyu).
 */
public class MgChatFeatures {

    public static final int MENU_AUTO_TEXT = 9020;
    public static final int MENU_AUTO_TRANSLATE = 9021;
    public static final int MENU_JOIN_REQUESTS = 9022;
    public static final int MENU_JOIN_ALL = 9023;
    public static final int MENU_STRANGER = 9024;
    public static final int MENU_ONE_TIME_VOICE = 9025;
    public static final int MENU_VOICE_TYPING = 9026;
    public static final int MENU_TEMPLATES = 9027;

    // ================= Avto-tarjima (chat bo'yicha) =================

    private static String atKey(int account, long did) {
        return "at_" + account + "_" + did;
    }

    /** Chat uchun avto-tarjima tili (yo'q bo'lsa null) */
    public static String getAutoTranslateLang(int account, long did) {
        String s = MgConfig.getString(atKey(account, did), null);
        return TextUtils.isEmpty(s) ? null : s;
    }

    public static boolean isAutoTranslatePreview() {
        return MgConfig.getBool("at_preview", false);
    }

    public static String autoTranslateMenuTitle(int account, long did) {
        String l = getAutoTranslateLang(account, did);
        return l == null ? "Avto-tarjima" : "Avto-tarjima: " + MgTranslate.nameOf(l);
    }

    public static void showAutoTranslatePicker(BaseFragment f, int account, long did) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        String cur = getAutoTranslateLang(account, did);
        CharSequence[] items = new CharSequence[MgTranslate.NAMES.length + 1];
        items[0] = "O'chirilgan" + (cur == null ? "  ✓" : "");
        for (int i = 0; i < MgTranslate.NAMES.length; i++) {
            items[i + 1] = MgTranslate.NAMES[i] + (MgTranslate.CODES[i].equals(cur) ? "  ✓" : "");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Bu chatga yozganlarim qaysi tilga tarjima qilinsin?");
        b.setItems(items, (d, which) -> {
            if (which == 0) {
                MgConfig.setString(atKey(account, did), null);
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Avto-tarjima o'chirildi").show();
            } else {
                String code = MgTranslate.CODES[which - 1];
                MgConfig.setString(atKey(account, did), code);
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Yozgan xabarlaringiz " + MgTranslate.nameOf(code) + "ga tarjima qilinib yuboriladi").show();
            }
        });
        f.showDialog(b.create());
    }

    // ================= Avto-imzo =================

    static CharSequence fromEntities(String text, ArrayList<TLRPC.MessageEntity> entities) {
        try {
            ArrayList<TLRPC.MessageEntity> ents = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
            CharSequence cs = ChatActivityEnterView.applyMessageEntities(ents, text, Theme.chat_msgTextPaint.getFontMetricsInt());
            return Emoji.replaceEmoji(cs, Theme.chat_msgTextPaint.getFontMetricsInt(), false);
        } catch (Throwable e) {
            FileLog.e(e);
            return text;
        }
    }

    public static String autoTextMenuTitle(int account, long did) {
        return MgAutoText.isActive(account, did) ? "Avto-imzo: yoqilgan" : "Avto-imzo";
    }

    public static void showAutoTextEditor(BaseFragment f, int account, long did) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        MgAutoText.Entry e = MgAutoText.get(account, did);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);

        TextCheckCell activeCell = new TextCheckCell(ctx, 23, true, f.getResourceProvider());
        activeCell.setTextAndCheck("Yoqilgan", e.active || TextUtils.isEmpty(e.text), false);
        activeCell.setOnClickListener(v -> activeCell.setChecked(!activeCell.isChecked()));
        root.addView(activeCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));

        EditTextCaption edit = new EditTextCaption(ctx, f.getResourceProvider());
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setHint("Masalan: 👉 @mening_kanalim");
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setSingleLine(false);
        edit.setMinLines(2);
        edit.setMaxLines(8);
        edit.setGravity(Gravity.TOP | Gravity.LEFT);
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        edit.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(12));
        bg.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack), 0.2f));
        edit.setBackground(bg);
        if (!TextUtils.isEmpty(e.text)) {
            edit.setText(fromEntities(e.text, e.entities));
        }
        root.addView(edit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 4, 24, 0));

        TextView hint = new TextView(ctx);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hint.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        hint.setText("Bu imzo shu chatga yuboradigan har bir xabaringiz va media izohingiz oxiriga avtomatik qo'shiladi. Matnni belgilab qalin, kursiv yoki havola qilish mumkin. Stiker, ovozli xabar va doira videoga qo'shilmaydi.");
        root.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 10, 24, 6));

        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Avto-imzo");
        b.setView(root);
        b.setPositiveButton("Saqlash", (d, w) -> {
            CharSequence[] arr = {new SpannableStringBuilder(edit.getText())};
            ArrayList<TLRPC.MessageEntity> ents = MediaDataController.getInstance(account).getEntities(arr, true, false);
            String text = arr[0] == null ? "" : arr[0].toString().trim();
            if (text.isEmpty()) {
                MgAutoText.save(account, did, false, "", null);
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Avto-imzo o'chirildi").show();
            } else {
                if (ents != null) {
                    for (int i = ents.size() - 1; i >= 0; i--) {
                        TLRPC.MessageEntity en = ents.get(i);
                        if (en.offset >= text.length()) {
                            ents.remove(i);
                        } else if (en.offset + en.length > text.length()) {
                            en.length = text.length() - en.offset;
                        }
                    }
                }
                MgAutoText.save(account, did, activeCell.isChecked(), text, ents);
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, activeCell.isChecked() ? "Avto-imzo yoqildi" : "Avto-imzo saqlandi (o'chiq)").show();
            }
        });
        if (!TextUtils.isEmpty(e.text)) {
            b.setNeutralButton("O'chirish", (d, w) -> {
                MgAutoText.save(account, did, false, "", null);
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Avto-imzo o'chirildi").show();
            });
        }
        b.setNegativeButton("Bekor qilish", null);
        f.showDialog(b.create());
    }

    // ================= Qo'shilish so'rovlari =================

    private static String ajChatKey(int account) {
        return "aj_chats_" + account;
    }

    public static boolean isAutoAcceptAll() {
        return MgConfig.getBool("aj_all", false);
    }

    private static HashSet<Long> autoAcceptChats(int account) {
        HashSet<Long> set = new HashSet<>();
        String raw = MgConfig.getString(ajChatKey(account), "");
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(",")) {
                try {
                    set.add(Long.parseLong(s));
                } catch (Throwable ignore) {
                }
            }
        }
        return set;
    }

    public static boolean isAutoAcceptChat(int account, long chatId) {
        return autoAcceptChats(account).contains(chatId);
    }

    public static void setAutoAcceptChat(int account, long chatId, boolean on) {
        HashSet<Long> set = autoAcceptChats(account);
        if (on) {
            set.add(chatId);
        } else {
            set.remove(chatId);
        }
        MgConfig.setString(ajChatKey(account), TextUtils.join(",", set));
    }

    /** MessagesController: kimdir qo'shilish so'rovini yuborganda chaqiriladi */
    public static void onPendingJoinRequests(int account, TL_update.TL_updatePendingJoinRequests update) {
        try {
            if (update == null || update.requests_pending <= 0 || update.peer == null) {
                return;
            }
            long chatId;
            if (update.peer instanceof TLRPC.TL_peerChannel) {
                chatId = update.peer.channel_id;
            } else if (update.peer instanceof TLRPC.TL_peerChat) {
                chatId = update.peer.chat_id;
            } else {
                return;
            }
            if (!isAutoAcceptAll() && !isAutoAcceptChat(account, chatId)) {
                return;
            }
            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.Chat chat = mc.getChat(chatId);
            if (chat == null || !ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_INVITE)) {
                return;
            }
            TLRPC.TL_messages_hideAllChatJoinRequests req = new TLRPC.TL_messages_hideAllChatJoinRequests();
            req.approved = true;
            req.peer = mc.getInputPeer(-chatId);
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
                if (res instanceof TLRPC.Updates) {
                    mc.processUpdates((TLRPC.Updates) res, false);
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void showJoinRequests(BaseFragment f, int account, TLRPC.Chat chat) {
        Context ctx = f.getParentActivity();
        if (ctx == null || chat == null) {
            return;
        }
        MessagesController mc = MessagesController.getInstance(account);
        AlertDialog progress = new AlertDialog(ctx, AlertDialog.ALERT_TYPE_SPINNER);
        progress.showDelayed(200);
        TLRPC.TL_messages_getChatInviteImporters req = new TLRPC.TL_messages_getChatInviteImporters();
        req.requested = true;
        req.peer = mc.getInputPeer(-chat.id);
        req.offset_user = new TLRPC.TL_inputUserEmpty();
        req.limit = 1;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            try {
                progress.dismiss();
            } catch (Throwable ignore) {
            }
            if (!(res instanceof TLRPC.TL_messages_chatInviteImporters)) {
                BulletinFactory.of(f).createErrorBulletin(err != null ? "Xato: " + err.text : "So'rovlarni olib bo'lmadi").show();
                return;
            }
            int pending = ((TLRPC.TL_messages_chatInviteImporters) res).count;
            showJoinRequestsMenu(f, account, chat, pending);
        }));
    }

    private static void showJoinRequestsMenu(BaseFragment f, int account, TLRPC.Chat chat, int pending) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        boolean auto = isAutoAcceptChat(account, chat.id);
        ArrayList<CharSequence> items = new ArrayList<>();
        ArrayList<Integer> actions = new ArrayList<>();
        if (pending > 0) {
            items.add("Hammasini qabul qilish (" + pending + ")");
            actions.add(1);
            items.add("Hammasini rad etish (" + pending + ")");
            actions.add(2);
            if (pending > 1) {
                items.add("Ma'lum sonini qabul qilish…");
                actions.add(3);
                items.add("Ma'lum sonini rad etish…");
                actions.add(4);
            }
        }
        items.add(auto ? "Avtomatik qabul qilishni o'chirish" : "Yangi so'rovlarni avtomatik qabul qilish");
        actions.add(5);
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Qo'shilish so'rovlari");
        b.setMessage(pending > 0 ? "Kutilayotgan so'rovlar: " + pending : "Hozircha kutilayotgan so'rov yo'q.");
        b.setItems(items.toArray(new CharSequence[0]), (d, which) -> {
            int action = actions.get(which);
            switch (action) {
                case 1:
                case 2:
                    processAll(f, account, chat, action == 1, pending);
                    break;
                case 3:
                case 4:
                    askCount(f, pending, n -> processSome(f, account, chat, action == 3, n));
                    break;
                case 5:
                    setAutoAcceptChat(account, chat.id, !auto);
                    BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, !auto
                            ? "Bu chatga yangi so'rovlar avtomatik qabul qilinadi"
                            : "Avtomatik qabul qilish o'chirildi").show();
                    break;
            }
        });
        f.showDialog(b.create());
    }

    private interface IntCallback {
        void run(int n);
    }

    private static void askCount(BaseFragment f, int max, IntCallback cb) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        EditTextBoldCursor et = new EditTextBoldCursor(ctx);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        et.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        et.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        et.setHint("1 – " + max);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        et.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        et.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        FrameLayout fl = new FrameLayout(ctx);
        fl.addView(et, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 6));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Nechtasi?");
        b.setView(fl);
        b.setPositiveButton("Boshlash", (d, w) -> {
            int n;
            try {
                n = Integer.parseInt(et.getText().toString().trim());
            } catch (Throwable e) {
                n = 0;
            }
            if (n > 0) {
                cb.run(Math.min(n, max));
            }
        });
        b.setNegativeButton("Bekor qilish", null);
        f.showDialog(b.create());
        AndroidUtilities.runOnUIThread(() -> {
            et.requestFocus();
            AndroidUtilities.showKeyboard(et);
        }, 250);
    }

    private static void processAll(BaseFragment f, int account, TLRPC.Chat chat, boolean approve, int pending) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_hideAllChatJoinRequests req = new TLRPC.TL_messages_hideAllChatJoinRequests();
        req.approved = approve;
        req.peer = mc.getInputPeer(-chat.id);
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res instanceof TLRPC.Updates) {
                mc.processUpdates((TLRPC.Updates) res, false);
            }
            if (err == null) {
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, (approve ? "Qabul qilindi: " : "Rad etildi: ") + pending + " ta so'rov").show();
            } else {
                BulletinFactory.of(f).createErrorBulletin("Xato: " + err.text).show();
            }
        }));
    }

    /** N ta so'rovni birma-bir qayta ishlaydi (flood-wait'ga chidamli) */
    private static void processSome(BaseFragment f, int account, TLRPC.Chat chat, boolean approve, int target) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        final boolean[] cancelled = {false};
        final AlertDialog progress = new AlertDialog(ctx, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setMessage((approve ? "Qabul qilinmoqda" : "Rad etilmoqda") + ": 0 / " + target);
        progress.setCanCancel(true);
        progress.setOnCancelListener(d -> cancelled[0] = true);
        progress.show();
        new JoinWorker(f, account, chat, approve, target, progress, cancelled).next();
    }

    private static class JoinWorker {
        final BaseFragment f;
        final int account;
        final TLRPC.Chat chat;
        final boolean approve;
        final int target;
        final AlertDialog progress;
        final boolean[] cancelled;
        final MessagesController mc;
        final ArrayList<TLRPC.TL_chatInviteImporter> queue = new ArrayList<>();
        final ArrayList<TLRPC.User> users = new ArrayList<>();
        int attempted, done, floodRetries;
        boolean exhausted;

        JoinWorker(BaseFragment f, int account, TLRPC.Chat chat, boolean approve, int target, AlertDialog progress, boolean[] cancelled) {
            this.f = f;
            this.account = account;
            this.chat = chat;
            this.approve = approve;
            this.target = target;
            this.progress = progress;
            this.cancelled = cancelled;
            this.mc = MessagesController.getInstance(account);
        }

        void finish() {
            try {
                progress.dismiss();
            } catch (Throwable ignore) {
            }
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, (approve ? "Qabul qilindi: " : "Rad etildi: ") + done + " ta so'rov").show();
        }

        void next() {
            if (cancelled[0] || attempted >= target) {
                finish();
                return;
            }
            if (queue.isEmpty()) {
                if (exhausted) {
                    finish();
                    return;
                }
                loadPage();
                return;
            }
            TLRPC.TL_chatInviteImporter imp = queue.get(0);
            TLRPC.User user = null;
            for (TLRPC.User u : users) {
                if (u.id == imp.user_id) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                queue.remove(0);
                attempted++;
                next();
                return;
            }
            TLRPC.TL_messages_hideChatJoinRequest req = new TLRPC.TL_messages_hideChatJoinRequest();
            req.approved = approve;
            req.peer = mc.getInputPeer(-chat.id);
            req.user_id = mc.getInputUser(user);
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (err != null && err.text != null && err.text.startsWith("FLOOD_WAIT") && floodRetries < 3) {
                    floodRetries++;
                    int secs = 5;
                    try {
                        secs = Integer.parseInt(err.text.substring(err.text.lastIndexOf('_') + 1));
                    } catch (Throwable ignore) {
                    }
                    secs = Math.max(1, Math.min(secs, 30));
                    progress.setMessage("Telegram cheklovi: " + secs + " soniya kutilmoqda…");
                    AndroidUtilities.runOnUIThread(this::next, secs * 1000L + 250);
                    return;
                }
                floodRetries = 0;
                queue.remove(0);
                attempted++;
                if (err == null) {
                    done++;
                    if (res instanceof TLRPC.Updates) {
                        mc.processUpdates((TLRPC.Updates) res, false);
                    }
                }
                progress.setMessage((approve ? "Qabul qilinmoqda" : "Rad etilmoqda") + ": " + attempted + " / " + target);
                AndroidUtilities.runOnUIThread(this::next, 120);
            }));
        }

        void loadPage() {
            TLRPC.TL_messages_getChatInviteImporters req = new TLRPC.TL_messages_getChatInviteImporters();
            req.requested = true;
            req.peer = mc.getInputPeer(-chat.id);
            req.offset_user = new TLRPC.TL_inputUserEmpty();
            req.limit = 50;
            // Qayta ishlangan so'rovlar ro'yxatdan chiqib ketadi, shuning uchun har doim boshidan olamiz
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.TL_messages_chatInviteImporters) {
                    TLRPC.TL_messages_chatInviteImporters r = (TLRPC.TL_messages_chatInviteImporters) res;
                    mc.putUsers(r.users, false);
                    users.addAll(r.users);
                    queue.addAll(r.importers);
                    if (r.importers.isEmpty()) {
                        exhausted = true;
                    }
                } else {
                    exhausted = true;
                }
                next();
            }));
        }
    }

    // ================= Barcha akkauntlardan qo'shilish =================

    public static boolean canJoinAll(TLRPC.Chat chat) {
        return chat != null && ChatObject.isChannel(chat) && ChatObject.getPublicUsername(chat) != null
                && UserConfig.getActivatedAccountsCount() > 1;
    }

    public static void joinAllAccounts(BaseFragment f, int currentAccount, TLRPC.Chat chat) {
        String username = ChatObject.getPublicUsername(chat);
        if (username == null) {
            return;
        }
        ArrayList<Integer> targets = new ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (a != currentAccount && UserConfig.getInstance(a).isClientActivated()) {
                targets.add(a);
            }
        }
        if (targets.isEmpty()) {
            BulletinFactory.of(f).createErrorBulletin("Boshqa akkaunt yo'q").show();
            return;
        }
        BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, targets.size() + " ta akkauntdan qo'shilinmoqda…").show();
        final String uname = username.toLowerCase(Locale.US);
        final AtomicInteger remaining = new AtomicInteger(targets.size());
        final AtomicInteger joined = new AtomicInteger();
        final int total = targets.size();
        for (int account : targets) {
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = uname;
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
                if (!(res instanceof TLRPC.TL_contacts_resolvedPeer) || ((TLRPC.TL_contacts_resolvedPeer) res).chats.isEmpty()) {
                    finishJoin(f, remaining, joined, total, false);
                    return;
                }
                TLRPC.TL_contacts_resolvedPeer rp = (TLRPC.TL_contacts_resolvedPeer) res;
                TLRPC.Chat c = rp.chats.get(0);
                if (rp.peer instanceof TLRPC.TL_peerChannel) {
                    for (TLRPC.Chat ch : rp.chats) {
                        if (ch.id == rp.peer.channel_id) {
                            c = ch;
                            break;
                        }
                    }
                }
                if (!ChatObject.isChannel(c)) {
                    finishJoin(f, remaining, joined, total, false);
                    return;
                }
                MessagesController.getInstance(account).putChats(rp.chats, false);
                MessagesController.getInstance(account).putUsers(rp.users, false);
                TLRPC.TL_inputChannel input = new TLRPC.TL_inputChannel();
                input.channel_id = c.id;
                input.access_hash = c.access_hash;
                TLRPC.TL_channels_joinChannel join = new TLRPC.TL_channels_joinChannel();
                join.channel = input;
                ConnectionsManager.getInstance(account).sendRequest(join, (res2, err2) -> {
                    boolean ok = false;
                    if (res2 instanceof TLRPC.TL_chatInviteJoinResultOk) {
                        TLRPC.Updates u = ((TLRPC.TL_chatInviteJoinResultOk) res2).updates;
                        if (u != null) {
                            MessagesController.getInstance(account).processUpdates(u, false);
                        }
                        ok = true;
                    } else if (res2 instanceof TLRPC.Updates) {
                        MessagesController.getInstance(account).processUpdates((TLRPC.Updates) res2, false);
                        ok = true;
                    } else if (err2 != null && "USER_ALREADY_PARTICIPANT".equals(err2.text)) {
                        ok = true;
                    }
                    finishJoin(f, remaining, joined, total, ok);
                });
            });
        }
    }

    private static void finishJoin(BaseFragment f, AtomicInteger remaining, AtomicInteger joined, int total, boolean ok) {
        if (ok) {
            joined.incrementAndGet();
        }
        if (remaining.decrementAndGet() == 0) {
            AndroidUtilities.runOnUIThread(() -> {
                int j = joined.get();
                if (j == total) {
                    BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Barcha " + total + " ta akkaunt qo'shildi").show();
                } else {
                    BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Qo'shildi: " + j + " / " + total + " ta akkaunt").show();
                }
            });
        }
    }

    // ================= Notanishlar =================

    public static String strangerMenuTitle(int account, long did) {
        return MgStrangers.belongsInInbox(account, did) ? "Notanish emas" : "Notanishlarga qaytarish";
    }

    public static boolean canToggleStranger(int account, TLRPC.User user) {
        return user != null && MgStrangers.isEnabled(account) && MgStrangers.isStranger(user);
    }

    public static void toggleStranger(BaseFragment f, int account, long did) {
        if (MgStrangers.belongsInInbox(account, did)) {
            MgStrangers.trust(account, did);
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Chat asosiy ro'yxatga qaytarildi").show();
        } else {
            MgStrangers.untrust(account, did);
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Chat \"Notanishlar\" jildiga o'tkazildi").show();
        }
        MgStrangers.refresh(account);
    }

    // ================= Bir martalik ovozli xabar =================

    public static boolean isOneTimeVoice(int account, long did) {
        return MgConfig.getBool("once_voice_" + account + "_" + did, false);
    }

    public static boolean canOneTimeVoice(TLRPC.User user) {
        return user != null && !user.bot && !UserObject.isUserSelf(user) && !UserObject.isService(user.id);
    }

    public static String oneTimeVoiceMenuTitle(int account, long did) {
        return isOneTimeVoice(account, did) ? "Bir martalik ovoz: yoqilgan" : "Bir martalik ovoz";
    }

    public static void toggleOneTimeVoice(BaseFragment f, int account, long did) {
        boolean on = !isOneTimeVoice(account, did);
        MgConfig.setBool("once_voice_" + account + "_" + did, on);
        BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip, on
                ? "Bu chatda ovozli va doira xabarlar bir marta tinglanadigan bo'lib yuboriladi"
                : "Bir martalik ovozli xabar o'chirildi").show();
    }
}
