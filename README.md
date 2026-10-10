# Epoch

Epoch is an Android application built for `com.example.epoch` supporting Android devices running API level 24 and above (targeting API level 37). The application utilizes AndroidX and Material libraries for its UI, ViewPager2 for page-based navigation, Coil for image loading, Retrofit and OkHttp for HTTP networking and JSON parsing, and Kotlin Coroutines for asynchronous execution.

## Tech Stack

- **Platform:** Android (Min SDK: 24, Target SDK: 37, Compile SDK: 37)
- **Language / Runtime:** Java 11 / Kotlin
- **Build System:** Gradle (Kotlin DSL)
- **Libraries & Frameworks:**
  - AndroidX (Activity KTX, AppCompat, ConstraintLayout, Core KTX)
  - Google Material Components
  - ViewPager2 (`1.1.0`)
  - Coil (`2.6.0`)
  - Retrofit (`2.11.0`) & Gson Converter (`2.11.0`)
  - OkHttp (`4.12.0`)
  - Kotlinx Coroutines Android (`1.8.1`)
- **Testing:**
  - JUnit
  - AndroidX Test JUnit
  - Espresso Core

## Setup

### Prerequisites
- JDK 11
- Android SDK (API 37)

### Build Commands

To build the project:

```bash
./gradlew build
```

To compile and assemble the debug APK:

```bash
./gradlew assembleDebug
```

To run unit tests:

```bash
./gradlew test
```

To run instrumented Android tests on an attached device or emulator:

```bash
./gradlew connectedAndroidTest
```