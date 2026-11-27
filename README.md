# Hammer

A standalone Fabric and NeoForge mod for Minecraft 26.1 and 26.3, built with Java 25. Install the Hammer jar for your Minecraft version and loader on the server and client. Fabric installations also need Fabric API. Find the Hammer in Tools & Utilities or use `/give @s hammer:hammer`. Hammer items, ability components and projectiles use the `hammer` namespace. Saves from older releases reference different IDs, so back up those worlds before updating. The split FloraFauna mod (0.0.2 or later) is optional.

## Build and test

Stonecutter builds four targets from this checkout, with 26.1 NeoForge as the editable default. Gameplay and UI live in `src/main`. Loader entrypoints, event adapters, and loader-specific resources live in `src/fabric` and `src/neoforge`. ModDevGradle builds the NeoForge targets and Loom builds the Fabric targets. Stonecutter evaluates loader conditions such as `//? if neoforge {` and Minecraft version conditions such as `//? if <26.3 {` before Java compilation. For example, both loaders use the same mining-speed calculation, with a NeoForge break-speed event and a Fabric mixin calling it.

```sh
./gradlew :26.1-fabric:build :26.1-fabric:runGameTestServer
./gradlew :26.3-fabric:build :26.3-fabric:runGameTestServer
./gradlew :26.1-neoforge:build :26.1-neoforge:runGameTestServer
./gradlew :26.3-neoforge:build :26.3-neoforge:runGameTestServer
```

Use `gradlew.bat` on Windows. Each target writes `hammer-<minecraft>-<loader>-<mod-version>.jar` to `versions/<minecraft>-<loader>/build/libs/`. Set the Hammer version for all targets with `mod_version` in the root `gradle.properties`. To run a single server test, pass `-PgameTestFilter=hammer:hammer_tunnel_gaps` to the version's GameTest task.

For client checks, run `./scripts/test_hammer.ps1 -Target 26.1 -SourceWorld <closed-save-directory>` in PowerShell 7. Add `-Loader fabric` to run against Fabric. Choose a save from the same Minecraft version. Both targets support the quarry and stair-outline scenarios. You can also select `-Scenario powers`, `radial`, or `progression`. Disposable saves, screenshots and logs are under `.tmp/world-playtests/`.

With FloraFauna's Stonecutter companion support, run `./gradlew -PdevMods=all :26.3-neoforge:runClient` from FloraFauna to play with all three mods. Use `:26.1-neoforge:runClient` for 26.1. Pass `-PhammerDir=<checkout>` and `-PfrenchiesDir=<checkout>` when using feature worktrees.

## Survival progression

Craft one Hammer and fit its two cores in order. In the Smithing Table, leave the template slot empty, put the Hammer in the middle and the core on the right. The upgrade carries forward the Hammer’s name, tool settings, favorites and enchantments. All three stages are available directly in Creative under Tools & Utilities.

| Craft | Top row | Middle row | Bottom row |
|-------|---------|------------|------------|
| Forged Hammer | Iron ingot · Diamond · Iron ingot | Iron ingot · Stick · Iron ingot | Centered stick |
| Resonance Core | Centered amethyst shard | Redstone dust · Diamond · Redstone dust | Centered Nether quartz |
| Tempest Core | Centered blaze rod | Eye of Ender · Netherite ingot · Eye of Ender | Centered blaze rod |

| Stage | Charge capacity | Newly available |
|-------|-----------------|-----------------|
| Forged | None | Single Block, 3×3, Walkable Tunnel, Bore, both staircases, Strip, Path, Till and normal throws. Includes mining previews, lava warnings, auto-torches and the Hammer settings. |
| Resonant | 64 | 5×5, 7×7, Large Tunnel, Vein, Seismic Scan and charged ore throws. The first core starts fully charged. |
| Tempest | 96 | Lightning, Ground Slam, Surface Teleport and Instabreak. Existing charge carries over. |

Hold **C** to inspect the stage and hover over a locked wedge to see which core it needs. Tap **C** to cycle available shapes or favorites. Double-tap **C** for Single Block at any stage.

All three stages use the original Hammer model and textures. The Hammer name and Hold-C menu show its stage. Powered Hammers show their charge in the tooltip and on the item bar.

## Data Components

| Component | Purpose |
|-----------|---------|
| `MULTI_BLOCK_MINING` | Selected mining shape, pattern parameters and favorite shapes |
| `TOOL_CONFIG` | Enchantment, mining speed, vein matching and auto torches |
| `LIGHTNING_ABILITY` | Spawn lightning at target |
| `TELEPORT_SURFACE_ABILITY` | Teleport to surface |
| `THROWABLE_ABILITY` | Throw item as projectile |
| `HAMMER_ACTION` | Selected right-click action |
| `HAMMER_STAGE` | Forged, Resonant or Tempest |
| `HAMMER_CHARGE` | Shared charge for ore strikes, Seismic Scan and Ground Slam |
| `RIGHT_CLICK_ACTION` | Fallback ability for Auto |
| `MULTI_TOOL_ABILITY` | Enables axe/shovel/hoe actions (strip, path, till) |

## Mining Shapes

| Shape | Pattern | Radius |
|-------|---------|--------|
| SINGLE | 1×1 | 0 |
| FLAT_3X3 | 3×3 plane | 1 |
| FLAT_5X5 | 5×5 plane | 2 |
| FLAT_7X7 | 7×7 plane | 3 |
| SHAPELESS | Vein mining | 1 |
| TUNNEL_UP | Staircase up | 0 |
| TUNNEL_DOWN | Staircase down | 0 |
| WALKABLE_TUNNEL | 1 wide × 3 high × 5 deep | 0 |
| LARGE_TUNNEL | 3 wide × 3 high × 5 deep | 0 |
| BORE | 1 wide × 1 high × 7 deep | 0 |

## Mining Speed

| Speed | Break Speed | Description |
|-------|-------------|-------------|
| STANDARD | Original | Normal mining speed |
| EFFICIENCY | 35.0 | Fast mining |
| INSTABREAK | 100.0 | Instant block break |

## Tool Config

Each Hammer remembers its settings.

| Setting | Options | Default |
|---------|---------|---------|
| Enchantment | Fortune III / Silk Touch | Fortune III |
| Mining Speed | Standard / Efficiency / Instabreak | Efficiency |
| Vein matching | Same ore / Exact block | Same ore |
| Auto torches | On / Off | Off |

**Note**: Fortune and Silk Touch are mutually exclusive - exactly one is always active.

## Hammer controls

| Input | Action |
|-------|--------|
| Tap **C** | Cycle mining shapes in their usual order. With favorites, cycle only the starred shapes. |
| Double-tap **C** within 250 ms | Select Single Block. This keeps your favorites and stays selected until you change modes. |
| Hold **C** for 300 ms | Open both Hammer wheels side by side. Keep **C** held while choosing. |
| Right-click on the ground | Use the selected ability. |
| Right-click off the ground | Attempt a Kinetic Ground Slam. |
| **R** / **M** | Call lightning / teleport to the surface with the corresponding ability. |
| Tap / hold **T** | Throw / charge an ore-mining throw. Release to throw. |

While holding **C**, click a mining shape on the left and a grounded ability on the right, in either order. Each click sets that wheel immediately. Hover highlights a wedge. Release **C** to close the wheels and keep your choices. You can change just one wheel or close both without changing either. The right wheel’s center switches to **Settings** and back. Use **Tab** to reach either wheel, arrow keys to move around it, and **Enter** to select. Press **Esc** to close.

The left wheel shows all ten mining shapes and enables the ones available at the current stage. Right-click its wedge or press **F** on it to toggle its favorite star. A new Hammer has no favorites. Removing its last star restores the full cycle. Selecting an unstarred shape leaves the stars alone. The next tap advances to the next starred shape in the usual order, wrapping as needed.

In **Abilities**, select Auto, Strip, Path, Till, Throw, Lightning, Surface Teleport or Seismic Scan. Auto tries enabled block transformations first and then the configured fallback, which is Throw on a new Hammer. Strip, Path and Till leave the usual block interaction available when the targeted block cannot be transformed. In **Settings**, change enchantment, mining speed, vein matching or auto torches. Each Hammer carries its own selections.

### Player action checks

Use the [client runner](#build-and-test) with the indicated `-Scenario`, target, loader and a closed save for that Minecraft version.

- **Choose both wheels in one hold** (`-Scenario radial`). With Seismic Scan unlocked, hold **C**, click Walkable Tunnel on the left, click Seismic Scan on the right, hover over Throw, then release. The wheels close and the held Hammer keeps Walkable and Scan on the server. The scripted client remaps **C** to **V** and also checks the reverse click order and a hover-only release.
- **Preview all nine blocks before a swing** (`-Scenario hammer`, GameTest `hammer:hammer_flat_3x3_all_faces`). Select 3×3 and aim at the center of a stone wall. The scripted client checks violet lines on the eight neighboring outlines and along the seams around the targeted block before mining. The headless test checks the nine planned positions and the eventual cut for every face.
- **Match the descending stair preview across air** (`-Scenario stair-outline`). Start mining the wall face, then aim at the top of the same block during the swing. The client preview must retain the five original wall-facing tread positions across the gap. The server must place a north-facing cobblestone stair at each previewed position. To check the visible teal outline through rock, use `-Scenario hammer`. Its quarry also checks the five treads, cleared headroom and intact stone around the cut.
- **Cut across a two-column cave** (GameTest `hammer:hammer_tunnel_gaps`). Strike the wall in ascending and descending stair modes. The swing bridges the two empty columns, preserves bedrock in its path and ends at the fifth column. This headless case checks the resulting blocks. Use the client checks above for the preview.

## Seismic Scan

Select **Seismic Scan**, stand on the ground and right-click. Chests, doors and other usable blocks still take their normal interaction. A violet pulse searches loaded terrain within 24 blocks for tagged ores. Up to 24 approximate deposit markers appear for the scanning player and fade after five seconds. Empty scans also spend 16 charge. The cooldown is eight seconds.

The server scans the sphere in slices shared across players. It checks the common `c:ores` tag, groups face-connected ore blocks, and rounds each deposit center to a two-block grid before sending it to the owner. Scan radius, duration, cost, cooldown, and marker count are constants in `SeismicScanner`.

## Kinetic Ground Slam

Jump or fall with the Hammer in hand and right-click to drive downward. Landing sends a shockwave up to 4.5 blocks, dealing up to 4 HP of damage and pushing nearby hostiles away. Damage and knockback decrease with distance. The impact preserves terrain and spares friendly mobs and other players.

A slam costs 12 charge and has a five-second cooldown. A failed airborne attempt consumes the click. Swimming, climbing, flying, gliding, riding and spectating prevent activation. Keep the same Hammer equipped through impact. The slam cushions two blocks of normal fall distance, so a jump is safe and a long fall can still hurt. Combat and cooldown values are constants in `GroundSlam`.

## Multi-Block Mining

When breaking a block with `MULTI_BLOCK_MINING` component:

1. Calculate pattern based on current shape and facing direction
2. Render block outline preview on client
3. On break, destroy all blocks in pattern
4. Collect drops through the normal player mining and loot callbacks. A canceled center break leaves all neighbors intact. Each extra block gets its own protection checks and break callback.

Flat planes follow the original hit face. Shapeless mining follows matching blocks through faces, edges and corners and defaults to 64 blocks, including the center. Existing component values are bounded to a radius of 0–3 and a vein limit of 1–64.

Instabreak sets mining speed to 100. Block hardness still determines how long a strike takes.

Straight cuts advance into the face you strike. On a wall, strike the bottom of the passage for Walkable Tunnel, the center for Large Tunnel, or the entry point for Bore. On a floor or ceiling, the cut advances vertically. Walkable Tunnel centers its three-block slot along your horizontal heading. Air gaps count toward depth. All three shapes leave the floor as mined.

Stair modes cut five columns with five blocks of clearance per column before placing the tread. A side hit builds ascending stairs from one block below the center, or descending stairs from two below. A floor hit in descending mode follows the player's horizontal heading. Ceiling hits and ascending floor hits mine just the center. Teal stair outlines show planned placements through rock and across air gaps before you strike.

Violet outlines show the additional blocks the Hammer will break. Orange outlines mark the faces of planned mining blocks that touch source or flowing lava. Check the marked side, ceiling, floor or end wall before striking. Selecting Single Block narrows this warning to the targeted block. The warning leaves the final decision to the player.

Shapeless mode groups matching stone and deepslate ore variants. An iron vein can cross the stone–deepslate boundary. Copper touching that vein stays in place. Select **Exact block** in the Hammer settings to stop at the variant boundary.

After a successful center hit, flat and tunnel swings skip air and continue through their usual footprint. Both stair directions place a cobblestone tread across an existing air gap at the planned floor height. The flight still ends at the fifth column. Vein mining follows matching neighbors within its configured radius.

## Tunnel Lighting

Enable **Auto torches** in the held Hammer’s **Settings** wheel (hold **C**). After cutting a horizontal Walkable or Large Tunnel, or an ascending or descending staircase, it can place one regular torch on the right wall in the direction of travel. It checks the newly opened passage for light level 7 or below and a solid attachment. It uses the offhand torch stack first, then searches the inventory. Survival consumes one torch on successful placement. Creative requires a torch and keeps the stack.

## Charged Hammer Throw

Aim at an exposed ore block and hold the throw key until the ready message appears (20 server ticks), then release. On impact the Hammer mines connected ore using its own Fortune or Silk Touch, vein matching and block limit, then returns. Stone or a denied center hit spends no charge. Other ore types touching the vein stay in place. Each ore block actually removed costs one charge. Ordinary swings and quick throws are free.

Powered Hammers recover one charge per second while held in either hand, up to 64 for Resonant or 96 for Tempest. The inventory bar and tooltip show the remaining charge.

## Right-Click Action

In Auto, the `RIGHT_CLICK_ACTION` component configures the fallback after block transformations. It stores an Identifier referencing one of the ability components.

| Ability ID | Effect |
|------------|--------|
| `hammer:throwable_ability` | Throw the item as a projectile |
| `hammer:multi_block_mining` | Cycle mining mode |
| `hammer:lightning_ability` | Spawn lightning at target block |
| `hammer:teleport_surface_ability` | Teleport to surface |

## Multi-Tool Ability

The `MULTI_TOOL_ABILITY` component enables tool modifications on right-click:

| Setting | Effect |
|---------|--------|
| `strip` | Strip logs/wood (like axe) |
| `flatten` | Create grass paths (like shovel) |
| `till` | Till dirt to farmland (like hoe) |

**Priority**: Multi-tool actions take priority over `RIGHT_CLICK_ACTION`. If a block can be modified (stripped, pathed, or tilled), only that action occurs. Otherwise, the right-click ability executes.

**Note**: On NeoForge 26.1, the multi-tool ability requires the item to override `canPerformAction()` to declare support for the tool actions. Currently only `HammerItem` implements this - other items with the component will not perform tool modifications until they add the override.

## Adding Abilities to Items

```java
// In item registration
ItemStack stack = new ItemStack(myItem);
stack.set(HammerRegistry.MULTI_BLOCK_MINING.get(), MiningModeData.DEFAULT);
stack.set(HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT);
stack.set(HammerRegistry.LIGHTNING_ABILITY.get(), Unit.INSTANCE);
stack.set(HammerRegistry.RIGHT_CLICK_ACTION.get(), new RightClickAction(Identifier.fromNamespaceAndPath("hammer", "throwable_ability")));
stack.set(HammerRegistry.MULTI_TOOL_ABILITY.get(), MultiToolAbilityData.DEFAULT);
```

## Event Handlers

Mining uses a server game-mode mixin for the completed center break. Other abilities use loader-specific events and mixins. The table lists the NeoForge hooks.

| Event | Handler |
|-------|---------|
| Server player mining action | Retain the hit face and complete multi-block mining once the center is removed |
| `PlayerEvent.BreakSpeed` | Mining speed modification |
| `PlayerInteractEvent.LeftClickBlock` | Visual feedback |
| `PlayerInteractEvent.RightClickBlock` | Right-click abilities and multi-tool |
| `PlayerInteractEvent.RightClickItem` | Right-click abilities (in air) |
| `ExtractBlockOutlineRenderStateEvent` | Block outline rendering |
