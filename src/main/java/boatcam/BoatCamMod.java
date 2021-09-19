package boatcam;

import boatcam.config.BoatCamConfig;
import boatcam.event.LookDirectionChangingEvent;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;

import static boatcam.config.BoatCamConfig.getConfig;
import static java.lang.Math.*;
import static net.minecraft.client.util.InputUtil.Type.KEYSYM;
import static net.minecraft.util.Formatting.GREEN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;

public class BoatCamMod implements ModInitializer, LookDirectionChangingEvent {
    private final KeyBinding TOGGLE = new KeyBinding("Toggle", KEYSYM, GLFW_KEY_B, "BoatCam");
    private Vec3d boatPos = null;
    private float previousYaw;

    @Override
    public void onInitialize() {
        AutoConfig.register(BoatCamConfig.class, JanksonConfigSerializer::new);
        KeyBindingHelper.registerKeyBinding(TOGGLE);
        ClientTickEvents.START_WORLD_TICK.register(this::onClientEndWorldTick);
        LookDirectionChangingEvent.EVENT.register(this);
    }

    private void onClientEndWorldTick(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        // key bind logic
        if (TOGGLE.wasPressed()) {
            getConfig().toggleBoatMode();
            client.inGameHud.setOverlayMessage(new LiteralText(getConfig().isBoatMode() ? "Boat mode" : "Normal mode").styled(s -> s.withColor(GREEN)), false);
        }
        // boat cam logic
        if (getConfig().isBoatMode() && client.player != null && client.player.getVehicle() instanceof BoatEntity boat) {
            float yaw = boat.getYaw();
            if (boatPos != null) {
                double dz = boat.getZ() - boatPos.z, dx = boat.getX() - boatPos.x;
                if (dx != 0 || dz != 0) {
                    float vel = (float) hypot(dz, dx); // max 2m/tick
                    float direction = (float) toDegrees(atan2(dz, dx)) - 90;
                    float t = min(1, vel / 2);
                    // TODO sqrt, normal or square?
                    yaw = AngleUtil.lerp((float) sqrt(t), yaw, direction);
                }
            }
            yaw = AngleUtil.lerp(getConfig().getSmoothness(), previousYaw, yaw);
            client.player.setYaw(yaw);
            // save pos and yaw
            previousYaw = yaw;
            boatPos = boat.getPos();
        } else boatPos = null; // reset boatPos
    }

    @Override
    public boolean onLookDirectionChanging(double dx, double dy) {
        if (getConfig().isBoatMode()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && player.getVehicle() instanceof BoatEntity && dx != 0) {
                // prevent vertical camera movement and cancel camera change by returning true
                player.changeLookDirection(0, dy);
                return true;
            }
        }
        return false;
    }
}