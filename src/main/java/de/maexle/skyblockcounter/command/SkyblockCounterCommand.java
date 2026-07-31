package de.maexle.skyblockcounter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.maexle.skyblockcounter.gui.SkyblockCounterActions;
import de.maexle.skyblockcounter.SkyblockCounterConfig;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.gui.ConfigPanelHelper;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import de.maexle.skyblockcounter.gui.SkyblockCounterConfigScreen;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SkyblockCounterCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("skycounter")
                .executes(SkyblockCounterCommand::showSkyCounterConfig)
                .then(literal("corleone_finder")
                        .executes(SkyblockCounterCommand::corleoneFinder))
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
                                .then(argument("number", IntegerArgumentType.integer())
                                        .executes(SkyblockCounterCommand::sessionAdd)))
                        .then(literal("remove")
                                .then(argument("number", IntegerArgumentType.integer())
                                        .executes(SkyblockCounterCommand::sessionRemove)))
                        .then(literal("reset")
                                .executes(SkyblockCounterCommand::sessionReset))
                        .then(literal("corleonite")
                                .executes(SkyblockCounterCommand::showCorleoniteStats))
                        .then(literal("corleonite_per_hour")
                                .executes(SkyblockCounterCommand::corleonitePerHour))
                        .then(literal("cph")
                                .executes(SkyblockCounterCommand::corleonitePerHour))
                        .then(literal("time_farmed")
                                .executes(SkyblockCounterCommand::showTimeFarmed))
                        .then(literal("time")
                                .executes(SkyblockCounterCommand::showTimeFarmed))
                        .then(literal("tf")
                                .executes(SkyblockCounterCommand::showTimeFarmed))
                )
        );
    }

    private static int showSkyCounterConfig(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        client.execute(() -> {
            client.setScreen(new SkyblockCounterConfigScreen(null));
        });
        return 1;
    }

    private static int showTimeFarmed(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterActions.formatTimeFarmed()
                .ifPresentOrElse(
                        time -> sendPrefixMessage(context.getSource(), "Time farmed: §e" + time),
                        () -> sendPrefixMessage(context.getSource(), "Please activate session mode first")
                );
        return 1;
    }

    private static int corleonitePerHour(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterActions.formatCorleonitePerHour()
                .ifPresentOrElse(
                        cph -> sendPrefixMessage(context.getSource(), "Corleonite per hour: " + cph),
                        () -> sendPrefixMessage(context.getSource(), "Please activate session mode first")
                );
        return 1;
    }

    private static int corleoneFinder(CommandContext<FabricClientCommandSource> context) {
        var world = context.getSource().getWorld();
        var player = context.getSource().getPlayer();
        if (world == null || player == null) {
            return 0;
        }

        SkyblockCounterActions.CorleoneFinderResult result = SkyblockCounterActions.findCorleoneStructures(world, player.getBlockPos());
        for (String message : result.messages()) {
            sendPrefixMessage(context.getSource(), message);
        }
        return result.success() ? 1 : 0;
    }

    private static int setAPI(CommandContext<FabricClientCommandSource> context) {
        String apiKey = StringArgumentType.getString(context, "apikey");
        SkyblockCounterActions.setApiKey(apiKey);
        sendPrefixMessage(context.getSource(), "API key was set");
        return 1;
    }

    private static int setUndashedUuid(CommandContext<FabricClientCommandSource> context) {
        String uuid = StringArgumentType.getString(context, "uuid");
        SkyblockCounterActions.setUuid(uuid);
        sendPrefixMessage(context.getSource(), "UUID was set");
        return 1;
    }

    private static int switchToTreasureHoarder(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterActions.switchToTreasureHoarder();
        sendPrefixMessage(context.getSource(), "Switched to Treasure Hoarder");
        return 1;
    }

    private static int switchToCorleoniteBoss(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterActions.switchToCorleone();
        sendPrefixMessage(context.getSource(), "Switched to Corleone");
        return 1;
    }

    private static int sessionAdd(CommandContext<FabricClientCommandSource> context) {
        int number = IntegerArgumentType.getInteger(context, "number");
        SkyblockCounterActions.addSessionKills(number);
        return 1;
    }

    private static int sessionRemove(CommandContext<FabricClientCommandSource> context) {
        int number = IntegerArgumentType.getInteger(context, "number");
        SkyblockCounterActions.removeSessionKills(number);
        return 1;
    }

    private static int sessionReset(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterActions.resetSession();
        return 1;
    }

    private static int switchToZealot(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterActions.switchToZealot();
        sendPrefixMessage(context.getSource(), "Switched to Zealot Lv.55");
        return 1;
    }

    private static int addMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        SkyblockCounterActions.MobAddResult result = SkyblockCounterActions.addMob(id, name, null);
        sendPrefixMessage(context.getSource(), result.message());
        return 1;
    }

    private static int addMobWithTexture(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        String texture = StringArgumentType.getString(context, "texture");
        SkyblockCounterActions.MobAddResult result = SkyblockCounterActions.addMob(id, name, texture);
        sendPrefixMessage(context.getSource(), result.message());
        return 1;
    }

    private static int removeMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        sendPrefixMessage(context.getSource(), SkyblockCounterActions.removeMob(id));
        return 1;
    }

    public static int listMobs(CommandContext<FabricClientCommandSource> context) {
        List<SkyblockCounterConfig.MobEntry> entries = SkyblockCounterActions.listMobs();
        sendPrefixMessage(context.getSource(), "Available mobs:");
        for (SkyblockCounterConfig.MobEntry entry : entries) {
            String textureInfo = entry.texture != null ? " [Texture: " + entry.texture + "]" : "";
            sendPrefixMessage(context.getSource(), "- " + entry.name + " (" + entry.id + ")" + textureInfo);
        }
        return 1;
    }

    private static int switchToMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        SkyblockCounterActions.MobSwitchResult result = SkyblockCounterActions.switchToMob(id);
        sendPrefixMessage(context.getSource(), result.message());
        return result.success() ? 1 : 0;
    }

    private static int setHudPosition(CommandContext<FabricClientCommandSource> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        SkyblockCounterActions.setHudPosition(x, y);
        sendPrefixMessage(context.getSource(), "HUD position set to X: " + x + ", Y: " + y);
        return 1;
    }

    private static int toggleSessionMode(CommandContext<FabricClientCommandSource> context) {
        boolean enabled = SkyblockCounterActions.toggleSession();
        sendPrefixMessage(context.getSource(), "Session mode: " + (enabled ? "ON" : "OFF"));
        return 1;
    }

    private static int showCorleoniteStats(CommandContext<FabricClientCommandSource> context) {
        /*
        if (SkyblockCounterService.isShowSessionKills()) {
            SkyblockCounterActions.getCorleoniteStats()
                    .ifPresent(stats -> sendPrefixMessage(context.getSource(), stats));
            return 1;
        }
        */
        sendPrefixMessage(context.getSource(), "Fetching Corleonite stats...");
        SkyblockCounterService.getCorleoniteDropPercentageAPI()
                .thenAccept(result -> MinecraftClient.getInstance().execute(() ->
                        sendPrefixMessage(context.getSource(), result)))
                .exceptionally(ex -> {
                    MinecraftClient.getInstance().execute(() ->
                            sendPrefixMessage(context.getSource(), "Error: " + ex.getMessage()));
                    return null;
                });
        return 1;
    }

    public static void sendPrefixMessage(FabricClientCommandSource source, String message) {
        source.sendFeedback(formatPrefixMessage(message));
    }

    public static void sendPlayerMessage(String message) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(formatPrefixMessage(message), false);
        }
    }

    private static Text formatPrefixMessage(String message) {
        return Text.literal("[").formatted(Formatting.GRAY)
                .append(Text.literal("Sky").styled(style -> style.withColor(ConfigPanelHelper.TEXT_PRIMARY)))
                .append(Text.literal("Counter").styled(style -> style.withColor(ConfigPanelHelper.ACCENT)))
                .append(Text.literal("] ").formatted(Formatting.GRAY))
                .append(Text.literal(message).formatted(Formatting.WHITE));
    }
}
