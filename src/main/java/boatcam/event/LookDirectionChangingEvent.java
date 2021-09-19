package boatcam.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface LookDirectionChangingEvent {
    Event<LookDirectionChangingEvent> EVENT = EventFactory.createArrayBacked(LookDirectionChangingEvent.class, listeners -> (dx, dy) -> {
        boolean cancel = false;
        for (LookDirectionChangingEvent listener : listeners) {
            if (listener.onLookDirectionChanging(dx, dy)) {
                cancel = true;
            }
        }
        return cancel;
    });

    // called when the camera is about to move, cancelled when true is returned
    boolean onLookDirectionChanging(double dx, double dy);
}