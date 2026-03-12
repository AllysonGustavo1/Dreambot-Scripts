package GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.util.concurrent.TimeUnit;

public class UI {

    private long startTime;
    private int purchased;
    private float estimatedProfit;
    private State state;

    public UI(long startTime, int purchased, float estimatedProfit , State state) {
        this.startTime = startTime;
        this.purchased = purchased;
        this.estimatedProfit = estimatedProfit;
        this.state = state;
    }

    public void onPaint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(4, 274, 200, 65);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Runtime: " + ft(System.currentTimeMillis() - this.startTime), 10, 287);
        g.setColor(Color.YELLOW);
        g.drawString("Ruby Rings Picked: " + purchased, 10, 303);
        g.setColor(Color.GREEN);
        g.drawString("Estimated Profit: " + estimatedProfit, 10, 319);
        g.setColor(Color.PINK);
        g.drawString("State: " + state, 10, 335);
    }

    private String ft(long duration) {
        String res = "";
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        if (days == 0) {
            res = (hours + ":" + minutes + ":" + seconds);
        } else {
            res = (days + ":" + hours + ":" + minutes + ":" + seconds);
        }
        return res;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setPurchased(int purchased) {
        this.purchased = purchased;
    }

    public void setEstimatedProfit(float estimatedProfit) {
        this.estimatedProfit = estimatedProfit;
    }
}