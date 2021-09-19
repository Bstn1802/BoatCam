package boatcam.mixin;

import boatcam.BoatCamMod;
import boatcam.event.LookDirectionChangingEvent;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin{
    @Inject(at = @At("INVOKE"), method = "changeLookDirection", cancellable = true)
    public void changeLookDirection(double dx, double dy, CallbackInfo info) {
        if (LookDirectionChangingEvent.EVENT.invoker().onLookDirectionChanging(dx, dy)) {
            info.cancel();
        }
    }
}