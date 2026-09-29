package web;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fetches HTML web pages and extracts clean, readable plain text using Jsoup.
 *
 * Fault Tolerance:
 * Resilient against network timeouts, HTTP errors (403, 404, 500), SSL handshakes, and bot-detection blocks.
 * Emits a polite warning on error and skips to the next candidate URL without interrupting the system.
 */
public class PageFetcher {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MILLIS = 7000;

    /**
     * Fetches plain text for a collection of deduplicated URLs.
     *
     * @param urls Set of unique candidate URLs.
     * @return Map of URL to extracted text.
     */
    public static Map<String, String> fetchAll(Set<String> urls) {
        Map<String, String> pageContents = new LinkedHashMap<>();
        if (urls == null || urls.isEmpty()) {
            return pageContents;
        }

        for (String url : urls) {
            String text = fetchSinglePage(url);
            if (text != null && !text.trim().isEmpty()) {
                pageContents.put(url, text);
            }
        }
        return pageContents;
    }

    /**
     * Fetches and parses a single URL.
     */
    public static String fetchSinglePage(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        // 1. Try fetching via Jsoup
        try {
            org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();

            if (jsoupDoc.body() != null) {
                return jsoupDoc.body().text();
            }
        } catch (LinkageError e) {
            // If Jsoup jar is absent from classpath, fall back to pure Java HttpClient + regex stripper
            return fetchWithHttpClientFallback(url);
        } catch (Exception e) {
            System.err.printf("  [WARNING] Failed to fetch URL %s: %s (Skipping)\n", url, e.getMessage());
            return null;
        }

        return null;
    }

    private static String fetchWithHttpClientFallback(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String html = resp.body();
                // Strip scripts, styles, and tags
                html = html.replaceAll("(?s)<script.*?</script>", " ");
                html = html.replaceAll("(?s)<style.*?</style>", " ");
                html = html.replaceAll("<[^>]+>", " ");
                html = html.replaceAll("\\s+", " ").trim();
                return html;
            }
        } catch (Exception e) {
            System.err.printf("  [WARNING] Fallback fetch failed for %s: %s\n", url, e.getMessage());
        }
        return null;
    }
}


