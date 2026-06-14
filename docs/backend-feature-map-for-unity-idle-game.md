# BeiDou 後端可供前端與 Unity 使用的功能盤點

盤點日期：2026-06-14  
範圍：`gms-server` Java 後端、`gms-ui/src/api` 既有前端呼叫、資料庫 migration、socket packet handler 與主要遊戲服務。

## 結論先看

`BeiDou-Server_git` 的後端不是單純 Web API 專案，而是「楓之谷遊戲伺服器 + Spring Boot 管理 API + 資料庫內容管理 + WZ/XML 遊戲資料 + JS 腳本系統」。目前可以直接給 Web 或 Unity 使用的是 Spring Boot REST API；真正遊戲邏輯則多數仍藏在原楓之谷 socket 封包處理器與 Java 物件模型裡。

如果要做成類似 `ms_web` 的放置型遊戲 Unity 版本，不建議 Unity 第一版直接接原始 MapleStory socket protocol。比較穩的做法是：保留後端資料庫與核心服務，新增一層 Unity 專用 REST API 或 BFF，先做帳號、角色、背包、裝備、掉落、離線收益、商店與轉蛋，再逐步把任務、技能、寵物、社群與活動接進來。

## API 共通規則

- API 版本目前固定為 `v1`，來源：`ApiConstant.LATEST = V1`。
- REST 回應格式為 `ResultBody<T>`：
  - `code`：成功時為 `20000`。
  - `message`：結果訊息。
  - `responseId`：後端產生或沿用前端 `requestId`。
  - `data`：實際資料。
- 除 `/auth/**`、Swagger 與靜態檔外，其餘 REST API 都需要 JWT。
- 前端目前會把 POST/PUT body 包成：

```json
{
  "requestId": "uuid",
  "data": {}
}
```

- Unity 若直接打現有 API，也應照這個格式送出 JSON，並在 header 加上：

```http
Authorization: Bearer <token>
```

## 目前可直接使用的 REST API 系統

### 1. 認證與帳號系統

來源：`AuthController`、`AccountController`、`SpringSecurityConfig`

可用功能：

- 登入取得 JWT：`POST /auth/v1/login`
- 登出：`DELETE /auth/v1/logout`
- 更新 token：`GET /auth/v1/refreshToken`
- 取得目前帳號資訊：`GET /account/v1/info`
- 帳號列表查詢：`GET /account/v1`
- 註冊帳號：`POST /account/v1`
- 玩家修改自身帳號資料：`PUT /account/v1`
- GM 修改帳號：`PUT /account/v1/{id}`
- 刪除帳號：`DELETE /account/v1/{id}`
- 重置登入狀態：`PUT /account/v1/{id}/reset/logged`
- 封鎖帳號：`PUT /account/v1/{id}/ban`
- 解封帳號：`PUT /account/v1/{id}/unban`

Unity 用途：

- 遊戲登入、註冊、token refresh。
- 玩家資料入口。
- 後台 GM 工具可共用。

注意：

- 現有 API 偏管理後台，不是完整玩家端 API。
- Unity 玩家端應新增更乾淨的 `/game/v1/me`、`/game/v1/register`、`/game/v1/session` 類型 API，避免暴露 GM 管理欄位。

### 2. 伺服器狀態與世界頻道系統

來源：`ServerController`、`ServerService`

可用功能：

- 關閉 Spring Boot 程序：`GET /server/v1/shutdown`
- 停止遊戲伺服器：`GET /server/v1/stopServer`
- 帶訊息與延遲停止伺服器：`POST /server/v1/stopServerWithMsgAndInternal`
- 啟動遊戲伺服器：`GET /server/v1/startServer`
- 重啟遊戲伺服器：`GET /server/v1/restartServer`
- 查詢是否在線：`GET /server/v1/online`
- 世界列表：`GET /server/v1/world/list`
- 頻道列表：`GET /server/v1/channel/list?worldId=...`
- 版本查詢：`GET /server/v1/version`

Unity 用途：

- 登入前顯示伺服器狀態。
- 選世界、選頻道。
- 維護公告與關機倒數可延伸。

注意：

- `shutdown`、`stopServer`、`restartServer` 屬高風險 GM API，不應開給一般 Unity 客戶端。

### 3. 角色與線上玩家系統

來源：`CharacterController`、`CharacterService`、`characters` 資料表

可用功能：

- 更新角色倍率：`POST /character/v1/updateRate`
- 重置單一倍率：`POST /character/v1/resetRate`
- 重置所有倍率：`GET /character/v1/resetRates`
- 線上角色列表：`POST /character/v1/online/list`

既有資料層能力：

- `characters` 保存角色基礎資料。
- `characterexplogs` 保存經驗紀錄。
- `extend_value` 可保存角色擴充倍率，例如經驗、楓幣、掉寶倍率。

Unity 用途：

- 玩家角色選擇。
- 放置收益倍率。
- 角色狀態與戰力顯示。

缺口：

- 現有 REST 沒有完整「建立角色、取得我的角色、取得角色詳細狀態、升級、配點」玩家 API。
- 這些能力在原 socket login/channel handler 內存在，但需要抽成 REST service。

### 4. 背包與裝備系統

來源：`InventoryController`、`InventoryService`、`inventoryitems`、`inventoryequipment`

可用功能：

- 背包類型列表：`GET /inventory/v1/getInventoryTypeList`
- 依條件查角色列表：`POST /inventory/v1/getCharacterList`
- 查角色背包：`POST /inventory/v1/getInventoryList`
- 更新背包物品：`POST /inventory/v1/updateInventory`
- 刪除背包物品：`POST /inventory/v1/deleteInventory`

既有資料層能力：

- 裝備屬性：力量、敏捷、智力、幸運、攻擊、魔攻、防禦、命中、迴避、速度、跳躍、卷軸槽、過期時間等。
- 一般道具、裝備、現金道具、倉庫與商人背包都有資料表支撐。

Unity 用途：

- 放置型遊戲第一版核心：道具掉落、裝備穿脫、裝備強化、背包管理。
- 可用既有裝備資料轉換成 Unity UI 顯示。

缺口：

- 現有 API 偏 GM 編輯背包。
- Unity 玩家端需要新增：取得我的背包、裝備物品、卸下裝備、販售、分解、批次領取掉落。

### 5. 發送資源與獎勵系統

來源：`GiveController`、`GiveService`

可用功能：

- 發送資源：`POST /give/v1/resource`

可發送內容從 `gms-ui/src/api/player.ts` 可見：

- 目標世界、角色 ID、角色名稱。
- 類型、物品 ID、數量、倍率。
- 裝備屬性：力量、敏捷、智力、幸運、HP、MP、物攻、魔攻、物防、魔防、命中、迴避、速度、跳躍、卷軸槽、過期時間。

Unity 用途：

- GM 補償。
- 活動獎勵。
- 放置收益結算。
- 新手禮包、每日登入、任務獎勵。

注意：

- 這個 API 權限應嚴格限制，不應讓 Unity 客戶端直接呼叫。
- 玩家領獎應走新的「後端驗證後發送」API。

### 6. 掉落表與全域掉落系統

來源：`DropController`、`DropService`、`drop_data`、`drop_data_global`

可用功能：

- 怪物掉落查詢：`POST /drop/v1/getDropList`
- 全域掉落查詢：`POST /drop/v1/getGlobalDropList`
- 新增怪物掉落：`PUT /drop/v1/addDropData`
- 新增全域掉落：`PUT /drop/v1/addGlobalDropData`
- 更新怪物掉落：`POST /drop/v1/updateDropData`
- 更新全域掉落：`POST /drop/v1/updateGlobalDropData`
- 刪除怪物掉落：`DELETE /drop/v1/deleteDropData/{id}`
- 刪除全域掉落：`DELETE /drop/v1/deleteGlobalDropData/{id}`

Unity 用途：

- 放置戰鬥最重要的內容來源。
- 地圖、怪物、關卡可對應到 `dropperId`。
- 離線收益可用掉落表做抽獎池。

建議：

- 第一版先把掉落表轉成「關卡掉落池」。
- 不必一開始完整模擬楓之谷即時打怪，只需要根據玩家戰力、關卡、時間、掉落率計算收益。

### 7. NPC 商店系統

來源：`ShopController`、`ShopService`、`shops`、`shopitems`

可用功能：

- 商店列表：`POST /shop/v1/getShopList`
- 商店物品列表：`POST /shop/v1/getShopItemList`
- 單一商店物品：`GET /shop/v1/getShopItem/{id}`
- 新增商店物品：`PUT /shop/v1/addShopItem`
- 更新商店物品：`POST /shop/v1/updateShopItem`
- 刪除商店物品：`DELETE /shop/v1/deleteShopItem/{id}`

Unity 用途：

- 一般商店、藥水商店、材料商店。
- 放置遊戲可改成「固定商店」「每日商店」「關卡解鎖商店」。

缺口：

- 現有 API 是管理商店資料，不是玩家購買流程。
- 需要新增玩家購買 API：檢查價格、扣楓幣或點數、塞入背包。

### 8. 現金商店系統

來源：`CashShopController`、`CashShopService`、`modified_cash_item`、`specialcashitems`、`wishlists`

可用功能：

- 現金商店分類：`GET /cashShop/v1/getAllCategoryList`
- 依分類查商品：`POST /cashShop/v1/getCommodityByCategory`
- 依 SN 查商品：`GET /cashShop/v1/getCommodityBySn/{sn}`
- 商品上架：`POST /cashShop/v1/onSale`
- 商品下架：`POST /cashShop/v1/offSale`
- 批次上架或調整：`POST /cashShop/v1/batchOnSale`

Unity 用途：

- 商城展示。
- 外觀、寵物、便利道具、加速券。
- 放置型版本可轉成鑽石商店或 NX 商店。

注意：

- 若要接真實付費，需另做付款訂單、平台驗證與發貨，不要只靠現有上架資料。

### 9. 轉蛋與抽獎池系統

來源：`GachaponController`、`GachaponService`、`gachapon_reward_pool`、`gachapon_reward`

可用功能：

- 轉蛋池列表：`POST /gachapon/v1/getPools`
- 更新轉蛋池：`POST /gachapon/v1/updatePool`
- 刪除轉蛋池：`POST /gachapon/v1/deletePool`
- 查轉蛋池獎勵：`POST /gachapon/v1/getRewards`
- 更新獎勵：`POST /gachapon/v1/updateReward`
- 刪除獎勵：`POST /gachapon/v1/deleteReward`

既有遊戲封包也包含：

- `RemoteGachaponHandler`
- `UseGachaExpHandler`
- `CouponCodeHandler`

Unity 用途：

- 抽裝備、抽道具、抽寵物。
- 活動池、常駐池、保底規則可以在此基礎上延伸。

缺口：

- 現有 REST 管理的是池與獎勵，不是玩家抽卡流程。
- Unity 需要新增：抽一次、抽十次、消耗券或貨幣、寫入背包、保存抽卡紀錄。

### 10. 遊戲設定與倍率系統

來源：`ConfigController`、`GameConfig`、`game_config`、`lang_resources`

可用功能：

- 設定分類列表：`GET /config/v1/getConfigTypeList`
- 設定列表：`POST /config/v1/getConfigList`
- 新增設定：`POST /config/v1/addConfig`
- 更新設定：`POST /config/v1/updateConfig`
- 刪除設定：`DELETE /config/v1/deleteConfig/{id}`
- 批次刪除設定：`POST /config/v1/deleteConfigList`
- 匯入 YAML：`POST /config/v1/importYml`
- 匯出 YAML：`GET /config/v1/exportYml`

已有設定方向：

- 經驗倍率、楓幣倍率、掉落倍率、Boss 掉落倍率、任務倍率、旅行倍率、釣魚倍率。
- 玩家指令限制。
- 傷害排名相關設定。
- 註冊 IP 限制。
- 是否允許偷取任務道具等遊戲機制。

Unity 用途：

- 放置收益倍率。
- 活動倍率。
- 營運調參。
- 玩家端顯示伺服器規則。

### 11. 查詢與資料工具系統

來源：`CommonController`、`FileController`

可用功能：

- 依物品 ID 查裝備初始資訊：`POST /common/v1/getEquipmentInfoByItemId`
- 查所有世界線上人數：`POST /common/v1/getAllWorldsOnlinePlayersCount`
- 資訊搜尋：`POST /common/v1/informationSearch`
- 讀取檔案樹內容：`POST /file/v1/tree/read`
- 寫入檔案樹內容：`POST /file/v1/tree/write`
- 查檔案樹：`POST /file/v1/tree`

Unity 用途：

- 物品百科。
- 裝備詳情。
- 怪物、地圖、NPC、技能、任務搜尋。
- 開發工具與營運工具。

注意：

- `file` API 可以讀寫伺服器檔案，應只給內部後台使用，不應開給 Unity 客戶端。

### 12. GM 指令與熱重載系統

來源：`CommandController`、`command_info`、`client.command.commands.*`

可用功能：

- GM 指令列表：`POST /command/v1/getCommandListFromDB`
- 更新 GM 指令：`POST /command/v1/updateCommand`
- 重新載入事件腳本：`GET /command/v1/reloadEventsByGMCommand`
- 重新載入 portal 腳本：`GET /command/v1/reloadPortalsByGMCommand`
- 重新載入地圖：`GET /command/v1/reloadMapsByGMCommand`

Unity 用途：

- 內部 GM 後台。
- 開發期間重載活動與地圖。
- 線上營運工具。

注意：

- 不應直接給玩家端。

### 13. 自動封鎖與風控系統

來源：`AutobanConfigController`、`AutobanFactory`、`autoban_config`、`ipbans`、`macbans`、`hwidbans`

可用功能：

- 查自動封鎖設定：`GET /autoban/v1/getConfigList`
- 更新自動封鎖設定：`POST /autoban/v1/updateConfig`

既有能力：

- IP ban、MAC ban、HWID ban。
- 封包異常、攻擊異常、非法操作等偵測點散落在 packet handler。

Unity 用途：

- 防止異常收益、異常請求頻率、重複領獎。
- 放置型遊戲可加入每日收益上限、請求節流與伺服器端結算。

## 後端已具備但目前主要透過 socket 使用的遊戲系統

以下系統不是完整 REST API，但在 Java 遊戲伺服器、資料表或 handler 裡已經存在。Unity 若要使用，建議逐步抽出 service，再包成 Unity 專用 API。

### 1. 登入、選角與角色建立

來源 handler：

- `LoginPasswordHandler`
- `ServerlistRequestHandler`
- `ServerStatusRequestHandler`
- `CharlistRequestHandler`
- `CreateCharHandler`
- `DeleteCharHandler`
- `CheckCharNameHandler`
- `CharSelectedHandler`
- `PlayerLoggedinHandler`

可轉成 Unity API：

- 取得我的角色列表。
- 檢查角色名稱。
- 建立角色。
- 刪除角色。
- 選擇角色並進入遊戲狀態。

### 2. 地圖、移動、傳送與場景系統

來源：

- `MapleMap`、`MapFactory`、`MapManager`
- `ChangeMapHandler`
- `ChangeMapSpecialHandler`
- `MovePlayerHandler`
- `PortalScriptManager`
- `MapScriptManager`
- `wz/Map.wz` 與 `wz-zh-CN/Map.wz`

可轉成 Unity API：

- 取得目前地圖。
- 解鎖地圖。
- 切換放置關卡。
- 關卡怪物池與掉落池。

放置型建議：

- 不要先做即時地圖同步。
- 第一版把地圖當成「章節 / 關卡 / 掛機地點」。

### 3. 戰鬥、技能與怪物系統

來源：

- `CloseRangeDamageHandler`
- `RangedAttackHandler`
- `MagicDamageHandler`
- `SpecialMoveHandler`
- `TakeDamageHandler`
- `MobSkillFactory`
- `MonsterInformationProvider`
- `SkillFactory`
- `skills`、`skillmacros`、`monsterbook`、`monstercarddata`

可轉成 Unity API：

- 角色戰力計算。
- 掛機傷害與擊殺速度。
- 技能升級。
- 怪物圖鑑。
- Boss 挑戰結果結算。

放置型建議：

- 初版用 deterministic formula：角色戰力、地圖需求戰力、每分鐘擊殺量、掉落率。
- 之後再接技能動畫與主動戰鬥。

### 4. 任務與腳本系統

來源：

- `QuestActionHandler`
- `Quest`
- `QuestRequirementType`
- `QuestActionType`
- `QuestScriptManager`
- `scripts/quest`
- `scripts-zh-CN/event`
- `queststatus`、`questprogress`、`questactions`、`questrequirements`

可轉成 Unity API：

- 任務列表。
- 接任務、完成任務。
- 每日任務、成就任務。
- 放置目標任務，例如擊殺 N 隻怪、收集 N 個物品。

注意：

- 原本 NPC 腳本對話流程比較適合即時端遊，不適合直接搬到 Unity 放置型。
- 建議先做資料化任務，不直接跑所有 JS 腳本。

### 5. NPC 對話、商店與腳本互動

來源：

- `NPCTalkHandler`
- `NPCMoreTalkHandler`
- `NPCShopHandler`
- `NPCScriptManager`
- `scripts/npc`
- `scripts-zh-CN/npc`

可轉成 Unity API：

- NPC 清單。
- NPC 商店。
- NPC 任務入口。
- NPC 對話內容。

放置型建議：

- 第一版不需要完整腳本對話機。
- 只抽出商店、任務與功能 NPC。

### 6. 寵物系統

來源：

- `SpawnPetHandler`
- `MovePetHandler`
- `PetCommandHandler`
- `PetFoodHandler`
- `PetLootHandler`
- `PetAutoPotHandler`
- `PetExcludeItemsHandler`
- `pets`、`petignores`

可轉成 Unity API：

- 寵物召喚。
- 寵物加成。
- 寵物自動拾取。
- 寵物餵食與親密度。

放置型用途：

- 增加離線收益、掉寶率、自動分解、自動販售。

### 7. 倉庫、包裹、交易與玩家商店

來源：

- `StorageHandler`
- `DueyHandler`
- `FredrickHandler`
- `PlayerInteractionHandler`
- `MTSHandler`
- `storages`、`dueyitems`、`dueypackages`、`fredstorage`、`mts_items`、`mts_cart`

可轉成 Unity API：

- 帳號倉庫。
- 郵件附件。
- 玩家市場。
- 拍賣或交易所。

建議：

- Unity 第一版先做「倉庫」與「系統郵件」。
- 玩家交易與市場牽涉經濟風控，放到第二階段後。

### 8. 社群、公會、好友、家族與聊天

來源：

- `BuddylistModifyHandler`
- `GuildOperationHandler`
- `AllianceOperationHandler`
- `PartyOperationHandler`
- `MessengerHandler`
- `MultiChatHandler`
- `WhisperHandler`
- `FamilyAddHandler`
- `FamilyUseHandler`
- `guilds`、`alliance`、`buddies`、`family_character`

可轉成 Unity API：

- 好友列表。
- 公會。
- 聊天。
- 隊伍或遠征。

建議：

- 放置型第一版可先不做即時聊天。
- 可以先做公會簽到、公會 Boss、公會排行榜。

### 9. 活動、副本、遠征與 Boss

來源：

- `server.events`
- `server.partyquest`
- `server.expeditions`
- `scripts/event`
- `scripts-zh-CN/event`
- `bosslog_daily`、`bosslog_weekly`、`eventstats`

可轉成 Unity API：

- 每日 Boss 次數。
- 副本入場與結算。
- 活動關卡。
- 排行榜與傷害統計。

放置型建議：

- 第一版可以只做「Boss 挑戰券 + 秒殺/掃蕩 + 掉落結算」。

### 10. 外觀、美容、婚姻與社交裝飾

來源：

- `FaceExpressionHandler`
- `RingActionHandler`
- `WeddingHandler`
- `marriages`、`rings`
- `Character.wz` 相關資料

可轉成 Unity API：

- 外觀裝備。
- 髮型、臉型。
- 戒指、婚姻。

建議：

- Unity 放置型若重視收藏，可第二階段做外觀圖鑑。

### 11. 製作、卷軸、強化與道具使用

來源：

- `MakerSkillHandler`
- `ScrollHandler`
- `UseItemHandler`
- `UseCashItemHandler`
- `UseChairHandler`
- `UseSummonBagHandler`
- `UseOwlOfMinervaHandler`
- `makercreatedata`、`makerrecipedata`、`makerrewarddata`、`makerreagentdata`

可轉成 Unity API：

- 裝備強化。
- 卷軸成功率。
- 製作配方。
- 消耗道具使用。

放置型建議：

- 第一版可以做簡化版：裝備強化等級、升星、分解、製作。

## 可拆分的大系統建議

### A. 帳號與玩家身份系統

包含：

- 註冊、登入、token、帳號狀態。
- 角色列表、角色建立、角色選擇。
- 玩家語系、生日、性別、點數資料。

Unity 第一版必要性：最高。

### B. 角色養成系統

包含：

- 等級、經驗、HP/MP、職業、能力值、技能。
- 裝備穿脫、戰力計算、倍率。
- 離線收益結算。

Unity 第一版必要性：最高。

### C. 背包、裝備與獎勵系統

包含：

- 背包查詢。
- 裝備屬性。
- 道具新增、刪除、堆疊。
- 領獎、郵件附件、新手禮包。

Unity 第一版必要性：最高。

### D. 放置關卡與掉落系統

包含：

- 地圖作為掛機關卡。
- 怪物掉落池。
- 全域掉落池。
- 離線時間、擊殺速度、經驗、楓幣、道具掉落。

Unity 第一版必要性：最高。

### E. 商店、轉蛋與經濟系統

包含：

- NPC 商店。
- 現金商店。
- 轉蛋池。
- NX / Maple Point / Reward Point / 楓幣。
- 商品上架與玩家購買。

Unity 第一版必要性：高。

### F. 任務、成就與每日系統

包含：

- 主線任務。
- 每日任務。
- 成就。
- Boss 次數。
- 活動任務。

Unity 第一版必要性：中高。

### G. 後台營運系統

包含：

- 伺服器狀態。
- 遊戲設定。
- 掉落表。
- 商店。
- 轉蛋。
- GM 發獎。
- 指令與熱重載。
- 自動封鎖。

Unity 第一版必要性：高，但應與玩家端 API 分離。

### H. 社群與長線留存系統

包含：

- 好友。
- 公會。
- 排行榜。
- 郵件。
- 聊天。
- 公會 Boss。

Unity 第一版必要性：中。

### I. 內容資料與資產查詢系統

包含：

- WZ/XML 物品、怪物、地圖、NPC、技能資料。
- `informationSearch`。
- 多語系資源。

Unity 第一版必要性：中高。

## Unity 放置型版本的建議起步

我建議從「Unity 專用玩家 API」開始，而不是從 Unity 直接接原楓之谷 socket。

原因：

- 現有 socket protocol 是給原版 MapleStory client 使用，Unity 直接接會被大量封包、加密、狀態同步與即時地圖邏輯綁住。
- 放置型遊戲需要的是伺服器權威結算，不需要完整即時位移與怪物 AI。
- 現有 REST 管理 API 已經證明 Spring Boot 層能操作帳號、背包、掉落、商店、轉蛋與設定，最適合在這層新增 Unity BFF。

### 建議第一階段：最小可玩核心

目標：Unity 能登入、看到角色、開始掛機、領取收益、穿裝備。

應做 API：

- `POST /game/v1/session/login`
- `GET /game/v1/player/me`
- `GET /game/v1/characters`
- `POST /game/v1/characters`
- `GET /game/v1/idle/stage`
- `POST /game/v1/idle/start`
- `POST /game/v1/idle/claim`
- `GET /game/v1/inventory`
- `POST /game/v1/equipment/equip`
- `POST /game/v1/equipment/unequip`

需要重用後端：

- `accounts`
- `characters`
- `inventoryitems`
- `inventoryequipment`
- `drop_data`
- `drop_data_global`
- `game_config`
- `GiveService` 或背包新增邏輯
- `ItemInformationProvider`
- `MonsterInformationProvider`

### 建議第二階段：經濟與抽獎

目標：有消耗、有商店、有抽裝備，遊戲開始有循環。

應做 API：

- `GET /game/v1/shop/list`
- `POST /game/v1/shop/buy`
- `GET /game/v1/gachapon/pools`
- `POST /game/v1/gachapon/draw`
- `GET /game/v1/mail`
- `POST /game/v1/mail/claim`

需要重用後端：

- `shopitems`
- `shops`
- `gachapon_reward_pool`
- `gachapon_reward`
- `modified_cash_item`
- `nxcode` / 帳號點數欄位
- `notes` 或新增 mail table

### 建議第三階段：任務、Boss 與活動

目標：增加每日目標與長線留存。

應做 API：

- `GET /game/v1/quests`
- `POST /game/v1/quests/claim`
- `GET /game/v1/bosses`
- `POST /game/v1/bosses/challenge`
- `GET /game/v1/events`
- `POST /game/v1/events/claim`

需要重用後端：

- `queststatus`
- `questprogress`
- `bosslog_daily`
- `bosslog_weekly`
- `eventstats`
- `scripts/event` 的資料或重新資料化後的活動表。

### 建議第四階段：社群與排行榜

目標：讓放置遊戲有競爭與留存。

應做 API：

- `GET /game/v1/rankings/power`
- `GET /game/v1/rankings/stage`
- `GET /game/v1/guild`
- `POST /game/v1/guild/checkin`
- `POST /game/v1/guild/boss`

需要重用後端：

- `guilds`
- `buddies`
- `family_character`
- `characterexplogs`
- 傷害排名設定。

## 具體開發順序建議

1. 先在後端新增 Unity 專用 namespace，例如 `org.gms.controller.game`、`org.gms.service.game`，不要把玩家 API 混進現有 GM 管理 controller。
2. 先做登入後的「我的角色列表」與「角色摘要」API，確認 Unity 可以登入並取得玩家資料。
3. 做 `idle_claim` 伺服器端結算：用角色目前關卡、戰力、離線秒數、掉落表、倍率設定算出經驗、楓幣與道具。
4. 把領到的道具寫進既有 `inventoryitems` / `inventoryequipment`，並提供 Unity 背包 API。
5. 做裝備穿脫與戰力計算，讓玩家有第一個養成循環。
6. 再接商店與轉蛋，讓資源有消耗出口。
7. 最後再接任務、Boss、公會、排行榜。

## 不建議第一版做的事

- 不建議 Unity 直接重作原版 MapleStory socket client。
- 不建議一開始搬完整 NPC JS 腳本流程。
- 不建議一開始做即時多人地圖同步。
- 不建議讓玩家端直接呼叫 `/give`、`/file`、`/server/*/shutdown`、`/command` 這類高權限 API。
- 不建議把現有管理 API 當玩家 API 原樣開放，因為欄位與權限邊界不夠乾淨。

## 最推薦的開始點

從「角色 + 放置收益 + 背包」開始。

這三個系統剛好能形成最小遊戲循環：

1. 玩家登入並選角色。
2. 選擇掛機地圖或關卡。
3. 伺服器依時間產出經驗、楓幣、道具。
4. 玩家領取收益。
5. 玩家穿裝備提升戰力。
6. 更高戰力解鎖更高關卡與更好掉落。

這條路能最大化重用現有後端，也最符合 Unity 放置型版本的需求。
