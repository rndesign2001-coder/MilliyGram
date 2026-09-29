/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Intent;
import android.os.Bundle;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.MgLocalFolders;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/**
 * Profil ⋮ → "Maxsus sozlamalar": foydalanuvchi, kanal yoki guruh uchun MilliyGram amallari.
 */
public class MgProfileTools {

    public static final int MENU_ID = 9601;

    /** Telegram ID (kanal/superguruh uchun -100… ko'rinishida) */
    public static long publicId(int account, long dialogId) {
        if (dialogId < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
            if (chat != null && ChatObject.isChannel(chat)) {
                return -1000000000000L - chat.id;
            }
        }
        return dialogId;
    }

    public static void show(BaseFragment f, int account, long dialogId) {
        if (f.getParentActivity() == null || dialogId == 0) {
            return;
        }
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.User user = dialogId > 0 ? mc.getUser(dialogId) : null;
        TLRPC.Chat chat = dialogId < 0 ? mc.getChat(-dialogId) : null;
        boolean self = user != null && user.id == UserConfig.getInstance(account).getClientUserId();
        ArrayList<Long> one = new ArrayList<>();
        one.add(dialogId);
        ArrayList<MgChatMenu.Section> sections = new ArrayList<>();

        // --- Kontakt va havola ---
        MgChatMenu.Section contact = new MgChatMenu.Section(user != null ? MgLang.t("Kontakt") : MgLang.t("Havola va ID"));
        if (user != null && !self && !user.bot) {
            boolean isContact = user.contact;
            contact.add(R.drawable.msg_addcontact, isContact ? MgLang.t("Kontakt nomini o'zgartirish") : MgLang.t("Maxsus kontakt qo'shish"), () -> {
                Bundle args = new Bundle();
                args.putLong("user_id", user.id);
                args.putBoolean("addContact", !isContact);
                f.presentFragment(new ContactAddActivity(args, f.getResourceProvider()));
            });
        }
        contact.add(R.drawable.msg_share, user != null ? MgLang.t("Foydalanuvchi eslatmasini ulashish") : MgLang.t("Havolani ulashish"), () -> shareMention(f, account, dialogId, user, chat));
        contact.add(R.drawable.msg_copy, MgLang.t("ID ni nusxalash"), () -> {
            long id = publicId(account, dialogId);
            if (AndroidUtilities.addToClipboard(String.valueOf(id))) {
                BulletinFactory.of(f).createCopyBulletin(MgLang.t("ID nusxalandi: ") + id).show();
            }
        });
        contact.add(R.drawable.msg_qrcode, MgLang.t("Profil kartasi (QR)"), () -> MgProfileCard.show(f, dialogId));
        sections.add(contact);

        // --- Qo'shish ---
        MgChatMenu.Section add = new MgChatMenu.Section(MgLang.t("Qo'shish"));
        if (user != null && !self) {
            add.add(R.drawable.msg_contact_add, MgLang.t("Guruh yoki kanalga qo'shish"), () -> MgDialogActions.addUserToGroup(f, account, user.id));
            add.add(R.drawable.msg_admins, MgLang.t("Kanal tahrirchilari safiga qo'shish"), () -> addAsChannelAdmin(f, account, user));
        }
        add.add(R.drawable.msg_addfolder, MgLang.t("Jildga qo'shish"), () -> addToCloudFolder(f, account, dialogId));
        add.add(R.drawable.msg_folders, MgLang.t("Toifaga qo'shish"), () -> MgDialogActions.addToCategory(f, account, one));
        boolean fav = MgLocalFolders.isFavorite(account, dialogId);
        add.add(R.drawable.msg_fave, fav ? MgLang.t("Tanlanganlardan olib tashlash") : MgLang.t("Tanlab olinganlarga qo'shish"), () -> {
            MgLocalFolders.setFavorite(account, one, !fav);
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, !fav ? MgLang.t("⭐ \"Tanlanganlar\" jildiga qo'shildi") : MgLang.t("\"Tanlanganlar\" jildidan olib tashlandi")).show();
        });
        sections.add(add);

        // --- Boshqa ---
        MgChatMenu.Section other = new MgChatMenu.Section(MgLang.t("Boshqa"));
        if (!self) {
            other.add(R.drawable.msg_secret, MgConfig.isDialogLocked(account, dialogId) ? MgLang.t("Qulfni olish") : MgLang.t("Chatni qulflash"), () -> MgChatLock.toggleLock(f, account, dialogId));
            other.add(R.drawable.msg_archive, MgConfig.isDialogHidden(account, dialogId) ? MgLang.t("Yashirishdan chiqarish") : MgLang.t("Chatni yashirish"), () -> MgHiddenActivity.toggleHidden(f, account, dialogId));
        }
        if (user != null && MgChatFeatures.canToggleStranger(account, user)) {
            other.add(R.drawable.msg_usersearch, MgChatFeatures.strangerMenuTitle(account, dialogId), () -> MgChatFeatures.toggleStranger(f, account, dialogId));
        }
        if (chat != null) {
            if (ChatObject.isChannel(chat)) {
                other.add(R.drawable.msg_stats, MgLang.t("Obunachilar kundaligi"), () -> f.presentFragment(new MgGrowthActivity(chat.id)));
            }
            if (ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_INVITE)) {
                other.add(R.drawable.msg_requests, MgLang.t("Qo'shilish so'rovlari"), () -> MgChatFeatures.showJoinRequests(f, account, chat));
            }
            if (MgMembersActivity.canUse(chat)) {
                other.add(R.drawable.msg_leave, MgLang.t("A'zolarni tozalash"), () -> f.presentFragment(new MgMembersActivity(chat.id)));
            }
            if (MgChatFeatures.canJoinAll(chat)) {
                other.add(R.drawable.msg_contact_add, MgLang.t("Barcha akkauntlardan qo'shilish"), () -> MgChatFeatures.joinAllAccounts(f, account, chat));
            }
        }
        sections.add(other);
        MgChatMenu.show(f, MgLang.t("Maxsus sozlamalar"), sections);
    }

    private static void shareMention(BaseFragment f, int account, long dialogId, TLRPC.User user, TLRPC.Chat chat) {
        String text;
        if (user != null) {
            String un = UserObject.getPublicUsername(user);
            text = UserObject.getUserName(user) + "\n" + (un != null ? "@" + un + "\nhttps://t.me/" + un : "tg://user?id=" + user.id);
        } else if (chat != null) {
            String un = ChatObject.getPublicUsername(chat);
            text = chat.title + (un != null ? "\n@" + un + "\nhttps://t.me/" + un : "\nID: " + publicId(account, dialogId));
        } else {
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            f.getParentActivity().startActivity(Intent.createChooser(i, MgLang.t("Ulashish")));
        } catch (Throwable e) {
            if (AndroidUtilities.addToClipboard(text)) {
                BulletinFactory.of(f).createCopyBulletin(MgLang.t("Nusxalandi")).show();
            }
        }
    }

    /** Siz egasi yoki admin-tayinlash huquqiga ega bo'lgan kanalni tanlab, foydalanuvchini admin qilish */
    private static void addAsChannelAdmin(BaseFragment f, int account, TLRPC.User user) {
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<TLRPC.Chat> channels = new ArrayList<>();
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        for (int i = 0; i < all.size(); i++) {
            TLRPC.Dialog d = all.get(i);
            if (d == null || d.id >= 0) {
                continue;
            }
            TLRPC.Chat c = mc.getChat(-d.id);
            if (c != null && ChatObject.isChannel(c) && !ChatObject.isNotInChat(c) && ChatObject.canAddAdmins(c)) {
                channels.add(c);
            }
        }
        if (channels.isEmpty()) {
            BulletinFactory.of(f).createErrorBulletin(MgLang.t("Siz admin tayinlay oladigan kanal yoki guruh yo'q")).show();
            return;
        }
        CharSequence[] names = new CharSequence[channels.size()];
        int[] icons = new int[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            TLRPC.Chat c = channels.get(i);
            names[i] = c.title;
            icons[i] = ChatObject.isChannelAndNotMegaGroup(c) ? R.drawable.msg_channel : R.drawable.msg_groups;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(f.getParentActivity(), f.getResourceProvider());
        b.setTitle(MgLang.t("Qaysi kanalga tahrirchi qilinsin?"));
        b.setItems(names, icons, (d, w) -> {
            TLRPC.Chat c = channels.get(w);
            ChatRightsEditActivity edit = new ChatRightsEditActivity(user.id, c.id, null, null, null, "", ChatRightsEditActivity.TYPE_ADMIN, true, true, null);
            f.presentFragment(edit);
        });
        f.showDialog(b.create());
    }

    /** Telegram (bulut) jildiga qo'shish */
    private static void addToCloudFolder(BaseFragment f, int account, long dialogId) {
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<MessagesController.DialogFilter> filters = new ArrayList<>();
        for (MessagesController.DialogFilter fl : mc.getDialogFilters()) {
            if (fl != null && !fl.isDefault() && !MgLocalFolders.isLocal(fl)) {
                filters.add(fl);
            }
        }
        if (filters.isEmpty()) {
            BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip, MgLang.t("Jildlar yo'q. Sozlamalar → Jildlar orqali yarating yoki \"Toifaga qo'shish\"dan foydalaning.")).show();
            f.presentFragment(new FiltersSetupActivity());
            return;
        }
        CharSequence[] names = new CharSequence[filters.size()];
        int[] icons = new int[filters.size()];
        for (int i = 0; i < filters.size(); i++) {
            MessagesController.DialogFilter fl = filters.get(i);
            names[i] = fl.name + (fl.alwaysShow.contains(dialogId) ? "  ✓" : "");
            icons[i] = R.drawable.msg_folders;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(f.getParentActivity(), f.getResourceProvider());
        b.setTitle(MgLang.t("Jildga qo'shish"));
        b.setItems(names, icons, (d, w) -> {
            MessagesController.DialogFilter fl = filters.get(w);
            if (fl.alwaysShow.contains(dialogId)) {
                BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, MgLang.t("Allaqachon shu jildda")).show();
                return;
            }
            ArrayList<Long> always = new ArrayList<>(fl.alwaysShow);
            ArrayList<Long> never = new ArrayList<>(fl.neverShow);
            always.add(dialogId);
            never.remove(dialogId);
            FilterCreateActivity.saveFilterToServer(fl, fl.flags, fl.name, fl.entities, fl.title_noanimate, fl.color, always, never, fl.pinnedDialogs, false, false, true, true, true, f,
                    () -> BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "\"" + fl.name + MgLang.t("\" jildiga qo'shildi")).show());
        });
        f.showDialog(b.create());
    }
}
