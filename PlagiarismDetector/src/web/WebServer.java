package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import preprocessing.ProcessedDocument;
import preprocessing.TextPreprocessor;
import report.SimilarityReport;
import similarity.LCSResult;
import util.TextUtils;
import ai.AIDetector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WebServer {

    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        startServer(port);
    }

    public static WebServer startServer(int preferredPort) {
        int currentPort = preferredPort;
        WebServer webServer = null;
        while (currentPort < preferredPort + 10) {
            try {
                webServer = new WebServer(currentPort);
                webServer.start();
                System.out.println("\n" + TextUtils.bar(78, '='));
                System.out.println(TextUtils.bold("  🚀 PLAGIARISM DETECTOR LOCALHOST WEB SERVER STARTED SUCCESSFULLY!"));
                System.out.println(TextUtils.bar(78, '='));
                System.out.println("  🌐 Open in your browser: " + TextUtils.cyan("http://localhost:" + currentPort));
                System.out.println("  📊 Features: N-by-N Code Peer Comparison & AI Generation Detection");
                System.out.println(TextUtils.bar(78, '=') + "\n");
                return webServer;
            } catch (IOException e) {
                currentPort++;
            }
        }
        System.err.println("Could not start WebServer on any port between " + preferredPort + " and " + (preferredPort + 9));
        return null;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticPageHandler());
        server.createContext("/api/compare_matrix", new CompareMatrixHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static class StaticPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }
            sendResponse(exchange, 200, getDashboardHtml(), "text/html; charset=UTF-8");
        }
    }

    private static class CompareMatrixHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
                return;
            }

            try {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject req = new JSONObject(body);

                JSONArray filesArray = req.optJSONArray("files");
                boolean enableAi = req.optBoolean("enableAi", false);

                if (filesArray == null || filesArray.length() < 2) {
                    sendResponse(exchange, 400, "{\"error\":\"Upload at least 2 files.\"}", "application/json");
                    return;
                }

                // Store raw texts alongside processed docs
                List<ProcessedDocument> docs = new ArrayList<>();
                List<String> rawTexts = new ArrayList<>();
                JSONArray filesResult = new JSONArray();

                for (int i = 0; i < filesArray.length(); i++) {
                    JSONObject fileObj = filesArray.getJSONObject(i);
                    String text = fileObj.getString("text");
                    String name = fileObj.getString("name");

                    ProcessedDocument doc = TextPreprocessor.process(text, name);
                    docs.add(doc);
                    rawTexts.add(text);

                    JSONObject fRes = new JSONObject();
                    fRes.put("name", name);
                    fRes.put("wordCount", doc.getWordCount());
                    fRes.put("sizeBytes", text.length());

                    if (enableAi) {
                        AIDetector.AIDetectionResult aiRes = AIDetector.detect(text);
                        JSONObject aiJson = new JSONObject();
                        aiJson.put("probability", aiRes.probability);
                        aiJson.put("reason", aiRes.reason == null ? aiRes.error : aiRes.reason);
                        fRes.put("aiResult", aiJson);
                    }
                    filesResult.put(fRes);
                }

                JSONArray pairsResult = new JSONArray();

                // N x N pairwise comparison with full proof
                for (int i = 0; i < docs.size(); i++) {
                    for (int j = i + 1; j < docs.size(); j++) {
                        ProcessedDocument doc1 = docs.get(i);
                        ProcessedDocument doc2 = docs.get(j);

                        SimilarityReport report = SimilarityReport.generate(doc1, doc2);

                        JSONObject pRes = new JSONObject();
                        pRes.put("file1", doc1.getSourceIdentifier());
                        pRes.put("file2", doc2.getSourceIdentifier());
                        pRes.put("compositeScore", report.getAggregatedScore().getFinalCompositeScore());
                        pRes.put("wordOverlapScore", report.getAggregatedScore().getWordOverlapPercentage());
                        pRes.put("phraseMatchScore", report.getAggregatedScore().getPhraseMatchPercentage());
                        pRes.put("lcsScore", report.getAggregatedScore().getLcsPercentage());
                        pRes.put("riskLevel", report.getAggregatedScore().getRiskLevel().toString());

                        // ---- PROOF DATA ----

                        // 1. Common vocabulary words
                        Set<String> commonWords = report.getHashingResult().getCommonWords();
                        JSONArray cwArr = new JSONArray();
                        int cwCount = 0;
                        for (String w : commonWords) {
                            if (cwCount++ >= 50) break; // cap at 50
                            cwArr.put(w);
                        }
                        pRes.put("commonWords", cwArr);
                        pRes.put("commonWordsTotal", commonWords.size());

                        // 2. Matched phrases (KMP exact matches)
                        JSONArray phrasesArr = new JSONArray();
                        for (matching.MatchRecord rec : report.getKmpResult().getMatches()) {
                            JSONObject m = new JSONObject();
                            m.put("phrase", rec.getPhrase());
                            m.put("offset", rec.getStartOffset());
                            m.put("length", rec.getLength());
                            phrasesArr.put(m);
                        }
                        pRes.put("matchedPhrases", phrasesArr);

                        // 3. LCS matched subsequence
                        LCSResult lcs = report.getLcsResult();
                        pRes.put("lcsLength", lcs.getLcsLength());
                        String lcsSnippet = lcs.getReconstructedTextSnippet(80);
                        pRes.put("lcsSnippet", lcsSnippet);

                        // 4. Raw texts for side-by-side highlight
                        pRes.put("text1", rawTexts.get(i));
                        pRes.put("text2", rawTexts.get(j));

                        pairsResult.put(pRes);
                    }
                }

                JSONObject finalResponse = new JSONObject();
                finalResponse.put("files", filesResult);
                finalResponse.put("pairs", pairsResult);
                sendResponse(exchange, 200, finalResponse.toString(), "application/json");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getDashboardHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Student Code Analyzer</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-main: #0f172a;
            --bg-card: #1e293b;
            --bg-deep: #0f172a;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --accent-blue: #3b82f6;
            --accent-purple: #8b5cf6;
            --accent-green: #10b981;
            --accent-red: #ef4444;
            --accent-yellow: #f59e0b;
            --border: #334155;
            --font-mono: 'JetBrains Mono', monospace;
        }
        * { box-sizing: border-box; }
        body { margin: 0; font-family: 'Inter', sans-serif; background: var(--bg-main); color: var(--text-main); }

        header {
            background: var(--bg-card); border-bottom: 1px solid var(--border); padding: 20px 40px;
            display: flex; justify-content: space-between; align-items: center;
        }
        .container { max-width: 1300px; margin: 30px auto; padding: 0 20px; }
        .card {
            background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 24px;
            margin-bottom: 24px;
        }
        h2 { margin-top: 0; font-size: 18px; color: #e2e8f0; }

        .file-list { margin-top: 12px; max-height: 250px; overflow-y: auto; }
        .file-item {
            display: flex; justify-content: space-between; padding: 10px 14px;
            background: var(--bg-deep); border: 1px solid var(--border); border-radius: 6px; margin-bottom: 6px;
            font-family: var(--font-mono); font-size: 13px;
        }

        button {
            background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
            color: white; border: none; padding: 14px 24px; border-radius: 8px; font-size: 16px;
            font-weight: 600; cursor: pointer; transition: opacity 0.2s; width: 100%;
        }
        button:hover { opacity: 0.9; }
        button:disabled { opacity: 0.5; cursor: not-allowed; }

        table { width: 100%; border-collapse: collapse; margin-top: 12px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid var(--border); }
        th { color: var(--text-muted); font-size: 12px; font-weight: 600; text-transform: uppercase; }
        td { font-size: 14px; }

        .badge { padding: 5px 10px; border-radius: 6px; font-size: 12px; font-weight: 700; }
        .badge.high { background: rgba(239,68,68,0.2); color: #fca5a5; }
        .badge.medium { background: rgba(245,158,11,0.2); color: #fcd34d; }
        .badge.low { background: rgba(16,185,129,0.2); color: #6ee7b7; }

        .tabs { display: flex; gap: 10px; margin-bottom: 20px; }
        .tab {
            padding: 10px 20px; background: var(--bg-deep); border: 1px solid var(--border); border-radius: 6px;
            cursor: pointer; font-weight: 600; color: var(--text-muted); font-size: 14px;
        }
        .tab.active { background: rgba(59,130,246,0.1); border-color: var(--accent-blue); color: var(--accent-blue); }
        .tab-content { display: none; }
        .tab-content.active { display: block; }

        /* Proof Modal */
        .modal-overlay {
            display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0,0,0,0.7); z-index: 1000; justify-content: center; align-items: flex-start;
            padding-top: 40px; overflow-y: auto;
        }
        .modal-overlay.open { display: flex; }
        .modal {
            background: var(--bg-card); border: 1px solid var(--border); border-radius: 16px;
            width: 95%; max-width: 1200px; padding: 30px; position: relative; margin-bottom: 60px;
        }
        .modal-close {
            position: absolute; top: 16px; right: 20px; background: none; border: none;
            color: var(--text-muted); font-size: 28px; cursor: pointer; width: auto; padding: 4px 10px;
        }
        .proof-section { margin-bottom: 24px; }
        .proof-section h3 {
            font-size: 15px; color: var(--accent-blue); margin-bottom: 10px;
            border-bottom: 1px solid var(--border); padding-bottom: 8px;
        }
        .proof-tag {
            display: inline-block; background: rgba(59,130,246,0.15); color: #93c5fd;
            padding: 4px 10px; border-radius: 4px; margin: 3px; font-family: var(--font-mono); font-size: 12px;
        }
        .phrase-match {
            background: var(--bg-deep); border: 1px solid var(--border); border-radius: 8px;
            padding: 12px 16px; margin-bottom: 8px; font-family: var(--font-mono); font-size: 13px;
        }
        .phrase-match .offset { color: var(--text-muted); font-size: 11px; }
        .side-by-side { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .code-panel {
            background: var(--bg-deep); border: 1px solid var(--border); border-radius: 8px;
            padding: 16px; max-height: 400px; overflow: auto; font-family: var(--font-mono);
            font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all;
        }
        .code-panel .panel-title {
            font-family: 'Inter', sans-serif; font-size: 13px; font-weight: 700;
            color: var(--accent-purple); margin-bottom: 10px;
        }
        mark {
            background: rgba(245,158,11,0.3); color: #fcd34d; border-radius: 2px; padding: 0 2px;
        }
        .lcs-box {
            background: var(--bg-deep); border: 1px solid var(--border); border-radius: 8px;
            padding: 16px; font-family: var(--font-mono); font-size: 13px; line-height: 1.8;
            max-height: 200px; overflow: auto; color: #a5b4fc;
        }
        .pair-row { cursor: pointer; transition: background 0.15s; }
        .pair-row:hover { background: rgba(59,130,246,0.08); }

        @media (max-width: 900px) {
            .side-by-side { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <header>
        <div>
            <h1 style="margin:0; font-size:24px; background:linear-gradient(to right, #3b82f6, #8b5cf6); -webkit-background-clip:text; -webkit-text-fill-color:transparent;">
                Comprehensive Student Code Analyzer
            </h1>
            <p style="margin:4px 0 0 0; font-size:13px; color:var(--text-muted);">Peer-to-Peer Plagiarism Matrix &amp; AI Verification &middot; Click any pair to see proof</p>
        </div>
    </header>
    <div class="container">
        <div class="card">
            <h2>1. Upload All Student Code Files</h2>
            <p style="font-size:13px; color:var(--text-muted); margin-top:0;">Upload any code files (.java, .py, .cpp, .js, .c, .ts, .cs, etc). Every file is compared against every other file.</p>
            <input type="file" id="queryFiles" multiple>
            <div id="queryFileList" class="file-list"></div>
        </div>

        <div class="card" style="display:flex; align-items:center; gap:12px;">
            <input type="checkbox" id="enableAi" style="width:20px; height:20px;">
            <div>
                <h3 style="margin:0; font-size:16px;">Enable AI Generation Check (per file)</h3>
                <p style="margin:4px 0 0 0; font-size:13px; color:var(--text-muted);">Requires GEMINI_API_KEY environment variable.</p>
            </div>
        </div>

        <button id="runBtn" onclick="runAnalysis()">⚡ Run Comprehensive Analysis</button>

        <div id="resultsCard" class="card" style="display:none; margin-top:30px;">
            <div class="tabs">
                <div class="tab active" onclick="switchTab('pairsTab', this)">Peer-to-Peer Matches</div>
                <div class="tab" onclick="switchTab('filesTab', this)">Individual File Analysis (AI)</div>
            </div>

            <div id="pairsTab" class="tab-content active">
                <p style="font-size:13px; color:var(--text-muted); margin-top:0;">🔍 Click any row to view <strong>detailed proof</strong> of where similarity was found.</p>
                <table>
                    <thead>
                        <tr>
                            <th>File 1</th>
                            <th>File 2</th>
                            <th>Similarity</th>
                            <th>Word / Phrase / LCS</th>
                            <th>Risk</th>
                        </tr>
                    </thead>
                    <tbody id="pairsBody"></tbody>
                </table>
            </div>

            <div id="filesTab" class="tab-content">
                <table>
                    <thead>
                        <tr>
                            <th>File Name</th>
                            <th>Word Count</th>
                            <th>Size</th>
                            <th>AI Generation Probability</th>
                        </tr>
                    </thead>
                    <tbody id="filesBody"></tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- Proof Modal -->
    <div class="modal-overlay" id="proofModal">
        <div class="modal">
            <button class="modal-close" onclick="closeProof()">&times;</button>
            <h2 id="proofTitle" style="margin-bottom:4px;"></h2>
            <p id="proofSubtitle" style="font-size:13px; color:var(--text-muted); margin-top:0;"></p>

            <div class="proof-section">
                <h3>📝 Exact Phrase Matches (KMP Algorithm)</h3>
                <div id="proofPhrases"></div>
            </div>

            <div class="proof-section">
                <h3>🔗 Common Vocabulary Tokens (Hashing)</h3>
                <div id="proofCommonWords"></div>
            </div>

            <div class="proof-section">
                <h3>🧬 Longest Common Subsequence (DP)</h3>
                <div class="lcs-box" id="proofLcs"></div>
            </div>

            <div class="proof-section">
                <h3>📄 Side-by-Side Code (Matched Phrases Highlighted)</h3>
                <div class="side-by-side">
                    <div class="code-panel">
                        <div class="panel-title" id="panelTitle1"></div>
                        <div id="codePanel1"></div>
                    </div>
                    <div class="code-panel">
                        <div class="panel-title" id="panelTitle2"></div>
                        <div id="codePanel2"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        let fileDataList = [];
        let pairsData = [];

        document.getElementById('queryFiles').addEventListener('change', async (e) => {
            fileDataList = [];
            const listDiv = document.getElementById('queryFileList');
            listDiv.innerHTML = '';
            for (let i = 0; i < e.target.files.length; i++) {
                const file = e.target.files[i];
                const text = await file.text();
                fileDataList.push({ name: file.name, text: text });
                listDiv.innerHTML += `<div class="file-item"><span>📝 ${file.name}</span><span>${(file.size/1024).toFixed(1)} KB</span></div>`;
            }
        });

        function switchTab(tabId, el) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            el.classList.add('active');
            document.getElementById(tabId).classList.add('active');
        }

        async function runAnalysis() {
            if (fileDataList.length < 2) return alert('Upload at least 2 files for peer comparison.');
            const btn = document.getElementById('runBtn');
            btn.disabled = true;
            btn.textContent = '⏳ Analyzing ' + fileDataList.length + ' files... Please wait';

            try {
                const res = await fetch('/api/compare_matrix', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        files: fileDataList,
                        enableAi: document.getElementById('enableAi').checked
                    })
                });
                const data = await res.json();
                if (data.error) throw new Error(data.error);
                pairsData = data.pairs;
                renderResults(data);
            } catch(e) {
                alert('Error: ' + e.message);
            } finally {
                btn.disabled = false;
                btn.textContent = '⚡ Run Comprehensive Analysis';
            }
        }

        function renderResults(data) {
            data.pairs.sort((a, b) => b.compositeScore - a.compositeScore);
            const pairsBody = document.getElementById('pairsBody');
            pairsBody.innerHTML = '';
            data.pairs.forEach((p, idx) => {
                const riskClass = p.riskLevel === 'HIGH' ? 'high' : p.riskLevel === 'MODERATE' ? 'medium' : 'low';
                pairsBody.innerHTML += `
                    <tr class="pair-row" onclick="showProof(${idx})">
                        <td style="font-family:var(--font-mono)">${p.file1}</td>
                        <td style="font-family:var(--font-mono)">${p.file2}</td>
                        <td><strong style="color:var(--accent-blue); font-size:16px;">${p.compositeScore.toFixed(1)}%</strong></td>
                        <td style="font-size:12px; color:var(--text-muted);">
                            W:${p.wordOverlapScore.toFixed(0)}% P:${p.phraseMatchScore.toFixed(0)}% LCS:${p.lcsScore.toFixed(0)}%
                        </td>
                        <td><span class="badge ${riskClass}">${p.riskLevel}</span></td>
                    </tr>
                `;
            });

            const filesBody = document.getElementById('filesBody');
            filesBody.innerHTML = '';
            data.files.forEach(f => {
                let aiDisplay = '<span style="color:#64748b">Not Checked</span>';
                if (f.aiResult) {
                    const hi = f.aiResult.probability > 70;
                    aiDisplay = `<div title="${f.aiResult.reason}" style="color:${hi ? '#ef4444' : '#10b981'}; font-weight:600;">${f.aiResult.probability}% <small style="color:var(--text-muted)">ℹ️</small></div>`;
                }
                filesBody.innerHTML += `
                    <tr>
                        <td style="font-family:var(--font-mono)">${f.name}</td>
                        <td>${f.wordCount} words</td>
                        <td>${(f.sizeBytes/1024).toFixed(1)} KB</td>
                        <td>${aiDisplay}</td>
                    </tr>
                `;
            });

            document.getElementById('resultsCard').style.display = 'block';
            window.scrollTo({ top: document.getElementById('resultsCard').offsetTop, behavior: 'smooth' });
        }

        function escapeHtml(text) {
            const d = document.createElement('div');
            d.textContent = text;
            return d.innerHTML;
        }

        function highlightPhrases(text, phrases) {
            if (!phrases || phrases.length === 0) return escapeHtml(text);

            // Build list of [start, end] intervals
            let intervals = [];
            phrases.forEach(p => {
                if (p.offset >= 0 && p.offset < text.length) {
                    intervals.push([p.offset, p.offset + p.length]);
                }
            });
            // Sort by start position
            intervals.sort((a, b) => a[0] - b[0]);

            // Merge overlapping intervals
            let merged = [];
            for (let iv of intervals) {
                if (merged.length > 0 && iv[0] <= merged[merged.length-1][1]) {
                    merged[merged.length-1][1] = Math.max(merged[merged.length-1][1], iv[1]);
                } else {
                    merged.push([...iv]);
                }
            }

            // Build highlighted HTML
            let result = '';
            let lastEnd = 0;
            for (let [s, e] of merged) {
                result += escapeHtml(text.substring(lastEnd, s));
                result += '<mark>' + escapeHtml(text.substring(s, e)) + '</mark>';
                lastEnd = e;
            }
            result += escapeHtml(text.substring(lastEnd));
            return result;
        }

        function showProof(idx) {
            const p = pairsData[idx];
            document.getElementById('proofTitle').textContent = p.file1 + '  ↔  ' + p.file2;
            document.getElementById('proofSubtitle').textContent =
                `Composite: ${p.compositeScore.toFixed(1)}% | Word Overlap: ${p.wordOverlapScore.toFixed(1)}% | Phrase Match: ${p.phraseMatchScore.toFixed(1)}% | LCS: ${p.lcsScore.toFixed(1)}% | Risk: ${p.riskLevel}`;

            // Matched Phrases
            const phrasesDiv = document.getElementById('proofPhrases');
            if (p.matchedPhrases && p.matchedPhrases.length > 0) {
                phrasesDiv.innerHTML = p.matchedPhrases.map(m =>
                    `<div class="phrase-match"><code>${escapeHtml(m.phrase)}</code> <span class="offset">@ offset ${m.offset}, ${m.length} chars</span></div>`
                ).join('');
            } else {
                phrasesDiv.innerHTML = '<p style="color:var(--text-muted); font-size:13px;">No exact phrase matches found.</p>';
            }

            // Common Words
            const cwDiv = document.getElementById('proofCommonWords');
            if (p.commonWords && p.commonWords.length > 0) {
                cwDiv.innerHTML = `<p style="font-size:12px; color:var(--text-muted); margin-bottom:8px;">${p.commonWordsTotal} common tokens found (showing up to 50)</p>` +
                    p.commonWords.map(w => `<span class="proof-tag">${escapeHtml(w)}</span>`).join('');
            } else {
                cwDiv.innerHTML = '<p style="color:var(--text-muted); font-size:13px;">No common vocabulary found.</p>';
            }

            // LCS
            const lcsDiv = document.getElementById('proofLcs');
            if (p.lcsSnippet) {
                lcsDiv.textContent = p.lcsSnippet;
            } else {
                lcsDiv.textContent = 'No common subsequence found.';
            }

            // Side-by-side with highlights
            document.getElementById('panelTitle1').textContent = p.file1;
            document.getElementById('panelTitle2').textContent = p.file2;
            document.getElementById('codePanel1').innerHTML = highlightPhrases(p.text1 || '', p.matchedPhrases || []);
            document.getElementById('codePanel2').innerHTML = escapeHtml(p.text2 || '');

            document.getElementById('proofModal').classList.add('open');
            document.body.style.overflow = 'hidden';
        }

        function closeProof() {
            document.getElementById('proofModal').classList.remove('open');
            document.body.style.overflow = '';
        }

        document.getElementById('proofModal').addEventListener('click', function(e) {
            if (e.target === this) closeProof();
        });
    </script>
</body>
</html>
""";
    }
}
