package com.sunsetbeach.security;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.sunsetbeach.model.Role;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.minidev.json.JSONObject;

/**
 * Mints {@code next-auth.session-token} cookie values for tests. Mirrors the encode side of
 * {@link NextAuthTokenService#decode}, which only implements decode (that's all the app needs
 * at runtime - encoding is the Next.js app's job).
 */
public final class TestSessionTokens {

    private static final String HKDF_INFO = "NextAuth.js Generated Encryption Key";

    private TestSessionTokens() {
    }

    public static String encode(String nextAuthSecret, String id, String email, Role role) {
        try {
            byte[] key = hkdfSha256(
                    nextAuthSecret.getBytes(StandardCharsets.UTF_8),
                    new byte[32],
                    HKDF_INFO.getBytes(StandardCharsets.UTF_8),
                    32);

            JSONObject claims = new JSONObject();
            claims.put("id", id);
            claims.put("email", email);
            claims.put("role", role.getValue());
            claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());

            JWEObject jwe = new JWEObject(new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM), new Payload(claims));
            jwe.encrypt(new DirectEncrypter(key));
            return jwe.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) {
        byte[] prk = hmacSha256(salt, ikm);
        byte[] okm = new byte[length];
        byte[] previousBlock = new byte[0];
        int bytesGenerated = 0;
        byte counter = 1;
        while (bytesGenerated < length) {
            byte[] input = new byte[previousBlock.length + info.length + 1];
            System.arraycopy(previousBlock, 0, input, 0, previousBlock.length);
            System.arraycopy(info, 0, input, previousBlock.length, info.length);
            input[input.length - 1] = counter;
            previousBlock = hmacSha256(prk, input);
            int toCopy = Math.min(previousBlock.length, length - bytesGenerated);
            System.arraycopy(previousBlock, 0, okm, bytesGenerated, toCopy);
            bytesGenerated += toCopy;
            counter++;
        }
        return okm;
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
