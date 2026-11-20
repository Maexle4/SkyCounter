package de.maexle.skyblockcounter;

import de.maexle.skyblockcounter.command.SkyblockCounterCommand;
import de.maexle.skyblockcounter.gui.SkyblockCounterConfigScreen;
import de.maexle.skyblockcounter.waypoint.WaypointRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class SkyblockCounterClient implements ClientModInitializer {

    private static final KeyMapping.Category SKYCOUNTER_CATEGORY = KeyMapping.Category.register(SkyblockCounter.id("general"));
    private static KeyMapping toggleHudKey;
    private static KeyMapping debugKey;
    private static KeyMapping hudEditorKey;
    public static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skyblockcounter.togglehud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                SKYCOUNTER_CATEGORY
        ));
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skyblockcounter.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                SKYCOUNTER_CATEGORY
        ));
        debugKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skyblockcounter.toggle_debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                SKYCOUNTER_CATEGORY
        ));
        hudEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skyblockcounter.hud_editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                SKYCOUNTER_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.consumeClick()) {
                boolean currentVisible = SkyblockCounterService.isGuiVisible();
                SkyblockCounterService.setGuiVisible(!currentVisible);
            }
            while (debugKey.consumeClick()) {
                SkyblockCounterService.toggleDebugMode();
            }
            while (hudEditorKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new de.maexle.skyblockcounter.gui.HudEditorScreen(null));
                }
            }
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new SkyblockCounterConfigScreen(null));
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            SkyblockCounterCommand.register(dispatcher);
        });

        SkyblockCounterService serviceInstance = new SkyblockCounterService();
        serviceInstance.startEventTracking();

        LevelRenderEvents.BEFORE_GIZMOS.register(WaypointRenderer::renderWaypoints);
    }
}
