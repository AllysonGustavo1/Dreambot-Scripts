package buyer;

import org.dreambot.api.Client;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.map.Area;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.methods.emotes.Emote;
import org.dreambot.api.methods.emotes.Emotes;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.wrappers.items.GroundItem;

import GUI.State;
import GUI.UI;
import helpers.BankingHelper;
import helpers.WorldHopperHelper;
import utils.PriceUtil;
import utils.WalkingUtil;

@ScriptManifest(author = "AllysonGustavo", description = "Pick ruby rings for you :)", name = "RubyRingPicker", category = Category.MONEYMAKING, version = 1.0)
public class RubyRingPicker extends AbstractScript {

    private static final int SHORT_SLEEP_MIN = 700;
    private static final int SHORT_SLEEP_MAX = 1000;
    private static final int MEDIUM_SLEEP_MIN = 1500;
    private static final int MEDIUM_SLEEP_MAX = 2000;
    private static final int LONG_SLEEP_MIN = 4000;
    private static final int LONG_SLEEP_MAX = 5000;

    private Area BankArea = new Area(3180, 3445, 3183, 3435);
    private Area GateAreaOutside = new Area(3191, 9825, 3192, 9825);
    private Area GateAreaInside = new Area(3191, 9824, 3192, 9824);
    private Area RubyRingsArea = new Area(3194, 9821, 3196, 9823); // Expanded to cover area around ruby ring spawn
    private Area CollectionArea = new Area(3186, 9824, 3196, 9818); // Expanded to include ruby rings area
    private Area GateSpecificArea = new Area(3191, 9824, 3192, 9824); // Specific gate area for emotes
    
    private Emote[] emoteSequence = {
        Emote.PANIC, Emote.NO, Emote.BECKON, Emote.LAUGH, Emote.SHRUG, 
        Emote.CRY, Emote.SPIN, Emote.YES, Emote.THINK, Emote.DANCE, 
        Emote.BLOW_KISS, Emote.WAVE, Emote.BOW, Emote.PANIC, Emote.HEADBANG, 
        Emote.JUMP_FOR_JOY, Emote.ANGRY
    };
    private int currentEmoteIndex = 0;
    private int gateEmoteIndex = 0;
    private boolean emotesCompleted = false;
    private boolean emotesAtGateCompleted = false;
    
    private State state;
    private long timeBeggone;
    private UI ui;
    private int rubyRingsPicked = 0;
    private float estimatedProfit = 0;
    private int rubyRingsPickedGlobal = 0;
    private boolean dialoguesHandled = false;
    private GroundItem item = null;
    List<Item> items = Inventory.all();

    @Override
    public int onLoop() {
        try {
            Logger.log("onLoop() started");
            state = getState();
            Logger.log("Current state: " + state);
            
            if (state == null) {
                Logger.log("State is null, returning default");
                return 600;
            }
            
            switch (state) {
                case WALKINGTOGATE:
                    Logger.log("STATE: WALKINGTOGATE");
                    ui.setState(State.WALKINGTOGATE);
                    WalkingUtil.walkToDestination(GateAreaOutside.getRandomTile());
                    Sleep.sleep(MEDIUM_SLEEP_MIN, MEDIUM_SLEEP_MAX);
                    break;
                case DOINGEMOTES:
                    Logger.log("STATE: DOINGEMOTES");
                    ui.setState(State.DOINGEMOTES);
                    if (!emotesCompleted && currentEmoteIndex < emoteSequence.length) {
                        Emote currentEmote = emoteSequence[currentEmoteIndex];
                        Logger.log("Doing emote: " + currentEmote.name() + " (" + (currentEmoteIndex + 1) + "/" + emoteSequence.length + ")");
                        
                        if (Emotes.doEmote(currentEmote)) {
                            Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                            Sleep.sleep(SHORT_SLEEP_MIN, SHORT_SLEEP_MAX);
                            currentEmoteIndex++;
                            
                            if (currentEmoteIndex >= emoteSequence.length) {
                                emotesCompleted = true;
                                Logger.log("All emotes completed!");
                            }
                        } else {
                            Logger.log("Failed to do emote: " + currentEmote.name());
                            Sleep.sleep(SHORT_SLEEP_MIN, SHORT_SLEEP_MAX);
                        }
                    }
                    break;
                case HANDLINGDIALOGUES:
                    Logger.log("STATE: HANDLINGDIALOGUES");
                    ui.setState(State.HANDLINGDIALOGUES);
                    if (Dialogues.inDialogue()) {
                        Logger.log("Dialogue is open, continuing dialogue...");
                        if (Dialogues.continueDialogue()) {
                            Sleep.sleepUntil(() -> !Dialogues.inDialogue(), 3000);
                            Sleep.sleep(SHORT_SLEEP_MIN, SHORT_SLEEP_MAX);
                        }
                    } else {
                        Logger.log("No dialogue found, dialogues handled!");
                        dialoguesHandled = true;
                    }
                    break;
                case WALKINGTORUBYRINGS:
                    Logger.log("STATE: WALKINGTORUBYRINGS");
                    ui.setState(State.WALKINGTORUBYRINGS);
                    WalkingUtil.walkToDestination(RubyRingsArea.getRandomTile());
                    Sleep.sleep(MEDIUM_SLEEP_MIN, MEDIUM_SLEEP_MAX);
                    break;
                case PICKINGUPRUBY:
                    Logger.log("STATE: PICKINGUPRUBY - Starting pickup process");
                    ui.setState(State.PICKINGUPRUBY);
                    
                    Logger.log("Checking for ruby ring on ground...");
                    Sleep.sleep(500, 700);
                    item = GroundItems.closest("Ruby ring");
                    
                    if (item != null) {
                        Logger.log("Found ruby ring, picking it up...");
                        if (item.interact("Take")) {
                            Sleep.sleep(2000, 3000);
                            rubyRingsPicked++;
                            rubyRingsPickedGlobal++;
                            
                            // Get current ruby ring price (using constant for now, can be updated to dynamic later)
                            int currentRubyRingPrice = getRubyRingPrice();
                            estimatedProfit += currentRubyRingPrice;
                            
                            ui.setPurchased(rubyRingsPickedGlobal);
                            ui.setEstimatedProfit(estimatedProfit);
                            Logger.log("Successfully picked up ruby ring! Total: " + rubyRingsPickedGlobal + " | Current price: " + currentRubyRingPrice + " GP | Estimated profit: " + estimatedProfit);
                        } else {
                            Logger.log("Failed to interact with ruby ring");
                        }
                    } else {
                        Logger.log("No ruby ring found, initiating world hop...");
                        hopToRandomWorld();
                    }
                    Logger.log("PICKINGUPRUBY case completed");
                    break;
            case WALKINGTOGATEAREA:
                Logger.log("STATE: WALKINGTOGATEAREA");
                ui.setState(State.WALKINGTOGATEAREA);
                WalkingUtil.walkToDestination(GateSpecificArea.getRandomTile());
                Sleep.sleep(MEDIUM_SLEEP_MIN, MEDIUM_SLEEP_MAX);
                break;
            case DOINGEMOTESATGATE:
                Logger.log("STATE: DOINGEMOTESATGATE");
                ui.setState(State.DOINGEMOTESATGATE);
                
                if (!emotesAtGateCompleted) {
                    if (gateEmoteIndex < emoteSequence.length) {
                        Emote currentEmote = emoteSequence[gateEmoteIndex];
                        Logger.log("Doing emote at gate: " + currentEmote.name() + " (" + (gateEmoteIndex + 1) + "/" + emoteSequence.length + ")");
                        
                        if (Emotes.doEmote(currentEmote)) {
                            Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                            Sleep.sleep(SHORT_SLEEP_MIN, SHORT_SLEEP_MAX);
                            gateEmoteIndex++;
                            
                            if (gateEmoteIndex >= emoteSequence.length) {
                                emotesAtGateCompleted = true;
                                Logger.log("All emotes at gate completed!");
                            }
                        } else {
                            Logger.log("Failed to do emote at gate: " + currentEmote.name());
                            Sleep.sleep(SHORT_SLEEP_MIN, SHORT_SLEEP_MAX);
                        }
                    } else {
                        Logger.log("Gate emote index out of bounds, marking gate emotes as completed");
                        emotesAtGateCompleted = true;
                    }
                } else {
                    Logger.log("Emotes at gate already completed, should proceed to bank");
                }
                break;
            case WALKINGTOBANK:
                Logger.log("STATE: WALKINGTOBANK");
                ui.setState(State.WALKINGTOBANK);
                BankingHelper.walkToClosestBank(BankArea, true);
                Sleep.sleep(MEDIUM_SLEEP_MIN, MEDIUM_SLEEP_MAX);
                break;
            case BANKING:
                Logger.log("STATE: BANKING");
                ui.setState(State.BANKING);
                depositAllItems();
                break;
            default:
                Logger.log("UNKNOWN STATE: " + state);
                ui.setState(State.DEFAULT);
                break;
        }
        Logger.log("onLoop() completed successfully, returning 600");
        return 600; // Return a positive value to continue the loop
        } catch (Exception e) {
            Logger.log("Error in onLoop: " + e.getMessage());
            e.printStackTrace();
            return 600; // Continue the loop even if there's an error
        }
    }

    public State getState() {
        Logger.log("Get State - Starting state determination");
        Player localPlayer = Players.getLocal();
        
        if (localPlayer == null) {
            Logger.log("Player not found!");
            return State.DEFAULT;
        }
        
        Tile playerTile = localPlayer.getTile();
        Logger.log("Player position: " + playerTile.getX() + ", " + playerTile.getY());
        
        boolean inGateAreaOutside = GateAreaOutside.contains(playerTile);
        boolean inGateAreaInside = GateAreaInside.contains(playerTile);
        boolean inRubyRingsArea = RubyRingsArea.contains(playerTile);
        boolean inCollectionArea = CollectionArea.contains(playerTile);
        boolean inGateSpecificArea = GateSpecificArea.contains(playerTile);
        
        Logger.log("Area checks - Outside: " + inGateAreaOutside + ", Inside: " + inGateAreaInside + 
                  ", RubyRings: " + inRubyRingsArea + ", Collection: " + inCollectionArea + ", GateSpecific: " + inGateSpecificArea);
        
        // Debug: Log area information for troubleshooting
        Logger.log("RubyRingsArea center: " + RubyRingsArea.getCenter());
        Logger.log("CollectionArea center: " + CollectionArea.getCenter());
        
        Logger.log("Inventory full: " + Inventory.isFull());
        Logger.log("Emotes completed: " + emotesCompleted);
        Logger.log("Emotes at gate completed: " + emotesAtGateCompleted);
        Logger.log("Current emote index: " + currentEmoteIndex);
        Logger.log("Gate emote index: " + gateEmoteIndex);
        Logger.log("Dialogues handled: " + dialoguesHandled);
        Logger.log("Bank area contains player: " + BankArea.contains(playerTile));

        // Se o player está na área do banco
        if (BankArea.contains(playerTile)) {
            Logger.log("Player in bank area - banking");
            return State.BANKING;
        }

        // Prioridade para inventário cheio - sempre vai para o banco após fazer emotes no portão
        if (Inventory.isFull()) {
            // Se está na área específica do portão interno e ainda não fez os emotes
            if (inGateSpecificArea && !emotesAtGateCompleted) {
                Logger.log("Player in gate specific area with full inventory - need to do emotes at gate");
                Logger.log("Gate emote index: " + gateEmoteIndex + ", Emotes at gate completed: " + emotesAtGateCompleted);
                return State.DOINGEMOTESATGATE;
            }
            // Se já fez os emotes no portão OU não está mais no portão específico, vai para o banco
            else if (emotesAtGateCompleted || (!inGateSpecificArea && !inCollectionArea && !inRubyRingsArea)) {
                Logger.log("Player with full inventory - going to bank");
                return State.WALKINGTOBANK;
            }
            // Se está na área de coleta com inventário cheio, vai para o portão
            else if (inCollectionArea) {
                Logger.log("Player in collection area with full inventory - going to gate area");
                return State.WALKINGTOGATEAREA;
            }
            // Se está na área dos ruby rings com inventário cheio, vai para o portão
            else if (inRubyRingsArea) {
                Logger.log("Player in ruby rings area with full inventory - walking to gate area");
                return State.WALKINGTOGATEAREA;
            }
            // Se está no portão interno mas ainda não fez os emotes, precisa fazer os emotes
            else if (inGateAreaInside && !emotesAtGateCompleted) {
                Logger.log("Player in gate area inside with full inventory - need to do emotes at gate first");
                return State.DOINGEMOTESATGATE;
            }
        }

        // Se o player está na área de coleta e não tem o inventário cheio
        if (inCollectionArea && !Inventory.isFull()) {
            // Se está na área específica do Ruby Ring, pega o ring
            if (inRubyRingsArea) {
                Logger.log("Player in ruby rings area - picking up ruby");
                return State.PICKINGUPRUBY;
            } else {
                Logger.log("Player in collection area with space in inventory - going to ruby rings");
                return State.WALKINGTORUBYRINGS;
            }
        }

        // Verificações para inventário vazio apenas
        if (!inGateAreaOutside && !inGateAreaInside && !inRubyRingsArea && !inCollectionArea && !BankArea.contains(playerTile)) {
            Logger.log("Player not in any defined area - walking to gate");
            return State.WALKINGTOGATE;
        } else if (inGateAreaOutside && !emotesCompleted) {
            Logger.log("Player at gate, doing emotes");
            return State.DOINGEMOTES;
        } else if (inGateAreaOutside && emotesCompleted && !dialoguesHandled) {
            Logger.log("Player at gate, handling dialogues");
            return State.HANDLINGDIALOGUES;
        } else if (inGateAreaOutside && emotesCompleted && dialoguesHandled) {
            Logger.log("Player at gate, emotes and dialogues done - walking to ruby rings");
            return State.WALKINGTORUBYRINGS;
        } else if (inRubyRingsArea && !Inventory.isFull()) {
            Logger.log("Player in ruby rings area with space - picking up ruby");
            return State.PICKINGUPRUBY;
        } else if (inGateAreaInside && !Inventory.isFull()) {
            Logger.log("Player inside gate with empty inventory - walking to gate area outside");
            return State.WALKINGTOGATE;
        }

        // Additional check: if player is close to ruby rings area (within 5 tiles) and inventory not full
        if (!Inventory.isFull() && playerTile != null) {
            Tile rubyRingCenter = RubyRingsArea.getCenter();
            if (rubyRingCenter != null && playerTile.distance(rubyRingCenter) <= 5) {
                Logger.log("Player is close to ruby rings area (distance: " + playerTile.distance(rubyRingCenter) + ") - walking to ruby rings");
                return State.WALKINGTORUBYRINGS;
            }
            
            Tile collectionCenter = CollectionArea.getCenter();
            if (collectionCenter != null && playerTile.distance(collectionCenter) <= 8) {
                Logger.log("Player is close to collection area (distance: " + playerTile.distance(collectionCenter) + ") - walking to ruby rings");
                return State.WALKINGTORUBYRINGS;
            }
        }

        Logger.log("No state matched - returning default");
        return State.DEFAULT;
    }
    
    private void hopToRandomWorld() {
        try {
            Logger.log("Starting world hop process...");
            WorldHopperHelper.hopToRandomWorld();
            Logger.log("World hop completed!");

            // Reset emotes and dialogues for the new world
            emotesCompleted = false;
            emotesAtGateCompleted = false;
            dialoguesHandled = false;
            currentEmoteIndex = 0;
            gateEmoteIndex = 0;
            Logger.log("Reset emotes and dialogues for new world");
            return;
        } catch (Exception e) {
            Logger.log("Error in hopToRandomWorld: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void depositAllItems() {
        if (BankArea != null && BankArea.contains(Players.getLocal())) {
            GameObject bank = GameObjects.closest("Bank booth");
            if (bank != null && bank.interact("Bank")) {
                while (!Bank.isOpen()) {
                    Sleep.sleep(200);
                    if(!bank.isOnScreen() && !Players.getLocal().isMoving()){
                        break;
                    }
                }
                if (Bank.isOpen()) {
                    Bank.depositAllItems();
                    Sleep.sleep(500, 700);
                    Bank.close();
                    
                    // Reset all states for new cycle
                    emotesCompleted = false;
                    emotesAtGateCompleted = false;
                    dialoguesHandled = false;
                    currentEmoteIndex = 0;
                    gateEmoteIndex = 0;
                    Logger.log("Banking completed! Starting new cycle...");
                }
            }
        } else {
            Logger.log("Not in bank area, walking to bank");
            Tile bankTile = BankArea.getRandomTile();
            if (bankTile != null) {
                Walking.walk(bankTile);
                Sleep.sleep(MEDIUM_SLEEP_MIN, MEDIUM_SLEEP_MAX);
            }
        }
    }

    /**
     * Gets the current Ruby Ring price using LivePrices API
     * @return current ruby ring price in GP
     */
    private int getRubyRingPrice() {
        return (int) PriceUtil.getItemPrice("Ruby ring", 1315.0f);
    }

    @Override
    public void onPaint(Graphics g) {
        if (ui != null) {
            ui.setState(getState());
            ui.onPaint(g);
        }
    }

    @Override
    public void onStart() {
        Logger.log("Welcome to RubyRingPicker!");
        timeBeggone = System.currentTimeMillis();
        state = getState();
        ui = new UI(timeBeggone, rubyRingsPickedGlobal, estimatedProfit, state);
        Logger.log("Script initialized successfully");
    }

    @Override
    public void onExit() {
        Logger.log("Thank you for using RubyRingPicker!");
    }
}
