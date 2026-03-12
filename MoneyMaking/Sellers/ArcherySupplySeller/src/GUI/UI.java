package GUI;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class UI {

    private long startTime;
    private int suppliesSold = 0;
    private float totalProfit = 0;
    private String currentState = "STARTING";

    public UI() {
        this.startTime = System.currentTimeMillis();
    }

    public void updateStats(int supplies, float profit, int earned) {
        this.suppliesSold += supplies;
        this.totalProfit += profit;
    }

    public void setCurrentState(String state) {
        this.currentState = state;
    }

    public void setSuppliesSold(int supplies) {
        this.suppliesSold = supplies;
    }

    public void setProfit(float profit) {
        this.totalProfit = profit;
    }

    public void onPaint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 280, 190, 10, 10);

        // Border
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.drawRoundRect(10, 10, 280, 190, 10, 10);

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.CYAN);
        g2d.drawString("Archery Supply Seller v1.0", 20, 35);

        // Author
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("by AllysonGustavo", 20, 50);

        // Runtime
        long runtime = System.currentTimeMillis() - startTime;
        String runtimeStr = formatTime(runtime);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Runtime: " + runtimeStr, 20, 70);

        // Current State
        g2d.setColor(Color.YELLOW);
        g2d.drawString("State: " + currentState, 20, 85);

        // Statistics
        g2d.setColor(Color.GREEN);
        g2d.drawString("Supplies Sold: " + suppliesSold, 20, 105);

        g2d.setColor(Color.ORANGE);
        g2d.drawString("Total Profit: " + formatValue((int)totalProfit) + " gp", 20, 120);

        // Per hour calculations
        if (runtime > 0) {
            double hoursRunning = runtime / 3600000.0;
            int suppliesPerHour = (int)(suppliesSold / hoursRunning);
            int profitPerHour = (int)(totalProfit / hoursRunning);

            g2d.setColor(Color.MAGENTA);
            g2d.drawString("Supplies/hr: " + formatValue(suppliesPerHour), 20, 140);
            g2d.drawString("Profit/hr: " + formatValue(profitPerHour) + " gp", 20, 155);
        }

        // Required items info
        g2d.setFont(new Font("Arial", Font.ITALIC, 11));
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("Selling to Brian at Rimmington", 20, 175);
        g2d.drawString("Required: Some gp (Recommended: 100k+)", 20, 190);
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
    }

    private String formatValue(int value) {
        if (value >= 1000000) {
            return String.format("%.1fM", value / 1000000.0);
        } else if (value >= 1000) {
            return String.format("%.1fK", value / 1000.0);
        } else {
            return String.valueOf(value);
        }
    }
}
