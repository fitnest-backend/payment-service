package az.fitnest.payment.service;

import az.fitnest.payment.client.abb.bnpl.AbbBnplProperties;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbbBnplJwksServiceTest {

    @Test
    void publishesRsaJwkWithKidWhenPrivateKeyConfigured() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        AbbBnplProperties properties = new AbbBnplProperties();
        properties.setPrivateKey(pem);
        properties.setClientId("fitnest-m2m");
        properties.setKeyId("fitnest-m2m");

        Map<String, Object> jwks = new AbbBnplJwksService(properties).jwks();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertEquals(1, keys.size());
        assertEquals("RSA", keys.get(0).get("kty"));
        assertEquals("sig", keys.get(0).get("use"));
        assertEquals("RS256", keys.get(0).get("alg"));
        assertEquals("fitnest-m2m", keys.get(0).get("kid"));
        assertTrue(keys.get(0).get("n").toString().length() > 20);
        assertFalse(keys.get(0).containsKey("d"));
    }

    @Test
    void emptyPrivateKeyPublishesEmptySetSoUrlStillWorks() {
        AbbBnplProperties properties = new AbbBnplProperties();
        Map<String, Object> jwks = new AbbBnplJwksService(properties).jwks();
        @SuppressWarnings("unchecked")
        List<?> keys = (List<?>) jwks.get("keys");
        assertTrue(keys.isEmpty());
    }
}
