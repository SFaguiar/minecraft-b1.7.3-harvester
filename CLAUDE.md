# minecraft-b1.7.3-harvester — Claude Code

@AGENTS.md

Plataforma pinada: Minecraft Beta 1.7.3, Babric, Fabric Loader `0.18.4`,
StationAPI `2.0.0-alpha.6.2`, Java 17, Gradle Wrapper `8.12.1`, mappings
`b1.7.3+e1fe071`. Versões exatas — nunca `latest`/`+`/`SNAPSHOT`.

## Build e teste

- Use sempre o JDK 17 pinado
  (`C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`), nunca o
  Java global da máquina. `.claude/settings.local.json` na raiz do
  workspace já define `JAVA_HOME` para os comandos deste agente.
- Rode `.\gradlew.bat clean test classes --console=plain` antes de
  reportar qualquer alteração de código como concluída.
- Nunca execute `runClient`/`runServer` sem pedido explícito para a
  tarefa em questão.

## Estado real

Não repita gates já concluídos sem uma causa de regressão documentada.

- Implementados e validados por build/testes automatizados e por gate
  manual completo (singleplayer e multiplayer, servidor autoritativo):
  keybinding de ativação e de configuração, descoberta BFS configurável
  por categoria (troncos, minérios, terra, cascalho, folhas,
  plantações), consolidação de drops na origem, tela de configuração
  in-game, e persistência de configuração.
- Estado canônico completo (mantido no repositório de governança, não
  neste): `better-beta-program/docs/operations/CURRENT_STATE.md`.

## Regras de código

Java/Mixin/StationAPI: seguem `.claude/rules/harvester-java.md` na raiz
do workspace, carregado automaticamente ao editar arquivos deste
repositório. Padrões completos — leia sob demanda, não pré-carregados:
`../docs/ENGINEERING_STANDARDS.md`, `../docs/MODDING_STANDARDS.md`.

## Pesquisa técnica

Antes de inferir comportamento de Minecraft/Babric/StationAPI/Fabric
Loader/Mixin, consulte (sob demanda, não pré-carregados):
`../better-beta-program/docs/sources/TECHNICAL_REFERENCE_INDEX.md`,
`../better-beta-program/docs/sources/TECHNICAL_RESEARCH_PROTOCOL.md`,
`../better-beta-program/docs/sources/MAPPING_GUIDE.md`. Não extrapole
documentação moderna do Fabric para o Beta 1.7.3 sem confirmação no
código mapeado pinado.
