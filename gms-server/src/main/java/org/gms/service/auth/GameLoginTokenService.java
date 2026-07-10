package org.gms.service.auth;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameLoginTokenService {
    private record Entry(int accountId, Instant expiresAt) {}
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> tokens = new ConcurrentHashMap<>();

    public String issue(int accountId) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new Entry(accountId, Instant.now().plusSeconds(60)));
        return token;
    }

    public Integer consume(String token) {
        Entry entry = tokens.remove(token);
        return entry == null || Instant.now().isAfter(entry.expiresAt()) ? null : entry.accountId();
    }
}
