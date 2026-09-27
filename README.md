# SmartSpend 💰

**SmartSpend** is a modern personal expense tracking Android application built with native **Kotlin** and **Jetpack Compose**. 

It automatically detects and extracts financial transactions from incoming banking SMS messages (debits, credits, balances, reference numbers) and stores them in a local **Room** database, enabling offline-first expense review, categorization, and financial insights.

## Tech Stack & Architecture
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material 3 (Fintech theme, edge-to-edge)
- **Local Database**: AndroidX Room (SQLite) with KSP and Kotlin Coroutines/Flow
- **Architecture**: Clean Architecture (Domain, Data, UI) & MVI/MVVM
- **Target SDK**: Android 15 (API 35), Min SDK: Android 8.0 (API 26)
- **SMS Engine**: Android `BroadcastReceiver` (`SMS_RECEIVED`) + `ContentResolver` ingestion
