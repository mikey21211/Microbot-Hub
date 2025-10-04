package net.runelite.client.plugins.microbot.ScreenRotation;

import com.google.inject.Provides;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPanel;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;

import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.*;
import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.MARKER_IMAGE;
import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.lastLocation;
import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.marker;
import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.startPointSet;

@PluginDescriptor(
        name = PluginDescriptor.Default + "ScreenRotation",
        description = "ScreenRotation plugin ML",
        tags = {"ScreenRotation", "microbot"},
        minClientVersion = "2.0.13",
        enabledByDefault = true
)
@Slf4j
public class ScreenRotationPlugin extends Plugin{
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

    @Inject
    private Client client;

    @Getter
    @Inject
    private ClientThread clientThread;

    private static final String SET = "Set";
    private static final String ROTATE_TILE = ColorUtil.wrapWithColorTag("Safe Spot", JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND);
    private static final String WALK_HERE = "Walk here";
    private WorldPoint trueTile;
    private MenuEntry lastClick;
    private Point lastMenuOpenedPoint;

    private ScreenRotationPanel panel;
    @Getter
    @Setter
    public static WorldMapPoint marker;

    private void onMenuOptionClicked(MenuEntry entry) {

        if (entry.getOption().equals(SET) && entry.getTarget().equals(ROTATE_TILE)) {
            setRotateTile(trueTile);
        }

        if (entry.getType() != MenuAction.WALK) {
            lastClick = entry;
        }
    }

    public static void setRotateTile(WorldPoint worldPoint)
    {
        Microbot.getConfigManager().setConfiguration(
                AIOFighterConfig.GROUP,
                "safeSpotLocation",
                worldPoint
        );
    }

    private WorldPoint getSelectedWorldPoint() {
        if (Microbot.getClient().getWidget(ComponentID.WORLD_MAP_MAPVIEW) == null) {
            if (Microbot.getClient().getSelectedSceneTile() != null) {
                return Microbot.getClient().isInInstancedRegion() ?
                        WorldPoint.fromLocalInstance(Microbot.getClient(), Microbot.getClient().getSelectedSceneTile().getLocalLocation()) :
                        Microbot.getClient().getSelectedSceneTile().getWorldLocation();
            }
        } else {
            return calculateMapPoint(Microbot.getClient().isMenuOpen() ? lastMenuOpenedPoint : Microbot.getClient().getMouseCanvasPosition());
        }
        return null;
    }

    public WorldPoint calculateMapPoint(Point point) {
        WorldMap worldMap = Microbot.getClient().getWorldMap();
        float zoom = worldMap.getWorldMapZoom();
        final WorldPoint mapPoint = new WorldPoint(worldMap.getWorldMapPosition().getX(), worldMap.getWorldMapPosition().getY(), 0);
        final Point middle = mapWorldPointToGraphicsPoint(mapPoint);

        if (point == null || middle == null) {
            return null;
        }

        final int dx = (int) ((point.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    public Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint) {
        WorldMap worldMap = Microbot.getClient().getWorldMap();

        float pixelsPerTile = worldMap.getWorldMapZoom();

        Widget map = Microbot.getClient().getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (map != null) {
            Rectangle worldMapRect = map.getBounds();

            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

            Point worldMapPosition = worldMap.getWorldMapPosition();

            int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
            int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
            int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

            int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
            int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

            yGraphDiff -= (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));
            xGraphDiff += (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));

            yGraphDiff = worldMapRect.height - yGraphDiff;
            yGraphDiff += (int) worldMapRect.getY();
            xGraphDiff += (int) worldMapRect.getX();

            return new Point(xGraphDiff, yGraphDiff);
        }
        return null;
    }

    public void setNewRotationPoint(WorldPoint target) {
        /*Microbot.getConfigManager().setConfiguration(
                AIOFighterConfig.GROUP,
                "monster",
                getNpcAttackList() + npcName + ","
        );*/

        int x = target.getX();
        int y = target.getY();
        int z = target.getPlane();

    }

    public void setTarget(WorldPoint target) {
        setTarget(target, false);
    }

    private void setTarget(WorldPoint target, boolean append) {
        Set<WorldPoint> targets = new HashSet<>();
        if (target != null) {
            targets.add(target);
        }
        //setTargets(targets, append);
    }

    private void addMenuEntry(MenuEntryAdded event, String option, String target, int position) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(Microbot.getClient().getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals(option) && e.getTarget().equals(target))) {
            return;
        }

        Microbot.getClient().createMenuEntry(position)
                .setOption(option)
                .setTarget(target)
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier())
                .setType(MenuAction.RUNELITE)
                .onClick(this::onMenuOptionClicked);
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event)
    {
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event) {
            if (Microbot.getClient().isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals(WALK_HERE) && event.getTarget().isEmpty())
            {
                addMenuEntry(event, SET, ROTATE_TILE, 1);
            }
        }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        lastMenuOpenedPoint = Microbot.getClient().getMouseCanvasPosition();
        trueTile = getSelectedWorldPoint();
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(screenRotationOverlay);
            screenRotationOverlay.myButton.hookMouseListener();
        }
        screenRotationScript.run(config);
        panel = injector.getInstance(ScreenRotationPanel.class);
    }

    protected void shutDown() {
        screenRotationScript.shutdown();
        overlayManager.remove(screenRotationOverlay);
        screenRotationOverlay.myButton.unhookMouseListener();
    }
}
