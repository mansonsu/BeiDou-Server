package org.gms.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {
    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (RequireUtil.isEmpty(remoteAddr)) {
            remoteAddr = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        }
        if (RequireUtil.isEmpty(remoteAddr)) {
            remoteAddr = request.getHeader("X-Real-IP");
        }
        return remoteAddr == null ? "" : remoteAddr.trim();
    }

    private static String firstForwardedIp(String forwardedIp) {
        if (RequireUtil.isEmpty(forwardedIp)) {
            return forwardedIp;
        }
        int commaIndex = forwardedIp.indexOf(',');
        if (commaIndex < 0) {
            return forwardedIp.trim();
        }
        return forwardedIp.substring(0, commaIndex).trim();
    }
}
