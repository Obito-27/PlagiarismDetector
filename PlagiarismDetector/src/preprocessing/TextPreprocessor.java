package preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * TextPreprocessor handles lexical normalization, sentence segmentation, word tokenization,
 * and stopword removal.
 *
 * Algorithms & Complexity:
 * - Sentence Tokenization: Regex boundary matching (?<=[.!?])\s+ in O(N) time.
 * - Word Tokenization: Regex non-alphanumeric splitting in O(N) time.
 * - Stopword Filtering: O(1) average hash lookup per token against HashSet<String>, total O(W) where W is word count.
 *
 * Overall Time Complexity: O(N) where N is the character length of the source document.
 * Overall Auxiliary Space Complexity: O(N) to store token lists and sentences.
 */
public class TextPreprocessor {

    /**
     * Standard set of high-frequency English stopwords that carry little discriminative value
     * for vocabulary overlap and query ranking.
     */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for",
            "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself",
            "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
            "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once",
            "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own", "same", "she",
            "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves",
            "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until",
            "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom",
            "why", "will", "with", "would", "you", "your", "yours", "yourself", "yourselves"
    ));

    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+|\\r?\\n+");
    private static final Pattern WORD_SPLIT_PATTERN = Pattern.compile("[^a-zA-Z0-9]+");

    /**
     * Processes raw text into a structured {@link ProcessedDocument}.
     *
     * @param rawText Source text as a string.
     * @param sourceIdentifier Label or file path identifier.
     * @return Fully populated {@link ProcessedDocument}.
     */
    public static ProcessedDocument process(String rawText, String sourceIdentifier) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return new ProcessedDocument(sourceIdentifier, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        // 1. Extract raw sentences
        List<String> sentences = extractSentences(rawText);

        // 2. Tokenize words (case-normalized, punctuation stripped)
        List<String> allTokens = extractTokens(rawText);

        // 3. Filter stopwords for frequency and hashing analysis
        List<String> filteredTokens = new ArrayList<>();
        for (String token : allTokens) {
            if (!isStopword(token)) {
                filteredTokens.add(token);
            }
        }

        return new ProcessedDocument(sourceIdentifier, rawText, sentences, allTokens, filteredTokens);
    }

    /**
     * Segments text into discrete sentences.
     *
     * Time Complexity: O(N)
     * Space Complexity: O(N)
     */
    public static List<String> extractSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = SENTENCE_SPLIT_PATTERN.split(text);
        List<String> sentences = new ArrayList<>(parts.length);
        for (String s : parts) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    /**
     * Splits text into normalized lowercase alphanumeric word tokens.
     *
     * Time Complexity: O(N)
     * Space Complexity: O(N)
     */
    public static List<String> extractTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] words = WORD_SPLIT_PATTERN.split(text);
        List<String> tokens = new ArrayList<>(words.length);
        for (String word : words) {
            String lower = word.toLowerCase().trim();
            if (!lower.isEmpty()) {
                tokens.add(lower);
            }
        }
        return tokens;
    }

    /**
     * Checks if a word is in the stopword dictionary.
     * Lookup is O(1) average time.
     */
    public static boolean isStopword(String word) {
        if (word == null) return false;
        return STOPWORDS.contains(word.toLowerCase());
    }

    public static Set<String> getStopwords() {
        return Collections.unmodifiableSet(STOPWORDS);
    }
}
