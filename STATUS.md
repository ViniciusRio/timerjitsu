# Status

**Done:** v0.1.0 (versionCode 1, tag `v0.1.0` on `3ff6934`) is live in Play Store production (submitted
2026-11-24, full rollout, 6 Portuguese-speaking countries). Tested on a Galaxy A26 (Android 16) with the release build: background workout, sounds, voice, music ducking, torch,
flash, laps, notification controls.

**Watching:** Play Console Android vitals (crashes, ANRs, background service killed by OEM battery savers)
and reviews. Next upload must bump `versionCode` to 2; test it on an internal track before production.

**Next (no OpenSpec change yet):** heart rate, smartwatch integration, drag to reorder timers, other languages
(UI strings are hardcoded in Kotlin).

**To investigate:** during a running workout the screen wakes ~3 s after `adb shell input keyevent KEYCODE_SLEEP`,
but not after `KEYCODE_POWER`. It does not look like a real-use problem.

**Blocked:** nothing.

**Do not touch:** the upload keystore and `keystore.properties` (outside git).
