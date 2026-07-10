package org.gms.model.auth;

public record VerifiedIdentity(String provider, String subject, String email,
                               boolean emailVerified, String displayName) {}
