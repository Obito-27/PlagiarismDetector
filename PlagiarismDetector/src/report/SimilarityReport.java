package report;

import hashing.WordHasher;
import matching.KMPMatcher;
import matching.BoyerMooreMatcher;
import matching.MatchRecord;
import matching.MatchSearchResult;
import preprocessing.ProcessedDocument;
import similarity.LCSCalculator;
import similarity.LCSResult;
import util.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Generates and prints a comprehensive, beautifully formatted comparison report to the console.
 */
public class SimilarityReport {

    private final ProcessedDocument docA;
    private final ProcessedDocument docB;
    private final WordHasher.HashingResult hashingResult;
    private final MatchSearchResult kmpResult;
    private final MatchSearchResult bmResult;
    private final LCSResult lcsResult;
    private final AggregatedScore aggregatedScore;

    public SimilarityReport(ProcessedDocument docA,
                            ProcessedDocument docB,
                            WordHasher.HashingResult hashingResult,
                            MatchSearchResult kmpResult,
                            MatchSearchResult bmResult,
                            LCSResult lcsResult,
                            AggregatedScore aggregatedScore) {
        this.docA = docA;
        this.docB = docB;
        this.hashingResult = hashingResult;
        this.kmpResult = kmpResult;
        this.bmResult = bmResult;
        this.lcsResult = lcsResult;
        this.aggregatedScore = aggregatedScore;
    }

    public ProcessedDocument getDocA() { return docA; }
    public ProcessedDocument getDocB() { return docB; }
    public WordHasher.HashingResult getHashingResult() { return hashingResult; }
    public MatchSearchResult getKmpResult() { return kmpResult; }
    public MatchSearchResult getBmResult() { return bmResult; }
    public LCSResult getLcsResult() { return lcsResult; }
    public AggregatedScore getAggregatedScore() { return aggregatedScore; }

    /**
     * Executes the end-to-end comparison between two processed documents.
     */
    public static SimilarityReport generate(ProcessedDocument docA, ProcessedDocument docB) {
        // 1. Edge Case: Empty documents short-circuit
        if (docA.isEmpty() || docB.isEmpty()) {
            WordHasher.HashingResult emptyHash = WordHasher.analyze(docA.getFilteredTokens(), docB.getFilteredTokens());
            MatchSearchResult emptyKmp = new MatchSearchResult("KMP (Knuth-Morris-Pratt)", Collections.emptyList(), 0, 0, 0);
            MatchSearchResult emptyBm = new MatchSearchResult("Boyer-Moore (Bad Character)", Collections.emptyList(), 0, 0, 0);
            LCSResult emptyLcs = new LCSResult(0, docA.getWordCount(), docB.getWordCount(), 0.0, Collections.emptyList());
            AggregatedScore zeroScore = new AggregatedScore(0.0, 0.0, 0.0);
            return new SimilarityReport(docA, docB, emptyHash, emptyKmp, emptyBm, emptyLcs, zeroScore);
        }

        // 2. Hashing & Jaccard Module (using stopword-filtered tokens for semantic accuracy)
        WordHasher.HashingResult hashingResult = WordHasher.analyze(
                docA.getFilteredTokens(),
                docB.getFilteredTokens()
        );

        // 3. Exact Phrase Matching (KMP and Boyer-Moore)
        // Extract qualifying candidate sentences S_A (length >= 10) once
        List<String> qualifyingPhrases = new ArrayList<>();
        for (String sentence : docA.getSentences()) {
            String trimmed = sentence.trim();
            if (trimmed.length() >= 10) {
                qualifyingPhrases.add(trimmed);
            }
        }

        // Single KMP search pass: evaluates each qualifying sentence exactly once
        MatchSearchResult kmpResult = KMPMatcher.evaluatePhrases(
                qualifyingPhrases,
                docB.getRawText(),
                true // Case-insensitive exact search
        );

        // Single Boyer-Moore search pass: evaluates the exact same candidate phrases
        MatchSearchResult bmResult = BoyerMooreMatcher.evaluatePhrases(
                qualifyingPhrases,
                docB.getRawText(),
                true // Case-insensitive exact search
        );

        // Derive matched count and coverage directly from kmpResult (no second search pass)
        int matchedPhrasesCount = kmpResult.getUniquePhrasesMatched();
        int totalPhrasesTested = kmpResult.getPhrasesTested();
        double phraseCoverage = AggregatedScore.calculatePhraseCoverage(matchedPhrasesCount, totalPhrasesTested);

        // 4. Word-Level LCS
        LCSResult lcsResult = LCSCalculator.compute(
                docA.getAllTokens(),
                docB.getAllTokens()
        );

        // 5. Aggregator (computes composite score and maps to 5-level risk scale)
        AggregatedScore aggregatedScore = new AggregatedScore(
                hashingResult.getSetJaccardPercentage(),
                phraseCoverage,
                lcsResult.getSimilarityPercentage()
        );

        return new SimilarityReport(docA, docB, hashingResult, kmpResult, bmResult, lcsResult, aggregatedScore);
    }

    /**
     * Renders the report to standard console output.
     */
    public void printConsole() {
        int width = 78;
        String line = TextUtils.bar(width, '=');
        String subline = TextUtils.bar(width, '-');

        System.out.println("\n" + line);
        System.out.println(TextUtils.bold("           TEXT SIMILARITY & PLAGIARISM DETECTION REPORT"));
        System.out.println(line);

        // Document Info
        System.out.println(TextUtils.bold("[1] DOCUMENTS ANALYZED:"));
        System.out.printf("  * Doc A (Query):  %-55s\n", docA.getSourceIdentifier());
        System.out.printf("    - Sentences: %-5d | Total Words: %-5d | Content Words: %d\n",
                docA.getSentenceCount(), docA.getWordCount(), docA.getFilteredWordCount());
        System.out.printf("  * Doc B (Target): %-55s\n", docB.getSourceIdentifier());
        System.out.printf("    - Sentences: %-5d | Total Words: %-5d | Content Words: %d\n",
                docB.getSentenceCount(), docB.getWordCount(), docB.getFilteredWordCount());

        if (docA.isEmpty() || docB.isEmpty()) {
            System.out.println("\n" + TextUtils.yellow("[NOTICE] One or both documents are empty. All similarity metrics evaluate to 0.0%."));
        }

        // Section 1: Hashing & Common Words
        System.out.println("\n" + subline);
        System.out.println(TextUtils.bold("[2] HASHING MODULE & VOCABULARY OVERLAP"));
        System.out.println(subline);
        System.out.printf("  * Unique Content Words (Doc A): %d\n", hashingResult.getUniqueWordsCountA());
        System.out.printf("  * Unique Content Words (Doc B): %d\n", hashingResult.getUniqueWordsCountB());
        System.out.printf("  * Common Vocabulary Set Size:   %d words\n", hashingResult.getCommonWordsCount());
        System.out.printf("  * Set Jaccard Similarity:       %.2f%%\n", hashingResult.getSetJaccardPercentage());
        System.out.printf("  * Multiset/Weighted Jaccard:    %.2f%%\n", hashingResult.getWeightedJaccardPercentage());

        // Sample common words
        Set<String> commonSet = hashingResult.getCommonWords();
        if (!commonSet.isEmpty()) {
            List<String> sortedCommon = new ArrayList<>(commonSet);
            Collections.sort(sortedCommon);
            int displayCount = Math.min(12, sortedCommon.size());
            System.out.print("  * Sample Common Words: [ ");
            for (int i = 0; i < displayCount; i++) {
                System.out.print(sortedCommon.get(i) + (i < displayCount - 1 ? ", " : ""));
            }
            if (sortedCommon.size() > displayCount) {
                System.out.printf(" ... +%d more", sortedCommon.size() - displayCount);
            }
            System.out.println(" ]");
        } else {
            System.out.println("  * Sample Common Words: [None]");
        }

        // Section 2: KMP vs Boyer-Moore Comparison
        System.out.println("\n" + subline);
        System.out.println(TextUtils.bold("[3] EXACT PHRASE MATCHING BENCHMARK (KMP vs BOYER-MOORE)"));
        System.out.println(subline);
        System.out.printf("  * Sentences / Phrases Tested:   %d\n", kmpResult.getPhrasesTested());
        System.out.printf("  * Distinct Phrases Matched:     %d\n", kmpResult.getUniquePhrasesMatched());
        System.out.printf("  * Phrase Match Coverage:        %d / %d (%.1f%%)\n",
                kmpResult.getUniquePhrasesMatched(),
                kmpResult.getPhrasesTested(),
                aggregatedScore.getPhraseMatchPercentage());
        System.out.printf("  * Matches Found (KMP):          %d (Time: %.3f ms / %.1f us)\n",
                kmpResult.getMatchCount(), kmpResult.getElapsedMillis(), kmpResult.getElapsedMicros());
        System.out.printf("  * Matches Found (Boyer-Moore):  %d (Time: %.3f ms / %.1f us)\n",
                bmResult.getMatchCount(), bmResult.getElapsedMillis(), bmResult.getElapsedMicros());

        // Speed comparison
        if (kmpResult.getElapsedNanos() > 0 && bmResult.getElapsedNanos() > 0) {
            double ratio = (double) kmpResult.getElapsedNanos() / Math.max(1, bmResult.getElapsedNanos());
            if (ratio >= 1.0) {
                System.out.printf("  * Performance: Boyer-Moore was %.2fx faster than KMP for this text.\n", ratio);
            } else {
                System.out.printf("  * Performance: KMP was %.2fx faster than Boyer-Moore for this text.\n", 1.0 / ratio);
            }
        }

        // List matched phrase snippets
        List<MatchRecord> matches = kmpResult.getMatches();
        if (!matches.isEmpty()) {
            System.out.println("  * Discovered Verbatim Phrases:");
            int showLimit = Math.min(4, matches.size());
            for (int i = 0; i < showLimit; i++) {
                MatchRecord m = matches.get(i);
                System.out.printf("    [%d] Offset [%4d..%4d] (len=%2d): \"%s\"\n",
                        i + 1, m.getStartOffset(), m.getEndOffset(), m.getLength(), TextUtils.truncate(m.getPhrase(), 60));
            }
            if (matches.size() > showLimit) {
                System.out.printf("    ... and %d more matching occurrences.\n", matches.size() - showLimit);
            }
        } else {
            System.out.println("  * Discovered Verbatim Phrases: [No exact sentence-level phrases found]");
        }

        // Section 3: Word-Level LCS
        System.out.println("\n" + subline);
        System.out.println(TextUtils.bold("[4] WORD-LEVEL LONGEST COMMON SUBSEQUENCE (LCS - DYNAMIC PROGRAMMING)"));
        System.out.println(subline);
        System.out.printf("  * Matrix Dimensions (N x M):    %d x %d\n", lcsResult.getDoc1WordCount(), lcsResult.getDoc2WordCount());
        System.out.printf("  * Longest Common Subsequence:   %d words\n", lcsResult.getLcsLength());
        System.out.printf("  * Word-Level LCS Similarity:    %.2f%%\n", lcsResult.getSimilarityPercentage());
        System.out.printf("  * Formula Applied:              (2 * %d) / (%d + %d) * 100\n",
                lcsResult.getLcsLength(), lcsResult.getDoc1WordCount(), lcsResult.getDoc2WordCount());
        System.out.println("  * Reconstructed Subsequence Snippet:");
        System.out.println("    \"" + lcsResult.getReconstructedTextSnippet(18) + "\"");

        // Section 4: Final Aggregated Verdict
        printFinalVerdict(line);
    }

    private void printFinalVerdict(String line) {
        String subline = TextUtils.bar(78, '-');
        System.out.println("\n" + line);
        System.out.println(TextUtils.bold("                     FINAL SIMILARITY ASSESSMENT"));
        System.out.println(line);

        System.out.printf("  1. Word Overlap (Jaccard) [Weight 25%%]:   %s\n",
                TextUtils.progressBar(aggregatedScore.getWordOverlapPercentage(), 20));
        System.out.printf("  2. Phrase Match Coverage  [Weight 35%%]:   %s\n",
                TextUtils.progressBar(aggregatedScore.getPhraseMatchPercentage(), 20));
        System.out.printf("  3. Word-Level LCS         [Weight 40%%]:   %s\n",
                TextUtils.progressBar(aggregatedScore.getLcsPercentage(), 20));

        System.out.println(subline);
        double finalScore = aggregatedScore.getFinalCompositeScore();
        RiskLevel risk = aggregatedScore.getRiskLevelEnum();

        String scoreColor;
        switch (risk) {
            case CRITICAL:
            case HIGH:
                scoreColor = TextUtils.RED;
                break;
            case MODERATE:
                scoreColor = TextUtils.YELLOW;
                break;
            case LOW:
            case NONE:
            default:
                scoreColor = TextUtils.GREEN;
                break;
        }

        System.out.printf("  " + TextUtils.bold(">> FINAL COMPOSITE SCORE:") + " %s\n",
                TextUtils.color(scoreColor, String.format("%s (%.2f%%)", TextUtils.progressBar(finalScore, 25), finalScore)));
        System.out.printf("  " + TextUtils.bold(">> RISK VERDICT:") + "          %s\n",
                TextUtils.color(scoreColor, aggregatedScore.getRiskLevel()));
        System.out.println(line + "\n");
    }
}
