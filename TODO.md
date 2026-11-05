# TODO

## Confirmed in real use

- [x] A user-created workout keeps running with the screen off
- [x] Sounds play normally in that scenario

Also checked on a Galaxy A26 (Android 16) with the minified release build:

- [x] Torch blink on interval change
- [x] Screen flash
- [x] Stopwatch automatic lap
- [x] Stopwatch manual lap
- [x] Pause/resume button in the notification
- [x] "Open last timer" setting, with a saved timer
- [x] Workout and sounds keep running with notifications denied

## Still to test on a device

- [ ] Voice can be heard (the speech engine plays, but Android 16 logs "background playback would be muted" for it)
- [ ] Music ducking during sounds can be heard (the audio focus request is sent)

## To fix

- [ ] The "Marcar volta" button moves up as laps are added, so a repeated tap in the same spot misses it

## Still to investigate

- [ ] The screen wakes about 3 s after `input keyevent KEYCODE_SLEEP` during a running workout, but not without a
      workout and not after `KEYCODE_POWER` (35 s with the screen off), so it does not look like a real-use problem

## Not implemented yet

- [ ] Heart rate
- [ ] Smartwatch integration
- [ ] Drag to reorder timers
- [ ] Other languages (Portuguese only; UI strings are still hardcoded in Kotlin)
