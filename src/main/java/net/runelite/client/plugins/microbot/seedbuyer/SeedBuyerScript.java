package net.runelite.client.plugins.microbot.seedbuyer;

import lombok.RequiredArgsConstructor;
import net.runelite.api.GameState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
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
        SUPERGLASS_MAKE,
        START_GLASSBLOWING,
        WAIT_CRAFTING,
        OPEN_SELL,
        SELL_PRODUCTS,
        LOOP_OR_STOP,
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
                    case OPEN_SELL:
                        openSell();
                        break;
                    case SELL_PRODUCTS:
                        sellProducts();
                        break;
                    case LOOP_OR_STOP:
                        loopOrStop();
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

        boolean hasCoins = Rs2Inventory.hasItem("Coins");
        if (!hasCoins) {
            Microbot.log("No coins available to buy seeds.");
            return;
        }

        // Assume all seeds are almost sold out until proven otherwise
        boolean allLowStock = true;

        for (String seed : seeds) {
            if (Rs2Shop.hasStock(seed)) {
                //int stock = Rs2Shop.getStock(seed);
                //if (stock > 5) {
                  //  allLowStock = false; // at least one seed worth buying
                    //Rs2Shop.buyItemOptimally(seed, stock);
                //}
            }
        }

        // If every seed is <= 5 in stock, hop worlds
        if (allLowStock) {
            Microbot.log("All seeds nearly sold out (<5 each). Hopping worlds...");
            //Rs2WorldHopper.hopToNextWorld();
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
        if (worldHopAttempts >= 3) {
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
        state = State.SUPERGLASS_MAKE;
    }

    private void superglassMake() {
        if (!(isPrepared && hasSetup)) {
            update("Superglass Make", "Not prepared or setup missing", isPrepared, hasSetup);
            state = State.STOP;
            return;
        }
        Rs2Tab.switchTo(InterfaceTab.MAGIC);
        if (!Rs2Magic.canCast(MagicAction.SUPERGLASS_MAKE)) {
            update("Superglass Make", "Cannot cast Superglass Make", isPrepared, hasSetup);
            state = State.STOP;
            return;
        }
        update("Superglass Make", "Casting Superglass Make", isPrepared, hasSetup);
        boolean cast = Rs2Magic.cast(MagicAction.SUPERGLASS_MAKE);
        if (cast) {
            sleepUntil(() -> Rs2Inventory.itemQuantity(CABBAGE_SEED) > 0, 5000);
            if (Rs2Inventory.itemQuantity(CABBAGE_SEED) > 0) {
                int produced = Rs2Inventory.itemQuantity(CABBAGE_SEED);
                plugin.addMoltenGlassCrafted(produced);
                state = State.START_GLASSBLOWING;
            } else {
                update("Superglass Make", "No molten glass after cast", isPrepared, hasSetup);
                state = State.STOP;
            }
        }
    }

    private void startGlassblowing() {
        update("Start Glassblowing", "Preparing to combine (waiting for animation to end)", isPrepared, hasSetup);
        sleepUntil(() -> Rs2Inventory.itemQuantity(CABBAGE_SEED) > 0 && !Rs2Player.isAnimating(), 8000);
        sleep(200);

        update("Start Glassblowing", "Combining pipe with molten glass", isPrepared, hasSetup);
        boolean combined = false;
        for (int attempt = 0; attempt < 3 && !combined; attempt++) {
            combined = Rs2Inventory.combine(
                    Rs2Inventory.get(BARLEY_SEED),
                    Rs2Inventory.get(CABBAGE_SEED)
            );
            if (!combined) {
                sleepUntil(() -> !Rs2Player.isAnimating(), 1500);
                sleep(200);
            }
        }
        if (!combined) {
            update("Start Glassblowing", "Failed to use pipe on molten glass", isPrepared, hasSetup);
            state = State.STOP;
            return;
        }
        String target = config.product().widgetName();
        sleepUntil(Rs2Widget::isProductionWidgetOpen, 5000);
        boolean selected = false;
        try {
            selected = Rs2Widget.clickWidget(target, true)
                    || Rs2Widget.clickWidget(target, false);
        } catch (Exception ignored) { }

        if (!selected) {
            update("Start Glassblowing", "Could not find product in crafting menu: " + target, isPrepared, hasSetup);
            state = State.STOP;
            return;
        }

        update("Wait Crafting", "Making: " + target, isPrepared, hasSetup);
        state = State.WAIT_CRAFTING;
    }

    private void waitCrafting() {
        sleep(600);
        if (!Rs2Inventory.contains(CABBAGE_SEED)) {
            state = State.OPEN_SELL;
        }
    }

    private void openSell() {
        if (Rs2Inventory.itemQuantity("Empty light orb") > 0 || Rs2Inventory.itemQuantity("Light orb") > 0) {
            update("Open Sell", "Dropping Light orbs (unsellable)", isPrepared, hasSetup);
            Rs2Inventory.dropAll("Empty light orb", "Light orb");
        }

        update("Open Sell", "Opening shop to sell", isPrepared, hasSetup);
        boolean opened = Rs2Shop.openShop(TRADER_NAME, true);
        if (opened && Rs2Shop.isOpen()) state = State.SELL_PRODUCTS;
    }

    private void sellProducts() {
        update("Sell Products", "Selling crafted items", isPrepared, hasSetup);
        for (SeedBuyerConfig.Product p : SeedBuyerConfig.Product.values()) {
            if ("Empty light orb".equals(p.sellName())) continue;
            sellAllOf(p.sellName());
        }
        isPrepared = false;
        state = State.BUY_MATERIALS;
    }

    private void loopOrStop() {
        isPrepared = false;
        boolean hasPipe = Rs2Inventory.contains(BARLEY_SEED);
        boolean hasCoins = Rs2Inventory.hasItemAmount("Coins", 1000);
        boolean hasAstral = Rs2Inventory.hasItemAmount("Astral rune", 2);
        boolean hasAirSupport = ensureElementSupport("Air rune", 10, HAMMERSTONE_SEED, ASGARNIAN_SEED);
        boolean hasFireSupport = ensureElementSupport("Fire rune", 6, ROSEMARY_SEED, MARIGOLD_SEED);
        hasSetup = hasPipe && hasCoins && hasAstral && hasAirSupport && hasFireSupport;

        if (hasSetup) {
            update("Loop Or Stop", "Looping for next batch", isPrepared, hasSetup);
            state = State.OPEN_SHOP;
        } else {
            update("Loop Or Stop", "Setup missing; stopping", isPrepared, hasSetup);
            state = State.STOP;
        }
    }

    private void sellAllOf(String name) {
        if (Rs2Inventory.itemQuantity(name) <= 0) return;
        while (Rs2Inventory.itemQuantity(name) > 0 && Rs2Shop.isOpen()) {
            Rs2Inventory.sellItem(name, "50");
            sleep(250);
        }
    }

    

    private boolean hasAnySellableGlassItems() {
        for (SeedBuyerConfig.Product p : SeedBuyerConfig.Product.values()) {
            if (Rs2Inventory.itemQuantity(p.sellName()) > 0) {
                return true;
            }
        }
        return false;
    }

    private void update(String s, String msg, boolean prepared, boolean setup) {
        plugin.updateState(s, msg, prepared, setup);
    }

    public void requestStop() {
        stopRequested = true;
    }

    private boolean ensureElementSupport(String runeName, int requiredAmount, String staffName, String battlestaffName) {
        if (Rs2Inventory.hasItemAmount(runeName, requiredAmount)) return true;
        if (Rs2Equipment.isWearing(staffName) || Rs2Equipment.isWearing(battlestaffName)) return true;

        if (Rs2Inventory.hasItem(staffName)) {
            if (Rs2Inventory.interact(staffName, "Wield")) {
                sleepUntil(() -> Rs2Equipment.isWearing(staffName), 5000);
                if (Rs2Equipment.isWearing(staffName)) return true;
            }
        }
        if (Rs2Inventory.hasItem(battlestaffName)) {
            if (Rs2Inventory.interact(battlestaffName, "Wield")) {
                sleepUntil(() -> Rs2Equipment.isWearing(battlestaffName), 5000);
                if (Rs2Equipment.isWearing(battlestaffName)) return true;
            }
        }
        return false;
    }
}
