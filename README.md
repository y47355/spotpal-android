# 搭趣 SpotPal · Android 客户端

基于**兴趣 + 位置**的搭子匹配轻社交 App。核心差异化：**AI 代协商**——把「约人时最难的沟通」交给 AI，用户只需上推一个意图。

> 客户端详细设计见姊妹仓 [`spotpal-docs`](../spotpal-docs)：《详细设计-客户端.md》

## 技术栈

| 层 | 选型 |
|---|---|
| UI | Kotlin 2.0 + Jetpack Compose + Material 3 |
| 架构 | MVI（Intent/State/Effect 三件套） + 单 Activity 多模块 |
| 网络 | Retrofit + OkHttp（kotlinx-serialization 转换器） |
| 实时 | WebSocket（自研 `WsClient`：seq 先落盘、resume 补拉、指数退避重连） |
| 本地 | Room（ws_messages / candidate_pool / negotiations 三表） |
| 依赖注入 | demo 用 `ApiGraph` ServiceLocator（生产迁 Hilt，迁移点唯一） |

## 模块结构（Gradle 多模块）

```
spotpal-android/
├── app/                    # 壳：MainActivity + RootNavHost（3 Tab 6 屏）
├── core/
│   ├── design/             # 设计系统：SpotPalColor/Shapes/Gradients + 上推托付手势
│   │   └── gesture/        #   GestureSpec（600ms/80dp/12dp）+ PushableCard 骨架 ★
│   ├── model/              # 纯 DTO（与服务端 JSON 一一对应）
│   ├── network/            # SpotPalApi 12 端点 + WsClient + SafeApi 门面
│   ├── common/             # AppResult 三态封装 + 退避工具
│   └── data/               # Room 三表 + SessionStore（token/last_seq/pref）
└── feature/                # 按屏组织（MVI）
    ├── discover/           # ① 发现（上推托付手势）
    ├── profile/            # ② 搭子主页
    ├── negotiate/          # ③ AI 协商（方案卡气泡流 + round/5 进度）
    ├── confirm/            # ④ 搭局确认（三模式费用切换 + 打卡结算）
    ├── squads/             # ⑤ 我的搭局（状态驱动卡片）
    └── me/                 # ⑥ 我的（信用分暗卡）
```

## 快速开始

```bash
# 1. 启动本地服务端（见 spotpal-server 仓）
cd spotpal-server && make dev        # 监听 :8080

# 2. Android Studio 打开本工程，Run app
#    debug 变体默认连 http://10.0.2.2:8080（模拟器直达宿主机）
#    真机：Settings → 同 Wi-Fi 局域网 IP 替换 API_BASE/WS_BASE
```

## 核心交互：上推托付手势

对应设计稿 4 帧：**静置 → 长按微调 → 上推托付 → 释放锁定**。

- 长按 600ms 弹出微调面板（时间滑块）
- 上推过 80dp 阈值 → 震动反馈 + 卡片缩至 0.6 倍
- 松手：过阈值则上飞入池（`POST /v1/pool/push`）；否则 spring 回弹
- 全程只动 `graphicsLayer` 属性（translationY/scale），不触发重组布局

参数全部收口在 `GestureSpec`（`core/design/gesture/`），便于调参与边界测试（79dp vs 80dp 参数化扫描）。

## 联调对照（与 spotpal-server）

| 客户端动作 | 服务端端点 | 验证点 |
|---|---|---|
| 上推松手 | `POST /v1/pool/push` | 弱网重试同幂等键，返回同一记录 |
| 释放委托 | `POST /v1/negotiation/release` | WS 收 `negotiation.proposal` 时延（mock LLM ≈2s） |
| 断网恢复 | WS `resume last_seq` + REST 补拉 | 飞行模式开关 10 次零丢失零重复 |
| 确认成局 | `POST /v1/squad/{id}/confirm` | 双端同时确认竞态（乐观锁兜底） |
| 打卡结算 | `POST /v1/squad/{id}/checkin` | 纯记账流转（无支付拉起） |

## 许可

MIT（见 [LICENSE](../LICENSE) 或仓库根目录）。
