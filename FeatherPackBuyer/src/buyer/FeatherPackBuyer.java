package buyer;

import org.dreambot.api.methods.map.Area;
import java.awt.Graphics;
import java.util.List;
import java.util.Optional;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankQuantitySelection;
import org.dreambot.api.methods.container.impl.Shop;
import org.dreambot.api.methods.dialogues.Dialogues;

import GUI.State;
import GUI.UI;
import helpers.BankingHelper;
import utils.WalkingUtil;

@ScriptManifest(author = "AllysonGustavo", description = "Buy feather packs for you :)", name = "FeatherPackBuyer", category = Category.MONEYMAKING, version = 1.0)
public class FeatherPackBuyer extends AbstractScript {

    private Area[] FeatherPackArea = {
            new Area(3011, 3229, 3017, 3223),
            new Area(3016, 3222, 3011, 3222),
            new Area(3011, 3221, 3015, 3221),
            new Area(3011, 3220, 3014, 3220)
    };

    private Area BankArea = new Area(3092, 3245, 3092, 3240);
    private State state;
    private long timeBeggone;
    private UI ui;
    private int featherpackpurchased = 0;
    private float estimatedProfit = 0;
    private int spentCoins = 0;
    private int coinsBefore = 0;
    private int coinsAfter = 0;
    private int featherPacksBefore = 0;
    private int featherPacksAfter = 0;
    private int featherpackpurchasedglobal = 0;
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
    public int onLoop() {
        switch (getState()) {
            case WALKINGTOSHOP:
                Logger.log("STATE: WALKINGTOSHOP");
                WalkingUtil.walkToDestination(FeatherPackArea[0].getRandomTile());
                Sleep.sleep(2000, 2500);
                break;
            case BUYING:
                Logger.log("STATE: BUYING");
                items = Inventory.all();
                coins = InventoryUtils.findItemByName(items, "Coins");

                if (!coins.isPresent()) {
                    Logger.log("No coins found in inventory.");
                    return 0;
                }

                while (coins.isPresent() && coins.get().getAmount() > 200) {
                    NPC gerrant = NPCs.closest("Gerrant");
                    if (gerrant != null) {
                        gerrant.interact();
                    } else {
                        Logger.log("Gerrant NPC not found.");
                        return 0;
                    }

                    while (!Dialogues.inDialogue()) {
                        Sleep.sleep(500, 700);
                    }

                    while (Dialogues.inDialogue()) {
                        if (Dialogues.canContinue()) {
                            Dialogues.spaceToContinue();
                            Sleep.sleep(1500, 2000);
                        } else if (Dialogues.getOptions() != null) {
                            Dialogues.chooseOption("Let's see what you've got then.");
                            Sleep.sleep(1500, 2000);
                        }
                    }

                    while (!Shop.isOpen()) {
                        Sleep.sleep(500, 700);
                    }

                    Sleep.sleep(1500, 2000);

                    while (Inventory.emptySlotCount() > 0 && coins.get().getAmount() > 200) {
                        Item featherPack = Shop.get("Feather pack");
                        if (featherPack == null) {
                            Logger.log("Feather pack not found in shop.");
                            Sleep.sleep(500, 700);
                            continue;
                        }

                        Mouse.click(featherPack.getDestination(), true);
                        if (featherPack.getAmount() == 100) {
                            items = Inventory.all();

                            featherPacksBefore = (int) items.stream()
                                    .filter(item -> item != null && "Feather pack".equals(item.getName()))
                                    .count();

                            coins = InventoryUtils.findItemByName(items, "Coins");
                            coinsBefore = coins.map(Item::getAmount).orElse(0);
                            Logger.log(featherPacksBefore + " Feather packs before purchase.");

                            Sleep.sleep(500, 700);
                            Shop.interact(item -> item != null && "Feather pack".equals(item.getName()), "Buy 10");
                            while(featherPacksBefore == featherPacksAfter){
                                items = Inventory.all();

                                featherPacksAfter = (int) items.stream()
                                        .filter(item -> item != null && "Feather pack".equals(item.getName()))
                                        .count();
                                Sleep.sleep(500, 700);
                            }

                            coins = InventoryUtils.findItemByName(items, "Coins");
                            coinsAfter = coins.map(Item::getAmount).orElse(0);
                            Logger.log(featherPacksAfter + " Feather packs after purchase.");

                            featherpackpurchased = featherPacksAfter - featherPacksBefore;
                            spentCoins = coinsBefore - coinsAfter;

                            estimatedProfit += ((featherpackpurchased * 100) * 3) - spentCoins;
                            featherpackpurchasedglobal += featherpackpurchased;
                            Logger.log("Spent coins: " + spentCoins);
                            Logger.log("Feather packs purchased: " + featherpackpurchased);
                            ui.setPurchased(featherpackpurchasedglobal);
                            ui.setEstimatedProfit(estimatedProfit);
                        }
                        Sleep.sleep(500, 1000);
                    }
                    Sleep.sleep(1700, 2200);
                    Shop.close();
                    Sleep.sleep(1500, 2000);
                    Item featherPackInInventory = Inventory.get("Feather pack");
                    if (featherPackInInventory != null) {
                        featherPackInInventory.interact();
                        Sleep.sleepUntil(() -> !Inventory.contains("Feather pack"), 60000);
                    }
                    featherPacksAfter = 0;
                    Sleep.sleep(2000, 3000);
                }
                break;
            case WALKINGTOBANK:
                Logger.log("STATE: WALKINGTOBANK");
                ui.setState(State.WALKINGTOBANK);
                BankingHelper.walkToClosestBank(BankArea, true);
                Sleep.sleep(2000, 2500);
                break;
            case BANKING:
                Logger.log("STATE: BANKING");

                if (!Bank.isOpen()) {
                    NPC banker = NPCs.closest("Banker");
                    if (banker != null) {
                        banker.interact();
                    } else {
                        Logger.log("Banker NPC not found.");
                        return 0;
                    }

                    Sleep.sleepUntil(() -> Dialogues.inDialogue(), 15000);
                    while (Dialogues.inDialogue()) {
                        if (Dialogues.canContinue()) {
                            Dialogues.spaceToContinue();
                            Sleep.sleep(1500, 2000);
                        } else if (Dialogues.getOptions() != null) {
                            Dialogues.chooseOption("I'd like to access my bank account, please.");
                            Sleep.sleep(1500, 2000);
                        }
                    }

                    Sleep.sleepUntil(() -> Bank.isOpen(), 15000);
                }

                if (Inventory.isFull()) {
                    Bank.depositAllItems();
                }

                if (Bank.getDefaultQuantity() != BankQuantitySelection.ALL) {
                    Bank.setDefaultQuantity(BankQuantitySelection.ALL);
                    Sleep.sleep(500);
                }

                if (Bank.contains("Coins")) {
                    int coinCount = Bank.count("Coins");
                    if (coinCount >= 200) {
                        Bank.withdrawAll("Coins");
                        Bank.close();
                    } else {
                        Logger.log("Insufficient coins in bank");
                        stop();
                    }
                } else {
                    Logger.log("Insufficient coins in bank");
                    stop();
                }
                break;
            default:
                Logger.log("UNKNOWN STATE");
                break;
        }
        ui.setState(state);
        return 0;
    }

    public State getState() {
        Logger.log("Get State");
        Player localPlayer = Players.getLocal();
        boolean inFeatherPackArea = false;
        boolean inBankArea = false;
        List<Item> items = Inventory.all();
        Optional<Item> coins = Optional.empty();

        if (localPlayer != null) {
            Tile playerTile = localPlayer.getTile();
            for (Area area : FeatherPackArea) {
                if (area.contains(playerTile)) {
                    inFeatherPackArea = true;
                    break;
                }
            }
        } else {
            Logger.log("Player not found!");
        }

        if (localPlayer != null) {
            Tile playerTile = localPlayer.getTile();
            if (BankArea.contains(playerTile)) {
                inBankArea = true;
            }
        } else {
            Logger.log("Player not found!");
        }

        if (items.stream().anyMatch(item -> item != null && "Coins".equals(item.getName()))) {
            coins = items.stream()
                    .filter(item -> item != null && "Coins".equals(item.getName()))
                    .findFirst();
        }

        if ((!coins.isPresent() || coins.get().getAmount() < 200) && !inBankArea) {
            return State.WALKINGTOBANK;
        } else if ((coins.isPresent() && coins.get().getAmount() >= 200) && !inFeatherPackArea) {
            return State.WALKINGTOSHOP;
        } else if (coins.isPresent() && coins.get().getAmount() >= 200 && inFeatherPackArea) {
            return State.BUYING;
        } else if ((!coins.isPresent() || coins.get().getAmount() < 200) && inBankArea) {
            return State.BANKING;
        }

        return State.DEFAULT;
    }

    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.onPaint(g);
        }
    }

    @Override
    public void onStart() {
        Logger.log("Welcome to FeatherPackBuyer!");
        timeBeggone = System.currentTimeMillis();
        state = getState();
        ui = new UI(timeBeggone, featherpackpurchased, estimatedProfit, state);
    }

    @Override
    public void onExit() {
        Logger.log("Thank you for using FeatherPackBuyer!");
    }
}
