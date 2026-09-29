package matching;

/**
 * Record holding details of a discovered phrase match in target text.
 * Stores both the query phrase evaluated from Document A and the matched snippet/offsets in Document B.
 */
public class MatchRecord {
    private final String queryPhrase;
    private final String matchedSnippet;
    private final int startOffset;
    private final int endOffset;
    private final int length;

    public MatchRecord(String queryPhrase, String matchedSnippet, int startOffset) {
        this.queryPhrase = queryPhrase;
        this.matchedSnippet = matchedSnippet != null ? matchedSnippet : "";
        this.startOffset = startOffset;
        this.length = this.matchedSnippet.length();
        this.endOffset = startOffset + this.length;
    }

    public MatchRecord(String snippet, int startOffset) {
        this(snippet, snippet, startOffset);
    }

    public String getQueryPhrase() {
        return queryPhrase;
    }

    public String getMatchedSnippet() {
        return matchedSnippet;
    }

    public String getPhrase() {
        return matchedSnippet;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return String.format("Match[pos=%d..%d, len=%d, snippet=\"%s\"]",
                startOffset, endOffset, length, matchedSnippet);
    }
}
