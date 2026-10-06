# Meowify

[简体中文](README-zh.md) | **English**

Appends a cat noise to item names in Minecraft. The text follows the language you picked in-game, so it reads 「 喵~」 in Chinese, ` meow~` in English, ` miaou~` in French, and so on.

- Hover over an item in your inventory: the **item name** on the first line of the tooltip gets the suffix appended.
- Switch hotbar slots: the **item name** that fades out in the middle of the screen gets it too.
- With [Jade](https://modrinth.com/mod/jade) installed, the **block names, entity names and item names** in Jade's overlay get it as well.

The mod is **client-side only**: it only changes what you see on your own screen. Servers don't need it installed, and a client running it won't get a mod-list mismatch. The Jade support is **optional** too — without Jade the mod behaves exactly as before.

## Languages

The suffix is a normal translation key (`meowify.suffix`), so the game picks it from your selected
language. Supported out of the box:

| Language | Shows |
| --- | --- |
| English (`en_us`) | ` meow~` |
| 简体中文 (`zh_cn`) | ` 喵~` |
| Français (`fr_fr`) | ` miaou~` |
| Español (`es_es`) | ` miau~` |
| Português (`pt_br`) | ` miau~` |
| Русский (`ru_ru`) | ` мяу~` |
| 日本語 (`ja_jp`) | ` ニャー~` |

Any other language falls back to English. To add one, drop a file into
`src/main/resources/assets/meowify/lang/` named after the locale code, for example `de_de.json`:

```json
{
  "meowify.suffix": " miau~"
}
```

No code change is needed — the file is picked up automatically. Resource packs can override these
files too.

## Configuration

There is an in-game screen for all three options: open **Mods** from the main menu, select **Meowify**
and press the **Config** button. The same values live in `config/meowify-client.toml`, which the mod
writes on first launch, so you can edit that file by hand instead:

```toml
#Append the suffix to the block, entity and item names in Jade's overlay.
#Only has an effect when Jade is installed.
enableJadeSuffix = true
#Append the suffix to item names in the inventory tooltip and in the item name
#that fades out in the middle of the screen when you switch hotbar slots.
enableGlobalSuffix = true
#Use this text instead of the translated suffix that ships in
#assets/meowify/lang/<locale>.json. Leave it empty to keep the translated one,
#which follows the language selected in game.
#The value is a literal string, not a translation key, so it is used verbatim in
#every language. Example: customSuffix = "meow~" renders as "Stone meow~".
customSuffix = ""
```

| Option | Default | Effect |
| --- | --- | --- |
| `enableGlobalSuffix` | `true` | The inventory tooltip and the hotbar switch overlay. Turning it off leaves those names untouched. |
| `enableJadeSuffix` | `true` | The names in Jade's overlay. Independent of the option above, so you can keep Jade only or vanilla only. |
| `customSuffix` | `""` | Replaces the translated text with a literal string. Empty keeps the per-language text. |

Notes:

- The toggles are saved the moment you click them; the custom suffix field is saved when you press
  **Done**. Both write straight to the config file, so there is no separate "apply".
- With `customSuffix` set, every language shows that same text — it is a literal, not a translation
  key. Surrounding whitespace is trimmed and one space is inserted automatically, so `"meow~"` and
  `" meow~"` both render as `Stone meow~`.
- The two toggles are independent: `enableGlobalSuffix = false` with `enableJadeSuffix = true` gives
  you the suffix in Jade only.
- Both default to `true`, so updating from an older version changes nothing until you edit them.
- If you want a different suffix *per language* rather than one literal everywhere, leave
  `customSuffix` empty and edit the language files instead — see above.

## Requirements

| Item | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x (47.4.10 is used for development) |
| Java | 17 |
| Side | Client only (`clientSideOnly=true`) |

## Installation

1. Get a Minecraft 1.20.1 + Forge 47.x client
2. Drop `meowify-1.2.0.jar` into `.minecraft/mods/`
3. Launch the game — no configuration needed

Dedicated servers don't need it; a client with the mod can join a server without it just fine (the mod list is treated as `IGNORE_ALL_VERSION`).

## Building from source

```bash
# The first run downloads the Forge dependencies and decompiles Minecraft, so it takes a while
./gradlew build
```

The jar ends up in `build/libs/meowify-1.2.0.jar`. On Windows, use `gradlew.bat` instead of `./gradlew`.

Compiling needs Jade's API, but only at compile time: the `downloadJade` / `jadeApi` tasks in `build.gradle` download the official Jade release from Modrinth, check its SHA-256 and unpack only the API classes into `build/jade-api-classes/` as a `compileOnly` dependency. The repository therefore contains no Jade binaries, the built jar does not bundle Jade, and Meowify never turns into "Jade is required". If Gradle can't reach the network you can place the jar in `gradle/jade/` by hand — see [`gradle/jade/README.md`](gradle/jade/README.md).

Two tasks you will use while developing:

```bash
./gradlew runClient    # launches a dev client with the mod
./gradlew runServer    # client-side mod, so it is not loaded here
```

> **Note**: `runClient` is ForgeGradle's **development environment**, and Jade only ships production jars (obfuscated to SRG names). Dropping one into `run/mods/` crashes on the SRG/official mapping mismatch (Jade raises `NoSuchMethodError`). To actually test the Jade integration, put `build/libs/meowify-1.2.0.jar` and Jade into a real 1.20.1 Forge client.

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

The suffix is built by a shared helper (`MeowifyText.appendSuffix`), which appends the suffix and copies the style of the name it follows (rarity colour; italic for renamed items).

### The suffix text

`MeowifyText` does not hard-code any text. By default it appends `Component.translatable("meowify.suffix")`, so the string is resolved by the client from `assets/meowify/lang/<locale>.json` every time it is rendered — switching the language in-game re-renders the suffix immediately, with no restart. When `customSuffix` is set in the config, a `Component.literal` is used instead, so the text is the same in every language. Either way the style attached to the component is copied from the name, so the suffix keeps the item's rarity colour.

### 3. Jade overlay — Jade's tooltip callback

Jade draws its overlay with its own pipeline: it does not go through the vanilla tooltip path that fires `ItemTooltipEvent`, and it is not affected by the Mixin inside `Gui`. So this hook uses Jade's plugin API:

```java
@WailaPlugin
public class MeowifyJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(new SuffixApplier());
    }
}
```

`addTooltipCollectedCallback` fires after every provider has written into the tooltip and before the overlay is rendered, so we walk the elements of each line and replace the "name line" with "name + suffix". Jade builds a new tooltip object every client tick, so the suffix can never pile up. The callback route is also what keeps `enableJadeSuffix` from interfering with the vanilla locations: it only ever touches Jade's own tooltip.

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
├── MeowifyMod.java          # @Mod entry point, registers the event bus and the config
├── MeowifyConfig.java       # the client config: the two toggles and customSuffix
├── MeowifyConfigScreen.java # the in-game screen behind the mod list's Config button
├── MeowifyEventHandler.java # ItemTooltipEvent: item tooltips
├── MeowifyText.java         # the shared suffix helper (translation key, custom text, toggles)
├── mixin/GuiMixin.java      # injection for the hotbar switch overlay
└── compat/jade/
    └── MeowifyJadePlugin.java  # block/entity/item names in Jade's overlay (loaded only with Jade)
src/main/resources/
├── META-INF/mods.toml       # mod metadata, clientSideOnly=true
├── assets/meowify/lang/     # the suffix in each supported language
│   ├── en_us.json           #   "  meow~"
│   ├── zh_cn.json           #   "  喵~"
│   ├── fr_fr.json           #   "  miaou~"
│   ├── es_es.json           #   "  miau~"
│   ├── pt_br.json           #   "  miau~"
│   ├── ru_ru.json           #   "  мяу~"
│   └── ja_jp.json           #   "  ニャー~"
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
- The config lives in `MeowifyConfig` (`config/meowify-client.toml`). It is registered as `ModConfig.Type.CLIENT` from the mod constructor, which is why Forge lists it in the mod list's config screen. The generated file is written in UTF-8, so a non-ASCII `customSuffix` round trips correctly.
- Forge greys out the mod list's Config button unless the mod registers a screen factory, so `MeowifyMod` also calls `MinecraftForge.registerConfigScreen(...)`. Without that call the button would be dead even though the config file itself is registered; `meowify-client.toml` would still work, but only by hand.
- `MeowifyConfigScreen` is a plain `Screen` on purpose. The vanilla options screens are built around `OptionInstance` buttons and an `OptionsList` that cannot hold an `EditBox`, so placing three controls directly was both shorter and clearer than bending those classes.
- Toggling an option calls `ConfigValue#set`, which Forge persists immediately. The custom suffix text is only written when the screen closes, so typing does not rewrite the file per keystroke.
- The language files under `assets/meowify/lang/` must be **valid UTF-8 JSON without a BOM** and may contain non-ASCII text directly (`喵`, `мяу`, `ニャー`). `processResources` filters only `mods.toml` and `pack.mcmeta`, so these files are copied byte for byte.
- Jade's API is **compile-only**: `build.gradle` pulls it in with `compileOnly`, so it is never bundled into `meowify-*.jar` and never makes Meowify require Jade. When upgrading Jade, update `jade_version` in `gradle.properties` and `jadeSha256` in `build.gradle` together.
- Because Jade's `snownee.jade.impl.ui.TextElement` is an internal class rather than a regular API, the `jadeApi` task unpacks `snownee/jade/impl/ui/TextElement.class` and `snownee/jade/impl/Tooltip*.class` on top of the API classes so they resolve at compile time.

## License

This project is released under the **MIT license**; the full text is in [LICENSE.md](LICENSE.md).

- You may use, modify and redistribute it freely, including commercially, as long as the copyright and license notices are kept.
- The `license` field in the mod metadata (which comes from `mod_license` in `gradle.properties`) is MIT as well, and the built jar bundles a copy of the license file.
- The build scripts are based on the **Forge MDK** template, and `gradlew` / `gradle-wrapper.jar` come from the Gradle project; both stay under their respective upstream licenses (Forge: LGPL-2.1, Gradle: Apache-2.0).

Copyright (c) 2026 Q-Ghast
