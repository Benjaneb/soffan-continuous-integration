package org.example;

import java.io.IOException;

public class CommandRunner {

    public static boolean showIO = true;

    public static boolean runCommand(String... args) throws InterruptedException, IOException {
        ProcessBuilder command = new ProcessBuilder(args);
        if (CommandRunner.showIO) command.inheritIO();
        int exitCode = command.start().waitFor();
        return exitCode == 0;
    }

    public static boolean cloneOrFetchRepo(boolean clone, String url, String repoDir, String branchName) throws InterruptedException, IOException {
        boolean success = (clone) ?
            runCommand("git", "clone", url, repoDir) :
            runCommand("git", "-C", repoDir, "fetch") ;
        
        // Switch working tree to correct branch
        runCommand("git", "-C", repoDir, "checkout", "-B", branchName, "origin/" + branchName) ;
        return success;
    }

    public static boolean buildRepo(String repoPath) throws InterruptedException, IOException {
        return runCommand("gradle", "build", "-x", "test", "--project-dir", repoPath);
    }

    public static boolean testRepo(String repoPath) throws InterruptedException, IOException {
        return runCommand("gradle", "test", "--project-dir", repoPath);
    }
}
