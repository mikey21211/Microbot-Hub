package net.runelite.client.plugins.microbot.seedbuyer;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginConstants.heapoverfl0w + "Seed Buyer",
        description = "Seed buyer plugin (UIM-friendly)",
        tags = {"seed", "buyer"},
        authors = {"heapoverfl0w"},
        version = SeedBuyerPlugin.version,
        minClientVersion = "1.9.8",
        iconUrl = "https://chsami.github.io/Microbot-Hub/CharterCrafterPlugin/assets/icon.png",
        cardUrl = "https://chsami.github.io/Microbot-Hub/CharterCrafterPlugin/assets/card.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class SeedBuyerPlugin extends Plugin {
    static final String version = "1.0.0";
    @Inject
    private SeedBuyerConfig config;

    @Inject
    private SeedBuyerOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    private SeedBuyerScript script;

    @Getter
    private volatile String state = "Idle";

    @Getter
    private volatile boolean prepared = false;

    @Getter
    private volatile boolean setup = false;

    @Getter
    private volatile String status = "";

    // Stats tracking
    @Getter
    private volatile long startTimeMillis = System.currentTimeMillis();
    @Getter
    private volatile int startMagicXp = 0;
    @Getter
    private volatile int startCraftingXp = 0;
    @Getter
    private volatile int moltenGlassCrafted = 0;

    @Provides
    SeedBuyerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SeedBuyerConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        startTimeMillis = System.currentTimeMillis();
        if (Microbot.getClient() != null) {
            startMagicXp = Microbot.getClient().getSkillExperience(Skill.MAGIC);
            startCraftingXp = Microbot.getClient().getSkillExperience(Skill.CRAFTING);
        } else {
            startMagicXp = 0;
            startCraftingXp = 0;
        }
        moltenGlassCrafted = 0;
        script = new SeedBuyerScript(this, config);
        script.run();
        if (overlayManager != null) overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        if (overlayManager != null) overlayManager.remove(overlay);
        if (script != null) {
            script.requestStop();
            script.shutdown();
            script = null;
        }
    }

    public String getTargetProduct() {
        return config.product().widgetName();
    }

    void updateState(String state, String status, boolean isPrepared, boolean hasSetup) {
        this.state = state;
        this.status = status;
        this.prepared = isPrepared;
        this.setup = hasSetup;
        Microbot.status = status;
    }

    void addMoltenGlassCrafted(int amount) {
        if (amount > 0) {
            moltenGlassCrafted += amount;
        }
    }

}
