/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgLang;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * "Xabar tafsilotlari": kartochkali sahifa (har bir qator bosilsa nusxalanadi),
 * xabarda ishlatilgan custom emojilar IDlari va xabarning HTML kodi.
 */
public class MgMessageDetailsActivity extends BaseFragment {

    private static final int MENU_COPY_ALL = 1;

    private final MessageObject message;
    private final MessageObject.GroupedMessages group;
    private final StringBuilder allText = new StringBuilder();

    public MgMessageDetailsActivity(int account, MessageObject message, MessageObject.GroupedMessages group) {
        this.message = message;
        this.group = group;
        this.currentAccount = account;
    }

    private void copy(CharSequence text, String toast) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        AndroidUtilities.addToClipboard(text);
        BulletinFactory.of(this).createCopyBulletin(toast != null ? toast : MgLang.t("Nusxalandi")).show();
    }

    private static String dcName(int dc) {
        switch (dc) {
            case 1: return "DC1 - MIA, Miami, US";
            case 2: return "DC2 - AMS, Amsterdam, NL";
            case 3: return "DC3 - MIA, Miami, US";
            case 4: return "DC4 - AMS, Amsterdam, NL";
            case 5: return "DC5 - SIN, Singapore, SG";
            default: return dc > 0 ? "DC" + dc : null;
        }
    }

    /** Ism, @username va ID — alohida qatorlarda */
    private String peerFull(TLRPC.Peer peer) {
        if (peer == null) {
            return null;
        }
        MessagesController mc = getMessagesController();
        long id = MessageObject.getPeerId(peer);
        StringBuilder sb = new StringBuilder();
        if (id > 0) {
            TLRPC.User u = mc.getUser(id);
            sb.append(u == null ? "?" : UserObject.getUserName(u));
            String un = u == null ? null : UserObject.getPublicUsername(u);
            if (!TextUtils.isEmpty(un)) {
                sb.append("\n@").append(un);
            }
            sb.append('\n').append(id);
        } else {
            TLRPC.Chat c = mc.getChat(-id);
            sb.append(c == null ? "?" : c.title);
            String un = c == null ? null : ChatObject.getPublicUsername(c);
            if (!TextUtils.isEmpty(un)) {
                sb.append("\n@").append(un);
            }
            sb.append('\n').append(id);
        }
        return sb.toString();
    }

    private String typeName(MessageObject m) {
        TLRPC.Message msg = m.messageOwner;
        if (m.isRoundVideo()) return MgLang.t("Dumaloq video");
        if (m.isVoice()) return MgLang.t("Ovozli xabar");
        if (m.isGif()) return "GIF";
        if (m.isSticker() || m.isAnimatedSticker()) return MgLang.t("Stiker");
        if (m.isVideo()) return MgLang.t("Video");
        if (m.isMusic()) return MgLang.t("Musiqa");
        if (m.isPhoto()) return MgLang.t("Rasm");
        if (msg.media instanceof TLRPC.TL_messageMediaPoll) return MgLang.t("So'rovnoma");
        if (msg.media instanceof TLRPC.TL_messageMediaGeo || msg.media instanceof TLRPC.TL_messageMediaVenue || msg.media instanceof TLRPC.TL_messageMediaGeoLive) return MgLang.t("Joylashuv");
        if (msg.media instanceof TLRPC.TL_messageMediaContact) return MgLang.t("Kontakt");
        if (m.getDocument() != null) return MgLang.t("Fayl");
        if (m.messageOwner instanceof TLRPC.TL_messageService) return MgLang.t("Xizmat xabari");
        return MgLang.t("Matn");
    }

    /** Matn va entity'lari bor xabar (albomda — izohli element) */
    private TLRPC.Message textOwner() {
        if (group != null) {
            MessageObject primary = group.findCaptionMessageObject();
            if (primary != null && primary.messageOwner != null && !TextUtils.isEmpty(primary.messageOwner.message)) {
                return primary.messageOwner;
            }
        }
        return message.messageOwner;
    }

    private void addRow(LinearLayout card, Context ctx, String title, CharSequence value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        final CharSequence v = value;
        card.addView(MgUi.infoRow(ctx, getResourceProvider(), title, v, () -> copy(v, title + ": " + MgLang.t("nusxalandi"))));
        allText.append(title).append(": ").append(v).append('\n');
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MgLang.t("Xabar tafsilotlari"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_COPY_ALL) {
                    copy(allText.toString().trim(), MgLang.t("Tafsilotlar nusxalandi"));
                }
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_COPY_ALL, R.drawable.msg_copy);

        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider()));
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        scroll.addView(list, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0));

        if (message == null || message.messageOwner == null) {
            fragmentView = scroll;
            return fragmentView;
        }
        final TLRPC.Message msg = message.messageOwner;
        final TLRPC.Message textMsg = textOwner();
        final String text = textMsg != null ? textMsg.message : null;
        final ArrayList<TLRPC.MessageEntity> entities = textMsg != null ? textMsg.entities : null;

        // ---------- Asosiy ma'lumotlar ----------
        LinearLayout main = MgUi.card(context, getResourceProvider());
        boolean hasMedia = msg.media != null && !(msg.media instanceof TLRPC.TL_messageMediaEmpty) && !(msg.media instanceof TLRPC.TL_messageMediaWebPage);
        addRow(main, context, MgLang.t("Xabar"), typeName(message));
        if (!TextUtils.isEmpty(text)) {
            String shown = text.length() > 1500 ? text.substring(0, 1500) + "…" : text;
            addRow(main, context, hasMedia ? MgLang.t("Izoh") : MgLang.t("Matn"), shown);
        }
        addRow(main, context, "Id", String.valueOf(message.getId()));
        addRow(main, context, MgLang.t("Chat"), peerFull(msg.peer_id));
        if (msg.from_id != null && MessageObject.getPeerId(msg.from_id) != MessageObject.getPeerId(msg.peer_id)) {
            addRow(main, context, MgLang.t("Kimdan"), peerFull(msg.from_id));
        }
        addRow(main, context, MgLang.t("Imzo"), msg.post_author);
        addRow(main, context, MgLang.t("Sana"), MgMessageTools.fullDate(msg.date));
        if (msg.edit_date > 0 && !msg.edit_hide) {
            addRow(main, context, MgLang.t("Tahrirlangan"), MgMessageTools.fullDate(msg.edit_date));
        }
        if (msg.fwd_from != null) {
            String ff = MgMessageTools.peerName(currentAccount, msg.fwd_from.from_id);
            if (ff == null && !TextUtils.isEmpty(msg.fwd_from.from_name)) {
                ff = msg.fwd_from.from_name + MgLang.t(" (yashirin profil)");
            }
            StringBuilder fwd = new StringBuilder(ff == null ? "—" : ff);
            fwd.append('\n').append(MgMessageTools.fullDate(msg.fwd_from.date));
            if (msg.fwd_from.channel_post != 0) {
                fwd.append('\n').append(MgLang.t("Asl post ID: ")).append(msg.fwd_from.channel_post);
            }
            addRow(main, context, MgLang.t("Uzatilgan"), fwd);
        }
        if (msg.reply_to != null && msg.reply_to.reply_to_msg_id != 0) {
            addRow(main, context, MgLang.t("Javob"), "#" + msg.reply_to.reply_to_msg_id);
        }
        if (msg.via_bot_id != 0) {
            TLRPC.User bot = getMessagesController().getUser(msg.via_bot_id);
            addRow(main, context, MgLang.t("Bot orqali"), bot == null ? String.valueOf(msg.via_bot_id) : "@" + UserObject.getPublicUsername(bot) + "\n" + msg.via_bot_id);
        }
        TLRPC.Chat chat = message.getDialogId() < 0 ? getMessagesController().getChat(-message.getDialogId()) : null;
        if (chat != null && ChatObject.isForum(chat)) {
            try {
                long topic = MessageObject.getTopicId(currentAccount, msg, true);
                addRow(main, context, MgLang.t("Mavzu IDsi"), String.valueOf(topic));
            } catch (Throwable ignore) {
            }
        }
        if (msg.views > 0) {
            addRow(main, context, MgLang.t("Ko'rishlar"), String.valueOf(msg.views));
        }
        if (msg.forwards > 0) {
            addRow(main, context, MgLang.t("Ulashishlar"), String.valueOf(msg.forwards));
        }
        if (message.getRepliesCount() > 0) {
            addRow(main, context, MgLang.t("Izohlar"), String.valueOf(message.getRepliesCount()));
        }
        if (msg.grouped_id != 0) {
            addRow(main, context, MgLang.t("Albom"), group != null && group.messages != null
                    ? group.messages.size() + MgLang.t(" ta element") + "\n" + msg.grouped_id
                    : String.valueOf(msg.grouped_id));
        }
        TLRPC.Document doc = message.getDocument();
        int dc = 0;
        if (doc != null) {
            addRow(main, context, MgLang.t("Fayl nomi"), message.getFileName());
            addRow(main, context, MgLang.t("Fayl turi"), doc.mime_type);
            addRow(main, context, MgLang.t("Hajmi"), AndroidUtilities.formatFileSize(doc.size));
            for (TLRPC.DocumentAttribute a : doc.attributes) {
                if ((a instanceof TLRPC.TL_documentAttributeVideo || a instanceof TLRPC.TL_documentAttributeImageSize) && a.w > 0 && a.h > 0) {
                    addRow(main, context, MgLang.t("O'lcham"), a.w + "×" + a.h);
                    break;
                }
            }
            double dur = message.getDuration();
            if (dur > 0) {
                int d = (int) Math.round(dur);
                addRow(main, context, MgLang.t("Davomiyligi"), String.format(Locale.US, "%d:%02d", d / 60, d % 60));
            }
            dc = doc.dc_id;
        } else if (msg.media instanceof TLRPC.TL_messageMediaPhoto && msg.media.photo != null) {
            TLRPC.PhotoSize big = FileLoader.getClosestPhotoSizeWithSize(msg.media.photo.sizes, AndroidUtilities.getPhotoSize());
            if (big != null) {
                addRow(main, context, MgLang.t("O'lcham"), big.w + "×" + big.h);
                if (big.size > 0) {
                    addRow(main, context, MgLang.t("Hajmi"), AndroidUtilities.formatFileSize(big.size));
                }
            }
            dc = msg.media.photo.dc_id;
        }
        addRow(main, context, MgLang.t("Ma'lumotlar markazi"), dcName(dc));
        if (hasMedia) {
            try {
                File f = getFileLoader().getPathToMessage(msg);
                if (f != null && f.exists()) {
                    addRow(main, context, MgLang.t("Fayl"), f.getAbsolutePath());
                }
            } catch (Throwable ignore) {
            }
        }
        if (msg.ttl_period > 0) {
            addRow(main, context, MgLang.t("Avto-o'chish"), msg.ttl_period / 3600 + MgLang.t(" soatdan keyin"));
        }
        ArrayList<String> flags = new ArrayList<>();
        if (msg.silent) flags.add(MgLang.t("Ovozsiz yuborilgan"));
        if (msg.noforwards) flags.add(MgLang.t("Uzatish taqiqlangan"));
        if (msg.pinned) flags.add(MgLang.t("Qadalgan"));
        if (!flags.isEmpty()) {
            addRow(main, context, MgLang.t("Belgilar"), TextUtils.join("\n", flags));
        }
        if (chat != null && ChatObject.isChannel(chat) && message.getId() > 0) {
            String username = ChatObject.getPublicUsername(chat);
            addRow(main, context, MgLang.t("Havola"), !TextUtils.isEmpty(username)
                    ? "https://t.me/" + username + "/" + message.getId()
                    : "https://t.me/c/" + chat.id + "/" + message.getId());
        }
        list.addView(main, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 4, 12, 6));

        // ---------- Custom emojilar ----------
        LinkedHashMap<Long, String> emojis = new LinkedHashMap<>();
        if (entities != null && text != null) {
            for (TLRPC.MessageEntity e : entities) {
                if (e instanceof TLRPC.TL_messageEntityCustomEmoji) {
                    long id = ((TLRPC.TL_messageEntityCustomEmoji) e).document_id;
                    if (!emojis.containsKey(id)) {
                        int s = Math.max(0, Math.min(text.length(), e.offset));
                        int en = Math.max(s, Math.min(text.length(), e.offset + e.length));
                        emojis.put(id, text.substring(s, en));
                    }
                }
            }
        }
        if (!emojis.isEmpty()) {
            LinearLayout ec = MgUi.card(context, getResourceProvider());
            ec.addView(MgUi.header(context, getResourceProvider(), MgLang.t("Custom emojilar") + " (" + emojis.size() + ")"));
            StringBuilder allIds = new StringBuilder();
            for (java.util.Map.Entry<Long, String> en : emojis.entrySet()) {
                final String id = String.valueOf(en.getKey());
                allIds.append(id).append('\n');
                ec.addView(MgUi.infoRow(context, getResourceProvider(), en.getValue() + "   " + id,
                        MgLang.t("Bosing — ID nusxalanadi"), () -> copy(id, MgLang.t("Emoji ID nusxalandi"))));
            }
            final String ids = allIds.toString().trim();
            View all = MgUi.tile(context, getResourceProvider(), MgLang.t("Barcha IDlarni nusxalash"),
                    () -> copy(ids, MgLang.t("Barcha emoji IDlari nusxalandi")));
            ec.addView(all, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 14, 6, 14, 8));
            list.addView(ec, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 6, 12, 6));
        }

        // ---------- HTML ----------
        if (!TextUtils.isEmpty(text)) {
            final String html = MgHtml.toHtml(text, entities);
            LinearLayout hc = MgUi.card(context, getResourceProvider());
            hc.addView(MgUi.header(context, getResourceProvider(), "HTML"));
            hc.addView(MgUi.codeBlock(context, "html", html, () -> copy(html, MgLang.t("HTML kod nusxalandi"))),
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 4, 12, 6));
            LinearLayout buttons = new LinearLayout(context);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.addView(MgUi.accentTile(context, getResourceProvider(), MgLang.t("HTMLni nusxalash"),
                    () -> copy(html, MgLang.t("HTML kod nusxalandi"))), LayoutHelper.createLinear(0, 46, 1f, 0, 0, 4, 0));
            buttons.addView(MgUi.tile(context, getResourceProvider(), MgLang.t("Matnni nusxalash"),
                    () -> copy(text, MgLang.t("Matn nusxalandi"))), LayoutHelper.createLinear(0, 46, 1f, 4, 0, 0, 0));
            hc.addView(buttons, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 4, 12, 10));
            list.addView(hc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 6, 12, 6));
        }

        fragmentView = scroll;
        return fragmentView;
    }
}
