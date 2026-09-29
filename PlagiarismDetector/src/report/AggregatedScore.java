package report;

/**
 * Aggregates multiple algorithmic similarity scores into a unified, weighted final score.
 *
 * Configurable weights:
 * - Word Overlap / Jaccard: 25% (W_WORD_OVERLAP = 0.25)
 * - Exact Phrase Match (KMP/BM Coverage): 35% (W_PHRASE_MATCH = 0.35)
 * - Longest Common Subsequence (Word LCS): 40% (W_LCS = 0.40)
 * Total = 1.00 (100%)
 *
 * Risk Verdict Scale:
 * Uses a standardized 5-level scale (None, Low, Moderate, High, Critical) defined in {@link RiskLevel}.
 */
public class AggregatedScore {

    public static final double WEIGHT_WORD_OVERLAP = 0.25;
    public static final double WEIGHT_PHRASE_MATCH = 0.35;
    public static final double WEIGHT_LCS = 0.40;

    private final double wordOverlapPercentage;
    private final double phraseMatchPercentage;
    private final double lcsPercentage;
    private final double finalCompositeScore;
    private final RiskLevel riskLevel;

    /**
     * Constructs an aggregated score and determines the standardized risk classification.
     *
     * @param wordOverlapPercentage Jaccard vocabulary overlap percentage [0.0, 100.0].
     * @param phraseMatchPercentage Verbatim phrase coverage percentage [0.0, 100.0].
     * @param lcsPercentage Word-level Longest Common Subsequence percentage [0.0, 100.0].
     */
    public AggregatedScore(double wordOverlapPercentage,
                           double phraseMatchPercentage,
                           double lcsPercentage) {
        this.wordOverlapPercentage = wordOverlapPercentage;
        this.phraseMatchPercentage = phraseMatchPercentage;
        this.lcsPercentage = lcsPercentage;

        double weighted = (WEIGHT_WORD_OVERLAP * wordOverlapPercentage)
                        + (WEIGHT_PHRASE_MATCH * phraseMatchPercentage)
                        + (WEIGHT_LCS * lcsPercentage);

        this.finalCompositeScore = Math.max(0.0, Math.min(100.0, weighted));
        this.riskLevel = RiskLevel.fromScore(this.finalCompositeScore);
    }

    /**
     * Calculates the Phrase Match Coverage percentage.
     *
     * Mathematical Definition:
     * Let S_A be the set of qualifying candidate sentences/phrases extracted from Document A
     * (filtering out trivial segments with character length < 10).
     * Let M_A be the subset of candidate phrases that have at least one exact substring match in Document B:
     *   M_A = { s in S_A | search(Text_B, s) != empty }
     *
     * Then:
     *   Phrase Match Coverage % = (|M_A| / |S_A|) * 100
     *
     * If |S_A| = 0 (e.g., empty file or no qualifying sentences), the coverage is defined as 0.0%.
     * The result is clamped to [0.0, 100.0].
     *
     * @param matchedPhrasesCount Count of unique candidate phrases from Document A found in Document B.
     * @param totalPhrasesTested Total count of qualifying candidate phrases tested.
     * @return Phrase coverage percentage in range [0.0, 100.0].
     */
    public static double calculatePhraseCoverage(int matchedPhrasesCount, int totalPhrasesTested) {
        if (totalPhrasesTested <= 0 || matchedPhrasesCount <= 0) {
            return 0.0;
        }
        return Math.min(100.0, ((double) matchedPhrasesCount / totalPhrasesTested) * 100.0);
    }

    public double getWordOverlapPercentage() { return wordOverlapPercentage; }
    public double getPhraseMatchPercentage() { return phraseMatchPercentage; }
    public double getLcsPercentage() { return lcsPercentage; }
    public double getFinalCompositeScore() { return finalCompositeScore; }

    /**
     * Returns the standardized risk classification label ("None", "Low", "Moderate", "High", or "Critical").
     */
    public String getRiskLevel() {
        return riskLevel.getLabel();
    }

    public RiskLevel getRiskLevelEnum() {
        return riskLevel;
    }

    public String getRiskDescription() {
        return riskLevel.getDescription();
    }
}
