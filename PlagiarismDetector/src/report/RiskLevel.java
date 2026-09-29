package report;

/**
 * Standardized 5-level plagiarism risk classification scale:
 * - Critical : score >= 80.0%
 * - High     : score >= 50.0% and < 80.0%
 * - Moderate : score >= 25.0% and < 50.0%
 * - Low      : score >= 10.0% and < 25.0%
 * - None     : score < 10.0%
 */
public enum RiskLevel {
    CRITICAL("Critical", "Extensive verbatim copying or structural match (score >= 80%)"),
    HIGH("High", "Substantial phrase reuse or structural alignment (score >= 50%)"),
    MODERATE("Moderate", "Shared vocabulary or common thematic phrasing (score >= 25%)"),
    LOW("Low", "Incidental overlap of general terminology (score >= 10%)"),
    NONE("None", "Negligible or clean independent content (score < 10%)");

    private final String label;
    private final String description;

    RiskLevel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static RiskLevel fromScore(double score) {
        if (score >= 80.0) {
            return CRITICAL;
        } else if (score >= 50.0) {
            return HIGH;
        } else if (score >= 25.0) {
            return MODERATE;
        } else if (score >= 10.0) {
            return LOW;
        } else {
            return NONE;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
