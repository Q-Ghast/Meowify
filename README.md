# Meowify 喵~

给 Minecraft 里的物品名后面加一句「 喵~」。

- 物品栏里把鼠标悬停在物品上时,tooltip 第一行的**物品名**后面会多出「 喵~」
- 快捷栏切换手持物品时,屏幕中央淡出的那个**物品名**同样会带上「 喵~」

模组是**纯客户端**的:只影响你自己的界面显示,服务器不需要安装它,也不会因为服务器没装而出现版本不匹配的红叉。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x(开发环境使用 47.4.10) |
| Java | 17 |
| 运行侧 | 仅客户端(`clientSideOnly=true`) |

## 安装

1. 准备好 Minecraft 1.20.1 + Forge 47.x 的客户端
2. 把 `meowify-1.0.0.jar` 放进 `.minecraft/mods/`
3. 启动游戏,不需要任何配置

专用服务器上不用装它;客户端装了本模组后,连进没装它的服务器也是正常的(模组清单按 `IGNORE_ALL_VERSION` 处理)。

## 从源码构建

```bash
# 首次运行需要下载 Forge 依赖并反编译,耗时较久
./gradlew build
```

产物在 `build/libs/meowify-1.0.0.jar`。Windows 下把 `./gradlew` 换成 `gradlew.bat`。

开发时常用的两个任务:

```bash
./gradlew runClient    # 启动带本模组的开发客户端
./gradlew runServer    # 纯客户端模组,这里不会加载它
```

## 实现原理

模组只有两条钩子,分别对应上面两个显示位置。

### 1. 物品 tooltip —— Forge 事件

`MeowifyEventHandler` 监听 `ItemTooltipEvent`,把提示列表的第 0 行(物品名)替换成「原名 + 后缀」:

```java
event.getToolTip().set(0, MeowifyText.appendSuffix(originalName));
```

### 2. 快捷栏切换提示 —— Mixin 注入

屏幕中央那个淡出的物品名由 `Gui#renderSelectedItemName` 直接绘制,Forge 没有对应事件,所以用 Mixin 注入:

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

这里有几个当时踩过的坑,记下来免得以后重复踩:

- 真正被绘制的文本来自 `ItemStack#getHighlightTip`,把注入点挂在它的赋值上,就既不依赖局部变量槽位,也不会误伤方法内部其他的 `Component`。
- 带 `yShift` 的那个重载是 **Forge 自己追加的方法、没有 SRG 名**,注解处理器会直接报 `Unable to locate obfuscation mapping`,必须显式写 `remap = false`。
- 不能用「临时改动物品的 hover name」这种取巧办法:`Gui` 每个 tick 都会比较 `getHoverName()` 来判断玩家是否换了物品,名字一旦被改动,淡出计时会被不断重置,提示就再也不会消失了。
- `defaultRequire = 1`:注入失败会直接抛错,而不是安静地什么都不做。

两个位置共用 `MeowifyText.appendSuffix`,后缀会继承物品名自身的样式(稀有度颜色;重命名过的物品则为斜体)。

## 项目结构

```
src/main/java/com/qxia/MeowifyMod/
├── MeowifyMod.java          # @Mod 入口,注册事件总线
├── MeowifyEventHandler.java # ItemTooltipEvent:物品 tooltip
├── MeowifyText.java         # 共用的后缀「 喵~」与拼接逻辑
└── mixin/GuiMixin.java      # 快捷栏切换提示的注入
src/main/resources/
├── META-INF/mods.toml       # 模组元数据,clientSideOnly=true
├── meowify.mixins.json      # mixin 配置(仅 client)
└── pack.mcmeta
```

## 开发注意事项

- **`gradle.properties` 里不要直接写中文**。Gradle 按 ISO-8859-1 读取该文件,非 ASCII 字符必须写成 `\uXXXX` 转义:`\u55b5` 就是「喵」。
- `processResources` 里固定了 `filteringCharset = 'UTF-8'`,否则生成的 `mods.toml` 会按平台编码(中文 Windows 上是 GBK)写出,进游戏就是乱码。
- 开发环境启动日志里下面两条警告是无害的,属于 1.20.1 + MixinGradle 的常见现象:
  - `Compatibility level JAVA_17 ... higher than the maximum level supported by this version of mixin (JAVA_13)`
  - `Reference map 'meowify.refmap.json' ... could not be read`(dev 环境下 refmap 只存在于 jar 里)
- 模组目前没有任何配置文件,行为是写死的。

## 许可证

本项目采用 **MIT 许可证**,全文见 [LICENSE.md](LICENSE.md)。

- 可以自由使用、修改、分发,包括商用,只需保留版权声明与许可声明。
- 模组元数据里的 `license` 字段(来自 `gradle.properties` 的 `mod_license`)同样是 MIT;构建出的 jar 里也附带了这份许可证文件。
- 构建脚本基于 **Forge MDK** 模板,`gradlew` / `gradle-wrapper.jar` 来自 Gradle 项目,它们分别遵循各自的上游许可(Forge 采用 LGPL-2.1、Gradle 采用 Apache-2.0)。

Copyright (c) 2026 Q-Ghast

## English

**Meowify** is a tiny **client-side** mod for Minecraft 1.20.1 (Forge 47.x, Java 17). It appends ` 喵~` to item names in two places: the item tooltip, and the item name overlay that fades out in the middle of the screen when you switch hotbar slots. Nothing runs server-side, so servers don't need it and clients won't get a mod-list mismatch.

Build with `./gradlew build`; the jar ends up in `build/libs/`. Tooltips go through a plain Forge `ItemTooltipEvent`; the hotbar overlay is handled by a Mixin into `Gui#renderSelectedItemName`, where `remap = false` is required because the two-argument overload is added by Forge and has no SRG name.

Released under the [MIT license](LICENSE.md).
