package org.example.server;

import org.example.shared.ProtocolMessage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileStorage {
    private static final String STORAGE_DIR = "server_files";
    private static final String METADATA_DIR = "server_metadata";

    static {
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
            Files.createDirectories(Paths.get(METADATA_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save(String id, ProtocolMessage msg) throws IOException {
        String content = msg.get("Content");
        if (content != null) {
            byte[] bytes = Base64.getDecoder().decode(content.replace("\n", "").replace(" ", ""));
            Path filePath = Paths.get(STORAGE_DIR, id);
            Files.write(filePath, bytes);
        }

        Path metaPath = Paths.get(METADATA_DIR, id + ".meta");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(metaPath.toFile()))) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Name", msg.get("Name"));
            metadata.put("MimeType", msg.get("MimeType"));
            metadata.put("Encoding", msg.get("Encoding"));
            metadata.put("Size", String.valueOf(msg.get("Size")));
            oos.writeObject(metadata);
        }
    }

    public static ProtocolMessage load(String id) throws IOException {
        Path filePath = Paths.get(STORAGE_DIR, id);
        Path metaPath = Paths.get(METADATA_DIR, id + ".meta");

        if (!Files.exists(filePath) || !Files.exists(metaPath)) {
            return null;
        }

        ProtocolMessage msg = new ProtocolMessage();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(metaPath.toFile()))) {
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) ois.readObject();
            msg.put("Name", metadata.get("Name"));
            msg.put("MimeType", metadata.get("MimeType"));
            msg.put("Encoding", metadata.get("Encoding"));
        } catch (ClassNotFoundException e) {
            throw new IOException("Corrupted metadata", e);
        }

        byte[] bytes = Files.readAllBytes(filePath);
        msg.put("Content", Base64.getEncoder().encodeToString(bytes));

        return msg;
    }
}