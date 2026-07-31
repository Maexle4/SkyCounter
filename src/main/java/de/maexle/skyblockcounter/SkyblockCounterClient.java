package de.maexle.skyblockcounter;

import de.maexle.skyblockcounter.command.SkyblockCounterCommand;
import de.maexle.skyblockcounter.gui.SkyblockCounterConfigScreen;
import de.maexle.skyblockcounter.waypoint.WaypointRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SkyblockCounterClient implements ClientModInitializer {

    private static final KeyBinding.Category SKYCOUNTER_CATEGORY = KeyBinding.Category.create(SkyblockCounter.id("general"));
    private static KeyBinding toggleHudKey;
    public static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockcounter.togglehud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                SKYCOUNTER_CATEGORY
        ));
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockcounter.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                SKYCOUNTER_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                boolean currentVisible = SkyblockCounterService.isGuiVisible();
                SkyblockCounterService.setGuiVisible(!currentVisible);
            }
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new SkyblockCounterConfigScreen(null));
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            SkyblockCounterCommand.register(dispatcher);
        });

        SkyblockCounterService serviceInstance = new SkyblockCounterService();
        serviceInstance.startEventTracking();

        WorldRenderEvents.AFTER_ENTITIES.register(WaypointRenderer::renderWaypoints);
    }
}