package org.example;

import java.io.File;
import java.io.IOException;

/**
 * Utility class that executes system commands and performs common Git and Gradle operations on repositories.
 */
public class CommandRunner {

    public static boolean showIO = true;
    /**
     * Executes a system command using ProcessBuilder}.
     *
     * @param args the command and its arguments to execute
     * @return {@code true} if the process exits with status code {@code 0};
     *         {@code false} otherwise
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted
     */
    public static boolean runCommand(String... args) throws InterruptedException, IOException {
        ProcessBuilder command = new ProcessBuilder(args);
        if (CommandRunner.showIO) command.inheritIO();
        int exitCode = command.start().waitFor();
        return exitCode == 0;
    }

    /**
     * Clones a Git repository if it does not already exist or fetches updates from an existing one,
     * then checks out the specified branch.
     *
     * @param clone whether to clone ({@code true}) or fetch ({@code false})
     * @param url the Git repository URL
     * @param repoDir the local repository directory
     * @param branchName the branch to check out
     * @return {@code true} if the clone or fetch command succeeds
     *         (exit code {@code 0}); {@code false}
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted
     */
    public static boolean cloneOrFetchRepo(boolean clone, String url, String repoDir, String branchName) throws InterruptedException, IOException {
        boolean success = (clone) ?
            runCommand("git", "clone", url, repoDir) :
            runCommand("git", "-C", repoDir, "fetch") ;
        
        // Switch working tree to correct branch
        runCommand("git", "-C", repoDir, "checkout", "-B", branchName, "origin/" + branchName) ;
        return success;
    }

    /**
     * Builds the specified repository using its gradlew.
     *
     * @param repoPath the path to the repository root directory
     * @return {@code true} if the build succeeds (exit code {@code 0});
     *         {@code false} if the wrapper is missing or the build fails
     * @throws IOException if an I/O error occurs during execution
     * @throws InterruptedException if the current thread is interrupted
     */
    public static boolean buildRepo(String repoPath) throws InterruptedException, IOException {
        File wrapperFile = gradleWrapperFile(repoPath);
        if (!wrapperFile.isFile()) return false;
        return runCommand(wrapperFile.getAbsolutePath(), "build", "-x", "test", "--project-dir", repoPath);
    }

    /**
     * Runs tests for the specified repository using its gradlew.
     *
     * @param repoPath the path to the repository root directory
     * @return {@code true} if the tests succeed (exit code {@code 0});
     *         {@code false} if the wrapper is missing or the tests fail
     * @throws IOException if an I/O error occurs during execution
     * @throws InterruptedException if the current thread is interrupted
     */
    public static boolean testRepo(String repoPath) throws InterruptedException, IOException {
        File wrapperFile = gradleWrapperFile(repoPath);
        if (!wrapperFile.isFile()) return false;
        return runCommand(wrapperFile.getAbsolutePath(), "test", "--project-dir", repoPath);
    }
    
    /**
     * Resolves the appropriate Gradle Wrapper file for the current
     * operating system.
     *
     * @param repoPath the repository root directory
     * @return a {@link File} representing the expected wrapper script
     */
    private static File gradleWrapperFile(String repoPath) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String wrapperName = isWindows ? "gradlew.bat" : "gradlew";
        return new File(repoPath, wrapperName);
    }
}
