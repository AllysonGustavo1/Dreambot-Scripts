package picker;

import java.awt.Graphics;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.items.GroundItem;

import GUI.State;
import GUI.UI;
import helpers.BankingHelper;
import helpers.ObjectInteractionHelper;
import helpers.WorldHopperHelper;
import utils.PriceUtil;
import utils.WalkingUtil;

@ScriptManifest(author = "Allyson Gustavo", description = "Pick fish food for you xD", name = "FishFoodPicker", category = Category.MONEYMAKING, version = 1, image = "https://i.imgur.com/6KlhpT2.png")
public class FishFoodPicker extends AbstractScript {

    private Area FishFoodArea = new Area(3107, 3357, 3109, 3355, 1);
    private Area BankArea = new Area(3092, 3245, 3093, 3241);
    private State state;
    private UI ui;
    private long timeBeggone;
    private int fishFoodPickedGlobal = 0;
    private float estimatedProfit = 0;
    private GroundItem item = null;
    private int bankingFailureCount = 0;
    private static final int MAX_BANKING_FAILURES = 5;

    @Override
    public int onLoop() {
        switch (getState()) {
            case WALKINGTOPICKAREA:
                Logger.log("State: Walking to Pick Area");
                ui.setState(State.WALKINGTOPICKAREA);
                WalkingUtil.walkToDestination(FishFoodArea.getRandomTile());
                break;
            case PICKING:
                Logger.log("State: Picking");
                ui.setState(State.PICKING);

                Sleep.sleep(500, 700);
                item = GroundItems.closest("Fish food");
                if (item != null) {
                    if (item.interact("Take")) {
                        Sleep.sleep(2000, 3000);
                        fishFoodPickedGlobal++;
                        float pricePerFishFood = getFishFoodPrice();
                        estimatedProfit += pricePerFishFood;
                        ui.setPurchased(fishFoodPickedGlobal);
                        ui.setEstimatedProfit(estimatedProfit);
                        Logger.log("Picked up Fish food! Total: " + fishFoodPickedGlobal + " | Price: " + pricePerFishFood + " GP | Estimated profit: " + estimatedProfit);
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
                BankingHelper.debugNearbyObjects(); // Debug para identificar o nome correto do banco
                Logger.log("Attempting to deposit items... (Failure count: " + bankingFailureCount + "/" + MAX_BANKING_FAILURES + ")");
                if (BankingHelper.depositAllItems(BankArea)) {
                    Logger.log("✓ Successfully deposited all items");
                    bankingFailureCount = 0; // Reset contador
                    Sleep.sleep(2000, 3000); // Wait longer for inventory to fully update
                } else {
                    bankingFailureCount++;
                    Logger.log("✗ Failed to deposit items (Failure #" + bankingFailureCount + ")");
                    if (bankingFailureCount >= MAX_BANKING_FAILURES) {
                        Logger.log("⚠ Banking failed too many times, returning to pick area...");
                        bankingFailureCount = 0;
                    }
                    Sleep.sleep(1000, 1500);
                }
                break;
            case DEFAULT:
                Logger.log("State: UNKNOWN STATE");
                ui.setState(State.DEFAULT);
                return 0;
        }
        return 0;
    }

    private State getState() {
        // Se inventário cheio, vai para banco
        if (Inventory.isFull() && BankArea != null && !BankArea.contains(Players.getLocal())) {
            return State.WALKINGTOBANK;
        } else if (Inventory.isFull() && BankArea != null && BankArea.contains(Players.getLocal()) && bankingFailureCount < MAX_BANKING_FAILURES) {
            return State.BANKING;
        }

        // Se muitas falhas de banking, volta para a área de coleta para resetar
        if (bankingFailureCount >= MAX_BANKING_FAILURES) {
            if (!FishFoodArea.contains(Players.getLocal())) {
                return State.WALKINGTOPICKAREA;
            }
            return State.PICKING;
        }

        // Se inventário não está cheio
        if (!Inventory.isFull()) {
            // Se não está na área do fish food, anda até lá
            if (!FishFoodArea.contains(Players.getLocal())) {
                return State.WALKINGTOPICKAREA;
            }
            // Se está na área do fish food, pega
            else if (FishFoodArea.contains(Players.getLocal())) {
                return State.PICKING;
            }
        }

        return State.DEFAULT;
    }

    /**
     * Gets the current Fish food price using LivePrices API
     * @return current fish food price in GP
     */
    private float getFishFoodPrice() {
        return PriceUtil.getItemPrice("Fish food", 10.0f);
    }

    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.onPaint(g);
        }
    }

    @Override
    public void onStart() {
        Logger.log("Welcome to FishFoodPicker!");
        timeBeggone = System.currentTimeMillis();
        state = getState();
        ui = new UI(timeBeggone, fishFoodPickedGlobal, estimatedProfit, state);
    }

    @Override
    public void onExit() {
        Logger.log("Thanks for using FishFoodPicker!");
    }
}
