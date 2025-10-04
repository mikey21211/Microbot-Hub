package net.runelite.client.plugins.microbot.ScreenRotation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Test")
@ConfigInformation("<div style='background-color:black;color:yellow;padding:20px;'>" +
        "<center><h2>Instructions</h2>" +
        " <p>Enter whatever code you want.<br /><br />" +
        " <b style='color:red;'>MUST</b> Profit <br />")
public interface ScreenRotationConfig extends Config {

    String GROUP = "screenrotation";

    @ConfigItem(
            keyName = "monster",
            name = "Rotation Worldpoint",
            description = "Worldpoint currently targeted to rotate to",
            position = 1
    )
    default String rotationCoordinates() {
        return "";
    }

}