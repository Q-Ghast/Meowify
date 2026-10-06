# Meowify

Appends a cat noise to item names in Minecraft. The text follows the language selected in game, so it
reads 「 喵~」 in Chinese, ` meow~` in English, ` miaou~` in French, and so on.

This repository holds the mod for two loaders. Each loader has a directory, and each supported
Minecraft version has its own project inside it:

| Project | Loader | Minecraft |
| --- | --- | --- |
| [`forge/mc1.20.1/`](forge/mc1.20.1/) | Forge 47.x | 1.20.1 |
| [`fabric/mc1.21.11/`](fabric/mc1.21.11/) | Fabric | 1.21.11 |

Every project is **client-side only** and they all share the same version number. Each has its own
README with its requirements, build instructions and implementation notes; start there.

## Layout

```
forge/mc1.20.1/              Forge 47.x build for Minecraft 1.20.1
fabric/mc1.21.11/            Fabric build for Minecraft 1.21.11
.github/workflows/build.yml  builds every project on every push
```

## Building

Each project is an independent Gradle build, so build it from its own directory:

```bash
cd forge/mc1.20.1 && ./gradlew build
cd fabric/mc1.21.11 && ./gradlew build
```

On Windows use `gradlew.bat`. Each build writes its jar to `<project>/build/libs/`.

Requirements differ per project: the Forge build targets Java 17, the Fabric build targets Java 21.

## Features

- The **item name** on the first line of the inventory tooltip.
- On Forge, the **item name** that fades out in the middle of the screen when you switch hotbar slots.
- With [Jade](https://modrinth.com/mod/jade) installed, the **block and entity names** in Jade's
  overlay.

The suffix is a translation key, so it follows the language selected in game. Every project also has an
**in-game configuration screen** — Forge opens it from the mod list's Config button, Fabric from the
ModMenu button — where each location can be turned on or off and the text replaced with a custom
string. Which of the features above each project supports today is documented in that project's README.

## License

MIT; see [`forge/mc1.20.1/LICENSE.md`](forge/mc1.20.1/LICENSE.md). The bundled license file is the same
for every project.
