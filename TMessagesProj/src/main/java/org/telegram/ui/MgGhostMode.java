/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.util.Base64;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.ArrayList;

/**
 * Sharpa rejimi — boshqalarga sezdirmasdan Telegram'dan foydalanish:
 *  • o'qilganlik belgisi yuborilmaydi (✓✓ ko'k bo'lmaydi);
 *  • "yozmoqda…" holati yuborilmaydi;
 *  • ilova ochiq bo'lsa ham "onlayn" bo'lib ko'rinmaysiz;
 *  • hikoyalarni ko'rganingiz bilinmaydi;
 *  • ixtiyoriy: Telegram maxfiylik sozlamasida "Oxirgi marta onlayn" va telefon raqami — "Hech kim".
 * Har bir qismni sozlamalarda alohida o'chirish mumkin. O'chirilganda maxfiylik sozlamalari aynan tiklanadi.
 */
public class MgGhostMode {

    private static final int[] TYPES = {
            ContactsController.PRIVACY_RULES_TYPE_LASTSEEN,
            ContactsController.PRIVACY_RULES_TYPE_PHONE,
            ContactsController.PRIVACY_RULES_TYPE_FORWARDS
    };

    public static final String OPT_READ = "ghost_opt_read";
    public static final String OPT_TYPING = "ghost_opt_typing";
    public static final String OPT_ONLINE = "ghost_opt_online";
    public static final String OPT_STORIES = "ghost_opt_stories";
    public static final String OPT_PRIVACY = "ghost_opt_privacy";

    /** Sharpa rejimi yoqilganmi (hisob bo'yicha) */
    public static boolean isEnabled(int account) {
        return MgConfig.getBool("ghost_mode_" + account, privacyApplied(account));
    }

    public static boolean option(String key) {
        return MgConfig.getBool(key, !OPT_PRIVACY.equals(key));
    }

    public static boolean hideRead(int account) {
        return isEnabled(account) && option(OPT_READ);
    }

    public static boolean hideTyping(int account) {
        return isEnabled(account) && option(OPT_TYPING);
    }

    public static boolean hideOnline(int account) {
        return isEnabled(account) && option(OPT_ONLINE);
    }

    public static boolean hideStories(int account) {
        return isEnabled(account) && option(OPT_STORIES);
    }

    /** Telegram maxfiylik sozlamalari sharpa uchun o'zgartirilganmi */
    private static boolean privacyApplied(int account) {
        return MgConfig.getBool("ghost_" + account, false);
    }

    /**
     * Sharpa rejimini yoqadi yoki o'chiradi.
     * @param done natija: null — muvaffaqiyatli, aks holda xato matni
     */
    public static void setEnabled(int account, boolean enable, Utilities.Callback<String> done) {
        boolean wantPrivacy = enable && option(OPT_PRIVACY);
        boolean needPrivacyChange = wantPrivacy != privacyApplied(account);
        if (!needPrivacyChange) {
            applyFlag(account, enable);
            done.run(null);
            return;
        }
        setPrivacy(account, wantPrivacy, err -> {
            // maxfiylik o'zgarmasa ham asosiy sharpa rejimi baribir ishlaydi
            applyFlag(account, enable);
            done.run(err);
        });
    }

    private static void applyFlag(int account, boolean enable) {
        MgConfig.setBool("ghost_mode_" + account, enable);
        // onlayn holatni darhol yangilash (taymer keyingi aylanishda to'g'ri holatni yuboradi)
        MessagesController.getInstance(account).getNotificationCenter().postNotificationName(org.telegram.messenger.NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
    }

    /** Sharpa yoqiq turganda "Oxirgi marta onlayn" bandi o'zgartirilsa — maxfiylikni moslash */
    public static void onPrivacyOptionChanged(int account, Utilities.Callback<String> done) {
        if (!isEnabled(account)) {
            done.run(null);
            return;
        }
        boolean want = option(OPT_PRIVACY);
        if (want == privacyApplied(account)) {
            done.run(null);
            return;
        }
        setPrivacy(account, want, done);
    }

    private static TLRPC.InputPrivacyKey keyFor(int type) {
        switch (type) {
            case ContactsController.PRIVACY_RULES_TYPE_PHONE:
                return new TLRPC.TL_inputPrivacyKeyPhoneNumber();
            case ContactsController.PRIVACY_RULES_TYPE_FORWARDS:
                return new TLRPC.TL_inputPrivacyKeyForwards();
            case ContactsController.PRIVACY_RULES_TYPE_LASTSEEN:
            default:
                return new TLRPC.TL_inputPrivacyKeyStatusTimestamp();
        }
    }

    /** Sozlamalar yuklanganmi */
    public static boolean isReady(int account) {
        ContactsController cc = ContactsController.getInstance(account);
        for (int type : TYPES) {
            if (cc.getPrivacyRules(type) == null) {
                cc.loadPrivacySettings();
                return false;
            }
        }
        return true;
    }

    /** Telegram maxfiylik sozlamalarini sharpa uchun o'zgartirish yoki tiklash */
    private static void setPrivacy(int account, boolean enable, Utilities.Callback<String> done) {
        if (!isReady(account)) {
            done.run(org.telegram.messenger.MgLang.t("Maxfiylik sozlamalari yuklanmoqda. Bir necha soniyadan keyin qayta urinib ko'ring."));
            return;
        }
        ContactsController cc = ContactsController.getInstance(account);
        final int[] pending = {TYPES.length};
        final String[] error = {null};
        for (int type : TYPES) {
            ArrayList<TLRPC.InputPrivacyRule> newRules = new ArrayList<>();
            if (enable) {
                backup(account, type, cc.getPrivacyRules(type));
                newRules.add(new TLRPC.TL_inputPrivacyValueDisallowAll());
            } else {
                ArrayList<TLRPC.PrivacyRule> old = restore(account, type);
                if (old == null || old.isEmpty()) {
                    newRules.add(new TLRPC.TL_inputPrivacyValueAllowContacts());
                } else {
                    for (TLRPC.PrivacyRule rule : old) {
                        TLRPC.InputPrivacyRule input = toInput(account, rule);
                        if (input != null) {
                            newRules.add(input);
                        }
                    }
                    if (newRules.isEmpty()) {
                        newRules.add(new TLRPC.TL_inputPrivacyValueAllowContacts());
                    }
                }
            }
            TL_account.setPrivacy req = new TL_account.setPrivacy();
            req.key = keyFor(type);
            req.rules = newRules;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (response instanceof TL_account.privacyRules) {
                    TL_account.privacyRules rules = (TL_account.privacyRules) response;
                    MessagesController.getInstance(account).putUsers(rules.users, false);
                    MessagesController.getInstance(account).putChats(rules.chats, false);
                    cc.setPrivacyRules(rules.rules, type);
                } else if (err != null) {
                    error[0] = err.text;
                }
                pending[0]--;
                if (pending[0] == 0) {
                    if (error[0] == null) {
                        MgConfig.setBool("ghost_" + account, enable);
                    }
                    done.run(error[0]);
                }
            }), ConnectionsManager.RequestFlagFailOnServerErrors);
        }
    }

    private static void backup(int account, int type, ArrayList<TLRPC.PrivacyRule> rules) {
        try {
            SerializedData data = new SerializedData();
            data.writeInt32(rules == null ? 0 : rules.size());
            if (rules != null) {
                for (TLRPC.PrivacyRule rule : rules) {
                    rule.serializeToStream(data);
                }
            }
            MgConfig.setString("ghost_backup_" + account + "_" + type, Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP));
            data.cleanup();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static ArrayList<TLRPC.PrivacyRule> restore(int account, int type) {
        try {
            String s = MgConfig.getString("ghost_backup_" + account + "_" + type, null);
            if (s == null) {
                return null;
            }
            SerializedData data = new SerializedData(Base64.decode(s, Base64.NO_WRAP));
            int count = data.readInt32(false);
            ArrayList<TLRPC.PrivacyRule> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                TLRPC.PrivacyRule rule = TLRPC.PrivacyRule.TLdeserialize(data, data.readInt32(false), false);
                if (rule != null) {
                    result.add(rule);
                }
            }
            data.cleanup();
            return result;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    private static TLRPC.InputPrivacyRule toInput(int account, TLRPC.PrivacyRule rule) {
        MessagesController mc = MessagesController.getInstance(account);
        if (rule instanceof TLRPC.TL_privacyValueAllowAll) {
            return new TLRPC.TL_inputPrivacyValueAllowAll();
        } else if (rule instanceof TLRPC.TL_privacyValueAllowContacts) {
            return new TLRPC.TL_inputPrivacyValueAllowContacts();
        } else if (rule instanceof TLRPC.TL_privacyValueDisallowAll) {
            return new TLRPC.TL_inputPrivacyValueDisallowAll();
        } else if (rule instanceof TLRPC.TL_privacyValueDisallowContacts) {
            return new TLRPC.TL_inputPrivacyValueDisallowContacts();
        } else if (rule instanceof TLRPC.TL_privacyValueAllowCloseFriends) {
            return new TLRPC.TL_inputPrivacyValueAllowCloseFriends();
        } else if (rule instanceof TLRPC.TL_privacyValueAllowPremium) {
            return new TLRPC.TL_inputPrivacyValueAllowPremium();
        } else if (rule instanceof TLRPC.TL_privacyValueAllowBots) {
            return new TLRPC.TL_inputPrivacyValueAllowBots();
        } else if (rule instanceof TLRPC.TL_privacyValueDisallowBots) {
            return new TLRPC.TL_inputPrivacyValueDisallowBots();
        } else if (rule instanceof TLRPC.TL_privacyValueAllowUsers) {
            TLRPC.TL_inputPrivacyValueAllowUsers r = new TLRPC.TL_inputPrivacyValueAllowUsers();
            for (Long id : ((TLRPC.TL_privacyValueAllowUsers) rule).users) {
                TLRPC.InputUser u = mc.getInputUser(id);
                if (u != null) {
                    r.users.add(u);
                }
            }
            return r;
        } else if (rule instanceof TLRPC.TL_privacyValueDisallowUsers) {
            TLRPC.TL_inputPrivacyValueDisallowUsers r = new TLRPC.TL_inputPrivacyValueDisallowUsers();
            for (Long id : ((TLRPC.TL_privacyValueDisallowUsers) rule).users) {
                TLRPC.InputUser u = mc.getInputUser(id);
                if (u != null) {
                    r.users.add(u);
                }
            }
            return r;
        } else if (rule instanceof TLRPC.TL_privacyValueAllowChatParticipants) {
            TLRPC.TL_inputPrivacyValueAllowChatParticipants r = new TLRPC.TL_inputPrivacyValueAllowChatParticipants();
            r.chats.addAll(((TLRPC.TL_privacyValueAllowChatParticipants) rule).chats);
            return r;
        } else if (rule instanceof TLRPC.TL_privacyValueDisallowChatParticipants) {
            TLRPC.TL_inputPrivacyValueDisallowChatParticipants r = new TLRPC.TL_inputPrivacyValueDisallowChatParticipants();
            r.chats.addAll(((TLRPC.TL_privacyValueDisallowChatParticipants) rule).chats);
            return r;
        }
        return null;
    }
}
