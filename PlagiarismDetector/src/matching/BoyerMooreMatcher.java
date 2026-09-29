package matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boyer-Moore string search algorithm utilizing the Bad Character Heuristic implemented from scratch.
 *
 * Algorithmic Concept:
 * Unlike KMP which reads pattern characters left-to-right, Boyer-Moore performs character
 * comparisons from right-to-left within the search window. If a mismatch occurs at text character C,
 * the pattern can be shifted forward so that the rightmost occurrence of C in the pattern aligns
 * with C in the text. If C does not occur in the pattern, the entire pattern can skip past C.
 *
 * Data Structure:
 * - Bad Character Table (Shift Table):
 *   Stores the 0-based rightmost index of every character occurring within the pattern.
 *   Implemented as an extended ASCII array of size 256 with a Map fallback for arbitrary Unicode characters.
 *
 * Complexity Analysis:
 * - Preprocessing Phase:
 *   Time Complexity: O(m + |Σ|) where m is pattern length and |Σ| is alphabet size.
 *   Auxiliary Space Complexity: O(|Σ|) to store the shift table.
 *
 * - Search Phase:
 *   Best Case Time: O(n / m). Occurs when characters at the end of the pattern window never appear
 *   in the pattern, allowing complete jumps of length m at every step.
 *   Average Case Time: Sublinear in practice on natural language text.
 *   Worst Case Time: O(n * m) without the good suffix heuristic (e.g., highly repetitive texts like "AAAA" in "AAAAAAAA").
 */
public class BoyerMooreMatcher {

    private static final int ALPHABET_SIZE = 256;

    /**
     * Internal container holding both ASCII fast lookup table and Unicode overflow map.
     */
    public static class BadCharTable {
        private final int[] asciiTable;
        private final Map<Character, Integer> unicodeMap;

        public BadCharTable(String pattern) {
            this.asciiTable = new int[ALPHABET_SIZE];
            Arrays.fill(asciiTable, -1);
            this.unicodeMap = new HashMap<>();

            int m = pattern.length();
            for (int i = 0; i < m; i++) {
                char ch = pattern.charAt(i);
                if (ch < ALPHABET_SIZE) {
                    asciiTable[ch] = i;
                } else {
                    unicodeMap.put(ch, i);
                }
            }
        }

        public int getLastOccurrence(char ch) {
            if (ch < ALPHABET_SIZE) {
                return asciiTable[ch];
            }
            return unicodeMap.getOrDefault(ch, -1);
        }
    }

    /**
     * Builds the Bad Character table for the given pattern.
     */
    public static BadCharTable buildBadCharTable(String pattern) {
        return new BadCharTable(pattern);
    }

    /**
     * Searches for all occurrences of pattern in text using the Boyer-Moore Bad Character rule.
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
        BadCharTable badChar = buildBadCharTable(pattern);

        List<Integer> occurrences = new ArrayList<>();
        int s = 0; // Alignment shift of the pattern with respect to text

        while (s <= (n - m)) {
            int j = m - 1;

            // Compare pattern with text from right to left
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }

            if (j < 0) {
                // Match found at shift s
                occurrences.add(s);

                // Shift pattern forward
                if (s + m < n) {
                    char nextChar = text.charAt(s + m);
                    s += m - badChar.getLastOccurrence(nextChar);
                } else {
                    s += 1;
                }
            } else {
                // Mismatch occurred at pattern[j] and text[s + j]
                char mismatchChar = text.charAt(s + j);
                int lastOccur = badChar.getLastOccurrence(mismatchChar);
                s += Math.max(1, j - lastOccur);
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
     * Evaluates candidate phrases against target text in a single pass.
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
        return new MatchSearchResult("Boyer-Moore (Bad Character)", allMatches, phrasesTested, uniqueMatchedCount, elapsedNano);
    }
}
