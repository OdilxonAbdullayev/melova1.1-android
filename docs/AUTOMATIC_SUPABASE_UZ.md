# MELOVA – Barcha foydalanuvchilar uchun avtomatik ulanish

1. Supabase Dashboard'ga kiring va **eski MELOVA web ilovangiz ishlatayotgan** projectni tanlang.
2. Project Settings > Data API yoki API bo'limidan `Project URL` ni nusxalang.
3. API Keys > Publishable key'dan `sb_publishable_...` yoki legacy `anon` public keyni oling. `sb_secret_...` yoki `service_role` **emas**!
4. GitHub `melova-android` repo > Settings > Secrets and variables > Actions > New repository secret.
5. `MELOVA_SUPABASE_URL` va `MELOVA_SUPABASE_PUBLISHABLE_KEY` degan ikkita secret yarating. Nomlari katta harflar bilan aynan shunday.
6. Actions > MELOVA - Android APK > Run workflow.
7. Build Success bo'lsa, `MELOVA-Android-debug-APK` artifactni yuklab oling, ZIPdan `app-debug.apk` ni chiqaring.
8. Yangi APKni telefonga o'rnating va internet yoqilgan holatda dasturga kiring. **Profil'da Supabase ulanishi ko'rinmasa — APK oldindan sozlangan.**
9. Ikkinchi telefonda ham shu APKni sinang: hech qanday key kiritmasdan bir xil umumiy treklar ko'rinishi kerak.

**Muhim:** GitHub Secrets'ni kiritishning o'zi oldingi build yoki oldin o'rnatilgan APKni o'zgartirmaydi — `Run workflow` bilan qayta build va yangi APKni qayta o'rnatish shart.

**Xavfsizlik:** publishable/anon keyni APKdan olish mumkin — uni maxfiy kalit deb hisoblamang. RLS va backend xavfsizlik siyosatlarini qo'llang. Admin huquqi serverda belgilanadi.
