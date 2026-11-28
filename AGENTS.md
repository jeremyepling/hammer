# Hammer development

Use Java 25 and the checked-in Gradle wrapper. Stonecutter builds 26.1, 26.3 and the 26.4 Fabric preview from one checkout with 26.1 as the VCS version. The target directories under `versions/` pin Minecraft and loader dependencies. See the README for the opt-in 26.4 NeoForge snapshot target. Use `src/main` for shared logic and `src/fabric` or `src/neoforge` for loader adapters. Keep gameplay, recipes, progression, controls and player interfaces shared across targets. Use Stonecutter version conditions for Minecraft API changes and loader conditions for small loader-specific hooks. Use `hammer` for Hammer registry and resource IDs. NeoForge requires only Minecraft and NeoForge. Fabric requires Minecraft, Fabric Loader, and Fabric API.

Test the behavior affected by each edit. JUnit, headless GameTests and the scripted quarry or action-wheel client can overlap independent work in other worktrees. The scripted client sends input inside Minecraft. Reserve the desktop only for steps that use real input or focus.

For wheel input or stair mining across gaps, use the matching [player action check](README.md#player-action-checks) and its selector.

- For shape cycling or configuration, use `./gradlew :26.1-neoforge:test --tests '*HammerConfigTest'` or the relevant JUnit class.
- For world behavior, run one GameTest or group, for example `./gradlew "-PgameTestFilter=hammer:hammer_tunnel_gaps" :26.1-neoforge:runGameTestServer`.
- For controls, rendering and save/reload, run `./scripts/test_hammer.ps1 -Target 26.1 -SourceWorld <closed-save-directory>` under PowerShell 7. After a headless test you can use its generated `versions/26.1-neoforge/run/gameTestServer/gametestserver/gametestworld`. The runner makes a disposable copy.
- For a release or combined change, run `./gradlew :26.1-neoforge:build :26.1-neoforge:runGameTestServer` and repeat for `26.3-neoforge`, `26.1-fabric`, `26.3-fabric`, and `26.4-fabric`, or use the verified CI run for the same revision. On Windows use `gradlew.bat`. Check the selected test names and result count.
- Keep worlds, screenshots, generated test output and local settings out of source control. Commit models, tags and other shipped resources under `src/main/resources`. The client quarry fixture and automation belong to `src/playtest`.
