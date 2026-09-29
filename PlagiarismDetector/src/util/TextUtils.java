package util;

/**
 * Text and terminal display utilities for clean console output.
 */
public class TextUtils {

    // ANSI Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private static boolean colorEnabled = true;

    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }

    public static String color(String colorCode, String text) {
        if (!colorEnabled) {
            return text;
        }
        return colorCode + text + RESET;
    }

    public static String bold(String text) {
        return color(BOLD, text);
    }

    public static String red(String text) {
        return color(RED, text);
    }

    public static String green(String text) {
        return color(GREEN, text);
    }

    public static String yellow(String text) {
        return color(YELLOW, text);
    }

    public static String cyan(String text) {
        return color(CYAN, text);
    }

    public static String bar(int width, char ch) {
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static String progressBar(double percentage, int barLength) {
        int filled = (int) Math.round((percentage / 100.0) * barLength);
        filled = Math.max(0, Math.min(barLength, filled));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                sb.append("=");
            } else if (i == filled && filled > 0 && filled < barLength) {
                sb.append(">");
            } else {
                sb.append(" ");
            }
        }
        sb.append(String.format("] %5.1f%%", percentage));
        return sb.toString();
    }
}
