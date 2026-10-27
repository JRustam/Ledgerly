# Ledgerly architecture verification

Date: 2026-10-25. Scope: completion of Milestone 2 on top of the existing multi-module migration. No commit or push performed.

## Checks

- Debug app build: passed.
- App JVM unit tests: 28 passed, zero failures/skips.
- Domain JVM unit tests: 22 passed, zero failures/skips.
- Module dependency validation: passed; app → domain/data/designsystem, data → domain, no cycles.
- Instrumentation APK compilation: passed. Device tests were not run: no device/emulator was connected.
- Lint: zero errors. The app report contains 78 warnings; data and design-system reports contain zero warnings. Warnings include toolchain/dependency notices and existing resource/UI issues.
- Room migration implementation and exported v1/v2/v3 schemas were compared with the pre-continuation snapshot and are byte-identical.

Commands:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :domain:test \
  :app:assembleDebugAndroidTest validateModuleDependencies \
  :app:lintDebug :data:lintDebug :core:designsystem:lintDebug \
  -Pkotlin.compiler.execution.strategy=in-process --no-watch-fs
./gradlew :domain:test --rerun-tasks \
  -Pkotlin.compiler.execution.strategy=in-process --no-watch-fs
```

## Focused coverage

Existing transaction, balance, preference, and finance tests remain intact. Added milestone coverage verifies separate-currency wallet totals, unassigned legacy rows counted once, month comparisons and budgets, quote-pair validation, decimal rounding, CSV snapshot escaping, replacement-request cancellation, cached-rate fallback, reactive ViewModel summaries, saved report-month restoration, write failure/retry, and reopening existing appearance/onboarding/currency preference keys.

## Limitations

Instrumentation APK compilation does not prove device behavior. The existing v1 → v2 → v3 migration test and portfolio flow require runtime verification. A dedicated migration test starting with populated v2 multi-currency data is still missing. The earlier portfolio activity-recreation failure remains unverified. Existing startup preference recovery, locale parsing, navigation edge cases, backup policy and release warnings are outside this milestone.

The initial preference-model compile failure was resolved by removing duplicate data-layer types and using the domain-owned models; storage keys, values and filenames were not changed. All existing uncommitted work remains in the checkout. Localization and visual-design work were not started.
