package de.maexle.skyblockcounter.waypoint;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class WaypointRenderer {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void renderWaypoints(WorldRenderContext context) {
        if (client.world == null || client.player == null) {
            return;
        }

        MatrixStack matrices = context.matrices();
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        VertexConsumerProvider consumers = context.consumers();

        if (consumers == null) {
            return;
        }

        PrimitiveCollector collector = new PrimitiveCollector(consumers, matrices, cameraPos);

        for (WaypointManager.Waypoint wp : WaypointManager.getWaypoints()) {
            collector.renderPoint(wp.pos());
        }

        collector.draw();
    }
}