# Hypixel Zombies Mod

一个面向 Hypixel Zombies / 僵尸末日模式的 Fabric 客户端辅助 Mod，主要提供 HUD 信息显示、队友状态追踪、枪械切换、波次提示、实体可视化与若干游戏体验优化功能。

当前分支：

| Branch | 用途 |
| --- | --- |
| `mc-26.1.2` | 稳定版 |
| `mc-26.2-test` | 26.2 迁移测试版，可能存在兼容问题 |

## Screenshot

![截图](assets/screenshot.png)

## Features

### Combat / Gun

| Module | 功能 |
| --- | --- |
| Auto Switch Weapon | 根据枪械配置自动切换武器，支持冷却模式与换弹辅助 |
| Right Clicker | 右键连点器，可限制只对枪械生效 |
| No Gun Fire | 屏蔽开枪时的火焰遮挡 |
| Damage Numbers | 显示伤害与金币数字，支持按武器和强化状态区分颜色 |
| DPS Counter | 实时统计 DPS，便于测试输出效率 |

### Zombies HUD

| Module | 功能 |
| --- | --- |
| Target Hud | 显示当前目标血量、距离等信息 |
| Wave Display | 显示 AA 波次，并用箭头标记当前波 |
| Teammates Glow | 队友高亮与队友信息显示 |
| Lightning Rod Queue | 记录团队 Lightning Rod 使用队列与 20 秒冷却 |
| Stats Query | 查询玩家 Zombies 战绩 |
| AA Powerup Predictor | 预测 AA 道具掉落 |
| Notification | 回合计时与 AA 回合建议提示 |

### Visual / Render

| Module | 功能                             |
| --- |--------------------------------|
| Zombie Chams | 僵尸穿墙显示                         |
| Bad Headshot | 标记无法正常暴击的实体，并用红色碰撞箱标记出来        |
| Hide Blocking Player | 玩家重叠遮挡时自动隐藏                    |
| Hide Zombies | 僵尸靠近遮挡时自动隐藏，LS时能看清楚            |
| No Fire Effect | 降低或去除屏幕火焰遮挡                    |
| Hologram Fix | 改善 Zombies 全息文字交互体验，支持忽略方块右键反应 |

### Utility

| Module | 功能 |
| --- | --- |
| Sprint | 强制疾跑 |
| Name Protect | 名字保护 |
| Hud | 基础 HUD 显示与调试入口 |

## Notes

- 本项目主要针对 Hypixel Zombies / 僵尸末日玩法开发。
- 功能依赖记分板、聊天消息、监听发包事件, 但不存在修改服务器发包等操作。
- 大部分功能为改本地客户端（即Mod行为），不存在任何改包操作，因此基本不会有封禁风险

## Credits

- [Hypixel Public API](https://github.com/HypixelDev/PublicAPI)
- 部分数据与思路参考：[ShowSpawnTime by Seosean](https://github.com/Seosean/ShowSpawnTime)

## Disclaimer

本项目仅用于学习、研究与个人使用。
