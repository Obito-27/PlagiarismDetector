package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import preprocessing.ProcessedDocument;
import preprocessing.TextPreprocessor;
import report.SimilarityReport;
import util.TextUtils;
import ai.AIDetector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            String html = getDashboardHtml();
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
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
                    sendResponse(exchange, 400, "{\"error\":\"Please upload at least 2 files for peer comparison.\"}", "application/json");
                    return;
                }

                List<ProcessedDocument> docs = new ArrayList<>();
                JSONArray filesResult = new JSONArray();

                // 1. Process all documents & run AI detection
                for (int i = 0; i < filesArray.length(); i++) {
                    JSONObject fileObj = filesArray.getJSONObject(i);
                    String text = fileObj.getString("text");
                    String name = fileObj.getString("name");

                    ProcessedDocument doc = TextPreprocessor.process(text, name);
                    docs.add(doc);

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

                // 2. Pairwise N x N comparison
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

                        pairsResult.put(pRes);
                    }
                }

                JSONObject finalResponse = new JSONObject();
                finalResponse.put("files", filesResult);
                finalResponse.put("pairs", pairsResult);

                sendResponse(exchange, 200, finalResponse.toString(), "application/json");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}", "application/json");
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
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Student Code Analyzer</title>\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;700&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        :root {\n" +
                "            --bg-main: #0f172a;\n" +
                "            --bg-card: #1e293b;\n" +
                "            --text-main: #f8fafc;\n" +
                "            --text-muted: #94a3b8;\n" +
                "            --accent-blue: #3b82f6;\n" +
                "            --accent-purple: #8b5cf6;\n" +
                "            --border: #334155;\n" +
                "        }\n" +
                "        body {\n" +
                "            margin: 0; font-family: 'Inter', sans-serif; background: var(--bg-main); color: var(--text-main);\n" +
                "        }\n" +
                "        header {\n" +
                "            background: var(--bg-card); border-bottom: 1px solid var(--border); padding: 20px 40px;\n" +
                "            display: flex; justify-content: space-between; align-items: center;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 1200px; margin: 40px auto; padding: 0 20px;\n" +
                "        }\n" +
                "        .card {\n" +
                "            background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 24px;\n" +
                "            margin-bottom: 24px;\n" +
                "        }\n" +
                "        h2 { margin-top: 0; font-size: 18px; color: #e2e8f0; }\n" +
                "        .file-list { margin-top: 12px; max-height: 250px; overflow-y: auto; }\n" +
                "        .file-item {\n" +
                "            display: flex; justify-content: space-between; padding: 10px 14px;\n" +
                "            background: #0f172a; border: 1px solid var(--border); border-radius: 6px; margin-bottom: 8px;\n" +
                "            font-family: 'JetBrains Mono', monospace; font-size: 13px;\n" +
                "        }\n" +
                "        button {\n" +
                "            background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));\n" +
                "            color: white; border: none; padding: 14px 24px; border-radius: 8px; font-size: 16px;\n" +
                "            font-weight: 600; cursor: pointer; transition: opacity 0.2s; width: 100%;\n" +
                "        }\n" +
                "        button:hover { opacity: 0.9; }\n" +
                "        button:disabled { opacity: 0.5; cursor: not-allowed; }\n" +
                "        table {\n" +
                "            width: 100%; border-collapse: collapse; margin-top: 12px;\n" +
                "        }\n" +
                "        th, td { padding: 14px; text-align: left; border-bottom: 1px solid var(--border); }\n" +
                "        th { color: var(--text-muted); font-size: 13px; font-weight: 600; text-transform: uppercase; }\n" +
                "        .badge {\n" +
                "            padding: 6px 10px; border-radius: 6px; font-size: 12px; font-weight: 700;\n" +
                "        }\n" +
                "        .badge.high { background: rgba(239,68,68,0.2); color: #fca5a5; }\n" +
                "        .badge.medium { background: rgba(245,158,11,0.2); color: #fcd34d; }\n" +
                "        .badge.low { background: rgba(16,185,129,0.2); color: #6ee7b7; }\n" +
                "        .tabs {\n" +
                "            display: flex; gap: 10px; margin-bottom: 20px;\n" +
                "        }\n" +
                "        .tab {\n" +
                "            padding: 10px 20px; background: #0f172a; border: 1px solid var(--border); border-radius: 6px;\n" +
                "            cursor: pointer; font-weight: 600; color: var(--text-muted);\n" +
                "        }\n" +
                "        .tab.active {\n" +
                "            background: rgba(59,130,246,0.1); border-color: var(--accent-blue); color: var(--accent-blue);\n" +
                "        }\n" +
                "        .tab-content { display: none; }\n" +
                "        .tab-content.active { display: block; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <div>\n" +
                "            <h1 style=\"margin:0; font-size:24px; background:linear-gradient(to right, #3b82f6, #8b5cf6); -webkit-background-clip:text; -webkit-text-fill-color:transparent;\">\n" +
                "                Comprehensive Student Code Analyzer\n" +
                "            </h1>\n" +
                "            <p style=\"margin:4px 0 0 0; font-size:13px; color:var(--text-muted);\">Peer-to-Peer Plagiarism Matrix & AI Verification</p>\n" +
                "        </div>\n" +
                "    </header>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"card\">\n" +
                "            <h2>1. Upload All Student Code Files</h2>\n" +
                "            <p style=\"font-size:13px; color:var(--text-muted); margin-top:0;\">Upload all Java, Python, C++, or JS files at once. The system will cross-check every file against every other file.</p>\n" +
                "            <input type=\"file\" id=\"queryFiles\" multiple>\n" +
                "            <div id=\"queryFileList\" class=\"file-list\"></div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"card\" style=\"display:flex; align-items:center; gap:12px;\">\n" +
                "            <input type=\"checkbox\" id=\"enableAi\" style=\"width:20px; height:20px;\">\n" +
                "            <div>\n" +
                "                <h3 style=\"margin:0; font-size:16px;\">Enable AI Generation Check (per file)</h3>\n" +
                "                <p style=\"margin:4px 0 0 0; font-size:13px; color:var(--text-muted);\">Requires GEMINI_API_KEY environment variable.</p>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <button id=\"runBtn\" onclick=\"runAnalysis()\">⚡ Run Comprehensive Analysis</button>\n" +
                "\n" +
                "        <div id=\"resultsCard\" class=\"card\" style=\"display:none; margin-top:30px;\">\n" +
                "            <div class=\"tabs\">\n" +
                "                <div class=\"tab active\" onclick=\"switchTab('pairsTab', this)\">Peer-to-Peer Matches (Cheating)</div>\n" +
                "                <div class=\"tab\" onclick=\"switchTab('filesTab', this)\">Individual File Analysis (AI)</div>\n" +
                "            </div>\n" +
                "\n" +
                "            <div id=\"pairsTab\" class=\"tab-content active\">\n" +
                "                <table id=\"pairsTable\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>File 1</th>\n" +
                "                            <th>File 2</th>\n" +
                "                            <th>Similarity (Composite)</th>\n" +
                "                            <th>Word / Phrase / LCS</th>\n" +
                "                            <th>Risk</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"pairsBody\"></tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +
                "\n" +
                "            <div id=\"filesTab\" class=\"tab-content\">\n" +
                "                <table id=\"filesTable\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>File Name</th>\n" +
                "                            <th>Code Word Count</th>\n" +
                "                            <th>File Size</th>\n" +
                "                            <th>AI Generation Probability</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"filesBody\"></tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let fileDataList = [];\n" +
                "\n" +
                "        document.getElementById('queryFiles').addEventListener('change', async (e) => {\n" +
                "            fileDataList = [];\n" +
                "            const listDiv = document.getElementById('queryFileList');\n" +
                "            listDiv.innerHTML = '';\n" +
                "            for (let i = 0; i < e.target.files.length; i++) {\n" +
                "                const file = e.target.files[i];\n" +
                "                const text = await file.text();\n" +
                "                fileDataList.push({ name: file.name, text: text });\n" +
                "                listDiv.innerHTML += `<div class=\"file-item\"><span>📝 ${file.name}</span><span>${(file.size/1024).toFixed(1)} KB</span></div>`;\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        function switchTab(tabId, el) {\n" +
                "            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));\n" +
                "            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));\n" +
                "            el.classList.add('active');\n" +
                "            document.getElementById(tabId).classList.add('active');\n" +
                "        }\n" +
                "\n" +
                "        async function runAnalysis() {\n" +
                "            if (fileDataList.length < 2) return alert('Please upload at least 2 files for peer comparison.');\n" +
                "\n" +
                "            const btn = document.getElementById('runBtn');\n" +
                "            btn.disabled = true;\n" +
                "            btn.textContent = '⏳ Analyzing N x N matrix... Please wait';\n" +
                "\n" +
                "            try {\n" +
                "                const res = await fetch('/api/compare_matrix', {\n" +
                "                    method: 'POST',\n" +
                "                    headers: { 'Content-Type': 'application/json' },\n" +
                "                    body: JSON.stringify({\n" +
                "                        files: fileDataList,\n" +
                "                        enableAi: document.getElementById('enableAi').checked\n" +
                "                    })\n" +
                "                });\n" +
                "\n" +
                "                const data = await res.json();\n" +
                "                if (data.error) throw new Error(data.error);\n" +
                "                \n" +
                "                renderResults(data);\n" +
                "            } catch(e) {\n" +
                "                alert('Error: ' + e.message);\n" +
                "            } finally {\n" +
                "                btn.disabled = false;\n" +
                "                btn.textContent = '⚡ Run Comprehensive Analysis';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function renderResults(data) {\n" +
                "            // Render Pairs (Sorted by Similarity)\n" +
                "            data.pairs.sort((a, b) => b.compositeScore - a.compositeScore);\n" +
                "            const pairsBody = document.getElementById('pairsBody');\n" +
                "            pairsBody.innerHTML = '';\n" +
                "            data.pairs.forEach(p => {\n" +
                "                const riskClass = p.riskLevel === 'HIGH' ? 'high' : p.riskLevel === 'MODERATE' ? 'medium' : 'low';\n" +
                "                pairsBody.innerHTML += `\n" +
                "                    <tr>\n" +
                "                        <td style=\"font-family:'JetBrains Mono',monospace\">${p.file1}</td>\n" +
                "                        <td style=\"font-family:'JetBrains Mono',monospace\">${p.file2}</td>\n" +
                "                        <td><strong style=\"color:var(--accent-blue); font-size: 16px;\">${p.compositeScore.toFixed(1)}%</strong></td>\n" +
                "                        <td style=\"font-size:12px; color:var(--text-muted);\">\n" +
                "                            W: ${p.wordOverlapScore.toFixed(0)}% | P: ${p.phraseMatchScore.toFixed(0)}% | LCS: ${p.lcsScore.toFixed(0)}%\n" +
                "                        </td>\n" +
                "                        <td><span class=\"badge ${riskClass}\">${p.riskLevel}</span></td>\n" +
                "                    </tr>\n" +
                "                `;\n" +
                "            });\n" +
                "\n" +
                "            // Render Individual Files\n" +
                "            const filesBody = document.getElementById('filesBody');\n" +
                "            filesBody.innerHTML = '';\n" +
                "            data.files.forEach(f => {\n" +
                "                let aiDisplay = '<span style=\"color:#64748b\">Not Checked</span>';\n" +
                "                if (f.aiResult) {\n" +
                "                    const isHighAi = f.aiResult.probability > 70;\n" +
                "                    aiDisplay = `<div title=\"${f.aiResult.reason}\" style=\"color:${isHighAi ? '#ef4444' : '#10b981'}; font-weight:600;\">\n" +
                "                        ${f.aiResult.probability}% <small style=\"color:var(--text-muted)\">ℹ️</small>\n" +
                "                    </div>`;\n" +
                "                }\n" +
                "                filesBody.innerHTML += `\n" +
                "                    <tr>\n" +
                "                        <td style=\"font-family:'JetBrains Mono',monospace\">${f.name}</td>\n" +
                "                        <td>${f.wordCount} words</td>\n" +
                "                        <td>${(f.sizeBytes/1024).toFixed(1)} KB</td>\n" +
                "                        <td>${aiDisplay}</td>\n" +
                "                    </tr>\n" +
                "                `;\n" +
                "            });\n" +
                "\n" +
                "            document.getElementById('resultsCard').style.display = 'block';\n" +
                "            window.scrollTo({ top: document.getElementById('resultsCard').offsetTop, behavior: 'smooth' });\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
