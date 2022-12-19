package boatcam.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@SuppressWarnings({ "unused", "FieldCanBeLocal", "FieldMayBeFinal" })
@Config(name = "boatcam")
public final class BoatCamConfig implements ConfigData {
    @Comment("Whether the camera should be controlled by this mod or not.\nNOTE: This setting can be toggled using a key bind.")
    private boolean boatMode = false;
    @Comment("Fixed - yaw follows boat heading.\nKartrider - yaw follows rate of rotation.\nClassic - yaw follows boat velocity.")
    private CamMode camMode = CamMode.ANGULAR_VELOCITY;
    @Comment("Sensitivity of mouse steering, on a semitone scale.")
    @BoundedDiscrete(min = -24, max = 24)
    private int sensitivity = 0;
    @Comment("How quickly the camera should follow the calculated angle.\nFixed Mode - No effect\nKartrider Mode - corresponds to critical damping factor\nClassic Mode - corresponds to average-weighting.")
    @BoundedDiscrete(min = 1, max = 100)
    private int smoothness = 50;
    @Comment("Perspective when riding a boat in boat mode. Perspective wont change when this is set to none.")
    private Perspective perspective = Perspective.NONE;
    @Comment("Whether to fix the camera angle at a certain pitch.")
    private boolean fixedPitch = false;
    @Comment("Fixed vertical angle of the camera when fixedPitch is enabled.")
    @BoundedDiscrete(min = -90, max = 90)
    private int pitch = 25;
    @Comment("Disables the turn limit in a boat.\nNOTE: The turn limit is always disabled in boat mode!")
    private boolean turnLimitDisabled = false;

    private BoatCamConfig() { }

    @Override
    public void validatePostLoad() {
        if(smoothness < 1 || smoothness > 100) smoothness = 50;
        if(perspective == null) perspective = Perspective.NONE;
        if(camMode == null) camMode = CamMode.FIXED;
    }

    public static BoatCamConfig getConfig() {
        return AutoConfig.getConfigHolder(BoatCamConfig.class).get();
    }

    public static void saveConfig() {
        AutoConfig.getConfigHolder(BoatCamConfig.class).save();
    }

    public float getSmoothness() {
        return smoothness / 100f;
    }

    public CamMode getCamMode() {
        return camMode;
    }

    public float getSensitivity() {
        return (float) Math.pow(2.0f,sensitivity/12.0f);
    }

    public boolean isBoatMode() {
        return boatMode;
    }

    public boolean shouldFixPitch() {
        return fixedPitch;
    }

    public int getPitch() {
        return pitch;
    }

    public void toggleBoatMode() {
        boatMode = !boatMode;
        saveConfig();
    }

    public Perspective getPerspective() {
        return perspective;
    }

    public boolean isTurnLimitDisabled() {
        return turnLimitDisabled;
    }

    public enum Perspective {
        NONE, FIRST_PERSON, THIRD_PERSON
    }

    public enum CamMode {
        FIXED, ANGULAR_VELOCITY, LINEAR_VELOCITY
    }
}
