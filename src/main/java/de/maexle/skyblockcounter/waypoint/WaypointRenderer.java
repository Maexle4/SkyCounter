package de.maexle.skyblockcounter.waypoint;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;

public final class WaypointRenderer {
    private WaypointRenderer() {
    }

    public static void renderWaypoints(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        PrimitiveCollector collector = new PrimitiveCollector();
        for (WaypointManager.Waypoint waypoint : WaypointManager.getWaypoints()) {
            collector.renderWaypoint(waypoint.pos(), waypoint.label(), waypoint.color());
        }
    }
}
