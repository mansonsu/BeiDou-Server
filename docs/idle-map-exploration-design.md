# Unity 探索地圖與放置刷怪設計

## 目標

Unity 放置版讓玩家選擇一張要探索的楓之谷地圖，後端記錄這個探索地圖，後續掛機刷怪、刷寶、戰鬥動畫都以這張地圖的怪物與掉落表為基準。

這個探索地圖不是原本楓之谷角色所在的 `characters.map`，也不呼叫原本 `ChangeMap` / `MapleMap` 傳送流程。玩家在楓之谷 socket 世界中的真實站位可以不變，Unity 放置玩法只讀寫獨立探索狀態。

## 核心規則

- 玩家可以選擇要探索的 `mapId`。
- 後端只記錄目前探索地圖，不把人物傳送到該地圖。
- 探索地圖必須存在 WZ 怪物配置，且怪物清單不可為空。
- 掛機刷怪只能從該地圖的怪物清單抽怪。
- 掉落只能使用被抽到怪物的既有掉落表。
- 戰鬥動畫可以隨機顯示該地圖會出現的怪物，但動畫不決定獎勵。
- 小關卡可以視為無限循環演出，不需要固定章節關卡或 Boss 關卡。
- 戰鬥傷害應由後端依玩家真實數值與怪物數值計算。

## 權威來源

後端是唯一權威來源：

- client 可以送：選擇探索地圖、查詢探索狀態。
- client 不可以送：掉落道具、怪物 id、傷害、最終獎勵。
- 若未來保留「小關卡打完後回報擊殺數」，後端也只能把它當作演出進度參考，必須用 server time、角色能力、地圖怪物數值限制合理擊殺數。

更安全的版本是後端自行依時間結算擊殺數，client 完全不回報擊殺數。前端只負責播動畫與顯示結果。

## 第一版封包

Recv：

- `IDLE_EXPLORE_SELECT(0x5004)`：選擇探索地圖。Payload：`int mapId`。
- `IDLE_EXPLORE_STATE(0x5005)`：查詢目前探索地圖。

Send：

- `IDLE_EXPLORE_RESULT(0x5102)`：回傳探索地圖狀態。
- `IDLE_EXPLORE_ERROR(0x5103)`：回傳探索地圖錯誤。

`IDLE_EXPLORE_RESULT`：

```text
byte action
bool success
bool hasMap
int mapId
string streetName
string mapName
int monsterCount
int monsterId[monsterCount]
long startedAtMillis
long updatedAtMillis
string message
```

## 資料表

`idle_exploration_state`：

- `characterid`：角色 id，主鍵。
- `explore_map_id`：目前探索地圖。
- `started_at`：這次開始探索該地圖的時間。
- `updated_at`：最後更新時間。

這張表只存放置玩法狀態，不覆蓋 `characters.map`。

## 後續掛機結算方向

1. `IDLE_EXPLORE_SELECT(mapId)` 記錄目前探索地圖。
2. server 根據 `idle_exploration_state.explore_map_id` 讀 WZ map life。
3. 每次 tick 從該地圖怪物池抽怪。
4. 用玩家能力與怪物 HP / 防禦 / 等級等數值計算合理擊殺。
5. 用怪物掉落表擲 EXP / 楓幣 / 道具。
6. 獎勵直接發給玩家背包與角色資料。
7. 回包給 Unity 顯示本輪擊殺、掉落與動畫用怪物清單。

## 防作弊重點

- 不接受 client 指定掉落物。
- 不接受 client 指定怪物 id 作為獎勵依據。
- 不接受 client 指定傷害。
- 若接受 client 回報擊殺數，必須用後端計算出的上限裁切。
- 探索地圖必須 server 驗證，之後應加入等級、任務、區域解鎖或戰力需求。
- claim / tick 必須是原子流程，避免同一段時間多次領獎。
- 離線與在線收益都應以 server timestamp 計算。
