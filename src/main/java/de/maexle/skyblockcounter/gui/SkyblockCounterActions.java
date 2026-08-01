package de.maexle.skyblockcounter.gui;

import de.maexle.skyblockcounter.SkyblockCounterConfig;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.waypoint.WaypointManager;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

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

    public static boolean toggleSession() {
        boolean currentMode = SkyblockCounterService.isShowSessionKills();
        SkyblockCounterService.setShowSessionKills(!currentMode);
        return !currentMode;
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
        if (!SkyblockCounterService.isShowSessionKills()) {
            return Optional.empty();
        }

        long totalSeconds = (System.currentTimeMillis() - SkyblockCounterService.sessionStartTime) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return Optional.of(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    public static Optional<String> formatCorleonitePerHour() {
        if (!SkyblockCounterService.isShowSessionKills()) {
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
        if (!SkyblockCounterService.isShowSessionKills()) {
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

    public static CorleoneFinderResult findCorleoneStructures(ClientWorld world, BlockPos playerPos) {
        if (world == null || playerPos == null) {
            return CorleoneFinderResult.notInWorld();
        }

        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;
        int radius = 16;

        List<BlockPos> andesitePositions = new ArrayList<>();
        List<BlockPos> granitePositions = new ArrayList<>();

        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                int chunkX = centerChunkX + xOffset;
                int chunkZ = centerChunkZ + zOffset;

                WorldChunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                int startX = chunk.getPos().getStartX();
                int startZ = chunk.getPos().getStartZ();
                int bottomY = world.getBottomY();
                int topY = world.getTopYInclusive();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = bottomY + 3; y < topY - 2; y++) {
                            BlockPos basePos = new BlockPos(startX + x, y, startZ + z);

                            if (chunk.getBlockState(basePos).isOf(Blocks.POLISHED_ANDESITE)
                                    && chunk.getBlockState(basePos.up()).isOf(Blocks.STONE_BRICK_STAIRS)
                                    && chunk.getBlockState(basePos.up(2)).isOf(Blocks.SMOOTH_STONE_SLAB)) {
                                andesitePositions.add(basePos);
                            }

                            if (chunk.getBlockState(basePos).isOf(Blocks.POLISHED_GRANITE)
                                    && world.getBlockState(basePos.down(1)).isOf(Blocks.POLISHED_GRANITE)
                                    && world.getBlockState(basePos.down(2)).isOf(Blocks.POLISHED_GRANITE)
                                    && world.getBlockState(basePos.down(3)).isOf(Blocks.POLISHED_GRANITE)) {
                                BlockPos[] step1Dirs = {basePos.north(), basePos.south(), basePos.east(), basePos.west()};
                                BlockPos[] step2Dirs = {basePos.north(2), basePos.south(2), basePos.east(2), basePos.west(2)};

                                for (int i = 0; i < 4; i++) {
                                    if (world.getBlockState(step1Dirs[i].up()).isOf(Blocks.LIGHT_GRAY_TERRACOTTA)
                                            && world.getBlockState(step2Dirs[i].up(2)).isOf(Blocks.STONE_BRICKS)) {
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
            if(!(centerPos.getX() == 513 && centerPos.getY() == 85 && centerPos.getZ() == 541)) {
                foundWaypoints.add(new WaypointManager.Waypoint(centerPos, "Corleone Structure 2"));
                messages.add("Corleone structure 2 found at: " + centerPos.toShortString());
            }
        }

        if (messages.isEmpty()) {
            return CorleoneFinderResult.nothingFound();
        }

        WaypointManager.setWaypoints(foundWaypoints);

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
