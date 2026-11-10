/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Xabar matni + entity'larini Telegram Bot API "HTML" formatiga o'giradi
 * (b, i, u, s, tg-spoiler, code, pre, a, tg-emoji, blockquote).
 */
public final class MgHtml {

    private MgHtml() {
    }

    private static final class Tag {
        final int start;
        final int end;
        final String open;
        final String close;
        final int order;

        Tag(int start, int end, String open, String close, int order) {
            this.start = start;
            this.end = end;
            this.open = open;
            this.close = close;
            this.order = order;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Tag tagFor(TLRPC.MessageEntity e, int start, int end, int order) {
        if (e instanceof TLRPC.TL_messageEntityBold) {
            return new Tag(start, end, "<b>", "</b>", order);
        } else if (e instanceof TLRPC.TL_messageEntityItalic) {
            return new Tag(start, end, "<i>", "</i>", order);
        } else if (e instanceof TLRPC.TL_messageEntityUnderline) {
            return new Tag(start, end, "<u>", "</u>", order);
        } else if (e instanceof TLRPC.TL_messageEntityStrike) {
            return new Tag(start, end, "<s>", "</s>", order);
        } else if (e instanceof TLRPC.TL_messageEntitySpoiler) {
            return new Tag(start, end, "<tg-spoiler>", "</tg-spoiler>", order);
        } else if (e instanceof TLRPC.TL_messageEntityCode) {
            return new Tag(start, end, "<code>", "</code>", order);
        } else if (e instanceof TLRPC.TL_messageEntityPre) {
            if (!TextUtils.isEmpty(e.language)) {
                return new Tag(start, end, "<pre><code class=\"language-" + escape(e.language) + "\">", "</code></pre>", order);
            }
            return new Tag(start, end, "<pre>", "</pre>", order);
        } else if (e instanceof TLRPC.TL_messageEntityTextUrl) {
            return new Tag(start, end, "<a href=\"" + escape(e.url) + "\">", "</a>", order);
        } else if (e instanceof TLRPC.TL_messageEntityMentionName) {
            return new Tag(start, end, "<a href=\"tg://user?id=" + ((TLRPC.TL_messageEntityMentionName) e).user_id + "\">", "</a>", order);
        } else if (e instanceof TLRPC.TL_inputMessageEntityMentionName) {
            TLRPC.InputUser u = ((TLRPC.TL_inputMessageEntityMentionName) e).user_id;
            long id = u != null ? u.user_id : 0;
            return new Tag(start, end, "<a href=\"tg://user?id=" + id + "\">", "</a>", order);
        } else if (e instanceof TLRPC.TL_messageEntityCustomEmoji) {
            return new Tag(start, end, "<tg-emoji emoji-id=\"" + ((TLRPC.TL_messageEntityCustomEmoji) e).document_id + "\">", "</tg-emoji>", order);
        } else if (e instanceof TLRPC.TL_messageEntityBlockquote) {
            return new Tag(start, end, e.collapsed ? "<blockquote expandable>" : "<blockquote>", "</blockquote>", order);
        }
        return null;
    }

    public static String toHtml(String text, ArrayList<TLRPC.MessageEntity> entities) {
        if (text == null) {
            text = "";
        }
        final int len = text.length();
        ArrayList<Tag> tags = new ArrayList<>();
        if (entities != null) {
            for (int i = 0; i < entities.size(); i++) {
                TLRPC.MessageEntity e = entities.get(i);
                if (e == null) {
                    continue;
                }
                int start = Math.max(0, Math.min(len, e.offset));
                int end = Math.max(start, Math.min(len, e.offset + e.length));
                if (end <= start) {
                    continue;
                }
                Tag t = tagFor(e, start, end, i);
                if (t != null) {
                    tags.add(t);
                }
            }
        }
        // boshlanishi bo'yicha, bir joydan boshlansa — uzunrog'i tashqarida
        Collections.sort(tags, (a, b) -> {
            if (a.start != b.start) {
                return Integer.compare(a.start, b.start);
            }
            if (a.end != b.end) {
                return Integer.compare(b.end, a.end);
            }
            return Integer.compare(a.order, b.order);
        });
        StringBuilder sb = new StringBuilder(len + tags.size() * 16);
        ArrayList<Tag> stack = new ArrayList<>();
        int next = 0;
        for (int i = 0; i <= len; i++) {
            // shu yerda tugaydiganlarni yopamiz (ichkisidan boshlab)
            for (int k = stack.size() - 1; k >= 0; k--) {
                if (stack.get(k).end <= i) {
                    // ichkaridagi hali tugamaganlarni vaqtincha yopib, qayta ochamiz
                    ArrayList<Tag> reopen = new ArrayList<>();
                    for (int j = stack.size() - 1; j > k; j--) {
                        Tag inner = stack.remove(j);
                        sb.append(inner.close);
                        if (inner.end > i) {
                            reopen.add(0, inner);
                        }
                    }
                    sb.append(stack.remove(k).close);
                    for (Tag t : reopen) {
                        sb.append(t.open);
                        stack.add(t);
                    }
                }
            }
            while (next < tags.size() && tags.get(next).start == i) {
                Tag t = tags.get(next++);
                sb.append(t.open);
                stack.add(t);
            }
            if (i < len) {
                char c = text.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    default: sb.append(c);
                }
            }
        }
        for (int k = stack.size() - 1; k >= 0; k--) {
            sb.append(stack.get(k).close);
        }
        return sb.toString();
    }
}
