package com.store.ui;

import javax.swing.*;
import java.awt.*;

public class ClientDashboard extends JFrame {
    private String username;

    public ClientDashboard(String username) {
        this.username = username;
        setTitle("Client Dashboard - " + username);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JButton btnBrowseProducts = new JButton("Browse Products");
        JButton btnMyOrders = new JButton("My Orders");

        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        panel.add(btnBrowseProducts);
        panel.add(btnMyOrders);

        add(panel);

        btnBrowseProducts.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Browse Products not implemented yet");
        });

        btnMyOrders.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "My Orders not implemented yet");
        });

        setVisible(true);
    }
}
