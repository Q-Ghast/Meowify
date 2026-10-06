# Meowify

Appends a cat noise to item names in Minecraft. The text follows the language selected in game, so it
reads 「 喵~」 in Chinese, ` meow~` in English, ` miaou~` in French, and so on.

This repository holds the mod for two loaders, built from one shared feature set and one shared set of
translations:

| Project | Loader | Minecraft | Entry point |
| --- | --- | --- | --- |
| [`forge/`](forge/) | Forge 47.x | 1.20.1 | `forge/README.md` |
| [`fabric/mc1.21.11/`](fabric/mc1.21.11/) | Fabric | 1.21.11 | `fabric/mc1.21.11/README.md` |

Both projects are **client-side only** and share the same version number. Each has its own README with
its requirements, build instructions and implementation notes; start there.

## Layout

```
forge/                       Forge 47.x build for Minecraft 1.20.1
fabric/mc1.21.11/            Fabric build for Minecraft 1.21.11
.github/workflows/build.yml  builds both projects on every push
```

## Building

The two projects are independent Gradle builds, so build them from their own directories:

```bash
cd forge && ./gradlew build
cd fabric/mc1.21.11 && ./gradlew build
```

On Windows use `gradlew.bat`. Each build writes its jar to `<project>/build/libs/`.

Requirements differ per project: the Forge build targets Java 17, the Fabric build targets Java 21.

## Features

- The **item name** on the first line of the inventory tooltip.
- On Forge, the **item name** that fades out in the middle of the screen when you switch hotbar slots.
- With [Jade](https://modrinth.com/mod/jade) installed, the **block and entity names** in Jade's
  overlay.

The suffix is a translation key, so it follows the language selected in game; a config file can turn
each location on or off and override the text with a custom string. Which of the above each loader
supports today is documented in that project's README.

## License

MIT; see [`forge/LICENSE.md`](forge/LICENSE.md). The bundled license file is the same for both projects.
