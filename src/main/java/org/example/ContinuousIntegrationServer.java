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

        switch (request.getMethod().toUpperCase()) {
            case "POST":
                if (target.equals("/")) {
                    handleWebhook(payload);
                } else {
                    System.out.println("Invalid request: " + request.getMethod() + " " + target);
                    return;
                }
                break;
            case "GET":
                String[] targetParts = target.split("/");
                if (targetParts.length < 3) {
                    System.out.println("Invalid request: " + request.getMethod() + " " + target);
                    return;
                }
                // targetParts[0] == ""
                String repoName = targetParts[1];
                String item = targetParts[2];
                if (item.equals("logs")) {
                    // Return all logs
                    String responseString = "Logs of " + repoName;
                    response.getWriter().println(responseString);
                } else if (item.equals("log")) {
                    String commitHash = targetParts[3];
                    // Return specific log
                    String responseString = "Log of " + commitHash;
                    response.getWriter().println(responseString);
                }
                break;
            default:
                System.out.println("Something other than POST or GET received");
                return;
        }
    }

    private void handleWebhook(String payload) {
        try {
            System.out.println("POST request received");

            // Get JSON data
            JSONObject json = new JSONObject(payload);
            String cloneUrl = json.getJSONObject("repository").getString("clone_url");
            String fullName = json.getJSONObject("repository").getString("full_name");
            String branchName = json.getString("ref").replace("refs/heads/", "");

            File repoDir = Utils.createHashedDir(fullName);
            String absoluteRepoDir = repoDir.getAbsolutePath();

            // Core CI feature #1: Set up and build (compile)
            boolean cloneRepo = !repoDir.exists();
            boolean repoSuccess = CommandRunner.cloneOrFetchRepo(cloneRepo, cloneUrl, absoluteRepoDir, branchName);            
            boolean buildSuccess = CommandRunner.buildRepo(absoluteRepoDir);

            if (buildSuccess) {
                System.out.println("✅ Build succeeded!");
            } else {
                System.out.println("❌ Build failed");
            }

            // Core CI feature #2: Run tests
            boolean testSuccess = CommandRunner.testRepo(absoluteRepoDir);
            
            if (testSuccess) {
                System.out.println("✅ Tests succeeded!");
            } else {
                System.out.println("❌ Tests failed");
            }

            // Core CI feature #3: Notification
            // ...
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
