# Local third-party jars

Nothing in this directory is bundled into the built jar, and neither mod is required at runtime.

## `Jade-1.21.11-Fabric-21.1.6.jar`

Jade's API is needed to **compile**
`src/client/java/com/qxiane/compat/jade/MeowifyJadePlugin.java`, which appends the suffix after the block
and entity names Jade shows in its overlay.

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

## `cloth-basic-math-0.6.1.jar`

This one is **not** a compile dependency and the mod does not use it. It exists only so that
`./gradlew runClient` can display the config screen in the development environment.

Cloth Config ships `cloth-basic-math` as a Fabric *nested jar*, inside its own jar under
`META-INF/jars/`. Fabric Loader unwraps nested jars in production, but Loom does not do that for the
development runtime, so without this file the config screen fails with `NoClassDefFoundError:
me/shedaniel/math/Rectangle`. It is added as a `modLocalRuntime` entry so it never reaches the built jar.

### Getting the jar

It is not published to any Maven repository, so extract it from a Cloth Config release:

```bash
unzip -p cloth-config-21.11.153-fabric.jar 'META-INF/jars/basic-math-0.6.1.jar' > cloth-basic-math-0.6.1.jar
```

On Windows, open the Cloth Config jar with an archive tool and copy `META-INF/jars/basic-math-0.6.1.jar`
into this folder under the name above. Cloth Config itself is fetched automatically by Gradle
(`cloth_config_version` in `gradle.properties`), so only this nested jar has to be placed by hand.

If you would rather not bother, deleting this file and the matching `modLocalRuntime` line in
`build.gradle` only means the config screen cannot be tested in `runClient`; the published mod is
unaffected either way.
