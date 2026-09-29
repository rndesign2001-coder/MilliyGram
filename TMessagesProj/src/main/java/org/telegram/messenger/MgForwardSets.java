/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Uzatish oynasi uchun chat to'plamlari: "Reklama kanallarim (5 ta)" kabi nomli to'plamlar
 * va oxirgi marta yuborilgan chatlar ro'yxati. Faqat telefonda saqlanadi.
 */
public final class MgForwardSets {

    public static final class Set {
        public String name;
        public final ArrayList<Long> ids = new ArrayList<>();
    }

    private MgForwardSets() {
    }

    private static String key(int account) {
        return "fwd_sets_" + account;
    }

    public static ArrayList<Set> load(int account) {
        ArrayList<Set> out = new ArrayList<>();
        try {
            String raw = MgConfig.getString(key(account), null);
            if (raw != null) {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Set s = new Set();
                    s.name = o.optString("n", "");
                    JSONArray ids = o.optJSONArray("i");
                    if (ids != null) {
                        for (int k = 0; k < ids.length(); k++) {
                            s.ids.add(ids.optLong(k));
                        }
                    }
                    out.add(s);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return out;
    }

    private static void save(int account, ArrayList<Set> sets) {
        try {
            JSONArray arr = new JSONArray();
            for (Set s : sets) {
                JSONObject o = new JSONObject();
                o.put("n", s.name);
                JSONArray ids = new JSONArray();
                for (Long id : s.ids) {
                    ids.put(id);
                }
                o.put("i", ids);
                arr.put(o);
            }
            MgConfig.setString(key(account), arr.toString());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void add(int account, String name, ArrayList<Long> ids) {
        ArrayList<Set> sets = load(account);
        for (int i = sets.size() - 1; i >= 0; i--) {
            if (sets.get(i).name.equalsIgnoreCase(name)) {
                sets.remove(i);
            }
        }
        Set s = new Set();
        s.name = name;
        s.ids.addAll(ids);
        sets.add(0, s);
        save(account, sets);
    }

    public static void remove(int account, int index) {
        ArrayList<Set> sets = load(account);
        if (index >= 0 && index < sets.size()) {
            sets.remove(index);
            save(account, sets);
        }
    }

    /** Oxirgi yuborilgan chatlar (bir nechta chatga yuborilganda eslab qolinadi) */
    public static ArrayList<Long> getLast(int account) {
        ArrayList<Long> out = new ArrayList<>();
        String raw = MgConfig.getString("fwd_last_" + account, "");
        if (raw != null && !raw.isEmpty()) {
            for (String p : raw.split(",")) {
                try {
                    out.add(Long.parseLong(p));
                } catch (Throwable ignore) {
                }
            }
        }
        return out;
    }

    public static void saveLast(int account, ArrayList<Long> ids) {
        if (ids == null || ids.size() < 2) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        MgConfig.setString("fwd_last_" + account, sb.toString());
    }
}
