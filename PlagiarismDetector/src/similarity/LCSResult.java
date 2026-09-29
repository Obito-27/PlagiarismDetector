package similarity;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the results of a word-level Longest Common Subsequence (LCS) calculation.
 */
public class LCSResult {
    private final int lcsLength;
    private final int doc1WordCount;
    private final int doc2WordCount;
    private final double similarityPercentage;
    private final List<String> matchedSubsequence;

    public LCSResult(int lcsLength,
                     int doc1WordCount,
                     int doc2WordCount,
                     double similarityPercentage,
                     List<String> matchedSubsequence) {
        this.lcsLength = lcsLength;
        this.doc1WordCount = doc1WordCount;
        this.doc2WordCount = doc2WordCount;
        this.similarityPercentage = similarityPercentage;
        this.matchedSubsequence = matchedSubsequence != null ?
                Collections.unmodifiableList(matchedSubsequence) : Collections.emptyList();
    }

    public int getLcsLength() {
        return lcsLength;
    }

    public int getDoc1WordCount() {
        return doc1WordCount;
    }

    public int getDoc2WordCount() {
        return doc2WordCount;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public List<String> getMatchedSubsequence() {
        return matchedSubsequence;
    }

    public String getReconstructedTextSnippet(int maxWords) {
        if (matchedSubsequence.isEmpty()) {
            return "[No common subsequence found]";
        }
        int limit = Math.min(matchedSubsequence.size(), maxWords);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(" ");
            sb.append(matchedSubsequence.get(i));
        }
        if (matchedSubsequence.size() > maxWords) {
            sb.append(String.format(" ... [%d more words]", matchedSubsequence.size() - maxWords));
        }
        return sb.toString();
    }
}
