package util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, zero-dependency JSON helper designed to extract URLs or strings from
 * web search API responses without requiring external library jars.
 */
public class SimpleJsonParser {

    private static final Pattern URL_KEY_PATTERN = Pattern.compile("\"(?:url|link)\"\\s*:\\s*\"(https?://[^\"]+)\"");

    /**
     * Extracts all HTTP/HTTPS URLs associated with "url" or "link" keys in a JSON string.
     *
     * @param json Raw JSON response body.
     * @return List of matched URL strings.
     */
    public static List<String> extractUrls(String json) {
        List<String> urls = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return urls;
        }

        Matcher matcher = URL_KEY_PATTERN.matcher(json);
        while (matcher.find()) {
            String url = matcher.group(1).replace("\\/", "/");
            urls.add(url);
        }
        return urls;
    }
}
