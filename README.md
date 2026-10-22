# 📜 Epoch – On This Day in History

An Android app that shows what happened **on today's date** throughout history. Swipe vertically through full-screen cards – each one has the year, the event, an image and a button to read more on Wikipedia.

Data comes live from the **Wikipedia "On this day" REST API**.

## ✨ Features

- Fetches today's historical events automatically on launch
- Vertical, reels-style swiping between events (ViewPager2)
- Event image loaded with Coil (thumbnail or original image)
- Year, headline and a short Wikipedia extract on each card
- "Read on Wikipedia" button opens the full article in the browser
- Edge-to-edge layout with light and dark themes

## 🔧 How it works

```
MainActivity
  └─ Retrofit → GET https://en.wikipedia.org/api/rest_v1/feed/onthisday/events/{MM}/{DD}
        └─ HistoryResponse(events: List<HistoricalEvent>)
              └─ FactAdapter (ViewPager2) → item_fact.xml card per event
```

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Builds the Retrofit/OkHttp client, gets today's date, loads events in a coroutine |
| `HistoricalEvent.kt` | Data classes matching the Wikipedia JSON (`year`, `text`, `pages`, thumbnails, URLs) |
| `FactAdapter.kt` | Binds each event to a card, loads images with Coil, handles the Wikipedia button |

## 🛠️ Tech stack

- **Kotlin**, Android SDK (min 24, target 37)
- AndroidX, Material Components, ViewPager2
- **Retrofit** + Gson converter, **OkHttp**
- **Coil** for image loading
- Kotlin **Coroutines** (`lifecycleScope`)
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)

## 🚀 Build & run

Requirements: Android Studio (or JDK 11+ and the Android SDK 37).

```bash
git clone https://github.com/Adarshmilan/Epoch.git
cd Epoch
./gradlew assembleDebug          # APK → app/build/outputs/apk/debug/
./gradlew installDebug           # install on a connected device/emulator
```

Or open the folder in Android Studio and press **Run**. The app needs an internet connection.
