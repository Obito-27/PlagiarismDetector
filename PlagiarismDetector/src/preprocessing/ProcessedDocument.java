package preprocessing;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the processed representations of a document for various algorithmic operations.
 *
 * Stores:
 * - rawText: Unmodified source text (mandatory for exact substring matchers like KMP & Boyer-Moore)
 * - sentences: List of raw sentences extracted via boundary detection
 * - allTokens: All lower-cased word tokens (used for word-level LCS alignment)
 * - filteredTokens: Tokens excluding common English stopwords (used for term frequency hashing & Jaccard overlap)
 *
 * Space Complexity: O(N) where N is total number of characters/tokens in the document.
 */
public class ProcessedDocument {

    private final String sourceIdentifier;
    private final String rawText;
    private final List<String> sentences;
    private final List<String> allTokens;
    private final List<String> filteredTokens;

    public ProcessedDocument(String sourceIdentifier,
                             String rawText,
                             List<String> sentences,
                             List<String> allTokens,
                             List<String> filteredTokens) {
        this.sourceIdentifier = sourceIdentifier;
        this.rawText = rawText != null ? rawText : "";
        this.sentences = sentences != null ? Collections.unmodifiableList(sentences) : Collections.emptyList();
        this.allTokens = allTokens != null ? Collections.unmodifiableList(allTokens) : Collections.emptyList();
        this.filteredTokens = filteredTokens != null ? Collections.unmodifiableList(filteredTokens) : Collections.emptyList();
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public String getRawText() {
        return rawText;
    }

    public List<String> getSentences() {
        return sentences;
    }

    public List<String> getAllTokens() {
        return allTokens;
    }

    public List<String> getFilteredTokens() {
        return filteredTokens;
    }

    public int getWordCount() {
        return allTokens.size();
    }

    public int getFilteredWordCount() {
        return filteredTokens.size();
    }

    public int getSentenceCount() {
        return sentences.size();
    }

    public boolean isEmpty() {
        return rawText.trim().isEmpty() || allTokens.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ProcessedDocument[source='%s', chars=%d, sentences=%d, words=%d, filteredWords=%d]",
                sourceIdentifier, rawText.length(), sentences.size(), allTokens.size(), filteredTokens.size());
    }
}
