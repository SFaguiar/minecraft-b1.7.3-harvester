# Harvester repository rules

- This is the clean source lineage for the Babric and StationAPI port.
- Use Java 17 for every modern build and test.
- Do not copy Java, bytecode, assets, mappings, or binaries from Minecraft,
  MCP, ModLoader, ModLoader Fix, or another mod.
- Do not import files or Git history from the legacy ModLoader repository.
- Use the legacy implementation only to extract behavior, edge cases, test
  criteria, and rejected alternatives into prose.
- Prefer StationAPI events and public APIs. Use the smallest possible Mixin
  only when no suitable API exists.
- Do not use `@Overwrite` without a separately approved architecture decision.
- Keep game integration outside isolated domain logic.
- Add automated tests for logic that can run without Minecraft.
- Treat successful manual client, dedicated-server, and MultiMC tests as
  mandatory release gates.
- Never commit Minecraft JARs, decompiled sources, MCP, launcher instances,
  account data, caches, personal logs, or third-party mod binaries.
- Run `scripts/check-prohibited-files.ps1` before every commit.
- Use Conventional Commits and short-lived branches.
- Do not publish a release or claim completion while manual testing is pending.
- Do not create or change a remote without human approval.
- The Harvester 2.x license remains undecided until the authorship audit is
  complete.
