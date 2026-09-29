package web;

import util.SimpleJsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SerpApi Google Search client implemented with Java 11+ java.net.http.HttpClient.
 */
public class SerpApiSearchClient implements SearchClient {

    private final String apiKey;
    private final HttpClient httpClient;

    public SerpApiSearchClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public String getServiceName() {
        return "SerpApi (Google Search Engine)";
    }

    @Override
    public List<String> searchUrls(String queryPhrase, int maxResults) throws Exception {
        String encodedQuery = URLEncoder.encode(queryPhrase, StandardCharsets.UTF_8);
        String endpoint = String.format(
                "https://serpapi.com/search.json?engine=google&q=%s&num=%d&api_key=%s",
                encodedQuery, maxResults, apiKey
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("SerpApi error: HTTP " + response.statusCode() + " - " + response.body());
        }

        List<String> urls = new ArrayList<>();
        try {
            org.json.JSONObject obj = new org.json.JSONObject(response.body());
            if (obj.has("organic_results")) {
                org.json.JSONArray organic = obj.getJSONArray("organic_results");
                for (int i = 0; i < organic.length(); i++) {
                    org.json.JSONObject item = organic.getJSONObject(i);
                    if (item.has("link")) {
                        urls.add(item.getString("link"));
                    }
                }
            }
        } catch (Throwable t) {
            urls = SimpleJsonParser.extractUrls(response.body());
        }

        return urls;
    }
}
