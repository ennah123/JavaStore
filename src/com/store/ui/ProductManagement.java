package com.store.ui;

import com.store.db.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;

public class ProductManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField nameField, descField, priceField, stockField, filterField;
    private JButton addBtn, updateBtn, deleteBtn, filterBtn, selectImageBtn;
    private JLabel imageLabel;
    private File selectedImageFile = null;
    private int selectedProductId = -1;
    private static final String IMAGES_DIR = "images";

    public ProductManagement() {
        setTitle("Product Management");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Create images directory if it doesn't exist
        new File(IMAGES_DIR).mkdirs();

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Description", "Price", "Stock", "Image Path"}, 0) {
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

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 5, 5));
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

        formPanel.add(new JLabel("Image:"));
        imageLabel = new JLabel("No image selected");
        formPanel.add(imageLabel);

        selectImageBtn = new JButton("Select Image");
        formPanel.add(selectImageBtn);
        formPanel.add(new JLabel()); // empty cell

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

        // Listeners
        addBtn.addActionListener(e -> addProduct());
        updateBtn.addActionListener(e -> updateProduct());
        deleteBtn.addActionListener(e -> deleteProduct());
        filterBtn.addActionListener(e -> filterProducts());
        selectImageBtn.addActionListener(e -> selectImageFile());

        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                selectedProductId = (int) model.getValueAt(selectedRow, 0);
                nameField.setText((String) model.getValueAt(selectedRow, 1));
                descField.setText((String) model.getValueAt(selectedRow, 2));
                priceField.setText(model.getValueAt(selectedRow, 3).toString());
                stockField.setText(model.getValueAt(selectedRow, 4).toString());
                String imgPath = (String) model.getValueAt(selectedRow, 5);
                imageLabel.setText(imgPath == null ? "No image selected" : imgPath);
                selectedImageFile = null; // reset; image path loaded from DB
            }
        });

        setVisible(true);
    }

    private void selectImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            imageLabel.setText(selectedImageFile.getName());
        }
    }

    private Connection getConnection() throws SQLException {
        return Database.getConnection();
    }

    private void loadProducts() {
        loadProducts(null);
    }

    private void loadProducts(String filter) {
        model.setRowCount(0);
        String sql = "SELECT id, name, description, price, stock, image_path FROM products";
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
                        rs.getInt("stock"),
                        rs.getString("image_path")
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

            // Copy image file if one was selected
            String imageFileName = null;
            if (selectedImageFile != null) {
                imageFileName = selectedImageFile.getName();
                Path source = selectedImageFile.toPath();
                Path target = Paths.get(IMAGES_DIR, imageFileName);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String sql = "INSERT INTO products (name, description, price, stock, image_path) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setDouble(3, price);
                stmt.setInt(4, stock);
                stmt.setString(5, imageFileName);
                stmt.executeUpdate();
                loadProducts();
                clearFields();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a number and Stock must be an integer.");
        } catch (SQLException e) {
            showError(e);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error copying image file: " + e.getMessage());
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

            // Handle image file
            String imageFileName = null;
            if (selectedImageFile != null) {
                imageFileName = selectedImageFile.getName();
                Path source = selectedImageFile.toPath();
                Path target = Paths.get(IMAGES_DIR, imageFileName);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // Update image_path only if a new image is selected
            String sql;
            if (selectedImageFile != null) {
                sql = "UPDATE products SET name = ?, description = ?, price = ?, stock = ?, image_path = ? WHERE id = ?";
            } else {
                sql = "UPDATE products SET name = ?, description = ?, price = ?, stock = ? WHERE id = ?";
            }

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setDouble(3, price);
                stmt.setInt(4, stock);

                if (selectedImageFile != null) {
                    stmt.setString(5, imageFileName);
                    stmt.setInt(6, selectedProductId);
                } else {
                    stmt.setInt(5, selectedProductId);
                }

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    loadProducts();
                    clearFields();
                    selectedProductId = -1;
                    selectedImageFile = null;
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found.");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a number and Stock must be an integer.");
        } catch (SQLException e) {
            showError(e);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error copying image file: " + e.getMessage());
        }
    }

    private void deleteProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected product?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM products WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selectedProductId);
                stmt.executeUpdate();
                loadProducts();
                clearFields();
                selectedProductId = -1;
                selectedImageFile = null;
            } catch (SQLException e) {
                showError(e);
            }
        }
    }

    private void filterProducts() {
        String filter = filterField.getText().trim();
        loadProducts(filter);
    }

    private void clearFields() {
        nameField.setText("");
        descField.setText("");
        priceField.setText("");
        stockField.setText("");
        imageLabel.setText("No image selected");
        selectedImageFile = null;
        selectedProductId = -1;
        table.clearSelection();
    }

    private void showError(SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
    }
}