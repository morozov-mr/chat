package org.example.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ChatWindow extends JFrame {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField input = new JTextField();
    private final DefaultListModel<String> users = new DefaultListModel<>();
    private final DefaultListModel<String> filesModel = new DefaultListModel<>();
    private final JList<String> filesList = new JList<>(filesModel);
    private final Map<String, String> fileIds = new HashMap<>();
    private final ClientConnection connection;
    private final JLabel userCountLabel;

    public ChatWindow(String host, int port, String user, String pass) {
        setTitle("Chat - " + user);
        setSize(900, 600);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JPanel rightPanel = new JPanel(new BorderLayout());
        JList<String> userList = new JList<>(users);
        userList.setFixedCellWidth(150);

        userCountLabel = new JLabel("Users");
        rightPanel.add(userCountLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.add(new JLabel("Files (double click to download)"), BorderLayout.NORTH);
        filesPanel.add(new JScrollPane(filesList), BorderLayout.CENTER);
        rightPanel.add(filesPanel, BorderLayout.SOUTH);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout());
        JButton sendButton = new JButton("Send");
        JButton fileButton = new JButton("Send File");
        JButton listButton = new JButton("Users List");
        JPanel buttons = new JPanel();
        buttons.add(sendButton);
        buttons.add(fileButton);
        buttons.add(listButton);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        connection = new ClientConnection(host, port, user, pass, this);

        sendButton.addActionListener(e -> {
            String text = input.getText();
            if (!text.isBlank()) {
                connection.sendMessage(text);
                input.setText("");
            }
        });

        fileButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                connection.sendFile(chooser.getSelectedFile());
        });

        listButton.addActionListener(e -> {
            connection.requestUserList();
        });

        filesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = filesList.getSelectedValue();
                    if (selected != null && fileIds.containsKey(selected))
                        connection.downloadFile(fileIds.get(selected));
                }
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void append(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void addUser(String user) {
        SwingUtilities.invokeLater(() -> {
            if (!users.contains(user)) users.addElement(user);
            updateUserCount();
        });
    }

    public void removeUser(String user) {
        SwingUtilities.invokeLater(() -> {
            users.removeElement(user);
            updateUserCount();
        });
    }

    public void clearUsers() {
        SwingUtilities.invokeLater(() -> {
            users.clear();
            updateUserCount();
        });
    }

    private void updateUserCount() {
        userCountLabel.setText("Users (" + users.size() + ")");
    }

    public void addFile(String fileId, String name, String size, String from) {
        String display = name + " (" + size + " bytes) from " + from;
        fileIds.put(display, fileId);
        SwingUtilities.invokeLater(() -> filesModel.addElement(display));
    }
}