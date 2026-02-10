package org.example;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.JSONObject;
import org.json.JSONException;

import org.example.util.Utils;
import org.example.github.GitHubStatusClient;


/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler
{
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        String payload = request.getReader().lines().collect(Collectors.joining());

        // If the request is not post
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.getWriter().println("CI server running");
            System.out.println("Something other than post received");
            return;
        }

        // If request is JSON
        try {
            JSONObject json = new JSONObject(payload);

            // normal CI logic
            System.out.println("Valid webhook received");
            String cloneUrl = json.getJSONObject("repository").getString("clone_url");
            String fullName = json.getJSONObject("repository").getString("full_name");
            String statusesUrlTemplate = json.getJSONObject("repository").optString("statuses_url", null);
            String sha = json.optString("after", null);

            String statusesUrl = GitHubStatusClient.resolveStatusesUrl(statusesUrlTemplate, sha);
            String token = System.getProperty("githubToken");
            boolean canPostStatus = token != null && statusesUrl != null;

            if (canPostStatus) {
                try {
                    GitHubStatusClient.postStatus(statusesUrl, "pending", "Build started", fullName, token);
                } catch (IOException e) {
                    System.out.println("Failed to post pending status to GitHub");
                }
            } else {
                System.out.println("GitHub token or statuses URL missing; skipping status updates");
            }

            File repoDir = Utils.createHashedDir(fullName);

            boolean buildSuccess = false;
            boolean testsSuccess = false;
            try {
                boolean repoSuccess = cloneOrFetchRepo(cloneUrl, repoDir, true);

                buildSuccess = repoSuccess && buildRepo(repoDir.getAbsolutePath(), true);
                // TODO: run tests here and set testsSuccess based on the result
                testsSuccess = true;

                if (buildSuccess)
                    System.out.println("Build succeeded!");
                else
                    System.out.println("Build failed");
            } finally {
                if (canPostStatus) {
                    try {
                        if (!buildSuccess) {
                            GitHubStatusClient.postStatus(statusesUrl, "failure", "Build failed!", fullName, token);
                        } else if (!testsSuccess) {
                            GitHubStatusClient.postStatus(statusesUrl, "failure", "Tests failed!", fullName, token);
                        } else {
                            GitHubStatusClient.postStatus(statusesUrl, "success", "Build succeeded and tests passed!", fullName, token);
                        }
                    } catch (IOException e) {
                        System.out.println("Failed to post final status to GitHub");
                    }
                }
            }
        }

        // This was needed for SHA-256 to run
        catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is not supported");
        }

        // Requst is not JSON format
        catch (JSONException e) {
            System.out.println("Received non-JSON payload (ignored)");
        }

        // Interruption
        catch (InterruptedException e) {
            System.out.println("CI job interrupted");
            Thread.currentThread().interrupt();
        }

        // IO error
        catch (IOException e) {
            System.out.println("IO error during CI job");
            e.printStackTrace();
        }
    }

    public static boolean cloneOrFetchRepo(String url, File repoDir, boolean showIO) throws InterruptedException, IOException {
        // Clone repo if it has not previously been cloned
        if (!repoDir.exists()) {
            ProcessBuilder clone = new ProcessBuilder("git", "clone", url, repoDir.getAbsolutePath());
            if (showIO) clone.inheritIO();
            int exitCode = clone.start().waitFor();
            return exitCode == 0;
        }
        // Fetch repo if it has previously been cloned
        else {
            ProcessBuilder fetch = new ProcessBuilder("git", "-C", repoDir.getAbsolutePath(), "fetch");
            if (showIO) fetch.inheritIO();
            int exitCode = fetch.start().waitFor();
            return exitCode == 0;
        }
    }

    public static boolean buildRepo(String repoPath, boolean showIO) throws InterruptedException, IOException {
        ProcessBuilder build = new ProcessBuilder("gradle", "build", "--project-dir", repoPath);
        if (showIO) build.inheritIO();
        int exitCode = build.start().waitFor();
        return exitCode == 0;
    }
 
    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8007);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
