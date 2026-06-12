# IP 註冊限制設計備忘

## 目標

未來希望限制：

```text
一個 IP 只能註冊一個遊戲帳號
```

這個限制主要用來降低大量註冊、多開帳號、濫用交易或活動獎勵的風險。

## 目前專案現況

目前 Server 已經有部分 IP 相關設計，但沒有「一個 IP 只能註冊一個帳號」的限制。

已存在的設計：

- `accounts` 表有 `ip` 欄位。
- `ipbans` 表可封鎖 IP。
- `ServerFilter` 會檢查 HTTP request 的 IP ban 與 rate limit。
- 遊戲連線端也有 IP ban 檢查。
- `AccountService.addAccount` 目前只檢查帳號名稱是否重複，沒有檢查註冊 IP。

目前缺少的設計：

- 註冊時沒有取得 request IP 並寫入 `accounts.ip`。
- 註冊時沒有查詢該 IP 是否已經註冊過帳號。
- DB 沒有 unique constraint 保護 IP 註冊唯一性。
- 沒有白名單或例外機制處理同住家人、網咖、公司網路等共用 IP 場景。

## 不建議只靠 accounts.ip

`accounts.ip` 目前是 `TEXT` 欄位，不適合直接加 unique index。

原因：

- `TEXT` 欄位加唯一索引較麻煩，且不同 DB 設定可能需要指定索引長度。
- IP 可能需要正規化，例如 IPv4、IPv6、proxy header。
- 未來可能需要保留註冊 IP、最後登入 IP、歷史 IP，單一欄位彈性不足。

建議新增獨立表來記錄註冊 IP。

## 建議資料表

```sql
CREATE TABLE account_registration_ips
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id INT NOT NULL,
    ip VARCHAR(45) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_registration_ips_account_id (account_id),
    UNIQUE KEY uk_account_registration_ips_ip (ip)
);
```

說明：

- `VARCHAR(45)` 可容納 IPv6。
- `UNIQUE (ip)` 保證一個 IP 只能註冊一次。
- `UNIQUE (account_id)` 保證一個帳號只有一筆註冊 IP。
- 註冊流程仍可同步寫入 `accounts.ip`，但正式限制以此表為準。

如果未來想允許特定 IP 多帳號，可再增加例外表：

```sql
CREATE TABLE registration_ip_allowlist
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    ip VARCHAR(45) NOT NULL,
    max_accounts INT NOT NULL DEFAULT 1,
    reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_registration_ip_allowlist_ip (ip)
);
```

## 註冊流程建議

```text
1. Client 呼叫註冊 API。
2. Server 從 request 取得真實 IP。
3. Server 正規化 IP。
4. Server 檢查 registration_ip_allowlist。
5. 如果不在白名單，檢查 account_registration_ips 是否已有相同 IP。
6. 若該 IP 已註冊過帳號，拒絕註冊。
7. 建立 accounts 資料。
8. 寫入 account_registration_ips。
9. transaction commit。
```

重要原則：

- 建立帳號與寫入註冊 IP 必須在同一個 transaction。
- 不能只靠程式邏輯檢查，DB unique constraint 必須作為最後防線。
- 併發註冊時，如果兩個 request 同時使用同一 IP，最終必須只能有一個成功。

## IP 取得方式

目前 `ServerFilter` 會讀：

```text
X-Forwarded-For
X-Real-IP
request.getRemoteAddr()
```

正式實作時，建議建立共用工具：

```java
ClientIpResolver.resolve(HttpServletRequest request)
```

並統一給：

- `ServerFilter`
- 註冊 API
- 登入紀錄
- 風控紀錄

注意：如果 Server 前面有 nginx、Cloudflare、Steam relay 或其他 proxy，不能無條件信任所有 `X-Forwarded-For`。

建議規則：

```text
只有 request.getRemoteAddr() 是可信任 proxy IP 時，才讀 X-Forwarded-For / X-Real-IP。
否則使用 request.getRemoteAddr()。
```

## 和 Steam 帳號唯一性的關係

如果未來 Steam 版採用：

```text
一個 SteamID64 只能一個遊戲帳號
```

那 IP 限制仍然可以作為第二層風控。

建議優先順序：

```text
1. SteamID64 unique constraint
2. 註冊 IP unique constraint
3. DLC 或交易權限限制
4. 風控紀錄與客服解鎖
```

SteamID64 是比較可靠的身份限制。IP 限制比較適合作為反濫用手段，但不能完全代表玩家身份。

## 可能的誤傷場景

一個 IP 只能註冊一個帳號會有誤傷風險：

- 同住家人共用同一個家用 IP。
- 學校、公司、宿舍使用同一出口 IP。
- 網咖或公共網路。
- 手機網路 CGNAT，很多人共用同一個外部 IP。
- IPv6 prefix 變化或 ISP 動態 IP。

因此建議保留 GM 後台或 DB 白名單機制，避免客服無法處理。

## 建議錯誤訊息

不要在錯誤訊息中提供太多風控細節。

建議：

```text
此網路位置已建立過帳號，如需協助請聯絡客服。
```

不建議：

```text
你的 IP 1.2.3.4 已經註冊過帳號。
```

避免讓濫用者更容易測試規則。

## 建議實作順序

1. 新增 `ClientIpResolver`，統一解析 request IP。
2. 新增 `account_registration_ips` migration。
3. 新增 mapper / entity / service。
4. 修改 `AccountController.register`，把 `HttpServletRequest` 傳入 service。
5. 修改 `AccountService.addAccount`，在 transaction 中建立帳號與註冊 IP。
6. 加入 DB unique constraint 錯誤處理，回傳可讀的註冊失敗訊息。
7. 視需求新增 `registration_ip_allowlist`。
8. 補註冊流程測試。

## 建議結論

可以做「一個 IP 只能註冊一個帳號」，但不要直接只改 `accounts.ip`。

建議用獨立表加 unique constraint：

```text
account_registration_ips.ip UNIQUE
```

Server 註冊流程負責檢查與寫入，DB 負責防止併發繞過。之後若要支援例外，就加白名單表或 GM 後台設定。
