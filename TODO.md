# TODO

## Confirmed in real use

- [x] A user-created workout keeps running with the screen off
- [x] Sounds play normally in that scenario

## Still to test on a device

- [ ] Voice (speaks the interval name)
- [ ] Torch blink on interval change
- [ ] Screen flash
- [ ] Stopwatch automatic lap
- [ ] Stopwatch manual lap
- [ ] Pause/resume button in the notification
- [ ] Music ducking during sounds
- [ ] "Open last timer" setting

## Still to investigate

- [ ] Why the screen woke 2 to 4 s after being turned off during `adb`-driven tests with a workout running
      (the manual test worked; it may only be an effect of `input keyevent` or a device gesture)
- [ ] Why `adb install` takes 3 to 7 minutes on the test device

## Not implemented yet

- [ ] Heart rate
- [ ] Smartwatch integration
- [ ] Drag to reorder timers
- [ ] Other languages (Portuguese only; UI strings are still hardcoded in Kotlin)
- [ ] Release APK (debug only, 17 MB)
