# Meowify 喵~

[简体中文](README-zh.md) | **English**

Appends 「 喵~」 to item names in Minecraft.

- Hover over an item in your inventory: the **item name** on the first line of the tooltip gets 「 喵~」 appended.
- Switch hotbar slots: the **item name** that fades out in the middle of the screen gets it too.
- With [Jade](https://modrinth.com/mod/jade) installed, the **block names, entity names and item names** in Jade's overlay get it as well.

The mod is **client-side only**: it only changes what you see on your own screen. Servers don't need it installed, and a client running it won't get a mod-list mismatch. The Jade support is **optional** too — without Jade the mod behaves exactly as before.

## Requirements

| Item | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x (47.4.10 is used for development) |
| Java | 17 |
| Side | Client only (`clientSideOnly=true`) |

## Installation

1. Get a Minecraft 1.20.1 + Forge 47.x client
2. Drop `meowify-1.1.0.jar` into `.minecraft/mods/`
3. Launch the game — no configuration needed

Dedicated servers don't need it; a client with the mod can join a server without it just fine (the mod list is treated as `IGNORE_ALL_VERSION`).

## Building from source

```bash
# The first run downloads the Forge dependencies and decompiles Minecraft, so it takes a while
./gradlew build
```

The jar ends up in `build/libs/meowify-1.1.0.jar`. On Windows, use `gradlew.bat` instead of `./gradlew`.

Compiling needs Jade's API, but only at compile time: the `downloadJade` / `jadeApi` tasks in `build.gradle` download the official Jade release from Modrinth, check its SHA-256 and unpack only the API classes into `build/jade-api-classes/` as a `compileOnly` dependency. The repository therefore contains no Jade binaries, the built jar does not bundle Jade, and Meowify never turns into "Jade is required". If Gradle can't reach the network you can place the jar in `gradle/jade/` by hand — see [`gradle/jade/README.md`](gradle/jade/README.md).

Two tasks you will use while developing:

```bash
./gradlew runClient    # launches a dev client with the mod
./gradlew runServer    # client-side mod, so it is not loaded here
```

> **Note**: `runClient` is ForgeGradle's **development environment**, and Jade only ships production jars (obfuscated to SRG names). Dropping one into `run/mods/` crashes on the SRG/official mapping mismatch (Jade raises `NoSuchMethodError`). To actually test the Jade integration, put `build/libs/meowify-1.1.0.jar` and Jade into a real 1.20.1 Forge client.

## How it works

The mod has three hooks, one for each of the display locations above.

### 1. Item tooltip — a Forge event

`MeowifyEventHandler` listens for `ItemTooltipEvent` and replaces line 0 of the tooltip (the item name) with "name + suffix":

```java
event.getToolTip().set(0, MeowifyText.appendSuffix(originalName));
```

### 2. Hotbar switch overlay — a Mixin injection

The item name that fades out in the middle of the screen is drawn directly by `Gui#renderSelectedItemName`, and Forge has no event for it, so it is handled with a Mixin:

```java
@ModifyVariable(
        method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V",
        remap = false,
        at = @At(
                value = "INVOKE_ASSIGN",
                target = "Lnet/minecraft/world/item/ItemStack;getHighlightTip(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"
        )
)
```

A few pitfalls worth writing down, so they don't have to be rediscovered:

- The text that actually gets drawn comes from `ItemStack#getHighlightTip`, so hooking that assignment keeps the injection independent of local variable slot numbers and stops it from hitting the method's other `Component` values by accident.
- The overload that takes `yShift` is **a method Forge adds itself and it has no SRG name**, so the annotation processor fails with `Unable to locate obfuscation mapping` unless `remap = false` is spelled out.
- The "just rename the stack's hover name" trick does not work: `Gui` compares `getHoverName()` every tick to decide whether the player switched items, so a renamed stack keeps resetting the fade timer and the overlay would never disappear.
- `defaultRequire = 1`: a failed injection throws instead of quietly doing nothing.

The suffix is built by a shared helper (`MeowifyText.appendSuffix`) and inherits the style of the name it follows (rarity colour; italic for renamed items).

### 3. Jade overlay — Jade's tooltip callback

Jade draws its overlay with its own pipeline: it does not go through the vanilla tooltip path that fires `ItemTooltipEvent`, and it is not affected by the Mixin inside `Gui`. So this hook uses Jade's plugin API:

```java
@WailaPlugin
public class MeowifyJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(MeowifyJadePlugin::appendSuffixToNames);
    }
}
```

`addTooltipCollectedCallback` fires after every provider has written into the tooltip and before the overlay is rendered, so we walk the elements of each line and replace the "name line" with "name + suffix". Jade builds a new tooltip object every client tick, so the suffix can never pile up.

Deciding which line is the name uses two signals:

- **Title line** (block name / entity name): Jade registers its `ObjectNameProvider` under the provider uid `Identifiers.CORE_OBJECT_NAME`, and `Tooltip#add` tags elements with the uid of the provider that is currently running, so we match on that tag exactly. This covers block names, entity names and every special case Jade itself handles (harvest results, custom names, dropped-item entities, item/block display entities) without reimplementing Jade's naming logic.
- **Inventory content lines** (chest, furnace contents, ...): Jade draws the item name as a line it builds itself, like `"12× Stone"`, and that line is **not tagged**. The only handle is the `× ` separator pattern, with the extra requirement that no further `× ` appears after the first one — because an item name can itself contain `× `, and rewriting it repeatedly would keep appending more suffixes.

Two Jade internals had to be relied on; both are validated against the jar at compile time:

- `snownee.jade.impl.ui.TextElement`'s `public final FormattedText text` is a public field, which is how the component is read and a new element is built;
- `snownee.jade.impl.Tooltip`'s `public final List<Line> lines` is a public field too, but the two lists inside `Line` are private, so the only way in is to take a reference to the internal list via `ITooltip#get(int, Align)` and replace the element in place.

`MeowifyJadePlugin` is only loaded when Jade is installed: Jade finds plugins by scanning for the `@WailaPlugin` annotation, so nothing has to be declared in `mods.toml`; without Jade the class is never loaded and Jade never becomes a required dependency.

## Project layout

```
src/main/java/com/qxia/MeowifyMod/
├── MeowifyMod.java          # @Mod entry point, registers the event bus
├── MeowifyEventHandler.java # ItemTooltipEvent: item tooltips
├── MeowifyText.java         # the shared 「 喵~」 suffix and concatenation logic
├── mixin/GuiMixin.java      # injection for the hotbar switch overlay
└── compat/jade/
    └── MeowifyJadePlugin.java  # block/entity/item names in Jade's overlay (loaded only with Jade)
src/main/resources/
├── META-INF/mods.toml       # mod metadata, clientSideOnly=true
├── meowify.mixins.json      # mixin config (client only)
└── pack.mcmeta
gradle/jade/
└── README.md                # the downloadJade/jadeApi tasks and how to stage the jar when offline
```

## Development notes

- **Do not write non-ASCII text directly into `gradle.properties`.** Gradle reads that file as ISO-8859-1, so non-ASCII characters have to be written as `\uXXXX` escapes: `\u55b5` is 「喵」.
- `processResources` pins `filteringCharset = 'UTF-8'`; without it the generated `mods.toml` would be written in the platform encoding (GBK on a Chinese Windows) and show up as mojibake in game.
- These two warnings in a development-environment log are harmless and common with 1.20.1 + MixinGradle:
  - `Compatibility level JAVA_17 ... higher than the maximum level supported by this version of mixin (JAVA_13)`
  - `Reference map 'meowify.refmap.json' ... could not be read` (in dev the refmap only exists inside the jar)
- The mod has no configuration file at all; its behaviour is hard-coded.
- Jade's API is **compile-only**: `build.gradle` pulls it in with `compileOnly`, so it is never bundled into `meowify-*.jar` and never makes Meowify require Jade. When upgrading Jade, update `jade_version` in `gradle.properties` and `jadeSha256` in `build.gradle` together.
- Because Jade's `snownee.jade.impl.ui.TextElement` is an internal class rather than a regular API, the `jadeApi` task unpacks `snownee/jade/impl/ui/TextElement.class` and `snownee/jade/impl/Tooltip*.class` on top of the API classes so they resolve at compile time.

## License

This project is released under the **MIT license**; the full text is in [LICENSE.md](LICENSE.md).

- You may use, modify and redistribute it freely, including commercially, as long as the copyright and license notices are kept.
- The `license` field in the mod metadata (which comes from `mod_license` in `gradle.properties`) is MIT as well, and the built jar bundles a copy of the license file.
- The build scripts are based on the **Forge MDK** template, and `gradlew` / `gradle-wrapper.jar` come from the Gradle project; both stay under their respective upstream licenses (Forge: LGPL-2.1, Gradle: Apache-2.0).

Copyright (c) 2026 Q-Ghast
