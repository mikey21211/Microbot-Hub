package net.runelite.client.plugins.microbot.seedbuyer;

import net.runelite.client.config.*;

@ConfigGroup("seedbuyer")
@ConfigInformation(
        "<html>" +
        "<div style='margin:0; padding:0;'>If you have any questions, please contact <b>heapoverfl0w</b> in the <b>Microbot</b> Discord!</div>" +
        "<br/>" +
        "<b>Requirements:</b>" +
        "<ul style='margin:0; padding-left: 10px; list-style-position: inside;'>" +
        "<li>Glassblowing pipe in inventory</li>" +
        "<li>Coins ≥ 1,000</li>" +
        "<li>Astral runes ≥ 2</li>" +
        "<li>Either Air runes ≥ 10 or wear Staff of air / Air battlestaff</li>" +
        "<li>Either Fire runes ≥ 6 or wear Staff of fire / Fire battlestaff</li>" +
        "<li>Be near a Trader Crewmember (no walking logic is included)</li>" +
        "</ul>" +
        "</html>"
)
public interface SeedBuyerConfig extends Config {

    @ConfigSection(
            name = "Settings",
            description = "Seed Buyer settings",
            position = 0
    )
    String SETTINGS_SECTION = "settings";

    enum Product {
        BEER_GLASS("Beer glass", "Beer glass"),
        CANDLE_LANTERN("Candle lantern", "Empty candle lantern"),
        OIL_LAMP("Oil lamp", "Empty oil lamp"),
        VIAL("Vial", "Vial"),
        FISHBOWL("Fishbowl", "Empty fishbowl"),
        UNPOWERED_STAFF_ORB("Unpowered staff orb", "Unpowered orb"),
        LANTERN_LENS("Lantern lens", "Lantern lens"),
        LIGHT_ORB("Light orb", "Empty light orb");

        private final String widgetName;
        private final String sellName;
        Product(String widgetName, String sellName) { this.widgetName = widgetName; this.sellName = sellName; }
        @Override public String toString() { return widgetName; }
        public String widgetName() { return widgetName; }
        public String sellName() { return sellName; }
    }

    @ConfigItem(
            keyName = "product",
            name = "Product",
            description = "Glass item to make",
            section = SETTINGS_SECTION,
            position = 0
    )
    default Product product() {
        return Product.BEER_GLASS;
    }

    
}
