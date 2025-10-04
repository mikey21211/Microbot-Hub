package net.runelite.client.plugins.microbot.seedbuyer;

import lombok.RequiredArgsConstructor;
import net.runelite.api.GameState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.shop.Rs2Shop.buyItem;

@RequiredArgsConstructor
public class SeedBuyerScript extends Script {
    private final SeedBuyerPlugin plugin;
    private final SeedBuyerConfig config;

    private enum State {
        BOOTSTRAP,
        OPEN_SHOP,
        BUY_MATERIALS,
        WORLD_HOP,
        CLOSE_SHOP,
        OPEN_SELL,
        SELL_PRODUCTS,
        STOP
    }

    private volatile boolean isPrepared = false;
    private volatile boolean hasSetup = false;
    private volatile State state = State.BOOTSTRAP;
    private volatile boolean worldHopPending = false;
    private volatile int beforeHopWorld = -1;
    private volatile int worldHopAttempts = 0;
    private volatile long lastHopAttemptMs = 0L;
    private volatile boolean stopRequested = false;

    private static final String TRADER_NAME = "Olivia";
    private static final String POTATO_SEED = "Potato seed";
    private static final String ONION_SEED = "Onion seed";
    private static final String CABBAGE_SEED = "Cabbage seed";
    private static final String BARLEY_SEED = "Barley seed";
    private static final String ROSEMARY_SEED = "Rosemary seed";
    private static final String MARIGOLD_SEED = "Marigold seed";
    private static final String HAMMERSTONE_SEED = "Hammerstone seed";
    private static final String ASGARNIAN_SEED = "Asgarnian seed";

    public boolean run() {
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled() && !mainScheduledFuture.isDone()) {
            mainScheduledFuture.cancel(true);
        }
        stopRequested = false;
        Microbot.pauseAllScripts.compareAndSet(true, false);
        Microbot.enableAutoRunOn = false;
        applyAntiBanSettings();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (stopRequested || Thread.currentThread().isInterrupted()) return;
                if (!super.run() || !Microbot.isLoggedIn()) return;

                switch (state) {
                    case BOOTSTRAP:
                        bootstrap();
                        break;
                    case OPEN_SHOP:
                        openShop();
                        break;
                    case BUY_MATERIALS:
                        buyMaterials();
                        break;
                    case WORLD_HOP:
                        worldHop();
                        break;
                    case CLOSE_SHOP:
                        closeShop();
                        break;
                    case STOP:
                        stopRequested = true;
                        Microbot.stopPlugin(plugin);
                        break;
                }
            } catch (Exception ex) {
                if (stopRequested) return;
            }
        }, 0, 600, TimeUnit.MILLISECONDS);

        return true;
    }

    private void bootstrap() {
        isPrepared = false;
        hasSetup = false;

        String target = config.product().widgetName();
        if (target == null || target.isBlank()) {
            update("Bootstrap", "Invalid target product", false, false);
            state = State.STOP;
            return;
        }

        boolean hasCoins = Rs2Inventory.hasItemAmount("Coins", 1000);

        hasSetup = hasCoins;
        if (!hasSetup) {
            List<String> missing = new ArrayList<>();
            if (!hasCoins) missing.add(">= 1,000 coins");
            String msg = missing.isEmpty() ? "Missing setup requirements" : ("Missing: " + String.join(", ", missing));
            Microbot.showMessage(msg);
            update("Bootstrap", msg, false, false);
            state = State.STOP;
            return;
        }

        Rs2NpcModel trader = Rs2Npc.getNpc(TRADER_NAME, false);
        if (trader == null) {
            update("Bootstrap", "Trader not nearby", false, true);
            state = State.STOP;
            return;
        }

        update("Bootstrap", "Ready → Open Shop", false, true);
        state = State.OPEN_SHOP;
    }

    private void openShop() {
        update("Open Shop", "Opening shop", isPrepared, hasSetup);
        boolean opened = Rs2Shop.openShop(TRADER_NAME, true);
        if (!opened) opened = Rs2Shop.openShop("Trader", false);
        if (opened && Rs2Shop.isOpen()) state = State.BUY_MATERIALS;
    }

    private void buyMaterials() {
        // Stop condition: if Potato or Onion seeds >= 2000
        int potatoCount = Rs2Inventory.itemQuantity(POTATO_SEED);
        int onionCount = Rs2Inventory.itemQuantity(ONION_SEED);
        if (potatoCount >= 2000 || onionCount >= 2000) {
            Microbot.log("Reached target: Potato=" + potatoCount + ", Onion=" + onionCount + ". Stopping.");
            return; // stop buying
        }

        // List of seeds to buy
        String[] seeds = {
                POTATO_SEED,
                ONION_SEED,
                CABBAGE_SEED,
                BARLEY_SEED,
                ROSEMARY_SEED,
                MARIGOLD_SEED,
                HAMMERSTONE_SEED,
                ASGARNIAN_SEED
        };

        if (!Rs2Inventory.hasItem("Coins")) {
            Microbot.log("No coins available to buy seeds.");
            return;
        }

        // Assume all seeds are almost sold out until proven otherwise
        boolean allLowStock = true;

        for (String seed : seeds) {
            // Check if shop has at least 6 of this seed
            if (Rs2Shop.hasMinimumStock(seed, 6)) {
                allLowStock = false; // at least one seed worth buying
                buyItem(seed, String.valueOf(50));
                sleepGaussian(170, 40);
            }
        }

        // If every seed is <= 5 in stock, hop worlds
        if (allLowStock) {
            Microbot.log("All seeds nearly sold out (<6 each). Hopping worlds...");
            // Rs2WorldHopper.hopToNextWorld();

            worldHopPending = true;
            worldHopAttempts = 0;
            beforeHopWorld = Microbot.getClient().getWorld();
            if (Rs2Shop.isOpen()) Rs2Shop.closeShop();
            state = SeedBuyerScript.State.WORLD_HOP;
            return;
        }

    }


    private void worldHop() {
        if (worldHopPending) {
            GameState gs = Microbot.getClient().getGameState();
            if (gs == GameState.HOPPING || gs == GameState.LOGIN_SCREEN) {
                update("World Hop", "Hop in progress...", isPrepared, hasSetup);
                return; // Do not trigger a new attempt while hopping/logging in
            }
            if (gs == GameState.LOGGED_IN) {
                int currentWorld = Microbot.getClient().getWorld();
                if (beforeHopWorld != -1 && currentWorld != beforeHopWorld) {
                    worldHopPending = false;
                    update("World Hop", "Hopped successfully to world " + currentWorld, isPrepared, hasSetup);
                    state = State.OPEN_SHOP;
                    return;
                }
                long sinceLast = System.currentTimeMillis() - lastHopAttemptMs;
                if (sinceLast < 6000) {
                    // Give previous attempt more time to complete before retrying
                    update("World Hop", "Waiting before next attempt (" + (int)((6000 - sinceLast)/1000) + "s)", isPrepared, hasSetup);
                    return;
                }
            }
        }
        if (worldHopAttempts >= 5) {
            update("World Hop", "Hop attempts exhausted; retry later", isPrepared, hasSetup);
            worldHopPending = false;
            state = State.BUY_MATERIALS;
            return;
        }

        if (Rs2Shop.isOpen()) Rs2Shop.closeShop();
        sleepUntil(() -> !Rs2Player.isAnimating(), 2000);

        int world = Login.getRandomWorld(Rs2Player.isMember());
        worldHopAttempts++;
        lastHopAttemptMs = System.currentTimeMillis();
        worldHopPending = true;
        update("World Hop", "Initiating hop attempt " + worldHopAttempts + " to world " + world, isPrepared, hasSetup);
        boolean hopCall = Microbot.hopToWorld(world);
        if (!hopCall) {
            sleep(800);
        }
    }

    private void closeShop() {
        update("Close Shop", "Closing shop", isPrepared, hasSetup);
        Rs2Shop.closeShop();
        state = State.WORLD_HOP;
    }

    private void update(String s, String msg, boolean prepared, boolean setup) {
        plugin.updateState(s, msg, prepared, setup);
    }

    public void requestStop() {
        stopRequested = true;
    }

    private void applyAntiBanSettings() {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2Antiban.setActivityIntensity(ActivityIntensity.VERY_LOW);
        Rs2AntibanSettings.dynamicIntensity = false;
        Rs2AntibanSettings.devDebug = false;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.actionCooldownChance = 0.082;
        Rs2AntibanSettings.microBreakChance = 0.7;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.1;
    }

}
