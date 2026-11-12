@AGENTS.md

## Claude Code

- Codex review runs automatically, without asking, in read-only mode through the Codex plugin:
  `node ~/.claude-pessoal/plugins/cache/openai-codex/codex/<version>/scripts/codex-companion.mjs adversarial-review "--wait ..."`
  - After creating or updating an OpenSpec change: review the change folder, show the result to Vinícius and wait for
    his approval before applying.
  - After opening or updating a PR: review with `--base main --scope branch`, then post the output as a PR comment
    titled "Revisão do Codex, rodada N".
  - Fix each "Bloqueante" finding; "Sugestão" findings stay on the PR. In round 2, ask Codex to check every original
    finding against the fixed diff. After 2 rounds with blocking findings left, Vinícius decides.
