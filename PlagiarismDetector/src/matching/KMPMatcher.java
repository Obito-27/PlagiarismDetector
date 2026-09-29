package matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Knuth-Morris-Pratt (KMP) exact string matching algorithm implemented from scratch.
 *
 * Algorithmic Concept:
 * The KMP algorithm avoids the O(n * m) worst-case comparison time of naive string search
 * by utilizing information gathered during previous character comparisons. When a mismatch occurs,
 * the pattern itself contains enough knowledge to determine where the next potential match could begin,
 * eliminating the need to backtrack the target text pointer.
 *
 * Data Structure:
 * - LPS Array (Longest Proper Prefix which is also Suffix, also known as the Failure Function pi).
 *   For pattern P of length m, LPS[i] stores the length of the longest proper prefix of P[0..i]
 *   that is identical to a suffix of P[0..i].
 *
 * Complexity Analysis:
 * - LPS Array Preprocessing:
 *   Time Complexity: O(m) where m is pattern length. The pointer `len` increases at most m times
 *   and decreases at most m times during the loop.
 *   Auxiliary Space Complexity: O(m) to store the integer array of size m.
 *
 * - Search Phase:
 *   Time Complexity: O(n) where n is text length. The text index `i` is strictly monotonically
 *   increasing from 0 to n-1 and never decrements. Pattern index `j` shifts in O(1) amortized steps.
 *   Auxiliary Space Complexity: O(1) beyond storing matching indices.
 *
 * Overall Time: O(n + m)
 * Overall Space: O(m)
 */
public class KMPMatcher {

    /**
     * Precomputes the Longest Proper Prefix that is also a Suffix (LPS) array.
     *
     * @param pattern Query pattern of length m.
     * @return int[] LPS array of size m.
     */
    public static int[] computeLPS(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new int[0];
        }

        int m = pattern.length();
        int[] lps = new int[m];
        lps[0] = 0; // Proper prefix of length 1 cannot be equal to itself

        int len = 0; // Length of previous longest prefix-suffix
        int i = 1;

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    /**
     * Searches for all occurrences of pattern in text using KMP.
     *
     * @param text Target document string of length n.
     * @param pattern Query pattern string of length m.
     * @return List of 0-based starting character offsets where pattern appears in text.
     */
    public static List<Integer> search(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty() || text.length() < pattern.length()) {
            return Collections.emptyList();
        }

        int n = text.length();
        int m = pattern.length();
        int[] lps = computeLPS(pattern);

        List<Integer> occurrences = new ArrayList<>();
        int i = 0; // Text index
        int j = 0; // Pattern index

        while (i < n) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }

            if (j == m) {
                occurrences.add(i - j);
                j = lps[j - 1];
            } else if (i < n && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return occurrences;
    }

    /**
     * Searches for occurrences with optional case-insensitivity.
     */
    public static List<MatchRecord> searchPhrase(String text, String pattern, boolean caseInsensitive) {
        if (text == null || pattern == null || pattern.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String searchIn = caseInsensitive ? text.toLowerCase() : text;
        String searchFor = caseInsensitive ? pattern.toLowerCase() : pattern;

        List<Integer> offsets = search(searchIn, searchFor);
        List<MatchRecord> records = new ArrayList<>(offsets.size());
        for (int offset : offsets) {
            String snippet = text.substring(offset, offset + pattern.length());
            records.add(new MatchRecord(pattern, snippet, offset));
        }
        return records;
    }

    /**
     * Evaluates a collection of qualifying candidate sentences against target text in a single pass.
     * Tracks both total qualifying candidate sentences tested and how many unique candidate sentences matched.
     *
     * @param candidatePhrases Qualifying candidate sentences S_A (length >= 10).
     * @param targetText Raw text of Document B.
     * @param caseInsensitive Whether matching is case-insensitive.
     * @return MatchSearchResult containing all matches, phrasesTested, uniquePhrasesMatched, and timing.
     */
    public static MatchSearchResult evaluatePhrases(List<String> candidatePhrases, String targetText, boolean caseInsensitive) {
        long startNano = System.nanoTime();

        List<MatchRecord> allMatches = new ArrayList<>();
        int phrasesTested = 0;
        int uniqueMatchedCount = 0;

        if (candidatePhrases != null && targetText != null && !targetText.isEmpty()) {
            for (String phrase : candidatePhrases) {
                String trimmed = phrase.trim();
                if (trimmed.length() >= 10) {
                    phrasesTested++;
                    List<MatchRecord> matches = searchPhrase(targetText, trimmed, caseInsensitive);
                    if (!matches.isEmpty()) {
                        uniqueMatchedCount++;
                        allMatches.addAll(matches);
                    }
                }
            }
        }

        long elapsedNano = System.nanoTime() - startNano;
        return new MatchSearchResult("KMP (Knuth-Morris-Pratt)", allMatches, phrasesTested, uniqueMatchedCount, elapsedNano);
    }
}
