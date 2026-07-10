package org.gms.controller;

import org.gms.model.auth.ExternalAuthRequest;
import org.gms.service.auth.ExternalAuthenticationService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class ExternalAuthController {
    private final ExternalAuthenticationService auth;
    public ExternalAuthController(ExternalAuthenticationService auth) { this.auth = auth; }

    @PostMapping("/external")
    public Map<String, Object> login(@RequestBody ExternalAuthRequest request) {
        return Map.of("gameToken", auth.login(request), "expiresIn", 60);
    }
}
