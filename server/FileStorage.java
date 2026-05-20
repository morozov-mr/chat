package org.example.server;

import org.example.shared.ProtocolMessage;
import java.util.*;

public class FileStorage {
    private static final Map<String, ProtocolMessage> files = new HashMap<>();
    public static void save(String id, ProtocolMessage msg) { files.put(id, msg); }
    public static ProtocolMessage load(String id) { return files.get(id); }
}