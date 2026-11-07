# Build / 构建

| Module | Minecraft | Java | Recorded loader / API |
| --- | --- | --- | --- |
| forge-1.20.1 | 1.20.1 | 17 | Forge 47.4.10 |
| fabric-1.20.1 | 1.20.1 | 17 | Fabric Loader 0.16.14; Fabric API 0.92.6+1.20.1 |
| neoforge-1.21.1 | 1.21.1 | 21 | NeoForge 21.1.234 |
| fabric-1.21.1 | 1.21.1 | 21 | Fabric Loader 0.16.14; Fabric API 0.116.15+1.21.1 |

These are recorded development combinations, not a claim of new testing. Run each module's Gradle Wrapper with the indicated JDK. For example:

```sh
cd forge-1.20.1
./gradlew build --console=plain
```

On Windows use `gradlew.bat`. Outputs are under each module's `build/libs`. Main source sets also read `../common/src/main/java` and `../common/src/main/resources`; keep the repository layout intact.

## Compile dependencies

TaCZ is optional for players, but the integration source needs its API at compile time. The Forge build expects the original selected TaCZ 1.1.8-hotfix JAR at `forge-1.20.1/work/compat/tacz-1.1.8-hotfix.jar`. Obtain that version from the official TaCZ project (recorded Modrinth version ID `yOVIzIJR`). Do not substitute a new version and claim identical verification.

For the three community-port modules, run `python tools/prepare-tacz-ports.py` with Python 3.12+ before building. It downloads the pinned external dependencies in `tools/tacz-ports.json`, verifies hashes and prepares local compile inputs. It does not add them to the released mod. `-Ptacz` enables those dependencies for development runs. Refabricated uses Fabric Loader 0.17.2 and the corresponding Forge Config API Port; these are its own prerequisites.

## Asset boundary

The source candidate omits the audio, existing CurseForge logo and two armor icons listed in LICENSE-ASSETS.md. Java compilation does not require these binary assets, but a build from this subset lacks their sounds and images and does not reproduce the published playable package. Do not distribute that incomplete output as the official mod. Restoring assets requires permission and the exact resource paths / hashes in ASSET_MANIFEST.json; there is no automated extraction of assets from the published JAR.

The current local candidate is `0.2-dev`, with Java package `dev.flower.apex_sound_effects`, name `ApexSound` and author `Rotten-flower`. Packaged mod descriptions are intentionally empty. The mod-list logo reuses the current CurseForge logo; it is omitted from the source candidate pending provenance. Published 0.1 files are not overwritten. Root LICENSE covers code, while LICENSE-ASSETS.md applies separately to assets.

## 中文

保留 common 与四端的相对目录，按上表选择 JDK，在目标模块运行 Gradle Wrapper。TaCZ 在玩家端可选，但编译适配代码仍需要选定 API；Forge 手动准备原选定 JAR，其余三端可运行已固定版本及哈希的准备工具。它们不进入正式包。

此源码候选未包含授权未确认的声音、现有 CF 项目图标和两张护甲图标。可编译 Java 不等于可重现完整安装包；当前本地候选为 0.2-dev，包名 dev.flower.apex_sound_effects，名称 ApexSound，作者 Rotten-flower，包内简介按要求留空。沿用现有 CF 项目图标，未确认来源前不放入源码候选。已发布 0.1 不覆盖。构建与启动记录分别核实，不把旧版验收记作本轮完成。
