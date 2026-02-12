package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Stores and retrieves CI build history in JSON files under data/repositories.
 */
public final class BuildHistoryStore {
    private static final File REPOSITORIES_DIR = new File("data/repositories");
    private static final String BUILDS_FILENAME = "builds.json";

    private BuildHistoryStore() {}

    /**
     * Appends a build record for a specific repository.
     *
     * @param repositoryFullName repository full name (owner/repo)
     * @param buildRecord JSON object representing one build
     * @throws IOException if reading or writing build files fails
     */
    public static synchronized void appendBuild(String repositoryFullName, JSONObject buildRecord) throws IOException {
        File repositoryDir = new File(REPOSITORIES_DIR, repositoryFullName);
        File buildsFile = new File(repositoryDir, BUILDS_FILENAME);
        JSONArray existingBuilds = readBuildsArray(buildsFile);
        existingBuilds.put(new JSONObject(buildRecord.toString()));
        writeBuildsArray(buildsFile, existingBuilds);
    }

    /**
     * Lists all builds across all repositories as summary records.
     *
     * @return summaries sorted by buildDate descending
     * @throws IOException if reading build files fails
     */
    public static synchronized JSONArray listBuildSummaries() throws IOException {
        List<JSONObject> summaries = new ArrayList<JSONObject>();
        for (JSONObject build : loadAllBuilds()) {
            JSONObject summary = new JSONObject();
            String buildId = build.optString("id", "");
            summary.put("id", buildId);
            summary.put("repository", build.optString("repository", ""));
            summary.put("commit", build.optString("commit", ""));
            summary.put("buildDate", build.optString("buildDate", ""));
            summary.put("status", build.optString("status", ""));
            summary.put("url", "/builds/" + buildId);
            summaries.add(summary);
        }
        JSONArray output = new JSONArray();
        for (JSONObject summary : summaries) {
            output.put(summary);
        }
        return output;
    }

    /**
     * Finds a build by ID across all repositories.
     *
     * @param buildId the build ID
     * @return the stored build record or null if no record matches
     * @throws IOException if reading build files fails
     */
    public static synchronized JSONObject getBuildById(String buildId) throws IOException {
        if (buildId == null || buildId.isEmpty()) {
            return null;
        }

        for (JSONObject build : loadAllBuilds()) {
            if (buildId.equals(build.optString("id", ""))) {
                return new JSONObject(build.toString());
            }
        }
        return null;
    }

    private static List<JSONObject> loadAllBuilds() throws IOException {
        List<JSONObject> builds = new ArrayList<JSONObject>();
        if (!REPOSITORIES_DIR.isDirectory()) {
            return builds;
        }

        for (File buildsFile : findBuildFiles(REPOSITORIES_DIR)) {
            JSONArray repositoryBuilds = readBuildsArray(buildsFile);
            for (int i = 0; i < repositoryBuilds.length(); i++) {
                builds.add(repositoryBuilds.getJSONObject(i));
            }
        }
        return builds;
    }

    private static List<File> findBuildFiles(File rootDirectory) {
        List<File> buildFiles = new ArrayList<File>();
        collectBuildFiles(rootDirectory, buildFiles);
        return buildFiles;
    }

    private static void collectBuildFiles(File directory, List<File> buildFiles) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                collectBuildFiles(child, buildFiles);
            } else if (BUILDS_FILENAME.equals(child.getName())) {
                buildFiles.add(child);
            }
        }
    }

    private static JSONArray readBuildsArray(File buildsFile) throws IOException {
        if (!buildsFile.isFile()) {
            return new JSONArray();
        }

        String content = new String(Files.readAllBytes(buildsFile.toPath()), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return new JSONArray();
        }

        try {
            return new JSONArray(content);
        } catch (JSONException e) {
            throw new IOException("Invalid JSON in " + buildsFile.getAbsolutePath(), e);
        }
    }

    private static void writeBuildsArray(File buildsFile, JSONArray buildsArray) throws IOException {
        Files.write(buildsFile.toPath(), buildsArray.toString(2).getBytes(StandardCharsets.UTF_8));
    }
}
