package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

/**
 * Client utility for interacting with the GitHub Commit Status API.
 *
 * <p>Provides helper methods for resolving GitHub status URLs
 * and posting commit status updates.</p>
 */
public class GitHubStatusClient {
    /**
     * Posts a commit status update to GitHub using the REST API.
     *
     * <p>This method sends an HTTP POST request to the provided
     * {@code statusesUrl} with a JSON body containing the state, description and context</p>
     *
     * @param statusesUrl the GitHub statuses API URL
     * @param state the commit state 
     * @param description a short message describing the status
     * @param context a string identifying the status context
     * @param token a GitHub personal access token used for authentication
     * @throws IOException if an I/O error occurs
     */
    public static void postStatus(String statusesUrl, String state, String description, String context, String token) throws IOException {
        URL url;
        try {
            url = URI.create(statusesUrl).toURL();
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid statuses URL", e);
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        JSONObject body = new JSONObject();
        body.put("state", state);
        body.put("description", description);
        body.put("context", context);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("GitHub status update failed with HTTP " + code);
        }
    }

    /**
     * Resolves a GitHub statuses URL template by replacing the
     * {@code {sha}} placeholder with the provided commit SHA.
     *
     * @param templateUrl the template URL returned by the GitHub API
     * @param sha the commit SHA to insert into the template
     * @return the resolved URL with the SHA substituted,
     *         or null if there is no template URL or it requires a SHA that is missing.
     */
    public static String resolveStatusesUrl(String templateUrl, String sha) {
        if (templateUrl == null || templateUrl.isEmpty()) {
            return null;
        }
        if (templateUrl.contains("{sha}")) {
            if (sha == null || sha.isEmpty()) {
                return null;
            }
            return templateUrl.replace("{sha}", sha);
        }
        return templateUrl;
    }
}
