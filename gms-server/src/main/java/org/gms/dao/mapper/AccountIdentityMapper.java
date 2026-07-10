package org.gms.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.gms.dao.entity.AccountIdentityDO;

@Mapper
public interface AccountIdentityMapper extends BaseMapper<AccountIdentityDO> {
    @Select("SELECT id, account_id AS accountId, provider, provider_subject AS providerSubject, email, email_verified AS emailVerified, display_name AS displayName FROM account_identities WHERE provider=#{provider} AND provider_subject=#{subject} LIMIT 1")
    AccountIdentityDO find(String provider, String subject);
}
