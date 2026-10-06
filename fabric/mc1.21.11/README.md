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

`config/meowify.json` is written on first launch. Fabric has no built-in config system, so this is a
plain JSON file:

```json
{
  "enableGlobalSuffix": true,
  "enableJadeSuffix": true,
  "customSuffix": ""
}
```

| Option | Default | Effect |
| --- | --- | --- |
| `enableGlobalSuffix` | `true` | Whether the inventory tooltip gets the suffix. Turning it off leaves item names untouched. |
| `enableJadeSuffix` | `true` | Whether the block and entity names in Jade's overlay get the suffix. Independent of the option above, so you can keep Jade only or vanilla only. Only has an effect when Jade is installed. |
| `customSuffix` | `""` | Replaces the translated text with a literal string. Empty keeps the per-language text. |

Notes:

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

- `ITooltip.get(ResourceLocation)` is typed as Minecraft's `LayoutElement`, which has no text accessor,
  so the elements are narrowed to Jade's `Element` to reach `getNarration()`. That is the only public
  way to read an element's text back; Jade offers no getter for the component a `TextElement` holds.
- `ITooltip.replace(ResourceLocation, Component)` then swaps the text in place, keeping the element's
  tag so the tooltip layout is unaffected.

Because the plugin only ever touches Jade's own tooltip, `enableJadeSuffix` cannot affect the vanilla
locations. The class lives in the `client` source set since Jade's element types extend Minecraft
client classes, which the `main` source set cannot see.

## Project layout

```
src/main/java/com/qxiane/
├── Meowify.java          # mod entrypoint: mod id, logger, config loading
├── MeowifyConfig.java    # the config file (config/meowify.json)
└── MeowifyText.java      # the suffix helper (translation key, custom text, toggles)
src/client/java/com/qxiane/
├── client/MeowifyClient.java        # client entrypoint: the tooltip callback
└── compat/jade/MeowifyJadePlugin.java  # Jade entrypoint (loaded only when Jade is present)
src/main/resources/
├── fabric.mod.json       # mod metadata, environment=client, the three entrypoints
└── assets/meowify/lang/  # the suffix in each supported language
libs/
├── Jade-1.21.11-Fabric-21.1.6.jar  # compile-only: Jade is optional, never bundled
└── README.md             # where the jar comes from, and how to update it
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
