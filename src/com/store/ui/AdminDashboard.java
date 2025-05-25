package com.store.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AdminDashboard extends JFrame {
    private String username;

    public AdminDashboard(String username) {
        this.username = username;

        setTitle("Admin Dashboard - Welcome " + username);
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));

        JButton manageUsersBtn = new JButton("Manage Users");
        JButton manageProductsBtn = new JButton("Manage Products");
        JButton manageOrdersBtn = new JButton("Manage Orders");
        JButton manageDiscountsBtn = new JButton("Manage Discounts");
        JButton logoutBtn = new JButton("Logout");

        panel.add(manageUsersBtn);
        panel.add(manageProductsBtn);
        panel.add(manageOrdersBtn);
        panel.add(manageDiscountsBtn);
        panel.add(logoutBtn);

        add(panel);

        manageUsersBtn.addActionListener(e -> openUserManagement());
        manageProductsBtn.addActionListener(e -> openProductManagement());
        manageOrdersBtn.addActionListener(e -> openOrderManagement());
        manageDiscountsBtn.addActionListener(e -> openDiscountManagement());
        logoutBtn.addActionListener(e -> logout());

        setVisible(true);
    }

    private void openUserManagement() {
        new UserManagement();
    }

    private void openProductManagement() {
        new ProductManagement();
    }

    private void openOrderManagement() {
        JOptionPane.showMessageDialog(this, "Open Order Management");
    }

    private void openDiscountManagement() {
        JOptionPane.showMessageDialog(this, "Open Discount Management");
    }

    private void logout() {
        dispose();
        new com.store.auth.Login();
    }
}
