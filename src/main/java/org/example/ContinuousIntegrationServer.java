package org.example;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

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

            // Set initial GitHub commit status to 'Pending'
            try {
                GitHubStatusClient.postStatus(statusesUrl, "pending", "Build started", fullName, token);
            } catch (IOException e) {
                System.out.println("Failed to post pending status to GitHub");
            }

            File repoDir = Utils.createHashedDir(fullName);
            String absoluteRepoDir = repoDir.getAbsolutePath();

            boolean buildSuccess = false;
            boolean testsSuccess = false;
            try {
                // Core CI feature #1: Set up and build (compile)
                boolean cloneRepo = !repoDir.exists();
                boolean repoSuccess = CommandRunner.cloneOrFetchRepo(cloneRepo, cloneUrl, absoluteRepoDir, branchName);
                buildSuccess = repoSuccess &&  CommandRunner.buildRepo(absoluteRepoDir);

                // Core CI feature #2: Run tests
                testsSuccess = CommandRunner.testRepo(absoluteRepoDir);
            } finally {
                // Send final commit status to GitHub
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
 
    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8007);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
