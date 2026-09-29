package io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Robust document reader responsible for loading text files from local storage.
 * Handles validation, missing files, permission errors, and character encoding.
 *
 * Time Complexity: O(N) where N is the file size in bytes (reading sequentially).
 * Space Complexity: O(N) to store the file contents in memory as a Java String.
 */
public class DocumentReader {

    /**
     * Reads the entire content of a file located at the specified path into a String.
     *
     * @param filePath Path to the document as a String.
     * @return Content of the file in UTF-8 format.
     * @throws IOException If file does not exist, cannot be read, or is inaccessible.
     */
    public static String readDocument(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }

        Path path = Paths.get(filePath.trim());

        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path.toAbsolutePath());
        }

        if (Files.isDirectory(path)) {
            throw new IOException("Specified path is a directory, not a text file: " + path.toAbsolutePath());
        }

        if (!Files.isReadable(path)) {
            throw new IOException("Permission denied: Cannot read file at " + path.toAbsolutePath());
        }

        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
