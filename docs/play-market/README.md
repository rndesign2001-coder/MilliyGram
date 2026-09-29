# MilliyGram'ni Google Play'ga chiqarish — qadamma-qadam

## 1. Imzo kaliti
Play'ga yuklanadigan `.aab` fayl imzolangan bo'lishi kerak. Ikkita yo'l bor:

**A) Mavjud kalit bilan (tavsiya etiladi).** Hozirgi APK'lar imzolangan kalit Play'da ham ishlatiladi. Shunda Play'dan va APK'dan o'rnatilgan ilovalar bir-birining ustidan yangilanadi.
Play Console → Setup → App signing → "Use existing app signing key" → PEPK vositasi bilan kalitni eksport qiling.

**B) Alohida "upload key".** Play o'z kalitini yaratadi, siz esa yuklash uchun alohida kalit ishlatasiz.
Bu holda GitHub secret'lariga qo'shing: `UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`.
⚠️ Bu yo'lda Play versiyasi va APK versiyasi bir-birining ustidan o'rnatilmaydi (imzo boshqa).

### Kalit parolini almashtirish
Parolni hech qachon kodga yoki repoga yozmang — faqat GitHub Secrets'ga.
1. Settings → Secrets and variables → Actions → **New repository secret**:
   - `NEW_KEYSTORE_PASSWORD` = yangi parol
   - `GH_SECRETS_TOKEN` = GitHub → Settings → Developer settings → Fine-grained tokens → shu repo, **Secrets: Read and write**
2. Actions → **"Imzo kaliti parolini almashtirish"** → Run workflow.
3. Muvaffaqiyatli tugagach `NEW_KEYSTORE_PASSWORD` va `GH_SECRETS_TOKEN`'ni o'chirib tashlang.

Parol almashsa ham imzo o'zgarmaydi — foydalanuvchilar odatdagidek yangilaydi.

## 2. AAB yig'ish
Actions → **"MilliyGram Google Play (AAB)"** → Run workflow → tugagach Artifacts'dan `MilliyGram-play-….aab`ni yuklab oling.
Play versiyasida Play qoidalariga zid ruxsatlar (APK o'rnatish, USE_EXACT_ALARM, fon joylashuvi) avtomatik olib tashlanadi. Ilova ichidagi "yangi versiya" oynasi ham Play'dan o'rnatilganda o'chadi — yangilanishni Play o'zi beradi.

## 3. Play Console
1. play.google.com/console → Create app → nom: **MilliyGram**, til: o'zbek, turi: App, bepul.
2. Store listing: `store-listing.md` dagi matnlar, ikonka 512×512, feature graphic 1024×500, kamida 2 ta skrinshot.
3. App content: maxfiylik siyosati URL (`privacy-policy.md`), Data safety, Content rating, Target audience (13+), Ads: Yo'q.
4. Testing → Internal testing → yangi reliz → `.aab` ni yuklang → testerlar bilan sinang.
5. Yangi shaxsiy akkauntlar uchun Play talabi: **20 ta tester 14 kun davomida** Closed testing'da sinashi kerak, keyin Production.

## 4. Diqqat
- Nomda/ikonkada "Telegram" so'zi va logotipini ishlatmang.
- Play har bir yangi yuklashda versionCode'ning oshishini talab qiladi — CI buni avtomatik qiladi (APP_VERSION_CODE + build raqami).
- Telegram API ID/HASH (my.telegram.org) sizniki bo'lishi kerak.
