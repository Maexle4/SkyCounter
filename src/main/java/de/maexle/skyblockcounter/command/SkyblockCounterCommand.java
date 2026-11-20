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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import de.maexle.skyblockcounter.gui.SkyblockCounterConfigScreen;
import net.minecraft.network.chat.*;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

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
                .then(literal("hud")
                        .executes(SkyblockCounterCommand::openHudEditor))
                .then(literal("debug")
                        .executes(SkyblockCounterCommand::toggleDebugMode)
                        .then(literal("on")
                                .executes(SkyblockCounterCommand::enableDebugMode))
                        .then(literal("off")
                                .executes(SkyblockCounterCommand::disableDebugMode)))
                .then(literal("timer")
                        .executes(SkyblockCounterCommand::toggleTimerHud)
                        .then(literal("on")
                                .executes(SkyblockCounterCommand::enableTimerHud))
                        .then(literal("off")
                                .executes(SkyblockCounterCommand::disableTimerHud))
                        .then(literal("start")
                                .executes(SkyblockCounterCommand::startRespawnTimer))
                        .then(literal("stop")
                                .executes(SkyblockCounterCommand::stopRespawnTimer))
                        .then(literal("status")
                                .executes(SkyblockCounterCommand::timerStatus)))
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
        Minecraft client = context.getSource().getClient();
        client.execute(() -> {
            client.gui.setScreen(new SkyblockCounterConfigScreen(null));
        });
        return 1;
    }

    private static int openHudEditor(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = context.getSource().getClient();
        client.execute(() -> {
            if (client.gui.screen() == null) {
                client.gui.setScreen(new de.maexle.skyblockcounter.gui.HudEditorScreen(null));
            }
        });
        return 1;
    }

    private static int toggleDebugMode(CommandContext<FabricClientCommandSource> context) {
        boolean enabled = SkyblockCounterService.toggleDebugMode();
        sendPrefixMessage(context.getSource(), "Debug mode " + (enabled ? "enabled - HUD now shows in any world" : "disabled"));
        return 1;
    }

    private static int enableDebugMode(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.setDebugMode(true);
        sendPrefixMessage(context.getSource(), "Debug mode enabled - HUD now shows in any world");
        return 1;
    }

    private static int disableDebugMode(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.setDebugMode(false);
        sendPrefixMessage(context.getSource(), "Debug mode disabled");
        return 1;
    }

    private static int toggleTimerHud(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.setShowRespawnTimer(!SkyblockCounterService.isShowRespawnTimer());
        sendPrefixMessage(context.getSource(), "Respawn timer HUD "
                + (SkyblockCounterService.isShowRespawnTimer() ? "enabled" : "disabled"));
        return 1;
    }

    private static int enableTimerHud(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.setShowRespawnTimer(true);
        sendPrefixMessage(context.getSource(), "Respawn timer HUD enabled");
        return 1;
    }

    private static int disableTimerHud(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.setShowRespawnTimer(false);
        sendPrefixMessage(context.getSource(), "Respawn timer HUD disabled");
        return 1;
    }

    private static int startRespawnTimer(CommandContext<FabricClientCommandSource> context) {
        de.maexle.skyblockcounter.RespawnTimer.syncFromWaypoints();
        de.maexle.skyblockcounter.RespawnTimer.startFallback();
        int count = de.maexle.skyblockcounter.RespawnTimer.getTimers().size();
        sendPrefixMessage(context.getSource(), "Respawn timers started for " + count + " structure(s)");
        return 1;
    }

    private static int stopRespawnTimer(CommandContext<FabricClientCommandSource> context) {
        de.maexle.skyblockcounter.RespawnTimer.stopAll();
        sendPrefixMessage(context.getSource(), "Respawn timers stopped");
        return 1;
    }

    private static int timerStatus(CommandContext<FabricClientCommandSource> context) {
        var timers = de.maexle.skyblockcounter.RespawnTimer.getTimers();
        if (timers.isEmpty()) {
            sendPrefixMessage(context.getSource(), "Respawn timer: no structure scanned yet");
            return 1;
        }
        for (var timer : timers) {
            String line = timer.started()
                    ? timer.label() + ": 60s="
                        + (timer.slotElapsed(0) ? "READY" : de.maexle.skyblockcounter.RespawnTimer.format(timer.remainingForSlot(0)))
                        + " 120s="
                        + (timer.slotElapsed(1) ? "READY" : de.maexle.skyblockcounter.RespawnTimer.format(timer.remainingForSlot(1)))
                    : timer.label() + ": idle";
            sendPrefixMessage(context.getSource(), line);
        }
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
        var world = context.getSource().getLevel();
        var player = context.getSource().getPlayer();
        if (world == null || player == null) {
            return 0;
        }

        SkyblockCounterActions.CorleoneFinderResult result = SkyblockCounterActions.findCorleoneStructures(world, player.getOnPos());
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
        de.maexle.skyblockcounter.CountingMode mode = SkyblockCounterActions.cycleCountingMode();
        sendPrefixMessage(context.getSource(), "Counting mode: " + mode.displayName());
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
                .thenAccept(result -> Minecraft.getInstance().execute(() ->
                        sendPrefixMessage(context.getSource(), result)))
                .exceptionally(ex -> {
                    Minecraft.getInstance().execute(() ->
                            sendPrefixMessage(context.getSource(), "Error: " + ex.getMessage()));
                    return null;
                });
        return 1;
    }

    public static void sendPrefixMessage(FabricClientCommandSource source, String message) {
        source.sendFeedback(formatPrefixMessage(message));
    }

    public static void sendPlayerMessage(String message) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(formatPrefixMessage(message));
        }
    }

    private static Component formatPrefixMessage(String message) {
        return Component.literal("[").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Sky").withStyle(style -> style.withColor(TextColor.fromRgb(ConfigPanelHelper.TEXT_PRIMARY & 0xFFFFFF))))
                .append(Component.literal("Counter").withStyle(style -> style.withColor(TextColor.fromRgb(ConfigPanelHelper.ACCENT & 0xFFFFFF))))
                .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
    }
}
