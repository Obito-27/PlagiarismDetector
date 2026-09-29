package similarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Word-Level Longest Common Subsequence (LCS) dynamic programming algorithm implemented from scratch.
 *
 * Algorithmic Concept:
 * Finds the longest subsequence of words that appears in both document sequences in the same relative order,
 * without requiring the words to occupy consecutive positions. Because it works at the WORD token level
 * rather than character level, it is immune to intra-word typographical noise and focuses strictly on
 * narrative and sentence structural preservation.
 *
 * Recurrence Relation:
 * Let X = <x_1, x_2, ..., x_n> and Y = <y_1, y_2, ..., y_m>.
 * Let L[i][j] be the length of the LCS of prefixes X[1..i] and Y[1..j].
 *
 * Base Cases:
 *   L[i][0] = 0  for all 0 <= i <= n
 *   L[0][j] = 0  for all 0 <= j <= m
 *
 * Recursive Step:
 *   If X[i] == Y[j]:
 *       L[i][j] = L[i-1][j-1] + 1
 *   Else:
 *       L[i][j] = max(L[i-1][j], L[i][j-1])
 *
 * Reconstructing Subsequence (Backtracking):
 * Starting at L[n][m], if X[i] == Y[j], include X[i] in the sequence and move to (i-1, j-1).
 * Otherwise, move in the direction of the strictly maximal neighbor: (i-1, j) or (i, j-1).
 *
 * Similarity Metric:
 *   Similarity % = (2 * LCS_length) / (len(doc1) + len(doc2)) * 100
 *   This formula (Sorensen-Dice coefficient applied to sequence lengths) guarantees 100% for identical
 *   sequences and 0% for completely disjoint sequences.
 *
 * Complexity Analysis:
 * - Time Complexity:
 *   DP Table Computation: O(n * m) where n is word count of doc1 and m is word count of doc2.
 *   Backtracking: O(n + m) to trace from (n, m) to (0, 0).
 *   Total Time: O(n * m).
 *
 * - Space Complexity:
 *   Auxiliary Space: O(n * m) for the integer DP table of size (n+1) x (m+1).
 */
public class LCSCalculator {

    /**
     * Computes the word-level Longest Common Subsequence between two lists of tokens,
     * reconstructs the aligned subsequence, and calculates the similarity percentage.
     *
     * @param tokensA Token sequence of document A.
     * @param tokensB Token sequence of document B.
     * @return {@link LCSResult} containing lengths, percentages, and reconstructed words.
     */
    public static LCSResult compute(List<String> tokensA, List<String> tokensB) {
        if (tokensA == null || tokensB == null || tokensA.isEmpty() || tokensB.isEmpty()) {
            int lenA = tokensA != null ? tokensA.size() : 0;
            int lenB = tokensB != null ? tokensB.size() : 0;
            return new LCSResult(0, lenA, lenB, 0.0, Collections.emptyList());
        }

        int n = tokensA.size();
        int m = tokensB.size();

        // 1. Allocate DP table of dimensions (n+1) x (m+1)
        int[][] dp = new int[n + 1][m + 1];

        // 2. Fill DP table iteratively bottom-up
        for (int i = 1; i <= n; i++) {
            String wordA = tokensA.get(i - 1);
            for (int j = 1; j <= m; j++) {
                String wordB = tokensB.get(j - 1);

                if (wordA.equals(wordB)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        int lcsLength = dp[n][m];

        // 3. Backtrack through DP table to reconstruct the actual common subsequence
        List<String> reconstructed = new ArrayList<>(lcsLength);
        int i = n;
        int j = m;

        while (i > 0 && j > 0) {
            String wordA = tokensA.get(i - 1);
            String wordB = tokensB.get(j - 1);

            if (wordA.equals(wordB)) {
                reconstructed.add(wordA);
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        // The backtracked sequence is collected in reverse order, reverse it back
        Collections.reverse(reconstructed);

        // 4. Compute similarity percentage: 2 * LCS / (len1 + len2) * 100
        double similarityPercentage = 0.0;
        int totalTokens = n + m;
        if (totalTokens > 0) {
            similarityPercentage = (2.0 * lcsLength / totalTokens) * 100.0;
        }

        return new LCSResult(lcsLength, n, m, similarityPercentage, reconstructed);
    }
}
