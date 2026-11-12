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
    ./gradlew :app:bundleRelease

The release bundle is signed only when a `keystore.properties` file exists at the project root (it is ignored by
git). Store listing texts, images and Play Console answers live in `play-store/`, and the privacy policy lives in
`docs/`.

The workout logic lives in `Engine.kt`, has no Android dependencies and is covered by `EngineTest.kt`.

Current state is in [`STATUS.md`](STATUS.md) and planned changes in [`openspec/`](openspec/). Agent rules
live in [`AGENTS.md`](AGENTS.md).

## License

[MIT](LICENSE)
