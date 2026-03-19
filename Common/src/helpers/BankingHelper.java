package helpers;

import org.dreambot.api.methods.container.impl.Inventory;
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
        // Se o banco já está aberto, retorna true
        if (Bank.isOpen()) {
            Logger.log("Bank is already open!");
            return true;
        }
        
        String[] bankNames = {"Bank table", "Bank booth", "Bank Booth", "Bank", "Banker", "bank booth"};
        GameObject bank = null;
        
        // Estratégia 1: Tentar encontrar o banco com diferentes nomes
        Logger.log("Searching for bank booth...");
        for (String name : bankNames) {
            bank = GameObjects.closest(name);
            if (bank != null) {
                Logger.log("Found bank with name: '" + name + "' at distance: " + bank.distance());
                break;
            }
        }
        
        // Se ainda não encontrou, procura por qualquer GameObject com nome perto
        if (bank == null) {
            Logger.log("Bank booth not found by name, trying closest named GameObject...");
            bank = GameObjects.closest(g -> g != null && g.getName() != null && !g.getName().equals("null") && g.distance() < 15);
            if (bank != null) {
                Logger.log("Found nearby GameObject: " + bank.getName() + " at distance: " + bank.distance());
            }
        }
        
        if (bank == null) {
            Logger.log("❌ Bank booth not found anywhere!");
            return false;
        }
        
        // Se o banco está muito longe, aproximar
        if (bank.distance() > 6) {
            Logger.log("Bank is too far (" + bank.distance() + "), moving closer...");
            WalkingUtil.walkToDestination(bank.getTile());
            Sleep.sleep(800, 1200);
        }
        
        Logger.log("Attempting to interact with bank: " + bank.getName() + " (Distance: " + bank.distance() + ")");
        
        // Tentar interagir com o banco - primeiro sem ação, depois com ações específicas
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Logger.log("Interaction attempt #" + attempt + "/" + maxAttempts);
            
            // Tentar 1: interact sem argumento
            if (attempt <= 2) {
                Logger.log("  Trying interact() without action...");
                if (bank.interact()) {
                    Logger.log("  ✓ Clicked on bank (no action), waiting for it to open...");
                    return waitForBankToOpen();
                }
            }
            // Tentar 2-3: interact com "Use"
            else if (attempt <= 4) {
                Logger.log("  Trying interact('Use')...");
                if (bank.interact("Use")) {
                    Logger.log("  ✓ Clicked on bank with 'Use' action, waiting for it to open...");
                    return waitForBankToOpen();
                }
            }
            // Tentar 4: interact com "Bank"
            else {
                Logger.log("  Trying interact('Bank')...");
                if (bank.interact("Bank")) {
                    Logger.log("  ✓ Clicked on bank with 'Bank' action, waiting for it to open...");
                    return waitForBankToOpen();
                }
            }
            
            Logger.log("  ✗ Interaction attempt #" + attempt + " failed, trying again...");
            Sleep.sleep(400, 700);
        }
        
        Logger.log("❌ Failed to open bank after " + maxAttempts + " attempts");
        return false;
    }
    
    /**
     * Helper method to wait for the bank to open after attempting interaction
     * @return true if bank opened successfully
     */
    private static boolean waitForBankToOpen() {
        int maxWaitAttempts = 40;
        int attempts = 0;
        
        while (!Bank.isOpen() && attempts < maxWaitAttempts) {
            Sleep.sleep(100);
            attempts++;
        }
        
        if (Bank.isOpen()) {
            Logger.log("✓ Bank opened successfully!");
            return true;
        } else {
            Logger.log("✗ Bank did not open after " + maxWaitAttempts + " waits");
            return false;
        }
    }

    /**
     * Deposits all items in the bank
     * @param bankArea The bank area where player should be
     * @return true if items were deposited successfully
     */
    public static boolean depositAllItems(Area bankArea) {
        // Se não estiver na área de banco, caminhar até lá
        if (bankArea == null || !bankArea.contains(Players.getLocal())) {
            Logger.log("Not in bank area, walking to bank...");
            walkToClosestBank(bankArea, true);
            Sleep.sleep(1000, 2000);
            return false; // Retorna false e deixa para a próxima iteração
        }
        
        Logger.log("Player is in bank area. Attempting to open bank...");
        
        // Tentar abrir o banco apenas UMA VEZ
        if (!openBank()) {
            Logger.log("❌ Failed to open bank!");
            Sleep.sleep(500, 800);
            return false;
        }
        
        Logger.log("✓ Bank is open! Depositing items...");
        
        // Depositar todos os itens
        Bank.depositAllItems();
        Logger.log("Called depositAllItems()");
        
        // Aguardar o depósito ser processado
        Sleep.sleep(800, 1200);
        
        // Verificar se o inventário ficou vazio
        int maxWaitAttempts = 15;
        int attempts = 0;
        
        while (Inventory.isFull() && attempts < maxWaitAttempts) {
            Sleep.sleep(300);
            attempts++;
            Logger.log("Waiting for items to be deposited... Attempt " + attempts + "/" + maxWaitAttempts + " | Still full: " + Inventory.isFull());
        }
        
        Logger.log("Inventory is full after deposit: " + Inventory.isFull());
        
        // Fechar o banco
        if (Bank.isOpen()) {
            Bank.close();
            Logger.log("Bank closed");
        }
        Sleep.sleep(500, 800);
        
        boolean success = !Inventory.isFull(); // Sucesso se o inventário NÃO está mais cheio
        Logger.log("Deposit result: " + (success ? "✓ SUCCESS - Inventory empty" : "❌ FAILED - Inventory still full"));
        return success;
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

    /**
     * DEBUG: Lists all nearby game objects to identify the correct bank name
     */
    public static void debugNearbyObjects() {
        Logger.log("=== NEARBY GAME OBJECTS DEBUG ===");
        java.util.List<GameObject> allObjects = GameObjects.all();
        for (GameObject obj : allObjects) {
            if (obj != null && obj.distance() < 20) {
                Logger.log("Object: '" + obj.getName() + "' | Distance: " + obj.distance() + " | OnScreen: " + obj.isOnScreen());
            }
        }
        Logger.log("=== END DEBUG ===");
    }
}
