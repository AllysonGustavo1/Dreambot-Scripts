package helpers;

import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

/**
 * Helper class for interacting with game objects
 */
public class ObjectInteractionHelper {

    private static final int MIN_SLEEP = 2000;
    private static final int MAX_SLEEP = 3000;
    private static final int WAIT_SLEEP = 200;

    /**
     * Climbs a staircase to go up
     * @return true if successfully climbed
     */
    public static boolean climbStaircaseUp() {
        GameObject staircase = GameObjects.closest("Staircase");
        if (staircase != null) {
            int currentFloor = Players.getLocal().getZ();
            if (staircase.interact("Climb-up")) {
                Logger.log("Climbing staircase up...");
                Sleep.sleep(MIN_SLEEP, MAX_SLEEP);

                // Wait until we're on a different floor
                long startTime = System.currentTimeMillis();
                long timeout = 5000;
                while (Players.getLocal().getZ() == currentFloor) {
                    Sleep.sleep(WAIT_SLEEP);
                    if (System.currentTimeMillis() - startTime > timeout) {
                        Logger.log("Timeout waiting to climb stairs");
                        return false;
                    }
                }
                Logger.log("Successfully climbed to floor " + Players.getLocal().getZ());
                return true;
            }
        } else {
            Logger.log("Staircase not found!");
        }
        return false;
    }

    /**
     * Climbs a staircase to go down
     * @return true if successfully climbed
     */
    public static boolean climbStaircaseDown() {
        GameObject staircase = GameObjects.closest("Staircase");
        if (staircase != null) {
            int currentFloor = Players.getLocal().getZ();
            if (staircase.interact("Climb-down")) {
                Logger.log("Climbing staircase down...");
                Sleep.sleep(MIN_SLEEP, MAX_SLEEP);

                // Wait until we're on a different floor
                long startTime = System.currentTimeMillis();
                long timeout = 5000;
                while (Players.getLocal().getZ() == currentFloor) {
                    Sleep.sleep(WAIT_SLEEP);
                    if (System.currentTimeMillis() - startTime > timeout) {
                        Logger.log("Timeout waiting to climb stairs");
                        return false;
                    }
                }
                Logger.log("Successfully climbed to floor " + Players.getLocal().getZ());
                return true;
            }
        } else {
            Logger.log("Staircase not found!");
        }
        return false;
    }

    /**
     * Climbs to a specific floor using stairs
     * @param targetFloor The floor number to climb to
     * @return true if successfully reached target floor
     */
    public static boolean climbToFloor(int targetFloor) {
        int currentFloor = Players.getLocal().getZ();

        if (currentFloor == targetFloor) {
            return true;
        }

        if (currentFloor < targetFloor) {
            return climbStaircaseUp();
        } else {
            return climbStaircaseDown();
        }
    }

    /**
     * Gets the current floor/plane the player is on
     * @return Current floor number (0 = ground floor)
     */
    public static int getCurrentFloor() {
        return Players.getLocal().getZ();
    }
}

