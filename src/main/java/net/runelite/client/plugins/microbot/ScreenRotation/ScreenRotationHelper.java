package net.runelite.client.plugins.microbot.ScreenRotation;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.camera.CameraPlugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.camera.NpcTracker;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Polygon;
import java.awt.Rectangle;


public class ScreenRotationHelper {
    private static final Logger log = LoggerFactory.getLogger(ScreenRotationHelper.class);
    private static final NpcTracker NPC_TRACKER = new NpcTracker();

    public static int angleToTile(Actor t) {
        int angle = (int)Math.toDegrees(Math.atan2((double)(t.getWorldLocation().getY() - Microbot.getClient().getLocalPlayer().getWorldLocation().getY()), (double)(t.getWorldLocation().getX() - Microbot.getClient().getLocalPlayer().getWorldLocation().getX())));
        return angle >= 0 ? angle : 360 + angle;
    }

    public static int angleToTile(TileObject t) {
        int angle = (int)Math.toDegrees(Math.atan2((double)(t.getWorldLocation().getY() - Microbot.getClient().getLocalPlayer().getWorldLocation().getY()), (double)(t.getWorldLocation().getX() - Microbot.getClient().getLocalPlayer().getWorldLocation().getX())));
        return angle >= 0 ? angle : 360 + angle;
    }

    public static int angleToTile(LocalPoint localPoint) {
        int angle = (int)Math.toDegrees(Math.atan2((double)(localPoint.getY() - Microbot.getClient().getLocalPlayer().getLocalLocation().getY()), (double)(localPoint.getX() - Microbot.getClient().getLocalPlayer().getLocalLocation().getX())));
        return angle >= 0 ? angle : 360 + angle;
    }

    public static int angleToTile(WorldPoint worldPoint) {
        int angle = (int)Math.toDegrees(Math.atan2((double)(worldPoint.getY() - Rs2Player.getWorldLocation().getY()), (double)(worldPoint.getX() - Rs2Player.getWorldLocation().getX())));
        return angle >= 0 ? angle : 360 + angle;
    }

    public static void turnTo(Actor actor) {
        int angle = getCharacterAngle(actor);
        setAngle(angle, 40);
    }

    public static void turnTo(Actor actor, int maxAngle) {
        int angle = getCharacterAngle(actor);
        setAngle(angle, maxAngle);
    }

    public static void turnTo(TileObject tileObject) {
        int angle = getObjectAngle(tileObject);
        setAngle(angle, 40);
    }

    public static void turnTo(TileObject tileObject, int maxAngle) {
        int angle = getObjectAngle(tileObject);
        setAngle(angle, maxAngle);
    }

    public static void turnTo(LocalPoint localPoint) {
        int angle = (angleToTile(localPoint) - 90) % 360;
        setAngle(angle, 40);
    }

    public static void turnTo(LocalPoint localPoint, int maxAngle) {
        int angle = (angleToTile(localPoint) - 90) % 360;
        setAngle(angle, maxAngle);
    }

    public static int getCharacterAngle(Actor actor) {
        return getTileAngle(actor);
    }

    public static int getObjectAngle(TileObject tileObject) {
        return getTileAngle(tileObject);
    }

    public static int getTileAngle(Actor actor) {
        int a = (angleToTile(actor) - 90) % 360;
        return a < 0 ? a + 360 : a;
    }

    public static int getTileAngle(TileObject tileObject) {
        int a = (angleToTile(tileObject) - 90) % 360;
        return a < 0 ? a + 360 : a;
    }

    public static boolean isAngleGood(int targetAngle, int desiredMaxAngle) {
        return Math.abs(getAngleTo(targetAngle)) <= desiredMaxAngle;
    }

    public static void setAngle(int targetDegrees, int maxAngle) {
        double defaultCameraSpeed = (double)1.0F;
        if (Microbot.isPluginEnabled(CameraPlugin.class)) {
            String configGroup = "zoom";
            String configKey = "cameraSpeed";
            defaultCameraSpeed = (Double)((ConfigManager)Microbot.getInjector().getInstance(ConfigManager.class)).getConfiguration(configGroup, configKey, Double.TYPE);
        }

        Microbot.getClient().setCameraSpeed(3.0F);
        if (getAngleTo(targetDegrees) > maxAngle) {
            Rs2Keyboard.keyHold(37);
            Global.sleepUntilTrue(() -> Math.abs(getAngleTo(targetDegrees)) <= maxAngle, 50, 5000);
            Rs2Keyboard.keyRelease(37);
        } else if (getAngleTo(targetDegrees) < -maxAngle) {
            Rs2Keyboard.keyHold(39);
            Global.sleepUntilTrue(() -> Math.abs(getAngleTo(targetDegrees)) <= maxAngle, 50, 5000);
            Rs2Keyboard.keyRelease(39);
        }

        Microbot.getClient().setCameraSpeed((float)defaultCameraSpeed);
    }

    public static void adjustPitch(float percentage) {
        float currentPitchPercentage = cameraPitchPercentage();
        if (currentPitchPercentage < percentage) {
            Rs2Keyboard.keyHold(38);
            Global.sleepUntilTrue(() -> cameraPitchPercentage() >= percentage, 50, 5000);
            Rs2Keyboard.keyRelease(38);
        } else {
            Rs2Keyboard.keyHold(40);
            Global.sleepUntilTrue(() -> cameraPitchPercentage() <= percentage, 50, 5000);
            Rs2Keyboard.keyRelease(40);
        }

    }

    public static int getPitch() {
        return Microbot.getClient().getCameraPitch();
    }

    public static void setPitch(int pitch) {
        int minPitch = 128;
        int maxPitch = 383;
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        Microbot.getClient().setCameraPitchTarget(pitch);
    }

    public static float cameraPitchPercentage() {
        int minPitch = 128;
        int maxPitch = 383;
        int currentPitch = Microbot.getClient().getCameraPitch();
        int adjustedPitch = currentPitch - minPitch;
        int adjustedMaxPitch = maxPitch - minPitch;
        return (float)adjustedPitch / (float)adjustedMaxPitch;
    }

    public static int getAngleTo(int degrees) {
        int ca = getAngle();
        if (ca < degrees) {
            ca += 360;
        }

        int da = ca - degrees;
        if (da > 180) {
            da -= 360;
        }

        return da;
    }

    public static int getAngle() {
        return (int)Math.abs((double)Microbot.getClient().getCameraYaw() / 45.51 * (double)8.0F);
    }

    public static int calculateCameraYaw(int npcAngle) {
        return (1536 + (int)Math.round((double)npcAngle * 5.688888888888889)) % 2048;
    }

    public static void trackNpc(int npcId) {
        NPC_TRACKER.startTracking(npcId);
    }

    public static void stopTrackingNpc() {
        NPC_TRACKER.stopTracking();
    }

    public static boolean isTrackingNpc() {
        return NPC_TRACKER.isTracking();
    }

    public static boolean isTileOnScreen(TileObject tileObject) {
        int viewportHeight = Microbot.getClient().getViewportHeight();
        int viewportWidth = Microbot.getClient().getViewportWidth();
        Polygon poly = Perspective.getCanvasTilePoly(Microbot.getClient(), tileObject.getLocalLocation());
        if (poly == null) {
            return false;
        } else {
            return poly.getBounds2D().getX() <= (double)viewportWidth && poly.getBounds2D().getY() <= (double)viewportHeight;
        }
    }

    public static boolean isTileOnScreen(LocalPoint localPoint) {
        Client client = Microbot.getClient();
        int viewportHeight = client.getViewportHeight();
        int viewportWidth = client.getViewportWidth();
        Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
        if (poly == null) {
            return false;
        } else {
            Rectangle viewportBounds = new Rectangle(0, 0, viewportWidth, viewportHeight);
            if (!poly.intersects(viewportBounds)) {
                return false;
            } else {
                Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getPlane());
                return canvasPoint != null;
            }
        }
    }

    public static int getZoom() {
        return Microbot.getClient().getVarcIntValue(74);
    }

    public static void setZoom(int zoom) {
        Microbot.getClientThread().invokeLater(() -> Microbot.getClient().runScript(new Object[]{42, zoom, zoom}));
    }

    public static int getYaw() {
        return Microbot.getClient().getCameraYaw();
    }

    public static void setYaw(int yaw) {
        if (yaw >= 0 && yaw < 2048) {
            Microbot.getClient().setCameraYawTarget(yaw);
        }

    }

    public static void resetPitch() {
        if (getPitch() < 280) {
            setPitch(280);
        }

    }

    public static void resetZoom() {
        if (getZoom() > 200) {
            setZoom(200);
        }

    }

    public static boolean isTileCenteredOnScreen(LocalPoint tile, double marginPercentage) {
        Polygon poly = Perspective.getCanvasTilePoly(Microbot.getClient(), tile);
        if (poly == null) {
            return false;
        } else {
            Rectangle tileBounds = poly.getBounds();
            int viewportWidth = Microbot.getClient().getViewportWidth();
            int viewportHeight = Microbot.getClient().getViewportHeight();
            int centerX = viewportWidth / 2;
            int centerY = viewportHeight / 2;
            int marginX = (int)((double)viewportWidth * (marginPercentage / (double)100.0F));
            int marginY = (int)((double)viewportHeight * (marginPercentage / (double)100.0F));
            Rectangle centerBox = new Rectangle(centerX - marginX / 2, centerY - marginY / 2, marginX, marginY);
            return centerBox.contains(tileBounds);
        }
    }

    public static boolean isTileCenteredOnScreen(LocalPoint tile) {
        return isTileCenteredOnScreen(tile, (double)10.0F);
    }

    public static void centerTileOnScreen(LocalPoint tile, double marginPercentage) {
        int rawAngle = angleToTile(tile) - 90;
        int angle = rawAngle < 0 ? rawAngle + 360 : rawAngle;
        if (!isTileCenteredOnScreen(tile, marginPercentage)) {
            setAngle(angle, 5);
        }

    }

    public static void centerTileOnScreen(LocalPoint tile) {
        centerTileOnScreen(tile, (double)10.0F);
    }
}
