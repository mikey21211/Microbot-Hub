package net.runelite.client.plugins.microbot.ScreenRotation;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "ScreenRotation",
        description = "ScreenRotation plugin ML",
        tags = {"ScreenRotation", "microbot"},
        minClientVersion = "2.0.13",
        enabledByDefault = true
)
@Slf4j
public class ScreenRotationPlugin extends Plugin {
    @Inject
    private ScreenRotationConfig config;
    @Provides
    ScreenRotationConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ScreenRotationConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ScreenRotationOverlay screenRotationOverlay;

    @Inject
    ScreenRotationScript screenRotationScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(screenRotationOverlay);
            screenRotationOverlay.myButton.hookMouseListener();
        }
        screenRotationScript.run(config);
    }

    protected void shutDown() {
        screenRotationScript.shutdown();
        overlayManager.remove(screenRotationOverlay);
        screenRotationOverlay.myButton.unhookMouseListener();
    }
}
