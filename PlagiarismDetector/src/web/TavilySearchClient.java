package web;

import util.SimpleJsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tavily Search API client implemented with Java 11+ java.net.http.HttpClient.
 */
public class TavilySearchClient implements SearchClient {

    private static final String ENDPOINT = "https://api.tavily.com/search";
    private final String apiKey;
    private final HttpClient httpClient;

    public TavilySearchClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public String getServiceName() {
        return "Tavily Search API";
    }

    @Override
    public List<String> searchUrls(String queryPhrase, int maxResults) throws Exception {
        // Construct JSON payload
        String escapedQuery = queryPhrase.replace("\"", "\\\"").replace("\n", " ");
        String jsonPayload = String.format(
                "{\"api_key\":\"%s\",\"query\":\"%s\",\"max_results\":%d,\"search_depth\":\"basic\"}",
                apiKey, escapedQuery, maxResults
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Tavily API error: HTTP " + response.statusCode() + " - " + response.body());
        }

        List<String> urls = new ArrayList<>();
        // Try org.json if available via classpath, else use regex parser
        try {
            org.json.JSONObject obj = new org.json.JSONObject(response.body());
            if (obj.has("results")) {
                org.json.JSONArray results = obj.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    org.json.JSONObject item = results.getJSONObject(i);
                    if (item.has("url")) {
                        urls.add(item.getString("url"));
                    }
                }
            }
        } catch (Throwable t) {
            // Fallback to zero-dep parser
            urls = SimpleJsonParser.extractUrls(response.body());
        }

        return urls;
    }
}
