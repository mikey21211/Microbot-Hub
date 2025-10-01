package net.runelite.client.plugins.microbot.ScreenRotation;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.ScreenRotation.ScreenRotationHelper;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import javax.inject.Inject;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ScreenRotationScript extends Script {


    public static double version = 1.0;
    @Inject
    private Notifier notifier;

    public static boolean test = false;

    boolean otherCameraChangedFlag;

    WorldPoint todtCenter = new WorldPoint(1639, 3993, 0);

    public boolean run(ScreenRotationConfig config) {
        Microbot.enableAutoRunOn = false;
        otherCameraChangedFlag = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                //Change rotation based on dice roll
                if(Rs2Random.dicePercentage(0.64)) {
                    rotateToTargetAngleWithJitter(todtCenter);
                }
                //Change the screen height based on random chance
                if(Rs2Random.dicePercentage(0.48)) {
                    rotateCameraPitchWithJitter();
                }

                //Change the screen height based on random chance
                if(Rs2Random.dicePercentage(0.27)) {
                    //randomCameraZoom();
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Rotate the screen, centered on the center of the agility course or similar.
     *
     * @param target The center of the agility course
     */
    private void rotateToTargetAngleWithJitter(WorldPoint target) {

        // Calculate yaw (left/right)
        int baseYaw = ScreenRotationHelper.angleToTile(target);
        int correctedYaw = (baseYaw - 90 + 360) % 360;

        // Add Gaussian overshoot/undershoot
        int yawJitter = (int) Rs2Random.gaussRand(0, 30); // 60° std dev
        int targetYaw = (correctedYaw + yawJitter + 360) % 360;

        //Clamp target yaw
        targetYaw = Math.max(0, Math.min(2048, targetYaw));

        // Rotate camera yaw
        ScreenRotationHelper.setAngle(targetYaw, 10); // 0° threshold for stopping
    }

    /**
     * Rotate the screen pitch, with randomness.
     */
    private void rotateCameraPitchWithJitter() {

        // Calculate random pitch
        int rawPitch = (int) Rs2Random.gaussRand(266, 38);
        int clampedPitch = Math.max(128, Math.min(383, rawPitch));
        float percentage = (float) (clampedPitch - 128) / (383 - 128);

        //Rotate camera pitch
        ScreenRotationHelper.adjustPitch(percentage);
    }

    private void randomCameraZoom()
    {
        ScreenRotationHelper.setZoom(Rs2Random.betweenInclusive(40,60));
    }

    private int calculateSleepDuration(double multiplier) {
        // Create a Random object
        Random random = new Random();

        // Calculate the mean (average) of sleepMin and sleepMax, adjusted by sleepTarget
        int sleepMin = 58;
        int sleepMax = 1200;
        int sleepTarget = 440;

        double mean = (sleepMin + sleepMax + sleepTarget) / 3.0;

        // Calculate the standard deviation with added noise
        double noiseFactor = 0.2; // Adjust the noise factor as needed (0.0 to 1.0)
        double stdDeviation = Math.abs(sleepTarget - mean) / 3.0 * (1 + noiseFactor * (random.nextDouble() - 0.5) * 2);

        // Generate a random number following a normal distribution
        int sleepDuration;
        do {
            // Generate a random number using nextGaussian method, scaled by standard deviation
            sleepDuration = (int) Math.round(mean + random.nextGaussian() * stdDeviation);
        } while (sleepDuration < sleepMin || sleepDuration > sleepMax); // Ensure the duration is within the specified range
        if ((int) Math.round(sleepDuration * multiplier) < 60) sleepDuration += ((60-sleepDuration)+Rs2Random.between(11,44));
        return sleepDuration;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}