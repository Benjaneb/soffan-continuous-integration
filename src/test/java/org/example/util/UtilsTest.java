package org.example.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {
    @Test
    @DisplayName("Stream gives back exact input")
        void testReadStream() {
        String input = "{\"ref\": \"refs/heads/assessment\"}";
        InputStream targetStream = new ByteArrayInputStream(input.getBytes());
        
        try {
            String result = Utils.readStream(targetStream);
            assertEquals(input, result, "Should read the raw string accurately");
        } catch(IOException e) {
            fail("Test resulted in an exception");
        }
    }
}
