package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.example.util.Utils;

/**
 * Utility class that executes system commands and performs common Git and Gradle operations on repositories.
 */
public class CommandRunner {

    public static boolean showIO = true;

    /**
     * Represents the result of a command execution.
     */
    public static final class CommandResult {
        public final boolean success;
        public final String output;

        public CommandResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }

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

    private static CommandResult runCommandWithOutput(String... args) throws InterruptedException, IOException {
        ProcessBuilder command = new ProcessBuilder(args);
        command.redirectErrorStream(true);
        Process process = command.start();

        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = Utils.readStream(inputStream);
        }

        int exitCode = process.waitFor();
        if (CommandRunner.showIO && !output.isEmpty()) {
            System.out.print(output);
        }

        return new CommandResult(exitCode == 0, output);
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
        return cloneOrFetchRepoWithLogs(clone, url, repoDir, branchName).success;
    }

    /**
     * Clones or fetches a repository and captures execution logs.
     *
     * @param clone whether to clone ({@code true}) or fetch ({@code false})
     * @param url the Git repository URL
     * @param repoDir the local repository directory
     * @param branchName the branch to check out
     * @return command result including combined logs
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted
     */
    public static CommandResult cloneOrFetchRepoWithLogs(boolean clone, String url, String repoDir, String branchName)
            throws InterruptedException, IOException {
        String[] syncCommand = clone
            ? new String[] {"git", "clone", url, repoDir}
            : new String[] {"git", "-C", repoDir, "fetch"};
        String[] checkoutCommand = new String[] {
            "git", "-C", repoDir, "checkout", "-B", branchName, "origin/" + branchName
        };

        CommandResult syncResult = runCommandWithOutput(syncCommand);
        CommandResult checkoutResult = runCommandWithOutput(checkoutCommand);

        StringBuilder logs = new StringBuilder();
        logs.append("$ ").append(String.join(" ", syncCommand)).append('\n');
        logs.append(syncResult.output).append('\n');
        logs.append("$ ").append(String.join(" ", checkoutCommand)).append('\n');
        logs.append(checkoutResult.output).append('\n');

        return new CommandResult(syncResult.success && checkoutResult.success, logs.toString());
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
        return buildRepoWithLogs(repoPath).success;
    }

    /**
     * Builds the specified repository using its gradlew and captures logs.
     *
     * @param repoPath the path to the repository root directory
     * @return command result including combined logs
     * @throws IOException if an I/O error occurs during execution
     * @throws InterruptedException if the current thread is interrupted
     */
    public static CommandResult buildRepoWithLogs(String repoPath) throws InterruptedException, IOException {
        File wrapperFile = gradleWrapperFile(repoPath);
        if (!wrapperFile.isFile()) {
            return new CommandResult(false, "Gradle wrapper not found: " + wrapperFile.getAbsolutePath() + "\n");
        }

        String[] command = new String[] {wrapperFile.getAbsolutePath(), "build", "-x", "test", "--project-dir", repoPath};
        CommandResult commandResult = runCommandWithOutput(command);
        String logs = "$ " + String.join(" ", command) + '\n' + commandResult.output + '\n';
        return new CommandResult(commandResult.success, logs);
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
        return testRepoWithLogs(repoPath).success;
    }

    /**
     * Runs tests for the specified repository using its gradlew and captures logs.
     *
     * @param repoPath the path to the repository root directory
     * @return command result including combined logs
     * @throws IOException if an I/O error occurs during execution
     * @throws InterruptedException if the current thread is interrupted
     */
    public static CommandResult testRepoWithLogs(String repoPath) throws InterruptedException, IOException {
        File wrapperFile = gradleWrapperFile(repoPath);
        if (!wrapperFile.isFile()) {
            return new CommandResult(false, "Gradle wrapper not found: " + wrapperFile.getAbsolutePath() + "\n");
        }

        String[] command = new String[] {wrapperFile.getAbsolutePath(), "test", "--project-dir", repoPath};
        CommandResult commandResult = runCommandWithOutput(command);
        String logs = "$ " + String.join(" ", command) + '\n' + commandResult.output + '\n';
        return new CommandResult(commandResult.success, logs);
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
