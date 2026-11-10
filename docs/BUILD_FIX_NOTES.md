# MELOVA Android — Kotlin build tuzatishi

## Sababi
`MelovaViewModel.kt` da `var ... by mutableStateOf(...)` ishlatilgan,
lekin `androidx.compose.runtime.getValue` va `setValue` importlari yo‘q edi.

Bu ikkita import yetishmaganligi tufayli `getValue/setValue` xatolari,
undan keyin esa Kotlin tipni aniqlay olmaydigan ketma-ket `Unresolved reference`
xatolari paydo bo‘lgan.

## Tuzatish
`app/src/main/java/uz/melova/app/model/MelovaViewModel.kt` fayliga quyidagilar qo‘shildi:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
```

`tests/verify_project.py` fayliga bu importlar mavjudligini tekshiruvchi test qo‘shildi.

## Tekshirish
Local statik tekshiruv: `python3 tests/verify_project.py`
To‘liq Android build: GitHub Actions → `MELOVA - Android APK`.

Bu muhitda Gradle/Android SDK to‘liq mavjud emas; APK build natijasi tasdiqlanmagan.
