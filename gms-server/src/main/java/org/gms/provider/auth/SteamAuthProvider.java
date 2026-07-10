package org.gms.provider.auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.gms.model.auth.ExternalAuthRequest;
import org.gms.model.auth.VerifiedIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SteamAuthProvider implements ExternalAuthProvider {
    private final RestClient http = RestClient.create();
    @Value("${auth.steam.publisher-key:}") private String publisherKey;
    @Value("${auth.steam.app-id:0}") private long appId;
    @Value("${auth.steam.identity:beidou-login}") private String expectedIdentity;

    @Override public String name() { return "steam"; }

    @Override public VerifiedIdentity verify(ExternalAuthRequest request) {
        if (publisherKey.isBlank() || appId <= 0) throw new IllegalStateException("Steam authentication is not configured");
        if (!expectedIdentity.equals(request.identity())) throw new IllegalArgumentException("Invalid Steam ticket identity");
        String body = http.get().uri(uri -> uri
                .scheme("https").host("partner.steam-api.com")
                .path("/ISteamUserAuth/AuthenticateUserTicket/v1/")
                .queryParam("key", publisherKey).queryParam("appid", appId)
                .queryParam("ticket", request.credential()).queryParam("identity", expectedIdentity).build())
                .retrieve().body(String.class);
        JSONObject params = JSON.parseObject(body).getJSONObject("response").getJSONObject("params");
        if (params == null || !"OK".equals(params.getString("result"))) throw new IllegalArgumentException("Steam rejected authentication ticket");
        String steamId = params.getString("steamid");
        if (steamId == null || steamId.isBlank()) throw new IllegalArgumentException("Steam response has no SteamID");
        return new VerifiedIdentity(name(), steamId, null, false, null);
    }
}
