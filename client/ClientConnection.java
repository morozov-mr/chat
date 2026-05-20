package org.example.client;

import org.example.shared.MessageParser;
import org.example.shared.ProtocolMessage;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.Base64;

public class ClientConnection {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final ChatWindow window;
    private boolean authenticated = false;

    public ClientConnection(String host, int port, String user, String pass, ChatWindow window) {
        try {
            this.window = window;
            socket = new Socket(host, port);
            socket.setSoTimeout(30000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            login(user, pass);
            new Thread(this::listen).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void login(String user, String pass) {
        ProtocolMessage msg = new ProtocolMessage();
        msg.put("Command", "login");
        msg.put("Name", user);
        msg.put("Password", pass);
        send(msg);
    }

    public void sendMessage(String text) {
        if (!authenticated) {
            window.append("Not connected to server");
            return;
        }
        ProtocolMessage msg = new ProtocolMessage();
        msg.put("Command", "message");
        msg.put("Message", text);
        send(msg);
    }

    public void sendFile(File file) {
        if (!authenticated) {
            window.append("Not connected to server");
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            ProtocolMessage msg = new ProtocolMessage();

            msg.put("Command", "upload");
            msg.put("Name", file.getName());

            String mime = Files.probeContentType(file.toPath());
            if (mime != null) msg.put("MimeType", mime);

            msg.put("Encoding", "base64");
            msg.put("Content", Base64.getEncoder().encodeToString(bytes));
            send(msg);
        } catch (Exception e) {
            e.printStackTrace();
            window.append("Failed to send file: " + e.getMessage());
        }
    }

    public void downloadFile(String fileId) {
        if (!authenticated) {
            window.append("Not connected to server");
            return;
        }
        ProtocolMessage msg = new ProtocolMessage();
        msg.put("Command", "download");
        msg.put("FileId", fileId);
        send(msg);
    }

    public void requestUserList() {
        if (!authenticated) {
            window.append("Not connected to server");
            return;
        }
        ProtocolMessage msg = new ProtocolMessage();
        msg.put("Command", "list");
        send(msg);
    }

    private void listen() {
        try {
            while (true) {
                ProtocolMessage msg = MessageParser.readMessage(reader);
                if (msg == null || msg.keys().isEmpty()) continue;
                handle(msg);
            }
        } catch (SocketTimeoutException e) {
            window.append("Connection timeout");
            disconnect();
        } catch (Exception e) {
            window.append("Disconnected");
            disconnect();
        }
    }

    private void handle(ProtocolMessage msg) {
        String status = msg.get("status");

        if ("success".equalsIgnoreCase(status) && msg.get("usercount") != null) {
            handleUserList(msg);
            return;
        }

        if ("success".equalsIgnoreCase(status)) {
            authenticated = true;
            if (msg.get("content") != null) {
                saveFile(msg);
            } else if (msg.get("fileid") != null) {
                window.append("File uploaded successfully");
            }
            return;
        }

        if ("error".equalsIgnoreCase(status)) {
            window.append("Error: " + msg.get("message"));
            if (!authenticated) disconnect();
            return;
        }

        String event = msg.get("event");
        if (event != null) {
            switch (event.toLowerCase()) {
                case "message":
                    window.append(msg.get("from") + ": " + msg.get("message"));
                    break;
                case "userlogin":
                    window.append(msg.get("username") + " joined");
                    window.addUser(msg.get("username"));
                    break;
                case "userlogout":
                    window.append(msg.get("username") + " left");
                    window.removeUser(msg.get("username"));
                    break;
                case "file":
                    window.append(msg.get("from") + " sent file " + msg.get("name") +
                            " (" + msg.get("size") + " bytes)");
                    window.addFile(msg.get("fileid"), msg.get("name"),
                            msg.get("size"), msg.get("from"));
                    break;
            }
        }
    }

    private void handleUserList(ProtocolMessage msg) {
        try {
            int count = Integer.parseInt(msg.get("usercount"));
            window.clearUsers();
            for (int i = 1; i <= count; i++) {
                String user = msg.get("username" + i);
                if (user != null && !user.isBlank()) {
                    window.addUser(user);
                }
            }
            window.append("User list refreshed: " + count + " users online");
        } catch (NumberFormatException e) {
            window.append("Error parsing user list");
        }
    }

    private void saveFile(ProtocolMessage msg) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(msg.get("name")));

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = Base64.getDecoder().decode(msg.get("content"));
                Files.write(chooser.getSelectedFile().toPath(), bytes);
                JOptionPane.showMessageDialog(null, "File saved to " + chooser.getSelectedFile().getName());
                window.append("File downloaded: " + msg.get("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            window.append("Failed to save file: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    private synchronized void send(ProtocolMessage msg) {
        try {
            writer.write(msg.serialize());
            writer.flush();
        } catch (Exception e) {
            window.append("Failed to send: " + e.getMessage());
        }
    }
}