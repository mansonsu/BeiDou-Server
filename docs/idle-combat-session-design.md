# Unity 放置版 IdleCombatSession 設計筆記

## 目標

本文件整理參考《TBH: 塔斯克巴·英雄》後，適合目前 BeiDou 後端與 Unity 前端採用的放置型玩法架構。重點不是複製 TBH 的 Steam 市集，而是保留它有效的核心循環：上線掛機、自動打怪、低頻結算、持續成長、寶箱與素材驅動的裝備追求。

## TBH 可參考的核心

- 小型常駐視窗，不依賴大型地圖探索。
- 隊伍、職業、技能、裝備、符文共同決定掛機效率。
- 玩家選擇關卡後，自動打怪並持續取得經驗、金幣、寶箱與素材。
- 關卡 Boss / 章節 Boss 作為進度門檻。
- 掉落與強化系統是主要留存來源。
- 玩家需要保持在線，才會持續取得有效掉落。

## 不建議照抄的部分

- 第一版不做 Steam 市集或玩家間交易。
- 不要每隻怪、每個掉落都即時同步封包。
- 不要讓 Unity 進入原本實體 MapleMap 後，再用原本怪物 AI、地上掉落物、多人同步來支撐放置版。
- 不要一開始就做離線收益；目前需求是玩家在線才持續打怪拿寶。

## 建議架構

放置版應新增獨立的 `IdleCombatSession`，它使用角色資料計算戰鬥，但不建立真實怪物、不把玩家移到真實多人地圖中跑 AI。

```text
Unity 選擇角色並登入
  -> 玩家已在原本流程進入地圖
  -> Unity 發送 IDLE_STAGE_ENTER(stageId)
  -> 後端建立或切換 IdleCombatSession
  -> 後端依照時間差低頻 tick
  -> Unity 發送 IDLE_STAGE_STATE 或後端回覆狀態摘要
  -> Unity 顯示簡化戰鬥、收益、掉落紀錄
  -> 玩家按領取，Unity 發送 IDLE_STAGE_CLAIM
  -> 後端把累積 EXP / 楓幣落到角色
```

## 第一版封包

放置版封包應與原始 MapleStory v83 opcode 有明顯區隔，目前建議從 `0x5000` 接收、`0x5100` 發送開始。

### Client -> Server

- `IDLE_STAGE_ENTER(0x5000)`：進入或切換放置關卡。Payload：`int stageId`。
- `IDLE_STAGE_STATE(0x5001)`：查詢目前放置狀態。
- `IDLE_STAGE_CLAIM(0x5002)`：領取目前累積獎勵。
- `IDLE_STAGE_EXIT(0x5003)`：離開放置模式。

### Server -> Client

- `IDLE_STAGE_RESULT(0x5100)`：回傳放置狀態或操作結果。
- `IDLE_STAGE_ERROR(0x5101)`：回傳放置系統錯誤。

## 第一版狀態封包格式

`IDLE_STAGE_RESULT`：

```text
byte action
byte success
int stageId
int elapsedSeconds
int totalKills
int pendingExp
int pendingMeso
int mapId
int monsterCount
repeat monsterCount:
  int monsterId
int rewardCount
repeat rewardCount:
  int itemId
  int quantity
int playerPower
int recommendedPower
string message
```

`action` 建議：

- `1`：enter
- `2`：state
- `3`：claim
- `4`：exit

## Idle Stage 設定

第一版先使用 Java 常數設定，等玩法穩定後再搬到資料表。

```text
stageId
name
requiredLevel
recommendedPower
killIntervalMillis
mapId
fallbackMonsterId
```

## 後端負載原則

- 不用每秒排程所有在線玩家。
- 每次玩家查詢、領取、離開時，根據 `lastTickMillis` 計算經過時間。
- 每個 session 最小 tick 單位可設為 5 秒或 10 秒。
- 每次結算只更新 session 記憶體；只有 claim 才寫角色 EXP / 楓幣 / 道具。
- EXP 使用抽到的怪物 `MonsterStats.exp`，並套用角色 `getExpRate()`、`getMobExpRate()`、EXP buff 與 family buff。
- 掉落直接使用關卡 `mapId` 對應的 WZ 地圖怪物配置，再依抽到的 `monsterId` 讀楓之谷 `drop_data` / `MonsterInformationProvider`。
- 同一怪物若在地圖 life 設定中有多個 spawn 點，放置模式抽怪時也會自然提高權重。
- `fallbackMonsterId` 只在 WZ 地圖讀不到怪物時使用，避免設定錯誤直接中斷流程。
- 放置模式第一版排除任務道具與 PQ 道具，避免污染任務流程；PQ 是 Party Quest 組隊任務專用道具。
- 楓幣使用怪物掉落表中的 `itemId = 0`，成功擲中後套用角色楓幣倍率與 `MESOUP` buff，再累積到 `pendingMeso`。

## 仍屬 Idle 自訂的部分

- `stageId`：放置模式自己的關卡 ID，不是楓之谷原始地圖 ID。
- `name`：放置關卡顯示名稱。
- `requiredLevel`：放置關卡進入門檻。
- `recommendedPower`：用來把角色能力換算成擊殺效率。
- `killIntervalMillis`：每隔多久產生一次基礎擊殺。
- `fallbackMonsterId`：WZ 地圖讀不到怪物時的保底怪物。
- `calculatePower`：Idle 自訂戰力公式，用角色總能力值、攻擊、魔攻與等級估算。
- `powerRatio`：Idle 自訂擊殺倍率，讓戰力高於或低於推薦值時影響擊殺數。

## Unity 第一版 UI 建議

第一階段先做服務層與可接收事件，不急著做完整 UI。後續 UI 可以拆成：

- 放置主面板：目前關卡、擊殺數、每分鐘收益、累積獎勵。
- 隊伍區：角色、HP/MP、簡化技能冷卻。
- 掉落紀錄：普通掉落、稀有掉落、寶箱。
- 操作列：進入、查詢、領取、離開。

## 建議開發順序

1. 建立 `IdleStageConfig` 與 `IdleCombatCalculator`。
2. 建立 `IdleCombatSession` 與 `IdleCombatService`。
3. 新增 socket handler 與 packet creator。
4. Unity 新增 opcode、狀態資料類、事件與 send methods。
5. 用手動呼叫封包確認 enter/state/claim/exit 可通。
6. 再做 Unity UI 與動畫。
7. 最後才接真實掉落表、背包、符文、方塊、寶箱。
