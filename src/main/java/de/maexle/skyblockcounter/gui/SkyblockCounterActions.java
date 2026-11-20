package de.maexle.skyblockcounter.gui;

import de.maexle.skyblockcounter.SkyblockCounterConfig;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.waypoint.WaypointManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SkyblockCounterActions {

    private SkyblockCounterActions() {
    }

    public record CorleoneFinderResult(boolean success, List<String> messages) {
        public static CorleoneFinderResult notInWorld() {
            return new CorleoneFinderResult(false, List.of("Must be in a world"));
        }

        public static CorleoneFinderResult nothingFound() {
            return new CorleoneFinderResult(true, List.of("Neither structure was found in the loaded chunks."));
        }
    }

    public record MobSwitchResult(boolean success, String message) {
    }

    public record MobAddResult(String message) {
    }

    public static void switchToTreasureHoarder() {
        SkyblockCounterService.switchMob("treasure_hoarder_70", "Treasure Hoarder");
    }

    public static void switchToCorleone() {
        SkyblockCounterService.switchMob("team_treasurite_corleone_200", "Corleone");
    }

    public static void switchToZealot() {
        SkyblockCounterService.switchMob("zealot_enderman_55", "Zealot");
    }

    public static MobSwitchResult switchToMob(String id) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        Optional<SkyblockCounterConfig.MobEntry> entry = config.getMobEntries().stream()
                .filter(e -> e.id.equals(id))
                .findFirst();

        if (entry.isEmpty()) {
            return new MobSwitchResult(false, "Mob not found: " + id);
        }

        SkyblockCounterConfig.MobEntry mob = entry.get();
        SkyblockCounterService.switchMob(mob.id, mob.name);
        return new MobSwitchResult(true, "Switched to " + mob.name);
    }

    public static List<SkyblockCounterConfig.MobEntry> listMobs() {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        if (config.getMobEntries().isEmpty()) {
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleone", "textures/gui/sprites/boss_corleone_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            config.save();
            SkyblockCounterService.reloadMobTextures();
        }
        return config.getMobEntries();
    }

    public static MobAddResult addMob(String id, String name, String texture) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        String textureValue = texture != null && !texture.isBlank() ? texture : null;
        config.addMobEntry(id, name, textureValue);
        config.save();
        SkyblockCounterService.reloadMobTextures();
        String textureInfo = textureValue != null ? " (Texture: " + textureValue + ")" : "";
        return new MobAddResult("Added mob: " + name + " (" + id + ")" + textureInfo);
    }

    public static String removeMob(String id) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.removeMobEntry(id);
        config.save();
        SkyblockCounterService.reloadMobTextures();
        return "Removed mob: " + id;
    }

    public static void setHudPosition(int x, int y) {
        SkyblockCounterService.setHudPosition(x, y);
    }

    public static void setTimerPosition(int x, int y) {
        SkyblockCounterService.setTimerPosition(x, y);
    }

    public static void setApiKey(String apiKey) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.setAPI_KEY(apiKey);
        SkyblockCounterService.setApiKey(apiKey);
        config.save();
    }

    public static void setUuid(String uuid) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        String undashedUuid = uuid.replace("-", "");
        config.setundashedUuid(undashedUuid);
        SkyblockCounterService.setUndashedUuid(undashedUuid);
        config.save();
    }

    /** Cycles through the four counting modes. */
    public static de.maexle.skyblockcounter.CountingMode cycleCountingMode() {
        return SkyblockCounterService.cycleCountingMode();
    }

    public static void setCountingMode(de.maexle.skyblockcounter.CountingMode mode) {
        SkyblockCounterService.setCountingMode(mode);
    }

    public static void addSessionKills(int amount) {
        SkyblockCounterService.addSessionKills(amount);
    }

    public static void removeSessionKills(int amount) {
        SkyblockCounterService.removeSessionKills(amount);
    }

    public static void resetSession() {
        SkyblockCounterService.setStartSessionKills(0);
    }

    public static Optional<String> formatTimeFarmed() {
        if (!SkyblockCounterService.isLocalCounting()) {
            return Optional.empty();
        }

        long totalSeconds = (System.currentTimeMillis() - SkyblockCounterService.sessionStartTime) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return Optional.of(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    public static Optional<String> formatCorleonitePerHour() {
        if (!SkyblockCounterService.isLocalCounting()) {
            return Optional.empty();
        }

        long timePassed = (System.currentTimeMillis() - SkyblockCounterService.sessionStartTime) / 1000;
        if (timePassed <= 0) {
            return Optional.of("0.00");
        }

        long corleonite = SkyblockCounterService.getLocalCorleoniteCount();
        double cph = ((double) corleonite / timePassed) * 3600.0;
        return Optional.of(String.format("%.2f", cph));
    }

    public static Optional<String> getCorleoniteStats() {
        if (!SkyblockCounterService.isLocalCounting()) {
            return Optional.empty();
        }

        int corleoniteCount = SkyblockCounterService.getLocalCorleoniteCount();
        int sessionKills = SkyblockCounterService.getSessionKills();
        if (sessionKills <= 0) {
            return Optional.of("Corleonite: " + corleoniteCount + " | No session kills yet");
        }

        double percentage = (corleoniteCount * 100.0) / sessionKills;
        return Optional.of(String.format("Corleonite: %d | Drop: %.2f%%", corleoniteCount, percentage));
    }

    // ---- Crystal Hollows geometry (fixed per instance) ----

    /** Full Crystal Hollows bounds, confirmed by the wiki. */
    private static final int HOLLOWS_MIN_X = 202;
    private static final int HOLLOWS_MAX_X = 823;
    private static final int HOLLOWS_MIN_Z = 202;
    private static final int HOLLOWS_MAX_Z = 823;

    /**
     * Mithril Deposits: the north-east quadrant of the upper layer.
     *
     * <p>The community guide and wiki both give "X512 Z0 to X1024 Z512"; world bounds clamp
     * that to the fixed Hollows extent. Boss Corleone only spawns in this quadrant, so there is
     * no reason to scan the other three.
     */
    private static final int DEPOSITS_MIN_X = 512;
    private static final int DEPOSITS_MAX_X = HOLLOWS_MAX_X;
    private static final int DEPOSITS_MIN_Z = HOLLOWS_MIN_Z;
    private static final int DEPOSITS_MAX_Z = 512;

    /** Upper layer; the Magma Fields sit below Y=64 and never contain the boss. */
    private static final int DEPOSITS_MIN_Y = 65;
    private static final int DEPOSITS_MAX_Y = 188;

    /** Chunk radius around the player, in chunks (16 = 256 blocks). */
    private static final int SCAN_RADIUS_CHUNKS = 16;

    public static CorleoneFinderResult findCorleoneStructures(ClientLevel world, BlockPos playerPos) {
        System.out.println("Finding Corleone structures");
        if (world == null || playerPos == null) {
            return CorleoneFinderResult.notInWorld();
        }

        // The whole scan is clamped to the Mithril Deposits rectangle, so players standing in
        // another quadrant still search the correct area instead of their own surroundings.
        int scanMinX = Math.max(DEPOSITS_MIN_X, playerPos.getX() - SCAN_RADIUS_CHUNKS * 16);
        int scanMaxX = Math.min(DEPOSITS_MAX_X, playerPos.getX() + SCAN_RADIUS_CHUNKS * 16);
        int scanMinZ = Math.max(DEPOSITS_MIN_Z, playerPos.getZ() - SCAN_RADIUS_CHUNKS * 16);
        int scanMaxZ = Math.min(DEPOSITS_MAX_Z, playerPos.getZ() + SCAN_RADIUS_CHUNKS * 16);

        if (scanMinX > scanMaxX || scanMinZ > scanMaxZ) {
            return new CorleoneFinderResult(false,
                    List.of("Not in the Mithril Deposits (expected X " + DEPOSITS_MIN_X + ".." + DEPOSITS_MAX_X
                            + ", Z " + DEPOSITS_MIN_Z + ".." + DEPOSITS_MAX_Z + ")"));
        }

        int minChunkX = scanMinX >> 4;
        int maxChunkX = scanMaxX >> 4;
        int minChunkZ = scanMinZ >> 4;
        int maxChunkZ = scanMaxZ >> 4;

        List<BlockPos> andesitePositions = new ArrayList<>();
        List<BlockPos> granitePositions = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (int x = 0; x < 16; x++) {
                    int worldX = (chunkX << 4) + x;
                    if (worldX < scanMinX || worldX > scanMaxX) {
                        continue;
                    }
                    for (int z = 0; z < 16; z++) {
                        int worldZ = (chunkZ << 4) + z;
                        if (worldZ < scanMinZ || worldZ > scanMaxZ) {
                            continue;
                        }
                        // Only the upper layer: Magma Fields (Y<64) never hold the boss.
                        for (int y = DEPOSITS_MIN_Y; y <= DEPOSITS_MAX_Y; y++) {
                            BlockPos basePos = new BlockPos(worldX, y, worldZ);

                            if (chunk.getBlockState(basePos).is(Blocks.POLISHED_ANDESITE)
                                    && chunk.getBlockState(basePos.above()).is(Blocks.STONE_BRICK_STAIRS)
                                    && chunk.getBlockState(basePos.above(2)).is(Blocks.SMOOTH_STONE_SLAB)) {
                                andesitePositions.add(basePos);
                            }

                            if (chunk.getBlockState(basePos).is(Blocks.POLISHED_GRANITE)
                                    && chunk.getBlockState(basePos.below(1)).is(Blocks.POLISHED_GRANITE)
                                    && chunk.getBlockState(basePos.below(2)).is(Blocks.POLISHED_GRANITE)
                                    && chunk.getBlockState(basePos.below(3)).is(Blocks.POLISHED_GRANITE)) {
                                BlockPos[] step1Dirs = {basePos.north(), basePos.south(), basePos.east(), basePos.west()};
                                BlockPos[] step2Dirs = {basePos.north(2), basePos.south(2), basePos.east(2), basePos.west(2)};

                                for (int i = 0; i < 4; i++) {
                                    if (world.getBlockState(step1Dirs[i].above()).is(Blocks.DYED_TERRACOTTA.lightGray())
                                            && world.getBlockState(step2Dirs[i].above(2)).is(Blocks.STONE_BRICKS)) {
                                        granitePositions.add(basePos);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<String> messages = new ArrayList<>();
        List<WaypointManager.Waypoint> foundWaypoints = new ArrayList<>();

        if (andesitePositions.size() > 1) {
            BlockPos centerPos = averagePosition(andesitePositions);
            foundWaypoints.add(new WaypointManager.Waypoint(centerPos, "Corleone Structure 1"));
            messages.add("Corleone structure 1 found at: " + centerPos.toShortString());
        }

        if (!granitePositions.isEmpty()) {
            BlockPos centerPos = averagePosition(granitePositions);
            // 513, 85, 541 is a false positive, the crystal nucleus is not a corleone structure
            if (!(centerPos.getX() == 513 && centerPos.getY() == 85 && centerPos.getZ() == 541)) {
                foundWaypoints.add(new WaypointManager.Waypoint(centerPos, "Corleone Structure 2"));
                messages.add("Corleone structure 2 found at: " + centerPos.toShortString());
            }
        }

        if (messages.isEmpty()) {
            return CorleoneFinderResult.nothingFound();
        }

        WaypointManager.setWaypoints(foundWaypoints);
        // Build one respawn timer per discovered structure.
        de.maexle.skyblockcounter.RespawnTimer.syncFromWaypoints();

        return new CorleoneFinderResult(true, messages);
    }

    private static BlockPos averagePosition(List<BlockPos> positions) {
        int sumX = 0;
        int sumY = 0;
        int sumZ = 0;
        for (BlockPos pos : positions) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }
        int size = positions.size();
        return new BlockPos(sumX / size, sumY / size, sumZ / size);
    }

    public static String addWaypoint(String name, int x, int y, int z, int color) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.addWaypoint(name, x, y, z, color);
        config.save();
        return "Added waypoint: " + name + " (" + x + ", " + y + ", " + z + ")";
    }

    public static String removeWaypoint(String name) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.removeWaypoint(name);
        config.save();
        return "Removed waypoint: " + name;
    }

}
