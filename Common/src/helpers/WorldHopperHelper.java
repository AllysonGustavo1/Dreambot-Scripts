package helpers;

import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
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
    public static void hopToRandomWorld() {
        hopToRandomWorld(true, false);
    }

    /**
     * Hops to a random world with specific criteria
     * @param f2pOnly If true, only hop to F2P worlds
     * @param membersOnly If true, only hop to members worlds
     */
    public static void hopToRandomWorld(boolean f2pOnly, boolean membersOnly) {
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
                    WorldHopper.hopWorld(newWorld);
                    Sleep.sleep(500, 700);
                    while (Client.getGameState() == GameState.HOPPING) {
                        Sleep.sleep(200);
                    }
                    return;
                }
            }
        }

        Logger.log("No suitable world found!");
    }

    /**
     * Hops to a specific world number
     * @param worldNumber The world number to hop to
     */
    public static void hopToWorld(int worldNumber) {
        List<World> worlds = Worlds.all();
        World targetWorld = null;

        for (World world : worlds) {
            if (world.getWorld() == worldNumber) {
                targetWorld = world;
                break;
            }
        }

        if (targetWorld != null) {
            Logger.log("Hopping to world: " + worldNumber);
            WorldHopper.hopWorld(targetWorld);
            Sleep.sleep(500, 700);
            while (Client.getGameState() == GameState.HOPPING) {
                Sleep.sleep(200);
            }
        } else {
            Logger.log("World " + worldNumber + " not found!");
        }
    }

    /**
     * Gets the current world number
     * @return Current world number
     */
    public static int getCurrentWorld() {
        return Worlds.getCurrent().getWorld();
    }
}

