package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubStatusClientTest {
    @Test
    @DisplayName("Resolve statuses URL replaces SHA placeholder")
    void testResolveStatusesUrlReplacesShaPlaceholder() {
        String template = "https://api.github.com/repos/x/y/statuses/{sha}";
        String sha = "abc123";
        String result = GitHubStatusClient.resolveStatusesUrl(template, sha);
        assertEquals("https://api.github.com/repos/x/y/statuses/abc123", result);
    }

    @Test
    @DisplayName("Post status rejects invalid URL")
    void testPostStatusRejectsInvalidUrl() {
        assertThrows(IOException.class, () ->
            GitHubStatusClient.postStatus(
                "not-a-valid-url",
                "success",
                "ok",
                "ci/test",
                "token"
            )
        );
    }
}
