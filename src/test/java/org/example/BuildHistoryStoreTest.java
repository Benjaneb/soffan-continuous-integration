package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildHistoryStoreTest {
    private static final String OWNER = "test_owner123";
    private static final String REPOSITORY = "test_repoitory123";
    private static final String FULL_REPOSITORY_NAME = OWNER + "/" + REPOSITORY;
    private static final Path OWNER_DIRECTORY = Paths.get("data", "repositories", OWNER);

    @BeforeEach
    void setUp() throws IOException {
        deleteTestBuildsDirectory();
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteTestBuildsDirectory();
    }

    @Test
    @DisplayName("Append build stores build in builds.json")
    void testAppendBuildStoresRecord() throws IOException {
        String buildId = "build-append";
        BuildHistoryStore.appendBuild(FULL_REPOSITORY_NAME, createBuild(buildId));
        Path buildsFile = Paths.get("data", "repositories", OWNER, REPOSITORY, "builds.json");
        assertTrue(Files.exists(buildsFile));

        JSONArray storedBuilds = new JSONArray(Files.readString(buildsFile));
        assertEquals(1, storedBuilds.length());
        assertEquals(buildId, storedBuilds.getJSONObject(0).getString("id"));
    }

    @Test
    @DisplayName("List build summaries includes stored build")
    void testListBuildSummariesIncludesBuild() throws IOException {
        String buildId = "build-summary";
        BuildHistoryStore.appendBuild(FULL_REPOSITORY_NAME, createBuild(buildId));
        JSONArray summaries = BuildHistoryStore.listBuildSummaries();
        JSONObject summary = null;
        for (int i = 0; i < summaries.length(); i++) {
            JSONObject current = summaries.getJSONObject(i);
            if (buildId.equals(current.optString("id"))) {
                summary = current;
                break;
            }
        }
        assertNotNull(summary);
        assertEquals(FULL_REPOSITORY_NAME, summary.getString("repository"));
        assertEquals("/builds/" + buildId, summary.getString("url"));
    }

    @Test
    @DisplayName("Get build by id returns stored build")
    void testGetBuildByIdReturnsBuild() throws IOException {
        String buildId = "build-by-id";
        BuildHistoryStore.appendBuild(FULL_REPOSITORY_NAME, createBuild(buildId));
        JSONObject build = BuildHistoryStore.getBuildById(buildId);

        assertNotNull(build);
        assertEquals(buildId, build.getString("id"));
        assertEquals(FULL_REPOSITORY_NAME, build.getString("repository"));
    }

    private JSONObject createBuild(String buildId) {
        return new JSONObject()
            .put("id", buildId)
            .put("repository", FULL_REPOSITORY_NAME)
            .put("commit", "abc123")
            .put("buildDate", "2026-02-12T00:00:00Z")
            .put("status", "success");
    }

    private static void deleteTestBuildsDirectory() throws IOException {
        if (Files.exists(OWNER_DIRECTORY)) {
            try (Stream<Path> files = Files.walk(OWNER_DIRECTORY)) {
                List<Path> sortedFiles = files.sorted(Comparator.reverseOrder()).toList();
                for (Path file : sortedFiles) {
                    Files.delete(file);
                }
            }
        }
    }
}
