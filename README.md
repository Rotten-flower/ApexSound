# ApexSound / APEX 音效

## English

AI was used extensively during the development of this mod. Issues may still occur with different mod combinations and environments. If you do not trust AI-assisted projects, please think carefully before downloading and using it.

### Overview

Add Apex Legends-style hit sounds and damage feedback to combat.

Armor hits, shield breaks, flesh hits and kills have distinct sounds. Consecutive hits show cumulative damage beside the target, with five armor colors based on its maximum health. The feedback follows the target’s health and does not change damage or add a shield.

Selected TaCZ versions also support ammo sound groups, headshot feedback and replacement of gun hit sounds. TaCZ is optional; see [BUILDING.md](BUILDING.md) for the recorded integration versions.

### Installation

Minecraft 1.20.1: Forge or Fabric. Minecraft 1.21.1: NeoForge or Fabric. Fabric requires Fabric API. Install the matching mod version on both the client and server; singleplayer needs only the local installation.

Adjust **Apex volume** in Music & Sounds.

### Configuration

Forge / NeoForge:

- `config/apex_sound_effects-client.toml`: sound volume and TaCZ hit-sound replacement.
- `<world>/serverconfig/apex_sound_effects-server.toml`: hit feedback, health thresholds, target types, weapon rules and ammo sound groups.

Fabric:

- `config/apex_sound_effects.properties`: server feedback rules and local client sound settings.

Key options:

- `enabled`: enable hit feedback.
- `fleshRatio`: set the health threshold for flesh hits and shield breaks, default `0.5`.
- `weapons.mode` / `weapons.whitelist`: allow all attacks, supported TaCZ bullets, or selected item IDs, gun IDs and item tags.
- `volume`: adjust Apex sound volume.
- `replaceTaczHitFeedback`: replace supported TaCZ hit, headshot and kill sounds.

Armor thresholds and ammo sound groups can also be configured. Restart the server and reconnect after changing server rules.

### Source and license

Author: **Rotten-flower**. Project-owned code and documentation use the [MIT License](LICENSE). Third-party sounds, images and fonts have separate terms; see [credits and sources](CREDITS.md), [LICENSE-ASSETS.md](LICENSE-ASSETS.md) and [THIRD_PARTY.md](THIRD_PARTY.md).

Some sounds and images are from the original game Apex Legends. See [build instructions](BUILDING.md) for building from source. Player downloads are on CurseForge.

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/apexsound) · [Build instructions](BUILDING.md) · [Contributing](CONTRIBUTING.md)

If you encounter any issues, please leave a comment or join our QQ group. Include your Minecraft version, mod version, mod loader, and logs whenever possible to help us investigate.

Suggestions are also welcome! Your feedback will help guide improvements in future updates.

QQ group: 1095622706 · Support the author: [Afdian](https://afdian.com/a/lbcm233)

## 中文

在该模组开发过程中广泛使用了人工智能。不同的模组组合和环境仍可能出现问题。如果你不相信AI辅助项目，下载使用前请三思。

### 简介

添加 Apex 风格的攻击音效和伤害反馈。

护甲命中、破甲、肉体命中和击杀使用不同音效。连续攻击会在目标旁显示累计伤害，五种甲色按目标最大血量区分。反馈根据血量判断，不改变伤害，也不增加实际护盾。

选定版本的 TaCZ 支持弹药音效分组、爆头反馈和枪械命中音效替换。TaCZ 为可选联动，已记录版本见 [BUILDING.md](BUILDING.md)。

### 安装与使用

Minecraft 1.20.1：Forge、Fabric。Minecraft 1.21.1：NeoForge、Fabric。Fabric 需要 Fabric API。联机时客户端和服务器安装对应版本，单人游戏安装在本地实例。

在“音乐和声音选项”中调整“Apex 总音量”。

### 配置

Forge / NeoForge：

- `config/apex_sound_effects-client.toml`：音量、TaCZ 命中音效替换。
- `<世界目录>/serverconfig/apex_sound_effects-server.toml`：命中反馈、血量阈值、目标范围、武器规则和弹药分组。

Fabric：

- `config/apex_sound_effects.properties`：服务端反馈规则和客户端本地声音设置。

主要配置：

- `enabled`：开启命中反馈。
- `fleshRatio`：设置肉体命中和破甲的血量阈值，默认 `0.5`。
- `weapons.mode` / `weapons.whitelist`：选择全部攻击、支持的 TaCZ 子弹或指定物品、枪械和物品标签。
- `volume`：调整 Apex 音效音量。
- `replaceTaczHitFeedback`：替换支持版本的 TaCZ 命中、爆头和击杀音效。

甲色阈值和弹药音效分组也可配置。修改服务端规则后，重启服务器并重新进入。

### 源码与许可

作者：**Rotten-flower**。项目自有代码和文档采用 [MIT](LICENSE)。第三方声音、图片和字体按各自条款处理，见 [素材署名与来源](CREDITS.md)、[素材许可](LICENSE-ASSETS.md) 与 [第三方说明](THIRD_PARTY.md)。

部分音效和图片素材取自原作游戏《Apex Legends》。源码构建见[构建说明](BUILDING.md)，玩家安装包在 CurseForge 下载。

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/apexsound) · [构建说明](BUILDING.md) · [贡献说明](CONTRIBUTING.md)

遇到问题可在评论区或 QQ 群反馈，并尽量提供 Minecraft 版本、模组版本、加载器和日志，方便排查。

欢迎提出改进建议，反馈将用于后续更新。

QQ 交流群：1095622706 · 支持作者：[爱发电](https://afdian.com/a/lbcm233)
