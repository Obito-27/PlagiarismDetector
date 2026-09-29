package hashing;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WordHasher provides term-frequency distribution analysis, common vocabulary set derivation,
 * and Jaccard-style similarity calculation using HashMaps and HashSets.
 *
 * Algorithms & Complexity:
 * - Frequency Map Computation:
 *   Iterates through list of W tokens. Each insertion/update into HashMap is O(1) average time.
 *   Total Time: O(W), Space: O(U) where U is the count of unique words.
 *
 * - Common Vocabulary Set (Set Intersection):
 *   Iterates over keys of smaller map. Set lookup is O(1) average time.
 *   Total Time: O(min(U_A, U_B)), Space: O(min(U_A, U_B)).
 *
 * - Set Jaccard Similarity:
 *   Jaccard = |A ∩ B| / |A ∪ B| = |A ∩ B| / (|A| + |B| - |A ∩ B|) * 100
 *   Time: O(U_A + U_B), Space: O(U_A + U_B).
 *
 * - Multiset (Weighted) Jaccard Similarity:
 *   Overlap = sum(min(fA(w), fB(w))) / sum(max(fA(w), fB(w))) * 100
 *   Time: O(U_A + U_B), Space: O(U_A + U_B).
 */
public class WordHasher {

    /**
     * Value container holding hashing and vocabulary overlap analysis results.
     */
    public static class HashingResult {
        private final Map<String, Integer> freqMapA;
        private final Map<String, Integer> freqMapB;
        private final Set<String> commonWords;
        private final double setJaccardPercentage;
        private final double weightedJaccardPercentage;

        public HashingResult(Map<String, Integer> freqMapA,
                             Map<String, Integer> freqMapB,
                             Set<String> commonWords,
                             double setJaccardPercentage,
                             double weightedJaccardPercentage) {
            this.freqMapA = Collections.unmodifiableMap(freqMapA);
            this.freqMapB = Collections.unmodifiableMap(freqMapB);
            this.commonWords = Collections.unmodifiableSet(commonWords);
            this.setJaccardPercentage = setJaccardPercentage;
            this.weightedJaccardPercentage = weightedJaccardPercentage;
        }

        public Map<String, Integer> getFreqMapA() { return freqMapA; }
        public Map<String, Integer> getFreqMapB() { return freqMapB; }
        public Set<String> getCommonWords() { return commonWords; }
        public double getSetJaccardPercentage() { return setJaccardPercentage; }
        public double getWeightedJaccardPercentage() { return weightedJaccardPercentage; }
        public int getUniqueWordsCountA() { return freqMapA.size(); }
        public int getUniqueWordsCountB() { return freqMapB.size(); }
        public int getCommonWordsCount() { return commonWords.size(); }
    }

    /**
     * Builds a word-frequency map (term occurrences) for a list of tokens.
     *
     * @param tokens Sequence of word tokens.
     * @return Map of token to frequency.
     */
    public static Map<String, Integer> computeFrequencyMap(List<String> tokens) {
        Map<String, Integer> freqMap = new HashMap<>();
        if (tokens == null) return freqMap;

        for (String token : tokens) {
            freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
        }
        return freqMap;
    }

    /**
     * Computes the set intersection of words occurring in both frequency maps.
     *
     * @param mapA Frequency map of document A.
     * @param mapB Frequency map of document B.
     * @return Set of common words.
     */
    public static Set<String> findCommonWords(Map<String, Integer> mapA, Map<String, Integer> mapB) {
        if (mapA == null || mapB == null || mapA.isEmpty() || mapB.isEmpty()) {
            return Collections.emptySet();
        }

        // Optimize iteration by traversing the smaller keyset
        Map<String, Integer> smaller = mapA.size() <= mapB.size() ? mapA : mapB;
        Map<String, Integer> larger = mapA.size() <= mapB.size() ? mapB : mapA;

        Set<String> common = new HashSet<>();
        for (String word : smaller.keySet()) {
            if (larger.containsKey(word)) {
                common.add(word);
            }
        }
        return common;
    }

    /**
     * Calculates the standard Jaccard Similarity coefficient between unique word sets:
     * J(A, B) = |A ∩ B| / |A ∪ B| * 100.
     *
     * @param mapA Frequency map of document A.
     * @param mapB Frequency map of document B.
     * @return Jaccard similarity percentage in range [0.0, 100.0].
     */
    public static double computeSetJaccard(Map<String, Integer> mapA, Map<String, Integer> mapB) {
        if (mapA == null || mapB == null || mapA.isEmpty() && mapB.isEmpty()) {
            return 0.0;
        }

        Set<String> setA = mapA.keySet();
        Set<String> setB = mapB.keySet();

        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }

        int intersectionCount = 0;
        for (String word : setA) {
            if (setB.contains(word)) {
                intersectionCount++;
            }
        }

        int unionCount = setA.size() + setB.size() - intersectionCount;
        if (unionCount == 0) return 0.0;

        return ((double) intersectionCount / unionCount) * 100.0;
    }

    /**
     * Calculates the Multiset (Weighted / Ruzicka) Jaccard Similarity coefficient:
     * Overlap = sum(min(fA, fB)) / sum(max(fA, fB)) * 100.
     *
     * This considers the actual repetition/frequency of words across both documents.
     */
    public static double computeWeightedJaccard(Map<String, Integer> mapA, Map<String, Integer> mapB) {
        if (mapA == null || mapB == null || (mapA.isEmpty() && mapB.isEmpty())) {
            return 0.0;
        }

        Set<String> allKeys = new HashSet<>(mapA.keySet());
        allKeys.addAll(mapB.keySet());

        if (allKeys.isEmpty()) return 0.0;

        long minSum = 0;
        long maxSum = 0;

        for (String word : allKeys) {
            int countA = mapA.getOrDefault(word, 0);
            int countB = mapB.getOrDefault(word, 0);

            minSum += Math.min(countA, countB);
            maxSum += Math.max(countA, countB);
        }

        if (maxSum == 0) return 0.0;

        return ((double) minSum / maxSum) * 100.0;
    }

    /**
     * Executes the full hashing and vocabulary overlap analysis pipeline.
     */
    public static HashingResult analyze(List<String> tokensA, List<String> tokensB) {
        Map<String, Integer> freqA = computeFrequencyMap(tokensA);
        Map<String, Integer> freqB = computeFrequencyMap(tokensB);

        Set<String> common = findCommonWords(freqA, freqB);
        double setJaccard = computeSetJaccard(freqA, freqB);
        double weightedJaccard = computeWeightedJaccard(freqA, freqB);

        return new HashingResult(freqA, freqB, common, setJaccard, weightedJaccard);
    }
}
