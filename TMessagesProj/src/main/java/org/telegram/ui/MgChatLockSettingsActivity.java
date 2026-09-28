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
        MgChatLock.askLock(fragment, S, org.telegram.messenger.MgLang.t("Chat qulfi sozlamalari"), ok -> {
            if (ok) {
                fragment.presentFragment(new MgChatLockSettingsActivity());
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return org.telegram.messenger.MgLang.t("Chat qulfi");
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        boolean has = MgConfig.hasLock(S);
        boolean pattern = MgConfig.LOCK_PATTERN.equals(MgConfig.getLockType(S));
        items.add(UItem.asTopViewStatic(org.telegram.messenger.MgLang.t("Qulflangan chat ochilganda kod so'raladi. Chatni qulflash: chat ichida ⋮ → \"Chatni qulflash\"."), R.drawable.msg_secret));
        items.add(UItem.asHeader(org.telegram.messenger.MgLang.t("Kod")));
        if (has) {
            items.add(UItem.asButton(ID_LOCK_TYPE, R.drawable.msg_secret, org.telegram.messenger.MgLang.t("Kod turi"), pattern ? org.telegram.messenger.MgLang.t("Grafik kalit") : org.telegram.messenger.MgLang.t("PIN kod")));
            items.add(UItem.asButton(ID_CHANGE_CODE, R.drawable.msg_edit, pattern ? org.telegram.messenger.MgLang.t("Grafik kalitni o'zgartirish") : org.telegram.messenger.MgLang.t("PIN kodni o'zgartirish")));
        } else {
            items.add(UItem.asButton(ID_LOCK_TYPE, R.drawable.msg_secret, org.telegram.messenger.MgLang.t("Kod o'rnatish")).accent());
        }
        items.add(UItem.asCheck(ID_FINGERPRINT, org.telegram.messenger.MgLang.t("Barmoq izi bilan ochish")).setChecked(MgConfig.isFingerprintEnabled(S)));
        if (pattern) {
            items.add(UItem.asCheck(ID_INVISIBLE, org.telegram.messenger.MgLang.t("Ko'rinmas grafik kalit")).setChecked(MgConfig.isPatternInvisible(S)));
        }
        items.add(UItem.asCheck(ID_VIBRATE, org.telegram.messenger.MgLang.t("Kiritishda tebranish")).setChecked(MgConfig.isLockVibrate()));
        items.add(UItem.asShadow(org.telegram.messenger.MgLang.t("Qulflangan chatlar: ") + MgConfig.getLockedCount()));
        if (has) {
            items.add(UItem.asButton(ID_REMOVE, R.drawable.msg_delete, org.telegram.messenger.MgLang.t("Kodni va barcha qulflarni o'chirish")).red());
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
                builder.setTitle(org.telegram.messenger.MgLang.t("Kod turi"));
                builder.setItems(new CharSequence[]{org.telegram.messenger.MgLang.t("PIN kod (4 raqam)"), org.telegram.messenger.MgLang.t("Grafik kalit (tasvirli kod)")}, (dialog, which) ->
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
                builder.setTitle(org.telegram.messenger.MgLang.t("Chat qulfini o'chirish"));
                builder.setMessage(org.telegram.messenger.MgLang.t("Kod o'chiriladi va barcha chatlar qulfdan chiqariladi."));
                builder.setPositiveButton(org.telegram.messenger.MgLang.t("O'chirish"), (dialog, which) -> {
                    MgConfig.removePinAndLocks();
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, org.telegram.messenger.MgLang.t("Chat qulfi o'chirildi")).show();
                    listView.adapter.update(true);
                });
                builder.setNegativeButton(org.telegram.messenger.MgLang.t("Bekor qilish"), null);
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
