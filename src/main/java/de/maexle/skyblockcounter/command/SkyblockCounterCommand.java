package de.maexle.skyblockcounter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.SkyblockCounterConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SkyblockCounterCommand {

    private static SkyblockCounterConfig config;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("skycounter")
                .then(literal("treasure_hoarder")
                        .executes(SkyblockCounterCommand::switchToTreasureHoarder))
                .then(literal("corleone")
                        .executes(SkyblockCounterCommand::switchToCorleoniteBoss))
                .then(literal("zealot")
                        .executes(SkyblockCounterCommand::switchToZealot))
                .then(literal("list")
                        .executes(SkyblockCounterCommand::listMobs))
                .then(literal("add")
                        .then(argument("id", StringArgumentType.string())
                                .then(argument("name", StringArgumentType.string())
                                        .executes(SkyblockCounterCommand::addMob)
                                        .then(argument("texture", StringArgumentType.string())
                                                .executes(SkyblockCounterCommand::addMobWithTexture)))))
                .then(literal("remove")
                        .then(argument("id", StringArgumentType.string())
                                .executes(SkyblockCounterCommand::removeMob)))
                .then(literal("switch")
                        .then(argument("id", StringArgumentType.string())
                                .executes(SkyblockCounterCommand::switchToMob)))
                .then(literal("position")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .executes(SkyblockCounterCommand::setHudPosition))))
                .then(literal("set_api")
                        .then(argument("apikey", StringArgumentType.string())
                            .executes(SkyblockCounterCommand::setAPI)))
                .then(literal("api")
                        .then(argument("apikey", StringArgumentType.string())
                                .executes(SkyblockCounterCommand::setAPI)))
                .then(literal("set_uuid")
                        .then(argument("uuid", StringArgumentType.string())
                            .executes(SkyblockCounterCommand::setUndashedUuid)))
                .then(literal("uuid")
                        .then(argument("uuid", StringArgumentType.string())
                                .executes(SkyblockCounterCommand::setUndashedUuid)))
                .then(literal("session")
                        .then(literal("toggle")
                                .executes(SkyblockCounterCommand::toggleSessionMode))
                        .then(literal("add")
                                .executes(SkyblockCounterCommand::sessionAdd1))
                        .then(literal("reset")
                                .executes(SkyblockCounterCommand::sessionReset))
                        .then(literal("corleonite")
                                .executes(SkyblockCounterCommand::showCorleoniteStats)))
        );
    }

    private static int setAPI(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        String API_KEY = StringArgumentType.getString(context, "apikey");
        config.setAPI_KEY(API_KEY);
        SkyblockCounterService.setApiKey(API_KEY);
        sendPrefixMessage(context.getSource(), "API key was set");
        config.save();
        return 1;

    }

    private static int setUndashedUuid(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        String uuid = StringArgumentType.getString(context, "uuid");
        String undashedUuid = uuid.replace("-", "");
        config.setundashedUuid(undashedUuid);
        SkyblockCounterService.setUndashedUuid(undashedUuid);
        sendPrefixMessage(context.getSource(), "Uuid key was set");
        config.save();
        return 1;

    }

    private static int switchToTreasureHoarder(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.switchMob("treasure_hoarder_70", "Treasure Hoarder");
        sendPrefixMessage(context.getSource(), "Switched to Treasure Hoarder");
        return 1;
    }

    private static int switchToCorleoniteBoss(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.switchMob("team_treasurite_corleone_200", "Corleone");
        sendPrefixMessage(context.getSource(), "Switched to Corleone");
        return 1;
    }

    private static int sessionAdd1(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.addStartSessionKills(1);
        return 1;
    }

    private static int sessionReset(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.setStartSessionKills(0);
        return 1;
    }

    private static int switchToZealot(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.switchMob("zealot_enderman_55", "Zealot");
        sendPrefixMessage(context.getSource(), "Switched to Zealot Lv.55");
        return 1;
    }

    private static int addMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        return addMobEntry(context, id, name, null);
    }

    private static int addMobWithTexture(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        String texture = StringArgumentType.getString(context, "texture");
        return addMobEntry(context, id, name, texture);
    }

    private static int addMobEntry(CommandContext<FabricClientCommandSource> context, String id, String name, String texture) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.addMobEntry(id, name, texture);
        config.save();
        SkyblockCounterService.reloadMobTextures();
        String textureInfo = texture != null ? " (Texture: " + texture + ")" : "";
        sendPrefixMessage(context.getSource(), "Mob hinzugefügt: " + name + " (" + id + ")" + textureInfo);
        return 1;
    }

    private static int removeMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.removeMobEntry(id);
        config.save();
        SkyblockCounterService.reloadMobTextures();
        sendPrefixMessage(context.getSource(), "Mob entfernt: " + id);
        return 1;
    }

    public static int listMobs(CommandContext<FabricClientCommandSource> context) {
        List<SkyblockCounterConfig.MobEntry> entries = SkyblockCounterService.getConfig().getMobEntries();
        if (entries.isEmpty()) {
            SkyblockCounterConfig config = SkyblockCounterService.getConfig();
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleone", "textures/gui/sprites/boss_corleone_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            config.save();
            SkyblockCounterService.reloadMobTextures();
            return 1;
        }
        sendPrefixMessage(context.getSource(), "Verfügbare Mobs: ");
        for (SkyblockCounterConfig.MobEntry entry : entries) {
            String textureInfo = entry.texture != null ? " [Texture: " + entry.texture + "]" : "";
            sendPrefixMessage(context.getSource(), "- " + entry.name + " (" + entry.id + ")" + textureInfo);
        }
        return 1;
    }

    private static int switchToMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        SkyblockCounterConfig.MobEntry entry = config.getMobEntries().stream()
                .filter(e -> e.id.equals(id))
                .findFirst()
                .orElse(null);
        
        if (entry == null) {
            sendPrefixMessage(context.getSource(), "Mob nicht gefunden: " + id);
            return 0;
        }
        
        SkyblockCounterService.switchMob(entry.id, entry.name);
        sendPrefixMessage(context.getSource(), "Switched to " + entry.name);
        return 1;
    }

    private static int showCurrentMob(CommandContext<FabricClientCommandSource> context) {
        String currentMob = SkyblockCounterService.getCurrentMobName();
        int currentKills = SkyblockCounterService.getCurrentKills();
        sendPrefixMessage(context.getSource(), "Current mob: " + currentMob + " (Kills: " + currentKills + ")");
        return 1;
    }

    private static int setHudPosition(CommandContext<FabricClientCommandSource> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        SkyblockCounterService.setHudPosition(x, y);
        sendPrefixMessage(context.getSource(), "HUD position set to X: " + x + ", Y: " + y);
        return 1;
    }

    private static int toggleSessionMode(CommandContext<FabricClientCommandSource> context) {
        boolean currentMode = SkyblockCounterService.isShowSessionKills();
        SkyblockCounterService.setShowSessionKills(!currentMode);
        String status = !currentMode ? "AN" : "AUS";
        context.getSource().sendFeedback(Text.literal("Session-Kills Modus: " + status));
        sendPrefixMessage(context.getSource(), "Session-Kills Modus: " + status);
        return 1;
    }

    private static int showCorleoniteStats(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("Rufe Corleonite-Statistiken ab..."));
        if(SkyblockCounterService.isShowSessionKills()){
            sendPrefixMessage(context.getSource(), SkyblockCounterService.getCorleoniteDropPercentage());
            return 1;
        } else {
        SkyblockCounterService.getCorleoniteDropPercentageAPI()
            .thenAccept(result -> {
                MinecraftClient.getInstance().execute(() -> {
                    sendPrefixMessage(context.getSource(), result);
                });
            })
            .exceptionally(ex -> {
                MinecraftClient.getInstance().execute(() -> {
                    sendPrefixMessage(context.getSource(), "Fehler: " + ex.getMessage());
                });
                return null;
            });
        }
        return 1;
    }

    public static void sendPrefixMessage(FabricClientCommandSource source, String message) {
        source.sendFeedback(
                Text.literal("[").formatted(Formatting.GRAY)
                        .append(Text.literal("SkyCounter").formatted(Formatting.AQUA))
                        .append(Text.literal("] ").formatted(Formatting.GRAY))
                        .append(Text.literal(message).formatted(Formatting.WHITE))
        );
    }

}
