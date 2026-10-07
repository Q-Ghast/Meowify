# Meowify

Fabric port of Meowify for Minecraft 1.21.11: appends a cat noise to item names. The text follows the
language selected in game, so it reads 「 喵~」 in Chinese, ` meow~` in English, ` miaou~` in French,
and so on.

- Hover over an item in your inventory: the **item name** on the first line of the tooltip gets the suffix.
- With [Jade](https://modrinth.com/mod/jade) installed, the **block and entity names** in Jade's overlay get it too.

The mod is **client-side only** (`"environment": "client"` in `fabric.mod.json`), so a server neither
needs it nor sees a mod-list mismatch. The Jade support is **optional**: Jade is compiled against but
never bundled, and without Jade the mod behaves exactly the same.

## Requirements

| Item | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | required |
| Java | 21 |

## Building

```bash
./gradlew build
```

The jar ends up in `build/libs/`. On Windows use `gradlew.bat`.

## Configuration

There is an in-game screen for all three options: open **Mods** from the main menu, find **Meowify** and
press the config button. That button comes from [ModMenu](https://modrinth.com/mod/modmenu) and the
screen is drawn with [Cloth Config](https://modrinth.com/mod/cloth-config); both are **optional**, and
without them the mod works exactly the same, you just edit the file by hand.

The values themselves live in `config/meowify.json`, written on first launch. Fabric has no built-in
config system, so this is a plain JSON file:

```json
{
  "enableGlobalSuffix": true,
  "enableJadeSuffix": true,
  "customSuffix": ""
}
```

| Option | Default | Effect |
| --- | --- | --- |
| `enableGlobalSuffix` | `true` | Whether the inventory tooltip gets the suffix. Turning it off leaves item names untouched and does not affect Jade's overlay. |
| `enableJadeSuffix` | `true` | Whether the block and entity names in Jade's overlay get the suffix. Independent of the option above, so you can keep Jade only or vanilla only. Only has an effect when Jade is installed. |
| `customSuffix` | `""` | Replaces the translated text with a literal string. Empty keeps the per-language text. |

Notes:

- The screen writes straight to the config file, so there is no separate "apply": toggles are saved
  when you flip them, and the custom suffix when you close the screen. Editing the file by hand still
  needs a restart, since the mod reads it once at startup.
- With `customSuffix` set, every language shows that same text — it is a literal, not a translation
  key. Surrounding whitespace is trimmed and one space is inserted automatically, so `"meow~"` and
  `" meow~"` both render as `Stone meow~`.
- `enableGlobalSuffix` defaults to `true`, so the mod does something the moment it is installed.
- If you want a different suffix *per language* rather than one literal everywhere, leave
  `customSuffix` empty and edit the language files instead — see below.

## Languages

The suffix is a normal translation key (`meowify.suffix`), so the game picks it from your selected
language:

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

No code change is needed, and resource packs can override these files too.

## How it works

The inventory tooltip is one Fabric API event:

```java
ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> appendSuffix(lines));
```

`ItemTooltipCallback` fires after the game has appended all base tooltip lines, and line 0 is the item
name, so that line is replaced with the name plus the suffix. `MeowifyText.appendSuffix` copies the
style of the name onto the suffix, so it keeps the item's rarity colour.

The text itself comes from `Component.translatable("meowify.suffix")`, resolved by the client every
time it is rendered — switching the language in game updates the suffix without a restart. When
`customSuffix` is set, a `Component.literal` is used instead.

### Jade

Jade draws its overlay with its own pipeline, so neither the tooltip event nor anything else in vanilla
reaches it. Jade instead discovers Fabric plugins through the `jade` entrypoint, so
`MeowifyJadePlugin` is listed there in `fabric.mod.json` and implements `IWailaPlugin`:

```java
@Override
public void registerClient(IWailaClientRegistration registration) {
    registration.addTooltipCollectedCallback(new SuffixApplier());
}
```

The callback fires after every provider has written into the tooltip and before it is rendered. Block
and entity names are the elements tagged with `JadeIds.CORE_OBJECT_NAME` — the uid Jade registers its
own `ObjectNameProvider` under — so matching that tag covers block names, entity names and the special
cases Jade resolves itself (picked results, custom names, item entities, item and block displays)
without reimplementing Jade's naming logic.

Two details of the Jade 21.x API shape the code:

- How an element's text is reached. `TextElementImpl#text` is private and Jade exposes no setter, so the
  plugin reads it reflectively. That field is the one `TextElementImpl#render` passes to the draw call,
  which is what makes writing it back effective.
- Why `ITooltip#replace(ResourceLocation, Component)` is *not* used. It builds a brand new element, and
  the element it leaves in the tooltip is not the one the tooltip draws from. The replacement reports
  success, is visible to every read-back of the tooltip — including a reflective read of the very field
  the render method uses — and still never reaches the screen. The plugin therefore writes that field on
  the element already in the tooltip, which keeps the element instance and its tag.

The rewrite also has to happen at collection time rather than just before the draw. Jade sizes the
tooltip while collecting it, so changing the text afterwards leaves a box measured for the plain name:
the text overflows and its end is clipped away, which for a short suffix such as ` 喵~` can look as if no
suffix was added at all.

Because the same element may be handed over more than once, the plugin remembers the component it wrote
last in a `WeakHashMap` and skips an element that already carries it, so the suffix is never appended
twice.

`appendSuffixForJade` keeps this path independent of the global toggle, so `enableJadeSuffix` alone
decides whether Jade's overlay is suffixed. The class lives in the `client` source set since Jade's
element types extend Minecraft client classes, which the `main` source set cannot see.

### Config screen

`MeowifyModMenu` is a ModMenu entrypoint, declared under `modmenu` in `fabric.mod.json`. ModMenu only
loads that entrypoint when it is installed, so nothing there runs otherwise and ModMenu stays optional.
The screen it returns is built with Cloth Config, which ModMenu already depends on:

```java
@Override
public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("meowify.config.title"))
            // ... one entry per option, each writing through to MeowifyConfig
            .build();
}
```

Each entry's save consumer calls a setter on `MeowifyConfig`, which updates the value and writes
`config/meowify.json` immediately — so the screen needs no separate apply step.

One wrinkle worth knowing if you touch the build: Cloth Config ships `cloth-basic-math` as a Fabric
*nested jar* inside its own jar. The loader unwraps nested jars in production, but Loom does not do that
for the development runtime, so `runClient` fails on `me.shedaniel.math.Rectangle` unless that nested jar
is also added. `libs/README.md` explains how it is provided here.

## Project layout

```
src/main/java/com/qxiane/
├── Meowify.java          # mod entrypoint: mod id, logger, config loading
├── MeowifyConfig.java    # the config file (config/meowify.json)
└── MeowifyText.java      # the suffix helper (translation key, custom text, toggles)
src/client/java/com/qxiane/
├── client/MeowifyClient.java       # client entrypoint: the tooltip callback
├── client/MeowifyModMenu.java      # ModMenu entrypoint: builds the Cloth Config screen
└── compat/jade/MeowifyJadePlugin.java  # Jade entrypoint (loaded only when Jade is present)
src/main/resources/
├── fabric.mod.json       # mod metadata, environment=client, the four entrypoints
└── assets/meowify/lang/  # the suffix in each supported language
libs/
├── Jade-1.21.11-Fabric-21.1.6.jar  # Jade's API, for compiling the Jade plugin
├── cloth-basic-math-0.6.1.jar      # dev runtime only, see libs/README.md
└── README.md             # where each jar comes from, and how to update them
```

## Not ported yet

The Forge version also appends the suffix to the item name that fades in the middle of the screen when
you switch hotbar slots. That is not in this port:

- **Hotbar overlay.** `Gui#renderSelectedItemName` no longer exists in 1.21.11 (`Gui` has been reduced
  to `render`, `renderDebugOverlay` and a few others), and the displayed name comes from
  `ItemStack#getHighlightTip`, which is Forge-only — vanilla 1.21.11 has `getHoverName` /
  `getStyledHoverName`. The injection point has to be re-located before this can be written.
- **Jade item-storage names.** The Forge version also suffixed the item names in a container listing
  (the `"12× Stone"` lines). Those elements are not tagged, so they cannot be found by uid; it needs a
  different approach.

## License

MIT; see [LICENSE](LICENSE).
