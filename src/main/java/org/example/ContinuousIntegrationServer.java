package org.example;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.JSONException;
import org.json.JSONObject;

import org.example.util.Utils;

/**
 * A simple Continuous Integration server implemented using Jetty.
 *
 * <p>This server acts as a GitHub webhook endpoint. It waits for a post request 
 * that contains a Github push event payload. When one is recieved it:<p>
 * <ol>
 *   <li>Optionally verifies the webhook signature (if a secret is configured).</li>
 *   <li>Looks at the JSON payload to get information about the repository and branch.</li>
 *   <li>Clones or fetches the repository locally.</li>
 *   <li>Builds the project using Gradle.</li>
 *   <li>Runs tests using Gradle.</li>
 *   <li>Updates the commit status on GitHub.</li>
 * </ol>
 */
public class ContinuousIntegrationServer extends AbstractHandler
{
    /**
     * Handles incoming HTTP requests to the CI server.
     *
     * <p> Expects a POST request and will otherwise respond with a simple 
     * message to indicate that the server is running
     *
     * <p> if a webhook secret is configured the signature is verified. 
     * Invalid signatures lead to the request being rejected.<p> 
     *
     * <p>For valid GitHub webhook payloads, this method:</p>
     * <ul>
     *   <li>Extracts repository metadata.</li>
     *   <li>Posts a pending commit status if token is given.</li>
     *   <li>Clones or fetches the repository.</li>
     *   <li>Builds the project using Gradle.</li>
     *   <li>Tests the project using Gradle.</li>
     *   <li>Posts a final success or sailure status if token is given.</li>
     * </ul>
     *
     * <p> Errors are handled and logged.<p> 
     *
     * @param target the target of the request
     * @param baseRequest the Jetty-specific request object
     * @param request the HTTP servlet request containing headers and payload
     * @param response the HTTP servlet response used to return status information
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet-related error occurs
     */
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        baseRequest.setHandled(true);
        String method = request.getMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGetRequest(target, response);
            return;
        }

        // If the request is not post
        if (!"POST".equalsIgnoreCase(method)) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("CI server running");
            System.out.println("Something other than post received");
            return;
        }
        String payload = Utils.readStream(request.getInputStream());

        // Check signature of payload (if we have one set up)
        String secret = System.getProperty("webhookSecret");
        boolean hasSecret = secret != null && !secret.isBlank();

        if (hasSecret) {
            boolean validPayload = PayloadVerifier.isValidPayload(
                payload,
                secret,
                request.getHeader("X-Hub-Signature-256")
            );

            if (!validPayload) {
                response.getWriter().println("Invalid signature");
                System.out.println("Signature of POST request was invalid");
                return;
            }
            System.out.println("Signature passed!");
        }

        // If request is JSON
        try {
            System.out.println("POST request received");

            // Get JSON data
            JSONObject json = new JSONObject(payload);
            String cloneUrl = json.getJSONObject("repository").getString("clone_url");
            String fullName = json.getJSONObject("repository").getString("full_name");
            String branchName = json.getString("ref").replace("refs/heads/", "");

            String statusesUrlTemplate = json.getJSONObject("repository").optString("statuses_url", null);
            String sha = json.optString("after", null);
            String statusesUrl = GitHubStatusClient.resolveStatusesUrl(statusesUrlTemplate, sha);
            String token = System.getProperty("githubToken");
            boolean hasToken = token != null && !token.isBlank();
            boolean canPostStatus = hasToken && statusesUrl != null && !statusesUrl.isBlank();
            String buildId = UUID.randomUUID().toString();
            String buildDate = Instant.now().toString();
            StringBuilder buildLogs = new StringBuilder();

            // Set initial GitHub commit status to 'Pending'
            if (canPostStatus) {
                try {
                    GitHubStatusClient.postStatus(statusesUrl, "pending", "Build started", fullName, token);
                } catch (IOException e) {
                    System.out.println("Failed to post pending status to GitHub");
                }
            } else if (!hasToken) {
                System.out.println("No githubToken provided; skipping GitHub status updates");
            } else {
                System.out.println("No statuses URL found in payload; skipping GitHub status updates");
            }

            File repoDir = Utils.createHashedDir(fullName);
            String absoluteRepoDir = repoDir.getAbsolutePath();

            boolean buildSuccess = false;
            boolean testsSuccess = false;
            try {
                // Core CI feature #1: Set up and build (compile)
                boolean cloneRepo = !repoDir.exists();
                CommandRunner.CommandResult repoResult = CommandRunner.cloneOrFetchRepoWithLogs(
                    cloneRepo, cloneUrl, absoluteRepoDir, branchName
                );
                buildLogs.append("Repository setup: \n").append(repoResult.output).append('\n');

                CommandRunner.CommandResult buildResult;
                if (repoResult.success) {
                    buildResult = CommandRunner.buildRepoWithLogs(absoluteRepoDir);
                } else {
                    buildResult = new CommandRunner.CommandResult(false, "Repository setup failed; build skipped.\n");
                }
                buildLogs.append("Build: \n").append(buildResult.output).append('\n');
                buildSuccess = repoResult.success && buildResult.success;

                // Core CI feature #2: Run tests
                CommandRunner.CommandResult testResult;
                if (repoResult.success) {
                    testResult = CommandRunner.testRepoWithLogs(absoluteRepoDir);
                } else {
                    testResult = new CommandRunner.CommandResult(false, "Repository setup failed; tests skipped.\n");
                }
                buildLogs.append("Test: \n").append(testResult.output).append('\n');
                testsSuccess = testResult.success;
            } finally {
                // Send final commit status to GitHub
                if (canPostStatus) {
                    try {
                        if (!buildSuccess) {
                            System.out.println("❌ Build failed");
                            GitHubStatusClient.postStatus(statusesUrl, "failure", "Build failed!", fullName, token);
                        } else if (!testsSuccess) {
                            System.out.println("❌ Tests failed");
                            GitHubStatusClient.postStatus(statusesUrl, "failure", "Tests failed!", fullName, token);
                        } else {
                            System.out.println("✅ Build & tests succeeded!");
                            GitHubStatusClient.postStatus(statusesUrl, "success", "Build succeeded and tests passed!", fullName, token);
                        }
                    } catch (IOException e) {
                        System.out.println("Failed to post final status to GitHub");
                    }
                }
            }

            JSONObject buildRecord = createBuildRecord(
                buildId,
                fullName,
                sha,
                branchName,
                buildDate,
                buildSuccess,
                testsSuccess,
                buildLogs.toString()
            );
            BuildHistoryStore.appendBuild(fullName, buildRecord);

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "Build processed");
            responseBody.put("url", "/builds/" + buildId);
            response.getWriter().println(responseBody.toString(2));
        }

        // Needed for SHA-256 to run
        catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is not supported");
        }

        // Request is not JSON format
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

    private void handleGetRequest(String target, HttpServletResponse response) throws IOException {
        if ("/builds".equals(target)) {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(BuildHistoryStore.listBuildSummaries().toString(2));
            return;
        }

        if (target != null && target.startsWith("/builds/")) {
            String buildId = target.substring("/builds/".length());
            JSONObject build = BuildHistoryStore.getBuildById(buildId);
            response.setContentType("application/json;charset=utf-8");
            if (build == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().println(new JSONObject().put("error", "Build not found").toString());
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(build.toString(2));
            }
            return;
        }

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("CI server running");
    }

    private JSONObject createBuildRecord(
        String buildId,
        String repository,
        String commit,
        String branch,
        String buildDate,
        boolean buildSuccess,
        boolean testsSuccess,
        String logs
    ) {
        JSONObject buildRecord = new JSONObject();
        buildRecord.put("id", buildId);
        buildRecord.put("repository", repository);
        buildRecord.put("commit", commit == null ? "" : commit);
        buildRecord.put("branch", branch);
        buildRecord.put("buildDate", buildDate);
        buildRecord.put("buildSuccess", buildSuccess);
        buildRecord.put("testsSuccess", testsSuccess);
        buildRecord.put("status", buildSuccess && testsSuccess ? "success" : "failure");
        buildRecord.put("logs", logs);
        return buildRecord;
    }
 
    /**
     * Starts the Continuous Integration server on port 8007.
     *
     * <p>This method initializes a Jetty server and registers
     * a {@link ContinuousIntegrationServer} instance to handle the requests,
     * and blocks the main thread until the server is stopped.</p>
     *
     * @param args command-line arguments
     * @throws Exception if the server fails to start or encounters
     *         a fatal runtime error
     */
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8007);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
