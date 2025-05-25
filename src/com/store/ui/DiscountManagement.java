package com.store.ui;

import com.store.db.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DiscountManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public DiscountManagement() {
        setTitle("Discount Management");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Table setup
        model = new DefaultTableModel(new Object[]{
                "ID", "Code", "Description", "Percentage", "Valid From", "Valid To"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Buttons panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addBtn = new JButton("Add Discount");
        JButton editBtn = new JButton("Edit Discount");
        JButton deleteBtn = new JButton("Delete Discount");

        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Button actions
        addBtn.addActionListener(e -> openDiscountForm(null));
        editBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a discount to edit.");
                return;
            }
            int id = (int) model.getValueAt(selectedRow, 0);
            openDiscountForm(id);
        });
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a discount to delete.");
                return;
            }
            int id = (int) model.getValueAt(selectedRow, 0);
            deleteDiscount(id);
        });

        loadDiscounts();

        setVisible(true);
    }

    private void loadDiscounts() {
        model.setRowCount(0);
        String sql = "SELECT * FROM discounts ORDER BY id DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("description"),
                        rs.getBigDecimal("percentage"),
                        rs.getDate("valid_from"),
                        rs.getDate("valid_to")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading discounts: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDiscountForm(Integer discountId) {
        // If discountId == null, it's Add new, else Edit existing
        JDialog dialog = new JDialog(this, discountId == null ? "Add Discount" : "Edit Discount", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(7, 2, 5, 5));

        JTextField codeField = new JTextField();
        JTextField descField = new JTextField();
        JTextField percField = new JTextField();
        JTextField validFromField = new JTextField("YYYY-MM-DD");
        JTextField validToField = new JTextField("YYYY-MM-DD");

        if (discountId != null) {
            // Load existing data
            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM discounts WHERE id = ?")) {
                stmt.setInt(1, discountId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    codeField.setText(rs.getString("code"));
                    descField.setText(rs.getString("description"));
                    percField.setText(rs.getBigDecimal("percentage").toString());
                    validFromField.setText(rs.getDate("valid_from") != null ? rs.getDate("valid_from").toString() : "");
                    validToField.setText(rs.getDate("valid_to") != null ? rs.getDate("valid_to").toString() : "");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Error loading discount data: " + e.getMessage());
                dialog.dispose();
                return;
            }
        }

        dialog.add(new JLabel("Code:"));
        dialog.add(codeField);
        dialog.add(new JLabel("Description:"));
        dialog.add(descField);
        dialog.add(new JLabel("Percentage (%):"));
        dialog.add(percField);
        dialog.add(new JLabel("Valid From (YYYY-MM-DD):"));
        dialog.add(validFromField);
        dialog.add(new JLabel("Valid To (YYYY-MM-DD):"));
        dialog.add(validToField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        dialog.add(saveBtn);
        dialog.add(cancelBtn);

        saveBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            String desc = descField.getText().trim();
            String percStr = percField.getText().trim();
            String validFromStr = validFromField.getText().trim();
            String validToStr = validToField.getText().trim();

            if (code.isEmpty() || percStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Code and Percentage are required.");
                return;
            }

            try {
                double percentage = Double.parseDouble(percStr);
                if (percentage <= 0 || percentage > 100) {
                    JOptionPane.showMessageDialog(dialog, "Percentage must be between 0 and 100.");
                    return;
                }

                Date validFrom = null;
                Date validTo = null;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                if (!validFromStr.isEmpty()) {
                    LocalDate lf = LocalDate.parse(validFromStr, formatter);
                    validFrom = Date.valueOf(lf);
                }
                if (!validToStr.isEmpty()) {
                    LocalDate lt = LocalDate.parse(validToStr, formatter);
                    validTo = Date.valueOf(lt);
                }

                if (discountId == null) {
                    // Insert new
                    String insertSql = "INSERT INTO discounts (code, description, percentage, valid_from, valid_to) VALUES (?, ?, ?, ?, ?)";
                    try (Connection conn = Database.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        stmt.setString(1, code);
                        stmt.setString(2, desc);
                        stmt.setDouble(3, percentage);
                        if (validFrom != null) stmt.setDate(4, validFrom); else stmt.setNull(4, Types.DATE);
                        if (validTo != null) stmt.setDate(5, validTo); else stmt.setNull(5, Types.DATE);
                        stmt.executeUpdate();
                    }
                } else {
                    // Update existing
                    String updateSql = "UPDATE discounts SET code = ?, description = ?, percentage = ?, valid_from = ?, valid_to = ? WHERE id = ?";
                    try (Connection conn = Database.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                        stmt.setString(1, code);
                        stmt.setString(2, desc);
                        stmt.setDouble(3, percentage);
                        if (validFrom != null) stmt.setDate(4, validFrom); else stmt.setNull(4, Types.DATE);
                        if (validTo != null) stmt.setDate(5, validTo); else stmt.setNull(5, Types.DATE);
                        stmt.setInt(6, discountId);
                        stmt.executeUpdate();
                    }
                }

                dialog.dispose();
                loadDiscounts();

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Invalid percentage value.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving discount: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void deleteDiscount(int id) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this discount?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM discounts WHERE id = ?")) {
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                JOptionPane.showMessageDialog(this, "Discount deleted successfully.");
                loadDiscounts();
            } else {
                JOptionPane.showMessageDialog(this, "Discount not found.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting discount: " + e.getMessage());
        }
    }
}
