# MELOVA 1.1 — Professional Player

Ushbu yangilanish **faqat musiqa pleyeri** uchun. Mavjud musiqa katalogi, profil, sevimlilar, pleylistlar, Google Auth, Supabase Storage, SQL RLS va administrator paneli oldingidek qoladi. `setup.sql` ni qayta ishga tushirish **kerak emas**.

## Qo‘shilgan funksiyalar

- Katta, ikki ustunli professional pleyer (mobil qurilmada to‘liq ekran)
- Musiqaga mos gradient, aylanuvchi vinil va ijro animatsiyasi (reduced-motion qo‘llab-quvvatlanadi)
- Ijro navbati: trekni tanlash, navbatga qo‘shish, yuqoriga/pastga ko‘chirish, navbatdan olib tashlash
- Shuffle, repeat-off/repeat-all/repeat-one (1 belgisi bilan), yaxshilangan seek
- Ovoz balandligi va mute, ijro tezligi: 0.75× / 1× / 1.25× / 1.5× / 2×
- Mobil katta pleyer va navbat orasida o'tish
- Space = play/pause; ←/→ = 5 soniya; M = mute; Q = navbat; Esc = yopish
- Media Session API qo‘llab-quvvatlansa, OS/naushnik media tugmalari uchun boshqaruv

## Muhim: avval Supabase konfiguratsiyasini saqlang

Ushbu arxivdagi `index.html` original MELOVA 1.0 *shablonidan* yangilangan. Uning yuqori qismidagi:

```js
window.MELOVA_CONFIG={
  SUPABASE_URL:'',
  SUPABASE_PUBLISHABLE_KEY:''
};
```

qiymatlari bo‘sh. Siz internetga chiqarilgan `index.html` faylingizga Supabase URL va publishable/anon key yozgan bo‘lsangiz, **yangi faylni Netlify’ga qo‘yishdan oldin aynan shu ikkita qiymatni eski saytingizdan yangi faylga ko‘chiring**. Yangi faylni avval shunday holatda sinang, so‘ng eski faylning zaxirasini oling. `service_role`/secret kalitni HTML ichiga hech qachon kiritmang.

## Joylash

1. Avval eski `index.html` ni zaxiraga oling.
2. Yangi `index.html` ga Supabase Project URL va publishable key ni kiriting.
3. Local rejimda demo musiqalar orqali pleyerni sinang.
4. Netlify saytingizga yangilangan `index.html` ni yuklang / qayta deploy qiling.
5. Saytni telefonda ham sinang, login va musiqalar saqlanishini tekshiring.

## Nimalar kiritilmadi?

Musiqaga rasm yuklash, lyrics, audio formatni qayta kodlash va yangi backend jadvallari **kiritilmadi** — faqat professional pleyer yangilandi.

## Sinov chegarasi

Supabase integratsiyasini faqat haqiqiy loyiha hisobingiz bilan jonli sinash mumkin. Bu fayl Supabase’ga ulanish uchun avvalgi konfiguratsiyasini talab qiladi.
