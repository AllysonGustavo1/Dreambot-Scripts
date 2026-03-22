package scatter;

import java.awt.Graphics;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.Item;

import GUI.State;
import GUI.UI;
import helpers.BankingHelper;
import utils.WalkingUtil;

@ScriptManifest(author = "AllysonGustavo", description = "Scatter Vile ashes automatically", name = "VileAshesScatter", category = Category.PRAYER, version = 1.0)
public class VileAshesScatter extends AbstractScript {

    private static final String ITEM_NAME = "Vile ashes";
    private static final Area GE_AREA = new Area(3162, 3487, 3167, 3485);
    private static final Area GE_BANK_AREA = new Area(3161, 3493, 3171, 3483);
    private static final int ACTION_SHORT_MIN = 250;
    private static final int ACTION_SHORT_MAX = 550;
    private static final int ACTION_MEDIUM_MIN = 700;
    private static final int ACTION_MEDIUM_MAX = 1200;
    private static final int WALK_TRANSITION_MIN = 900;
    private static final int WALK_TRANSITION_MAX = 1600;

    private State state;
    private UI ui;
    private long startTime;
    private int ashesScattered = 0;
    private boolean shouldStop = false;

    @Override
    public void onStart() {
        Logger.log("Welcome to VileAshesScatter!");
        startTime = System.currentTimeMillis();
        state = State.DEFAULT;
        ui = new UI(startTime, ashesScattered, state);
    }

    @Override
    public int onLoop() {
        state = getState();
        ui.setState(state);

        switch (state) {
            case WALKINGTOBANK:
                Logger.log("STATE: WALKINGTOBANK");
                BankingHelper.walkToClosestBank(GE_BANK_AREA, true);
                Sleep.sleep(WALK_TRANSITION_MIN, WALK_TRANSITION_MAX);
                break;

            case BANKING:
                Logger.log("STATE: BANKING");
                handleBanking();
                break;

            case WALKINGTOGE:
                Logger.log("STATE: WALKINGTOGE");
                WalkingUtil.walkToDestination(GE_AREA.getRandomTile());
                Sleep.sleep(WALK_TRANSITION_MIN, WALK_TRANSITION_MAX);
                break;

            case SCATTERING:
                Logger.log("STATE: SCATTERING");
                scatterOneAshes();
                break;

            case STOPPING:
                Logger.log("STATE: STOPPING - No Vile ashes in bank.");
                stop();
                break;

            default:
                Logger.log("STATE: DEFAULT");
                break;
        }

        return Calculations.random(120, 260);
    }

    private State getState() {
        if (shouldStop) {
            return State.STOPPING;
        }

        if (Inventory.contains(ITEM_NAME)) {
            if (GE_AREA.contains(Players.getLocal())) {
                return State.SCATTERING;
            }
            return State.WALKINGTOGE;
        }

        if (!GE_BANK_AREA.contains(Players.getLocal())) {
            return State.WALKINGTOBANK;
        }

        return State.BANKING;
    }

    private void handleBanking() {
        if (!Bank.isOpen()) {
            if (!openBankWithBanker()) {
                Sleep.sleep(300, 600);
                return;
            }

            Sleep.sleep(ACTION_SHORT_MIN, ACTION_SHORT_MAX);
        }

        if (!Inventory.isEmpty()) {
            Bank.depositAllItems();
            Sleep.sleepUntil(Inventory::isEmpty, 2500);
            Sleep.sleep(ACTION_MEDIUM_MIN, ACTION_MEDIUM_MAX);
        }

        if (!Bank.contains(ITEM_NAME)) {
            shouldStop = true;
            return;
        }

        Bank.withdrawAll(ITEM_NAME);
        Sleep.sleepUntil(() -> Inventory.contains(ITEM_NAME), 2500);
        Sleep.sleep(ACTION_MEDIUM_MIN, ACTION_MEDIUM_MAX);
        Bank.close();
        Sleep.sleep(ACTION_SHORT_MIN, ACTION_SHORT_MAX);
    }

    private boolean openBankWithBanker() {
        NPC banker = NPCs.closest(n -> n != null && "Banker".equals(n.getName()) && GE_BANK_AREA.contains(n.getTile()));
        if (banker == null) {
            banker = NPCs.closest("Banker");
        }

        if (banker == null) {
            Logger.log("No Banker found nearby.");
            return false;
        }

        if (!banker.interact("Bank")) {
            return false;
        }

        Sleep.sleep(ACTION_SHORT_MIN, ACTION_SHORT_MAX);

        return Sleep.sleepUntil(Bank::isOpen, 3500);
    }

    private void scatterOneAshes() {
        Item ashes = Inventory.get(ITEM_NAME);
        if (ashes == null) {
            return;
        }

        int before = Inventory.count(ITEM_NAME);
        if (ashes.interact("Scatter")) {
            Sleep.sleepUntil(() -> Inventory.count(ITEM_NAME) < before, 1800);
            int after = Inventory.count(ITEM_NAME);
            int scatteredNow = Math.max(0, before - after);

            if (scatteredNow > 0) {
                ashesScattered += scatteredNow;
                ui.setAshesScattered(ashesScattered);
                Sleep.sleep(200, 1200);
            }
        }
    }

    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.onPaint(g);
        }
    }

    @Override
    public void onExit() {
        Logger.log("Thanks for using VileAshesScatter! Total scattered: " + ashesScattered);
    }
}
