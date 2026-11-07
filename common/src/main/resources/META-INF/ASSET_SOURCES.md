# 素材来源

以下记录当前使用的素材、固定来源、处理与许可证。打包文件的 SHA-256 见 ASSET_MANIFEST.json。

| 内容 | 来源与版本 | 处理与许可证 |
|---|---|---|
| 肉体命中声音 | https://github.com/AlexBoi301/Apex_Hitmarker ，提交 6aaf4152a301a625be2d35e2a6f99fe472b43329 | 使用原声音文件；仓库未声明许可证。未使用其中的准星图片。 |
| 四段破甲声音、护甲图标 | https://github.com/zxy19/battery_shield ，提交 9d6c34a55e512595be476dab3803e25af2a2013b | 使用声音与选定的 64×64 护甲图标，碎甲图标由护甲图标拆分；附带仓库 MIT 许可证及原始署名。 |
| 护甲命中、碎甲命中瞬间、弓箭反馈、各弹药终结音 | https://www.bilibili.com/video/BV1sB4y1v7C2/ ，奶油湫刀魚，2022-07-13 | 按画面名称与时间截取、去除开头静音并编码 OGG；视频简介允许剪辑使用，未声明模组再分发许可证。 |
| 伤害数字字体 | https://github.com/google/fonts/tree/main/ofl/barlowcondensed ，Barlow Condensed SemiBold，blob 9df66576492bee4bd0559066825472808fa98adc | SIL Open Font License 1.1，完整许可证随资源保存。 |
| 破甲碎片 | 本工程制作 | 六边形粒子及短时动画，属于 Minecraft 中的效果适配。 |

部分声音使用 `_loud.ogg` 文件：幅度乘 2，超过 0.8 时采用平滑限幅，再按原采样率与声道数编码 OGG。未被当前声音定义引用的声音与图片不打包。加工文件记录源文件哈希；音轨经过编码，不声称与游戏音频库原始文件字节一致。

五色分档参考经典白、蓝、紫、金、红护甲。本工程只用甲色表示最大血量区间，不模拟金甲额外特性。官方机制变化参考 https://www.ea.com/games/apex-legends/apex-legends/news/breakout-patch-notes 。

位置与累计显示参考 https://www.bilibili.com/video/BV1XnmHBNE3B/ 、https://www.gamepressure.com/apex-legends/damage-system/zebdec 、https://knowl.jp/game/apex-legends-damage-numbers 。这些参考画面不打包；投影、跟随与停留时间为 Minecraft 适配。

Forge 开发依赖：Minecraft 1.20.1、Forge 47.4.10；可选 TaCZ 1.1.8-hotfix，项目 https://github.com/MCModderAnchor/TACZ ，许可证按其原 JAR 元数据记录为 GPL3 / CC BY-NC-ND 4.0。TaCZ 不嵌入本模组 JAR。spark / Modrinth 版本 4FXHDE9F 仅用于开发验证，不是玩家前置。各端依赖和验证状态见 VALIDATION.md。

第三方项目的许可证与视频用途说明不能代替 EA 原始素材的再分发授权；本记录如实保留目前可核实的范围。

模组列表图标：沿用 ApexSound 现有 CurseForge 发布页的 64×64 项目图标，原字节与来源见清单。原始美术权利尚未确认，公开源码候选暂不含此图；不纳入代码 MIT。
