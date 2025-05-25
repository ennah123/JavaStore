package com.store.auth;

import com.store.db.Database;
import com.store.models.User;
import com.store.ui.AdminDashboard;
import com.store.ui.ClientDashboard;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Login extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public Login() {
        setTitle("Login");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 1, 5, 5));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginBtn = new JButton("Login");

        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginBtn);

        loginBtn.addActionListener(e -> login());

        setVisible(true);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                JOptionPane.showMessageDialog(this, "Login successful as " + role);
                dispose();

                if (role.equalsIgnoreCase("admin")) {
                    new AdminDashboard(username);
                } else {
                    new ClientDashboard(username);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }
}
