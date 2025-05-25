package com.store.ui;

import com.store.db.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField usernameField, passwordField;
    private JComboBox<String> roleCombo;
    private JButton addBtn, updateBtn, deleteBtn;
    private int selectedUserId = -1;

    public UserManagement() {
        setTitle("User Management");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new Object[]{"ID", "Username", "Role"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JTextField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Role:"));
        roleCombo = new JComboBox<>(new String[]{"admin", "client"});
        formPanel.add(roleCombo);

        addBtn = new JButton("Add");
        updateBtn = new JButton("Update");
        deleteBtn = new JButton("Delete");

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(formPanel, BorderLayout.NORTH);
        add(btnPanel, BorderLayout.SOUTH);

        loadUsers();

        addBtn.addActionListener(e -> addUser());
        updateBtn.addActionListener(e -> updateUser());
        deleteBtn.addActionListener(e -> deleteUser());

        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                selectedUserId = (int) model.getValueAt(selectedRow, 0);
                usernameField.setText((String) model.getValueAt(selectedRow, 1));
                passwordField.setText("");
                roleCombo.setSelectedItem(model.getValueAt(selectedRow, 2));
            }
        });

        setVisible(true);
    }

    private Connection getConnection() throws SQLException {
        return Database.getConnection();
    }

    private void loadUsers() {
        model.setRowCount(0);
        String sql = "SELECT id, username, role FROM users";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role")
                });
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void addUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and Password cannot be empty.");
            return;
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
            loadUsers();
            clearFields();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Select a user to update.");
            return;
        }
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.");
            return;
        }

        String sql;
        if (password.isEmpty()) {
            sql = "UPDATE users SET username = ?, role = ? WHERE id = ?";
        } else {
            sql = "UPDATE users SET username = ?, password = ?, role = ? WHERE id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            int paramIndex = 2;
            if (password.isEmpty()) {
                stmt.setString(paramIndex++, role);
                stmt.setInt(paramIndex, selectedUserId);
            } else {
                stmt.setString(paramIndex++, password);
                stmt.setString(paramIndex++, role);
                stmt.setInt(paramIndex, selectedUserId);
            }

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                loadUsers();
                clearFields();
                selectedUserId = -1;
            } else {
                JOptionPane.showMessageDialog(this, "User not found.");
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Select a user to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedUserId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                loadUsers();
                clearFields();
                selectedUserId = -1;
            } else {
                JOptionPane.showMessageDialog(this, "User not found.");
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        roleCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
