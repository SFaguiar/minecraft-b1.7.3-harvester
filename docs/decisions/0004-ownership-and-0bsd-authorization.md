# ADR 0004: Ownership declaration and 0BSD authorization for Harvester's own content

- Status: Accepted
- Date: 2026-07-24

Samuel Figueira Aguiar, as owner and maintainer of this project, made the
following declaration on 2026-07-24, after an independent technical audit
covering the repository's full Git history (all 30 commits, not only the
current tree): sole authorship confirmed across every commit; no
decompiler, MCP-obfuscation, or Mojang-copyright markers found in any
commit ever made; no embedded third-party license or copyright headers
other than the standard Apache-2.0 Gradle Wrapper; no Mojang assets
present in resources; the runtime dependency license inventory in
`docs/LICENSE_AUDIT.md` verified empirically by inspecting the remapped
distributable JAR for bundled third-party classes. Supporting commits:
`dfe763e` (documentation staleness corrections) and `c9f7dfb` (dependency
license inventory close-out).

> Eu, Samuel Figueira Aguiar, na qualidade de proprietário e mantenedor do
> projeto Harvester, declaro que, até onde sei e com base na auditoria
> técnica realizada sobre todo o histórico Git e a árvore atual do
> projeto, o código novo do Harvester foi desenvolvido de forma
> independente, sem incorporação de código legado, decompilado ou de
> terceiros sem licença compatível.
>
> Declaro que detenho ou controlo os direitos necessários sobre o código
> próprio e demais contribuições autorais do projeto, inclusive as
> produzidas com auxílio de ferramentas de inteligência artificial sob
> minha direção, seleção, revisão e integração, na medida permitida pela
> legislação aplicável.
>
> Autorizo a distribuição desse conteúdo próprio sob a licença 0BSD.
>
> Dependências, ferramentas, APIs, mappings, Gradle Wrapper e quaisquer
> outros componentes de terceiros permanecem sujeitos às suas respectivas
> licenças e não são relicenciados por esta declaração.

This resolves the authorship-audit gate that previously blocked
relicensing (`LicenseRef-Harvester-Audit-Pending`). The declaration is
recorded here as the durable decision; it does not by itself change any
license-bearing file. Adding the `LICENSE` file, updating
`fabric.mod.json`'s `license` field, and updating `README.md`'s
license-status section are deferred — by the owner's own choice — to the
1.0.0 release commit, rather than applied separately now.
