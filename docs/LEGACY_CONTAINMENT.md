# Legacy containment record

## Completed locally

- Verified the legacy tag and commit without changing either.
- Created and verified a complete private Git bundle.
- Created this unrelated clean Git history.
- Imported no Java source or binary from the legacy repository.
- Added an automated prohibited-content check.

## Public remote containment

Completed on 2026-07-16 without force push or tag changes:

1. removed the `Harvester.jar` asset while preserving the `v1.0.0` release and tag;
2. merged legacy pull request
   [`#1`](https://github.com/SFaguiar/minecraft-b1.7.3-harvester/pull/1);
3. removed `PlayerControllerSP.java`, `MLProp.java`, and the unsafe build script from
   the default branch;
4. added a provenance notice and prohibited-content guard to the legacy branch;
5. removed the short remediation branch after merge.

The legacy `master` merge commit is
`16c97cbcc9eb2ee8346426463c9a06565bf53fb5`. The annotated tag object remains
`7126beda6977ee7e4ef7e6804150949e349ad093` and still points to legacy commit
`186f99402541aae93ee70ae137dbee1c31ce0655`.

Objects reachable from that published tag still contain the historical sources. Full
erasure would require destructive history replacement or repository deletion, which
remains outside the approved policy. The tag and private bundle are evidence only and
must never be used as the source base or a release artifact for Harvester 2.x.
