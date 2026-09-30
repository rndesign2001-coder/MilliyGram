# MilliyGram'ni Google Play'ga chiqarish — to'liq qo'llanma

Bu papkadagi fayllar:

| Fayl | Nima uchun |
|---|---|
| `store-listing.md` | Do'kon sahifasi matnlari: nom, qisqa va to'liq tavsif, toifa, kontent reytingi va Data safety javoblari |
| `privacy-policy.md` | Maxfiylik siyosati (o'zbekcha + inglizcha). Play talab qiladigan URL: `https://github.com/rndesign2001-coder/MilliyGram/blob/claude/zen-hypatia-ngf4ae/docs/play-market/privacy-policy.md` |
| `icon-512.png` | Do'kon ikonkasi (512×512, 32-bit PNG) |
| `feature-graphic-1024x500.png` | Feature graphic (1024×500) |

---

## 0. Oldindan tayyorlab qo'yish (bir marta)

1. **Google Play Developer akkaunti** — play.google.com/console, bir martalik to'lov 25$. Shaxsni tasdiqlash (pasport) va telefon raqamini tasdiqlash so'raladi.
2. **Telegram API ma'lumotlari** — my.telegram.org → API development tools:
   - `App title` ni **MilliyGram** qiling, `Short name` ni esa `milliygram` qiling. Faol seanslarda boshqa Telegram ilovalarida ko'rinadigan ilova nomi shu yerdan olinadi. Hozir u yerda "PostBot" yozilgan, shuning uchun seanslarda "PostBot 12.10.3" chiqyapti.
   - MilliyGram'ning o'zida bu seanslar allaqachon **MilliyGram** nomi va logotipi bilan ko'rsatiladi. Qurilma ham aniq nomlanadi, masalan "Samsung Galaxy A56 5G".
   - `API_ID` va `API_HASH` GitHub Secrets'da turibdi. Ularni hech kimga bermang.
3. **Email** — Play'da ko'rinadigan ishlab chiquvchi emaili. Foydalanuvchilar shu manzilga yozadi.

## 1. Imzo kaliti

**Tavsiya: "Use existing app signing key".** Bunda Play'dagi va APK'dagi ilova bir xil kalit bilan imzolanadi. Natijada APK orqali o'rnatganlar ham Play'dan yangilana oladi.

Play Console → ilova → **Test and release → Setup → App signing** → "Use existing app signing key from Java keystore" → Play ko'rsatgan PEPK buyrug'i bilan `TMessagesProj/config/release.keystore` dan kalitni eksport qilib yuklang. Buyruq keystore parolini so'raydi — uni faqat o'zingiz terasiz, hech qayerga yozmang.

Muqobil yo'l — alohida **upload key**. Bunda GitHub secrets'ga `UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS` va `UPLOAD_KEY_PASSWORD` qo'shiladi. ⚠️ Bu yo'lda Play va APK versiyalari bir-birining ustidan o'rnatilmaydi.

## 2. AAB faylini olish

1. GitHub → Actions → **"MilliyGram Google Play (AAB)"** → Run workflow (branch: `main`).
2. Build ~30 daqiqa davom etadi. Tugagach, run sahifasining pastidagi **Artifacts** bo'limidan `MilliyGram-play-<versiya>-b<raqam>.aab` ni yuklab oling. U zip ichida keladi — uni oching.
3. Play versiyasida Play qoidalariga zid ruxsatlar avtomatik olib tashlanadi: APK o'rnatish, `USE_EXACT_ALARM` va fon joylashuvi.
4. versionCode har build'da avtomatik oshadi. AAB raqamlari APK raqamlaridan 1000 ta yuqori, shuning uchun Play hech qachon "versionCode already used" demaydi.

## 3. Play Console — ilova yaratish

**Create app** tugmasini bosing va quyidagilarni tanlang:
- App name: `MilliyGram — milliy messenjer` (30 belgigacha)
- Default language: **O'zbek (uz)**
- App or game: **App**
- Free or paid: **Free**
- Deklaratsiyalar: Developer Program Policies va US export laws — ✅

## 4. Do'kon sahifasi (Grow → Store presence → Main store listing)

| Maydon | Qiymat |
|---|---|
| App name | `store-listing.md` → "Ilova nomi" |
| Short description (80) | `store-listing.md` → "Qisqa tavsif" |
| Full description (4000) | `store-listing.md` → "To'liq tavsif" |
| App icon | `icon-512.png` |
| Feature graphic | `feature-graphic-1024x500.png` |
| Phone screenshots | **2–8 ta**, 9:16, har tomoni 320–3840 px. Tavsiya etiladi: chatlar ro'yxati, qibla, namoz vaqti, kanal profilidagi tezkor tugmalar, sozlamalar |
| Category | Communication |
| Contact | email + (ixtiyoriy) website: `https://t.me/MilliyGramm` |

⚠️ Skrinshotlarda "Telegram" logotipi, boshqa odamlarning shaxsiy chatlari va telefon raqamlari ko'rinmasin — test akkauntdan oling.

## 5. App content (Policy → App content) — hammasi to'ldirilishi shart

| Bo'lim | Javob |
|---|---|
| **Privacy policy** | Yuqoridagi `privacy-policy.md` URL'i |
| **App access** | "All or some functionality is restricted" → test uchun telefon raqami bilan kirish. Ko'rib chiquvchilarga yo'riqnoma bering: "Login with any Telegram account via phone number + SMS/Telegram code". Iloji bo'lsa, alohida test raqam bering |
| **Ads** | No, my app does not contain ads |
| **Content rating** | Anketa: Category — *Communication / Social*. User interaction — **Yes**. Shares location — **Yes**. Digital purchases — **No**. Zo'ravonlik/qimor/giyohvandlik — **No**. Natija odatda **12+ / Teen** chiqadi |
| **Target audience** | **13–15, 16–17, 18+** (13 yoshdan kichiklar — yo'q). "Appeals to children" — **No** |
| **News app** | No |
| **Government app** | No |
| **Financial features** | "My app doesn't provide any financial features" |
| **Health** | No |
| **Data safety** | Quyidagi 6-bo'limga qarang |
| **Account deletion** | Ilovada akkaunt yaratiladi → URL: `https://my.telegram.org/delete`. Ilova ichida: Sozlamalar → Maxfiylik → "Akkauntni o'chirish" |

## 6. Data safety — javoblar

- Does your app collect or share user data? — **Yes**. Xabarlar va telefon raqami Telegram serverlariga yuboriladi.
- Is all data encrypted in transit? — **Yes** (MTProto).
- Can users request data deletion? — **Yes** (`https://my.telegram.org/delete`).

Belgilanadigan ma'lumot turlari. Hammasining maqsadi — **App functionality**, hammasi "Collected", "Shared" esa **yo'q**:

| Tur | Nega |
|---|---|
| Personal info → Name, Phone number, User IDs | Telegram akkaunti |
| Messages → Other in-app messages | Chatlar |
| Photos and videos, Audio (voice), Files and docs | Media yuborish |
| Contacts | Kontaktlarni sinxronlash (ixtiyoriy) |
| Location → Approximate / Precise | Joylashuv yuborish, namoz va qibla (ixtiyoriy) |
| App info and performance → Crash logs | Faqat foydalanuvchi o'zi yuborsa |

## 7. Ruxsatlar deklaratsiyasi (Policy → App content → Sensitive permissions)

Play AAB'ni yuklaganingizdan keyin quyidagilarni so'rashi mumkin:

| Ruxsat | Nima yozish kerak |
|---|---|
| **Foreground service** (camera, microphone, mediaPlayback, mediaProjection, dataSync, location) | "Voice/video calls (camera, microphone, screen sharing), music/voice playback, sending large files, live location sharing — standard messenger features started by the user." Har biri uchun 30 soniyalik video havola (YouTube unlisted) so'raladi: qo'ng'iroq qilish, ekran ulashish, jonli joylashuv yuborish |
| **USE_FULL_SCREEN_INTENT** | "Incoming voice/video calls" — kiruvchi qo'ng'iroqlar (ruxsat etilgan holat) |
| **READ_MEDIA_IMAGES / VIDEO** | "Core functionality: users send photos and videos in chats; gallery picker with albums" |
| **SCHEDULE_EXACT_ALARM** | Namoz vaqti eslatmasi va rejalashtirilgan post o'chirish (1/24) |
| **READ_CONTACTS** | Telegram kontaktlarini topish (foydalanuvchi ruxsat bersa) |

## 8. Sinov va chiqarish

1. **Test and release → Testing → Internal testing** → Create new release → `.aab` ni yuklang → Release notes (o'zbekcha) → Save → Review → **Start rollout**. Testerlar ro'yxatiga o'z emailingizni qo'shing va ilovani Play'dan o'rnatib sinang.
2. **Yangi shaxsiy akkaunt** bo'lsa, Play qoidasi bor: Production'dan oldin **Closed testing**da **kamida 12 tester 14 kun uzluksiz** qatnashishi kerak. Testerlarni `@MilliyGramChat` dan yig'ish qulay: Google Group yarating, havolasini chatga tashlang.
3. 14 kun o'tgach **Apply for production** → savollarga javob bering → **Production** relizini yarating → ko'rib chiqish odatda 1–7 kun davom etadi.
4. Mamlakatlar: Production → Countries/regions → **O'zbekiston** va boshqa kerakli davlatlar.

## 9. Yangilanishlar

- Har yangi versiya uchun Actions'dan yangi AAB oling → Production (yoki Testing) → Create new release → yuklang.
- Ilova Google Play'ning **In-App Updates** mexanizmidan foydalanadi. Foydalanuvchi ilovani ochganda yangilanish fonda yuklanadi va "O'rnatish" so'raladi.
- Muhim yangilanish uchun relizga **prioritet 4–5** bering (Play Developer API → `inAppUpdatePriority`). Shunda to'liq ekranli majburiy oyna chiqadi.
- Yangiliklarni `@MilliyGramm` kanalida e'lon qiling. Kanal havolasi ilovada: Sozlamalar → MilliyGram sozlamalari → "MilliyGram hamjamiyati".

## 10. Rad etilmaslik uchun tekshiruv ro'yxati

- [ ] Nom, ikonka, skrinshot va tavsifda "Telegram" logotipi yo'q; "Telegram" so'zi faqat "Telegram API asosidagi norasmiy klient" iborasida ishlatilgan
- [ ] Maxfiylik siyosati URL'i ochiladi (repo **public** bo'lishi kerak, aks holda siyosatni GitHub Pages yoki boshqa ochiq sahifaga joylang)
- [ ] App access bo'limida kirish yo'riqnomasi yozilgan
- [ ] Data safety va Content rating to'ldirilgan
- [ ] Account deletion URL'i ko'rsatilgan
- [ ] Foreground service va full-screen intent deklaratsiyalari (videolar bilan) to'ldirilgan
- [ ] Release notes o'zbekcha yozilgan
- [ ] API app title my.telegram.org'da "MilliyGram"

## Parol va maxfiy ma'lumotlar

Keystore paroli, API_HASH va tokenlarni **hech qachon** kodga, issue'ga yoki chatga yozmang — ular faqat GitHub → Settings → Secrets and variables → Actions'da turadi. Parolni almashtirish uchun "Imzo kaliti parolini almashtirish" workflow'idan foydalaning. Keyin `NEW_KEYSTORE_PASSWORD` va `GH_SECRETS_TOKEN` secret'larini o'chirib tashlang.
