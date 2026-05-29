package org.example.shared;

import java.util.*;

public class ProtocolMessage {
    private final Map<String, String> fields = new LinkedHashMap<>();
    private final Map<String, String> originalKeys = new HashMap<>();

    public void put(String key, String value) {
        if (key == null || value == null) return;
        fields.put(key.toLowerCase(), value);
        originalKeys.put(key.toLowerCase(), key);
    }

    public String get(String key) {
        if (key == null) return null;
        return fields.get(key.toLowerCase());
    }

    public Set<String> keys() {
        return fields.keySet();
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String originalKey = originalKeys.get(e.getKey());
            sb.append(originalKey)
                    .append(": ")
                    .append(e.getValue().replace("\n", "\n  "))
                    .append("\n");
        }
        return sb.append("\n").toString();
    }
}