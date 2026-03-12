package helpers;

import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import utils.WalkingUtil;

/**
 * Helper class for banking operations
 */
public class BankingHelper {

    private static final int MIN_SLEEP = 200;
    private static final int MAX_SLEEP = 300;
    private static final int DEPOSIT_SLEEP_MIN = 500;
    private static final int DEPOSIT_SLEEP_MAX = 700;

    /**
     * Walks to the closest bank
     * @param includeTeleports Whether to use teleports
     */
    public static void walkToClosestBank(boolean includeTeleports) {
        walkToClosestBank(null, includeTeleports);
    }

    /**
     * Walks to a specific bank area or the closest bank
     * @param specificBankArea The specific bank area to walk to (null for closest)
     * @param includeTeleports Whether to use teleports
     */
    public static void walkToClosestBank(Area specificBankArea, boolean includeTeleports) {
        if (specificBankArea != null) {
            WalkingUtil.walkToDestination(specificBankArea.getRandomTile());
        } else {
            BankLocation closestBank = Bank.getClosestBankLocation(includeTeleports);
            if (closestBank != null) {
                WalkingUtil.walkToDestination(closestBank.getTile());
            }
        }
    }

    /**
     * Opens the bank using a bank booth
     * @return true if bank was opened successfully
     */
    public static boolean openBank() {
        GameObject bank = GameObjects.closest("Bank booth");
        if (bank != null && bank.interact("Bank")) {
            while (!Bank.isOpen()) {
                Sleep.sleep(MIN_SLEEP);
                if (!bank.isOnScreen() && !Players.getLocal().isMoving()) {
                    break;
                }
            }
            return Bank.isOpen();
        }
        return false;
    }

    /**
     * Deposits all items in the bank
     * @param bankArea The bank area where player should be
     * @return true if items were deposited successfully
     */
    public static boolean depositAllItems(Area bankArea) {
        if (bankArea != null && bankArea.contains(Players.getLocal())) {
            if (openBank()) {
                Bank.depositAllItems();
                Sleep.sleep(DEPOSIT_SLEEP_MIN, DEPOSIT_SLEEP_MAX);
                Bank.close();
                return true;
            }
        } else {
            Logger.log("Not in bank area, walking to bank...");
            walkToClosestBank(bankArea, true);
        }
        return false;
    }

    /**
     * Deposits a specific item in the bank
     * @param itemName The name of the item to deposit
     * @param bankArea The bank area where player should be
     * @return true if items were deposited successfully
     */
    public static boolean depositItem(String itemName, Area bankArea) {
        if (bankArea != null && bankArea.contains(Players.getLocal())) {
            if (openBank()) {
                Bank.depositAll(itemName);
                Sleep.sleep(DEPOSIT_SLEEP_MIN, DEPOSIT_SLEEP_MAX);
                Bank.close();
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if player is currently in a bank area
     * @param bankArea The bank area to check
     * @return true if player is in the bank area
     */
    public static boolean isInBankArea(Area bankArea) {
        return bankArea != null && bankArea.contains(Players.getLocal());
    }
}

