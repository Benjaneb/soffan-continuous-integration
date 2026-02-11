package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class PayloadVerifierTest {
    @Test
    @DisplayName("Tests correct SHA256 signature")
    void testValidSignature() {
        String payload = "{\"test\": \"payload\"}";
        String secret = "it-is-a-secret";
        // Pre-calculated HMAC SHA256 for the above payload and secret
        String expectedSignature = "sha256=7c2d06f3b8e608994db8fa0e1cbf9c91311dc09b4105fa58039728faefdeae8c";
        
        assertTrue(PayloadVerifier.isValidPayload(payload, secret, expectedSignature));
    }

    @Test
    @DisplayName("Tests incorrect SHA256 signature")
    void testInvalidSignature() {
        String payload = "{\"test\": \"data\"}";
        String secret = "it-is-a-secret";
        String expectedSignature = "sha256=wronghash";
        
        assertFalse(PayloadVerifier.isValidPayload(payload, secret, expectedSignature));
    }
}
