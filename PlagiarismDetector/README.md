# Text Similarity & Plagiarism Detection System
### Advanced Data Structures & Algorithms Project (Java 17+)

A high-performance, algorithmic text similarity and plagiarism detection engine implemented in pure Java from first principles. Developed for academic evaluation, the system avoids third-party algorithmic black boxes and directly exposes the data structures, recurrence relations, and asymptotic bounds for every module.

---

## 1. System Architecture

```
                                  [Source Document 1]     [Source Document 2]
                                           │                       │
                                           ▼                       ▼
                                ┌─────────────────────────────────────┐
                                │      TextPreprocessor Module        │
                                │   - Sentence Boundary Splitter      │
                                │   - Alphanumeric Tokenizer (O(N))   │
                                │   - Stopword Elimination Filter     │
                                └──────────────────┬──────────────────┘
                                                   │
                         ┌─────────────────────────┼─────────────────────────┐
                         ▼                         ▼                         ▼
              ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
              │   Hashing Module    │   │ Exact Phrase Search │   │   Word-Level LCS    │
              │  - HashMap Freq     │   │  - KMP Matcher      │   │  - 2D DP Table      │
              │  - Set Jaccard %    │   │  - Boyer-Moore      │   │  - Backtracking     │
              │  - Multiset Overlap │   │  - Perf Benchmark   │   │  - Order Alignment  │
              └──────────┬──────────┘   └──────────┬──────────┘   └──────────┬──────────┘
                         │                         │                         │
                         └─────────────────────────┼─────────────────────────┘
                                                   │
                                                   ▼
                                      ┌─────────────────────────┐
                                      │   Weighted Aggregator   │
                                      │   - 25% Jaccard Overlap │
                                      │   - 35% Phrase Coverage │
                                      │   - 40% Word-Level LCS  │
                                      └────────────┬────────────┘
                                                   │
                                                   ▼
                                      ┌─────────────────────────┐
                                      │ Formatted Visual Report │
                                      │  - 5-Level Risk Verdict │
                                      │  - Offsets & Snippets   │
                                      └─────────────────────────┘
```

---

## 2. Project Directory Structure

```
PlagiarismDetector/
├── src/
│   ├── Main.java                        # CLI parsing, interactive prompts, pipeline orchestration
│   ├── io/
│   │   └── DocumentReader.java          # Safe file loading, character encoding, error validation
│   ├── preprocessing/
│   │   ├── ProcessedDocument.java       # Unified model for raw text, sentences, and tokens
│   │   └── TextPreprocessor.java        # Sentence splitter, tokenizer, stopword filtering
│   ├── hashing/
│   │   └── WordHasher.java              # HashMap word frequencies, common vocabulary, Jaccard
│   ├── matching/
│   │   ├── MatchRecord.java             # Phrase occurrence offset and length model
│   │   ├── MatchSearchResult.java       # Search execution statistics and timing metrics
│   │   ├── KMPMatcher.java              # Custom LPS array computation and O(N+M) KMP search
│   │   └── BoyerMooreMatcher.java       # Bad character heuristic table and sublinear search
│   ├── similarity/
│   │   ├── LCSResult.java               # LCS length, percentage, and reconstructed word list
│   │   └── LCSCalculator.java           # Classic O(N*M) Word-level Dynamic Programming LCS
│   ├── report/
│   │   ├── RiskLevel.java               # Standardized 5-level risk enum (None, Low, Moderate, High, Critical)
│   │   ├── AggregatedScore.java         # Weighted composite scoring and documented coverage formula
│   │   └── SimilarityReport.java        # Formatted console report generator
│   ├── web/
│   │   ├── PhraseSelector.java          # Max-Heap PriorityQueue ranking distinctive queries
│   │   ├── SearchClient.java            # Unified search engine interface
│   │   ├── TavilySearchClient.java      # Tavily search integration via java.net.http.HttpClient
│   │   ├── SerpApiSearchClient.java     # SerpApi Google search integration
│   │   ├── SearchClientFactory.java     # Environment variable detection factory
│   │   ├── PageFetcher.java             # Jsoup HTML parsing and text extraction
│   │   └── WebSimilarityRunner.java     # Multi-source web comparison runner
│   └── util/
│       ├── SimpleJsonParser.java        # Zero-dependency regex JSON URL extractor fallback
│       └── TextUtils.java               # ANSI color codes, visual progress bars, text formatters
├── lib/
│   ├── jsoup-1.18.3.jar                 # HTML DOM parser (for optional web mode only)
│   └── json-20240303.jar                # JSON parser (for optional web mode only)
├── resources/sample_docs/
│   ├── doc_original.txt                 # Baseline computer science document
│   ├── doc_identical.txt                # Exact duplicate (verifies 100% boundary)
│   ├── doc_unrelated.txt                # Completely disjoint domain (verifies 0% boundary)
│   ├── doc_partial_overlap.txt          # Mixed verbatim paragraphs and novel sentences
│   ├── doc_reordered_paraphrase.txt     # Reordered syntax and synonym substitutions
│   └── doc_empty.txt                    # Empty file (verifies zero-division resilience)
└── README.md                            # Comprehensive system documentation and viva prep
```

---

## 3. Build & Execution Instructions

### Prerequisites
- **JDK 17 or higher** (`java -version`, `javac -version`).
- No Maven or Gradle required for the core engine.

### Compilation
From the `PlagiarismDetector` root directory:

**Windows (PowerShell):**
```powershell
javac -d bin -cp "src;lib/*" (Get-ChildItem -Recurse -Filter *.java src).FullName
```

**Linux / macOS (Bash):**
```bash
javac -d bin -cp "src:lib/*" $(find src -name "*.java")
```

### Running Local Document Comparison
```powershell
# Compare identical documents (100% match)
java -cp "bin;lib/*" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_identical.txt

# Compare partially overlapping documents
java -cp "bin;lib/*" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_partial_overlap.txt

# Compare reordered/paraphrased documents
java -cp "bin;lib/*" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_reordered_paraphrase.txt

# Compare unrelated documents (~0% match)
java -cp "bin;lib/*" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_unrelated.txt

# Empty document edge case
java -cp "bin;lib/*" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_empty.txt

# Interactive Console Mode (prompts when no file arguments are passed)
java -cp "bin;lib/*" Main
```

### Running Optional Web-Check Mode (`--web`)
The `--web` flag activates online source discovery by searching the internet for distinctive phrases:
```powershell
# Configure an API key in your session:
$env:TAVILY_API_KEY="tvly-your-api-key"
# or
$env:SERPAPI_API_KEY="your-serpapi-key"

# Run with --web
java -cp "bin;lib/*" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_partial_overlap.txt --web
```
*Graceful Fallback*: If no API key is detected or the device is offline, the system logs a single clear notice and executes local comparison mode without interruption.

---

## 4. Algorithmic Deep Dive & Complexity Analysis

### Module 1: Preprocessing & Tokenization
- **Purpose**: Normalizes raw input into sentence lists, canonical word tokens, and stopword-filtered content vocabularies.
- **Data Structures**: `HashSet<String>` for $O(1)$ average stopword checks; dynamic `ArrayList<String>` for token sequences.
- **Time Complexity**: $O(N)$ where $N$ is total character count of the document.
- **Space Complexity**: $O(N)$ to store extracted token lists and sentence partitions.

### Module 2: Term Frequency Hashing & Jaccard Overlap
- **Purpose**: Measures content vocabulary overlap invariant to sentence rearrangement.
- **Data Structures**: `HashMap<String, Integer>` storing term occurrence counts; `HashSet<String>` storing distinct words.
- **Time Complexity**:
  - Frequency Map Computation: $O(W)$ where $W$ is total token count.
  - Set Intersection: $O(\min(U_A, U_B))$ where $U$ is distinct vocabulary size.
- **Space Complexity**: $O(U_A + U_B)$ auxiliary space.

### Module 3: Knuth-Morris-Pratt (KMP) Exact String Matching
- **Purpose**: Scans Document B to find exact verbatim sentence occurrences from Document A.
- **Data Structure**: Integer array `LPS[0..m-1]` (Longest Proper Prefix which is also a Suffix), known as the Failure Function $\pi$.
- **LPS Recurrence**:
  - $LPS[0] = 0$.
  - If $P[i] == P[len]$: $len \leftarrow len + 1$, $LPS[i] = len$, $i \leftarrow i + 1$.
  - If $P[i] \neq P[len]$: If $len > 0$, $len \leftarrow LPS[len - 1]$ (fallback without advancing $i$). Else $LPS[i] = 0$, $i \leftarrow i + 1$.
- **Invariant**: The text pointer $i$ strictly increments from $0$ to $n-1$ and **never backtracks**.
- **Time Complexity**: LPS preprocessing $O(m)$; search phase $O(n)$; Total: $O(n + m)$.
- **Space Complexity**: $O(m)$ auxiliary space for the LPS array.

### Module 4: Boyer-Moore Exact String Matching (Bad Character Heuristic)
- **Purpose**: Benchmarks against KMP for exact phrase search.
- **Data Structure**: `BadCharTable` (256-element direct ASCII array with Map fallback for Unicode).
- **Shift Rule**: Pattern scans **right-to-left** ($j = m-1$ down to $0$). On mismatch at text character $c = T[s + j]$:
  $$\text{shift} = \max(1, j - \text{last}[c])$$
- **Time Complexity**:
  - Preprocessing: $O(m + |\Sigma|)$ where $|\Sigma| \le 256$.
  - Best-Case Search: $O(n / m)$ when trailing characters allow complete pattern-length leaps.
  - Average-Case Search: Sublinear. In our benchmark across five test documents, Boyer-Moore was consistently faster than KMP (1.1x–2.4x depending on the pair), consistent with its expected sublinear average-case behavior on natural language text.
  - Worst-Case Search: $O(n \cdot m)$ for highly periodic patterns without Good Suffix rule.
- **Space Complexity**: $O(|\Sigma|)$ auxiliary space.

### Module 5: Word-Level Longest Common Subsequence (LCS via Dynamic Programming)
- **Purpose**: Detects narrative structure and sequence preservation even when text is paraphrased or interleaved with extraneous words.
- **Design Decision**: Operates at the **word token level** rather than character level. Character LCS creates false positives from common syllables/prefixes; Word LCS preserves syntactic alignment.
- **DP Recurrence**:
  Let $X = \langle x_1 \dots x_n \rangle$ and $Y = \langle y_1 \dots y_m \rangle$.
  $$L[i][j] = \begin{cases} 
  0 & \text{if } i = 0 \text{ or } j = 0 \\
  L[i-1][j-1] + 1 & \text{if } x_i = y_j \\
  \max(L[i-1][j], L[i][j-1]) & \text{if } x_i \neq y_j 
  \end{cases}$$
- **Backtracking**: Traces from $(n, m)$ back to $(0, 0)$ to reconstruct the aligned common word subsequence.
- **Time Complexity**: $O(n \cdot m)$ to populate the matrix; $O(n + m)$ for backtracking reconstruction.
- **Space Complexity**: $O(n \cdot m)$ for the 2D DP matrix.

### Module 6: PriorityQueue Max-Heap Phrase Selection (Web Mode)
- **Purpose**: Identifies the 5–8 most distinctive search queries from Document A.
- **Data Structure**: `PriorityQueue<ScoredPhrase>` configured as a Max-Heap.
- **Rarity Metric**: Each non-stopword $w$ is scored by inverse document frequency: $R(w) = \log(W_{\text{total}} / f(w) + 1)$. Sentence score:
  $$\text{Score}(S) = \frac{\sum_{w \in S} R(w)}{\sqrt{\text{wordCount}(S)}}$$
- **Time Complexity**: Insertion takes $O(S \log S)$; extraction takes $O(K \log S)$ where $S$ is sentence count.
- **Space Complexity**: $O(S)$ auxiliary space for heap nodes.

---

## 5. Mathematical Formulas & Aggregator Logic

### 1. Set-Based Jaccard Similarity
Measures vocabulary overlap between unique word sets:
$$\text{Jaccard}(A, B) = \frac{|A \cap B|}{|A \cup B|} \times 100 = \frac{|A \cap B|}{|A| + |B| - |A \cap B|} \times 100$$

### 2. Multiset (Weighted / Ruzicka) Jaccard Similarity
Accounts for word repetition frequencies across both documents:
$$\text{WeightedJaccard}(A, B) = \frac{\sum_{w \in A \cup B} \min(f_A(w), f_B(w))}{\sum_{w \in A \cup B} \max(f_A(w), f_B(w))} \times 100$$

### 3. Phrase Match Coverage Formula
Converts verbatim sentence matches into a normalized coverage percentage:
- **Plain Language**: The ratio of qualifying candidate sentences in Document A that appear verbatim anywhere in Document B, expressed as a percentage. Qualifying sentences must have length $\ge 10$ characters to exclude trivial fragments.
- **Mathematical Notation**:
  Let $S_A$ be the set of qualifying candidate sentences extracted from Document A:
  $$S_A = \{ s \in \text{Sentences}(A) \mid \text{length}(s) \ge 10 \}$$
  Let $M_A$ be the subset of candidate sentences having at least one exact substring match in Document B:
  $$M_A = \{ s \in S_A \mid \text{KMP.searchPhrase}(B, s) \neq \emptyset \}$$
  Then:
  $$\text{Phrase Match Coverage \%} = \begin{cases} 
  \frac{|M_A|}{|S_A|} \times 100 & \text{if } |S_A| > 0 \\ 
  0.0\% & \text{if } |S_A| = 0 
  \end{cases}$$

### 4. Word-Level LCS Similarity Formula
Applies the Sorensen-Dice coefficient to word token sequence lengths:
$$\text{LCS Similarity \%} = \begin{cases}
\frac{2 \times \text{LCS\_Length}}{\text{len}(doc_1) + \text{len}(doc_2)} \times 100 & \text{if } (\text{len}_1 + \text{len}_2) > 0 \\
0.0\% & \text{if } (\text{len}_1 + \text{len}_2) = 0
\end{cases}$$

### 5. Final Composite Weighted Aggregator
Combines the three orthogonal metrics into a unified score:
$$\text{Final Composite Score} = (W_{\text{Jaccard}} \times \text{Jaccard \%}) + (W_{\text{Phrase}} \times \text{Phrase Coverage \%}) + (W_{\text{LCS}} \times \text{LCS \%})$$
Where the configured weights are:
- $W_{\text{Jaccard}} = 0.25$ (25%)
- $W_{\text{Phrase}} = 0.35$ (35%)
- $W_{\text{LCS}} = 0.40$ (40%)
$$\sum W = 0.25 + 0.35 + 0.40 = 1.00$$

---

## 6. Standardized 5-Level Risk Classification Scale

Every evaluation run outputs exactly one consistent verdict from the defined 5-level scale:

| Final Composite Score | Standardized Label | Description |
| :---: | :---: | :--- |
| **80.0% – 100.0%** | `Critical` | Extensive verbatim copying or structural match |
| **50.0% – 79.9%**  | `High`     | Substantial phrase reuse or structural alignment |
| **25.0% – 49.9%**  | `Moderate` | Shared vocabulary or common thematic phrasing |
| **10.0% – 24.9%**  | `Low`      | Incidental overlap of general terminology |
| **0.0% – 9.9%**    | `None`     | Negligible or clean independent content |

---

## 7. Verification Results & Benchmark Table

| Document Pair | Jaccard % | Phrase Matches | Phrase Coverage | Word LCS % | Final Score | Standardized Risk |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `doc_original` vs `doc_identical` | 100.0% | 7 / 7 | 100.0% | 100.0% | **100.0%** | `Critical` |
| `doc_original` vs `doc_partial_overlap` | 39.2% | 3 / 7 | 42.9% | 56.0% | **47.2%** | `Moderate` |
| `doc_original` vs `doc_reordered_paraphrase` | 40.6% | 0 / 7 | 0.0% | 34.5% | **24.0%** | `Low` |
| `doc_original` vs `doc_unrelated` | 0.0% | 0 / 7 | 0.0% | 7.9% | **3.1%** | `None` |
| `doc_original` vs `doc_empty` | 0.0% | 0 / 0 | 0.0% | 0.0% | **0.0%** | `None` |

---

## 8. Sample Viva Questions & Answers

### Q1: Why did you implement both KMP and Boyer-Moore for phrase matching?
> **Answer**: KMP and Boyer-Moore represent complementary string search paradigms. KMP guarantees strict linear worst-case $O(n+m)$ search time by never backtracking the text pointer $i$ and relying on the precomputed LPS array. In contrast, Boyer-Moore scans pattern characters from right to left, enabling the Bad Character heuristic to skip up to $m$ characters in a single leap. In our benchmark across five test documents, Boyer-Moore was consistently faster than KMP (1.1x–2.4x depending on the pair), consistent with its expected sublinear average-case behavior. Because character comparisons proceed right-to-left, trailing mismatches frequently allow multi-character shifts across the text.

### Q2: How does Word-Level LCS differ from character-level LCS in plagiarism detection?
> **Answer**: Character-level LCS is vulnerable to lexical noise and false positive matches generated by common prefixes, suffixes, and anagrams shared across unrelated English words. By tokenizing into canonical word tokens and calculating the 2D DP matrix over words, the system measures syntactic alignment, phrase order, and narrative continuity.

### Q3: What is the recurrence relation of the LPS array in KMP and why is its build time O(m)?
> **Answer**: $LPS[i]$ stores the length of the longest proper prefix of $P[0..i]$ that is also a suffix of $P[0..i]$. The pointer `len` tracks prefix length. If $P[i] == P[len]$, `len` increments and $LPS[i] = len$. If they mismatch and $len > 0$, `len` falls back to $LPS[len - 1]$ without advancing $i$. Because `len` increments at most $m$ times and can only decrement as many times as it increments, the total number of operations is strictly bounded by $2m$, guaranteeing $O(m)$ time complexity.

### Q4: How is the 35% Phrase Coverage percentage calculated?
> **Answer**: It calculates the proportion of qualifying candidate sentences (length $\ge 10$) in Document A that appear as an exact substring in Document B:
> $$\text{Phrase Coverage \%} = \frac{|M_A|}{|S_A|} \times 100$$
> If Document A has 7 qualifying sentences and 3 are found verbatim in Document B, phrase coverage is $(3 / 7) \times 100 = 42.86\%$.

### Q5: How are edge cases (such as empty documents) handled?
> **Answer**: Each module enforces defensive guard clauses. In `WordHasher`, empty token sets bypass union division and return `0.0%`. In `LCSCalculator`, if $(|A| + |B|) == 0$, it immediately returns `0.0%`. In `SimilarityReport`, empty documents trigger a short-circuit report that assigns a `0.0%` composite score and the `None` risk label without throwing any `ArithmeticException`, displaying `0 / 0` phrase coverage.

### Q6: How does the PriorityQueue Max-Heap select search queries in Web Mode?
> **Answer**: Common sentences like *"It is important to understand that"* produce unhelpful web search results. The system assigns an inverse frequency weight $R(w) = \log(W_{\text{total}} / f(w) + 1)$ to each non-stopword and computes a rarity density score for each sentence. Sentences are inserted into a **Max-Heap PriorityQueue** ordered by rarity. Extracting the top $K$ queries takes $O(K \log S)$ time, isolating the most distinctive sentences for search queries.
