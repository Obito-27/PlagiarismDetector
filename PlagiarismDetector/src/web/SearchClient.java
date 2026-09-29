package web;

import java.util.List;

/**
 * Interface abstracting search engine query operations behind a unified contract.
 */
public interface SearchClient {

    /**
     * Queries the external search API with a distinctive phrase.
     *
     * @param queryPhrase Distinctive search phrase.
     * @param maxResults Maximum number of result URLs to retrieve.
     * @return List of discovered web URLs.
     * @throws Exception On network or API communication errors.
     */
    List<String> searchUrls(String queryPhrase, int maxResults) throws Exception;

    /**
     * Returns the name of the search provider.
     */
    String getServiceName();
}
