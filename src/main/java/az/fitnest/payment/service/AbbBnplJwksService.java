package az.fitnest.payment.service;

import az.fitnest.payment.client.abb.bnpl.AbbBnplProperties;
import az.fitnest.payment.client.abb.bnpl.AbbBnplRsaKeys;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

/**
 * Public JWKS for ABB Private Key JWT ({@code fitnest-m2m}). ABB fetches this URL
 * to verify our {@code client_assertion}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbbBnplJwksService {

    private final AbbBnplProperties properties;

    public Map<String, Object> jwks() {
        String privatePem = properties.getPrivateKey();
        if (privatePem == null || privatePem.isBlank()) {
            log.warn("[BNPL][JWKS] ABB_BNPL_PRIVATE_KEY is empty; publishing empty key set");
            return new JWKSet(List.of()).toJSONObject();
        }
        try {
            RSAPrivateKey privateKey = AbbBnplRsaKeys.loadPrivateKey(privatePem);
            RSAPublicKey publicKey = AbbBnplRsaKeys.publicKeyFromPrivate(
                    privateKey, properties.getPublicKey());
            RSAKey rsa = new RSAKey.Builder(publicKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(resolveKeyId())
                    .build();
            return new JWKSet(rsa).toJSONObject();
        } catch (Exception e) {
            log.error("[BNPL][JWKS] Failed to publish signing key", e);
            return new JWKSet(List.of()).toJSONObject();
        }
    }

    public String resolveKeyId() {
        if (properties.getKeyId() != null && !properties.getKeyId().isBlank()) {
            return properties.getKeyId().trim();
        }
        if (properties.getClientId() != null && !properties.getClientId().isBlank()) {
            return properties.getClientId().trim();
        }
        return "fitnest-m2m";
    }
}
