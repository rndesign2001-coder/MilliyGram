# MilliyGram — Play Console'ni to'ldirish bo'yicha to'liq yo'riqnoma (agent uchun)

> Bu fayl Play Console'da MilliyGram ilovasini **to'liq** to'ldiradigan yordamchi (inson yoki AI agent) uchun.
> Har bir bo'limda aniq qaysi tugmani bosish va maydonlarga aynan nima yozish kerakligi ko'rsatilgan.
> Play Console interfeysi inglizcha bo'lgani uchun tugma va maydon nomlari **inglizcha** berilgan.

---

## 0. Agent uchun qoidalar (MAJBURIY)

1. **Hech qanday parol, kalit yoki maxfiy fayl** so'ramang, saqlamang va hech qayerga yozmang (keystore, API_HASH, service account JSON va h.k.).
2. Ilovani **Production'ga chiqarmang** — faqat **Internal testing** va **Closed testing**. Production'ga chiqarishni egasi o'zi tasdiqlaydi.
3. **Narx — bepul (Free)**. Hech qanday to'lov, obuna yoki reklama sozlamasini yoqmang.
4. Quyida `⟨EGASIDAN SO'RANG⟩` deb belgilangan joylarni o'zingiz to'qib chiqarmang — ilova egasidan so'rang.
5. Har bir bo'limni saqlagach (**Save**) keyingisiga o'ting. Oxirida **Publishing overview**da hamma bo'lim yashil ✅ ekanini tekshiring.
6. Xato yoki noma'lum savol chiqsa — to'xtab, egasiga skrinshot bilan xabar bering.

---

## 1. Asosiy ma'lumotlar (o'zgarmaydi)

| Narsa | Qiymat |
|---|---|
| Ilova nomi | `MilliyGram — milliy messenjer` |
| Package name | `uz.milliygram.app` (AAB fayldan avtomatik olinadi, o'zgartirib bo'lmaydi) |
| Versiya | `12.10.3` |
| Asosiy til | **Uzbek – uz** |
| Turi | **App** (o'yin emas) |
| Narx | **Free** |
| Toifa | **Communication** |
| Teglar (Tags) | Messaging, Communication, Chat (ruxsat berilganlaridan) |
| Ishlab chiquvchi emaili | ⟨EGASIDAN SO'RANG⟩ |
| Website | `https://t.me/MilliyGramm` |
| Maxfiylik siyosati URL | `https://github.com/rndesign2001-coder/MilliyGram/blob/claude/zen-hypatia-ngf4ae/docs/play-market/privacy-policy.md` |
| Akkauntni o'chirish URL | `https://my.telegram.org/delete` |

---

## 2. Fayllar

| Fayl | Qayerdan olinadi | Qayerga yuklanadi |
|---|---|---|
| **AAB** | GitHub Actions → "MilliyGram Google Play (AAB)" → **eng oxirgi muvaffaqiyatli** run → **Artifacts** → `MilliyGram-play-12.10.3-bXXXX.aab` (zip ichida; ichidan `.aab`ni chiqaring). Egasi faylni o'zi beradi. | Testing → Internal testing → Create new release |
| **App icon** (512×512 PNG) | https://raw.githubusercontent.com/rndesign2001-coder/MilliyGram/claude/zen-hypatia-ngf4ae/docs/play-market/icon-512.png | Store listing → App icon |
| **Feature graphic** (1024×500 PNG) | https://raw.githubusercontent.com/rndesign2001-coder/MilliyGram/claude/zen-hypatia-ngf4ae/docs/play-market/feature-graphic-1024x500.png | Store listing → Feature graphic |
| **Telefon skrinshotlari** (kamida 2 ta, tavsiya 4–8 ta, vertikal) | ⟨EGASIDAN SO'RANG⟩ — telefondan olingan bo'lishi kerak | Store listing → Phone screenshots |

**Skrinshotlar talabi:** PNG yoki JPEG, har tomoni 320–3840 px, nisbat 9:16 atrofida. Ularda shaxsiy chatlar, telefon raqamlari va **Telegram logotipi bo'lmasligi** kerak.
Tavsiya etiladigan ekranlar: chatlar ro'yxati, qibla kompasi, namoz vaqtlari, MilliyGram sozlamalari, chat qulfi ekrani, kanal profilidagi tezkor tugmalar.

---

## 3. Ilovani yaratish (agar hali yaratilmagan bo'lsa)

**Home → Create app**
- App name: `MilliyGram — milliy messenjer`
- Default language: **Uzbek – uz**
- App or game: **App**
- Free or paid: **Free**
- Declarations: ikkala belgini qo'ying (Developer Program Policies, US export laws) → **Create app**

---

## 4. App signing (birinchi relizdan oldin)

**Test and release → Setup → App signing**
- Ilova egasi qaysi usulni tanlaganini so'rang ⟨EGASIDAN SO'RANG⟩:
  - **"Use existing app signing key"** (tavsiya) — egasi PEPK bilan eksport qilingan faylni o'zi yuklaydi. Agent keystore parolini **so'ramaydi**.
  - yoki **"Let Google manage and protect your app signing key"** — bu holda shunchaki AAB yuklanadi.

---

## 5. Main store listing

**Grow users → Store presence → Main store listing**

### Uzbek (uz) — asosiy

**App name** (30 belgigacha):
```
MilliyGram — milliy messenjer
```

**Short description** (80 belgigacha):
```
O'zbekcha qulay messenjer: chat qulfi, namoz vaqti, qibla, kanal adminlari uchun
```

**Full description** (4000 belgigacha):
```
MilliyGram — o'zbek foydalanuvchilari uchun qo'shimcha imkoniyatlarga ega, Telegram API asosida ishlaydigan norasmiy messenjer klienti.

🔒 MAXFIYLIK
• Istalgan chatni PIN kod yoki grafik kalit bilan qulflash
• Yashirin bo'lim — tanlangan chatlar ro'yxatdan yashiriladi
• Sharpa rejimi — o'qiganingiz, yozayotganingiz va onlayn ekaningiz ko'rinmaydi
• Yangi qurilmadan kirish haqida darhol ogohlantirish
• Telefonni silkitsangiz, maxfiy chatlar darhol yopiladi
• Firibgarlik xabarlari va xavfli APK fayllardan ogohlantirish

🕌 NAMOZ, QIBLA VA OB-HAVO
• O'zbekistonning barcha tumanlari uchun namoz vaqtlari (internetsiz)
• Jonli qibla kompasi — 3 xil dizayn, GPS bilan aniq yo'nalish
• Namoz vaqtini eslatish va 3 kunlik ob-havo

📢 KANAL VA GURUH ADMINLARI UCHUN
• Reklama postini belgilangan vaqtdan keyin avtomatik o'chirish (1/24, 1/48)
• Bitta postni ko'p kanalga tahrirlab yuborish
• Obunachilar kundaligi va eng yaxshi post vaqti tavsiyasi
• Profilda tezkor tugmalar: obunachilar, adminlar, ruxsatlar, so'nggi amallar
• "So'nggi amallar"da xabar qachon o'chirilgani va jurnalni eksport qilish
• "Mening kanal va guruhlarim" jildi — o'zingiz yaratgan chatlar bir joyda
• Kanal va guruh uchun doimiy avto-o'chirish rejimi

👓 ODDIY REJIM (KEKSALAR UCHUN)
• Katta yozuvlar, katta tugmalar va soddalashtirilgan menyu

✨ BOSHQA QULAYLIKLAR
• Lotin ↔ kirill o'girish, yozganingizni tarjima qilib yuborish
• Milliy naqshli mavzular
• Lokal jildlar, ⭐ "Tanlanganlar" jildi, avto-javob, shablonlar
• Xabarni ikki marta bosib tez javob berish, nusxalash yoki saqlash
• Stiker, GIF va ovozli xabarni yuborishdan oldin tasdiqlash
• Guruhdagi barcha xabarlaringizni bir tugma bilan o'chirish
• O'zbek, rus va ingliz tillarida

MilliyGram Telegram FZ-LLC tomonidan ishlab chiqilmagan. U Telegram'ning ochiq manba kodi (GPL v2) asosida qurilgan. Xabarlaringiz to'g'ridan-to'g'ri Telegram serverlari orqali yuboriladi; MilliyGram hech qanday shaxsiy ma'lumot yig'maydi.
```

### English (en-US) — tarjima (Add translations → English (United States))

**App name:**
```
MilliyGram — Uzbek messenger
```
**Short description:**
```
Uzbek-first messenger: chat lock, ghost mode, prayer times, tools for admins
```
**Full description:**
```
MilliyGram is an unofficial messaging client built on the Telegram API, with extra features for Uzbek users.

🔒 PRIVACY
• Lock any chat with a PIN or pattern
• Hidden section for selected chats
• Ghost mode: others don't see that you read, type or are online
• Instant alert when someone signs in from a new device
• Shake the phone to close private chats instantly
• Scam message and dangerous APK warnings

🕌 PRAYER TIMES, QIBLA AND WEATHER
• Prayer times for every district of Uzbekistan (offline)
• Live Qibla compass with GPS
• Prayer reminders and 3-day weather

📢 FOR CHANNEL AND GROUP ADMINS
• Auto-delete ad posts after a set time
• Send one post to many channels
• Subscriber diary and best posting time
• Quick buttons: subscribers, admins, permissions, recent actions
• See when a message was deleted and export recent actions
• "My channels and groups" folder

👓 SIMPLE MODE for older users — large text and buttons

✨ MORE
• Latin ↔ Cyrillic conversion, translate before sending
• National pattern themes, local folders, templates, auto-reply
• Double-tap actions, send confirmations, delete all my messages in a group
• Uzbek, Russian and English

MilliyGram is not developed by Telegram FZ-LLC. It is built on Telegram's open-source code (GPL v2). Messages go directly through Telegram servers; MilliyGram does not collect personal data.
```

### Russian (ru-RU) — ixtiyoriy tarjima
**App name:** `MilliyGram — мессенджер`
**Short description:** `Мессенджер на узбекском: замок чатов, режим призрака, время намаза, кибла`

### Grafika va kontakt (shu sahifada)
- App icon → `icon-512.png`
- Feature graphic → `feature-graphic-1024x500.png`
- Phone screenshots → egasidan olingan skrinshotlar
- **Store settings** (Grow users → Store presence → Store settings): Category **Communication**, Email ⟨EGASIDAN SO'RANG⟩, Website `https://t.me/MilliyGramm`

---

## 6. App content (Policy and programs → App content)

Har bir bo'limni oching, javob bering, **Save**.

### 6.1 Privacy policy
- URL: `https://github.com/rndesign2001-coder/MilliyGram/blob/claude/zen-hypatia-ngf4ae/docs/play-market/privacy-policy.md`

### 6.2 App access
- **"All or some functionality in my app is restricted"**
- **Add instructions:**
  - Name: `Telegram login`
  - Username/phone: ⟨EGASIDAN SO'RANG — sinov uchun telefon raqami bo'lsa⟩
  - Instructions:
```
MilliyGram is a Telegram client. To use the app, sign in with any Telegram account:
1. Tap "Start Messaging" (in Uzbek: "Xabar almashishni boshlash").
2. Enter a phone number registered in Telegram.
3. Enter the login code sent by Telegram (via the Telegram app or SMS).
You can use your own Telegram account. No other credentials are required.
```

### 6.3 Ads
- **No, my app does not contain ads**

### 6.4 Advertising ID
- "Does your app use advertising ID?" → **No**
  (Play versiyasida `AD_ID` ruxsati olib tashlangan.)

### 6.5 Content rating (IARC anketa)
- Email: ⟨EGASIDAN SO'RANG⟩
- Category: **All Other App Types** yoki **Communication** (qaysi biri taklif qilinsa)
- Javoblar:
  - Violence / Sexuality / Profanity / Drugs / Alcohol / Tobacco / Gambling / Horror — hammasi **No**
  - Does the app allow users to interact or exchange content with each other? → **Yes**
  - Does the app share the user's current physical location with other users? → **Yes** (foydalanuvchi o'zi joylashuv yuborishi mumkin)
  - Does the app allow users to purchase digital goods? → **No**
  - Does the app contain or allow access to unrestricted web content / web browser? → **Yes** (havolalar ilova ichidagi brauzerda ochiladi)
  - Is the app a news app? → **No**
  - Boshqa savollar → **No**
- Kutilgan natija: **Teen / 12+ / USK 12** atrofida — qanday chiqsa ham qabul qiling, **Submit**.

### 6.6 Target audience and content
- Target age groups: **16–17** va **18 and over**. 13 yoshdan kichiklarni **tanlamang**.
- "Could your store listing unintentionally appeal to children?" → **No**

### 6.7 News apps
- **No**

### 6.8 COVID-19 contact tracing and status apps
- **My app is not a publicly available COVID-19 contact tracing or status app**

### 6.9 Government apps
- **No**

### 6.10 Financial features
- **My app doesn't provide any financial features**

### 6.11 Health apps
- **My app does not have any health features**

### 6.12 Data safety
**Data collection and security:**
- Does your app collect or share any of the required user data types? → **Yes**
- Is all of the user data collected by your app encrypted in transit? → **Yes**
- Do you provide a way for users to request that their data is deleted? → **Yes** (URL: `https://my.telegram.org/delete`)

**Data types** — quyidagilarni belgilang. Har biri uchun javoblar:

| Data type | Collected | Shared | Ephemeral | Required / Optional | Purposes |
|---|---|---|---|---|---|
| Personal info → **Name** | ✅ | ❌ | No | Required | App functionality, Account management |
| Personal info → **Phone number** | ✅ | ❌ | No | Required | App functionality, Account management |
| Personal info → **User IDs** | ✅ | ❌ | No | Required | App functionality, Account management |
| Messages → **Other in-app messages** | ✅ | ✅ | No | Required | App functionality |
| Photos and videos → **Photos**, **Videos** | ✅ | ❌ | No | Optional | App functionality |
| Audio → **Voice or sound recordings**, **Music files**, **Other audio files** | ✅ | ❌ | No | Optional | App functionality |
| Files and docs | ✅ | ❌ | No | Optional | App functionality |
| Contacts | ✅ | ❌ | No | Optional | App functionality |
| Location → **Approximate location** | ✅ | ✅ | No | Optional | App functionality |
| Location → **Precise location** | ✅ | ❌ | No | Optional | App functionality |
| Device or other IDs | ✅ | ❌ | No | Required | App functionality (push notifications) |

Izohlar (agent bilsin):
- "Collected" — ma'lumot Telegram serverlariga yuboriladi (xabar, media, kontakt sinxronlash, push-token).
- Messages **Shared = Yes** — foydalanuvchi "tarjima qilish"ni bosganda matn Google Tarjimon'ga yuboriladi.
- Approximate location **Shared = Yes** — ob-havo uchun tuman koordinatasi Open-Meteo xizmatiga yuboriladi.
- Analitika, reklama va kuzatuv **yo'q** — ularni belgilamang.

### 6.13 Account deletion
- "Does your app allow users to create an account?" → **Yes**
- Delete account URL: `https://my.telegram.org/delete`
- In-app: Settings → Privacy and Security → Delete my account

---

## 7. Ruxsatlar deklaratsiyasi (AAB yuklangandan keyin so'ralishi mumkin)

**Policy and programs → App content** yoki reliz paytida chiqadigan oynalarda:

### 7.1 Foreground service permissions
Har bir tur uchun quyidagi tavsif. **Video havola** so'ralsa: ⟨EGASIDAN SO'RANG — YouTube "Unlisted" video havolasi⟩

| Turi | Tavsif (inglizcha yozing) |
|---|---|
| **dataSync** | `Uploading and downloading files and media the user sends or receives in chats, and keeping messages in sync while the user is actively sending.` |
| **mediaPlayback** | `Playing voice messages, music and videos the user starts in chats, with playback controls in the notification.` |
| **camera** / **microphone** | `Voice and video calls and video messages started by the user.` |
| **mediaProjection** | `Screen sharing during a video call, started by the user.` |
| **location** | `Live location sharing in a chat for the period the user chooses.` |
| **connectedDevice** | `Connecting to a smartwatch or car display to show and reply to messages.` |

### 7.2 Full-screen intent
- Category: **Calling / Video calling** — `Shows incoming voice and video calls on the lock screen.`

### 7.3 Photo and video permissions (READ_MEDIA_IMAGES / READ_MEDIA_VIDEO)
- Core functionality: **Yes**
- Tavsif: `Users frequently attach and send photos and videos in chats, and browse their gallery by albums inside the app. This is core messaging functionality.`

### 7.4 Exact alarm (agar so'ralsa)
- `Prayer time reminders set by the user and deleting scheduled posts at the exact time the user chose.`

---

## 8. Reliz (Internal testing)

**Test and release → Testing → Internal testing**
1. **Testers** tab → **Create email list** → nomi `MilliyGram test` → egasining emaili ⟨EGASIDAN SO'RANG⟩ → **Save**.
2. **Releases** tab → **Create new release** → AAB faylni yuklang.
3. Release name: `12.10.3 (bXXXX)` — XXXX AAB fayl nomidagi raqam.
4. Release notes:
```
<uz-UZ>
Birinchi versiya: sharpa rejimi, chat qulfi, namoz vaqtlari va qibla, kanal adminlari uchun vositalar, oddiy rejim.
</uz-UZ>
<ru-RU>
Первая версия: режим призрака, замок чатов, время намаза и кибла, инструменты для админов, простой режим.
</ru-RU>
<en-US>
First release: ghost mode, chat lock, prayer times and Qibla, tools for channel admins, simple mode.
</en-US>
```
5. **Next** → ogohlantirishlarni o'qing (xato bo'lsa — to'xtab, egasiga ayting) → **Save** → **Start rollout to Internal testing**.
6. **Testers** tab → "How testers join your test" → **Copy link** → havolani egasiga bering.

---

## 9. Closed testing (yangi shaxsiy akkauntlar uchun majburiy)

Google qoidasi: Production'dan oldin **kamida 12 ta tester 14 kun uzluksiz** Closed testing'da bo'lishi kerak.
1. **Test and release → Testing → Closed testing → Create track** (yoki mavjud "Alpha").
2. Testers: Google Group yoki email ro'yxati ⟨EGASIDAN SO'RANG⟩.
3. Internal testing'dagi relizni **Promote release → Closed testing** qiling.
4. Countries/regions: **Uzbekistan** (+ egasi xohlagan davlatlar).
5. **Start rollout to Closed testing**.

---

## 10. Production (FAQAT EGASI RUXSAT BERSA)

14 kundan keyin: **Dashboard → Apply for production** → savollarga javob bering → egasi tasdiqlagach Production release yarating.
- Countries: **Uzbekistan** va egasi aytgan boshqa davlatlar.

---

## 11. Yakuniy tekshiruv ro'yxati

- [ ] Store listing: nom, qisqa va to'liq tavsif (uz + en), ikonka, feature graphic, ≥2 skrinshot
- [ ] Store settings: Communication, email, website
- [ ] Privacy policy URL ochiladi
- [ ] App access — kirish yo'riqnomasi
- [ ] Ads: No · Advertising ID: No
- [ ] Content rating — yuborilgan
- [ ] Target audience — 16+
- [ ] News, COVID, Government, Financial, Health — "yo'q"
- [ ] Data safety — jadval bo'yicha to'ldirilgan
- [ ] Account deletion URL
- [ ] Foreground service, full-screen intent, photo/video deklaratsiyalari (so'ralganlari)
- [ ] Internal testing relizi chiqarilgan, havola egasiga berilgan
- [ ] "Telegram" so'zi nomda, ikonkada va skrinshotlarda yo'q

## 12. Tez-tez uchraydigan rad etish sabablari va yechimi

| Play xabari | Yechim |
|---|---|
| Impersonation / "Telegram" brendi | Nom, ikonka, skrinshotlardan "Telegram" so'zi va logotipini olib tashlang. Tavsifda faqat "Telegram API asosidagi norasmiy klient" iborasi qoladi |
| Privacy policy link broken | URL ochilishini tekshiring; ochilmasa — egasiga ayting |
| App access — "couldn't log in" | Kirish yo'riqnomasini aniqroq yozing; egasidan sinov raqami so'rang |
| Data safety mismatch | 6.12 jadvaliga qaytib, belgilanganlarni solishtiring |
| Foreground service declaration missing | 7.1 bo'yicha tavsif va video havolasini qo'shing |
| Payments policy | Play versiyasida karta orqali xarid o'chirilgan; agar baribir savol bo'lsa — egasiga xabar bering |
