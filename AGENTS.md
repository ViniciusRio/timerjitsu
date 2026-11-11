# AGENTS.md

Timer Jitsu: an interval workout timer for Android (Kotlin + Jetpack Compose, single `app` module, package
`dev.timerjitsu`). The workflow below follows `~/projects/personal/ai-docs/boas-praticas-ia.md`.

## Commands

    ./gradlew :app:testDebugUnitTest   # unit tests
    ./gradlew :app:lintDebug           # Android lint
    ./gradlew :app:assembleDebug       # build and type-check
    ./gradlew :app:bundleRelease       # signed only when keystore.properties exists

Definition of done before a handoff: tests, lint and `assembleDebug` pass, run by the implementer.

## Structure

- `Engine.kt`: workout logic with no Android dependencies, covered by `EngineTest.kt`. Keep it Android-free.
- `play-store/`: store listing texts, images and Play Console answers. `docs/`: privacy policy.
- `openspec/`: specs and changes. Progress goes in `openspec/changes/*/tasks.md`.
- `STATUS.md`: current state, at most ~15 lines, updated when an agent stops.

## Workflow

- Relevant changes start with an OpenSpec change (`proposal.md`, `design.md`, `tasks.md`), reviewed by Codex and
  approved by Vinícius before code. No implementation (including `openspec-apply-change`) starts before both.
  Small fixes (typo, one-liner) skip the spec.
- Work on a `feat/...`, `fix/...` or `chore/...` branch, never on `main`. Open a PR with the handoff in its
  description: Objetivo, Branch e commit, Arquivos alterados, Decisões, Checagens rodadas, Dúvidas em aberto,
  Próxima ação.
- The reviewer is read-only and leaves comments on the PR. Only Vinícius merges.
- Conventional Commits (`feat: ...`, `fix: ...`, `docs: ...`, `chore: ...`), with the agent in a
  `Co-Authored-By` trailer.

## Conventions

- Code, comments and docs are in English. UI strings are in Portuguese, hardcoded in Kotlin for now.
- All sounds are synthesized in `Feedback.kt`. Do not add audio files.

## Recurring corrections

- The repo is public. Never commit `PLAY_STORE_CHECKLIST.md` (kept out through `.git/info/exclude`),
  `keystore.properties`, keystores or passwords. Check with `git check-ignore` and `git ls-files` before claiming
  a file is ignored.
