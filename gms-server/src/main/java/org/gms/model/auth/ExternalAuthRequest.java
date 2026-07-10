package org.gms.model.auth;

public record ExternalAuthRequest(String provider, String credential, String identity) {}
