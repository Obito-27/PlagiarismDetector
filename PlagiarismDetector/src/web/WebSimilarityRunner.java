package web;

import preprocessing.ProcessedDocument;
import preprocessing.TextPreprocessor;
import report.SimilarityReport;
import util.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates the optional web plagiarism detection workflow.
 * Extracts distinctive phrases, executes search queries, fetches candidate pages,
 * evaluates each source using the core DSA comparison pipeline, and prints a web report.
 */
public class WebSimilarityRunner {

    public static class WebMatchEntry {
        private final String url;
        private final SimilarityReport report;

        public WebMatchEntry(String url, SimilarityReport report) {
            this.url = url;
            this.report = report;
        }

        public String getUrl() { return url; }
        public SimilarityReport getReport() { return report; }
        public double getScore() { return report.getAggregatedScore().getFinalCompositeScore(); }
    }

    /**
     * Executes the web comparison pipeline.
     * Guaranteed to fall back gracefully without terminating the application if offline or unconfigured.
     *
     * @param queryDoc The primary document being evaluated.
     */
    public static void runWebCheck(ProcessedDocument queryDoc) {
        System.out.println("\n" + TextUtils.bar(78, '='));
        System.out.println(TextUtils.bold("               OPTIONAL WEB-CHECK PLAGIARISM INSPECTION"));
        System.out.println(TextUtils.bar(78, '='));

        // 1. Check for API key and search client availability
        SearchClient client = SearchClientFactory.createClient();
        if (client == null) {
            System.out.println(TextUtils.yellow("[NOTICE] Web check skipped: Neither TAVILY_API_KEY nor SERPAPI_API_KEY found in environment variables."));
            System.out.println("         To enable web mode, set: export TAVILY_API_KEY=\"your_key\" or SERPAPI_API_KEY=\"your_key\"");
            System.out.println("         Falling back cleanly to local-only inspection.");
            System.out.println(TextUtils.bar(78, '='));
            return;
        }

        System.out.printf("  * Search Provider Active: %s\n", client.getServiceName());

        // 2. Select 5-8 distinctive queries using the Max-Heap PhraseSelector
        List<String> queryPhrases = PhraseSelector.selectTopPhrases(queryDoc, 6);
        if (queryPhrases.isEmpty()) {
            System.out.println("  [NOTICE] Insufficient distinctive text in document to generate web search queries.");
            return;
        }

        System.out.printf("  * Generated %d distinctive search queries via PriorityQueue Max-Heap:\n", queryPhrases.size());
        for (int i = 0; i < queryPhrases.size(); i++) {
            System.out.printf("    [%d] \"%s\"\n", i + 1, TextUtils.truncate(queryPhrases.get(i), 65));
        }

        // 3. Query search engine and deduplicate URLs
        System.out.println("\n  * Querying search engine for candidate sources...");
        Set<String> candidateUrls = new LinkedHashSet<>();
        for (String phrase : queryPhrases) {
            try {
                List<String> urls = client.searchUrls(phrase, 3);
                candidateUrls.addAll(urls);
            } catch (Exception e) {
                System.err.printf("  [WARNING] Search query failed for phrase: %s\n", e.getMessage());
            }
        }

        if (candidateUrls.isEmpty()) {
            System.out.println(TextUtils.yellow("  [NOTICE] No candidate web URLs returned by search engine. Proceeding in local mode."));
            return;
        }

        System.out.printf("  * Discovered %d unique candidate URLs across all queries.\n", candidateUrls.size());

        // 4. Fetch plain text from each candidate page
        System.out.println("  * Fetching and parsing web page contents...");
        Map<String, String> fetchedPages = PageFetcher.fetchAll(candidateUrls);

        if (fetchedPages.isEmpty()) {
            System.out.println(TextUtils.yellow("  [NOTICE] Could not fetch text from any candidate web pages (timeouts or access restrictions)."));
            return;
        }

        System.out.printf("  * Successfully fetched and extracted text from %d web pages.\n", fetchedPages.size());

        // 5. Evaluate each fetched page through the same Core Comparison Pipeline
        System.out.println("  * Running Core Comparison Pipeline (Hashing + KMP + Boyer-Moore + DP-LCS)...");
        List<WebMatchEntry> rankedEntries = new ArrayList<>();

        for (Map.Entry<String, String> entry : fetchedPages.entrySet()) {
            String url = entry.getKey();
            String pageText = entry.getValue();

            ProcessedDocument webDoc = TextPreprocessor.process(pageText, url);
            SimilarityReport report = SimilarityReport.generate(queryDoc, webDoc);
            rankedEntries.add(new WebMatchEntry(url, report));
        }

        // 6. Rank sources by final composite score in descending order
        rankedEntries.sort(Comparator.comparingDouble(WebMatchEntry::getScore).reversed());

        // 7. Render Web Plagiarism Leaderboard
        printWebReport(rankedEntries);
    }

    private static void printWebReport(List<WebMatchEntry> entries) {
        String subline = TextUtils.bar(78, '-');
        System.out.println("\n" + subline);
        System.out.println(TextUtils.bold("                  WEB SOURCES SIMILARITY LEADERBOARD"));
        System.out.println(subline);
        System.out.printf("  %-4s | %-38s | %-7s | %-7s | %-9s\n",
                "Rank", "Source Web URL", "Phrases", "LCS %", "Score %");
        System.out.println(subline);

        int rank = 1;
        for (WebMatchEntry entry : entries) {
            String url = TextUtils.truncate(entry.getUrl(), 38);
            int phrasesMatched = entry.getReport().getKmpResult().getMatchCount();
            double lcsPercent = entry.getReport().getLcsResult().getSimilarityPercentage();
            double totalScore = entry.getScore();

            String color = totalScore >= 60.0 ? TextUtils.RED : (totalScore >= 30.0 ? TextUtils.YELLOW : TextUtils.GREEN);

            System.out.printf("  #%-3d | %-38s | %-7d | %5.1f%%  | %s\n",
                    rank++,
                    url,
                    phrasesMatched,
                    lcsPercent,
                    TextUtils.color(color, String.format("%5.1f%%", totalScore)));
        }
        System.out.println(subline);

        if (!entries.isEmpty()) {
            WebMatchEntry top = entries.get(0);
            System.out.printf("  >> TOP MATCHING SOURCE: %s\n", top.getUrl());
            System.out.printf("     Composite Similarity: %.2f%% - %s\n",
                    top.getScore(), top.getReport().getAggregatedScore().getRiskLevel());
        }
        System.out.println(TextUtils.bar(78, '=') + "\n");
    }
}
