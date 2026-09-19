# Meowify 喵~

**简体中文** | [English](README.md)

给 Minecraft 里的物品名后面加一句「 喵~」。

- 物品栏里把鼠标悬停在物品上时,tooltip 第一行的**物品名**后面会多出「 喵~」
- 快捷栏切换手持物品时,屏幕中央淡出的那个**物品名**同样会带上「 喵~」
- 装了 [Jade](https://modrinth.com/mod/jade) 时,Jade 提示框里的**方块名、实体名和物品名**后面也会多出「 喵~」

模组是**纯客户端**的:只影响你自己的界面显示,服务器不需要安装它,也不会因为服务器没装而出现版本不匹配的红叉。Jade 适配同样是**可选**的 —— 没装 Jade 时行为和以前完全一样。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x(开发环境使用 47.4.10) |
| Java | 17 |
| 运行侧 | 仅客户端(`clientSideOnly=true`) |

## 安装

1. 准备好 Minecraft 1.20.1 + Forge 47.x 的客户端
2. 把 `meowify-1.1.0.jar` 放进 `.minecraft/mods/`
3. 启动游戏,不需要任何配置

专用服务器上不用装它;客户端装了本模组后,连进没装它的服务器也是正常的(模组清单按 `IGNORE_ALL_VERSION` 处理)。

## 从源码构建

```bash
# 首次运行需要下载 Forge 依赖并反编译,耗时较久
./gradlew build
```

产物在 `build/libs/meowify-1.1.0.jar`。Windows 下把 `./gradlew` 换成 `gradlew.bat`。

编译需要 Jade 的 API,但只用于编译:`build.gradle` 里的 `downloadJade` / `jadeApi` 两个任务会从
Modrinth 下载官方 Jade 发布包、校验 SHA-256,然后只把其中的 API 类解包到 `build/jade-api-classes/`
作为 `compileOnly` 依赖。因此仓库里不存放任何 Jade 二进制,构建出的 jar 里也不含 Jade,Meowify 更不会
变成"必须装 Jade"。如果 Gradle 连不上网,可以把 jar 手动放到 `gradle/jade/`,细节见
[`gradle/jade/README.md`](gradle/jade/README.md)。

开发时常用的两个任务:

```bash
./gradlew runClient    # 启动带本模组的开发客户端
./gradlew runServer    # 纯客户端模组,这里不会加载它
```

> **注意**:`runClient` 是 ForgeGradle 的**开发环境**,而 Jade 只有生产环境的 jar(混淆到 SRG 名),
> 直接丢进 `run/mods/` 会因为 SRG/官方映射不匹配而崩溃(`Jade` 报 `NoSuchMethodError`)。
> 想实测 Jade 适配,请把 `build/libs/meowify-1.1.0.jar` 和 Jade 一起放进一个真正的 1.20.1 Forge 客户端。

## 实现原理

模组有三条钩子,分别对应上面三个显示位置。

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

### 3. Jade 提示框 —— Jade 的 tooltip 回调

Jade 用自己的渲染管线绘制提示框,既不走原版 tooltip 那条会触发 `ItemTooltipEvent` 的路,也不受 `Gui` 里那段 Mixin 影响。所以这里用的是 Jade 的插件 API:

```java
@WailaPlugin
public class MeowifyJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(MeowifyJadePlugin::appendSuffixToNames);
    }
}
```

`addTooltipCollectedCallback` 在所有 provider 都往 tooltip 里写过内容之后、提示框开始渲染之前触发,这时我们遍历每一行的元素,把「名字行」换成「名字 + 后缀」。Jade 每个客户端 tick 都会新建一个 tooltip 对象,所以后缀不会被叠加两次。

判定「哪一行是名字」用了两种依据:

- **标题行**(方块名 / 实体名):Jade 的 `ObjectNameProvider` 用 `Identifiers.CORE_OBJECT_NAME` 这个 provider uid 注册,而 `Tooltip#add` 会把当前正在执行的 provider 的 uid 打到元素上,所以按 tag 精确命中。这样方块名、实体名以及 Jade 自己处理的各种特例(拾取结果、自定义命名、掉落物实体、物品/方块展示体)全都覆盖到了,不需要重新实现 Jade 的取名逻辑。
- **物品栏内容行**(箱子、熔炉等内容物):Jade 把物品名画成 `"12× 石头"` 这样一行自造文本,而且**没有打 tag**。只能按 `× ` 这个分隔形式识别,并且额外要求「第一个 `× ` 之后不再出现 `× `」——因为物品名本身可能包含 `× `,重复改写会让后缀越加越多。

实现时有两个 Jade 内部细节必须依赖,它们都在编译期由 jar 校验过:

- `snownee.jade.impl.ui.TextElement` 的 `public final FormattedText text` 是公开字段,靠它才能拿到组件并重新拼一个元素;
- `snownee.jade.impl.Tooltip` 的 `public final List<Line> lines` 虽然是公开字段,但 `Line` 里的两个列表是私有的,所以只能通过 `ITooltip#get(int, Align)` 拿到**其内部列表的引用**再原地替换元素。

`MeowifyJadePlugin` 只有装了 Jade 才会被加载:Jade 扫描 `@WailaPlugin` 注解来发现插件,所以 `mods.toml` 里不需要任何声明;没装 Jade 时这个类永远不会被加载,也不需要把 Jade 写成前置依赖。

## 项目结构

```
src/main/java/com/qxia/MeowifyMod/
├── MeowifyMod.java          # @Mod 入口,注册事件总线
├── MeowifyEventHandler.java # ItemTooltipEvent:物品 tooltip
├── MeowifyText.java         # 共用的后缀「 喵~」与拼接逻辑
├── mixin/GuiMixin.java      # 快捷栏切换提示的注入
└── compat/jade/
    └── MeowifyJadePlugin.java  # Jade 提示框的方块/实体/物品名(仅装了 Jade 时加载)
src/main/resources/
├── META-INF/mods.toml       # 模组元数据,clientSideOnly=true
├── meowify.mixins.json      # mixin 配置(仅 client)
└── pack.mcmeta
gradle/jade/
└── README.md                # 说明 downloadJade/jadeApi 任务与"离线时手动放 jar"的办法
```

## 开发注意事项

- **`gradle.properties` 里不要直接写中文**。Gradle 按 ISO-8859-1 读取该文件,非 ASCII 字符必须写成 `\uXXXX` 转义:`\u55b5` 就是「喵」。
- `processResources` 里固定了 `filteringCharset = 'UTF-8'`,否则生成的 `mods.toml` 会按平台编码(中文 Windows 上是 GBK)写出,进游戏就是乱码。
- 开发环境启动日志里下面两条警告是无害的,属于 1.20.1 + MixinGradle 的常见现象:
  - `Compatibility level JAVA_17 ... higher than the maximum level supported by this version of mixin (JAVA_13)`
  - `Reference map 'meowify.refmap.json' ... could not be read`(dev 环境下 refmap 只存在于 jar 里)
- 模组目前没有任何配置文件,行为是写死的。
- Jade 的 API **只用于编译**:`build.gradle` 以 `compileOnly` 引入,所以它不会被打进 `meowify-*.jar`,也不会让 Meowify 变成"必须装 Jade"。升级 Jade 时同步改 `gradle.properties` 的 `jade_version` 与 `build.gradle` 的 `jadeSha256` 即可。
- 因为 Jade 的 `snownee.jade.impl.ui.TextElement` 是内部类而非常规 API,`jadeApi` 任务在解包 API 类之外还额外带上 `snownee/jade/impl/ui/TextElement.class` 和 `snownee/jade/impl/Tooltip*.class` 供编译期解析。

## 许可证

本项目采用 **MIT 许可证**,全文见 [LICENSE.md](LICENSE.md)。

- 可以自由使用、修改、分发,包括商用,只需保留版权声明与许可声明。
- 模组元数据里的 `license` 字段(来自 `gradle.properties` 的 `mod_license`)同样是 MIT;构建出的 jar 里也附带了这份许可证文件。
- 构建脚本基于 **Forge MDK** 模板,`gradlew` / `gradle-wrapper.jar` 来自 Gradle 项目,它们分别遵循各自的上游许可(Forge 采用 LGPL-2.1、Gradle 采用 Apache-2.0)。

Copyright (c) 2026 Q-Ghast
