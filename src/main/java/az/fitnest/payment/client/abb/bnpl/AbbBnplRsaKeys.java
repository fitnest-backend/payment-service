package az.fitnest.payment.client.abb.bnpl;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the ABB M2M RSA keypair used for Private Key JWT and the public JWKS document.
 */
public final class AbbBnplRsaKeys {

    private AbbBnplRsaKeys() {
    }

    public static RSAPrivateKey loadPrivateKey(String pemOrBase64) throws Exception {
        byte[] keyBytes = decodePem(pemOrBase64);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        try {
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception pkcs8Fail) {
            byte[] pkcs8 = wrapPkcs1ToPkcs8(keyBytes);
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        }
    }

    static RSAPublicKey loadPublicKey(String pemOrBase64) throws Exception {
        byte[] keyBytes = decodePem(pemOrBase64);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(keyBytes));
        return (RSAPublicKey) publicKey;
    }

    public static RSAPublicKey publicKeyFromPrivate(RSAPrivateKey privateKey, String publicPemOrBase64) throws Exception {
        if (privateKey instanceof RSAPrivateCrtKey crt) {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        }
        if (publicPemOrBase64 != null && !publicPemOrBase64.isBlank()) {
            return loadPublicKey(publicPemOrBase64);
        }
        throw new IllegalStateException("ABB BNPL private key has no CRT params and no public key is configured");
    }

    private static byte[] decodePem(String pemOrBase64) {
        String stripped = pemOrBase64
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(stripped);
    }

    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        byte[] algId = new byte[]{
                0x30, 0x0d,
                0x06, 0x09,
                0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] octetString = encodeAsn1Length(0x04, pkcs1Bytes);
        byte[] inner = new byte[3 + algId.length + octetString.length];
        inner[0] = 0x02;
        inner[1] = 0x01;
        inner[2] = 0x00;
        System.arraycopy(algId, 0, inner, 3, algId.length);
        System.arraycopy(octetString, 0, inner, 3 + algId.length, octetString.length);
        return encodeAsn1Length(0x30, inner);
    }

    private static byte[] encodeAsn1Length(int tag, byte[] content) {
        int length = content.length;
        byte[] header;
        if (length < 128) {
            header = new byte[]{(byte) tag, (byte) length};
        } else if (length < 256) {
            header = new byte[]{(byte) tag, (byte) 0x81, (byte) length};
        } else {
            header = new byte[]{(byte) tag, (byte) 0x82, (byte) (length >> 8), (byte) (length & 0xff)};
        }
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }
}
