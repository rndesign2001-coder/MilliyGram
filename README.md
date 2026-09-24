# MilliyGram

**MilliyGram** — milliy dizayndagi, o'zbek foydalanuvchilari uchun qo'shimcha imkoniyatlarga ega **norasmiy Telegram klienti** (Android).

> ⚠️ MilliyGram Telegram FZ-LLC tomonidan ishlab chiqilmagan va rasmiy Telegram ilovasi emas. U Telegram'ning ochiq manba kodi ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)) asosida qurilgan va Telegram API'dan foydalanadi.

## Yuklab olish

Eng so'nggi APK: **[Releases](../../releases)** bo'limida.

## Imkoniyatlar (reja)

- Milliy dizayn: feruza va oltin rangli mavzu, naqshli ikonka, bayram kunlari maxsus bezak
- Chatlarni PIN bilan qulflash
- 10 tagacha akkaunt
- Trafik tejash rejimi va proksi menejeri
- Stiker va GIF'ni galereyaga saqlash
- Kengaytirilgan profil (ID, akkaunt yoshi)
- Chat ichida sana bo'yicha sakrash, birinchi xabarga o'tish
- Xabarlarni tarjima qilish
- Oddiy rejim (katta shrift, soddalashtirilgan menyu)
- O'chirilgan xabar belgisi va tahrir vaqti (mazmun saqlanmaydi)
- Rasmdagi matnni nusxalash (OCR)
- Xabar eslatmalari
- Ovozli xabar tezligi 0.5x–3x
- Yuklab olish menejeri
- Spam so'z filtri
- Fokus rejimi
- Siqmasdan yuborish
- Sozlamalar zaxirasi

## Build

APK GitHub Actions orqali avtomatik yig'iladi (`.github/workflows/build.yml`). Kerakli secrets:

| Secret | Tavsif |
|---|---|
| `API_ID`, `API_HASH` | https://my.telegram.org dan olingan kalitlar |
| `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | APK imzolash kaliti |
| `GOOGLE_SERVICES_JSON` *(ixtiyoriy)* | Firebase (push bildirishnomalar) |

## Litsenziya

GNU GPL v2 yoki keyingi versiya — [LICENSE](LICENSE). Asl Telegram README: [README.telegram.md](README.telegram.md).
