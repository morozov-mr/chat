package org.example.server;

import org.example.shared.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.*;

public class ChatServer {
    private final int port;
    private final int historyMax;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final List<ProtocolMessage> history = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger("ChatServer");
    private static boolean logEnabled;
    private static String logFile;

    public ChatServer(int port, int historyMax) { this.port = port; this.historyMax = historyMax; }

    public void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        log("Server started on port " + port);
        while (true) {
            Socket socket = serverSocket.accept();
            socket.setSoTimeout(300000);
            new Thread(new ClientHandler(socket, this)).start();
        }
    }

    public void register(String name, ClientHandler handler) {
        clients.put(name, handler);
        log(name + " registered");
    }

    public void unregister(String name) {
        clients.remove(name);
        log(name + " unregistered");
    }

    public Collection<String> getUsers() { return clients.keySet(); }
    public ClientHandler getClient(String name) { return clients.get(name); }

    public void broadcast(ProtocolMessage msg) {
        if (history.size() >= historyMax) history.remove(0);
        history.add(msg);
        for (ClientHandler c : clients.values()) c.send(msg);
        log("Broadcast: " + msg.get("event"));
    }

    public List<ProtocolMessage> getHistory() { return history; }

    public static void log(String msg) {
        if (logEnabled) {
            System.out.println("[" + new Date() + "] " + msg);
            if (logFile != null) {
                try (FileWriter fw = new FileWriter(logFile, true)) {
                    fw.write("[" + new Date() + "] " + msg + "\n");
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) { p.load(fis); }
        catch (FileNotFoundException e) { System.err.println("server.properties not found, using defaults"); }

        int port = Integer.parseInt(p.getProperty("server.port", "5000"));
        logEnabled = Boolean.parseBoolean(p.getProperty("log.enabled", "true"));
        logFile = p.getProperty("log.file", null);
        int historyMax = Integer.parseInt(p.getProperty("history.max", "100"));

        new ChatServer(port, historyMax).start();
    }
}