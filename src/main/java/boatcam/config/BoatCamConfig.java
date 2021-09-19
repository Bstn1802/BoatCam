package boatcam.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@SuppressWarnings("unused")
@Config(name = "boatcam")
public final class BoatCamConfig implements ConfigData {
    @Comment("Whether the camera should be controlled by this mod or not.\nCan be toggled using a key bind")
    private boolean boatMode;
    @Comment("1 - Smooth camera, might even lag behind.\n100 - Camera angle might change very abruptly.")
    @BoundedDiscrete(min = 1, max = 100)
    private int smoothness;

    private BoatCamConfig() { }

    public static BoatCamConfig getConfig() {
        return AutoConfig.getConfigHolder(BoatCamConfig.class).get();
    }

    public static void saveConfig() {
        AutoConfig.getConfigHolder(BoatCamConfig.class).save();
    }

    public float getSmoothness() {
        return smoothness / 100f;
    }

    public boolean isBoatMode() {
        return boatMode;
    }

    public void toggleBoatMode() {
        boatMode = !boatMode;
        saveConfig();
    }
}