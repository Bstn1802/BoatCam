package boatcam.mixin;


import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



import static boatcam.config.BoatCamConfig.getConfig;
import static boatcam.BoatCamMod.getImpulseAndClearBuffer;
import static boatcam.BoatCamMod.saveMu;
import static boatcam.BoatCamMod.saveOmega;
import static boatcam.BoatCamMod.getOmega;
import static boatcam.BoatCamMod.getMu;

@Mixin(BoatEntity.class)
public class BoatEntityMixin {
	@Inject(method = "copyEntityData", at = @At("INVOKE"), cancellable = true)
	private void copyEntityData(Entity entity, CallbackInfo info) {
		// disable turn limit in a boat if entity is the player and always disable turn limit is enabled or player is in boat mode
		if (entity.equals(MinecraftClient.getInstance().player) && (getConfig().isTurnLimitDisabled() || getConfig().isBoatMode())) {
			// just copied the code and cancelled, easier than making a complicated mixin
			//noinspection ConstantConditions
			float yaw = ((Entity) (Object) this).getYaw();
			entity.setBodyYaw(yaw);
			entity.setHeadYaw(entity.getYaw());
			info.cancel();
		}
	}
}


@Mixin(BoatEntity.class)
abstract class PaddleMixin {
    @Shadow private boolean pressingLeft;
    @Shadow private boolean pressingRight;
    @Shadow private boolean pressingForward;
    @Shadow private boolean pressingBack;
    @Shadow private float yawVelocity;
    @Shadow private float nearbySlipperiness;


    @Shadow private double boatYaw;

    @Shadow @Nullable public abstract Entity getPrimaryPassenger();

    @Redirect(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;updatePaddles()V"))
    private void replaceUpdatePaddles(BoatEntity instance) {
        if (instance.hasPassengers()) {
            Entity driver = this.getPrimaryPassenger();
            float f = 0.0F;
            float MAXTORQUE = 1.0F;
            float steer = getImpulseAndClearBuffer();
            float val = Math.min(1.0F,Math.max(-1.0F,steer + (this.pressingRight?1.0F:0.0F) + (this.pressingLeft?-1.0F:0.0F)));

            this.yawVelocity += MAXTORQUE*val;
            saveOmega(this.yawVelocity);
            saveMu(this.nearbySlipperiness);

            if (val!=0.0F && !this.pressingForward && !this.pressingBack) {
                f += 0.005F*Math.abs(val);
            }

            instance.setYaw(instance.getYaw() + this.yawVelocity);
            if (this.pressingForward) {
                f += 0.04F;
            }

            if (this.pressingBack) {
                f -= 0.005F;
            }

            instance.setVelocity(instance.getVelocity().add((double)(MathHelper.sin(-instance.getYaw() * 0.017453292F) * f), 0.0D, (double)(MathHelper.cos(instance.getYaw() * 0.017453292F) * f)));
            instance.setPaddleMovings(val>0 || this.pressingForward, val<0 || this.pressingForward);
        }
    }
}