# Minus

<p align="center">
  <img src="assets/app_icon.webp" alt="App Icon" width="128"/>
</p>

<p align="center">
  <strong>Familiar calculator-style budget tracking app for Android.</strong><br>
  Built with Jetpack Compose and Material Design 3 Expressive
</p>

<p align="center">
  <a href="https://github.com/isaacsa51/Sorter/releases">
    <img src="https://img.shields.io/github/v/release/isaacsa51/Minus?include_prereleases" alt="Release">
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/API-31%2B-brightgreen.svg" alt="API">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-blue.svg" alt="Kotlin">
</p>

<p align="center">
  <img src="assets/screenshot_2.png" alt="Screenshot 1" width="200" style="border-radius:26px;"/>
  <img src="assets/screenshot_3.png" alt="Screenshot 2" width="200" style="border-radius:26px;"/>
  <img src="assets/screenshot_4.png" alt="Screenshot 3" width="200" style="border-radius:26px;"/>
  <img src="assets/screenshot_5.png" alt="Screenshot 4" width="200" style="border-radius:26px;"/>
</p>

---

## Project Overview

The primary goal of Minus is to provide an easy and familiar interface for recording daily spending. By mimicking the layout of a standard calculator interface that reduces it's complexity, that way any user can easy and quickly enter their expenses.

### Core Features

- Easy Expense Entry: Log expenses directly through a Numpad interface.
- CSV Export: Securely export expense data to the device Downloads folder for backup or further analysis in spreadsheet software.
- Undo & Batch Actions: Restore individual expenses or delete all at once.
- Wear OS Integration: Provides a companion app for quick expense entry on wearable devices.
- Subscription Management: Easily manage and track subscriptions.
- Customizable Settings: Light/Dark/System themes, Material You colors and typography styles.
- Make calculations on the fly: Need to split a expense amount among multiple people? Swipe up to reveal the operator button, make an operation and press equals to see the result and directly save that expense amount.


## Technology Stack

| Category               | Technology                    | Purpose                                              |
|:-----------------------|:------------------------------|:-----------------------------------------------------|
| UI Framework           | Jetpack Compose               | Declarative UI for Mobile and Wear OS                |
| Architecture           | MVI (Model-View-Intent)       | Uni-directional data flow and state management       |
| Asynchronous / Streams | Kotlin Coroutines & Flow      | Non-blocking database I/O and reactive state updates |
| Dependency Injection   | Hilt / Dagger                 | Modular dependency management and scoping            |
| Database               | Room                          | Local persistence with SQLite                        |
| Logging                | Logcat (Square)               | Pattern-based structured logging                     |
| Navigation             | Compose Navigation            | Type-safe in-app navigation                          |
| Design System          | Material 3 Expressive (Alpha) | Modern UI components and dynamic theming             |

## Architecture

Minus follows Modern Android Development (MAD) practices and a Clean Architecture approach:

*   Presentation Layer: Built entirely with Jetpack Compose. Uses ViewModel and StateFlow for reactive UI updates.
*   Data Layer: Utilizes Room Database for local persistence and MediaStore for file exports.
*   Navigation: Managed via Jetpack Compose Navigation with a centralized AppNavGraph.
*   Concurrency: Powered by Kotlin Coroutines and Flow for non-blocking operations.
*   Wear OS Integration: Provides a companion app for quick expense entry on wearable devices.
*   MVI (Model-View-Intent) Architecture: Used for state management and event handling.

## Wear OS Integration _(still very early on development, may not work properly)_

Minus includes a companion Wear OS application. The wearable version provides:
*   Similar numpad interface for quick expense entry.
*   Glanceable history of recent entries.
*   Notification sync through system.
*   Optimization for round and square watch faces using the latest Wear OS Compose libraries.

### Disclaimer
This project is still in its early stages of development. While the core features are functional, the Wear OS integration is still under active development and may not work as expected. Please report any issues or feedback you have.

### Contributing
Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) for more information.

### Translations
Any contributions to translate Minus into other languages are greatly appreciated. Please submit a pull request with your translations.

Current Translation Status:
- [x] English
- [x] Spanish
- [?] French (WIP)
- [ ] German
- [ ] Italian
- [ ] Portuguese
- [ ] Russian
- [ ] Chinese
- [ ] Japanese
- [ ] Korean

Made with ❤️ by [Isaac Serrano](https://linkedin.com/in/serranoie)