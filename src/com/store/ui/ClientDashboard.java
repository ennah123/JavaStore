package com.store.ui;

import com.store.db.Database;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class ClientDashboard extends JFrame {

    private String username;
    private int userId;

    private JPanel productsPanel;
    private JScrollPane productsScroll;

    private DefaultListModel<String> cartListModel;
    private JList<String> cartList;
    private JButton placeOrderBtn;

    private Map<Integer, CartItem> cart = new HashMap<>();

    public ClientDashboard(String username) {
        this.username = username;
        userId = fetchUserId();

        setTitle("Client Dashboard - Welcome " + username);
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 245, 245));

        JLabel welcomeLabel = new JLabel("Welcome, " + username, JLabel.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(welcomeLabel, BorderLayout.NORTH);

        productsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        productsPanel.setBackground(new Color(245, 245, 245));
        productsScroll = new JScrollPane(productsPanel);
        productsScroll.setBorder(BorderFactory.createEmptyBorder());
        add(productsScroll, BorderLayout.CENTER);

        JPanel cartPanel = new JPanel(new BorderLayout(10,10));
        cartPanel.setPreferredSize(new Dimension(300, 0));
        cartPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150)),
            "Panier",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Segoe UI Semibold", Font.BOLD, 16),
            new Color(70, 70, 70)
        ));
        cartPanel.setBackground(Color.WHITE);

        cartListModel = new DefaultListModel<>();
        cartList = new JList<>(cartListModel);
        cartList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cartPanel.add(new JScrollPane(cartList), BorderLayout.CENTER);

        placeOrderBtn = new JButton("Place Order");
        placeOrderBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        placeOrderBtn.setBackground(new Color(0, 123, 255));
        placeOrderBtn.setForeground(Color.WHITE);
        placeOrderBtn.setFocusPainted(false);
        placeOrderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        placeOrderBtn.addActionListener(e -> placeOrder());
        cartPanel.add(placeOrderBtn, BorderLayout.SOUTH);

        add(cartPanel, BorderLayout.EAST);

        loadProducts();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private int fetchUserId() {
        int id = -1;
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching user ID: " + e.getMessage());
        }
        return id;
    }

    private void loadProducts() {
        productsPanel.removeAll();

        String sql = "SELECT id, name, description, price, stock, image_path FROM products";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                BigDecimal price = rs.getBigDecimal("price");
                int stock = rs.getInt("stock");
                String imagePath = rs.getString("image_path");

                JPanel card = createProductCard(id, name, description, price, stock, imagePath);
                productsPanel.add(card);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }

        productsPanel.revalidate();
        productsPanel.repaint();
    }

    private JPanel createProductCard(int id, String name, String description, BigDecimal price, int stock, String imagePath) {
        return new ProductCard(id, name, description, price, stock, imagePath);
    }

    private void addToCart(int productId, String name, BigDecimal price, int stock) {
        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity for " + name + ":");
        if (qtyStr == null) return;

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be > 0.");
                return;
            }
            if (quantity > stock) {
                JOptionPane.showMessageDialog(this, "Not enough stock. Available: " + stock);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
            return;
        }

        CartItem item = cart.get(productId);
        if (item == null) {
            cart.put(productId, new CartItem(productId, name, price, quantity));
        } else {
            int newQty = item.quantity + quantity;
            if (newQty > stock) {
                JOptionPane.showMessageDialog(this, "Total quantity exceeds stock.");
                return;
            }
            item.quantity = newQty;
        }
        refreshCartList();
    }

    private void refreshCartList() {
        cartListModel.clear();
        for (CartItem item : cart.values()) {
            String line = String.format("%s x%d - $%.2f", item.name, item.quantity, item.price.multiply(new BigDecimal(item.quantity)));
            cartListModel.addElement(line);
        }
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your panier is empty.");
            return;
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            String insertOrderSql = "INSERT INTO orders (user_id, status) VALUES (?, ?)";
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, "pending");
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    orderId = keys.getInt(1);
                } else {
                    throw new SQLException("Failed to get order ID.");
                }
            }

            String insertItemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock = stock - ? WHERE id = ?";
            for (CartItem item : cart.values()) {
                try (PreparedStatement psItem = conn.prepareStatement(insertItemSql);
                     PreparedStatement psStock = conn.prepareStatement(updateStockSql)) {

                    psItem.setInt(1, orderId);
                    psItem.setInt(2, item.productId);
                    psItem.setInt(3, item.quantity);
                    psItem.setBigDecimal(4, item.price);
                    psItem.executeUpdate();

                    psStock.setInt(1, item.quantity);
                    psStock.setInt(2, item.productId);
                    psStock.executeUpdate();
                }
            }

            conn.commit();

            JOptionPane.showMessageDialog(this, "Order placed successfully!");
            cart.clear();
            refreshCartList();
            loadProducts();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error placing order: " + e.getMessage());
        }
    }

    private class ProductCard extends JPanel {
        private final Color BASE_BG = Color.WHITE;
        private final Color HOVER_BG = new Color(230, 245, 255);
        private final Border BASE_BORDER = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                new LineBorder(new Color(180, 180, 180), 1, true));
        private final Border HOVER_BORDER = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                new LineBorder(new Color(30, 144, 255), 2, true));

        public ProductCard(int id, String name, String description, BigDecimal price, int stock, String imagePath) {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(260, 200));
            setBackground(BASE_BG);
            setBorder(BASE_BORDER);

            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(260, 100));
            imageLabel.setHorizontalAlignment(JLabel.CENTER);

            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    ImageIcon icon = new ImageIcon(getClass().getResource("/images/"+imagePath));
                    Image img = icon.getImage().getScaledInstance(260, 100, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(img));
                } catch (Exception e) {
                    imageLabel.setText("No Image");
                }
            } else {
                imageLabel.setText("No Image");
            }

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 18));
            nameLabel.setForeground(new Color(40, 40, 40));
            nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JTextArea descArea = new JTextArea(description);
            descArea.setWrapStyleWord(true);
            descArea.setLineWrap(true);
            descArea.setEditable(false);
            descArea.setFocusable(false);
            descArea.setOpaque(false);
            descArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            descArea.setForeground(new Color(80, 80, 80));
            descArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            bottomPanel.setOpaque(false);

            JLabel priceLabel = new JLabel("Price: $" + price);
            priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            priceLabel.setForeground(new Color(0, 102, 204));

            JLabel stockLabel = new JLabel("Stock: " + stock);
            stockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            stockLabel.setForeground(new Color(100, 100, 100));

            bottomPanel.add(priceLabel);
            bottomPanel.add(stockLabel);

            JButton addToCartBtn = new JButton("Add to Panier");
            addToCartBtn.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
            addToCartBtn.setBackground(new Color(0, 123, 255));
            addToCartBtn.setForeground(Color.WHITE);
            addToCartBtn.setFocusPainted(false);
            addToCartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addToCartBtn.addActionListener(e -> addToCart(id, name, price, stock));

            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setOpaque(false);
            centerPanel.add(nameLabel, BorderLayout.NORTH);
            centerPanel.add(descArea, BorderLayout.CENTER);
            centerPanel.add(bottomPanel, BorderLayout.SOUTH);

            add(imageLabel, BorderLayout.NORTH);
            add(centerPanel, BorderLayout.CENTER);
            add(addToCartBtn, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(HOVER_BG);
                    setBorder(HOVER_BORDER);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(BASE_BG);
                    setBorder(BASE_BORDER);
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }
    }

    private static class CartItem {
        int productId;
        String name;
        BigDecimal price;
        int quantity;

        CartItem(int productId, String name, BigDecimal price, int quantity) {
            this.productId = productId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }

}
