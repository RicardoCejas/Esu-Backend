package com.SaludUnificada.Esu.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RequestIpUtil {

    private final Set<String> trustedProxies;

    public RequestIpUtil(@Value("${app.security.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String trustedProxiesConfig) {
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            this.trustedProxies = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } else {
            this.trustedProxies = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
        }
    }

    public String obtenerIpCliente(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = "127.0.0.1";
        }

        boolean isTrusted = trustedProxies.contains(remoteAddr);
        if (isTrusted) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                String[] ips = forwarded.split(",");
                if (ips.length > 0 && !ips[0].trim().isEmpty()) {
                    return ips[0].trim();
                }
            }
        }

        return remoteAddr;
    }
}
