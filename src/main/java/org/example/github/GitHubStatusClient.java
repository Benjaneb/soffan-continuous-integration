package org.example.github;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class GitHubStatusClient {
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
