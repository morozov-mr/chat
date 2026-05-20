package org.example.shared;

import java.util.*;

public class ProtocolMessage {
    private final Map<String, String> fields = new LinkedHashMap<>();

    public void put(String key, String value) { fields.put(key.toLowerCase(), value); }
    public String get(String key) { return fields.get(key.toLowerCase()); }
    public Set<String> keys() { return fields.keySet(); }
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields.entrySet())
            sb.append(e.getKey()).append(": ").append(e.getValue().replace("\n", "\n  ")).append("\n");
        return sb.append("\n").toString();
    }
}