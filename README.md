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

### Core Features

- Easy Expense Entry: Log expenses directly through a Numpad interface.
- Splitting Functionality: Built-in functionality to split checks among multiple people directly on demand.
- CSV Export: Securely export expense data to the device Downloads folder for backup or further analysis in spreadsheet software.
- Undo & Batch Actions: Restore individual expenses or delete all at once.
- Wear OS Integration: Provides a companion app for quick expense entry on wearable devices.
- Subscription Management: Easily manage and track subscriptions.
- Material Design 3: Dynamic colors, smooth animations, predictive back gestures.
- Customizable Settings: Light/Dark/System themes, Material You colors.

## Project Overview

The primary goal of Minus is to provide a "zero-friction" interface for recording daily spending. By mimicking the layout of a standard calculator interface that reduces it's complexity, that way any user can easy and quickly enter their expenses.

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

### Key Features

*   Calculator-First Entry: Log expenses directly through a Numpad interface.
*   Check Splitting: Built-in functionality to split checks among multiple people directly on demand.
*   CSV Export: Securely export expense data to the device Downloads folder for backup or further analysis in spreadsheet software.

## Architecture

Minus follows Modern Android Development (MAD) practices and a Clean Architecture approach:

*   Presentation Layer: Built entirely with Jetpack Compose. Uses ViewModel and StateFlow for reactive UI updates.
*   Data Layer: Utilizes Room Database for local persistence and MediaStore for file exports.
*   Navigation: Managed via Jetpack Compose Navigation with a centralized AppNavGraph.
*   Concurrency: Powered by Kotlin Coroutines and Flow for non-blocking operations.
*   Wear OS Integration: Provides a companion app for quick expense entry on wearable devices.
*   MVI (Model-View-Intent) Architecture: Used for state management and event handling.

## UI Components and Widgets

*   Numpad: A highly optimized custom grid layout using weight-based animations for mode transitions.
*   NumpadButton: Custom buttons with smooth, state-aware corner radius transitions (Circle to Rounded Square).
*   WavyDivider: A custom drawing component that uses BoxWithConstraints to ensure consistent rendering across different aspect ratios and accessibility settings.

## Wear OS Integration

Minus includes a companion Wear OS application. The wearable version provides:
*   Quick-add functionality for common expense amounts.
*   Glanceable history of recent entries.
*   Optimization for round and square watch faces using the latest Wear OS Compose libraries.
