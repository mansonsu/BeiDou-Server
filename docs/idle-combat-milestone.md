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

## 2026-06-15：放置掉落物發放第一版已完成

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
