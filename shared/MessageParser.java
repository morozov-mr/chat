package org.example.shared;

import java.io.*;

public class MessageParser {
    public static ProtocolMessage readMessage(BufferedReader reader) throws IOException {
        ProtocolMessage msg = new ProtocolMessage();
        String line, currentKey = null;
        StringBuilder currentValue = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (currentKey != null) msg.put(currentKey, currentValue.toString());
                return msg.keys().isEmpty() ? null : msg;
            }
            if (line.startsWith("  ") && currentKey != null) {
                currentValue.append("\n").append(line.substring(2));
                continue;
            }
            if (currentKey != null) msg.put(currentKey, currentValue.toString());
            int idx = line.indexOf(": ");
            if (idx == -1) continue;
            currentKey = line.substring(0, idx);
            currentValue = new StringBuilder(idx + 2 < line.length() ? line.substring(idx + 2) : "");
        }
        return msg.keys().isEmpty() ? null : msg;
    }
}