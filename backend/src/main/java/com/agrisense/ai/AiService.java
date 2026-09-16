package com.agrisense.ai;

import java.util.List;

/** NFR-6: business logic phụ thuộc interface này, đổi provider không sửa controller. */
public interface AiService {

    record ReadingPoint(String type, double value, String recordedAt) {}

    /** FR-7: nhận xét sức khỏe cây trồng từ readings 24h (tiếng Việt). */
    String analyzeHealth(String cropType, List<ReadingPoint> last24h);

    /** FR-8: giải thích 1 alert + cách khắc phục (tiếng Việt). */
    String explainAlert(String sensorType, double value, double threshold, String minOrMax);
}
