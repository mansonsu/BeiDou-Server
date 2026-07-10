package org.gms.service.auth;

import org.gms.dao.entity.AccountIdentityDO;
import org.gms.dao.mapper.AccountIdentityMapper;
import org.gms.dao.mapper.AccountsMapper;
import org.gms.dao.entity.AccountsDO;
import org.gms.client.DefaultDates;
import org.gms.service.AccountService;
import org.gms.model.auth.ExternalAuthRequest;
import org.gms.model.auth.VerifiedIdentity;
import org.gms.provider.auth.ExternalAuthProvider;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Timestamp;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalAuthenticationService {
    private final Map<String, ExternalAuthProvider> providers;
    private final AccountIdentityMapper identities;
    private final GameLoginTokenService tokens;
    private final AccountsMapper accounts;
    private final AccountService accountService;

    public ExternalAuthenticationService(List<ExternalAuthProvider> providers, AccountIdentityMapper identities, GameLoginTokenService tokens,
                                         AccountsMapper accounts, AccountService accountService) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(ExternalAuthProvider::name, Function.identity()));
        this.identities = identities; this.tokens = tokens; this.accounts = accounts; this.accountService = accountService;
    }

    @Transactional
    public String login(ExternalAuthRequest request) {
        ExternalAuthProvider provider = providers.get(request.provider().toLowerCase());
        if (provider == null) throw new IllegalArgumentException("Unsupported identity provider");
        VerifiedIdentity verified = provider.verify(request);
        AccountIdentityDO identity = identities.find(verified.provider(), verified.subject());
        if (identity == null) identity = createAccount(verified);
        return tokens.issue(identity.getAccountId());
    }

    private AccountIdentityDO createAccount(VerifiedIdentity verified) {
        try {
            String suffix = Long.toUnsignedString(hash64(verified.provider() + ":" + verified.subject()), 36);
            String name = ("u" + suffix);
            if (name.length() > 13) name = name.substring(0, 13);
            int attempt = 0;
            while (accounts.selectOneByName(name) != null) {
                String tail = Integer.toString(++attempt, 36);
                name = name.substring(0, Math.min(13 - tail.length(), name.length())) + tail;
            }
            byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
            AccountsDO account = AccountsDO.builder().name(name)
                    .password(accountService.encryptPassword(java.util.Base64.getEncoder().encodeToString(random)))
                    .birthday(Date.valueOf(DefaultDates.getBirthday())).tempban(Timestamp.valueOf(DefaultDates.getTempban()))
                    .lastlogin(Timestamp.valueOf(DefaultDates.getTempban())).language(3).email(verified.email()).build();
            accounts.insertSelective(account);
            AccountIdentityDO identity = new AccountIdentityDO();
            identity.setAccountId(account.getId()); identity.setProvider(verified.provider()); identity.setProviderSubject(verified.subject());
            identity.setEmail(verified.email()); identity.setEmailVerified(verified.emailVerified()); identity.setDisplayName(verified.displayName());
            identities.insert(identity);
            return identity;
        } catch (Exception e) { throw new IllegalStateException("Could not create external account", e); }
    }

    private static long hash64(String value) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) { hash ^= b & 0xff; hash *= 0x100000001b3L; }
        return hash;
    }
}
