package com.store.ui;

import com.store.db.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.List;

public class OrderManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> statusFilter;

    public OrderManagement() {
        setTitle("Order Management");
        setSize(1200, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new Object[]{
                "Order ID", "Customer", "Order Date", "Products Summary", "Quantity", 
                "Subtotal", "Discount", "Total Amount", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton acceptBtn = new JButton("Accept Order");
        JButton denyBtn = new JButton("Deny Order");
        JButton refreshBtn = new JButton("Refresh");

        statusFilter = new JComboBox<>(new String[]{"All", "pending", "accepted", "denied"});
        statusFilter.addActionListener(this::filterOrders);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Filter by Status:"));
        topPanel.add(statusFilter);
        topPanel.add(refreshBtn);

        JPanel btnPanel = new JPanel();
        btnPanel.add(acceptBtn);
        btnPanel.add(denyBtn);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        acceptBtn.addActionListener(e -> updateOrderStatus("accepted"));
        denyBtn.addActionListener(e -> updateOrderStatus("denied"));
        refreshBtn.addActionListener(e -> loadOrders());

        loadOrders();
        setVisible(true);
    }

    private void loadOrders() {
        model.setRowCount(0);

        String sql = """
            SELECT 
                o.id AS order_id,
                u.username AS customer,
                o.order_date,
                STRING_AGG(
                    CONCAT(
                        oi.quantity, 'x ', p.name, ' (', p.description, ') @ ', 
                        FORMAT(oi.unit_price, 'N2'), ' = ', FORMAT(oi.quantity * oi.unit_price, 'N2')
                    ), 
                    '; '
                ) AS products_summary,
                SUM(oi.quantity) AS total_quantity,
                SUM(oi.quantity * oi.unit_price) AS subtotal,
                d.code AS discount_code,
                d.percentage AS discount_percentage,
                ROUND(
                    SUM(oi.quantity * oi.unit_price) * 
                    (1 - ISNULL(d.percentage, 0) / 100.0),
                    2
                ) AS total_amount,
                o.status
            FROM orders o
            JOIN users u ON o.user_id = u.id
            JOIN order_items oi ON o.id = oi.order_id
            JOIN products p ON oi.product_id = p.id
            LEFT JOIN discounts d ON o.discount_id = d.id
            GROUP BY 
                o.id, u.username, o.order_date, d.code, d.percentage, o.status
            ORDER BY o.order_date DESC
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String discountInfo = rs.getString("discount_code") != null ?
                        rs.getString("discount_code") + " (" + rs.getBigDecimal("discount_percentage") + "%)" : "None";

                model.addRow(new Object[]{
                        rs.getInt("order_id"),
                        rs.getString("customer"),
                        rs.getTimestamp("order_date"),
                        rs.getString("products_summary"),
                        rs.getInt("total_quantity"),
                        rs.getBigDecimal("subtotal"),
                        discountInfo,
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading orders: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void filterOrders(ActionEvent e) {
        String filter = (String) statusFilter.getSelectedItem();
        if ("All".equals(filter)) {
            loadOrders();
            return;
        }

        model.setRowCount(0); // Clear existing data

        String sql = """
            SELECT 
                o.id AS order_id,
                u.username AS customer,
                o.order_date,
                STRING_AGG(
                    CONCAT(
                        oi.quantity, 'x ', p.name, ' (', p.description, ') @ ', 
                        FORMAT(oi.unit_price, 'N2'), ' = ', FORMAT(oi.quantity * oi.unit_price, 'N2')
                    ), 
                    '; '
                ) AS products_summary,
                SUM(oi.quantity) AS total_quantity,
                SUM(oi.quantity * oi.unit_price) AS subtotal,
                d.code AS discount_code,
                d.percentage AS discount_percentage,
                ROUND(
                    SUM(oi.quantity * oi.unit_price) * 
                    (1 - ISNULL(d.percentage, 0) / 100.0),
                    2
                ) AS total_amount,
                o.status
            FROM orders o
            JOIN users u ON o.user_id = u.id
            JOIN order_items oi ON o.id = oi.order_id
            JOIN products p ON oi.product_id = p.id
            LEFT JOIN discounts d ON o.discount_id = d.id
            WHERE o.status = ?
            GROUP BY 
                o.id, u.username, o.order_date, d.code, d.percentage, o.status
            ORDER BY o.order_date DESC
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, filter.toLowerCase());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String discountInfo = rs.getString("discount_code") != null ?
                        rs.getString("discount_code") + " (" + rs.getBigDecimal("discount_percentage") + "%)" : "None";

                model.addRow(new Object[]{
                        rs.getInt("order_id"),
                        rs.getString("customer"),
                        rs.getTimestamp("order_date"),
                        rs.getString("products_summary"),
                        rs.getInt("total_quantity"),
                        rs.getBigDecimal("subtotal"),
                        discountInfo,
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status")
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error filtering orders: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateOrderStatus(String status) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order.");
            return;
        }

        // Get the actual model row (accounting for sorting)
        int modelRow = table.convertRowIndexToModel(selectedRow);
        Integer orderId = (Integer) model.getValueAt(modelRow, 0);
        if (orderId == null) {
            JOptionPane.showMessageDialog(this, "Invalid order selected.");
            return;
        }

        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Order #" + orderId + " status updated to " + status);

                // Update stock if order is accepted
                if ("accepted".equals(status)) {
                    updateProductStock(orderId, true);
                }

                loadOrders();
            } else {
                JOptionPane.showMessageDialog(this, "No order found with ID: " + orderId);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating status: " + e.getMessage());
        }
    }

    private void updateProductStock(int orderId, boolean isAccepting) {
        String sql = """
            UPDATE p
            SET p.stock = p.stock + (? * oi.quantity)
            FROM products p
            JOIN order_items oi ON p.id = oi.product_id
            WHERE oi.order_id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // If accepting order, subtract from stock (multiply by -1)
            // If rejecting order, add back to stock (multiply by 1)
            int multiplier = isAccepting ? -1 : 1;
            stmt.setInt(1, multiplier);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating product stock: " + e.getMessage());
        }
    }
}
