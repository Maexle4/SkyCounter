package de.maexle.skyblockcounter.waypoint;

import de.maexle.skyblockcounter.gui.SkyblockCounterActions;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

public class WaypointManager {
    public record Waypoint(BlockPos pos, String label, int color) {
        public Waypoint(BlockPos pos, String label) {
            this(pos, label, 0xFF00FF00);
        }
    }

    private static final List<Waypoint> waypoints = new ArrayList<>();

    public static void setWaypoints(List<Waypoint> newWaypoints) {
        waypoints.clear();
        waypoints.addAll(newWaypoints);
        for (WaypointManager.Waypoint wp : newWaypoints){
            SkyblockCounterActions.addWaypoint(wp.label, wp.pos().getX(), wp.pos().getY(), wp.pos().getZ(), wp.color());
        }

    }

    public static void clear() {
        waypoints.clear();
    }

    /**
     * Drops every waypoint from memory and from the saved config. Used when the player
     * changes world so coordinates from the previous island do not linger.
     */
    public static void clearAll() {
        waypoints.clear();
        de.maexle.skyblockcounter.SkyblockCounterConfig config =
                de.maexle.skyblockcounter.SkyblockCounterService.getConfig();
        if (!config.waypoints.isEmpty()) {
            config.waypoints.clear();
            config.save();
        }
    }

    public static List<Waypoint> getWaypoints() {
        return waypoints;
    }
}



