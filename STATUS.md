# Status

**Done:** app ready for the Play Store (signed release, privacy policy, listing). Tested on a Galaxy A26
(Android 16) with the release build: background workout, sounds, voice, music ducking, torch, flash, laps,
notification controls.

**Next (no OpenSpec change yet):** heart rate, smartwatch integration, drag to reorder timers, other languages
(UI strings are hardcoded in Kotlin).

**To investigate:** during a running workout the screen wakes ~3 s after `adb shell input keyevent KEYCODE_SLEEP`,
but not after `KEYCODE_POWER`. It does not look like a real-use problem.

**Blocked:** nothing.

**Do not touch:** the upload keystore and `keystore.properties` (outside git).
