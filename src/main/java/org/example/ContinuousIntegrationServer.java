package org.example;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.JSONObject;


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

            // Create file for repo with hashed dirName
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fullName.getBytes(StandardCharsets.UTF_8));
            String dirName = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            File repoDir = new File("/tmp/ci/" + dirName);

            // Clone repo if it has not previously been cloned
            if (!repoDir.exists()) {
                ProcessBuilder clone = new ProcessBuilder("git", "clone", cloneUrl, repoDir.getAbsolutePath());
                clone.inheritIO();
                clone.start().waitFor();
            } 
            
            // Fetch repo if it has previously been cloned
            else {
                ProcessBuilder fetch = new ProcessBuilder("git", "-C", repoDir.getAbsolutePath(), "fetch");
                fetch.inheritIO();
                fetch.start().waitFor();
            }
        } 
        
        // This was needed for SHA-256 to run
        catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is not supported");
        }

        // Requst is not JSON format
        catch (org.json.JSONException e) {
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