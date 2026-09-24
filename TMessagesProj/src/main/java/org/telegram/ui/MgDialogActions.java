/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FilePathDatabase;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgLocalFolders;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * Chatlar ro'yxatidagi qo'shimcha amallar: bosh ekranga chiqarish, toifaga qo'shish,
 * guruh/kanalga qo'shish, keshni tozalash, arxivni turlar bo'yicha saralash.
 */
public class MgDialogActions {

    // ---------- Chat turlari (arxiv saralash uchun) ----------

    public static final int KIND_ALL = 0;
    public static final int KIND_USERS = 1;
    public static final int KIND_GROUPS = 2;
    public static final int KIND_CHANNELS = 3;
    public static final int KIND_BOTS = 4;
    public static final int KIND_UNREAD = 5;

    public static final String[] KIND_NAMES = {"Barcha chatlar", "Foydalanuvchilar", "Guruhlar", "Kanallar", "Botlar", "O'qilmaganlar"};
    public static final int[] KIND_ICONS = {R.drawable.msg_media, R.drawable.msg_contacts, R.drawable.msg_groups, R.drawable.msg_channel, R.drawable.msg_bots, R.drawable.msg_markunread};

    public static boolean matchesKind(MessagesController mc, TLRPC.Dialog d, int kind) {
        if (kind == KIND_ALL || d == null) {
            return true;
        }
        if (d instanceof TLRPC.TL_dialogFolder) {
            return false;
        }
        long did = d.id;
        if (kind == KIND_UNREAD) {
            return d.unread_count > 0 || d.unread_mark;
        }
        if (DialogObject.isEncryptedDialog(did)) {
            return kind == KIND_USERS;
        }
        if (did > 0) {
            TLRPC.User u = mc.getUser(did);
            boolean bot = u != null && u.bot;
            return bot ? kind == KIND_BOTS : kind == KIND_USERS;
        }
        TLRPC.Chat c = mc.getChat(-did);
        boolean broadcast = c != null && ChatObject.isChannelAndNotMegaGroup(c);
        return broadcast ? kind == KIND_CHANNELS : kind == KIND_GROUPS;
    }

    public static ArrayList<TLRPC.Dialog> filterKind(MessagesController mc, ArrayList<TLRPC.Dialog> list, int kind) {
        if (kind == KIND_ALL || list == null) {
            return list;
        }
        ArrayList<TLRPC.Dialog> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (matchesKind(mc, list.get(i), kind)) {
                out.add(list.get(i));
            }
        }
        return out;
    }

    // ---------- Bosh ekranga chiqarish ----------

    public static void addShortcuts(BaseFragment fragment, int account, ArrayList<Long> dids) {
        for (Long did : dids) {
            try {
                MediaDataController.getInstance(account).installShortcut(did, MediaDataController.SHORTCUT_TYPE_USER_OR_CHAT);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, dids.size() == 1 ? "Bosh ekranga qo'shildi" : "Bosh ekranga qo'shildi: " + dids.size() + " ta").show();
    }

    // ---------- Toifalar ----------

    public static void addToCategory(BaseFragment fragment, int account, ArrayList<Long> dids) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null || dids.isEmpty()) {
            return;
        }
        ArrayList<MgLocalFolders.Entry> cats = MgLocalFolders.getCategories(account);
        CharSequence[] names = new CharSequence[cats.size() + 1];
        int[] icons = new int[cats.size() + 1];
        for (int i = 0; i < cats.size(); i++) {
            names[i] = cats.get(i).name + "  (" + cats.get(i).always.size() + ")";
            Integer ic = MgLocalFolders.ICONS.get(cats.get(i).icon);
            icons[i] = ic == null ? R.drawable.msg_folders : ic;
        }
        names[cats.size()] = "Yangi toifa yaratish";
        icons[cats.size()] = R.drawable.msg_add;
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        b.setTitle("Toifaga qo'shish");
        b.setItems(names, icons, (d, which) -> {
            if (which == cats.size()) {
                askName(fragment, "Yangi toifa", "", name -> {
                    int id = MgLocalFolders.createCategory(account, name, dids);
                    if (id == 0) {
                        BulletinFactory.of(fragment).createErrorBulletin("Toifalar soni chegaraga yetdi").show();
                    } else {
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "\"" + name + "\" toifasi yaratildi").show();
                    }
                });
            } else {
                MgLocalFolders.Entry e = cats.get(which);
                int added = MgLocalFolders.addToCategory(account, e.id, dids);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, added > 0 ? "\"" + e.name + "\" toifasiga qo'shildi" : "Allaqachon shu toifada").show();
            }
        });
        fragment.showDialog(b.create());
    }

    public interface NameCallback {
        void run(String name);
    }

    public static void askName(BaseFragment fragment, String title, String initial, NameCallback cb) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(ctx);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint("Nomi");
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setSingleLine(true);
        editText.setText(initial);
        editText.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        FrameLayout frameLayout = new FrameLayout(ctx);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 6, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        builder.setTitle(title);
        builder.setView(frameLayout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String name = editText.getText() == null ? "" : editText.getText().toString().trim();
            if (!name.isEmpty()) {
                cb.run(name.length() > 12 ? name.substring(0, 12) : name);
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        fragment.showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 250);
    }

    // ---------- Guruh yoki kanalga qo'shish ----------

    public static void addUserToGroup(BaseFragment fragment, int account, long userId) {
        TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        if (user == null || userId <= 0) {
            BulletinFactory.of(fragment).createErrorBulletin("Faqat foydalanuvchini qo'shish mumkin").show();
            return;
        }
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_ADD_USERS_TO);
        args.putBoolean("resetDelegate", false);
        args.putBoolean("closeFragment", false);
        DialogsActivity picker = new DialogsActivity(args);
        picker.setDelegate((picker1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids == null || dids.isEmpty()) {
                return true;
            }
            long did = dids.get(0).dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-did);
            if (chat == null || picker1.getParentActivity() == null) {
                return true;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(picker1.getParentActivity(), picker1.getResourceProvider());
            builder.setTitle(ChatObject.isChannelAndNotMegaGroup(chat) ? "Kanalga qo'shish" : "Guruhga qo'shish");
            builder.setMessage(AndroidUtilities.replaceTags("**" + UserObject.getUserName(user) + "** foydalanuvchisini **" + chat.title + "** ga qo'shasizmi?"));
            builder.setPositiveButton("Qo'shish", (d, w) -> {
                MessagesController.getInstance(account).addUserToChat(chat.id, user, 0, null, fragment, () -> AndroidUtilities.runOnUIThread(() ->
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, UserObject.getFirstName(user) + " — " + chat.title + " ga qo'shildi").show()));
                picker1.finishFragment();
            });
            builder.setNegativeButton("Bekor qilish", null);
            picker1.showDialog(builder.create());
            return true;
        });
        fragment.presentFragment(picker);
    }

    // ---------- Keshni tozalash ----------

    public static void clearCache(BaseFragment fragment, int account, ArrayList<Long> dids) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null || dids.isEmpty()) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx, fragment.getResourceProvider());
        b.setTitle("Keshni tozalash");
        b.setMessage(dids.size() == 1
                ? "Bu chatdan yuklab olingan rasm, video, ovozli xabar va fayllar telefondan o'chiriladi. Xabarlarning o'zi o'chmaydi, kerak bo'lsa qayta yuklanadi."
                : dids.size() + " ta chatdan yuklab olingan media fayllar telefondan o'chiriladi. Xabarlarning o'zi o'chmaydi.");
        b.setPositiveButton("Tozalash", (d, w) -> doClearCache(fragment, account, new HashSet<>(dids)));
        b.setNegativeButton("Bekor qilish", null);
        AlertDialog dialog = b.create();
        fragment.showDialog(dialog);
        dialog.redPositive();
    }

    private static void doClearCache(BaseFragment fragment, int account, HashSet<Long> dids) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) {
            return;
        }
        final AlertDialog progress = new AlertDialog(ctx, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.showDelayed(300);
        FileLoader fileLoader = FileLoader.getInstance(account);
        FilePathDatabase db = fileLoader.getFileDatabase();
        db.getQueue().postRunnable(() -> {
            long[] result = {0, 0};
            try {
                db.ensureDatabaseCreated();
                int[] dirs = {FileLoader.MEDIA_DIR_CACHE, FileLoader.MEDIA_DIR_IMAGE, FileLoader.MEDIA_DIR_IMAGE_PUBLIC,
                        FileLoader.MEDIA_DIR_VIDEO, FileLoader.MEDIA_DIR_VIDEO_PUBLIC, FileLoader.MEDIA_DIR_AUDIO,
                        FileLoader.MEDIA_DIR_DOCUMENT, FileLoader.MEDIA_DIR_FILES};
                HashSet<String> seen = new HashSet<>();
                FilePathDatabase.FileMeta meta = new FilePathDatabase.FileMeta();
                for (int type : dirs) {
                    File dir = FileLoader.checkDirectory(type);
                    if (dir == null || !seen.add(dir.getAbsolutePath())) {
                        continue;
                    }
                    walk(db, dir, dids, meta, result, 0);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progress.dismiss();
                } catch (Throwable ignore) {
                }
                if (result[0] == 0) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Tozalanadigan kesh topilmadi").show();
                } else {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Tozalandi: " + result[0] + " ta fayl, " + formatSize(result[1])).show();
                }
            });
        });
    }

    private static void walk(FilePathDatabase db, File dir, HashSet<Long> dids, FilePathDatabase.FileMeta meta, long[] result, int depth) {
        File[] files = dir.listFiles();
        if (files == null || depth > 4) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                walk(db, f, dids, meta, result, depth + 1);
                continue;
            }
            FilePathDatabase.FileMeta m = db.getFileDialogId(f, meta);
            if (m != null && m.dialogId != 0 && dids.contains(m.dialogId)) {
                long size = f.length();
                if (f.delete()) {
                    result[0]++;
                    result[1] += size;
                }
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f);
        }
        return String.format(Locale.US, "%.2f GB", bytes / 1024f / 1024f / 1024f);
    }
}
