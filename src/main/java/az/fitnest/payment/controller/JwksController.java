package az.fitnest.payment.controller;

import az.fitnest.payment.service.AbbBnplJwksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * RFC 7517 JWKS for ABB M2M ({@code client_assertion} RS256). Public, no auth.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final AbbBnplJwksService jwksService;

    @GetMapping(value = {"/.well-known/jwks.json", "/payment/.well-known/jwks.json"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(jwksService.jwks());
    }
}
