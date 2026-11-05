# Timer Jitsu

**Todo segundo importa.**

Timer de treino para Android (Kotlin + Jetpack Compose), para uso pessoal. Código, visual, modelos e sons são
próprios: os sons são sintetizados em `Feedback.kt`, sem arquivos de áudio no app.

- Timers com preparação, séries, repetições, ciclos, descanso entre ciclos e relaxamento
- Cronômetro com voltas manuais e automáticas
- Modelos prontos, busca, duplicar e excluir
- Sons, contagem regressiva, voz, vibração, lanterna e flash na tela
- Continua em segundo plano por um serviço em primeiro plano

## Comandos

    ./gradlew :app:testDebugUnitTest
    ./gradlew :app:assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk

A lógica do treino fica em `Engine.kt`, sem dependências do Android, e é coberta por `EngineTest.kt`.

O que falta testar e fazer está em [`PENDENCIAS.md`](PENDENCIAS.md).
