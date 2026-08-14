package de.maexle.skyblockcounter.waypoint;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

public final class PrimitiveCollector {
    private static final float MARKER_SIZE = 10.0F;
    private static final float OUTLINE_WIDTH = 2.0F;
    private static final float LABEL_SCALE = 1.0F;

    public void renderWaypoint(BlockPos position, String label, int configuredColor) {
        int color = opaqueArgb(configuredColor);
        Vec3 center = position.getCenter();

        Gizmos.cuboid(position, 0.01F, GizmoStyle.stroke(color, OUTLINE_WIDTH)).setAlwaysOnTop();
        Gizmos.point(center, color, MARKER_SIZE).setAlwaysOnTop();

        if (!label.isBlank()) {
            Gizmos.billboardText(
                    label,
                    center.add(0.0, 1.35, 0.0),
                    TextGizmo.Style.forColorAndCentered(color).withScale(LABEL_SCALE)
            ).setAlwaysOnTop();
        }
    }

    private static int opaqueArgb(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }
}
