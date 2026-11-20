package de.maexle.skyblockcounter;

import de.maexle.skyblockcounter.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks Corleone respawn windows per boss structure.
 *
 * <p>Corleone "seemingly spawns every 60 or 120 seconds alternately in these structures" (wiki),
 * and two structures can be active at the same time. Every structure therefore gets its own
 * countdown, and each one shows the two candidate windows (60s and 120s) side by side so the
 * player can tell which one is coming up.
 *
 * <p>A kill is attributed to the nearest scanned structure, so only that structure's timer
 * restarts while the others keep running.
 */
public final class RespawnTimer {

    /** Short respawn window. */
    public static final long SHORT_WINDOW_MS = 60_000L;

    /** Long respawn window. */
    public static final long LONG_WINDOW_MS = 120_000L;

    /** How long a finished timer keeps showing READY before it hides itself. */
    public static final long LINGER_AFTER_ELAPSE_MS = 15_000L;

    private static final Logger LOGGER = LoggerFactory.getLogger("skyblockcounter");

    /** Per-structure state, ordered the same way as the waypoints from the scan. */
    private static final List<StructureTimer> timers = new ArrayList<>();

    /** Colour used for the first structure, matching the scan waypoint palette. */
    private static final int[] PALETTE = {
            0xFFB16EFF, 0xFF4ADE80, 0xFF60A5FA, 0xFFFBBF24, 0xFFF472B6, 0xFF22D3EE
    };

    private RespawnTimer() {
    }

    /** One countdown belonging to a single Corleone structure. */
    public static final class StructureTimer {
        private final String label;
        private final BlockPos pos;
        private final int color;

        /** True once this structure has seen at least one kill in this session. */
        private boolean started = false;

        /** The moment of the last kill; both windows are measured from here. */
        private long killTime = 0L;

        private StructureTimer(String label, BlockPos pos, int color) {
            this.label = label;
            this.pos = pos;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public BlockPos pos() {
            return pos;
        }

        public int color() {
            return color;
        }

        public boolean started() {
            return started;
        }

        /**
         * Remaining millis for one of the two candidate windows.
         *
         * <p>Both windows start counting at the same moment (the kill), they are not
         * alternatives: Corleone can come back after 60s <em>or</em> after 120s, so the short
         * countdown reaching zero does not stop the long one. Slot 0 is the 60s window and
         * slot 1 is the 120s window.
         */
        public long remainingForSlot(int slot) {
            long window = slot == 0 ? SHORT_WINDOW_MS : LONG_WINDOW_MS;
            if (!started) {
                return window;
            }
            long deadline = killTime + window;
            return Math.max(0L, deadline - System.currentTimeMillis());
        }

        /** True when that window has already passed since the kill. */
        public boolean slotElapsed(int slot) {
            return started && remainingForSlot(slot) <= 0L;
        }

        /** Progress of one window, 0..1. */
        public float progressForSlot(int slot) {
            if (!started) {
                return 0.0F;
            }
            long window = slot == 0 ? SHORT_WINDOW_MS : LONG_WINDOW_MS;
            if (window <= 0L) {
                return 1.0F;
            }
            long elapsed = System.currentTimeMillis() - killTime;
            return Math.min(1.0F, Math.max(0.0F, (float) elapsed / (float) window));
        }

        /** True once both windows are over. */
        public boolean hasElapsed() {
            return started && remainingForSlot(1) <= 0L;
        }

        /**
         * The whole row disappears only after the long window plus the linger period, so the
         * player still sees the row during the 60s..120s span.
         */
        public boolean shouldHide() {
            return started && System.currentTimeMillis() > killTime + LONG_WINDOW_MS + LINGER_AFTER_ELAPSE_MS;
        }

        /**
         * Restarts both countdowns from now. Called when a Corleone dies at this structure.
         */
        public void startCycle() {
            killTime = System.currentTimeMillis();
            started = true;
        }

        /** Returns the timer to its idle state. */
        public void reset() {
            started = false;
            killTime = 0L;
        }
    }

    /**
     * Rebuilds the timer list from the scanned waypoints. Structure timers that already exist
     * keep their countdown, so a rescan does not lose progress.
     */
    public static void syncFromWaypoints() {
        List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();

        List<StructureTimer> rebuilt = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            WaypointManager.Waypoint wp = waypoints.get(i);

            StructureTimer existing = null;
            for (StructureTimer timer : timers) {
                if (timer.pos.equals(wp.pos())) {
                    existing = timer;
                    break;
                }
            }

            if (existing != null) {
                rebuilt.add(existing);
            } else {
                rebuilt.add(new StructureTimer(wp.label(), wp.pos(), PALETTE[i % PALETTE.length]));
            }
        }

        timers.clear();
        timers.addAll(rebuilt);
        LOGGER.info("Respawn timers synced with " + timers.size() + " structure(s)");
    }

    public static List<StructureTimer> getTimers() {
        return timers;
    }

    public static boolean hasTimers() {
        return !timers.isEmpty();
    }

    /** Runs a countdown on every known structure, used by debug mode / manual starts. */
    public static void startAll() {
        if (timers.isEmpty()) {
            return;
        }
        for (StructureTimer timer : timers) {
            timer.startCycle();
        }
    }

    /**
     * Restarts the countdown of the structure closest to the given position, which is how a
     * Corleone kill is attributed when several structures are active at once.
     *
     * @return the timer that was restarted, or null when no structure is known
     */
    public static StructureTimer startNearest(BlockPos deathPos) {
        if (timers.isEmpty()) {
            return null;
        }

        StructureTimer nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (StructureTimer timer : timers) {
            double distance = timer.pos.distSqr(deathPos);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = timer;
            }
        }

        if (nearest != null) {
            nearest.startCycle();
            LOGGER.info("Corleone kill attributed to '" + nearest.label + "' (distance "
                    + String.format("%.1f", Math.sqrt(bestDistance)) + " blocks), both 60s and 120s windows restarted");
        }
        return nearest;
    }

    /**
     * Fallback when no structure has been scanned: keeps a single virtual timer so the HUD
     * still works without running the Corleone finder first.
     */
    public static void startFallback() {
        if (!timers.isEmpty()) {
            startAll();
            return;
        }
        StructureTimer virtual = new StructureTimer("Corleone", BlockPos.ZERO, PALETTE[0]);
        virtual.startCycle();
        timers.add(virtual);
        LOGGER.info("No structure scanned, using a virtual respawn timer");
    }

    public static void stopAll() {
        timers.clear();
    }

    /** True when at least one timer is still counting or lingering. */
    public static boolean anyVisible() {
        for (StructureTimer timer : timers) {
            if (timer.started && !timer.shouldHide()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears finished timers back to their idle state. The structure entries themselves are
     * kept, because they map to scanned waypoints and must survive between kills.
     */
    public static void pruneFinished() {
        for (StructureTimer timer : timers) {
            if (timer.started && timer.shouldHide()) {
                timer.reset();
            }
        }
    }

    /** Formatted as mm:ss, rounding up so the last second is not skipped. */
    public static String format(long millis) {
        long totalSeconds = (Math.max(0L, millis) + 999L) / 1000L;
        return String.format("%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }
}
