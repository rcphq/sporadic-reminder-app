---
name: test
description: Run unit tests and report results
---

Run the Sporadic Reminder App unit test suite.

Run: `./gradlew testDebugUnitTest` from the project root.

After tests complete:
1. Report total pass/fail/skip counts
2. If any tests failed, read the test report at `app/build/reports/tests/testDebugUnitTest/index.html` and list each failure with:
   - Test class and method name
   - Expected vs actual values
   - Stack trace summary (first relevant line)
3. If all tests pass, confirm with the count.
