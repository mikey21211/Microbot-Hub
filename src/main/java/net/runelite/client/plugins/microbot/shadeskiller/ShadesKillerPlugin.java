package net.runelite.client.plugins.microbot.shadeskiller;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "ShadesKiller",
        description = "Microbot ShadesKiller plugin",
        tags = {"Shades", "microbot", "Moneymaking"},
        version = ShadesKillerPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class ShadesKillerPlugin extends Plugin {

    public final static String version = "1.0.1";
    @Inject
    private ShadesKillerConfig config;
    @Provides
    ShadesKillerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ShadesKillerConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ShadesKillerOverlay shadesKillerOverlay;

    @Inject
    ShadesKillerScript shadesKillerScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(shadesKillerOverlay);
        }
        shadesKillerScript.run(config);
    }

    protected void shutDown() {
        shadesKillerScript.shutdown();
        overlayManager.remove(shadesKillerOverlay);
    }
}
