# ShiftLore

Paper 插件：在 **打开背包或容器界面** 时，**按住 Shift（潜行）并悬停物品** 才展开详细 lore；仅潜行或仅悬停不会展开。

通过 **ProtocolLib** 向客户端发送带 lore 的容器包，**不修改**服务端物品 NBT，适合 CraftEngine 等自定义物品。

## 依赖

| 插件 | 必需 | 说明 |
| --- | --- | --- |
| [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) | 是 | 客户端 lore 同步 |
| [CraftEngine](https://modrinth.com/plugin/craftengine) | 否 | 用于按 CE 物品 ID（如 `rpg:frost_crystal`）匹配配置 |

- **Paper** 1.21.x（在 1.21.11 上构建）
- **Java** 21

## 构建

```bash
./gradlew jar
```

产物：`build/libs/ShiftLore-1.0.0.jar`（版本见 `build.gradle.kts`）。

部署到本地测试服（可选）：

```bash
./gradlew jar -PdeployServer=K:/MC/plugins
```

## 安装

1. 将 jar 放入服务端 `plugins/`
2. 确保 ProtocolLib 已启用
3. 重启；首次运行生成 `plugins/ShiftLore/config.yml` 与 `plugins/ShiftLore/items/`

## 配置

- `config.yml` — 全局选项
- `items/*.yml` — 按 CraftEngine 物品 ID 配置 `short` / `detail`（[MiniMessage](https://docs.advntr.dev/minimessage/)）

示例：

```yaml
profiles:
  rpg:frost_crystal:
    short:
      - "<gray>元素材料 · 冰"
      - "<dark_gray>按住 Shift 查看详情"
    detail:
      - "<yellow>来源"
      - "<gray>挖掘冰元素矿石……"
```

## 命令

- `/shiftlore reload` — 重载配置（权限 `shiftlore.admin`，默认 OP）

## 与本仓库作者的其他项目

四元素矿物示例配置见 `src/main/resources/items/rpg_ores.yml`；若与 [MC 生存服配置](https://github.com/chocochato0713) 联用，CE 侧只保留短 lore，详情由本插件负责。

## 许可证

MIT — 见 [LICENSE](LICENSE)
