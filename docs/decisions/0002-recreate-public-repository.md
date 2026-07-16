# ADR 0002: Recreate the public repository from the clean lineage

- Status: Accepted
- Date: 2026-07-16

## Context

The original public repository contained legacy source and a release artifact outside
the intended license boundary. A normal remediation commit removed them from its
default branch, but published Git objects remained reachable through the historical
tag.

After preserving a verified private Git bundle, the repository owner deleted the old
GitHub repository. This made it possible to reuse the public name without carrying its
object database, tags, releases, pull requests, or branch history into Harvester 2.x.

## Decision

Recreate `SFaguiar/minecraft-b1.7.3-harvester` as a public repository by pushing only
the unrelated clean-room `main` branch.

At recreation, the remote contained:

- two original clean-room commits;
- no tags or releases;
- no legacy Java, bytecode, JARs, or archives;
- no remote relationship to the private legacy bundle;
- no license grant pending the Harvester 2.x authorship audit.

The default branch is protected against force pushes and deletion, requires pull
requests and resolved conversations, and uses squash merge. Secret scanning and push
protection are enabled.

## Consequences

- The public URL is retained without retaining the contaminated Git history.
- Legacy tags and commits exist only in private evidence and the consolidated local
  legacy checkout.
- Future work must enter through short branches and pull requests.
- A license and CI-required status checks will be added only after their respective
  audits and workflows are ready.
