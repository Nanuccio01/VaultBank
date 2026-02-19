package com.example.backend.api.banking;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/banking")
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Map.of(
                "email", jwt.getSubject(),
                "uid", jwt.getClaimAsString("uid"),
                "scope", jwt.getClaimAsString("scope")
        );
    }
}
