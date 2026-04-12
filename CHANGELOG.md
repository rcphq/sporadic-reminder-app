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
- Material 3 with dynamic colors and dark mode support
- Runtime permission handling for notifications and exact alarms
- Design spec and implementation plan documentation
- Project conventions (CLAUDE.md)
- Developer tooling: scheduling-reviewer and notification-tester agents, /build /test /lint commands
