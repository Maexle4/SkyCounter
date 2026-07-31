package de.maexle.skyblockcounter.waypoint;

import de.maexle.skyblockcounter.gui.SkyblockCounterActions;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

public class WaypointManager {
    public record Waypoint(BlockPos pos, String label) {}

    private static final List<Waypoint> waypoints = new ArrayList<>();

    public static void setWaypoints(List<Waypoint> newWaypoints) {
        waypoints.clear();
        waypoints.addAll(newWaypoints);
        for (WaypointManager.Waypoint wp : newWaypoints){
            SkyblockCounterActions.addWaypoint(wp.label, wp.pos().getX(), wp.pos().getY(), wp.pos().getZ(), 65280);
        }

    }

    public static void clear() {
        waypoints.clear();
    }

    public static List<Waypoint> getWaypoints() {
        return waypoints;
    }
}



