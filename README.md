# JQWave

**JQWave** is an Android app that reminds you about selected Jewish calendar moments—**Rosh Chodesh**, **Sfirat HaOmer**, and **Shabbat** (candle lighting and havdalah)—using **location-aware halachic times** (via [KosherJava Zmanim](https://github.com/KosherJava/zmanim)). You choose *when* each reminder fires relative to clock time or astronomical anchors (sunrise, sunset, nightfall).

---

## Why you might want it

- **You want quiet, reliable nudges** for recurring observances without digging through a full calendar app every day.
- **Times depend on where you are**: sunset, tzeit, and related anchors change with latitude/longitude. JQWave uses your coarse location (refreshed about once per day when permitted) so notifications stay in the right ballpark as you travel.
- **You control the schedule**: For each event you can add multiple rules—for example, Shabbat **start** (candles) vs **end** (havdalah)—with offsets *before* or *after* the chosen anchor.
- **Israel vs. diaspora**: A toggle applies Israel-oriented holiday handling when that matches how you keep the calendar.
- **Hebrew or English UI**: Use the language control in the app bar to switch locales; the app restarts to apply it.

---

## How to use the app

### First launch

1. **Notifications** — On Android 13+, grant notification permission when prompted so reminders can appear.
2. **Location** — Allow **approximate (coarse) location** so the app can align zmanim with your area. If you deny it, you can still enter coordinates manually under **Location & calendar**.

### Main screen

- Each **event** (Rosh Chodesh, Sfirat HaOmer, Shabbat) is a card. Use the **switch** to enable or disable reminders for that event.
- Tap the **card header** (title row) to **expand** or **collapse** details.
- When expanded and enabled, you can:
  - Adjust **when** each reminder fires: **Time** (wall clock), **Sunrise**, **Sunset**, or (for Shabbat end) **Nightfall** / **Time** as applicable.
  - Set **before/after** offsets for astronomical anchors.
  - For **Shabbat**, pick **Start (candles)** or **End (havdalah)** per rule, and add more rules with the **+** control.
  - Tap **Test** to fire a sample notification for that event (useful after changing rules).

### Location & calendar

- Expand **Location & calendar** to edit **latitude**, **longitude**, and **time zone ID**, then tap **Apply location**.
- Toggle **Use Israel holiday rules** if you want the Israel-oriented rule set.
- With location permission, values are **refreshed automatically** about every 24 hours; the UI explains this on the card.

### Notifications

- Shabbat and other alerts use a dedicated notification channel (**Jewish calendar events**). You can tune sound and importance in system settings.
- Where supported, notifications may include a **Share** action to pass the reminder text to another app.

---

## Building and running

**Prerequisites**

- [Android Studio](https://developer.android.com/studio) (or Android SDK + compatible JDK)
- **JDK 17**
- **Min SDK 26**, **Target/compile SDK 35** (see `app/build.gradle.kts`)

**From the project root**

```bash
./gradlew :app:assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/` on a device or emulator, or run **Run ▶** from Android Studio with the `app` module selected.

---

## Tech stack (short)

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **Room** for local configuration storage
- **WorkManager** + Play Services **location** for periodic location refresh
- **KosherJava Zmanim** for time calculations

---

## License

Add a `LICENSE` file to the repository if you plan to distribute the app; none is included in this README by default.
