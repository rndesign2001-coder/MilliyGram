/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgTranslit;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Xabarlar bilan kundalik yordamchilar:
 *  - Kirill ↔ Lotin o'girish;
 *  - "Keyin eslatish" (Saqlangan xabarlarga rejalashtirilgan eslatma);
 *  - tezkor shablonlar;
 *  - firibgarlikdan ogohlantirish.
 */
public class MgMessageTools {

    // ================= Kirill ↔ Lotin =================

    public static String messageText(MessageObject m, MessageObject.GroupedMessages group) {
        if (m == null || m.messageOwner == null) {
            return null;
        }
        MessageObject primary = group != null ? group.findCaptionMessageObject() : null;
        if (primary != null && primary.messageOwner != null && !TextUtils.isEmpty(primary.messageOwner.message)) {
            return primary.messageOwner.message;
        }
        return m.messageOwner.message;
    }

    public static void showTranslit(BaseFragment f, String text) {
        Context ctx = f.getParentActivity();
        if (ctx == null || TextUtils.isEmpty(text)) {
            return;
        }
        boolean cyr = MgTranslit.isCyrillic(text);
        final String out = MgTranslit.flip(text);
        TextView tv = new TextView(ctx);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tv.setTextIsSelectable(true);
        tv.setText(out);
        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        scroll.addView(tv);
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle(cyr ? "Lotin yozuvida" : "Kirill yozuvida");
        b.setView(scroll);
        b.setPositiveButton("Nusxalash", (d, w) -> {
            AndroidUtilities.addToClipboard(out);
            BulletinFactory.of(f).createCopyBulletin("Matn nusxalandi").show();
        });
        b.setNegativeButton("Yopish", null);
        f.showDialog(b.create());
    }

    // ================= Keyin eslatish =================

    public static void showRemind(ChatActivity f, MessageObject m, MessageObject.GroupedMessages group) {
        Context ctx = f.getParentActivity();
        if (ctx == null || m == null) {
            return;
        }
        final int[] minutes = {30, 60, 180, -1, -2};
        CharSequence[] names = {"30 daqiqadan keyin", "1 soatdan keyin", "3 soatdan keyin", "Ertaga soat 9:00 da", "Boshqa vaqt…"};
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Qachon eslatay?");
        b.setItems(names, (d, w) -> {
            Calendar c = Calendar.getInstance();
            if (minutes[w] > 0) {
                c.add(Calendar.MINUTE, minutes[w]);
                schedule(f, m, group, (int) (c.getTimeInMillis() / 1000));
            } else if (minutes[w] == -1) {
                c.add(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 9);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                schedule(f, m, group, (int) (c.getTimeInMillis() / 1000));
            } else {
                AlertsCreator.createScheduleDatePickerDialog(ctx, f.getDialogId(), (notify, date, repeat) -> schedule(f, m, group, date));
            }
        });
        f.showDialog(b.create());
    }

    private static void schedule(ChatActivity f, MessageObject m, MessageObject.GroupedMessages group, int date) {
        int account = f.getCurrentAccount();
        long self = UserConfig.getInstance(account).getClientUserId();
        int now = ConnectionsManager.getInstance(account).getCurrentTime();
        if (date < now + 60) {
            date = now + 60;
        }
        SendMessagesHelper helper = SendMessagesHelper.getInstance(account);
        boolean forwarded = false;
        try {
            if (m.canForwardMessage() && !f.getMessagesController().isChatNoForwards(f.getCurrentChat()) && !(m.messageOwner != null && m.messageOwner.noforwards)) {
                ArrayList<MessageObject> list = new ArrayList<>();
                if (group != null && group.messages != null && !group.messages.isEmpty()) {
                    list.addAll(group.messages);
                } else {
                    list.add(m);
                }
                helper.sendMessage(list, self, false, false, true, date, 0);
                forwarded = true;
            }
        } catch (Throwable e) {
            forwarded = false;
        }
        String title = chatTitle(f);
        StringBuilder sb = new StringBuilder("🔔 Eslatma");
        if (!TextUtils.isEmpty(title)) {
            sb.append(" · ").append(title);
        }
        String link = messageLink(f, m);
        if (!forwarded) {
            String text = messageText(m, group);
            if (!TextUtils.isEmpty(text)) {
                sb.append("\n\n").append(text.length() > 3000 ? text.substring(0, 3000) + "…" : text);
            }
        }
        if (link != null) {
            sb.append("\n\n").append(link);
        }
        if (!forwarded || link != null || !TextUtils.isEmpty(title)) {
            helper.sendMessage(SendMessagesHelper.SendMessageParams.of(sb.toString(), self, null, null, null, false, null, null, null, true, date, 0, null, false));
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date * 1000L);
        String when = String.format(java.util.Locale.US, "%02d.%02d %02d:%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Eslatma qo'yildi: " + when + ". U \"Saqlangan xabarlar\"ga keladi").show();
    }

    private static String chatTitle(ChatActivity f) {
        if (f.getCurrentChat() != null) {
            return f.getCurrentChat().title;
        }
        if (f.getCurrentUser() != null) {
            return UserObject.getUserName(f.getCurrentUser());
        }
        return null;
    }

    private static String messageLink(ChatActivity f, MessageObject m) {
        TLRPC.Chat chat = f.getCurrentChat();
        if (chat == null || !ChatObject.isChannel(chat) || m.getId() <= 0) {
            return null;
        }
        String username = ChatObject.getPublicUsername(chat);
        if (!TextUtils.isEmpty(username)) {
            return "https://t.me/" + username + "/" + m.getId();
        }
        return "https://t.me/c/" + chat.id + "/" + m.getId();
    }

    // ================= Tezkor shablonlar =================

    public static ArrayList<String> templates() {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(MgConfig.getString("tpl_list", "[]"));
            for (int i = 0; i < a.length(); i++) {
                list.add(a.getString(i));
            }
        } catch (Throwable ignore) {
        }
        return list;
    }

    private static void saveTemplates(List<String> list) {
        JSONArray a = new JSONArray();
        for (String s : list) {
            a.put(s);
        }
        MgConfig.setString("tpl_list", a.toString());
    }

    public static void saveTemplate(BaseFragment f, String text) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(text.trim())) {
            return;
        }
        ArrayList<String> list = templates();
        if (!list.contains(text.trim())) {
            list.add(0, text.trim());
            while (list.size() > 50) {
                list.remove(list.size() - 1);
            }
            saveTemplates(list);
        }
        BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Shablon saqlandi. Chat → ⋮ → \"Tezkor shablonlar\"").show();
    }

    private static String preview(String s) {
        String one = s.replace('\n', ' ');
        return one.length() > 60 ? one.substring(0, 60) + "…" : one;
    }

    /** chat != null — tanlangan shablon yozish maydoniga qo'yiladi; aks holda faqat boshqaruv */
    public static void showTemplates(BaseFragment f, ChatActivity chat) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        ArrayList<String> list = templates();
        CharSequence[] items = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            items[i] = preview(list.get(i));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Tezkor shablonlar");
        if (list.isEmpty()) {
            b.setMessage("Hali shablon yo'q. Ko'p yoziladigan gaplarni (manzil, karta raqami, \"hozir bandman\"…) qo'shing — keyin bir bosishda yozish maydoniga qo'yiladi.\n\nYozgan matningizni yuborish tugmasini uzoq bosib ham \"Shablonga saqlash\" mumkin.");
        } else {
            b.setItems(items, (d, w) -> {
                if (chat != null) {
                    ChatActivityEnterView enter = chat.getChatActivityEnterView();
                    if (enter != null) {
                        CharSequence cur = enter.getFieldText();
                        String t = list.get(w);
                        enter.setFieldText(cur != null && cur.toString().trim().length() > 0 ? cur.toString().trim() + " " + t : t);
                        enter.openKeyboard();
                    }
                } else {
                    editTemplate(f, list, w);
                }
            });
        }
        b.setPositiveButton("+ Yangi", (d, w) -> editTemplate(f, list, -1));
        if (!list.isEmpty()) {
            b.setNeutralButton("Tahrirlash", (d, w) -> {
                AlertDialog.Builder e = new AlertDialog.Builder(ctx, f.getResourceProvider());
                e.setTitle("Qaysi shablon?");
                e.setItems(items, (d2, i) -> editTemplate(f, list, i));
                f.showDialog(e.create());
            });
        }
        b.setNegativeButton("Yopish", null);
        f.showDialog(b.create());
    }

    private static void editTemplate(BaseFragment f, ArrayList<String> list, int index) {
        Context ctx = f.getParentActivity();
        if (ctx == null) {
            return;
        }
        EditTextBoldCursor edit = new EditTextBoldCursor(ctx);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setBackground(null);
        edit.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setMaxLines(8);
        edit.setHint("Masalan: Manzilim — Toshkent, Chilonzor 5-kvartal");
        if (index >= 0) {
            edit.setText(list.get(index));
        }
        FrameLayout fl = new FrameLayout(ctx);
        fl.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle(index >= 0 ? "Shablonni tahrirlash" : "Yangi shablon");
        b.setView(fl);
        b.setPositiveButton("Saqlash", (d, w) -> {
            String t = edit.getText() == null ? "" : edit.getText().toString().trim();
            ArrayList<String> cur = templates();
            if (index >= 0 && index < cur.size()) {
                if (t.isEmpty()) {
                    cur.remove(index);
                } else {
                    cur.set(index, t);
                }
            } else if (!t.isEmpty()) {
                cur.add(0, t);
            }
            saveTemplates(cur);
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Saqlandi").show();
        });
        if (index >= 0) {
            b.setNeutralButton("O'chirish", (d, w) -> {
                ArrayList<String> cur = templates();
                if (index < cur.size()) {
                    cur.remove(index);
                    saveTemplates(cur);
                }
            });
        }
        b.setNegativeButton("Bekor qilish", null);
        f.showDialog(b.create());
        AndroidUtilities.runOnUIThread(() -> {
            edit.requestFocus();
            AndroidUtilities.showKeyboard(edit);
        }, 250);
    }

    // ================= Firibgarlikdan ogohlantirish =================

    public static boolean isScamGuardEnabled() {
        return MgConfig.getBool("scam_guard", true);
    }

    private static final HashSet<String> shownThisSession = new HashSet<>();

    private static final Pattern CARD = Pattern.compile("(?<!\\d)(8600|9860|5614|6262|4\\d{3}|5[1-5]\\d{2}|2\\d{3})[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}(?!\\d)");
    private static final Pattern CODE = Pattern.compile("(sms|смс|код|kod)[^\\n]{0,40}(ayt|yubor|tashla|jo'nat|jo‘nat|yoz|kelgan|пришл|скаж|отправ|продикт|напиш|пришед)|(ayt|yubor|tashla|пришл|скаж)[^\\n]{0,25}(sms|смс|код|kod)");
    private static final Pattern MONEY = Pattern.compile("pul (o'tkaz|o‘tkaz|tashla|yubor)|o'tkazib yubor|hisobingizga|to'lov qiling|to‘lov qiling|oldindan to'lov|переведи|переведите|скинь|предоплат|click orqali|payme orqali|kartangizga");
    private static final Pattern PRIZE = Pattern.compile("yutuq|yutib oldingiz|g'olib bo'ldingiz|sovg'a oling|bonus oling|бесплатн|выиграли|приз|розыгрыш|вы победили|подарок");
    private static final Pattern SECRET = Pattern.compile("\\bcvv\\b|\\bcvc\\b|amal qilish muddat|срок действия карт|pin[- ]?kod|пин[- ]?код");
    private static final Pattern LINK = Pattern.compile("(bit\\.ly|tinyurl|cutt\\.ly|clck\\.ru|goo\\.gl|is\\.gd|t\\.co/|[a-z0-9-]*(telegram|telegrarn|teleqram|payme|click|uzcard|humo)[a-z0-9-]*\\.(?!org\\b|me\\b|uz\\b)[a-z]{2,6}|\\.apk\\b)");

    public static void scamCheck(ChatActivity f, List<MessageObject> messages) {
        try {
            if (!isScamGuardEnabled() || messages == null || messages.isEmpty() || f.getParentActivity() == null) {
                return;
            }
            long did = f.getDialogId();
            int account = f.getCurrentAccount();
            if (did <= 0) {
                return;
            }
            String key = account + "_" + did;
            if (shownThisSession.contains(key) || MgConfig.getBool("scam_ok_" + key, false)) {
                return;
            }
            TLRPC.User user = MessagesController.getInstance(account).getUser(did);
            if (user == null || user.bot || user.contact || user.verified || user.support || UserObject.isUserSelf(user) || UserObject.isService(user.id)) {
                return;
            }
            ArrayList<String> reasons = new ArrayList<>();
            int checked = 0;
            for (int i = 0; i < messages.size() && checked < 40; i++) {
                MessageObject m = messages.get(i);
                if (m == null || m.isOut() || m.messageOwner == null || m.getDialogId() != did) {
                    continue;
                }
                checked++;
                String t = m.messageOwner.message;
                if (TextUtils.isEmpty(t)) {
                    continue;
                }
                String low = t.toLowerCase();
                if (CARD.matcher(low).find() && !reasons.contains("karta raqami")) reasons.add("karta raqami");
                if (CODE.matcher(low).find() && !reasons.contains("SMS kod so'rash")) reasons.add("SMS kod so'rash");
                if (MONEY.matcher(low).find() && !reasons.contains("pul o'tkazishni so'rash")) reasons.add("pul o'tkazishni so'rash");
                if (PRIZE.matcher(low).find() && !reasons.contains("\"yutuq\" yoki \"sovg'a\" va'dasi")) reasons.add("\"yutuq\" yoki \"sovg'a\" va'dasi");
                if (SECRET.matcher(low).find() && !reasons.contains("karta ma'lumotlarini so'rash")) reasons.add("karta ma'lumotlarini so'rash");
                if (LINK.matcher(low).find() && !reasons.contains("shubhali havola")) reasons.add("shubhali havola");
            }
            if (reasons.isEmpty()) {
                return;
            }
            shownThisSession.add(key);
            AndroidUtilities.runOnUIThread(() -> showScamWarning(f, account, did, user, reasons), 500);
        } catch (Throwable ignore) {
        }
    }

    private static void showScamWarning(ChatActivity f, int account, long did, TLRPC.User user, ArrayList<String> reasons) {
        if (f.getParentActivity() == null || f.getFragmentView() == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(UserObject.getUserName(user)).append(" kontaktlaringizda yo'q, xabarlarida esa quyidagilar bor: ")
                .append(TextUtils.join(", ", reasons)).append(".\n\n")
                .append("• Hech kimga SMS kod, karta PIN-kodi, CVV va amal qilish muddatini aytmang.\n")
                .append("• Bank, Telegram, Click, Payme xodimlari hech qachon kod so'ramaydi.\n")
                .append("• Notanish havola va .apk fayllarni ochmang.\n")
                .append("• Pul so'rashsa — shu odamga boshqa yo'l bilan (qo'ng'iroq qilib) tekshiring.");
        AlertDialog.Builder b = new AlertDialog.Builder(f.getParentActivity(), f.getResourceProvider());
        b.setTitle("⚠️ Ehtiyot bo'ling");
        b.setMessage(sb.toString());
        b.setPositiveButton("Tushundim", null);
        b.setNeutralButton("Bu odamni taniyman", (d, w) -> MgConfig.setBool("scam_ok_" + account + "_" + did, true));
        b.setNegativeButton("Bloklash", (d, w) -> {
            MessagesController mc = MessagesController.getInstance(account);
            mc.blockPeer(did);
            mc.reportSpam(did, user, null, null, false);
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Bloklandi va spam deb belgilandi").show();
        });
        AlertDialog dialog = b.create();
        f.showDialog(dialog);
        TextView btn = (TextView) dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    // ================= Tez saqlash (bulutcha) =================

    public static void quickSave(ChatActivity f, ArrayList<MessageObject> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return;
        }
        int account = f.getCurrentAccount();
        long self = UserConfig.getInstance(account).getClientUserId();
        SendMessagesHelper.getInstance(account).sendMessage(msgs, self, false, false, true, 0, 0);
        BulletinFactory.of(f).createSimpleBulletin(R.raw.saved_messages, msgs.size() > 1
                ? msgs.size() + " ta xabar Saqlangan xabarlarga saqlandi"
                : "Saqlangan xabarlarga saqlandi").show();
    }

    // ================= Matnning bir qismidan nusxa olish =================

    public static void showSelectText(BaseFragment f, String text) {
        Context ctx = f.getParentActivity();
        if (ctx == null || TextUtils.isEmpty(text)) {
            return;
        }
        org.telegram.ui.ActionBar.BottomSheet.Builder b = new org.telegram.ui.ActionBar.BottomSheet.Builder(ctx, false, f.getResourceProvider());
        android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        title.setText("Kerakli qismini barmoq bilan belgilang");
        box.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 22, 16, 22, 10));
        TextView tv = new TextView(ctx);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tv.setTextIsSelectable(true);
        tv.setHighlightColor(Theme.multAlpha(Theme.getColor(Theme.key_featuredStickers_addButton), 0.35f));
        tv.setText(text);
        tv.setLineSpacing(AndroidUtilities.dp(2), 1f);
        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int max = (int) (AndroidUtilities.displaySize.y * 0.6f);
                super.onMeasure(widthMeasureSpec, android.view.View.MeasureSpec.makeMeasureSpec(max, android.view.View.MeasureSpec.AT_MOST));
            }
        };
        scroll.addView(tv);
        scroll.setPadding(AndroidUtilities.dp(22), 0, AndroidUtilities.dp(22), 0);
        box.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        android.widget.LinearLayout buttons = new android.widget.LinearLayout(ctx);
        buttons.setGravity(Gravity.END);
        TextView copyAll = makeTextButton(ctx, "HAMMASINI NUSXALASH");
        TextView copySel = makeTextButton(ctx, "BELGILANGANINI NUSXALASH");
        buttons.addView(copyAll);
        buttons.addView(copySel);
        box.addView(buttons, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 8, 4, 8, 4));
        b.setCustomView(box);
        org.telegram.ui.ActionBar.BottomSheet sheet = b.create();
        copyAll.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(text);
            sheet.dismiss();
            BulletinFactory.of(f).createCopyBulletin("Matn nusxalandi").show();
        });
        copySel.setOnClickListener(v -> {
            int st = Math.max(0, Math.min(tv.getSelectionStart(), tv.getSelectionEnd()));
            int en = Math.max(tv.getSelectionStart(), tv.getSelectionEnd());
            if (en <= st) {
                title.setText("⚠️ Avval matnning bir qismini barmoq bilan belgilang");
                title.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
                return;
            }
            AndroidUtilities.addToClipboard(text.substring(st, Math.min(en, text.length())));
            sheet.dismiss();
            BulletinFactory.of(f).createCopyBulletin("Belgilangan qism nusxalandi").show();
        });
        f.showDialog(sheet);
    }

    private static TextView makeTextButton(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        t.setGravity(Gravity.CENTER);
        t.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
        t.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_featuredStickers_addButton), 0.15f), 2));
        return t;
    }

    // ================= Xabar tafsilotlari =================

    private static String fullDate(int unix) {
        if (unix <= 0) {
            return "—";
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(unix * 1000L);
        String[] months = {"yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"};
        return String.format(java.util.Locale.US, "%d-%s %d, %02d:%02d:%02d", c.get(Calendar.DAY_OF_MONTH), months[c.get(Calendar.MONTH)],
                c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    }

    private static String peerName(int account, TLRPC.Peer peer) {
        if (peer == null) {
            return null;
        }
        MessagesController mc = MessagesController.getInstance(account);
        long id = MessageObject.getPeerId(peer);
        if (id > 0) {
            TLRPC.User u = mc.getUser(id);
            String un = u == null ? null : UserObject.getPublicUsername(u);
            return (u == null ? "?" : UserObject.getUserName(u)) + (un != null ? " (@" + un + ")" : "") + " · ID " + id;
        }
        TLRPC.Chat c = mc.getChat(-id);
        String un = c == null ? null : ChatObject.getPublicUsername(c);
        return (c == null ? "?" : c.title) + (un != null ? " (@" + un + ")" : "") + " · ID " + (-id);
    }

    public static void showDetails(ChatActivity f, MessageObject m, MessageObject.GroupedMessages group) {
        Context ctx = f.getParentActivity();
        if (ctx == null || m == null || m.messageOwner == null) {
            return;
        }
        int account = f.getCurrentAccount();
        TLRPC.Message msg = m.messageOwner;
        StringBuilder sb = new StringBuilder();
        sb.append("🆔 Xabar ID: ").append(m.getId()).append('\n');
        sb.append("💬 Chat: ").append(peerName(account, msg.peer_id)).append('\n');
        String from = peerName(account, msg.from_id);
        if (from != null) {
            sb.append("👤 Yuboruvchi: ").append(from).append('\n');
        }
        if (!TextUtils.isEmpty(msg.post_author)) {
            sb.append("✍️ Imzo: ").append(msg.post_author).append('\n');
        }
        sb.append("🕒 Yuborilgan: ").append(fullDate(msg.date)).append('\n');
        if (msg.edit_date > 0 && !msg.edit_hide) {
            sb.append("✏️ Tahrirlangan: ").append(fullDate(msg.edit_date)).append('\n');
        }
        if (msg.fwd_from != null) {
            String ff = peerName(account, msg.fwd_from.from_id);
            if (ff == null && !TextUtils.isEmpty(msg.fwd_from.from_name)) {
                ff = msg.fwd_from.from_name + " (yashirin profil)";
            }
            sb.append("↪️ Uzatilgan: ").append(ff == null ? "—" : ff).append('\n');
            sb.append("   Asl sanasi: ").append(fullDate(msg.fwd_from.date)).append('\n');
            if (msg.fwd_from.channel_post != 0) {
                sb.append("   Asl post ID: ").append(msg.fwd_from.channel_post).append('\n');
            }
        }
        if (msg.reply_to != null && msg.reply_to.reply_to_msg_id != 0) {
            sb.append("↩️ Javob: #").append(msg.reply_to.reply_to_msg_id).append(" xabarga").append('\n');
        }
        if (msg.via_bot_id != 0) {
            TLRPC.User bot = MessagesController.getInstance(account).getUser(msg.via_bot_id);
            sb.append("🤖 Bot orqali: ").append(bot == null ? String.valueOf(msg.via_bot_id) : "@" + UserObject.getPublicUsername(bot)).append('\n');
        }
        if (msg.views > 0) {
            sb.append("👁 Ko'rishlar: ").append(msg.views).append('\n');
        }
        if (msg.forwards > 0) {
            sb.append("🔁 Ulashishlar: ").append(msg.forwards).append('\n');
        }
        int replies = m.getRepliesCount();
        if (replies > 0) {
            sb.append("💭 Izohlar: ").append(replies).append('\n');
        }
        if (msg.grouped_id != 0) {
            sb.append("🖼 Albom: ").append(group != null && group.messages != null ? group.messages.size() + " ta element" : "ha").append('\n');
        }
        String text = messageText(m, group);
        if (!TextUtils.isEmpty(text)) {
            sb.append("🔤 Matn: ").append(text.length()).append(" belgi, ").append(text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length).append(" so'z");
            if (msg.entities != null && !msg.entities.isEmpty()) {
                sb.append(", ").append(msg.entities.size()).append(" ta format/havola");
            }
            sb.append('\n');
        }
        TLRPC.Document doc = m.getDocument();
        if (doc != null) {
            String name = m.getFileName();
            sb.append("📎 Fayl: ").append(TextUtils.isEmpty(name) ? "—" : name).append('\n');
            sb.append("   Hajmi: ").append(AndroidUtilities.formatFileSize(doc.size)).append(" · ").append(doc.mime_type).append('\n');
            sb.append("   DC: ").append(doc.dc_id).append('\n');
            double dur = m.getDuration();
            if (dur > 0) {
                int d = (int) Math.round(dur);
                sb.append("   Davomiyligi: ").append(d / 60).append(":").append(String.format(java.util.Locale.US, "%02d", d % 60)).append('\n');
            }
            for (TLRPC.DocumentAttribute a : doc.attributes) {
                if (a instanceof TLRPC.TL_documentAttributeVideo || a instanceof TLRPC.TL_documentAttributeImageSize) {
                    if (a.w > 0 && a.h > 0) {
                        sb.append("   O'lcham: ").append(a.w).append("×").append(a.h).append('\n');
                        break;
                    }
                }
            }
        } else if (msg.media instanceof TLRPC.TL_messageMediaPhoto && msg.media.photo != null) {
            TLRPC.PhotoSize big = FileLoader.getClosestPhotoSizeWithSize(msg.media.photo.sizes, AndroidUtilities.getPhotoSize());
            sb.append("🖼 Rasm");
            if (big != null) {
                sb.append(": ").append(big.w).append("×").append(big.h);
                if (big.size > 0) {
                    sb.append(" · ").append(AndroidUtilities.formatFileSize(big.size));
                }
            }
            sb.append(" · DC ").append(msg.media.photo.dc_id).append('\n');
        }
        if (msg.ttl_period > 0) {
            sb.append("⏳ Avto-o'chish: ").append(msg.ttl_period / 3600).append(" soatdan keyin").append('\n');
        }
        if (msg.silent) {
            sb.append("🔕 Ovozsiz yuborilgan").append('\n');
        }
        if (msg.noforwards) {
            sb.append("🚫 Uzatish taqiqlangan").append('\n');
        }
        String link = messageLink(f, m);
        if (link != null) {
            sb.append("🔗 ").append(link).append('\n');
        }
        final String out = sb.toString().trim();
        TextView tv = new TextView(ctx);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tv.setLineSpacing(AndroidUtilities.dp(3), 1f);
        tv.setTextIsSelectable(true);
        tv.setText(out);
        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        scroll.addView(tv);
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, f.getResourceProvider());
        b.setTitle("Xabar tafsilotlari");
        b.setView(scroll);
        b.setPositiveButton("Yopish", null);
        b.setNeutralButton("Nusxalash", (d, w) -> {
            AndroidUtilities.addToClipboard(out);
            BulletinFactory.of(f).createCopyBulletin("Tafsilotlar nusxalandi").show();
        });
        f.showDialog(b.create());
    }
}
