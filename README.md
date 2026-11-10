# MELOVA Android 1.0.2 — avtomatik Supabase ulanishi (Kotlin / Jetpack Compose / Media3)

**1.0.2 o'zgarishi:** MELOVA bitta umumiy Supabase loyihasiga ulanadi. GitHub Secrets'da URL va publishable/anon key **bir marta** sozlanadi, APK qayta build qilinadi. Bu APKni o'rnatadigan odamlar hech qachon server URL/key kiritmaydi. Oldin telefonda qo'lda sozlangan boshqa URL yangi buildni almashtirmaydi. Server almashtirilsa eski login sessiyasi xavfsizlik uchun tozalanadi. Profil menyusida server sozlash faqat demo/manual test buildlarida ko'rinadi.

**Eslatma:** Ushbu ZIPda real Supabase URL yoki key yo'q, chunki ular hali berilmagan. Avtomatik ulanish ishlashi uchun quyidagi 2 ta GitHub Secretni kiritish va GitHub Actions'dan yangi APK olish **majburiy**. Oldin o'rnatilgan APK o'z-o'zidan yangilanmaydi.


Bu Android loyiha **MELOVA 1.1 Professional Player** web-versiyasining funksional native Android portidir. ZIP ichida Android Studio loyihasi, GitHub Actions APK workflow, dastlabki 3 ta original demo MP3 va dastlabki web-versiya (faqat ma'lumot uchun) bor. **Bu hali Google Play uchun sinovdan o'tgan release emas.**

## 1. Nima ishlaydi?

- Native Android sahifalari: bosh sahifa, qidiruv / janr filtrlari, kutubxona, sevimlilar, tinglash tarixi, pleylistlar, profil, audio yuklash, moderator.
- **Media3 ExoPlayer + MediaSessionService**: fon rejimida tinglash, lock screen/media tugmalari, play/pause, seek, oldingi/keyingi, shuffle, repeat, ijro tezligi va navbat.
- Supabase REST: email/parol bilan kirish, ro'yxatdan o'tish, Google OAuth (tashqi brauzer + PKCE), tokenni yangilash, parolni tiklash.
- Supabase mavjud jadval/bucketlari (`tracks`, `profiles`, `admins`, `favorites`, `playlists`, `playlist_tracks`, `listening_history`, `reports`, `melova-audio`) bilan ishlaydi. Admin huquqi **serverdagi RLS** asosida tekshiriladi, ilova ichida oddiy switch yo'q.
- Musiqa yuborish: MP3/M4A/WAV/OGG, 20 MiB cheklov, tekshiruvga yuborish uchun mualliflik huquqi tasdig'i. Moderator tasdiqlashi/rad etishi va shikoyatlar.
- Sessiya maxsus Android Keystore AES-GCM orqali saqlanadi. Ilovaga `service_role` yoki secret key kiritish bloklangan.
- Supabase bo'lmasa ham MELOVA web-demo'dan olingan **3 ta real audio** telefonning o'zidan ijro bo'ladi.

**Hozir kiritilmagan yoki sinov talab qiladigan qismlar:** asl 1.1 Web UI'ning 1:1 pixel-perfect ko'rinishi, internetdan avtomatik cover-art va avatar yuklash, trek lyrics ko'rinishi, internet bo'lmaganda cloud qo'shiqlarni offline tinglash, audio yuklashning uzilishdan davom ettiriladigan TUS varianti, to'liq paging, Google Play release signing va jonli Supabase/Google OAuth qurilma testi. Ijro navbati bitta bosishda eng ko'pi 30 trekni tayyorlaydi. Bu xususiyatlar tayyor deb da'vo qilinmaydi.

## 2. Eng oson yo'l: GitHub orqali APK

1. ZIP faylni kompyuterda **Extract** qiling.
2. GitHub'da yangi **`melova-android`** nomli repository yarating, **Private** qilib saqlash tavsiya qilinadi. (Eski VARAQ repoga aralashtirmang.)
3. Repository'da `Add file → Upload files` orqali arxiv ichidagi fayl va papkalarni yuklang. **`.github` yashirin papka** sifatida ko'rinishi mumkin: `.github/workflows/android-apk.yml` aynan shu yo'lda joylashishi zarur. GitHub web interfeysi papkalarni yuklay olmasa, `git` yoki GitHub Desktop ishlating. ZIP'ni bir fayl sifatida yuklash yetmaydi.
4. **Majburiy:** `Settings → Secrets and variables → Actions → New repository secret` orqali eski web ilovangiz ishlatgan qiymatlarni kiriting:

   | Secret nomi | Qiymat |
   |---|---|
   | `MELOVA_SUPABASE_URL` | Masalan: `https://xxxx.supabase.co` |
   | `MELOVA_SUPABASE_PUBLISHABLE_KEY` | Project publishable key (`sb_publishable_...`) yoki legacy `anon` |

   **Muhim:** `service_role`, `sb_secret_...`, database password, Google OAuth client secret bu yerda kerak emas. Publishable key Android APK'da ochiq ko'rinishi mumkin; Supabase RLS asosiy xavfsizlikdir.

5. `Actions → MELOVA - Android APK → Run workflow` ni bosing.
6. **Yashil Success** chiqsa, ishga tushirilgan run sahifasidagi `Artifacts → MELOVA-Android-debug-APK` ni bosing.
7. ZIP ichidagi `app-debug.apk` ni Android telefoningizga o'rnating.

**1.0.2 da secrets kiritilmasa GitHub Actions qasddan qizil FAIL bo'ladi** va qaysi sozlama yetishmaganini ko'rsatadi. Bu foydalanuvchilarga xatolik bilan demo APK tarqatib yubormaslik uchun. Ikkala secret ham to'g'ri kiritilsa, bir xil sozlangan APK barcha telefonlarda avtomatik ulanadi.

**Bir martalik sozlash:** Project URL va **publishable/anon key** — ilovada ko'rinadigan, ommaviy qiymatlar; asl xavfsizlik Supabase RLS siyosatlari va Auth orqali ta'minlanadi. `service_role`, `sb_secret_` yoki database parolini bu loyiha ichiga kiritmang.

**Agar oldingi MELOVA APK telefoningizda bo'lsa:** yangi APKni uning ustiga yangilab o'rnatishingiz mumkin (agar oldingi debug imzosi mos kelsa). Imzo mos kelmasa, yangisini o'rnatishdan oldin lokal ma'lumotlaringizni saqlab qo'ying.

**Oldingi sozlama bo'yicha:** ZIP ichidagi `docs/ORIGINAL-MELOVA-1.1.html` faylida URL/key maydonlari bo'sh — bunda sizning haqiqiy Supabase hisobingizdagi sozlamalar saqlanmagan. Qiymatlarni Supabase Dashboard'dan oling.

## 3. Mavjud Melova web ilovasiga ulash (ma'lumotlarni o'chirmang)

Siz allaqachon Melova web ilovasini Supabase'ga ulagan bo'lsangiz **YANGI SUPABASE PROJECT OCHMANG, `setup.sql` NI QAYTA RUN QILMANG.** Bu app avvalgi `profiles`, `tracks`, `favorites`, `admins` va qolgan jadvallardan foydalanadi. Sizning eski foydalanuvchilaringiz, musiqalaringiz va pleylistlaringiz saqlanadi.

Eski `index.html` ichida `MELOVA_CONFIG` ning `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` qiymatlarini topish mumkin. Faqat public keyni oling.

Supabase Dashboard → **Authentication → URL Configuration → Redirect URLs** ga `uzmelova://auth/callback` qo'shing. Google orqali login uchun **Authentication → Providers → Google** ham sozlangan bo'lishi kerak. Google login uchun ilova **tashqi brauzerni** ochadi va callback orqali qaytadi. Parolni tiklash linki ham shu callback'dan foydalanadi. Web login uchun Netlify URL ni Redirect URLs ro'yxatidan o'chirmang.

Agar hali Supabase bazangiz bo'lmasa, yangi project yarating va faqat unda `supabase/setup_1_0_NEW_ONLY.sql` dan foydalaning. Bu SQL faqat **yangi bo'sh project** uchun. GitHub ichidagi `supabase/` papkasi qurilmaga deploy qilinmaydi.

## 4. Native ilovaning tuzilishi

```text
MELOVA-Android/
├── .github/workflows/android-apk.yml    # Build va artifact
├── app/build.gradle.kts                  # Android, Compose, Media3
├── app/src/main/AndroidManifest.xml
├── app/src/main/java/uz/melova/app/
│   ├── MainActivity.kt                  # MediaController va OAuth deep link
│   ├── data/MelovaApi.kt                # Supabase Auth, REST, Storage
│   ├── data/Vault.kt                    # Keystore token saqlash
│   ├── model/Models.kt
│   ├── model/MelovaViewModel.kt         # UI state + player commands
│   ├── playback/MelovaPlaybackService.kt
│   └── ui/                              # Native Compose ekranlari
├── app/src/main/res/raw/demo_*.mp3
├── supabase/setup_1_0_NEW_ONLY.sql
├── docs/ORIGINAL-MELOVA-1.1.html        # Original web reference
└── tests/verify_project.py
```

**Versiyalar:** JDK 17, AGP 8.11.1, Kotlin 2.2.21, Gradle 8.14.3, Android compile SDK 36, min SDK 26, Compose BOM 2025.08.00, Media3 1.11.1.

## 5. GitHub build muvaffaqiyatli chiqqach sinang

1. Internet o'chirilgan holda demo treklar ijrosini, next/previous, ekran bloklanganida ijroni tekshiring.
2. Real Supabase konfiguratsiyasi bilan login/ro'yxatdan o'tishni sinang.
3. Ikkita Android qurilmada bitta akkaunt sevimlilari, tarix va pleylistlari sinxronlashayotganini tekshiring.
4. Private trek yuklang, faqat egasi ko'rishi va tinglashi kerak.
5. Tekshiruvga yuboring; oddiy user o'z trekini to'g'ridan-to'g'ri `approved` qila olmasligi kerak. Moderator tasdiqlagach umumiy katalogda ko'rinishi kerak.
6. Google login va parolni tiklashni Redirect URL sozlangan holda alohida sinang.
7. 100+ trek bilan qidiruv/scroll, 20MB audio upload, internet uzilishi va telefonni qayta ishga tushirish holatlarini sinang.

**Sifat bo'yicha halol holat:** manba tuzilmasi va demo MP3'lar statik tekshirildi. Bu muhitda Android SDK, Gradle va Google Maven mavjud emasligi sababli **`assembleDebug` bu yerda bajarilmadi**; haqiqiy kompilyatsiya va qurilma sinovi GitHub Actions/telefonda zarur. Hozircha “100% buglarsiz” deb da'vo qilmaymiz.

## 6. Ommaviy chiqarishdan oldin

Debug APK faqat sinov uchun. Google Play uchun release signing, AAB, Play Console, maxfiylik siyosati, kontent moderatsiyasi va mualliflik huquqlari, Google login/recovery audit, rate limit / monitoring va foydalanuvchi hajmiga mos Supabase tarifini rejalashtiring.
