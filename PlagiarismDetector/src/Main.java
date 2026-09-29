import io.DocumentReader;
import preprocessing.ProcessedDocument;
import preprocessing.TextPreprocessor;
import report.SimilarityReport;
import util.TextUtils;
import web.WebSimilarityRunner;

import java.io.IOException;
import java.util.Scanner;

/**
 * Main Application Entrypoint for the Text Similarity and Plagiarism Detection System.
 * Supports command-line execution and an interactive console workflow.
 */
public class Main {

    public static void main(String[] args) {
        // Parse flags and arguments
        boolean webMode = false;
        boolean serverMode = false;
        String doc1Path = null;
        String doc2Path = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if ("--server".equalsIgnoreCase(arg) || "-s".equalsIgnoreCase(arg)) {
                serverMode = true;
            } else if ("--web".equalsIgnoreCase(arg) || "-w".equalsIgnoreCase(arg)) {
                webMode = true;
            } else if ("--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg)) {
                printHelp();
                return;
            } else if ("--no-color".equalsIgnoreCase(arg)) {
                TextUtils.setColorEnabled(false);
            } else if (doc1Path == null) {
                doc1Path = arg;
            } else if (doc2Path == null) {
                doc2Path = arg;
            }
        }

        if (serverMode) {
            web.WebServer.startServer(8080);
            return;
        }

        // Interactive prompt if arguments are missing
        if (doc1Path == null) {
            Scanner scanner = new Scanner(System.in);
            printBanner();
            System.out.println("No document arguments supplied.\n");
            System.out.println("Select Mode:");
            System.out.println("  [1] Launch Localhost Web UI Server (http://localhost:8080)");
            System.out.println("  [2] Run Console Document Comparison");
            System.out.print("Choice (default: 1): ");
            String modeChoice = scanner.nextLine().trim();

            if (modeChoice.isEmpty() || "1".equals(modeChoice)) {
                web.WebServer.startServer(8080);
                return;
            }

            System.out.print("Enter path to Document 1 (Query Document): ");
            doc1Path = scanner.nextLine().trim();

            System.out.print("Enable web plagiarism search? (y/n) [default: n]: ");
            String webAns = scanner.nextLine().trim();
            if (webAns.equalsIgnoreCase("y") || webAns.equalsIgnoreCase("yes")) {
                webMode = true;
            }

            System.out.print("Enter path to Document 2 (Target Document, or press Enter to skip if running web-only): ");
            doc2Path = scanner.nextLine().trim();
            if (doc2Path.isEmpty()) {
                doc2Path = null;
            }
        }

        if (doc1Path == null || doc1Path.isEmpty()) {
            System.err.println("Error: Document 1 path is required.");
            System.exit(1);
        }

        // Read and process Document 1
        ProcessedDocument doc1;
        try {
            String text1 = DocumentReader.readDocument(doc1Path);
            doc1 = TextPreprocessor.process(text1, doc1Path);
        } catch (IOException e) {
            System.err.println("Error loading Document 1: " + e.getMessage());
            System.exit(1);
            return;
        }

        // If Document 2 is provided, execute local document comparison
        if (doc2Path != null && !doc2Path.isEmpty()) {
            ProcessedDocument doc2;
            try {
                String text2 = DocumentReader.readDocument(doc2Path);
                doc2 = TextPreprocessor.process(text2, doc2Path);
            } catch (IOException e) {
                System.err.println("Error loading Document 2: " + e.getMessage());
                System.exit(1);
                return;
            }

            // Run core comparison pipeline and print formatted report
            SimilarityReport report = SimilarityReport.generate(doc1, doc2);
            report.printConsole();
        } else if (!webMode) {
            System.err.println("Error: Please provide a second document path or enable --web mode.");
            System.exit(1);
        }

        // Execute optional web search if requested
        if (webMode) {
            WebSimilarityRunner.runWebCheck(doc1);
        }
    }

    private static void printBanner() {
        System.out.println(TextUtils.bar(78, '='));
        System.out.println(TextUtils.bold("       TEXT SIMILARITY AND PLAGIARISM DETECTION SYSTEM (DSA CORE)"));
        System.out.println(TextUtils.bar(78, '='));
    }

    private static void printHelp() {
        printBanner();
        System.out.println("USAGE:");
        System.out.println("  java -cp \"bin;lib/*\" Main --server");
        System.out.println("  java -cp \"bin;lib/*\" Main <doc1> <doc2> [options]");
        System.out.println("  java -cp \"bin;lib/*\" Main <doc1> --web");
        System.out.println("  java -cp \"bin;lib/*\" Main (launches interactive prompt)\n");
        System.out.println("OPTIONS:");
        System.out.println("  --server, -s    Launch localhost web dashboard server (http://localhost:8080)");
        System.out.println("  --web, -w       Enable optional web-check mode (queries web search engine)");
        System.out.println("  --no-color      Disable ANSI color codes in console output");
        System.out.println("  --help, -h      Display this help menu\n");
        System.out.println("ENVIRONMENT VARIABLES (for --web mode):");
        System.out.println("  TAVILY_API_KEY   Tavily AI Search API key");
        System.out.println("  SERPAPI_API_KEY  SerpApi Google Search API key\n");
        System.out.println("EXAMPLES:");
        System.out.println("  java -cp \"bin;lib/*\" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_partial_overlap.txt");
        System.out.println("  java -cp \"bin;lib/*\" Main resources/sample_docs/doc_original.txt resources/sample_docs/doc_identical.txt --web");
        System.out.println(TextUtils.bar(78, '='));
    }
}
