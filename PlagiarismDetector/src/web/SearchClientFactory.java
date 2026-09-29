package web;

/**
 * Factory that detects available search API keys from environment variables.
 */
public class SearchClientFactory {

    /**
     * Attempts to create a SearchClient from environment variables.
     * Checks TAVILY_API_KEY first, then SERPAPI_API_KEY.
     *
     * @return Initialized SearchClient, or null if no valid key is configured.
     */
    public static SearchClient createClient() {
        String tavilyKey = System.getenv("TAVILY_API_KEY");
        if (tavilyKey != null && !tavilyKey.trim().isEmpty()) {
            return new TavilySearchClient(tavilyKey.trim());
        }

        String serpapiKey = System.getenv("SERPAPI_API_KEY");
        if (serpapiKey != null && !serpapiKey.trim().isEmpty()) {
            return new SerpApiSearchClient(serpapiKey.trim());
        }

        return null;
    }
}
