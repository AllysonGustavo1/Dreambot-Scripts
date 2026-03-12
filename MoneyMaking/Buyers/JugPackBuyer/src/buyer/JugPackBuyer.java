package buyer;

import GUI.State;
import org.dreambot.api.Client;
import org.dreambot.api.methods.map.Area;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.wrappers.widgets.WidgetChild;

import GUI.State;
import GUI.UI;
import helpers.WorldHopperHelper;
import utils.WalkingUtil;

@ScriptManifest(author = "AllysonGustavo", description = "Buy jug packs from Fortunato :)", name = "JugPackBuyer", category = Category.MONEYMAKING, version = 1.0)
public class JugPackBuyer extends AbstractScript {

    private static final int MIN_SLEEP_SHORT = 300;
    private static final int MAX_SLEEP_SHORT = 600;
    private static final int MIN_SLEEP_MEDIUM = 500;
    private static final int MAX_SLEEP_MEDIUM = 800;
    private static final int MIN_SLEEP_LONG = 1000;
    private static final int MAX_SLEEP_LONG = 1500;
    private static final int MIN_SLEEP_EXTRA_LONG = 1500;
    private static final int MAX_SLEEP_EXTRA_LONG = 2500;
    private static final int MIN_SLEEP_WALKING = 2000;
    private static final int MAX_SLEEP_WALKING = 3000;
    private static final int MIN_SLEEP_WORLD_HOP = 3000;
    private static final int MAX_SLEEP_WORLD_HOP = 5000;

    private Area fortunatoArea = new Area(3074, 3255, 3086, 3245);
    private UI ui;
    private boolean isHoppingWorld = false;
    private int jugpackpurchased = 0;
    private float estimatedProfit = 0;
    private int spentCoins = 0;
    private int coinsBefore = 0;
    private int coinsAfter = 0;
    private int jugPacksBefore = 0;
    private int jugPacksAfter = 0;
    private int jugpackpurchasedglobal = 0;
    private Random random = new Random();

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

    private String getRandomBuyOption() {
        String[] buyOptions = {"Buy 5", "Buy 10", "Buy 50"};
        return buyOptions[random.nextInt(buyOptions.length)];
    }

    @Override
    public int onLoop() {
        if (ui != null) {
            ui.setCurrentState(getState().toString());
        }

        switch (getState()) {
            case WALKINGTOFORTUNATOAREA:
                Logger.log("STATE: WALKINGTOFORTUNATOAREA");
                if (!fortunatoArea.contains(Players.getLocal())) {
                    WalkingUtil.walkToDestination(fortunatoArea.getRandomTile());
                    Sleep.sleep(MIN_SLEEP_WALKING, MAX_SLEEP_WALKING);
                }
                break;

            case TRADING:
                Logger.log("STATE: TRADING");
                items = Inventory.all();
                coins = InventoryUtils.findItemByName(items, "Coins");

                if (!coins.isPresent() || coins.get().getAmount() < 726) {
                    return -1;
                }

                while (coins.isPresent() && coins.get().getAmount() >= 726) {
                    NPC fortunato = NPCs.closest("Fortunato");
                    if (fortunato != null && fortunato.isOnScreen()) {
                        Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);

                        if (fortunato.interact()) {
                            Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                        } else {
                            Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                            continue;
                        }
                    } else {
                        Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                        continue;
                    }

                    int dialogueWait = 0;
                    while (!Dialogues.inDialogue() && dialogueWait < 20) {
                        Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                        dialogueWait++;
                    }

                    if (!Dialogues.inDialogue()) {
                        continue;
                    }

                    while (Dialogues.inDialogue()) {
                        if (Dialogues.canContinue()) {
                            Dialogues.spaceToContinue();
                            Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                        } else if (Dialogues.getOptions() != null && Dialogues.getOptions().length > 0) {
                            Dialogues.chooseOption(1);
                            Sleep.sleep(MIN_SLEEP_EXTRA_LONG, MAX_SLEEP_EXTRA_LONG);
                        } else {
                            Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                        }
                    }

                    int shopWait = 0;
                    while (!Shop.isOpen() && shopWait < 30) {
                        Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                        shopWait++;
                    }

                    if (!Shop.isOpen()) {
                        continue;
                    }

                    Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);

                    Item jugPack = Shop.get("Empty jug pack");
                    if (jugPack == null || jugPack.getAmount() < 5) {
                        Shop.close();
                        Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                        hopToRandomWorld();
                        break;
                    }

                    items = Inventory.all();
                    jugPacksBefore = (int) items.stream()
                            .filter(item -> item != null && "Empty jug pack".equals(item.getName()))
                            .count();

                    coins = InventoryUtils.findItemByName(items, "Coins");
                    coinsBefore = coins.map(Item::getAmount).orElse(0);

                    Sleep.sleep(MIN_SLEEP_SHORT, MAX_SLEEP_SHORT);

                    Item shopJugPack = Shop.get("Empty jug pack");
                    if (shopJugPack != null) {

                        if (Shop.isOpen()) {
                            Item jugPackInShop = Shop.get("Empty jug pack");

                            if (jugPackInShop != null) {
                                Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_LONG);

                                WidgetChild jugPackWidget = Widgets.getWidget(300).getChild(16).getChild(3);

                                if (jugPackWidget != null && jugPackWidget.isVisible()) {
                                    int widgetX = jugPackWidget.getX() + (jugPackWidget.getWidth() / 2);
                                    int widgetY = jugPackWidget.getY() + (jugPackWidget.getHeight() / 2);

                                    int randomOffsetX = random.nextInt(10) - 5;
                                    int randomOffsetY = random.nextInt(10) - 5;
                                    int randomX = widgetX + randomOffsetX;
                                    int randomY = widgetY + randomOffsetY;

                                    java.awt.Rectangle clickRect = new java.awt.Rectangle(randomX, randomY, 1, 1);

                                    if (Mouse.click(clickRect, true)) {
                                        Sleep.sleep(MIN_SLEEP_SHORT, MAX_SLEEP_MEDIUM);

                                        String buyOption = getRandomBuyOption();
                                        if (jugPackWidget.interact(buyOption)) {
                                        } else {
                                            Shop.interact(item -> item != null && "Empty jug pack".equals(item.getName()), buyOption);
                                        }
                                    } else {
                                        String buyOption = getRandomBuyOption();
                                        jugPackWidget.interact(buyOption);
                                    }
                                } else {
                                    String buyOption = getRandomBuyOption();
                                    Shop.interact(item -> item != null && "Empty jug pack".equals(item.getName()), buyOption);
                                }

                                Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                            } else {
                                String buyOption = getRandomBuyOption();
                                Shop.interact(item -> item != null && "Empty jug pack".equals(item.getName()), buyOption);
                                Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                            }
                        } else {
                            continue;
                        }

                        Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                    } else {
                        Shop.close();
                        Sleep.sleep(MIN_SLEEP_MEDIUM, MAX_SLEEP_MEDIUM);
                        hopToRandomWorld();
                        break;
                    }

                    int attempts = 0;
                    while(jugPacksBefore == jugPacksAfter && attempts < 20){
                        items = Inventory.all();
                        jugPacksAfter = (int) items.stream()
                                .filter(item -> item != null && "Empty jug pack".equals(item.getName()))
                                .count();
                        Sleep.sleep(MIN_SLEEP_SHORT, MAX_SLEEP_MEDIUM);
                        attempts++;
                    }

                    coins = InventoryUtils.findItemByName(items, "Coins");
                    coinsAfter = coins.map(Item::getAmount).orElse(0);

                    jugpackpurchased = jugPacksAfter - jugPacksBefore;
                    spentCoins = coinsBefore - coinsAfter;

                    if (jugpackpurchased > 0) {
                        int actualPurchased = 5;
                        jugpackpurchasedglobal += actualPurchased;
                        float currentProfit = actualPurchased * 154.8f;
                        estimatedProfit += currentProfit;
                        if (ui != null) {
                            ui.updateStats(actualPurchased, currentProfit, spentCoins);
                        }
                    }

                    Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);

                    Shop.close();
                    Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);

                    if (Inventory.emptySlotCount() <= 1) {
                        break;
                    }

                    hopToRandomWorld();
                    break;
                }
                break;

            case OPENINGJUGPACKS:
                Logger.log("STATE: OPENINGJUGPACKS");
                items = Inventory.all();

                Optional<Item> firstJugPack = items.stream()
                        .filter(item -> item != null && "Empty jug pack".equals(item.getName()))
                        .findFirst();

                if (firstJugPack.isPresent()) {
                    firstJugPack.get().interact("Open");
                    Sleep.sleep(MIN_SLEEP_WALKING, MAX_SLEEP_WALKING);

                    int openingAttempts = 0;
                    while (openingAttempts < 30) {
                        items = Inventory.all();
                        boolean hasJugPacks = items.stream()
                                .anyMatch(item -> item != null && "Empty jug pack".equals(item.getName()));

                        if (!hasJugPacks) {
                            Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                            break;
                        }

                        Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                        openingAttempts++;
                    }
                } else {
                    Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                }
                break;

            case HOPPINGWORLD:
                Logger.log("STATE: HOPPINGWORLD");
                Sleep.sleep(MIN_SLEEP_WORLD_HOP, MAX_SLEEP_WORLD_HOP);
                break;

            case DEFAULT:
                Logger.log("STATE: DEFAULT");
                Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
                break;
        }
        return 600;
    }

    private void hopToRandomWorld() {
        try {
            isHoppingWorld = true;
            WorldHopperHelper.hopToRandomWorld();
            Sleep.sleep(MIN_SLEEP_LONG, MAX_SLEEP_LONG);
            isHoppingWorld = false;
        } catch (Exception e) {
            isHoppingWorld = false;
        }
    }

    private State getState() {
        items = Inventory.all();

        if (isHoppingWorld) {
            return State.HOPPINGWORLD;
        }

        boolean hasJugPacks = items.stream()
                .anyMatch(item -> item != null && "Empty jug pack".equals(item.getName()));

        if (hasJugPacks && Inventory.emptySlotCount() <= 1) {
            return State.OPENINGJUGPACKS;
        }

        if (!fortunatoArea.contains(Players.getLocal())) {
            return State.WALKINGTOFORTUNATOAREA;
        }

        if (fortunatoArea.contains(Players.getLocal())) {
            return State.TRADING;
        }

        return State.DEFAULT;
    }

    @Override
    public void onStart() {
        ui = new UI();
    }

    @Override
    public void onExit() {
    }

    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.onPaint(g);
        }
    }
}
