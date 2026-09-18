# Optional Jade jar staging

You normally do **not** need to put anything here. `./gradlew build` downloads Jade into `build/jade/`
on its own (see the `downloadJade` task in `build.gradle`) and unpacks only its API classes for
compiling `com.qxia.MeowifyMod.compat.jade.MeowifyJadePlugin`.

This folder exists for machines that cannot reach the network from Gradle. Put the exact file below
here and the build will use it instead of downloading:

```
Jade-1.20.1-Forge-11.13.1.jar
```

- Modrinth: <https://modrinth.com/mod/jade/version/11.13.1+forge>
- CurseForge: <https://www.curseforge.com/minecraft/mc-mods/jade/files?version=1.20.1>
- SHA-256 that the build checks: `921996fef486fdc0d03d3e9574698243edb380e118aaa6a59dccd55b1776b32f`

The file is git-ignored on purpose: Jade is licensed **CC BY-NC-SA 4.0**, so this repository does not
redistribute it. When you update Jade, change `jade_version` in `gradle.properties` and `jadeSha256`
in `build.gradle` together.
