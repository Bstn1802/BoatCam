package boatcam;

import net.minecraft.util.math.MathHelper;

public final class AngleUtil {
    private AngleUtil() { }

    // lerps two angles the shortest way (e.g. lerp(0.5, -135, 135) -> -180)
    // resulting angle should be in range [-180, 180)
    public static float lerp(float t, float a, float b) {
        // make sure a and b are in range [-180, 180)
        a = mod(a);
        b = mod(b);
        // make sure b is greater than a
        if (a > b) return lerp(1 - t, b, a);
        // lerp if already close together
        if (b - a < 180) return MathHelper.lerp(t, a, b);
        // otherwise add 360 so it lerps the other way around the angle circle
        a += 360;
        float result = MathHelper.lerp(1 - t, b, a);
        if (result >= 180) result -= 360;
        return result;
    }

    // makes sure the angle is in range [-180, 180)
    public static float mod(float angle) {
        return angle - (float) Math.floor(angle / 360 + 0.5) * 360;
    }
}