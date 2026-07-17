package com.sunsetbeach.security;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.sunsetbeach.model.Role;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Verifies the same next-auth.session-token cookie the Next.js app issues.
 *
 * next-auth v4's default {@code encode}/{@code decode} (node_modules/next-auth/jwt/index.js)
 * derive a 32-byte key via HKDF-SHA256(ikm = NEXTAUTH_SECRET, salt = "", info =
 * "NextAuth.js Generated Encryption Key", 32) and encrypt the claims as a JWE with
 * header {alg: "dir", enc: "A256GCM"} - no nested JWS. Nothing in next-auth's core calls
 * encode/decode with an explicit salt, so it is always "".
 */
@Service
public class NextAuthTokenService {

    private static final String HKDF_INFO = "NextAuth.js Generated Encryption Key";
    private static final long CLOCK_TOLERANCE_SECONDS = 15;

    private final byte[] derivedKey;

    public NextAuthTokenService(@Value("${app.security.nextauth-secret}") String nextAuthSecret) {
        this.derivedKey = hkdfSha256(
                nextAuthSecret.getBytes(StandardCharsets.UTF_8),
                new byte[32], // empty salt, HMAC zero-pads a short/empty key to block size anyway
                HKDF_INFO.getBytes(StandardCharsets.UTF_8),
                32);
    }

    public Optional<StaffPrincipal> decode(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            JWEObject jwe = JWEObject.parse(token);
            JWEHeader header = jwe.getHeader();
            if (header.getAlgorithm() != JWEAlgorithm.DIR || header.getEncryptionMethod() != EncryptionMethod.A256GCM) {
                return Optional.empty();
            }
            jwe.decrypt(new DirectDecrypter(derivedKey));

            Map<String, Object> claims = jwe.getPayload().toJSONObject();

            Object exp = claims.get("exp");
            if (exp instanceof Number expNumber) {
                Instant expiry = Instant.ofEpochSecond(expNumber.longValue());
                if (Instant.now().isAfter(expiry.plusSeconds(CLOCK_TOLERANCE_SECONDS))) {
                    return Optional.empty();
                }
            }

            String id = asString(claims.get("id"));
            if (id == null) {
                id = asString(claims.get("sub"));
            }
            String email = asString(claims.get("email"));
            String roleValue = asString(claims.get("role"));
            if (id == null || roleValue == null) {
                return Optional.empty();
            }

            Role role;
            try {
                role = Role.fromValue(roleValue);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }

            return Optional.of(new StaffPrincipal(id, email, role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /** RFC 5869 HKDF-SHA256 (extract-then-expand). */
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
