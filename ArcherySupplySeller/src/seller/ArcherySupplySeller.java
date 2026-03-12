package seller;

import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.map.Area;
import java.awt.Graphics;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.container.impl.Shop;

import GUI.State;
import GUI.UI;
import helpers.WorldHopperHelper;
import utils.PriceUtil;
import utils.WalkingUtil;

@ScriptManifest(author = "AllysonGustavo", description = "Buy archery supplies and sell at GE :)", name = "ArcherySupplySeller", category = Category.MONEYMAKING, version = 1.0)
public class ArcherySupplySeller extends AbstractScript {

    private static final int MIN_SLEEP_MEDIUM = 500;
    private static final int MAX_SLEEP_MEDIUM = 800;
    private static final int MIN_SLEEP_LONG = 1000;
    private static final int MAX_SLEEP_LONG = 1500;
    private static final int MIN_SLEEP_WALKING = 2000;
    private static final int MAX_SLEEP_WALKING = 3000;
    private static final int MIN_SLEEP_WORLD_HOP = 3000;
    private static final int MAX_SLEEP_WORLD_HOP = 5000;

    private final Area brianArea = new Area(2953, 3205, 2960, 3202);
    
    private UI ui;
    private boolean isHoppingWorld = false;
    private int suppliesSold = 0;
    private int totalProfit = 0;
    private int currentWorldIndex = 0;

    private final String[] REQUIRED_ITEMS = {
        "Mithril arrow",
        "Adamant arrow", 
        "Oak longbow",
        "Willow shortbow",
        "Willow longbow",
        "Maple shortbow",
        "Maple longbow"
    };

    private final int[] F2P_WORLDS = {
        308, 316, 326, 335, 379, 380, 382, 383, 384, 397, 398, 399, 
        417, 418, 430, 431, 433, 434, 435, 436, 437, 451, 452, 
        453, 454, 455, 456, 469, 475, 476, 483, 497, 498, 499, 500, 501, 537, 544, 545, 546, 547, 552, 553, 554, 555, 571, 575
    };

    // Valores fixos que o Brian paga por 10 unidades de cada item
    private final Map<String, Integer> BRIAN_PRICES = new HashMap<String, Integer>() {{
        put("Adamant arrow", 444);
        put("Mithril arrow", 174);
        put("Willow longbow", 1788);
        put("Oak longbow", 892);
        put("Willow shortbow", 1120);
        put("Maple shortbow", 2240);
        put("Maple longbow", 3580);
    }};

    List<Item> items = Inventory.all();
    Optional<Item> coins = Optional.empty();

    public static class InventoryUtils {
        public static Optional<Item> findItemByName(List<Item> items, String name) {
            if (items != null) {
                return items.stream()
                            .filter(item -> item != null && name.equals(item.getName()))
                            .findFirst();
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onStart() {
        ui = new UI();
        
        // Detectar o mundo atual e definir o índice correto
        int currentWorld = WorldHopperHelper.getCurrentWorld();
        currentWorldIndex = findWorldIndex(currentWorld);
        Logger.log("Current world: " + currentWorld + " (Index: " + currentWorldIndex + ")");
        
        if (!hasRequiredItems()) {
            Logger.log("ERROR: You need at least 10 of one of these items to start:");
            for (String item : REQUIRED_ITEMS) {
                Logger.log("- " + item);
            }
            Logger.log("Script will stop in 5 seconds...");
            Sleep.sleep(5000);
            stop();
            return;
        }
        
        Logger.log("Archery Supply Seller started!");
        Logger.log("Required items found in inventory!");
    }
    
    private int findWorldIndex(int worldNumber) {
        for (int i = 0; i < F2P_WORLDS.length; i++) {
            if (F2P_WORLDS[i] == worldNumber) {
                return i;
            }
        }
        // Se o mundo atual não estiver na lista F2P, começar do primeiro
        Logger.log("Current world " + worldNumber + " not in F2P list, starting from first F2P world");
        return 0;
    }

    private boolean hasRequiredItems() {
        List<Item> currentItems = Inventory.all();
        
        for (String requiredItem : REQUIRED_ITEMS) {
            Optional<Item> item = InventoryUtils.findItemByName(currentItems, requiredItem);
            if (item.isPresent() && item.get().getAmount() >= 10) {
                Logger.log("Found " + item.get().getAmount() + "x " + requiredItem);
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void onExit() {
        Logger.log("Archery Supply Seller stopped!");
    }

    private State getState() {
        if (isHoppingWorld) {
            return State.HOPPINGWORLD;
        }

        items = Inventory.all();
        coins = InventoryUtils.findItemByName(items, "Coins");
        
        boolean hasRequiredItems = items.stream()
            .anyMatch(item -> item != null && 
                java.util.Arrays.asList(REQUIRED_ITEMS).contains(item.getName()) &&
                item.getAmount() >= 10);

        if (hasRequiredItems && !brianArea.contains(Players.getLocal())) {
            return State.WALKINGTOBRIANSARCHERY;
        }
        
        if (hasRequiredItems && brianArea.contains(Players.getLocal())) {
            return State.SELLINGSUPPLIES;
        }

        return State.DEFAULT;
    }

    private void hopToRandomWorld() {
        isHoppingWorld = true;
        Logger.log("Hopping to next F2P world...");
        
        currentWorldIndex = (currentWorldIndex + 1) % F2P_WORLDS.length;
        int targetWorld = F2P_WORLDS[currentWorldIndex];
        
        Logger.log("Hopping to world " + targetWorld);
        WorldHopperHelper.hopToWorld(targetWorld);
        Sleep.sleep(MIN_SLEEP_WORLD_HOP, MAX_SLEEP_WORLD_HOP);

        isHoppingWorld = false;
    }

    @Override
    public int onLoop() {
        if (ui != null) {
            ui.setCurrentState(getState().toString());
        }
        
        switch (getState()) {
            case WALKINGTOBRIANSARCHERY:
                Logger.log("STATE: WALKINGTOBRIANSARCHERY - Going to Brian's archery shop");
                if (!brianArea.contains(Players.getLocal())) {
                    WalkingUtil.walkToDestination(brianArea.getRandomTile());
                    Sleep.sleep(MIN_SLEEP_WALKING, MAX_SLEEP_WALKING);
                }
                break;
                
            case SELLINGSUPPLIES:
                Logger.log("STATE: SELLINGSUPPLIES - Selling to Brian");
                sellToBrian();
                break;
                
            case HOPPINGWORLD:
                Logger.log("STATE: HOPPINGWORLD");
                Sleep.sleep(MIN_SLEEP_WORLD_HOP, MAX_SLEEP_WORLD_HOP);
                break;
                
            case DEFAULT:
                Logger.log("STATE: DEFAULT - Waiting...");
                Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                break;
        }
        
        return 600;
    }

    private void sellToBrian() {
        NPC brian = NPCs.closest("Brian");
        if (brian != null && brian.interact("Trade")) {
            Logger.log("Clicked trade with Brian, waiting for shop to open...");
            
            // Aguardar até 10 segundos para a loja abrir
            long startTime = System.currentTimeMillis();
            long timeout = 10000; // 10 segundos
            
            while (!Shop.isOpen() && (System.currentTimeMillis() - startTime) < timeout) {
                Sleep.sleep(200, 400);
            }
            
            if (Shop.isOpen()) {
                Logger.log("Brian's shop is open, checking stock levels...");
                
                // Aguardar um pouco antes de começar a vender (mais realista)
                Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                
                // Verificar e vender todos os itens que atendem aos critérios
                // Mithril arrow: inventário >= 10 E loja == 1000
                if (hasInventoryItem("Mithril arrow", 10) && getShopItemAmount("Mithril arrow") == 1000) {
                    sellItemToBrian("Mithril arrow");
                }
                
                // Adamant arrow: inventário >= 10 E loja == 800  
                if (hasInventoryItem("Adamant arrow", 10) && getShopItemAmount("Adamant arrow") == 800) {
                    sellItemToBrian("Adamant arrow");
                }
                
                // Oak longbow: inventário >= 10 E loja == 4
                if (hasInventoryItem("Oak longbow", 10) && getShopItemAmount("Oak longbow") == 4) {
                    sellItemToBrian("Oak longbow");
                }
                
                // Willow shortbow: inventário >= 10 E loja == 3
                if (hasInventoryItem("Willow shortbow", 10) && getShopItemAmount("Willow shortbow") == 3) {
                    sellItemToBrian("Willow shortbow");
                }
                
                // Willow longbow: inventário >= 10 E loja == 3
                if (hasInventoryItem("Willow longbow", 10) && getShopItemAmount("Willow longbow") == 3) {
                    sellItemToBrian("Willow longbow");
                }
                
                // Maple shortbow: inventário >= 10 E loja == 2
                if (hasInventoryItem("Maple shortbow", 10) && getShopItemAmount("Maple shortbow") == 2) {
                    sellItemToBrian("Maple shortbow");
                }
                
                // Maple longbow: inventário >= 10 E loja == 2
                if (hasInventoryItem("Maple longbow", 10) && getShopItemAmount("Maple longbow") == 2) {
                    sellItemToBrian("Maple longbow");
                }
                
                // Fechar loja e pular mundo
                Logger.log("Finished checking all items, closing shop...");
                Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM); // Delay antes de fechar
                Shop.close();
                Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG); // Delay maior após fechar
                hopToRandomWorld();
            } else {
                Logger.log("Shop didn't open within 10 seconds, hopping worlds...");
                hopToRandomWorld();
            }
        } else {
            Logger.log("Brian not found or couldn't interact");
            hopToRandomWorld();
        }
    }
    
    private boolean hasInventoryItem(String itemName, int minAmount) {
        Optional<Item> item = InventoryUtils.findItemByName(Inventory.all(), itemName);
        return item.isPresent() && item.get().getAmount() >= minAmount;
    }
    
    private int getShopItemAmount(String itemName) {
        if (Shop.isOpen()) {
            Optional<Item> shopItem = Shop.all().stream()
                .filter(item -> item != null && itemName.equals(item.getName()))
                .findFirst();
            return shopItem.isPresent() ? shopItem.get().getAmount() : 0;
        }
        return 0;
    }
    
    private void sellItemToBrian(String itemName) {
        Optional<Item> item = InventoryUtils.findItemByName(Inventory.all(), itemName);
        if (item.isPresent()) {
            Logger.log("Selling conditions met for " + itemName);
            if (item.get().interact("Sell 10")) {
                Logger.log("Sold 10x " + itemName + " to Brian");
                suppliesSold += 10;
                
                // Calcular profit dinamicamente: (Preço GE * 10) - Valor que Brian paga
                int profit = calculateProfit(itemName);
                
                totalProfit += profit;
                Logger.log("Profit from this sale: " + profit + " GP (Total profit: " + totalProfit + " GP)");
                
                ui.setSuppliesSold(suppliesSold);
                ui.setProfit(totalProfit);
                Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
            }
        }
    }
    
    private int calculateProfit(String itemName) {
        int gePrice = (int) PriceUtil.getItemPrice(itemName, 0);
        int geTotal = gePrice * 10;

        Integer brianPrice = BRIAN_PRICES.get(itemName);
        if (brianPrice != null) {
            int profit = brianPrice - geTotal;
            Logger.log("GE Price: " + gePrice + " GP each (" + geTotal + " GP for 10) - Brian pays: " + brianPrice + " GP = Profit: " + profit + " GP");
            return profit;
        } else {
            Logger.log("Brian price not found for " + itemName);
            return 0;
        }
    }
    
    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.onPaint(g);
        }
    }
}
