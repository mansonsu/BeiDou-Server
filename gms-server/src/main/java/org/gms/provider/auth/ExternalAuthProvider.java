package org.gms.provider.auth;

import org.gms.model.auth.ExternalAuthRequest;
import org.gms.model.auth.VerifiedIdentity;

public interface ExternalAuthProvider {
    String name();
    VerifiedIdentity verify(ExternalAuthRequest request);
}
