package picker;

import java.awt.Graphics;
import java.util.ArrayList;

import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;

import GUI.State;
import GUI.UI;
import helpers.BankingHelper;
import helpers.WorldHopperHelper;
import utils.PriceUtil;
import utils.WalkingUtil;

@ScriptManifest(author = "Allyson Gustavo", description = "Pick monk's robe for you xD", name = "MonksRobePicker", category = Category.MONEYMAKING, version = 1)
public class MonksRobePicker extends AbstractScript {

    private Area RobeArea = new Area(3058, 3487, 3058, 3488, 1);
    private Area BankArea;
    private State state;
    private UI ui;
    private long timeBeggone;
    private int monksrobepickedglobal = 0;
    private float estimatedProfit = 0;
    private GroundItem item = null;

    @Override
    public int onLoop() {
        switch (getState()) {
            case WALKINGTOROBE:
                Logger.log("State: Walking to Robe");
                ui.setState(State.WALKINGTOROBE);
                WalkingUtil.walkToDestination(RobeArea.getRandomTile());
                break;
            case PICKING:
                Logger.log("State: Picking");
                ui.setState(State.PICKING);

                Sleep.sleep(500, 700);
                item = GroundItems.closest("Monk's robe top");
                if (item != null) {
                    if (item.interact("Take")) {
                        Sleep.sleep(2000, 3000);
                        // Update counters and profit immediately after picking up
                        monksrobepickedglobal++;
                        float pricePerMonksRobeTop = getMonksRobeTopPrice();
                        estimatedProfit += pricePerMonksRobeTop;
                        ui.setPurchased(monksrobepickedglobal);
                        ui.setEstimatedProfit(estimatedProfit);
                        Logger.log("Picked up Monk's robe top! Total: " + monksrobepickedglobal + " | Price: " + pricePerMonksRobeTop + " GP | Estimated profit: " + estimatedProfit);
                    }
                }

                Sleep.sleep(500, 700);
                item = GroundItems.closest("Monk's robe");
                if (item != null) {
                    if (item.interact("Take")) {
                        Sleep.sleep(2000, 3000);
                        // Update counters and profit immediately after picking up
                        monksrobepickedglobal++;
                        float pricePerMonksRobe = getMonksRobePrice();
                        estimatedProfit += pricePerMonksRobe;
                        ui.setPurchased(monksrobepickedglobal);
                        ui.setEstimatedProfit(estimatedProfit);
                        Logger.log("Picked up Monk's robe! Total: " + monksrobepickedglobal + " | Price: " + pricePerMonksRobe + " GP | Estimated profit: " + estimatedProfit);
                    }
                } else {
                    WorldHopperHelper.hopToRandomWorld();
                }
                break;
            case WALKINGTOBANK:
                Logger.log("State: Walking to Bank");
                ui.setState(State.WALKINGTOBANK);
                BankingHelper.walkToClosestBank(BankArea, true);
                break;
            case BANKING:
                Logger.log("State: Banking");
                ui.setState(State.BANKING);
                BankingHelper.depositAllItems(BankArea);
                break;
            case DEFAULT:
                Logger.log("State: UNKNOWN STATE");
                ui.setState(State.DEFAULT);
                return 0;
        }
        return 0;
    }


    private State getState() {
        if (Inventory.isFull() && BankArea != null && !BankArea.contains(Players.getLocal())) {
            return State.WALKINGTOBANK;
        } else if (!Inventory.isFull() && !RobeArea.contains(Players.getLocal())) {
            return State.WALKINGTOROBE;
        } else if (!Inventory.isFull() && RobeArea.contains(Players.getLocal())) {
            return State.PICKING;
        } else if (Inventory.isFull() && BankArea != null && BankArea.contains(Players.getLocal())) {
            return State.BANKING;
        }

        return State.DEFAULT;
    }

    /**
     * Gets the current Monk's robe top price using LivePrices API
     * @return current monk's robe top price in GP
     */
    private float getMonksRobeTopPrice() {
        return PriceUtil.getItemPrice("Monk's robe top", 54.0f);
    }

    /**
     * Gets the current Monk's robe price using LivePrices API
     * @return current monk's robe price in GP
     */
    private float getMonksRobePrice() {
        return PriceUtil.getItemPrice("Monk's robe", 60.0f);
    }

    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.onPaint(g);
        }
    }

    @Override
    public void onStart() {
        Logger.log("Welcome to MonksRobePicker!");
        timeBeggone = System.currentTimeMillis();
        state = getState();
        ui = new UI(timeBeggone, monksrobepickedglobal, estimatedProfit, state);
        BankLocation closestBank = Bank.getClosestBankLocation(true);
        if (closestBank != null) {
            BankArea = closestBank.getArea(5);
        } else {
            Logger.log("No bank found!");
        }
    }

    @Override
    public void onExit() {
        Logger.log("Thanks for using MonksRobePicker!");
    }
}
