# Pendências

## Confirmado em uso real

- [x] Treino criado pelo usuário continua funcionando com a tela desligada
- [x] Sons tocam normalmente nesse cenário

## Falta testar no aparelho

- [ ] Voz (fala o nome do intervalo)
- [ ] Lanterna piscando na troca de intervalo
- [ ] Flash na tela
- [ ] Volta automática do cronômetro
- [ ] Marcar volta manualmente
- [ ] Botão de pausar/continuar na notificação
- [ ] Música abaixando durante os sons
- [ ] Opção "Abrir último timer"

## Falta verificar

- [ ] Por que a tela acordou sozinha 2 a 4 s depois de desligada nos testes via `adb`, com treino rodando
      (o teste manual funcionou; pode ser só efeito do `input keyevent` ou de um gesto do aparelho)
- [ ] Por que o `adb install` leva de 3 a 7 minutos neste aparelho

## Ainda não feito

- [ ] Batimentos cardíacos
- [ ] Integração com relógio
- [ ] Arrastar para reordenar timers
- [ ] Outros idiomas (hoje só português)
- [ ] APK de release (hoje só debug, com 17 MB)
