package net.runelite.client.plugins.microbot.ScreenRotation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;

@ConfigGroup("Test")
@ConfigInformation("<div style='background-color:black;color:yellow;padding:20px;'>" +
        "<center><h2>Instructions</h2>" +
        " <p>Enter whatever code you want.<br /><br />" +
        " <b style='color:red;'>MUST</b> Profit <br />")
public interface ScreenRotationConfig extends Config {
}