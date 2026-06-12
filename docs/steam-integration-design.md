# Steam 整合設計備忘

## 目標

未來遊戲上架 Steam 後，玩家身份與部分遊戲功能需要由 Steam 授權結果決定。

目前規劃重點：

- 一個 Steam 帳號只能對應一個遊戲帳號。
- 玩家購買指定 DLC 後，才可以使用遊戲內玩家交易系統。
- DLC 權限判斷必須由 Server 負責，不能信任 Unity Client。
- 交易封包必須在 Server 端檢查權限，避免玩家修改 Client 或封包繞過限制。

## SteamID64 與遊戲帳號

SteamID64 是 Steam 帳號的唯一識別值，可以用來確認玩家身份。

建議不要直接把 SteamID64 當成遊戲自己的 account_id，而是保留遊戲內部帳號 ID，再用額外資料表綁定 SteamID64。

建議結構：

```sql
accounts
- id
- account_name
- password_hash
- created_at

account_steam_links
- account_id BIGINT NOT NULL
- steam_id64 BIGINT NOT NULL
- linked_at DATETIME NOT NULL
- last_login_at DATETIME NULL

UNIQUE (account_id)
UNIQUE (steam_id64)
```

這樣可以保證：

- 一個遊戲帳號只能綁定一個 Steam 帳號。
- 一個 Steam 帳號只能註冊或登入一個遊戲帳號。
- 之後若支援其他登入平台，資料結構仍然可以擴充。

## Steam 登入流程

建議流程：

```text
1. Unity Client 從 Steamworks SDK 取得 Steam auth ticket。
2. Unity Client 將 ticket 傳給 Java Server。
3. Java Server 使用 Steam Web API 驗證 ticket。
4. 驗證成功後取得 SteamID64。
5. Server 查詢 account_steam_links。
6. 如果 SteamID64 已綁定帳號，直接登入該 account_id。
7. 如果 SteamID64 尚未綁定帳號，建立新遊戲帳號後綁定 SteamID64。
```

重要原則：

- Steam Publisher Web API Key 只能放在 Server。
- Unity Client 不應持有 Web API Key。
- Server 必須驗證 ticket 後才接受 SteamID64。
- 不應讓 Client 自行傳入 SteamID64 後直接登入。

## 一個 Steam 帳號只能一個遊戲帳號

這個限制不能只靠程式邏輯判斷，必須由 DB unique constraint 保護。

原因是如果玩家同時開多個 Client 註冊，單純的「先查詢再新增」可能遇到併發問題。

建議由 DB 保證：

```sql
UNIQUE (steam_id64)
UNIQUE (account_id)
```

Server 流程仍然要先檢查，但最終以 DB constraint 作為最後防線。

## DLC 交易憑證

若希望玩家購買指定 DLC 後才可使用交易系統，可以把 DLC 視為一種帳號權限。

例如：

```text
TRADE_LICENSE
```

建議資料表：

```sql
account_entitlements
- account_id BIGINT NOT NULL
- entitlement_key VARCHAR(64) NOT NULL
- source VARCHAR(32) NOT NULL
- source_app_id BIGINT NULL
- enabled BOOLEAN NOT NULL
- last_checked_at DATETIME NULL
- created_at DATETIME NOT NULL
- updated_at DATETIME NOT NULL

UNIQUE (account_id, entitlement_key)
```

範例資料：

```text
account_id: 1001
entitlement_key: TRADE_LICENSE
source: STEAM_DLC
source_app_id: 交易憑證 DLC 的 AppID
enabled: true
```

## DLC 權限檢查流程

建議流程：

```text
1. 玩家登入時，Server 已透過 Steam ticket 驗證 SteamID64。
2. Server 使用 Steam Publisher Web API Key 查詢該 SteamID64 是否擁有指定 DLC AppID。
3. 如果玩家擁有 DLC，寫入或更新 account_entitlements.TRADE_LICENSE = enabled。
4. 如果玩家沒有 DLC，寫入或更新 account_entitlements.TRADE_LICENSE = disabled。
5. 玩家使用交易系統時，Server 只根據 account_entitlements 判斷是否允許。
```

注意：

- Unity Client 可以用 Steamworks SDK 顯示 DLC 是否安裝或購買，但只能作為 UI 提示。
- 正式權限判斷必須由 Server 呼叫 Steam Web API。
- 不要讓 Client 傳「我有 DLC」這種結果給 Server 後直接相信。

## 交易系統封包限制

所有交易相關封包都必須檢查 `TRADE_LICENSE`。

需要檢查的地方包含：

- 發起交易
- 接受交易
- 放入物品
- 取消交易
- 確認交易
- 市集上架或購買，如果之後有做市集

Server 端概念：

```java
if (!accountEntitlementService.has(accountId, "TRADE_LICENSE")) {
    reject("需要購買可交易憑證 DLC");
    return;
}
```

UI 可以灰掉交易按鈕，但 UI 不是安全檢查。真正的限制一定要放在 Server 封包處理層。

## Steam API 快取策略

不建議每次交易都呼叫 Steam API。

建議策略：

- 玩家登入時檢查一次 DLC。
- 將結果寫入 DB。
- `last_checked_at` 超過指定時間後才重新檢查，例如 6 小時或 24 小時。
- 可以提供「重新檢查 DLC」功能，讓剛購買 DLC 的玩家手動刷新。
- 如果 Steam API 暫時失敗，可以保留上次成功檢查結果，但要寫 log。

較嚴格的版本：

```text
交易前如果 TRADE_LICENSE 的 last_checked_at 超過 24 小時，Server 先重新查 Steam。
查詢成功後再決定是否允許交易。
查詢失敗時依營運策略決定暫時允許或暫時拒絕。
```

## 建議的最終架構

```text
Unity Steam Client
    ↓ 取得 Steam auth ticket
Java Server
    ↓ 驗證 ticket
Steam Web API
    ↓ 回傳 SteamID64
Java Server
    ↓ 查詢或建立遊戲帳號
Database
    ↓ 綁定 account_id 與 steam_id64
Java Server
    ↓ 查詢 DLC 權限
Steam Web API
    ↓ 回傳 DLC 擁有狀態
Database
    ↓ 寫入 account_entitlements
交易封包
    ↓ Server 檢查 TRADE_LICENSE
允許或拒絕交易
```

## 實作優先順序

建議之後依照這個順序實作：

1. 新增 Steam 帳號綁定資料表。
2. 新增 Server 端 Steam ticket 驗證服務。
3. 新增 Steam 登入或綁定流程。
4. 新增帳號權限資料表 `account_entitlements`。
5. 新增 DLC 權限同步服務。
6. 在交易封包入口加入 `TRADE_LICENSE` 檢查。
7. Unity Client 顯示交易權限狀態與購買提示。

## 安全原則

- Steam Web API Key 只能存在 Server。
- SteamID64 必須由 Server 驗證 ticket 後取得。
- DLC 權限必須由 Server 查詢 Steam API。
- 交易是否允許必須由 Server 決定。
- DB 必須有 unique constraint 防止 Steam 帳號重複綁定。
- Client 顯示狀態只能作為 UI 提示，不能作為安全依據。
