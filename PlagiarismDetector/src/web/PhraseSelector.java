package web;

import hashing.WordHasher;
import preprocessing.ProcessedDocument;
import preprocessing.TextPreprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Selects the most distinctive, query-worthy phrases from a document using a Max-Heap PriorityQueue.
 *
 * Algorithmic Concept:
 * Common phrases (e.g. "it is important to note that") yield noisy web results.
 * By computing term rarity from the document word-frequency map, sentences containing rare,
 * content-rich non-stopwords are assigned higher rarity weights.
 *
 * Data Structure:
 * - PriorityQueue (Max-Heap):
 *   Candidate phrases are prioritized using a Max-Heap ordered by their rarity score.
 *   Retrieving the top K queries takes O(K log S) where S is the total sentence count.
 *
 * Complexity Analysis:
 * - Scoring: O(S * L) where S is sentences and L is average sentence length.
 * - Heap Operations: O(S log S) to insert, O(K log S) to extract top K.
 * - Total Time Complexity: O(S log S).
 * - Auxiliary Space Complexity: O(S) to store candidate heap nodes.
 */
public class PhraseSelector {

    public static class ScoredPhrase {
        private final String phrase;
        private final double score;

        public ScoredPhrase(String phrase, double score) {
            this.phrase = phrase;
            this.score = score;
        }

        public String getPhrase() {
            return phrase;
        }

        public double getScore() {
            return score;
        }
    }

    /**
     * Extracts the top K most distinctive sentences/phrases from a processed document.
     *
     * @param doc ProcessedDocument to select queries from.
     * @param topK Number of queries to return (typically 5 to 8).
     * @return List of selected phrase strings.
     */
    public static List<String> selectTopPhrases(ProcessedDocument doc, int topK) {
        if (doc == null || doc.isEmpty() || topK <= 0) {
            return Collections.emptyList();
        }

        // 1. Compute term frequency map for rarity weighting
        Map<String, Integer> freqMap = WordHasher.computeFrequencyMap(doc.getFilteredTokens());
        int totalFilteredWords = doc.getFilteredWordCount();

        // 2. Max-Heap PriorityQueue: sorts ScoredPhrase by score in descending order
        PriorityQueue<ScoredPhrase> maxHeap = new PriorityQueue<>(
                Comparator.comparingDouble(ScoredPhrase::getScore).reversed()
        );

        // 3. Score each candidate sentence
        for (String sentence : doc.getSentences()) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 25) {
                continue; // Skip very short phrases
            }

            List<String> words = TextPreprocessor.extractTokens(trimmed);
            int nonStopwordCount = 0;
            double scoreSum = 0.0;

            for (String word : words) {
                if (!TextPreprocessor.isStopword(word)) {
                    nonStopwordCount++;
                    int freq = freqMap.getOrDefault(word, 1);
                    // Rarity metric: inverse term frequency normalized against vocabulary size
                    double rarity = Math.log((double) totalFilteredWords / freq + 1.0);
                    scoreSum += rarity;
                }
            }

            // Require at least 3 content words to qualify as a distinctive search query
            if (nonStopwordCount >= 3) {
                // Score normalized by square root of length to avoid penalizing moderately long sentences
                double finalScore = scoreSum / Math.sqrt(nonStopwordCount);
                maxHeap.offer(new ScoredPhrase(trimmed, finalScore));
            }
        }

        // 4. Extract top K phrases from Max-Heap
        List<String> topPhrases = new ArrayList<>();
        int count = 0;
        while (!maxHeap.isEmpty() && count < topK) {
            ScoredPhrase sp = maxHeap.poll();
            topPhrases.add(sp.getPhrase());
            count++;
        }

        return topPhrases;
    }
}
