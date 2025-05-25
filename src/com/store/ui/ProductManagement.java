package com.store.ui;

import com.store.db.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ProductManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField nameField, descField, priceField, stockField, filterField;
    private JButton addBtn, updateBtn, deleteBtn, filterBtn;
    private int selectedProductId = -1;

    public ProductManagement() {
        setTitle("Product Management");
        setSize(700, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Description", "Price", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by name:"));
        filterField = new JTextField(15);
        filterPanel.add(filterField);
        filterBtn = new JButton("Filter");
        filterPanel.add(filterBtn);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Description:"));
        descField = new JTextField();
        formPanel.add(descField);

        formPanel.add(new JLabel("Price:"));
        priceField = new JTextField();
        formPanel.add(priceField);

        formPanel.add(new JLabel("Stock:"));
        stockField = new JTextField();
        formPanel.add(stockField);

        addBtn = new JButton("Add");
        updateBtn = new JButton("Update");
        deleteBtn = new JButton("Delete");

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);

        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(formPanel, BorderLayout.EAST);
        add(btnPanel, BorderLayout.SOUTH);

        loadProducts();

        addBtn.addActionListener(e -> addProduct());
        updateBtn.addActionListener(e -> updateProduct());
        deleteBtn.addActionListener(e -> deleteProduct());
        filterBtn.addActionListener(e -> filterProducts());

        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                selectedProductId = (int) model.getValueAt(selectedRow, 0);
                nameField.setText((String) model.getValueAt(selectedRow, 1));
                descField.setText((String) model.getValueAt(selectedRow, 2));
                priceField.setText(model.getValueAt(selectedRow, 3).toString());
                stockField.setText(model.getValueAt(selectedRow, 4).toString());
            }
        });

        setVisible(true);
    }

    private Connection getConnection() throws SQLException {
        return Database.getConnection();
    }

    private void loadProducts() {
        loadProducts(null);
    }

    private void loadProducts(String filter) {
        model.setRowCount(0);
        String sql = "SELECT id, name, description, price, stock FROM products";
        if (filter != null && !filter.isEmpty()) {
            sql += " WHERE name LIKE ?";
        }
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (filter != null && !filter.isEmpty()) {
                stmt.setString(1, "%" + filter + "%");
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                });
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void addProduct() {
        String name = nameField.getText().trim();
        String description = descField.getText().trim();
        String priceStr = priceField.getText().trim();
        String stockStr = stockField.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, Price, and Stock are required.");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int stock = Integer.parseInt(stockStr);

            String sql = "INSERT INTO products (name, description, price, stock) VALUES (?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setDouble(3, price);
                stmt.setInt(4, stock);
                stmt.executeUpdate();
                loadProducts();
                clearFields();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a number and Stock must be an integer.");
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to update.");
            return;
        }
        String name = nameField.getText().trim();
        String description = descField.getText().trim();
        String priceStr = priceField.getText().trim();
        String stockStr = stockField.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, Price, and Stock are required.");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int stock = Integer.parseInt(stockStr);

            String sql = "UPDATE products SET name = ?, description = ?, price = ?, stock = ? WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setDouble(3, price);
                stmt.setInt(4, stock);
                stmt.setInt(5, selectedProductId);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    loadProducts();
                    clearFields();
                    selectedProductId = -1;
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found.");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a number and Stock must be an integer.");
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedProductId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                loadProducts();
                clearFields();
                selectedProductId = -1;
            } else {
                JOptionPane.showMessageDialog(this, "Product not found.");
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void filterProducts() {
        String filterText = filterField.getText().trim();
        loadProducts(filterText);
    }

    private void clearFields() {
        nameField.setText("");
        descField.setText("");
        priceField.setText("");
        stockField.setText("");
        table.clearSelection();
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
