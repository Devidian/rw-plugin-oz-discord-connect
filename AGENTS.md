# AGENTS.md

## Repository Purpose
This repository owns Discord integration for Rising World server, player, support, status, and admin interactions.

It must remain usable standalone. Workspace-root orchestration is optional and must never be required for build, release, or local agent operation.

## Ownership
Owns:
- in-game chat/support/status forwarding to Discord
- Discord-triggered admin/server commands and restart coordination
- Discord webhook, bot, and public plugin API integration surfaces

Does not own:
- generic shared helpers that belong in `rw-plugin-oz-tools`
- feature-plugin business events before they become Discord notifications
- land claim, GPS, intercom, or admin utility domain logic

## Mandatory Workflow Rules
- Preserve the Java 20 baseline.
- Preserve Maven build and GitHub tag-release behavior.
- Keep dependencies minimal and runtime-safe.
- Use `rw-plugin-oz-tools` for reusable infrastructure.
- Follow `.codex/agents.toml` for local agent roles, task classes, context loading, and escalation.
- Follow `docs/policies/repository-policy.md` for reusable governance rules.
- Keep `README.md`, `HISTORY.md`, and `PLANS.md` aligned with behavior or structure changes.

## Validation
- Run `mvn -B -DskipTests package` for build-impacting changes.
- Run `mvn -B test` when tests exist.
- Verify new Rising World API usage before relying on it.
- Review webhook, bot command, public API, and config compatibility for user-visible changes.
