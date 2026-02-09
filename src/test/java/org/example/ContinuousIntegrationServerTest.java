package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.example.util.Utils;

class ContinuousIntegrationServerTest {
    @TempDir
    File tempDir;

    @Test
    @DisplayName("Empty directory fails build")
    void testEmptyDirBuildFail() {
        try {
            boolean success = ContinuousIntegrationServer.buildRepo(tempDir.getAbsolutePath(), false);
            assertFalse(success);
        } catch (Exception e) {
            fail("Building empty directory resulted in an exception");
        }
    }

    @Test
    @DisplayName("Empty Gradle project succeeds build")
    void testEmptyGradleProjectBuilds() {
        try {
            ProcessBuilder init = new ProcessBuilder(
                "gradle", "init",
                "--project-dir", tempDir.getAbsolutePath(),
                "--type=java-application",
                "--use-defaults"
            );
            int exitCode = init.start().waitFor();

            boolean success = ContinuousIntegrationServer.buildRepo(tempDir.getAbsolutePath(), false);
            assertTrue(success);
        } catch (Exception e) {
            fail("Building empty directory resulted in an exception");
        }
    }
}
