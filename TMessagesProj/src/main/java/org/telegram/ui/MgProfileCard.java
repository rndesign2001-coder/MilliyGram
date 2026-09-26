/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.TelegramQRCodeWriter;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

/**
 * Profil / kanal "vizitka" kartasi: avatar, ism, @username, tavsif, obunachilar soni va QR kod.
 * Rasm sifatida ulashish yoki galereyaga saqlash mumkin.
 */
public class MgProfileCard {

    private static final int W = 1080, H = 1440;
    public static final String[] STYLES = {"Moviy", "Tungi", "Zumrad", "Oltin"};
    private static final int[][] GRAD = {
            {0xFF1E3C8C, 0xFF1AA3D9},
            {0xFF0F1026, 0xFF3A2C6E},
            {0xFF0E5E4E, 0xFF36B37E},
            {0xFF8A4B08, 0xFFE5A83B},
    };

    public static void show(BaseFragment f, long dialogId) {
        Activity act = f.getParentActivity();
        if (act == null) {
            return;
        }
        final int[] style = {org.telegram.messenger.MgConfig.getInt("card_style", 0)};
        final Bitmap[] current = {render(f.getCurrentAccount(), dialogId, style[0])};
        if (current[0] == null) {
            BulletinFactory.of(f).createErrorBulletin("Kartani yaratib bo'lmadi").show();
            return;
        }
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        ImageView iv = new ImageView(act);
        iv.setAdjustViewBounds(true);
        iv.setImageBitmap(current[0]);
        box.addView(iv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 4, 20, 4));
        AlertDialog.Builder b = new AlertDialog.Builder(act, f.getResourceProvider());
        b.setTitle("Profil kartasi");
        b.setView(box);
        b.setPositiveButton("Ulashish", (d, w) -> share(act, current[0]));
        b.setNegativeButton("Saqlash", (d, w) -> save(f, current[0]));
        b.setNeutralButton("Rang", null);
        AlertDialog dialog = b.create();
        f.showDialog(dialog);
        android.view.View neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutral != null) {
            neutral.setOnClickListener(v -> {
                style[0] = (style[0] + 1) % STYLES.length;
                org.telegram.messenger.MgConfig.setInt("card_style", style[0]);
                Bitmap nb = render(f.getCurrentAccount(), dialogId, style[0]);
                if (nb != null) {
                    current[0] = nb;
                    iv.setImageBitmap(nb);
                }
            });
        }
    }

    private static Bitmap loadAvatar(int account, TLRPC.FileLocation loc) {
        if (loc == null) {
            return null;
        }
        try {
            File file = FileLoader.getInstance(account).getPathToAttach(loc, true);
            if (file != null && file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static Bitmap render(int account, long dialogId, int style) {
        try {
            MessagesController mc = MessagesController.getInstance(account);
            String name, username = null, about = null, sub = null, link;
            Bitmap avatar = null;
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            if (dialogId > 0) {
                TLRPC.User u = mc.getUser(dialogId);
                if (u == null) {
                    return null;
                }
                name = UserObject.getUserName(u);
                username = UserObject.getPublicUsername(u);
                TLRPC.UserFull full = mc.getUserFull(dialogId);
                about = full != null ? full.about : null;
                if (u.photo != null) {
                    avatar = loadAvatar(account, u.photo.photo_big);
                    if (avatar == null) {
                        avatar = loadAvatar(account, u.photo.photo_small);
                    }
                }
                avatarDrawable.setInfo(account, u);
                sub = u.bot ? "Bot" : null;
                link = username != null ? "https://t.me/" + username : "tg://user?id=" + u.id;
            } else {
                TLRPC.Chat c = mc.getChat(-dialogId);
                if (c == null) {
                    return null;
                }
                name = c.title;
                username = ChatObject.getPublicUsername(c);
                TLRPC.ChatFull full = mc.getChatFull(c.id);
                about = full != null ? full.about : null;
                int count = full != null && full.participants_count > 0 ? full.participants_count : c.participants_count;
                if (count > 0) {
                    sub = String.format(java.util.Locale.US, "%,d", count).replace(',', ' ') + (ChatObject.isChannelAndNotMegaGroup(c) ? " obunachi" : " a'zo");
                }
                if (c.photo != null) {
                    avatar = loadAvatar(account, c.photo.photo_big);
                    if (avatar == null) {
                        avatar = loadAvatar(account, c.photo.photo_small);
                    }
                }
                avatarDrawable.setInfo(account, c);
                link = username != null ? "https://t.me/" + username : null;
            }

            Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bmp);
            int[] g = GRAD[Math.max(0, Math.min(GRAD.length - 1, style))];
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setShader(new LinearGradient(0, 0, W, H, g[0], g[1], Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, W, H, bg);
            // milliy naqsh
            try {
                Bitmap pattern = SvgHelper.getBitmap(R.raw.mg_pattern, W, H, Color.WHITE, 1f, SvgHelper.ScaleMode.ByWidth);
                if (pattern != null) {
                    Paint pp = new Paint(Paint.FILTER_BITMAP_FLAG);
                    pp.setAlpha(28);
                    cv.drawBitmap(pattern, 0, 0, pp);
                }
            } catch (Throwable ignore) {
            }
            // karta
            Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
            card.setColor(0x26FFFFFF);
            RectF cardRect = new RectF(60, 170, W - 60, H - 110);
            cv.drawRoundRect(cardRect, 56, 56, card);

            // avatar
            int ar = 150;
            float acx = W / 2f, acy = 190 + ar;
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setColor(Color.WHITE);
            cv.drawCircle(acx, acy - 20, ar + 10, ring);
            if (avatar != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(avatar, ar * 2, ar * 2, true);
                Paint ap = new Paint(Paint.ANTI_ALIAS_FLAG);
                BitmapShader sh = new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                android.graphics.Matrix m = new android.graphics.Matrix();
                m.setTranslate(acx - ar, acy - 20 - ar);
                sh.setLocalMatrix(m);
                ap.setShader(sh);
                cv.drawCircle(acx, acy - 20, ar, ap);
            } else {
                avatarDrawable.setTextSize(110);
                avatarDrawable.setBounds((int) (acx - ar), (int) (acy - 20 - ar), (int) (acx + ar), (int) (acy - 20 + ar));
                avatarDrawable.draw(cv);
            }

            float y = acy + ar + 40;
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(Color.WHITE);
            tp.setTypeface(AndroidUtilities.bold());
            tp.setTextSize(68);
            y = drawCentered(cv, TextUtils.ellipsize(name, tp, W - 200, TextUtils.TruncateAt.END), tp, y, 1);
            if (username != null) {
                TextPaint up = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                up.setColor(0xDDFFFFFF);
                up.setTextSize(46);
                y = drawCentered(cv, "@" + username, up, y + 8, 1);
            }
            if (sub != null) {
                TextPaint sp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                sp.setColor(0xCCFFFFFF);
                sp.setTextSize(40);
                y = drawCentered(cv, sub, sp, y + 6, 1);
            }
            if (!TextUtils.isEmpty(about)) {
                TextPaint bp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                bp.setColor(0xE6FFFFFF);
                bp.setTextSize(36);
                y = drawCentered(cv, about.replace('\n', ' '), bp, y + 18, 3);
            }

            // QR
            if (link != null) {
                int qs = 400;
                float qx = (W - qs) / 2f, qy = Math.max(y + 40, H - 110 - qs - 120);
                Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
                white.setColor(Color.WHITE);
                cv.drawRoundRect(new RectF(qx - 30, qy - 30, qx + qs + 30, qy + qs + 30), 40, 40, white);
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                TelegramQRCodeWriter writer = new TelegramQRCodeWriter();
                Bitmap qr = writer.encode(link, qs, qs, hints, null, 0.75f, Color.WHITE, g[0]);
                if (qr != null) {
                    cv.drawBitmap(qr, qx, qy, null);
                    int hole = writer.getImageSize();
                    Drawable logo = ApplicationLoader.applicationContext.getResources().getDrawable(R.mipmap.ic_launcher_round);
                    int ls = (int) (Math.max(hole, 60) * 0.9f);
                    logo.setBounds((int) (qx + qs / 2f - ls / 2f), (int) (qy + qs / 2f - ls / 2f), (int) (qx + qs / 2f + ls / 2f), (int) (qy + qs / 2f + ls / 2f));
                    logo.draw(cv);
                }
                TextPaint lp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                lp.setColor(0xDDFFFFFF);
                lp.setTextSize(34);
                drawCentered(cv, link.replace("https://", ""), lp, qy + qs + 50, 1);
            }
            TextPaint foot = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            foot.setColor(0x99FFFFFF);
            foot.setTextSize(32);
            foot.setTypeface(AndroidUtilities.bold());
            drawCentered(cv, "MilliyGram", foot, H - 80, 1);
            return bmp;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    private static float drawCentered(Canvas cv, CharSequence text, TextPaint p, float y, int maxLines) {
        StaticLayout l = new StaticLayout(text, p, W - 200, Layout.Alignment.ALIGN_CENTER, 1.1f, 0, false);
        int lines = Math.min(maxLines, l.getLineCount());
        if (l.getLineCount() > maxLines) {
            CharSequence cut = text.subSequence(0, l.getLineEnd(maxLines - 1));
            String s = cut.toString().trim();
            if (s.length() > 1) {
                s = s.substring(0, s.length() - 1) + "…";
            }
            l = new StaticLayout(s, p, W - 200, Layout.Alignment.ALIGN_CENTER, 1.1f, 0, false);
            lines = l.getLineCount();
        }
        cv.save();
        cv.translate(100, y);
        l.draw(cv);
        cv.restore();
        return y + l.getLineBottom(lines - 1);
    }

    private static File writeTemp(Bitmap b) throws Exception {
        File dir = AndroidUtilities.getSharingDirectory();
        dir.mkdirs();
        File file = new File(dir, "MilliyGram_card_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream os = new FileOutputStream(file)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
        return file;
    }

    private static void share(Activity act, Bitmap b) {
        try {
            File file = writeTemp(b);
            Uri uri = FileProvider.getUriForFile(act, ApplicationLoader.getApplicationId() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(Intent.createChooser(intent, "Profil kartasini ulashish"));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void save(BaseFragment f, Bitmap b) {
        try {
            File file = writeTemp(b);
            MediaController.saveFile(file.getAbsolutePath(), f.getParentActivity(), 0, null, "image/png");
            BulletinFactory.of(f).createSimpleBulletin(R.raw.contact_check, "Karta galereyaga saqlandi").show();
        } catch (Throwable e) {
            FileLog.e(e);
            BulletinFactory.of(f).createErrorBulletin("Saqlab bo'lmadi").show();
        }
    }
}
