package boatcam;



import boatcam.config.BoatCamConfig;
import boatcam.event.LookDirectionChangingEvent;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Field;
import java.util.List;

import static boatcam.config.BoatCamConfig.getConfig;
import static java.lang.Math.*;
import static net.minecraft.client.util.InputUtil.Type.KEYSYM;
import static net.minecraft.util.Formatting.GREEN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;

public class BoatCamMod implements ModInitializer, LookDirectionChangingEvent {
	// key binds
	private final KeyBinding TOGGLE = new KeyBinding("key.boatcam.toggle", KEYSYM, GLFW_KEY_B, "BoatCam");
	private final KeyBinding LOOK_BEHIND = new KeyBinding("key.boatcam.lookbehind", KEYSYM, -1, "BoatCam");

	// things to remember temporarily
	private Perspective perspective = null;
	private Vec3d boatPos = null;
	private float previousYaw = 0;
	private float[] camState = {0,0};

	// states
	private boolean lookingBehind = false;

	// mouse stuff
	private static double yawAccu = 0;
	private static float decay = 0.9F;
	private static float angVel = 0;
	private static float angAcc = 0;
	
	public static float getImpulseAndClearBuffer() {
		var temp = yawAccu;
		yawAccu = 0; 
		return (float) temp * getSteerSens();
	}
	public static void saveStates(float rawDecay, float rawAngVel, float rawAngAcc) {
		decay  = rawDecay;
		angVel = rawAngVel;
		angAcc = rawAngAcc;
	}
	public static float getRawAngVel(){
		return angVel;
	}
	public static float getRawAngAcc(){
		return angAcc;
	}
	public static float getOmega(){
		return (float) toRadians(angVel*20);
	}
	public static float getMu(){
		return 20F*(1F-decay);
	}
	public static void addImpulse(double delta){
		yawAccu += delta;
	}
	private static float getSteerSens(){
		return 0.015625F*getConfig().getSensitivity(); 
	}


	@Override
	public void onInitialize() {
		AutoConfig.register(BoatCamConfig.class, JanksonConfigSerializer::new);
		KeyBindingHelper.registerKeyBinding(TOGGLE);
		KeyBindingHelper.registerKeyBinding(LOOK_BEHIND);
		ClientTickEvents.START_WORLD_TICK.register(this::onClientEndWorldTick);
		LookDirectionChangingEvent.EVENT.register(this);
		AutoConfig.getGuiRegistry(BoatCamConfig.class).registerPredicateTransformer(
			(guis, s, f, c, d, g) -> dropdownToEnumList(guis, f),
			field -> BoatCamConfig.Perspective.class.isAssignableFrom(field.getType())
		);
		AutoConfig.getGuiRegistry(BoatCamConfig.class).registerPredicateTransformer(
				(guis, s, f, c, d, g) -> dropdownToEnumListCamMode(guis, f),
				field -> BoatCamConfig.CamMode.class.isAssignableFrom(field.getType())
		);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<AbstractConfigListEntry> dropdownToEnumList(List<AbstractConfigListEntry> guis, Field field) {
		return guis.stream()
			.filter(DropdownBoxEntry.class::isInstance)
			.map(DropdownBoxEntry.class::cast)
			// transform dropdown menu into enum list
			.map(dropdown -> ConfigEntryBuilder.create()
				.startEnumSelector(dropdown.getFieldName(), BoatCamConfig.Perspective.class, (BoatCamConfig.Perspective) dropdown.getValue())
				.setDefaultValue((BoatCamConfig.Perspective) dropdown.getDefaultValue().orElse(null))
				.setSaveConsumer(p -> {
					try {
						field.set(getConfig(), p);
					} catch (IllegalAccessException ignored) { }
				})
				.setEnumNameProvider(perspective -> switch ((BoatCamConfig.Perspective) perspective) {
					case FIRST_PERSON -> Text.translatable("text.autoconfig.boatcam.option.perspective.firstPerson");
					case THIRD_PERSON -> Text.translatable("text.autoconfig.boatcam.option.perspective.thirdPerson");
					case NONE -> Text.translatable("text.autoconfig.boatcam.option.perspective.none");
				})
				.build())
			.map(AbstractConfigListEntry.class::cast)
			.toList();
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<AbstractConfigListEntry> dropdownToEnumListCamMode(List<AbstractConfigListEntry> guis, Field field) {
		return guis.stream()
				.filter(DropdownBoxEntry.class::isInstance)
				.map(DropdownBoxEntry.class::cast)
				// transform dropdown menu into enum list
				.map(dropdown -> ConfigEntryBuilder.create()
						.startEnumSelector(dropdown.getFieldName(), BoatCamConfig.CamMode.class, (BoatCamConfig.CamMode) dropdown.getValue())
						.setDefaultValue((BoatCamConfig.CamMode) dropdown.getDefaultValue().orElse(null))
						.setSaveConsumer(p -> {
							try {
								field.set(getConfig(), p);
							} catch (IllegalAccessException ignored) { }
						})
						.setEnumNameProvider(cammode -> switch ((BoatCamConfig.CamMode) cammode) {
							case ANGULAR_VELOCITY -> Text.translatable("text.autoconfig.boatcam.option.cammode.angular");
							case FIXED -> Text.translatable("text.autoconfig.boatcam.option.cammode.fixed");
							case LINEAR_VELOCITY -> Text.translatable("text.autoconfig.boatcam.option.cammode.linear");
						})
						.build())
				.map(AbstractConfigListEntry.class::cast)
				.toList();
	}

	private void onClientEndWorldTick(ClientWorld world) {
		MinecraftClient client = MinecraftClient.getInstance();
		// key bind logic
		if (TOGGLE.wasPressed()) {
			getConfig().toggleBoatMode();
			client.inGameHud.setOverlayMessage(Text.literal(getConfig().isBoatMode() ? "Boat mode" : "Normal mode").styled(s -> s.withColor(GREEN)), false);
		}
		// camera logic
		assert client.player != null;
		if (getConfig().isBoatMode() && client.player.getVehicle() instanceof BoatEntity boat) {
			calculateYaw(client.player, boat);
			// first tick riding in boat mode
			if (perspective == null) {
				// fix pitch if configured
				if (getConfig().shouldFixPitch()) {
					client.player.setPitch(getConfig().getPitch());
				}
				// init look behind
				lookingBehind = false;
				// save perspective
				perspective = client.options.getPerspective();
				// set perspective
				switch (getConfig().getPerspective()) {
					case FIRST_PERSON -> client.options.setPerspective(Perspective.FIRST_PERSON);
					case THIRD_PERSON -> client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
				}
			}
			// if pressed state changed
			if (LOOK_BEHIND.isPressed() != lookingBehind) {
				// save state
				lookingBehind = LOOK_BEHIND.isPressed();
				// handle change
				invertPitch();
				if (lookingBehind) {
					// set look back perspective
					client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
				} else {
					// reset perspective
					switch (getConfig().getPerspective()) {
						case FIRST_PERSON -> client.options.setPerspective(Perspective.FIRST_PERSON);
						case THIRD_PERSON -> client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
						default -> resetPerspective();
					}
				}
			}
		} else {
			// first tick after disabling boat mode or leaving boat
			if (perspective != null) {
				resetPerspective();
				// invert pitch if looking behind
				if (lookingBehind) {
					invertPitch();
					lookingBehind = false;
				}
			}
		}
	}

	private void invertPitch() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		assert player != null;
		player.setPitch(-player.getPitch());
	}

	private void resetPerspective() {
		if (perspective != null) {
			MinecraftClient.getInstance().options.setPerspective(perspective);
			perspective = null;
		}
	}
	
	private float[] CriticalImpulseResponse(float t, float d, float v, float n) {
		float e = (float) Math.exp(-t*n);
		float[] phi = new float[2];
		phi[0] = e * ( d*(1+t*n   ) + v*(  t  ) ) ;
		phi[1] = e * ( d*( -t*n*n ) + v*(1-t*n) ) ;
		return phi;
	}
		

	private void calculateYaw(ClientPlayerEntity player, BoatEntity boat) {
		// yaw calculations
		float boatYaw = boat.getYaw();
		float yaw = boatYaw;

		switch(getConfig().getCamMode()) {
			case ANGULAR_VELOCITY: // rotation match
				float rawAngVel = getRawAngVel();
				float deltaYaw = MathHelper.wrapDegrees(camState[0]-boatYaw);
		                float maxRate = 4000F/22F/9F; // unit: s^-1
				float[] response = CriticalImpulseResponse(1F/20F, deltaYaw, camState[1], maxRate*getConfig().getSmoothness());
				if (response[0]*response[0] >= 8100F) { // if exceed 90deg, clip position and velocity
					response[0] = 90F*Math.signum(response[0]);
					response[1] = rawAngVel*20;
				}
				camState[0] = MathHelper.wrapDegrees(boatYaw + response[0]);
				camState[1] = response[1];
				yaw = camState[0];
				break;
			case LINEAR_VELOCITY: // momentum match, original boatcam
				if (boatPos != null) {
					double dz = boat.getZ() - boatPos.z, dx = boat.getX() - boatPos.x;
					if (dx != 0 || dz != 0) {
						float vel = (float) hypot(dz, dx);
						float direction = (float) toDegrees(atan2(dz, dx)) - 90;
						float t = min(1, vel / 3); // max 70 m/s = 3.5 m/tick on blue ice, cut off at 3
						yaw = AngleUtil.lerp(t, yaw, direction);
					}
					yaw = AngleUtil.lerp(getConfig().getSmoothness(), previousYaw, yaw);
				}
				break;
			case FIXED:
			default:
			    break;
		}

		player.setYaw(yaw);
		// save pos and yaw
		previousYaw = yaw;
		boatPos = boat.getPos();
	}

	@Override
	public boolean onLookDirectionChanging(double dx, double dy) {
		if (getConfig().isBoatMode()) {
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null && player.getVehicle() instanceof BoatEntity && (dx != 0 || getConfig().shouldFixPitch() && dy != 0)) {
				// mouse accumulator
                addImpulse(dx);
				// prevent horizontal camera movement and cancel camera change by returning true
				// prevent vertical movement as well if configured
				player.changeLookDirection(0, getConfig().shouldFixPitch() ? 0 : dy);
				return true;
			}
		}
		return false;
	}
}
