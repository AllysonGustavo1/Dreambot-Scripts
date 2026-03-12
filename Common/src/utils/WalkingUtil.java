package utils;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;

/**
 * Utility class for walking and movement operations
 */
public class WalkingUtil {

    private static final long DEFAULT_TIMEOUT = 5000;
    private static final int MIN_SLEEP = 150;
    private static final int MAX_SLEEP = 250;

    /**
     * Walks to a destination tile with timeout
     * @param destination The tile to walk to
     */
    public static void walkToDestination(Tile destination) {
        walkToDestination(destination, DEFAULT_TIMEOUT);
    }

    /**
     * Walks to a destination tile with custom timeout
     * @param destination The tile to walk to
     * @param timeout Maximum time to wait for walking in milliseconds
     */
    public static void walkToDestination(Tile destination, long timeout) {
        Walking.walk(destination);

        long startTime = System.currentTimeMillis();

        if (!Players.getLocal().isMoving()) {
            Sleep.sleep(MIN_SLEEP, MAX_SLEEP);
        }

        while (Walking.getDestinationDistance() > 1) {
            Sleep.sleep(MIN_SLEEP, MAX_SLEEP);
            if (System.currentTimeMillis() - startTime > timeout) {
                break;
            }
        }
    }

    /**
     * Checks if the player is currently moving
     * @return true if player is moving, false otherwise
     */
    public static boolean isPlayerMoving() {
        return Players.getLocal().isMoving();
    }

    /**
     * Gets the distance to the current walking destination
     * @return distance to destination
     */
    public static int getDestinationDistance() {
        return Walking.getDestinationDistance();
    }
}

