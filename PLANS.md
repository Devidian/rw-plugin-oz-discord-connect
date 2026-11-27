# PLANS.md

- [x] Route connected Discord chat only among consenting players and provide one local fallback when the bot is unavailable.
- [ ] Verify both connected and disconnected chat modes on Development with two players.
- [x] Keep webhook transport moving after image submission failures and make pending waits interruptible during shutdown.
- [ ] Verify webhook image failure recovery and plugin shutdown on Development.

Planning is stored in repository-local docs.

Restart ownership moved to Admin Utils; this plugin retains `/restart` and `/dc restart` as optional forwarding commands.

- Active implementation tasks: [docs/active/](docs/active/)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

Discord event thread routing is implemented locally; Development Discord validation and the separately authorized release remain open. Configuration is documented in README.md.
