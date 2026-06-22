# IdleCombatSession Milestone

## 2026-06-15：建立第一版方向

### 已決策

- 放置版沿用既有 socket 與加解密流程。
- 玩家仍可走原本登入、選角、進地圖流程。
- 放置戰鬥不使用實體 MapleMap、怪物 AI、多人地圖同步與地上掉落物。
- 玩家必須在線，才會持續累積放置收益。
- 第一版只做 EXP / 楓幣結算與掉落摘要，不先寫入背包掉落物。

### 第一版後端目標

- 新增 `IdleStageConfig`：暫時用 Java 常數定義關卡。
- 新增 `IdleCombatResult`：承載 tick 結果。
- 新增 `IdleCombatCalculator`：純計算器，依角色能力與關卡計算收益。
- 新增 `IdleCombatSession`：保存玩家放置狀態與累積收益。
- 新增 `IdleCombatService`：管理角色的 session。
- 新增 socket handlers：
  - `IdleStageEnterHandler`
  - `IdleStageStateHandler`
  - `IdleStageClaimHandler`
  - `IdleStageExitHandler`
- 新增 packet：
  - `IDLE_STAGE_RESULT`
  - `IDLE_STAGE_ERROR`

### 第一版 Unity 目標

- 新增 `InOpCode` / `OutOpCode`。
- 新增 `IdleStageState` 資料類。
- 新增 `OnIdleStageResult` / `OnIdleStageError` 事件。
- 新增 send methods：
  - `SendIdleStageEnter(int stageId)`
  - `SendIdleStageState()`
  - `SendIdleStageClaim()`
  - `SendIdleStageExit()`
- 新增 result handler，先用事件與 log 驗證，不先做完整 UI。

### 待辦

- 第二階段接 Unity UI。
- 第三階段接真實掉落表與背包。
- 第四階段接符文、方塊、寶箱、Boss 門票。
- 第五階段做資料表與營運調參工具。

## 2026-06-15：第一版骨架已完成

### 後端已完成

- 新增 `org.gms.idle` 套件，建立第一版放置戰鬥核心：
  - `IdleStageConfig`
  - `IdleCombatResult`
  - `IdleCombatCalculator`
  - `IdleCombatSnapshot`
  - `IdleCombatSession`
  - `IdleCombatService`
- 新增第一批放置關卡設定，先用 Java 常數管理：
  - `20000`：弓箭手訓練場
  - `20010`：森林小徑
  - `20020`：燃燒木道
- 新增接收封包：
  - `IDLE_STAGE_ENTER(0x5000)`：放置版：進入或切換放置關卡
  - `IDLE_STAGE_STATE(0x5001)`：放置版：查詢目前放置狀態
  - `IDLE_STAGE_CLAIM(0x5002)`：放置版：領取目前累積獎勵
  - `IDLE_STAGE_EXIT(0x5003)`：放置版：離開放置關卡
- 新增送出封包：
  - `IDLE_STAGE_RESULT(0x5100)`：放置版：回傳放置關卡狀態或操作結果
  - `IDLE_STAGE_ERROR(0x5101)`：放置版：回傳放置關卡錯誤訊息
- 新增 handlers：
  - `IdleStageEnterHandler`
  - `IdleStageStateHandler`
  - `IdleStageClaimHandler`
  - `IdleStageExitHandler`
- 新增 `PacketCreator.idleStageResult(...)` 與 `PacketCreator.idleStageError(...)`。
- 第一版 `claim` 已能發放 EXP 與楓幣，掉落物目前只回傳摘要數量，尚未寫入背包。

### Unity 已完成

- 新增 `InOpCode` / `OutOpCode` 放置封包常數。
- 新增 `IdleStageState` 資料類，對應後端 `IDLE_STAGE_RESULT` 欄位。
- 新增事件：
  - `OnIdleStageResult`
  - `OnIdleStageError`
- 新增封包解析：
  - `HandleIdleStageResult(PacketReader reader)`
  - `HandleIdleStageError(PacketReader reader)`
- 新增送出方法：
  - `SendIdleStageEnter(int stageId)`
  - `SendIdleStageState()`
  - `SendIdleStageClaim()`
  - `SendIdleStageExit()`

### 驗證結果

- 後端執行 `.\build-windows.bat`：成功。
- Unity 執行 `dotnet build Assembly-CSharp.csproj`：成功，0 errors，保留既有 53 warnings。

### 目前限制

- Unity 尚未做畫面按鈕或正式 UI，所以現在只是網路層可對接。
- 目前不會在進入一般地圖後自動送 `IDLE_STAGE_ENTER`，避免再次干擾原本登入與進圖流程。
- 放置 session 只存在記憶體，伺服器重啟會清空。
- 掉落物還沒有接真實掉落表、背包欄位與道具發放。
- 關卡設定目前寫死在 Java，之後應改成資料表或設定檔。

### 下一個 milestone

- 在 Unity 做一個開發用 Idle Debug Panel：
  - 進入 `20000`
  - 查詢狀態
  - 領取獎勵
  - 離開放置
- Debug Panel 驗證成功後，再改成正式放置主畫面。
- 正式 UI 第一版只需要顯示：
  - 目前關卡
  - 累積秒數
  - 擊殺數
  - 待領 EXP
  - 待領楓幣
  - 玩家戰力與推薦戰力

## 2026-06-15：Unity Idle Debug Panel 已完成

### Unity 已完成

- 新增 `UIIdleDebugPanel`，用執行期自建 Canvas 的方式顯示，不需要先建立 prefab 或修改 scene。
- Debug Panel 預設顯示在畫面右上角，按 `F9` 可顯示或隱藏。
- Panel 目前提供：
  - Stage 輸入欄，預設 `20000`
  - `Enter`：送出 `SendIdleStageEnter(stageId)`
  - `State`：送出 `SendIdleStageState()`
  - `Claim`：送出 `SendIdleStageClaim()`
  - `Exit`：送出 `SendIdleStageExit()`
- Panel 已訂閱：
  - `MapleNetworkService.OnIdleStageResult`
  - `MapleNetworkService.OnIdleStageError`
- Panel 會顯示：
  - 後端訊息
  - 關卡 ID
  - 累積秒數
  - 擊殺數
  - 待領 EXP
  - 待領楓幣
  - 普通掉落摘要與 itemId
  - 稀有掉落摘要與 itemId
  - 玩家戰力 / 推薦戰力

### 驗證結果

- Unity 執行 `dotnet build Assembly-CSharp.csproj`：成功，0 errors，維持既有 53 warnings。
- 為了讓 `dotnet build` 編譯新腳本，驗證時曾暫時加入 `Assembly-CSharp.csproj` 編譯項目；驗證後已移除，避免提交 Unity 產生檔。

### 下一個 milestone

- 實機登入角色後，用右上角 Debug Panel 測試：
  - `Enter 20000`
  - 等待數秒
  - `State`
  - `Claim`
  - 確認 EXP / 楓幣是否真的增加
- 若封包流程穩定，下一步開始做正式 Idle 主畫面。
- 正式 UI 前，建議先補後端掉落物發放設計，避免 UI 先做死欄位。

## 2026-06-15：放置掉落物發放第一版已完成（已廢棄）

> 這一版曾暫時使用手動指定的普通/稀有 itemId，例如 `4000000`、`4010000`。這只是過渡實作，後續已改成直接套用楓之谷怪物掉落表，不應再沿用這個設計。

### 後端已完成

- `IdleStageConfig` 新增普通/稀有獎勵道具欄位：
  - `commonRewardItemId`
  - `rareRewardItemId`
- 第一批關卡暫定獎勵：
  - `20000`：普通 `4000000`，稀有 `4010000`
  - `20010`：普通 `4000001`，稀有 `4010001`
  - `20020`：普通 `4000002`，稀有 `4010002`
- `IdleCombatSession.claim(...)` 現在會在領取時發放：
  - EXP
  - 楓幣
  - 普通掉落物
  - 稀有掉落物
- 發放道具使用既有 `InventoryManipulator.addById(...)`，會走原本背包新增與更新封包。
- 領取前會用 `canHoldAllAfterRemoving(...)` 一次模擬多個獎勵道具的背包空間。
- 單一獎勵數量如果超過 `short` 上限，會切成多筆檢查與多筆發放。
- 如果背包空間不足，會回傳錯誤，而且不會清掉待領獎勵。

### 驗證結果

- 後端執行 `.\build-windows.bat`：成功。

### 目前限制

- 掉落物還不是從怪物真實掉落表抽出，而是每個放置關卡先指定一組普通/稀有 itemId。
- 現在回包已回傳普通/稀有掉落數量與 itemId，尚未回傳 itemName。
- 目前沒有做資料庫持久化，伺服器重啟仍會清空 session。

### 下一個 milestone

- Unity Debug Panel 已能顯示本次可領取或已發放的普通/稀有 itemId。
- 後端 `IDLE_STAGE_RESULT` 已增加「普通獎勵 itemId / 稀有獎勵 itemId」欄位。
- 若要正式做 UI，建議改成獎勵清單格式，不要長期維持 `commonDrops` / `rareDrops` 兩個硬欄位。

## 2026-06-15：Idle Result 回傳獎勵 itemId 已完成

> 這一版仍是普通/稀有兩欄封包，後續已改成 reward list。

### 已完成

- 後端 `IdleCombatSnapshot` 新增：
  - `commonRewardItemId`
  - `rareRewardItemId`
- 後端 `IDLE_STAGE_RESULT` 封包在掉落數量後新增兩個欄位：
  - `int commonRewardItemId`
  - `int rareRewardItemId`
- Unity `IdleStageState` 已同步新增這兩個欄位。
- Unity `HandleIdleStageResult(...)` 已同步解析新封包順序。
- Unity `UIIdleDebugPanel` 已顯示：
  - `普通掉落：數量 x itemId`
  - `稀有掉落：數量 x itemId`

### 下一個 milestone

- 如果要接正式 UI，下一步應改成獎勵明細清單格式：
  - `rewardCount`
  - 多筆 `itemId / quantity / rarity`
- 然後 Unity 可以用同一份資料渲染掉落列表，不需要硬寫普通與稀有兩欄。

## 2026-06-15：改用楓之谷怪物掉落表

### 已完成

- 移除手動指定的普通/稀有獎勵 itemId 設計。
- `IdleStageConfig` 改成每個放置關卡指定 `mapId` 與 `fallbackMonsterId`。
- `IdleCombatSession` 依擊殺數從 WZ 地圖 `life` 設定抽怪，再讀取 `MonsterInformationProvider.retrieveEffectiveDrop(monsterId)`，直接使用既有 `drop_data`。
- 放置掉落累積改成 `itemId -> quantity`。
- `IDLE_STAGE_RESULT` 改成 reward list：
  - `int mapId`
  - `int monsterCount`
  - 多筆 `int monsterId`
  - `int rewardCount`
  - 多筆 `int itemId`
  - 多筆 `int quantity`
- Unity `IdleStageState` 改成 `PendingRewards` 清單。
- Unity Debug Panel 改成顯示地圖、怪物清單與多筆待領道具。

### 目前規則

- 任務道具與 PQ 道具先排除，避免放置模式影響任務流程。
- PQ 是 Party Quest 組隊任務專用道具，通常只應該存在於組隊任務流程。
- 怪物掉落中的楓幣掉落 `itemId = 0` 已納入放置結算，成功擲中後會累積到 `pendingMeso`。
- `pendingMeso` 只是尚未領取的楓幣暫存值，不代表另有一套放置專用楓幣表。
- 裝備掉落會依照數量拆成一件一件發放，避免 `InventoryManipulator.addById(...)` 的裝備數量限制。
- 同一怪物如果在地圖 WZ life 設定有多個 spawn 點，抽怪時會保留這個權重。
- 若 `mapId` 讀不到任何怪物，才會退回使用 `fallbackMonsterId`。

### 下一個 milestone

- 實機測試 `Enter 20000 -> State -> Claim`，確認背包真的收到該 `mapId` 內怪物群的掉落物。
- 補 item name 顯示：Unity 可用本地 WZ item string，或後端封包額外回傳名稱。
- 之後把 `IdleStageConfig` 從 Java 常數搬到資料表或設定檔，欄位只需要關卡資料、`mapId` 與 fallback，不需要重建掉落表。

## 2026-06-15：放置關卡改讀地圖怪物群

### 已完成

- `MapFactory` 新增只讀 WZ 地圖怪物 ID 的 helper。
- Idle 不建立 `MapleMap`、不 spawn 怪物，只讀 `map/life` 中 `type = m` 的怪物 ID。
- `IdleStageConfig` 保留放置用 `stageId`，另外新增楓之谷 `mapId`。
- 每次放置擊殺會從該地圖怪物清單抽一隻怪，再用該怪既有掉落表擲掉落。
- 封包改回傳 `mapId` 與 `monsterIds` 清單。
- Unity Debug Panel 改顯示地圖 ID 與怪物 ID 清單。

## 2026-06-15：放置楓幣改用怪物原始掉落

### 已完成

- 移除 Idle 關卡設定中的 `baseMesoPerKill`。
- `IdleCombatCalculator` 不再產生楓幣，避免與怪物掉落表重複。
- `IdleCombatSession` 將 `drop_data` 內 `itemId = 0` 視為楓幣掉落。
- 楓幣掉落會套用角色 `getMesoRate()` 與 `MESOUP` buff。
- 這一版曾透過 `pendingMeso` 一次發給角色；後續已改成 tick 當下直接發放。

## 2026-06-15：放置 EXP 改用怪物原始 EXP

### 已完成

- 移除 Idle 關卡設定中的 `baseExpPerKill`。
- `IdleCombatCalculator` 不再產生 EXP，只負責計算本次應有擊殺數。
- `IdleCombatSession` 每次抽到怪物後，讀取 `LifeFactory.getMonster(monsterId).getExp()`。
- EXP 會套用角色 `getExpRate()`、`getMobExpRate()`、`EXP_INCREASE`、`EXP_BUFF` 與 family buff。
- 這一版曾透過 `pendingExp` 一次發給角色；後續已改成 tick 當下直接發放。

### 目前仍是 Idle 自訂

- 放置關卡 ID、顯示名稱、進入等級、推薦戰力。
- 角色戰力公式。
- 戰力影響擊殺數的倍率。
- 擊殺間隔。
- WZ 地圖讀不到怪物時使用的 fallback 怪物。

## 2026-06-15：EXP 與楓幣改為 tick 直接發放

### 已完成

- 移除 `IdleCombatSession` 內的 `pendingExp` 與 `pendingMeso` 狀態。
- 每次 idle tick 擲出怪物 EXP 後，立刻呼叫 `Character.gainExp(...)`。
- 每次 idle tick 擲出怪物楓幣後，立刻呼叫 `Character.gainMeso(...)`。
- `claim` 現在只觸發一次即時結算；EXP / 楓幣 / 道具都在 tick 當下直接發放。
- `IDLE_STAGE_RESULT` 原本兩個位置保留，但語意改為 `gainedExp` / `gainedMeso`，代表本次 tick 直接獲得的數量。
- Unity Debug Panel 改顯示「本次 EXP / 本次楓幣」。

## 2026-06-15：放置同步改為 5 秒並支援 Prototype 戰鬥演出

### 已完成

- 後端放置狀態主動推送週期從 10 秒改為 5 秒。
- `IDLE_STAGE_RESULT` 追加本輪演出用欄位：
  - `lastKills`：本輪擊殺數。
  - `lastDamage`：本輪估算每次攻擊傷害。
  - `lastMonsterId`：本輪主要演出怪物 ID。
  - `attackIntervalMillis`：前端播放攻擊節奏用間隔。
- `IdleCombatSession` 會保留未打完怪物的 `carriedDamage`，即使本輪沒有擊殺也不會遺失傷害進度。
- Unity `MapleNetworkService` 已解析新增欄位。
- Unity `UIIdleDebugPanel` 改成簡易放置戰鬥 prototype 面板：
  - 左側玩家、右側怪物。
  - 收到 server 結果後播放攻擊位移、傷害飄字、擊破提示。
  - 顯示本輪擊殺、總擊殺、本輪 EXP、楓幣、戰力與本輪取得道具。

### 下一個 milestone

- 把目前 UI 方塊替換成真正角色與怪物 sprite / animator。
- 依職業選擇預設放置技能，讓 `lastDamage` 與演出技能一致。
- 讓掉落物以圖示飛入本輪取得獎勵列表。
