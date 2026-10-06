# Local build-time dependencies

Files here are only needed to **compile** the mod. Nothing in this directory is bundled into the built
jar, and none of it is required at runtime.

## `Jade-1.21.11-Fabric-21.1.6.jar`

Jade's API is needed to compile `src/client/java/com/qxiane/compat/jade/MeowifyJadePlugin.java`, which
appends ` 喵~` after the block and entity names Jade shows in its overlay.

Meowify treats Jade as an **optional** dependency: it is declared as `modCompileOnly`, and the plugin is
only loaded when Jade is present (Jade discovers Fabric plugins through the `jade` entrypoint that
`fabric.mod.json` declares). A client without Jade is unaffected.

### Getting the jar

Download `Jade-1.21.11-Fabric-21.1.6.jar` and drop it in this folder:

- Modrinth: <https://modrinth.com/mod/jade/versions?g=1.21.11&l=fabric>
- CurseForge: <https://www.curseforge.com/minecraft/mc-mods/jade/files?version=1.21.11>

The name has to match `jade_version` in `gradle.properties`, because `build.gradle` resolves it as
`jade:Jade-${minecraft_version}-Fabric-${jade_version}` through the `flatDir` repository declared in
that same file.

Loom remaps the jar to this project's named mappings on the fly, so the Jade types resolve at compile
time even though the released jar is published in intermediary mappings.

The jar is git-ignored on purpose: Jade is licensed **CC BY-NC-SA 4.0**, so this repository does not
redistribute it. When updating Jade, change `jade_version` in `gradle.properties` to match the new file
name.
