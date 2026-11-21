<div align="center">
  <img src="docs/archive-shelf/icon.png" width="104" height="104" alt="阅读S">
  <h1>阅读S · 档案式书架</h1>
  <p>给「阅读R」加了一页<b>档案式书架</b>：3D 立体书架 + 档案式书目 + 分组抽屉</p>
  <p>
    <a href="https://github.com/sssdex/legado/releases/latest"><b>⬇ 下载 APK</b></a>
    &nbsp;·&nbsp;
    <a href="ARCHIVE-SHELF.md">改动与构建说明</a>
    &nbsp;·&nbsp;
    <a href="README-UPSTREAM.md">上游原版说明</a>
  </p>
  <sub>包名 <code>com.legado.archive</code> · 与官方阅读R 共存 · 默认简体中文 · 桌面名「阅读S」</sub>
</div>

<br>

![档案式书架](docs/archive-shelf/01-shelf-light.png)

<br>

## 我加的东西

书架页重做成「档案」：上面一排立体的书，下面一份档案式书目，分组收进左侧抽屉。设置里留了开关，关掉就是原来的书架。

| 新增 | 说明 |
| --- | --- |
| **立体书架** | 平行投影（不是 3D 透视）：书脊挨着书脊，当前这本被「抽出」上浮、封面朝右上铺开，远处的书脊下坠成一条导轨。封面 107dp、书脊 14dp，整条书架约 365dp 高 |
| **档案式书目** | `SHELF / CATALOGUE` 标题 + 排序按钮，下面是 56×76 封面缩略 + `档案 / 001` + 书名 + 作者 + 阅读进度 |
| **分组抽屉** | 点左上角分组图标（或标题里的「全部 ▾」）从左边滑出，`01 全部 / 02 分组名 …`，底部是分组管理。面板是实心纸色，不会透出底下的书架和壁纸 |
| **可折叠** | 「收起书架」只留书目，按钮变「展开书架」 |
| **循环滑动** | 滑到头继续同方向滑会无缝接回另一端，反过来也一样 |
| **轻振** | 每滑过一本书振一次：15ms / 振幅 150 |
| **内置宋体** | 书名、作者、书脊竖排字都用内置的思源宋体（SIL OFL 1.1）；字号 书名 10.6sp / 作者 8.4sp / 书脊 8.2sp |
| **不透明纸面** | 主题背景色是半透明（壁纸主题）时，纸面、书脊、顶面、抽屉面板都会先合成到白/黑底再画，壁纸不会透进书页，相邻书的字也不会互相叠印 |
| **档案编号** | 封面上 `NO.001`、下面 `档案 / 001`，**只在当前抽出来的那本书上显示**（其余书被右邻书脊压住） |
| **书架数量 1–500** | 立体书架展示前 N 本（默认 12），封面按焦点窗口按需加载 + LRU 缓存；下面的书目始终是全部书 |
| **设置开关** | 我的 → 界面设置 →「档案式书架」，关掉立即回到原版书架，数据与设置都不动 |
| **新应用名与新图标** | 桌面名「阅读S」；默认图标换成深灰底白书，并且作为 `icon8` 也加进了原来的「切换图标」列表 |

<br>

## 长什么样

| 浅色 · 立体书架 | 分组抽屉 |
| --- | --- |
| <img src="docs/archive-shelf/01-shelf-light.png" width="300"> | <img src="docs/archive-shelf/02-drawer.png" width="300"> |

| 夜间模式 | 半透明壁纸主题（纸面仍不透字） |
| --- | --- |
| <img src="docs/archive-shelf/03-shelf-dark.png" width="300"> | <img src="docs/archive-shelf/04-wallpaper.png" width="300"> |

| 壁纸主题下的抽屉（是实心的） | 桌面图标与名称 |
| --- | --- |
| <img src="docs/archive-shelf/05-wallpaper-drawer.png" width="300"> | <img src="docs/archive-shelf/07-launcher.png" width="300"> |

<br>

## 最新一版修了什么（v6 · 3.26.112206）

1. **分组抽屉不再透明**：浅色 + 壁纸主题下原来能看到底下的书架和壁纸、字叠在一起。现在书架和抽屉共用同一套不透明纸色（`ArchivePalette.paper()`），浅色深色都是实心。
2. **默认图标换成深灰底白书**：自适应图标的前景缩进了安全区（约 66%），圆角/圆形遮罩都不会裁到书角；另生成了 5 个密度的传统图标。原「切换图标」保留，新图标作为第 8 项 `icon8` 加入。

   <img src="docs/archive-shelf/06-icons.png" width="300" alt="切换图标列表">

3. **应用名改为「阅读S」**：桌面、应用信息、最近任务、通知栏一致。

<br>

## 怎么用

- **开关**：我的 → 界面设置 →「档案式书架」（默认开启）。同一页的「书架数量」可调 1–500，改完返回书架立即生效。
- **切换图标**：同一页顶部「切换图标」，`iconMain` 是默认的新图标，`icon8` 也是它（方便和其它图标来回切）。
- **安装**：包名 `com.legado.archive`，不会覆盖官方阅读R / 阅读；数据独立，第一次装上书架是空的，到「我的 → 备份与恢复」导入自己的备份即可。

<br>

## 构建

构建方式、签名（自签 keystore，只能覆盖本改版）、字体授权和已知限制都写在 **[ARCHIVE-SHELF.md](ARCHIVE-SHELF.md)**。

<br>

---

<sub>本仓库 fork 自 [refgd/legado](https://github.com/refgd/legado)（阅读R，基线 `accef33`）。除书架页、应用名和默认图标外没有改动上游功能，数据库结构、书源、阅读器、备份格式都保持原样。上游完整说明见 [README-UPSTREAM.md](README-UPSTREAM.md) / [English.md](English.md)。</sub>
