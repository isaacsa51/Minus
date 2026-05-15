# Contributing to Minus

Thanks for helping improve Minus. This project is an Android budget tracker built with Kotlin, Jetpack Compose, Room, Hilt, and a Wear OS companion module.

## Getting started

- Use Android Studio with JDK 17.
- Clone the repository and open the project root.
- Let Gradle sync before making changes.
- Do not commit local files such as `local.properties`, generated build outputs, or signing credentials.

## Before opening a pull request

Run the same basic checks used by CI:

```bash
./gradlew :app:compileDebugKotlin :wear:compileDebugKotlin :sync-contract:compileKotlin :app:testDebugUnitTest
```

If your change only affects one module, still make sure the full app compiles before requesting review.

## Pull request guidelines

- Use commit messages that follow the [conventional commits guidelines](https://www.conventionalcommits.org/en/v1.0.0/).
- Keep PRs focused and small when possible.
- Include a short description of the problem and the approach used.
- Add screenshots or screen recordings for UI changes.
- Mention any manual testing performed, especially for budget periods, recurring expenses, notifications, CSV import/export, and Wear OS sync.
- Update translations in `values`, `values-es`, and `values-fr` when adding user-facing strings.

## Code style

- Follow the existing Kotlin and Compose style in nearby files.
- Prefer string resources for user-facing text.
- Keep business/date logic out of UI code when it is reusable or testable.
- Add or update unit tests for date, budget, recurrence, or mapping logic when possible.
