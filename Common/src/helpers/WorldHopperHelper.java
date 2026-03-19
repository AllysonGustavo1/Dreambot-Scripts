package helpers;

import org.dreambot.api.methods.world.World;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper class for world hopping operations
 */
public class WorldHopperHelper {

    private static final Random random = new Random();
    private static final int EXCLUDED_WORLD = 301; // World 301 is usually excluded

    /**
     * Hops to a random F2P world
     */
    public static boolean hopToRandomWorld() {
        return hopToRandomWorld(true, false);
    }

    /**
     * Hops to a random world with specific criteria
     * @param f2pOnly If true, only hop to F2P worlds
     * @param membersOnly If true, only hop to members worlds
     */
    public static boolean hopToRandomWorld(boolean f2pOnly, boolean membersOnly) {
        List<World> worlds = Worlds.all();

        if (!worlds.isEmpty()) {
            List<World> suitableWorlds = new ArrayList<>();
            for (World world : worlds) {
                if (world.isNormal() && world.getMinimumLevel() == 0 && world.getWorld() != EXCLUDED_WORLD) {
                    if (f2pOnly && world.isF2P()) {
                        suitableWorlds.add(world);
                    } else if (membersOnly && world.isMembers()) {
                        suitableWorlds.add(world);
                    } else if (!f2pOnly && !membersOnly) {
                        suitableWorlds.add(world);
                    }
                }
            }

            if (!suitableWorlds.isEmpty()) {
                World newWorld = suitableWorlds.get(random.nextInt(suitableWorlds.size()));
                if (newWorld != null) {
                    Logger.log("Hopping to world: " + newWorld.getWorld());
                    return hopToWorld(newWorld.getWorld());
                }
            }
        }

        Logger.log("No suitable world found!");
        return false;
    }

    /**
     * Hops to a specific world number
     * @param worldNumber The world number to hop to
     */
    public static boolean hopToWorld(int worldNumber) {
        int currentWorld = getCurrentWorld();
        if (currentWorld == worldNumber) {
            Logger.log("Already in world " + worldNumber + ", skipping hop");
            return true;
        }

        List<World> worlds = Worlds.all();
        World targetWorld = null;

        for (World world : worlds) {
            if (world.getWorld() == worldNumber) {
                targetWorld = world;
                break;
            }
        }

        if (targetWorld != null) {
            Logger.log("Hopping from world " + currentWorld + " to world " + worldNumber);
            boolean hopRequested = WorldHopper.hopWorld(targetWorld);
            if (!hopRequested) {
                Logger.log("World hop request failed for world " + worldNumber);
                return false;
            }

            // Confirm that the client actually changed worlds before reporting success.
            boolean hopped = Sleep.sleepUntil(
                () -> getCurrentWorld() == worldNumber,
                12000,
                250
            );

            if (hopped) {
                Logger.log("Successfully hopped to world " + worldNumber);
                return true;
            }

            int finalWorld = getCurrentWorld();
            Logger.log("Hop timed out. Expected " + worldNumber + ", currently in " + finalWorld);
            return false;
        } else {
            Logger.log("World " + worldNumber + " not found!");
            return false;
        }
    }

    /**
     * Gets the current world number
     * @return Current world number
     */
    public static int getCurrentWorld() {
        World current = Worlds.getCurrent();
        return current != null ? current.getWorld() : -1;
    }
}

