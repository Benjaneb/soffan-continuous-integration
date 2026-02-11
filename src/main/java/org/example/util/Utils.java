package org.example.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public final class Utils {
    private Utils() {}

    // Creates file with hashed directory name
    public static File createHashedDir(String nameSeed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(nameSeed.getBytes(StandardCharsets.UTF_8));
        String dirName = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        return new File("/tmp/ci/" + dirName);
    }

    public static String readStream(InputStream is) throws IOException {
        // Use a Scanner to read the entire stream into a single String
        try (Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }
}
