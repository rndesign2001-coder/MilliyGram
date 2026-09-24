/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Chat qulfi sozlamalari (yashirin bo'limdan alohida kod).
 */
public class MgChatLockSettingsActivity extends UniversalFragment {

    private static final int ID_LOCK_TYPE = 1;
    private static final int ID_CHANGE_CODE = 2;
    private static final int ID_FINGERPRINT = 3;
    private static final int ID_INVISIBLE = 4;
    private static final int ID_VIBRATE = 5;
    private static final int ID_REMOVE = 6;

    private static final String S = MgConfig.SCOPE_CHAT;

    public static void open(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (!MgConfig.hasLock(S)) {
            fragment.presentFragment(new MgChatLockSettingsActivity());
            return;
        }
        MgChatLock.askLock(fragment, S, "Chat qulfi sozlamalari", ok -> {
            if (ok) {
                fragment.presentFragment(new MgChatLockSettingsActivity());
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return "Chat qulfi";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        boolean has = MgConfig.hasLock(S);
        boolean pattern = MgConfig.LOCK_PATTERN.equals(MgConfig.getLockType(S));
        items.add(UItem.asTopViewStatic("Qulflangan chat ochilganda kod so'raladi. Chatni qulflash: chat ichida ⋮ → \"Chatni qulflash\".", R.drawable.msg_secret));
        items.add(UItem.asHeader("Kod"));
        if (has) {
            items.add(UItem.asButton(ID_LOCK_TYPE, R.drawable.msg_secret, "Kod turi", pattern ? "Grafik kalit" : "PIN kod"));
            items.add(UItem.asButton(ID_CHANGE_CODE, R.drawable.msg_edit, pattern ? "Grafik kalitni o'zgartirish" : "PIN kodni o'zgartirish"));
        } else {
            items.add(UItem.asButton(ID_LOCK_TYPE, R.drawable.msg_secret, "Kod o'rnatish").accent());
        }
        items.add(UItem.asCheck(ID_FINGERPRINT, "Barmoq izi bilan ochish").setChecked(MgConfig.isFingerprintEnabled(S)));
        if (pattern) {
            items.add(UItem.asCheck(ID_INVISIBLE, "Ko'rinmas grafik kalit").setChecked(MgConfig.isPatternInvisible(S)));
        }
        items.add(UItem.asCheck(ID_VIBRATE, "Kiritishda tebranish").setChecked(MgConfig.isLockVibrate()));
        items.add(UItem.asShadow("Qulflangan chatlar: " + MgConfig.getLockedCount()));
        if (has) {
            items.add(UItem.asButton(ID_REMOVE, R.drawable.msg_delete, "Kodni va barcha qulflarni o'chirish").red());
            items.add(UItem.asShadow(null));
        }
    }

    private void toggle(View view, String key, boolean value) {
        MgConfig.setBool(key, value);
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_LOCK_TYPE: {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                builder.setTitle("Kod turi");
                builder.setItems(new CharSequence[]{"PIN kod (4 raqam)", "Grafik kalit (tasvirli kod)"}, (dialog, which) ->
                        MgChatLock.createLock(this, S, which == 1 ? MgConfig.LOCK_PATTERN : MgConfig.LOCK_PIN, () -> listView.adapter.update(true)));
                showDialog(builder.create());
                break;
            }
            case ID_CHANGE_CODE:
                MgChatLock.createLock(this, S, MgConfig.getLockType(S), () -> listView.adapter.update(true));
                break;
            case ID_FINGERPRINT:
                toggle(view, MgConfig.fingerprintKey(S), !MgConfig.isFingerprintEnabled(S));
                break;
            case ID_INVISIBLE:
                toggle(view, MgConfig.patternInvisibleKey(S), !MgConfig.isPatternInvisible(S));
                break;
            case ID_VIBRATE:
                toggle(view, "lock_vibrate", !MgConfig.isLockVibrate());
                break;
            case ID_REMOVE: {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
                builder.setTitle("Chat qulfini o'chirish");
                builder.setMessage("Kod o'chiriladi va barcha chatlar qulfdan chiqariladi.");
                builder.setPositiveButton("O'chirish", (dialog, which) -> {
                    MgConfig.removePinAndLocks();
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Chat qulfi o'chirildi").show();
                    listView.adapter.update(true);
                });
                builder.setNegativeButton("Bekor qilish", null);
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                break;
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
