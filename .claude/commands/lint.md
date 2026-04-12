---
name: lint
description: Run Android lint and report issues
---

Run Android lint on the Sporadic Reminder App.

Run: `./gradlew lintDebug` from the project root.

After lint completes:
1. Read the lint report at `app/build/reports/lint-results-debug.html`
2. Report any errors (must fix) and warnings (should fix) grouped by category
3. For each issue, include: file path, line number, description, and suggested fix
4. If clean, confirm no issues found.
