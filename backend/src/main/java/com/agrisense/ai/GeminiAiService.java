package com.agrisense.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FR-7/8: gọi Google Gemini REST (server-side, khóa trong env — NFR-4).
 * Nếu thiếu API key → trả stub để demo chạy không cần key.
 * ponytail: gọi đồng bộ, không streaming. Nâng: SSE streaming (FR-9.4).
 */
@Service
public class GeminiAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);
    private static final String MODEL = "gemini-flash-latest";

    private final String apiKey;
    private final RestClient http = RestClient.create();

    public GeminiAiService(@Value("${agrisense.ai.gemini-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    private boolean hasKey() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String analyzeHealth(String cropType, List<ReadingPoint> last24h) {
        if (last24h.isEmpty()) return "Chưa đủ dữ liệu 24h để phân tích. Hãy chạy replay/live trước.";

        String summary = last24h.stream()
                .collect(Collectors.groupingBy(ReadingPoint::type,
                        Collectors.summarizingDouble(ReadingPoint::value)))
                .entrySet().stream()
                .map(e -> String.format("%s: min=%.1f max=%.1f avg=%.1f (n=%d)",
                        e.getKey(), e.getValue().getMin(), e.getValue().getMax(),
                        e.getValue().getAverage(), e.getValue().getCount()))
                .collect(Collectors.joining("\n"));

        String prompt = """
                Bạn là chuyên gia nông nghiệp. Dựa trên dữ liệu cảm biến 24h của nông trại trồng "%s":
                %s
                Hãy đưa ra: (1) nhận xét sức khỏe cây trồng, (2) 2-3 gợi ý hành động cụ thể.
                Trả lời ngắn gọn bằng tiếng Việt, giọng thân thiện cho nông dân.
                """.formatted(cropType == null ? "không rõ" : cropType, summary);

        return hasKey() ? callGemini(prompt)
                : "[Demo không có API key] Tóm tắt 24h:\n" + summary
                  + "\n\nGợi ý mẫu: theo dõi độ ẩm đất; nếu giảm dưới ngưỡng, tưới trước 8h sáng.";
    }

    @Override
    public String explainAlert(String sensorType, double value, double threshold, String minOrMax) {
        String prompt = """
                Cảm biến "%s" báo động: giá trị %.1f %s ngưỡng %.1f.
                Giải thích ngắn gọn nguyên nhân khả dĩ và cách khắc phục, bằng tiếng Việt cho nông dân.
                """.formatted(sensorType, value, "min".equals(minOrMax) ? "thấp hơn" : "cao hơn", threshold);

        return hasKey() ? callGemini(prompt)
                : String.format("[Demo không có API key] %s = %.1f %s ngưỡng %.1f. "
                        + "Kiểm tra thiết bị và điều kiện môi trường; điều chỉnh tưới/che chắn phù hợp.",
                        sensorType, value, "min".equals(minOrMax) ? "dưới" : "trên", threshold);
    }

    private String callGemini(String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + apiKey;
            Map<String, Object> reqBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

            JsonNode res = http.post().uri(url)
                    .body(reqBody)
                    .retrieve()
                    .body(JsonNode.class);

            // candidates[0].content.parts[0].text
            JsonNode text = res != null
                    ? res.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    : null;
            if (text == null || text.isMissingNode() || text.asText().isBlank()) {
                log.warn("Gemini trả rỗng: {}", res);
                return "AI hiện chưa trả lời được. Vui lòng thử lại sau.";
            }
            return text.asText();
        } catch (Exception e) {
            // FR-7.3: lỗi AI không sập UI
            log.error("Lỗi gọi Gemini: {}", e.getMessage());
            return "Không kết nối được dịch vụ AI (" + e.getClass().getSimpleName()
                    + "). Vui lòng thử lại sau.";
        }
    }
}
