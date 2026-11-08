# Build / 构建

| Module | Minecraft | Java | Recorded loader / API |
| --- | --- | --- | --- |
| forge-1.20.1 | 1.20.1 | 17 | Forge 47.4.10 |
| fabric-1.20.1 | 1.20.1 | 17 | Fabric Loader 0.16.14; Fabric API 0.92.6+1.20.1 |
| neoforge-1.21.1 | 1.21.1 | 21 | NeoForge 21.1.234 |
| fabric-1.21.1 | 1.21.1 | 21 | Fabric Loader 0.16.14; Fabric API 0.116.15+1.21.1 |

Run each module's Gradle Wrapper with the indicated JDK. For example:

```sh
cd forge-1.20.1
./gradlew build --console=plain
```

On Windows use `gradlew.bat`. Outputs are under each module's `build/libs`. Main source sets also read `../common/src/main/java` and `../common/src/main/resources`; keep the repository layout intact.

## Compile dependencies

TaCZ is optional for players, but the integration source needs its API at compile time. The Forge build expects the original selected TaCZ 1.1.8-hotfix JAR at `forge-1.20.1/work/compat/tacz-1.1.8-hotfix.jar`. Obtain that version from the official TaCZ project (recorded Modrinth version ID `yOVIzIJR`). Do not substitute a new version and claim identical verification.

For the three community-port modules, run `python tools/prepare-tacz-ports.py` with Python 3.12+ before building. It downloads the pinned external dependencies in `tools/tacz-ports.json`, verifies hashes and prepares local compile inputs. It does not add them to the released mod. `-Ptacz` enables those dependencies for development runs. Refabricated uses Fabric Loader 0.17.2 and the corresponding Forge Config API Port; these are its own prerequisites.

## Resources

The source repository does not include the third-party audio, project icon or armor images. Java compilation works without them, but the resulting JAR lacks those sounds and images. Resource paths and hashes are listed in ASSET_MANIFEST.json. Player downloads are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/apexsound).

Root LICENSE covers project-owned code and documentation. LICENSE-ASSETS.md applies separately to assets.

## 中文

保留 common 与四端的相对目录，按上表选择 JDK，在目标模块运行 Gradle Wrapper。TaCZ 在玩家端可选，但编译适配代码仍需要选定 API；Forge 手动准备原选定 JAR，其余三端可运行已固定版本及哈希的准备工具。它们不进入正式包。

源码仓库未包含第三方音效、项目图标和护甲图片。可直接编译 Java，但构建出的 JAR 会缺少这些声音和图片；资源路径与哈希见 ASSET_MANIFEST.json。玩家安装包从 [CurseForge](https://www.curseforge.com/minecraft/mc-mods/apexsound) 下载。自有代码与文档采用 MIT，素材许可另见 LICENSE-ASSETS.md。
