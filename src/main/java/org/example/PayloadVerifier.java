package org.example;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PayloadVerifier {
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
