# Sporadic Reminder App

## Project Overview

Native Android app (API 34+) that delivers randomized reminders throughout the day. Built with Kotlin, Jetpack Compose, and Material 3.

See `docs/superpowers/specs/2026-04-12-sporadic-reminder-app-design.md` for the full design spec.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3 (dynamic colors)
- **Architecture**: MVVM + Clean Architecture (UI -> ViewModel -> UseCase -> Repository -> Room)
- **Scheduling**: AlarmManager (exact alarms)
- **Database**: Room
- **DI**: Hilt
- **Min SDK**: 34 (Android 14)

## Documentation Requirements

Keep these files up to date with every meaningful change:

- **README.md** — Project description, setup instructions, build steps, screenshots (when available). Update when adding new features or changing setup requirements.
- **CHANGELOG.md** — Follow [Keep a Changelog](https://keepachangelog.com/) format. Every user-facing change, bug fix, or breaking change gets an entry. Group under: Added, Changed, Deprecated, Removed, Fixed, Security.
- **This file (CLAUDE.md)** — Update when architecture decisions, conventions, or tooling change.

## Code Conventions

- Follow Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- Compose functions are PascalCase, all other functions are camelCase
- One ViewModel per screen, expose state as `StateFlow`
- ViewModels talk to Use Cases, never directly to Room or system APIs
- Use Cases are single-responsibility classes named as verb phrases (e.g., `ScheduleRemindersUseCase`)
- Repository pattern for all data access
- Enums for fixed sets: Priority (LOW/DEFAULT/HIGH/URGENT), DndBehavior (SKIP/SNOOZE), NotificationAction (DONE/SKIPPED/SNOOZED), AlarmStatus (PENDING/FIRED/SKIPPED/SNOOZED)

## Project Structure

```
app/src/main/java/com/sporadic/reminder/
  ui/           # Compose screens and components
    dashboard/
    reminders/
    groups/
    settings/
  viewmodel/    # Screen ViewModels
  domain/       # Use Cases
  data/         # Room entities, DAOs, Repositories
  scheduler/    # AlarmScheduler, BootReceiver, DailySchedulerReceiver
  notification/ # NotificationPublisher, DndStateChecker
  export/       # Export/Import logic
  di/           # Hilt modules
```

## Testing

- Unit tests for all Use Cases and ViewModels
- Instrumented tests for Room DAOs
- UI tests for critical flows (create reminder, notification actions)

## Naming

- Notification user actions: **Done**, **Skip**, **Snooze** (not "ignore" or "defer")
- "Reminder" = a single configurable notification unit
- "Group" = a collection of reminders that can share a schedule
