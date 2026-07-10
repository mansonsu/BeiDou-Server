package org.gms.dao.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

@Data
@Table("account_identities")
public class AccountIdentityDO {
    @Id(keyType = KeyType.Auto) private Long id;
    private Integer accountId;
    private String provider;
    private String providerSubject;
    private String email;
    private Boolean emailVerified;
    private String displayName;
}
