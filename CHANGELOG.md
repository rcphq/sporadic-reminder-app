# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Reminder creation and management with full customization (text, tone, vibrate, priority, time window, day-of-week, DND behavior)
- Reminder groups with optional shared schedules
- Random notification scheduling within configurable time windows using AlarmManager
- Dashboard with today's stats (pending/done/skipped/snoozed) and activity log
- Notification actions: Done / Skip / Snooze (from notification and in-app)
- DND-aware scheduling with per-reminder skip or snooze behavior
- Boot recovery for scheduled alarms
- Export/import data as JSON with schema versioning and merge/replace support
- Catppuccin color theme (Latte for light, Mocha for dark) replacing default dynamic colors
- Custom semantic color system (SporadicColors) for status and priority colors
- Material Icons Extended for rich iconography across all screens
- Custom typography (SemiBold headlines, Medium titles) and rounded shapes
- Colored stat cards with status icons on Dashboard
- Notification bell icon and colored action buttons on fired alarm cards
- Circular icon containers and colored badges on reminder/group list cards
- Priority-colored segmented buttons with leading icons on edit screens
- Day-of-week chips with checkmarks and Catppuccin Mauve selection color
- DND behavior buttons with colored skip/snooze containers
- Leading form icons (tone, vibrate, time, group) on edit screens
- Schedule section wrapped in surfaceVariant card on group edit
- Section headers with icons and primary color on settings screen
- Theme mode segmented buttons with light/dark/system icons
- Error messages displayed in errorContainer cards
- Lavender pill highlight on selected bottom navigation tab
- Peach-tinted FABs for visual distinction from primary Mauve
- Catppuccin Mauve launcher background color
- Material 3 with dark mode support
- Runtime permission handling for notifications and exact alarms
- Design spec and implementation plan documentation
- Project conventions (CLAUDE.md)
- Developer tooling: scheduling-reviewer and notification-tester agents, /build /test /lint commands
