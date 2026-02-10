package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

class CommandRunnerTest {
    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        CommandRunner.showIO = false;
    }

    @Test
    @DisplayName("Empty directory fails build")
    void testEmptyDirBuildFail() {
        try {
            boolean success = CommandRunner.buildRepo(tempDir.getAbsolutePath());
            assertFalse(success);
        } catch (Exception e) {
            fail("Test resulted in an exception");
        }
    }

    @Test
    @DisplayName("Empty Gradle project succeeds build")
    void testEmptyGradleProjectBuilds() {
        try {
            boolean initSuccess = CommandRunner.runCommand(
                "gradle", "init",
                "--project-dir", tempDir.getAbsolutePath(),
                "--type=java-application",
                "--use-defaults"
            );
            assertTrue(initSuccess);

            boolean success = CommandRunner.buildRepo(tempDir.getAbsolutePath());
            assertTrue(success);
        } catch (Exception e) {
            fail("Test resulted in an exception");
        }
    }

    @Test
    @DisplayName("Invalid URL fails clone")
    void testInvalidURLFailsClone() {
        try {
            File repoDir = new File(tempDir, "repo");
            String absoluteRepoDir = repoDir.getAbsolutePath();
            boolean success = CommandRunner.cloneOrFetchRepo(true, "", absoluteRepoDir, "");
            assertFalse(success);
        } catch (Exception e) {
            fail("Test resulted in an exception");
        }
    }

    @Test
    @DisplayName("Cloning the repo of this code succeeds")
    void testCloningThisRepoSucceeds() {
        try {
            File repoDir = new File(tempDir, "repo");
            String absoluteRepoDir = repoDir.getAbsolutePath();
            boolean success = CommandRunner.cloneOrFetchRepo(true, "git@github.com:Benjaneb/soffan-continuous-integration.git", absoluteRepoDir, "main");
            assertTrue(success);
        } catch (Exception e) {
            fail("Test resulted in an exception");
        }
    }

    @Test
    @DisplayName("Empty Gradle project succeeds testing")
    void testEmptyGradleProjectTest() {
        try {
            boolean initSuccess = CommandRunner.runCommand(
                "gradle", "init",
                "--project-dir", tempDir.getAbsolutePath(),
                "--type=java-application",
                "--use-defaults"
            );
            assertTrue(initSuccess);

            boolean buildSuccess = CommandRunner.buildRepo(tempDir.getAbsolutePath());
            assertTrue(buildSuccess);

            boolean testSuccess = CommandRunner.testRepo(tempDir.getAbsolutePath());
            assertTrue(testSuccess);
        } catch (Exception e) {
            fail("Test resulted in an exception");
        }
    }
}
