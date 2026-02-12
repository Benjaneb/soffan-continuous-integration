package org.example.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;


/**
 * Utility helper methods used by the Continuous Integration server.
 */
public final class Utils {
    private Utils() {}

    /**
     * Creates a  directory path based on a SHA-256 hash of the provided seed string.
     * 
     * <p>This ensures a consistent and filesystem-safe directory name
     * for a given input string.</p>
     *
     * @param nameSeed the input string used to generate the directory name
     * @return a {@link File} representing the hashed directory path
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     *         in the current Java environment
     */
    public static File createHashedDir(String nameSeed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(nameSeed.getBytes(StandardCharsets.UTF_8));
        String dirName = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        return new File("/tmp/ci/" + dirName);
    }

    /**
     * Reads the entire contents of an {@link InputStream} into a single
     * String using UTF-8 encoding.
     *
     * @param is the input stream to read from
     * @return a {@code String} containing the full contents of the stream
     * @throws IOException if an I/O error occurs 
     */
    public static String readStream(InputStream is) throws IOException {
        // Use a Scanner to read the entire stream into a single String
        try (Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }
}
