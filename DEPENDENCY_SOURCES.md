# 枪械适配依赖

2026-10-08 联动策略调整：以下选定版本是专用联动启用清单，不是本模组的启动前置范围。未安装、不支持或接口不匹配时跳过联动并记录原因；普通反馈继续按原配置运行。枪械模组自身的依赖要求仍保留，本模组不强制更换它们的版本。

核对日期：2026-10-07。以下文件仅用于编译与独立开发测试，不嵌入 APEX Sound effects 的正式 JAR，也不随交付目录分发。固定下载地址、版本 ID 与 SHA-512 在 `tools/tacz-ports.json`；`tools/prepare-tacz-ports.py` 校验缓存后下载缺失文件。

| 平台 | 选定项目与版本 | 源码 | 许可证记录 |
|---|---|---|---|
| NeoForge 1.21.1 | [UNOFFICIAL TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1)，1.1.8-hotfix-r7 | [MUKSC/TACZ-1.21.1](https://github.com/MUKSC/TACZ-1.21.1)，neoforge/1.21.1 分支 | 项目页面 GPL-3.0-only；JAR 元数据 GPL3 / CC BY-NC-ND 4.0 |
| Fabric 1.20.1 | [TaCZ: Refabricated](https://modrinth.com/mod/tacz-refabricated)，1.20.1-0.7.1-forge1.1.8-hotfix2 | [Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated)，1.20.1 分支 | 项目页面 GPL-3.0-only；JAR 元数据 GPL3 / CC BY-NC-ND 4.0 |
| Fabric 1.21.1 | 同项目，1.21.1-0.7.1-forge1.1.8-hotfix2 | 同仓库，1.21.1 分支 | 同上 |
| Fabric 1.20.1 | [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)，8.0.3 | [源码](https://github.com/Fuzss/forgeconfigapiport) | MPL-2.0 |
| Fabric 1.21.1 | 同项目，21.1.6 | 同仓库 | MPL-2.0 |

两端 Refabricated 的内部模组版本均为 `0.7.1-forge1.1.8-hotfix2`，Minecraft 版本由各自文件区分；选定 JAR 要求 Fabric Loader ≥0.17.2。Config API Port 是 Refabricated 自己的前置；没有安装枪械移植版时，APEX Sound effects 不需要它。上游嵌套依赖由上游文件负责加载。

Loom 开发重映射会移除本地 JAR 的内嵌库入口。准备脚本将上游 JAR 中声明的 BCEL、Commons Math、LuaJ、Cardinal Components 与 SimpleBedrockModel 原样提取到各端 work/compat/nested，并仅在 `-Ptacz` 时暴露给开发运行。不从其他来源替换库，也不把它们加入正式依赖；玩家安装原始 Refabricated JAR 时由其内嵌库负责加载。

嵌套库版本与声明：BCEL 6.6.1、Commons Math 3.6.1 的内置 LICENSE 为 Apache-2.0；Cardinal Components 在 1.20.1 为 5.2.3、1.21.1 为 6.1.2，元数据 MIT；SimpleBedrockModel 2.3.0.1（分别对应 Minecraft 版本）元数据 LGPL-3.0。LuaJ core/jse 3.0.8-figura、MAE 1.1.1 的选定内嵌 JAR 未声明独立许可证，保留这一未确认状态；不随本模组再分发。1.21.1 的 Config API Port 还内嵌 NightConfig core/toml 3.8.0，其 JAR 元数据未声明独立许可证；来源均为已校验的上游 JAR，未另行替换。

适配通过公开事件读取枪击前后的实际血量、弹药与爆头信息；通过声音事件或 vanilla SoundManager 的精确拦截替换命中声音。工程没有复制或内嵌上游的实现代码、音频或模型；开发探针和源码参考均不打包。项目页面与 JAR 的许可证描述分别记录，不将枪械代码许可证解释为其素材的无限制分发许可。
