# Timer Jitsu

**Todo segundo importa.** (Every second counts.)

An interval workout timer for Android, built with Kotlin and Jetpack Compose. All sounds are synthesized in
`Feedback.kt`, so the app ships no audio files. The app's interface is currently in Portuguese.

## Features

- Timers with prepare, sets, repeats, cycles, rest between cycles and cooldown
- Stopwatch with manual and automatic laps
- Ready-made templates, search, duplicate and delete
- Sounds, countdown, voice, vibration, torch and screen flash
- Keeps running in the background through a foreground service

## Commands

    ./gradlew :app:testDebugUnitTest
    ./gradlew :app:assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk

The workout logic lives in `Engine.kt`, has no Android dependencies and is covered by `EngineTest.kt`.

Remaining work is tracked in [`TODO.md`](TODO.md).

## License

[MIT](LICENSE)
