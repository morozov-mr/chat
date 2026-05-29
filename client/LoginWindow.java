package org.example.client;

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    public LoginWindow() {
        setTitle("Login");
        setSize(300, 200);
        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField("8888");
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton connectButton = new JButton("Connect");

        setLayout(new GridLayout(5, 2));
        add(new JLabel("Host"));
        add(hostField);
        add(new JLabel("Port"));
        add(portField);
        add(new JLabel("User"));
        add(userField);
        add(new JLabel("Password"));
        add(passField);
        add(connectButton);

        connectButton.addActionListener(e -> {
            new ChatWindow(hostField.getText(), Integer.parseInt(portField.getText()),
                    userField.getText(), new String(passField.getPassword()));
            dispose();
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(LoginWindow::new); }
}