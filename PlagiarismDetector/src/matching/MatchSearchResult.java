package matching;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the results and execution performance metrics of a phrase matching algorithm.
 */
public class MatchSearchResult {
    private final String algorithmName;
    private final List<MatchRecord> matches;
    private final int phrasesTested;
    private final int uniquePhrasesMatched;
    private final long elapsedNanos;

    public MatchSearchResult(String algorithmName,
                             List<MatchRecord> matches,
                             int phrasesTested,
                             int uniquePhrasesMatched,
                             long elapsedNanos) {
        this.algorithmName = algorithmName;
        this.matches = matches != null ? Collections.unmodifiableList(matches) : Collections.emptyList();
        this.phrasesTested = phrasesTested;
        this.uniquePhrasesMatched = uniquePhrasesMatched;
        this.elapsedNanos = elapsedNanos;
    }

    public MatchSearchResult(String algorithmName, List<MatchRecord> matches, int phrasesTested, long elapsedNanos) {
        this(algorithmName, matches, phrasesTested, matches != null ? matches.size() : 0, elapsedNanos);
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public List<MatchRecord> getMatches() {
        return matches;
    }

    public int getMatchCount() {
        return matches.size();
    }

    public int getPhrasesTested() {
        return phrasesTested;
    }

    public int getUniquePhrasesMatched() {
        return uniquePhrasesMatched;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    public double getElapsedMillis() {
        return elapsedNanos / 1_000_000.0;
    }

    public double getElapsedMicros() {
        return elapsedNanos / 1_000.0;
    }
}
