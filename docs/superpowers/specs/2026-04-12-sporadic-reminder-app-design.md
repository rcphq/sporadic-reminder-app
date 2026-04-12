# Sporadic Reminder App — Design Spec

## Overview

A native Android app (14+) that delivers randomized reminders throughout the day. Users configure independent reminders with custom text, tones, vibration, priority, time windows, and day-of-week schedules. Reminders can be organized into groups that share schedules. Notifications require user action (Done/Skip/Snooze) and the dashboard tracks activity.

Inspired by "Randomly Remind Me."

## Target Platform

- Android 14+ (API 34+)
- Kotlin + Jetpack Compose
- Material 3 with dynamic colors

---

## Data Model

### Reminder

| Field              | Type              | Description                                                        |
| ------------------ | ----------------- | ------------------------------------------------------------------ |
| id                 | Long (PK)         | Auto-generated                                                     |
| name               | String            | User-facing label for the reminder                                 |
| notificationText   | String            | Text displayed in the notification                                 |
| notificationToneUri| String (nullable)  | URI from system ringtone picker; null = silent                     |
| vibrate            | Boolean           | Whether the notification vibrates                                  |
| priority           | Enum              | LOW / DEFAULT / HIGH / URGENT — maps to Android notification importance |
| startTime          | LocalTime         | Earliest time a notification can fire                              |
| endTime            | LocalTime         | Latest time a notification can fire                                |
| notificationCount  | Int               | Number of notifications to randomly distribute in the window       |
| activeDays         | Int (bitmask)     | Which days of the week this reminder is active (Mon=1 .. Sun=64)   |
| dndBehavior        | Enum              | SKIP / SNOOZE — what to do when DND is active at fire time         |
| isActive           | Boolean           | Master on/off toggle                                               |
| groupId            | Long? (FK)        | Nullable reference to ReminderGroup                                |

### ReminderGroup

| Field          | Type          | Description                                                            |
| -------------- | ------------- | ---------------------------------------------------------------------- |
| id             | Long (PK)     | Auto-generated                                                         |
| name           | String        | User-facing group label                                                |
| startTime      | LocalTime?    | Shared schedule override — nullable (null = members use own schedules) |
| endTime        | LocalTime?    | Shared schedule override                                               |
| notificationCount | Int?       | Shared count override                                                  |
| activeDays     | Int?          | Shared active days override (bitmask)                                  |

When a group has non-null shared schedule fields, those override the individual member reminders' corresponding fields. When null, each member reminder uses its own settings.

### NotificationLog

| Field          | Type          | Description                                              |
| -------------- | ------------- | -------------------------------------------------------- |
| id             | Long (PK)     | Auto-generated                                           |
| reminderId     | Long (FK)     | Which reminder fired                                     |
| scheduledTime  | Instant       | When the alarm was originally scheduled                  |
| firedTime      | Instant       | When the notification was actually posted                |
| action         | Enum          | DONE / SKIPPED / SNOOZED                                |
| actionTimestamp| Instant?      | When the user acted (null if pending)                    |

Designed to support future analytics (streaks, trends, charts) without schema changes.

### ScheduledAlarm

| Field          | Type          | Description                                              |
| -------------- | ------------- | -------------------------------------------------------- |
| id             | Long (PK)     | Auto-generated                                           |
| reminderId     | Long (FK)     | Which reminder this alarm is for                         |
| scheduledTime  | Instant       | The exact time this alarm will fire                      |
| status         | Enum          | PENDING / FIRED / SKIPPED / SNOOZED                     |

Ephemeral working state — regenerated daily. Not exported.

---

## Architecture

MVVM with Clean Architecture layers.

### UI Layer (Jetpack Compose)

- **DashboardScreen** — Summary cards (today's stats: fired/done/skipped/snoozed counts, active reminders count), list of fired notifications with Done/Skip/Snooze action buttons.
- **ReminderListScreen** — All reminders and groups displayed in a list. Toggle active/inactive. Tap to edit.
- **ReminderEditScreen** — Full editor for a single reminder: name, notification text, tone picker, vibrate toggle, priority selector, start/end time pickers, notification count, day-of-week selector, DND behavior toggle, group assignment.
- **GroupEditScreen** — Group name, shared schedule toggle (with schedule fields), member reminder management.
- **SettingsScreen** — Export/import, theme override (Light/Dark/System), about, permissions management.

### ViewModel Layer

- One ViewModel per screen.
- Exposes UI state as `StateFlow`.
- Communicates with Use Cases only — never directly with Room or system APIs.

### Domain Layer (Use Cases)

- **ScheduleRemindersUseCase** — Generates N random times for a reminder within its [startTime, endTime] window. Writes to ScheduledAlarm table. Delegates to AlarmScheduler to register with AlarmManager.
- **HandleNotificationActionUseCase** — Processes Done/Skip/Snooze actions. Writes to NotificationLog. For Snooze, triggers Android's native notification snooze.
- **ExportDataUseCase** — Serializes Reminders, Groups, NotificationLogs, and app settings to a versioned JSON file.
- **ImportDataUseCase** — Validates JSON schema version, presents preview, then merges or replaces data.
- **SyncGroupScheduleUseCase** — When a group's shared schedule changes, updates scheduling for all member reminders.

### Data Layer

- Room database with DAOs for Reminder, ReminderGroup, NotificationLog, ScheduledAlarm.
- `ReminderRepository`, `GroupRepository`, `NotificationLogRepository`.

### System Integration Layer

- **AlarmScheduler** — Wraps `AlarmManager.setExactAndAllowWhileIdle()`. Handles scheduling and cancellation of exact alarms.
- **NotificationPublisher** — Builds and posts notifications with Done/Skip/Snooze action buttons via notification actions. Tapping the notification body opens the app to the reminder detail.
- **BootReceiver** — `BroadcastReceiver` registered for `BOOT_COMPLETED`. Queries ScheduledAlarm for PENDING alarms whose time hasn't passed, re-registers them with AlarmManager.
- **DailySchedulerReceiver** — Fires once daily (early morning, e.g., 4am). For each active reminder whose activeDays includes today, generates random fire times and schedules alarms.
- **DndStateChecker** — Queries `NotificationManager.getCurrentInterruptionFilter()` before firing. Applies the reminder's dndBehavior (SKIP or SNOOZE).

---

## Notification & Scheduling Flow

### Daily Cycle

1. **Daily trigger** — `DailySchedulerReceiver` fires early morning. For each active reminder whose `activeDays` includes today:
   - If the reminder belongs to a group with a shared schedule, use the group's schedule fields.
   - Generate N random times within [startTime, endTime].
   - Write each to `ScheduledAlarm` with status PENDING.
   - Register each with AlarmManager via `AlarmScheduler`.

2. **Alarm fires** — AlarmManager triggers a BroadcastReceiver at the scheduled time:
   - Query `DndStateChecker` for current DND state.
   - If DND active, check the reminder's `dndBehavior`:
     - **SKIP** — Mark ScheduledAlarm as SKIPPED, log to NotificationLog as SKIPPED. Do not fire.
     - **SNOOZE** — Reschedule alarm with a fixed 15-minute retry interval. On each retry, re-check DND. If DND is still active, reschedule again. If clear, fire. If the retry would fall outside the reminder's endTime, mark as SKIPPED instead.
   - If DND inactive — post notification via `NotificationPublisher`.
   - Update ScheduledAlarm status to FIRED.

3. **User acts on notification**:
   - **Done** (action button or in-app) — Log DONE to NotificationLog.
   - **Skip** (action button or in-app) — Log SKIPPED to NotificationLog.
   - **Snooze** (action button or in-app) — Use Android native snooze. Log SNOOZED to NotificationLog.
   - Tapping notification body opens DashboardScreen with the relevant notification highlighted.

4. **Group pooling** — When a group has a shared schedule, the daily trigger generates `notificationCount` random times for the group as a whole. At each fire time, one member reminder is randomly selected from the group to display.

### Boot Recovery

`BootReceiver` fires on `BOOT_COMPLETED`. Queries `ScheduledAlarm` for PENDING entries whose `scheduledTime` is still in the future. Re-registers each with AlarmManager.

### Permissions

- `SCHEDULE_EXACT_ALARM` — Required for exact alarm scheduling. User-granted on Android 14+.
- `POST_NOTIFICATIONS` — Required for posting notifications on Android 13+.
- `RECEIVE_BOOT_COMPLETED` — For rescheduling alarms after reboot.

---

## Export/Import

- **Format**: JSON with a `schemaVersion` field at the root for forward compatibility.
- **Export scope**: All Reminders, ReminderGroups, NotificationLogs, and app settings. ScheduledAlarm is excluded (ephemeral).
- **File handling**: Uses Android's Storage Access Framework (SAF) for file picker — user chooses save/load location.
- **Import flow**:
  1. User selects a JSON file via SAF picker.
  2. App validates schema version.
  3. App shows a preview: count of reminders, groups, and log entries to be imported.
  4. User chooses: **Merge** (add new items, skip duplicates matched by name) or **Replace** (wipe current data, restore from file).
  5. After import, reschedule all alarms for today.

---

## Theming & Accessibility

### Theming

- Material 3 with `dynamicColorScheme()` — leverages Android 12+ dynamic color extraction from the user's wallpaper.
- Dark mode follows system setting by default. In-app override in Settings: Light / Dark / System.

### Accessibility

- Content descriptions on all interactive elements.
- Contrast ratios meet WCAG AA standards.
- Full TalkBack navigation support.
- Scalable text using `sp` units.
- Minimum touch target size: 48dp.

---

## Roadmap (Post-MVP)

1. **Recalculate today's times** — Manual button to regenerate and reshuffle random fire times for the current day.
2. **Home screen widgets** — Glanceable stats card or next-upcoming reminder widget.
3. **Detailed analytics** — Completion streaks, per-reminder trends, charts. Data model already supports this.
4. **Multi-language support** — Spanish/English to start. Architecture-ready from day one with `strings.xml` resource files.
5. **Evenly-spaced mode** — Alternative to random distribution, per-reminder toggle.
