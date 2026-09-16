package com.agrisense.stream;

public final class StreamConstants {
    private StreamConstants() {}

    // ponytail: 1 stream chung cho mọi loại sensor — đủ cho MVP, giữ pattern consumer-group.
    // Nâng cấp: tách stream:{type} + nhiều group nếu cần scale/định tuyến theo loại.
    public static final String STREAM_KEY = "stream:readings";
    public static final String GROUP = "agrisense-workers";
}
