package org.example.server;

import org.example.shared.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private boolean authenticated = false;
    private volatile boolean running = true;
    private static final Map<String, String> users = new ConcurrentHashMap<>();

    public ClientHandler(Socket socket, ChatServer server) { this.socket = socket; this.server = server; }

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            while (running) {
                ProtocolMessage msg = MessageParser.readMessage(reader);
                if (msg == null) {
                    break;
                }
                if (msg.keys().isEmpty()) continue;
                handle(msg);
            }
        } catch (SocketTimeoutException e) {
            ChatServer.log("Timeout: " + (username != null ? username : "unknown"));
        } catch (Exception ignored) {

        } finally {
            disconnect();
        }
    }

    private void handle(ProtocolMessage msg) throws Exception {
        String command = msg.get("Command");
        if (command == null) return;

        switch (command.toLowerCase()) {
            case "login": login(msg); break;
            case "message": ifAuth(() -> sendMessage(msg)); break;
            case "list": ifAuth(this::sendUserList); break;
            case "logout": ifAuth(() -> {
                ProtocolMessage response = new ProtocolMessage();
                response.put("Status", "success");
                send(response);
                disconnect();
            }); break;
            case "upload": ifAuth(() -> {
                try { uploadFile(msg); } catch (Exception e) { throw new RuntimeException(e); }
            }); break;
            case "download": ifAuth(() -> {
                try { downloadFile(msg); } catch (Exception e) { throw new RuntimeException(e); }
            }); break;
            default: ChatServer.log("Unknown command: " + command);
        }
    }

    private void ifAuth(Runnable action) throws Exception {
        if (!authenticated) sendError("Not authenticated");
        else action.run();
    }

    private void login(ProtocolMessage msg) throws Exception {
        username = msg.get("Name");
        String password = msg.get("Password");
        if (username == null || username.isBlank()) {
            sendError("Username required");
            running = false;
            return;
        }

        String hash = HashUtil.sha256(password == null ? "" : password);

        if (!users.containsKey(username)) {
            users.put(username, hash);
            ChatServer.log("New user registered: " + username);
        } else if (!users.get(username).equals(hash)) {
            sendError("Invalid password");
            running = false;
            return;
        }

        if (server.getClient(username) != null && server.getClient(username) != this) {
            sendError("User already logged in");
            running = false;
            return;
        }

        authenticated = true;
        ProtocolMessage response = new ProtocolMessage();
        response.put("Status", "success");
        send(response);

        server.register(username, this);

        for (ProtocolMessage m : server.getHistory()) {
            send(m);
        }

        ProtocolMessage event = new ProtocolMessage();
        event.put("Event", "userlogin");
        event.put("UserName", username);
        server.broadcast(event);

        ChatServer.log("User logged in: " + username);
    }

    private void sendMessage(ProtocolMessage msg) {
        String text = msg.get("Message");
        if (text == null || text.isBlank()) { sendError("Message cannot be empty"); return; }

        ProtocolMessage response = new ProtocolMessage();
        response.put("Status", "success");
        send(response);

        ProtocolMessage event = new ProtocolMessage();
        event.put("Event", "message");
        event.put("From", username);
        event.put("Message", text);
        server.broadcast(event);
        ChatServer.log(username + " sent message");
    }


    private void sendUserList() {
        ProtocolMessage response = new ProtocolMessage();
        response.put("Status", "success");
        int i = 1;
        for (String user : server.getUsers()) {
            response.put("UserName" + i, user);
            i++;
        }
        response.put("UserCount", String.valueOf(i - 1));
        send(response);
        ChatServer.log("Sent user list to " + username);
    }

    private void uploadFile(ProtocolMessage msg) throws Exception {
        String name = msg.get("Name");
        String content = msg.get("Content");
        if (name == null || content == null) {
            sendError("File name and content required");
            return;
        }

        String fileId = java.util.UUID.randomUUID().toString();
        FileStorage.save(fileId, msg);

        ProtocolMessage response = new ProtocolMessage();
        response.put("Status", "success");
        response.put("FileId", fileId);
        send(response);

        byte[] bytes = java.util.Base64.getDecoder().decode(content.replace("\n", "").replace(" ", ""));
        ProtocolMessage event = new ProtocolMessage();
        event.put("Event", "file");
        event.put("FileId", fileId);
        event.put("From", username);
        event.put("Name", name);
        event.put("Size", String.valueOf(bytes.length));
        event.put("MimeType", msg.get("MimeType"));
        server.broadcast(event);
        ChatServer.log(username + " uploaded file: " + name + " (" + bytes.length + " bytes)");
    }

    private void downloadFile(ProtocolMessage msg) throws Exception {
        String fileId = msg.get("FileId");
        if (fileId == null) {
            sendError("FileId required");
            return;
        }

        ProtocolMessage stored = FileStorage.load(fileId);  // Загружает с диска
        if (stored == null) {
            sendError("File not found");
            return;
        }

        ProtocolMessage response = new ProtocolMessage();
        response.put("Status", "success");
        response.put("FileId", fileId);
        response.put("Name", stored.get("Name"));
        response.put("MimeType", stored.get("MimeType"));
        response.put("Encoding", "base64");
        response.put("Content", stored.get("Content"));
        send(response);
        ChatServer.log(username + " downloaded file: " + stored.get("Name"));
    }

    private void sendError(String message) {
        ProtocolMessage error = new ProtocolMessage();
        error.put("Status", "error");
        error.put("Message", message);
        send(error);
    }

    public synchronized void send(ProtocolMessage msg) {
        try {
            writer.write(msg.serialize());
            writer.flush();
        } catch (Exception e) {
            running = false;
        }
    }

    private void disconnect() {
        running = false;
        try {
            if (username != null && authenticated) {
                authenticated = false;
                server.unregister(username);
                ProtocolMessage event = new ProtocolMessage();
                event.put("Event", "userlogout");
                event.put("UserName", username);
                server.broadcast(event);
                ChatServer.log("User disconnected: " + username);
            }
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}