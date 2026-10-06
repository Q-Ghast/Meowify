# Meowify

**简体中文** | [English](README.md)

给 Minecraft 里的物品名后面加一句猫叫。具体文字会**跟着游戏内选择的语言走**:中文是「 喵~」,英文是 ` meow~`,法语是 ` miaou~`,以此类推。

- 物品栏里把鼠标悬停在物品上时,tooltip 第一行的**物品名**后面会多出这个后缀
- 快捷栏切换手持物品时,屏幕中央淡出的那个**物品名**同样会带上它
- 装了 [Jade](https://modrinth.com/mod/jade) 时,Jade 提示框里的**方块名、实体名和物品名**后面也会带上它

模组是**纯客户端**的:只影响你自己的界面显示,服务器不需要安装它,也不会因为服务器没装而出现版本不匹配的红叉。Jade 适配同样是**可选**的 —— 没装 Jade 时行为和以前完全一样。

## 支持的语言

后缀是一个普通的翻译键(`meowify.suffix`),由游戏按你选择的语言去取。目前内置:

| 语言 | 显示为 |
| --- | --- |
| English (`en_us`) | ` meow~` |
| 简体中文 (`zh_cn`) | ` 喵~` |
| Français (`fr_fr`) | ` miaou~` |
| Español (`es_es`) | ` miau~` |
| Português (`pt_br`) | ` miau~` |
| Русский (`ru_ru`) | ` мяу~` |
| 日本語 (`ja_jp`) | ` ニャー~` |

其他语言会回退到英文。想加一门语言,只要在 `src/main/resources/assets/meowify/lang/` 下新建一个以语言代码命名的文件(如 `de_de.json`)即可:

```json
{
  "meowify.suffix": " miau~"
}
```

不需要改任何代码,文件会被自动加载;资源包也可以覆盖这些文件。

## 配置文件

首次启动时模组会生成 `config/meowify-client.toml`。它是 Forge 的**客户端配置**,所以模组列表的配置界面里也能直接改:

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

| 选项 | 默认值 | 作用 |
| --- | --- | --- |
| `enableGlobalSuffix` | `true` | 物品栏 tooltip 和快捷栏切换提示。关掉后这两处名字保持原样。 |
| `enableJadeSuffix` | `true` | Jade 提示框里的名字。与上一项**互不影响**,可以只留 Jade 或只留原版。 |
| `customSuffix` | `""` | 用一段自定义文字替换翻译文本。留空则继续用各语言自己的文字。 |

几点说明:

- 设置了 `customSuffix` 后,**所有语言**都会显示这段文字 —— 它是字面量,不是翻译键。前后空白会被去掉、并自动补一个空格,所以写 `"meow~"` 或 `" meow~"` 都是 `Stone meow~`。
- 两个开关相互独立:`enableGlobalSuffix = false` + `enableJadeSuffix = true` 就是「只在 Jade 里加后缀」。
- 两项都默认开启,所以从旧版本升级后行为不变,除非你自己去改。
- 如果你想要的是**按语言**分别自定义(而不是所有语言同一段文字),请保持 `customSuffix` 为空,改语言文件即可,见上一节。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x(开发环境使用 47.4.10) |
| Java | 17 |
| 运行侧 | 仅客户端(`clientSideOnly=true`) |


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

### 后缀文字本身

`MeowifyText` 里**没有写死任何文字**。默认追加的是 `Component.translatable("meowify.suffix")`,由客户端在每次渲染时从 `assets/meowify/lang/<语言>.json` 里取,所以在游戏里切换语言会立刻生效,不需要重启游戏。如果在配置里设置了 `customSuffix`,则改用 `Component.literal`,此时所有语言都显示同一段文字。两种情况下附加在组件上的样式都会从物品名拷贝过来,后缀依然保持物品的稀有度颜色。

### 3. Jade 提示框 —— Jade 的 tooltip 回调

Jade 用自己的渲染管线绘制提示框,既不走原版 tooltip 那条会触发 `ItemTooltipEvent` 的路,也不受 `Gui` 里那段 Mixin 影响。所以这里用的是 Jade 的插件 API:

```java
@WailaPlugin
public class MeowifyJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(new SuffixApplier());
    }
}
```

`addTooltipCollectedCallback` 在所有 provider 都往 tooltip 里写过内容之后、提示框开始渲染之前触发,这时我们遍历每一行的元素,把「名字行」换成「名字 + 后缀」。Jade 每个客户端 tick 都会新建一个 tooltip 对象,所以后缀不会被叠加两次。走回调这条路的另一个好处是:`enableJadeSuffix` 只会影响 Jade 自己的提示框,不会波及原版那两处。

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
├── MeowifyMod.java          # @Mod 入口,注册事件总线与配置
├── MeowifyConfig.java       # 客户端配置:两个开关 + customSuffix
├── MeowifyEventHandler.java # ItemTooltipEvent:物品 tooltip
├── MeowifyText.java         # 共用的后缀工具(翻译键、自定义文字、开关)
├── mixin/GuiMixin.java      # 快捷栏切换提示的注入
└── compat/jade/
    └── MeowifyJadePlugin.java  # Jade 提示框的方块/实体/物品名(仅装了 Jade 时加载)
src/main/resources/
├── META-INF/mods.toml       # 模组元数据,clientSideOnly=true
├── assets/meowify/lang/     # 各语言下的后缀文字
│   ├── en_us.json           #   "  meow~"
│   ├── zh_cn.json           #   "  喵~"
│   ├── fr_fr.json           #   "  miaou~"
│   ├── es_es.json           #   "  miau~"
│   ├── pt_br.json           #   "  miau~"
│   ├── ru_ru.json           #   "  мяу~"
│   └── ja_jp.json           #   "  ニャー~"
├── meowify.mixins.json      # mixin 配置(仅 client)
└── pack.mcmeta
gradle/jade/
└── README.md                # 说明 downloadJade/jadeApi 任务与"离线时手动放 jar"的办法
```

Copyright (c) 2026 Q-Ghast
