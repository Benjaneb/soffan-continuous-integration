package org.example;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for verifying GitHub webhook payload signatures.
 *
 * <p>If signature verification fails for any reason, the payload is treated
 * as invalid.</p>
 */
public class PayloadVerifier {

    /**
     * Verifies that a webhook payload matches the provided HMAC-SHA256 signature.
     *
     * @param payload the request body received from GitHub
     * @param secret the webhook secret configured in GitHub and on the server
     * @param signature the value of the {@code X-Hub-Signature-256} header
     * @return {@code true} if the computed signature matches the provided signature;
     *         {@code false} if the signature is {@code null}, does not match,
     *         or if any error occurs during verification
     */
    public static boolean isValidPayload(String payload, String secret, String signature) {
        if (signature == null) return false;
        try {
            // Initialize HMAC SHA256
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(key);

            // Compute hash and convert to Hex
            byte[] rawHash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("sha256=");
            for (byte b : rawHash) hex.append(String.format("%02x", b));

            return hex.toString().equals(signature);
        } catch (Exception e) {
            // If anything fails, treat as invalid
            return false;
        }
    }
}
