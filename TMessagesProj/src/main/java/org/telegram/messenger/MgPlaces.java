/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 *
 * Hududlar nomlari: uzbgeo (MIT, O'zbekiston Statistika qo'mitasi SDMX 2.01.01.0036 asosida).
 * Koordinatalar: GeoNames (CC BY 4.0) aholi punktlari, qolganlari tuman markazlari bo'yicha taxminiy.
 */

package org.telegram.messenger;

/** O'zbekistonning barcha viloyatlari, shaharlari va tumanlari (markaz koordinatalari bilan) */
public final class MgPlaces {

    public static final class Place {
        public final String name;
        public final double lat, lon;

        Place(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static final class Region {
        public final String key, name;
        public final Place[] places;

        Region(String key, String name, Place[] places) {
            this.key = key;
            this.name = name;
            this.places = places;
        }
    }

    private static Place p(String n, double lat, double lon) {
        return new Place(n, lat, lon);
    }

    public static final Region[] REGIONS = {
            new Region("tashkent_city", "Toshkent shahri", new Place[]{
                    p("Toshkent shahri (markaz)", 41.311, 69.279),
                    p("Bektemir tumani", 41.21, 69.334),
                    p("Chilonzor tumani", 41.28, 69.2),
                    p("Mirobod tumani", 41.29, 69.28),
                    p("Mirzo Ulug'bek tumani", 41.33, 69.34),
                    p("Olmazor tumani", 41.35, 69.21),
                    p("Shayxontoxur tumani", 41.33, 69.23),
                    p("Sirg'ali tumani", 41.23, 69.22),
                    p("Uchtepa tumani", 41.29, 69.17),
                    p("Yakkasaroy tumani", 41.29, 69.26),
                    p("Yangihayot tumani", 41.21, 69.2),
                    p("Yashnobod tumani", 41.3, 69.34),
                    p("Yunusobod tumani", 41.37, 69.29),
            }),
            new Region("tashkent", "Toshkent viloyati", new Place[]{
                    p("Nurafshon shahri", 41.04, 69.36),
                    p("Angren shahri", 41.017, 70.144),
                    p("Bekobod shahri", 40.221, 69.27),
                    p("Bo'ka shahri", 40.811, 69.194),
                    p("Chinoz shahri", 40.936, 68.761),
                    p("Chirchiq shahri", 41.469, 69.582),
                    p("Do'stobod shahri", 40.9, 69.38),
                    p("G'azalkent shahri", 41.558, 69.771),
                    p("Keles shahri", 41.4, 69.2),
                    p("Ohangaron shahri", 40.906, 69.638),
                    p("Olmaliq shahri", 40.845, 69.598),
                    p("Oqqo'rg'on shahri", 40.88, 69.05),
                    p("Parkent shahri", 41.294, 69.676),
                    p("Piskent shahri", 40.897, 69.351),
                    p("Yangiyo'l shahri", 41.112, 69.047),
                    p("Bekobod tumani", 40.221, 69.27),
                    p("Bo'ka tumani", 40.811, 69.194),
                    p("Bo'stonliq tumani", 41.558, 69.771),
                    p("Chinoz tumani", 40.936, 68.761),
                    p("O'rtachirchiq tumani", 41.032, 69.363),
                    p("Ohangaron tumani", 40.906, 69.638),
                    p("Oqqo'rg'on tumani", 40.88, 69.05),
                    p("Parkent tumani", 41.294, 69.676),
                    p("Piskent tumani", 40.897, 69.351),
                    p("Qibray tumani", 41.39, 69.465),
                    p("Quyichirchiq tumani", 40.9, 69.38),
                    p("Toshkent tumani", 41.265, 69.216),
                    p("Yangiyo'l tumani", 41.112, 69.047),
                    p("Yuqorichirchiq tumani", 41.24, 69.5),
                    p("Zangiota tumani", 41.2, 69.11),
            }),
            new Region("andijan", "Andijon viloyati", new Place[]{
                    p("Andijon shahri", 40.782, 72.344),
                    p("Asaka shahri", 40.642, 72.239),
                    p("Jalaquduq shahri", 40.72, 72.64),
                    p("Marhamat shahri", 40.502, 72.326),
                    p("Paxtaobod shahri", 40.929, 72.497),
                    p("Poytug' shahri", 40.898, 72.245),
                    p("Qo'rg'ontepa shahri", 40.732, 72.762),
                    p("Qorasuv shahri", 40.72, 72.88),
                    p("Shahrixon shahri", 40.713, 72.057),
                    p("Xo'jaobod shahri", 40.669, 72.56),
                    p("Xonobod shahri", 40.8, 73.0),
                    p("Andijon tumani", 40.782, 72.344),
                    p("Asaka tumani", 40.642, 72.239),
                    p("Baliqchi tumani", 40.905, 71.847),
                    p("Bo'ston tumani", 40.7, 71.94),
                    p("Buloqboshi tumani", 40.629, 72.502),
                    p("Izboskan tumani", 40.898, 72.245),
                    p("Jalaquduq tumani", 40.72, 72.64),
                    p("Marxamat tumani", 40.502, 72.326),
                    p("Oltinko'l tumani", 40.801, 72.173),
                    p("Paxtaobod tumani", 40.929, 72.497),
                    p("Qo'rg'ontepa tumani", 40.732, 72.762),
                    p("Shaxrixon tumani", 40.713, 72.057),
                    p("Ulug'nor tumani", 40.77, 71.72),
                    p("Xo'jaobod tumani", 40.669, 72.56),
            }),
            new Region("bukhara", "Buxoro viloyati", new Place[]{
                    p("Buxoro shahri", 39.775, 64.429),
                    p("G'ijduvon shahri", 40.102, 64.682),
                    p("Galaosiyo shahri", 39.857, 64.446),
                    p("Kogon shahri", 39.727, 64.555),
                    p("Olot shahri", 39.415, 63.803),
                    p("Qorako'l shahri", 39.533, 63.833),
                    p("Qorovulbozor shahri", 39.501, 64.794),
                    p("Romitan shahri", 39.929, 64.379),
                    p("Shofirkon shahri", 40.12, 64.501),
                    p("Vobkent shahri", 40.0, 64.502),
                    p("Buxoro tumani", 39.775, 64.429),
                    p("G'ijduvon tumani", 40.102, 64.682),
                    p("Jondor tumani", 39.742, 64.18),
                    p("Kogon tumani", 39.727, 64.555),
                    p("Olot tumani", 39.415, 63.803),
                    p("Peshku tumani", 40.12, 64.27),
                    p("Qorako'l tumani", 39.533, 63.833),
                    p("Qorovulbozor tumani", 39.501, 64.794),
                    p("Romitan tumani", 39.929, 64.379),
                    p("Shofirkon tumani", 40.12, 64.501),
                    p("Vobkent tumani", 40.0, 64.502),
            }),
            new Region("fergana", "Farg'ona viloyati", new Place[]{
                    p("Farg'ona shahri", 40.384, 71.784),
                    p("Beshariq shahri", 40.436, 70.61),
                    p("Marg'ilon shahri", 40.472, 71.725),
                    p("Qo'qon shahri", 40.529, 70.942),
                    p("Quva shahri", 40.522, 72.073),
                    p("Quvasoy shahri", 40.297, 71.98),
                    p("Rishton shahri", 40.357, 71.285),
                    p("Yaypan shahri", 40.376, 70.816),
                    p("Beshariq tumani", 40.436, 70.61),
                    p("Bog'dod tumani", 40.463, 71.212),
                    p("Buvayda tumani", 40.59, 70.99),
                    p("Dang'ara tumani", 40.584, 70.914),
                    p("Farg'ona tumani", 40.384, 71.784),
                    p("Furqat tumani", 40.52, 70.66),
                    p("O'zbekiston tumani", 40.376, 70.816),
                    p("Oltiariq tumani", 40.392, 71.474),
                    p("Qo'shtepa tumani", 40.51, 71.58),
                    p("Quva tumani", 40.522, 72.073),
                    p("Rishton tumani", 40.357, 71.285),
                    p("So'x tumani", 39.977, 71.135),
                    p("Toshloq tumani", 40.477, 71.768),
                    p("Uchko'prik tumani", 40.542, 71.061),
                    p("Yozyovon tumani", 40.661, 71.744),
            }),
            new Region("jizzakh", "Jizzax viloyati", new Place[]{
                    p("Jizzax shahri", 40.123, 67.828),
                    p("Do'stlik shahri", 40.529, 68.032),
                    p("G'allaorol shahri", 40.027, 67.588),
                    p("Gagarin shahri", 40.665, 68.168),
                    p("Paxtakor shahri", 40.312, 67.957),
                    p("Arnasoy tumani", 40.495, 67.876),
                    p("Baxmal tumani", 39.74, 67.648),
                    p("Do'stlik tumani", 40.529, 68.032),
                    p("Forish tumani", 40.415, 67.179),
                    p("G'allaorol tumani", 40.027, 67.588),
                    p("Mirzacho'l tumani", 40.665, 68.168),
                    p("Paxtakor tumani", 40.312, 67.957),
                    p("Sharof Rashidov tumani", 40.204, 67.904),
                    p("Yangiobod tumani", 39.95, 68.6),
                    p("Zafarobod tumani", 40.389, 67.822),
                    p("Zarbdor tumani", 40.081, 68.165),
                    p("Zomin tumani", 39.961, 68.396),
            }),
            new Region("khorezm", "Xorazm viloyati", new Place[]{
                    p("Urganch shahri", 41.55, 60.633),
                    p("Pitnak shahri", 41.2, 61.18),
                    p("Xiva shahri", 41.378, 60.364),
                    p("Bog'ot tumani", 41.34, 60.82),
                    p("Gurlan tumani", 41.845, 60.392),
                    p("Qo'shko'pir tumani", 41.535, 60.346),
                    p("Shovot tumani", 41.656, 60.303),
                    p("Tuproqqal'a tumani", 41.2, 61.18),
                    p("Urganch tumani", 41.55, 60.633),
                    p("Xazorasp tumani", 41.319, 61.074),
                    p("Xiva tumani", 41.378, 60.364),
                    p("Xonqa tumani", 41.47, 60.78),
                    p("Yangiariq tumani", 41.35, 60.55),
                    p("Yangibozor tumani", 41.72, 60.93),
            }),
            new Region("namangan", "Namangan viloyati", new Place[]{
                    p("Namangan shahri", 40.998, 71.673),
                    p("Chortoq shahri", 41.069, 71.824),
                    p("Chust shahri", 41.003, 71.238),
                    p("Haqqulobod shahri", 40.917, 72.117),
                    p("Kosonsoy shahri", 41.249, 71.547),
                    p("Pop shahri", 40.874, 71.109),
                    p("Uchqo'rg'on shahri", 41.11, 72.08),
                    p("Chortoq tumani", 41.069, 71.824),
                    p("Chust tumani", 41.003, 71.238),
                    p("Kosonsoy tumani", 41.249, 71.547),
                    p("Mingbuloq tumani", 40.92, 71.28),
                    p("Namangan tumani", 40.998, 71.673),
                    p("Norin tumani", 40.917, 72.117),
                    p("Pop tumani", 40.874, 71.109),
                    p("To'raqo'rg'on tumani", 41.003, 71.511),
                    p("Uchqo'rg'on tumani", 41.114, 72.079),
                    p("Uychi tumani", 41.081, 71.923),
                    p("Yangiqo'rg'on tumani", 41.195, 71.724),
            }),
            new Region("navoi", "Navoiy viloyati", new Place[]{
                    p("Navoiy shahri", 40.084, 65.379),
                    p("G'ozg'on shahri", 40.58, 65.49),
                    p("Nurota shahri", 40.561, 65.689),
                    p("Qiziltepa shahri", 40.033, 64.85),
                    p("Uchquduq shahri", 42.15, 63.552),
                    p("Zarafshon shahri", 41.57, 64.2),
                    p("Karmana tumani", 40.138, 65.375),
                    p("Konimex tumani", 40.276, 65.145),
                    p("Navbahor tumani", 40.198, 65.335),
                    p("Nurota tumani", 40.561, 65.689),
                    p("Qiziltepa tumani", 40.033, 64.85),
                    p("Tomdi tumani", 41.751, 64.617),
                    p("Uchquduq tumani", 42.15, 63.552),
                    p("Xatirchi tumani", 40.025, 65.961),
            }),
            new Region("kashkadarya", "Qashqadaryo viloyati", new Place[]{
                    p("Qarshi shahri", 38.861, 65.789),
                    p("Beshkent shahri", 38.821, 65.653),
                    p("Chiroqchi shahri", 39.034, 66.572),
                    p("G'uzor shahri", 38.626, 66.245),
                    p("Kitob shahri", 39.123, 66.876),
                    p("Koson shahri", 39.038, 65.585),
                    p("Muborak shahri", 39.258, 65.157),
                    p("Qamashi shahri", 38.82, 66.464),
                    p("Shahrisabz shahri", 39.052, 66.821),
                    p("Tallimarjon shahri", 38.29, 65.55),
                    p("Yakkabog' shahri", 38.977, 66.689),
                    p("Yangi Nishon shahri", 38.645, 65.69),
                    p("Chiroqchi tumani", 39.034, 66.572),
                    p("Dehqonobod tumani", 38.342, 66.563),
                    p("G'uzor tumani", 38.626, 66.245),
                    p("Kasbi tumani", 38.919, 65.412),
                    p("Kitob tumani", 39.123, 66.876),
                    p("Ko'kdala tumani", 39.0, 66.3),
                    p("Koson tumani", 39.038, 65.585),
                    p("Mirishkor tumani", 38.851, 65.278),
                    p("Muborak tumani", 39.258, 65.157),
                    p("Nishon tumani", 38.694, 65.675),
                    p("Qamashi tumani", 38.82, 66.464),
                    p("Qarshi tumani", 38.861, 65.789),
                    p("Shahrisabz tumani", 39.052, 66.821),
                    p("Yakkabog' tumani", 38.977, 66.689),
            }),
            new Region("karakalpakstan", "Qoraqalpog'iston Respublikasi", new Place[]{
                    p("Nukus shahri", 42.453, 59.61),
                    p("Beruniy shahri", 41.699, 60.755),
                    p("Bo'ston shahri", 41.846, 60.947),
                    p("Chimboy shahri", 42.93, 59.782),
                    p("Mang'it shahri", 42.122, 60.063),
                    p("Mo'ynoq shahri", 43.768, 59.021),
                    p("Qo'ng'irot shahri", 43.052, 58.846),
                    p("Shumanay shahri", 42.634, 58.931),
                    p("To'rtko'l shahri", 41.561, 61.002),
                    p("Xo'jayli shahri", 42.409, 59.445),
                    p("Amudaryo tumani", 42.116, 60.06),
                    p("Beruniy tumani", 41.699, 60.755),
                    p("Bo'zatov tumani", 42.93, 59.13),
                    p("Chimboy tumani", 42.93, 59.782),
                    p("Ellikkala tumani", 41.846, 60.947),
                    p("Kegeyli tumani", 42.777, 59.608),
                    p("Mo'ynoq tumani", 43.768, 59.021),
                    p("Nukus tumani", 42.453, 59.61),
                    p("Qanliko'l tumani", 42.84, 59.001),
                    p("Qo'ng'irot tumani", 43.052, 58.846),
                    p("Qorao'zak tumani", 43.022, 60.017),
                    p("Shumanay tumani", 42.634, 58.931),
                    p("Taxiatosh tumani", 42.32, 59.6),
                    p("Taxtako'pir tumani", 43.012, 60.301),
                    p("To'rtko'l tumani", 41.561, 61.002),
                    p("Xo'jayli tumani", 42.409, 59.445),
            }),
            new Region("samarkand", "Samarqand viloyati", new Place[]{
                    p("Samarqand shahri", 39.654, 66.96),
                    p("Bulung'ur shahri", 39.76, 67.274),
                    p("Chelak shahri", 39.92, 66.862),
                    p("Ishtixon shahri", 39.966, 66.486),
                    p("Jomboy shahri", 39.699, 67.093),
                    p("Juma shahri", 39.716, 66.664),
                    p("Kattaqo'rg'on shahri", 39.905, 66.266),
                    p("Nurobod shahri", 39.609, 66.287),
                    p("Payariq shahri", 39.992, 66.85),
                    p("Bulung'ur tumani", 39.76, 67.274),
                    p("Ishtixon tumani", 39.966, 66.486),
                    p("Jomboy tumani", 39.699, 67.093),
                    p("Kattaqo'rg'on tumani", 39.905, 66.266),
                    p("Narpay tumani", 39.927, 65.93),
                    p("Nurobod tumani", 39.609, 66.287),
                    p("Oqdaryo tumani", 39.879, 66.751),
                    p("Pastdarg'om tumani", 39.716, 66.664),
                    p("Paxtachi tumani", 40.031, 65.666),
                    p("Payariq tumani", 39.992, 66.85),
                    p("Qo'shrabot tumani", 40.254, 66.688),
                    p("Samarqand tumani", 39.654, 66.96),
                    p("Tayloq tumani", 39.601, 67.091),
                    p("Urgut tumani", 39.419, 67.261),
            }),
            new Region("syrdarya", "Sirdaryo viloyati", new Place[]{
                    p("Guliston shahri", 40.49, 68.784),
                    p("Baxt shahri", 40.32, 68.95),
                    p("Shirin shahri", 40.235, 69.13),
                    p("Sirdaryo shahri", 40.844, 68.662),
                    p("Yangiyer shahri", 40.275, 68.823),
                    p("Boyovut tumani", 40.41, 68.94),
                    p("Guliston tumani", 40.49, 68.784),
                    p("Mirzaobod tumani", 40.46, 68.58),
                    p("Oqoltin tumani", 40.6, 68.35),
                    p("Sardoba tumani", 40.29, 68.24),
                    p("Sayxunobod tumani", 40.66, 68.73),
                    p("Sirdaryo tumani", 40.844, 68.662),
                    p("Xovos tumani", 40.22, 68.83),
            }),
            new Region("surkhandarya", "Surxondaryo viloyati", new Place[]{
                    p("Termiz shahri", 37.224, 67.278),
                    p("Boysun shahri", 38.208, 67.207),
                    p("Denov shahri", 38.267, 67.899),
                    p("Jarqo'rg'on shahri", 37.51, 67.41),
                    p("Qumqo'rg'on shahri", 37.83, 67.59),
                    p("Sharg'un shahri", 38.46, 67.96),
                    p("Sherobod shahri", 37.67, 67.0),
                    p("Sho'rchi shahri", 37.999, 67.787),
                    p("Angor tumani", 37.47, 67.13),
                    p("Bandixon tumani", 37.86, 67.4),
                    p("Boysun tumani", 38.208, 67.207),
                    p("Denov tumani", 38.267, 67.899),
                    p("Jarqo'rg'on tumani", 37.51, 67.41),
                    p("Muzrabot tumani", 37.55, 67.0),
                    p("Oltinsoy tumani", 38.45, 67.7),
                    p("Qiziriq tumani", 37.66, 67.23),
                    p("Qumqo'rg'on tumani", 37.83, 67.59),
                    p("Sariosiyo tumani", 38.42, 67.96),
                    p("Sherobod tumani", 37.67, 67.0),
                    p("Sho'rchi tumani", 37.999, 67.787),
                    p("Termiz tumani", 37.33, 67.25),
                    p("Uzun tumani", 38.36, 68.02),
            }),
    };

    public static Region findRegion(String key) {
        for (Region r : REGIONS) {
            if (r.key.equals(key)) {
                return r;
            }
        }
        return REGIONS[0];
    }

    public static Place findPlace(String regionKey, String name) {
        Region r = findRegion(regionKey);
        for (Place p : r.places) {
            if (p.name.equals(name)) {
                return p;
            }
        }
        return r.places[0];
    }
}
