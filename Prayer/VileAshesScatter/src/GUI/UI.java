package GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.util.concurrent.TimeUnit;

public class UI {

    private final long startTime;
    private int ashesScattered;
    private State state;

    public UI(long startTime, int ashesScattered, State state) {
        this.startTime = startTime;
        this.ashesScattered = ashesScattered;
        this.state = state;
    }

    public void onPaint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(4, 274, 220, 65);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Runtime: " + formatTime(System.currentTimeMillis() - this.startTime), 10, 287);
        g.setColor(Color.YELLOW);
        g.drawString("Vile Ashes Scattered: " + ashesScattered, 10, 303);
        g.setColor(Color.PINK);
        g.drawString("State: " + state, 10, 319);
    }

    private String formatTime(long duration) {
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));

        if (days == 0) {
            return hours + ":" + minutes + ":" + seconds;
        }

        return days + ":" + hours + ":" + minutes + ":" + seconds;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setAshesScattered(int ashesScattered) {
        this.ashesScattered = ashesScattered;
    }
}

