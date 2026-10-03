# Treenipäiväkirja (Android)

Kotlin + Jetpack Compose + Room. Toimii offline, data tallentuu puhelimeen.

## Kääntäminen
1. Pura zip ja avaa kansio Android Studiossa (File → Open).
2. Odota Gradle Sync (lataa riippuvuudet ensimmäisellä kerralla).
3. Kytke puhelin USB:llä (kehittäjäasetukset + USB-vianetsintä päälle) ja paina ▶ Run.

APK ilman puhelinta: Build → Build App Bundle(s) / APK(s) → Build APK(s)
→ `app/build/outputs/apk/debug/app-debug.apk`

## Rakenne
- `data/Database.kt` – Room-taulut (liikkeet, sarjat) ja kyselyt
- `WorkoutViewModel.kt` – tila, ryhmittely päivittäin, 1RM-arvio (Epley)
- `ui/DayScreen.kt` – päivänäkymä, viikkonauha, lisäysdialogi
- `ui/HistoryScreen.kt` + `ui/LineChart.kt` – kehityskäyrät
- `ui/Theme.kt` – värit ja muotoilu

## Tekijänoikeus
© 2026 Definitos. Kaikki oikeudet pidätetään. Ks. [LICENSE](LICENSE).
