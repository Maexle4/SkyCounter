package de.maexle.skyblockcounter;

import net.minecraft.client.renderer.RenderPipelines;
import org.json.JSONArray;
import org.json.JSONObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.render.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import de.maexle.skyblockcounter.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class SkyblockCounterService {

    private static final Logger LOGGER = LoggerFactory.getLogger("skyblockcounter");

    private static volatile int currentKills = -1;
    private static volatile boolean guiVisible = false;
    private static volatile String currentMobId = "treasure_hoarder_70";
    private static volatile String currentMobName = "Treasure Hoarder";
    private static volatile String API_KEY = "";
    private static volatile String undashedUuid = "";
    private static volatile int hudX;
    private static volatile int hudY;
    private static volatile int startSessionKills = 0;
    private static volatile CountingMode countingMode = CountingMode.API;
    private static volatile boolean showCorleonitePercentage = false;
    private static volatile boolean showRespawnTimerHud = true;
    private static volatile boolean debugMode = false;

    /**
     * True while the in-game HUD editor is open. The editor draws the elements itself at the
     * real cursor-driven positions, so the normal HUD renderer must stay silent to avoid
     * drawing everything twice.
     */
    private static volatile boolean hudEditorActive = false;

    public static void setHudEditorActive(boolean active) {
        hudEditorActive = active;
    }

    public static boolean isHudEditorActive() {
        return hudEditorActive;
    }
    private static volatile int timerX;
    private static volatile int timerY;
    private static volatile int cachedCorleoniteCount = 0;

    /**
     * Corleonite collected as reported by Hypixel's [Sacks] messages. Items that go straight
     * into a sack never appear in the inventory, so this is the only reliable source.
     */
    private static volatile int sackCorleoniteTotal = 0;

    private static volatile int localSessionKills = 0;
    private static volatile int localSessionStartCorleonite = 0;


    private static final Identifier TREASURE_HOARDER_HEAD = Identifier.fromNamespaceAndPath("skyblockcounter", "treasure_hoarder_head");
    private static final Identifier CORLEONE_HEAD = Identifier.fromNamespaceAndPath("skyblockcounter", "boss_corleone_head");
    private static final Identifier ZEALOT_HEAD = Identifier.fromNamespaceAndPath("skyblockcounter", "zealot_enderman_head");

    private static final int HEAD_SIZE = 16;

    /** Size of one respawn timer row, one row per Corleone structure. */
    public static final int TIMER_PANEL_WIDTH = 168;
    public static final int TIMER_PANEL_HEIGHT = 34;
    private static final int TIMER_ROW_GAP = 4;

    List<SkyblockCounterConfig.MobEntry> entries = SkyblockCounterService.getConfig().getMobEntries();

    private static final Set<Integer> knownCorleones = new HashSet<>();

    public static long sessionStartTime = 0;

    private long lastUnloadTrigger = 0;

    private static final Map<String, Identifier> MOB_TEXTURES = new HashMap<>();
    private static SkyblockCounterConfig config;

    static {
        config = SkyblockCounterConfig.load();
        de.maexle.skyblockcounter.i18n.ModLang.setCurrent(
                "zh_cn".equals(config.getLanguage())
                        ? de.maexle.skyblockcounter.i18n.ModLang.Language.CHINESE
                        : de.maexle.skyblockcounter.i18n.ModLang.Language.ENGLISH);
        hudX = config.getHudX();
        guiVisible = config.isHudVisible();
        showCorleonitePercentage = config.isShowCorleonitePercentage();
        showRespawnTimerHud = config.isShowRespawnTimer();
        debugMode = config.isDebugMode();
        countingMode = CountingMode.fromId(config.getCountingMode());
        if (countingMode.isPersistent()) {
            localSessionKills = config.getStoredKills();
        }
        timerX = config.getTimerX();
        timerY = config.getTimerY();
        API_KEY = config.getAPI_KEY();
        undashedUuid = config.getundashedUuid();
        hudY = config.getHudY();
        currentMobId = config.getLastMobId();
        currentMobName = config.getLastMobName();

        for (SkyblockCounterConfig.MobEntry entry : config.getMobEntries()) {
            if (entry.texture != null) {
                MOB_TEXTURES.put(entry.id, Identifier.fromNamespaceAndPath("skyblockcounter", entry.texture));
            }
        }
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("SykblockCounter-Timer");
        return thread;
    });

    public void startEventTracking() {
        LOGGER.info("BestiaryService initialisiert (Rein Clientseitig)");
        LOGGER.info("Config geladen: HUD Position (" + hudX + ", " + hudY + "), Mob: " + currentMobName);

        registerHudRenderer();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                for (Entity entity : client.level.entitiesForRendering()) {
                    if (entity.getCustomName() != null) {
                        String name = entity.getCustomName().getString();

                        if (name.contains("Corleone") && !knownCorleones.contains(entity.getId())) {
                            knownCorleones.add(entity.getId());
                            LOGGER.info("[DEBUG] " + name + " hat jetzt einen Namen! Spiele Sound 3x ab...");

                            for (int i = 0; i < 3; i++) {
                                scheduler.schedule(() -> {
                                    client.execute(() -> {
                                        if (client.player != null) {
                                            client.player.playSound(SoundEvents.ANVIL_LAND, 20.0F, 1.0F);
                                        }
                                    });
                                }, (long) i * 300, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                }
            }
        });

        // Optionally drop saved waypoints when the player switches world/island, so stale
        // coordinates from the previous island do not stay on the screen.
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            onWorldChanged();

            if (config.isClearWaypointsOnWorldChange()) {
                WaypointManager.clearAll();
                RespawnTimer.stopAll();
                LOGGER.info("World changed - cleared saved waypoints and respawn timers");
            }
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            knownCorleones.remove(entity.getId());

            if (entity.getCustomName() != null) {
                String name = entity.getCustomName().getString();

                if (name.contains("Corleone")) {
                    long now = System.currentTimeMillis();
                    if (now - lastUnloadTrigger > 2000) {
                        lastUnloadTrigger = now;

                        // Attribute the kill to the closest scanned structure so only that
                        // structure's countdown restarts while the others keep running.
                        RespawnTimer.startNearest(entity.blockPosition());

                        if (countingMode.isLocal()) {
                            localSessionKills++;
                            if (countingMode.isPersistent()) {
                                config.setStoredKills(localSessionKills);
                                config.save();
                            }
                            LOGGER.info("[DEBUG] " + name + " getötet! Lokale Session-Kills: " + localSessionKills);
                            scheduler.schedule(this::pingForNextCorleone, 120, TimeUnit.SECONDS);
                        } else {
                            LOGGER.info("[DEBUG] " + name + " aus der Welt geladen! API-Update in 1 Sekunde...");
                            scheduler.schedule(this::fetchCurrentBestiaryKills, 1, TimeUnit.SECONDS);
                            scheduler.schedule(this::pingForNextCorleone, 120, TimeUnit.SECONDS);
                        }
                    }
                }
            }
        });

        // Hypixel reports everything that lands in a sack via [Sacks] messages whose real
        // contents are inside a hover tooltip. Read them from the component tree instead of
        // sniffing the inventory, which misses items that never reach it.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            int gained = SackMessageParser.parseCorleonite(message);
            if (gained > 0) {
                sackCorleoniteTotal += gained;
                LOGGER.info("[Sacks] Corleonite total now " + sackCorleoniteTotal);
            }
        });

        // 30s
        scheduler.scheduleAtFixedRate(this::fetchCurrentBestiaryKills, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Optional chime that plays when the respawn window is over.
     *
     * <p>Gated only by the explicit {@code timerSoundEnabled} switch. It previously checked the
     * live session-mode flag instead, so a timer scheduled while session mode was off would
     * still fire after the player enabled it, which looked like random noise.
     */
    private void pingForNextCorleone() {
        if (!config.isTimerSoundEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            scheduler.schedule(() -> client.execute(() -> {
                if (client.player != null) {
                    client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                }
            }), (long) i * 300, TimeUnit.MILLISECONDS);
        }
    }

    private void fetchCurrentBestiaryKills() {
        if (countingMode.isLocal()) return;

        retrieveBestiaryKillsAsync(undashedUuid, currentMobId)
                .thenAccept(kills -> {
                    if (kills >= 0) {
                        updateGameInterface(kills);
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Fehler bei automatischer Bestiary-Abfrage: " + ex.getMessage());
                    return null;
                });
    }

    private void registerHudRenderer() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("skyblockcounter", "hud_element"),
                (drawContext, tickCounter) -> {
                    if (!hudEditorActive && (guiVisible || debugMode)) {
                        renderHud(drawContext);
                    }
                }
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("skyblockcounter", "respawn_timer"),
                (drawContext, tickCounter) -> {
                    if (!showRespawnTimerHud || hudEditorActive) {
                        return;
                    }
                    RespawnTimer.pruneFinished();

                    if (RespawnTimer.anyVisible()) {
                        renderRespawnTimers(drawContext);
                    } else if (debugMode) {
                        // Debug mode: keep a preview panel on screen so the layout can be
                        // checked in a singleplayer world without waiting for a kill.
                        renderRespawnTimerPreview(drawContext);
                    }
                }
        );
    }

    /** Inactive-state preview of the timer panel, used by debug mode. */
    private void renderRespawnTimerPreview(GuiGraphicsExtractor drawContext) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) {
            return;
        }

        int panelWidth = TIMER_PANEL_WIDTH;
        int panelHeight = TIMER_PANEL_HEIGHT;

        drawRoundedRect(drawContext, timerX, timerY, panelWidth, panelHeight, 0xFF5A4A7A);
        drawRoundedRect(drawContext, timerX + 1, timerY + 1, panelWidth - 2, panelHeight - 2, 0xD01B1A23);
        drawRoundedRect(drawContext, timerX + 2, timerY + 2, 3, panelHeight - 4, 0xFFB16EFF);

        drawContext.text(mc.font, "Corleone",
                timerX + 11, timerY + 5, 0xFFA7A3B3, false);
        drawContext.text(mc.font, "DEBUG",
                timerX + panelWidth - 40, timerY + 5, 0xFFFFC24B, false);

        // Preview both candidate windows so the two-segment layout can be judged.
        drawTwoSegments(drawContext, mc, timerX + 11, timerY + 15, panelWidth - 20,
                RespawnTimer.SHORT_WINDOW_MS, RespawnTimer.LONG_WINDOW_MS, 0xFF8A8798, 0xFF8A8798, false, false);

        drawContext.fill(timerX + 11, timerY + panelHeight - 8,
                timerX + 11 + (panelWidth - 20), timerY + panelHeight - 5, 0xFF322F3E);
    }

    /**
     * Draws one row per Corleone structure. Each row shows the two candidate respawn windows
     * (60s and 120s) as two segments, with the segment for the current cycle highlighted.
     */
    private void renderRespawnTimers(GuiGraphicsExtractor drawContext) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) {
            return;
        }

        drawRespawnTimersAt(drawContext, timerX, timerY);
    }

    /** Draws the structure timer rows starting at the given screen position. */
    public static void drawRespawnTimersAt(GuiGraphicsExtractor drawContext, int startX, int startY) {
        Minecraft mc = Minecraft.getInstance();

        int y = startY;
        for (RespawnTimer.StructureTimer timer : RespawnTimer.getTimers()) {
            if (!timer.started() || timer.shouldHide()) {
                continue;
            }

            boolean allDone = timer.hasElapsed();
            int accentColor = timer.color();
            int borderColor = allDone ? 0xFF3E7C55 : 0xFF3D3A4C;

            drawRoundedRect(drawContext, startX, y, TIMER_PANEL_WIDTH, TIMER_PANEL_HEIGHT, borderColor);
            drawRoundedRect(drawContext, startX + 1, y + 1, TIMER_PANEL_WIDTH - 2, TIMER_PANEL_HEIGHT - 2, 0xF01B1A23);
            drawRoundedRect(drawContext, startX + 2, y + 2, 3, TIMER_PANEL_HEIGHT - 4, accentColor);

            drawContext.text(mc.font, timer.label(),
                    startX + 11, y + 5, 0xFFA7A3B3, false);

            // Both windows run in parallel from the kill; each one turns green when it passes.
            long shortRemaining = timer.remainingForSlot(0);
            long longRemaining = timer.remainingForSlot(1);
            boolean shortDone = timer.slotElapsed(0);
            boolean longDone = timer.slotElapsed(1);

            int countingColor = 0xFFFFFFFF;
            int doneColor = 0xFF4ADE80;

            drawTwoSegments(drawContext, mc, startX + 11, y + 15, TIMER_PANEL_WIDTH - 20,
                    shortRemaining, longRemaining,
                    shortDone ? doneColor : countingColor,
                    longDone ? doneColor : countingColor,
                    shortDone, longDone);

            int barX = startX + 11;
            int barY = y + TIMER_PANEL_HEIGHT - 8;
            int barWidth = TIMER_PANEL_WIDTH - 20;
            drawContext.fill(barX, barY, barX + barWidth, barY + 3, 0xFF322F3E);

            // Single bar tracking the 120s window, which is the one that matters once the
            // 60s window has already passed.
            int filled = Math.round(barWidth * (1.0F - timer.progressForSlot(1)));
            if (filled > 0) {
                drawContext.fill(barX, barY, barX + filled, barY + 3, longDone ? doneColor : accentColor);
            }

            y += TIMER_PANEL_HEIGHT + TIMER_ROW_GAP;
        }
    }

    /** Height the timer block occupies for the given number of visible rows. */
    public static int timerBlockHeight() {
        int rows = 0;
        for (RespawnTimer.StructureTimer timer : RespawnTimer.getTimers()) {
            if (timer.started() && !timer.shouldHide()) {
                rows++;
            }
        }
        return rows == 0 ? TIMER_PANEL_HEIGHT : rows * TIMER_PANEL_HEIGHT + (rows - 1) * TIMER_ROW_GAP;
    }

    /**
     * Renders two "60s / 120s" segments on a single line. The left segment is the short
     * window and the right segment is the long window.
     */
    private static void drawTwoSegments(GuiGraphicsExtractor context, Minecraft mc, int x, int y, int width,
                                        long shortRemaining, long longRemaining,
                                        int shortColor, int longColor,
                                        boolean shortDone, boolean longDone) {
        String shortText = shortDone ? "READY" : RespawnTimer.format(shortRemaining);
        String longText = longDone ? "READY" : RespawnTimer.format(longRemaining);

        String label60 = "60s ";
        String label120 = " 120s ";

        int x60 = x;
        context.text(mc.font, label60, x60, y, 0xFF6E6A7C, false);
        x60 += mc.font.width(label60);
        context.text(mc.font, shortText, x60, y, shortColor, false);

        int sepX = x + (width / 2) - 2;
        context.text(mc.font, "|", sepX, y, 0xFF3D3A4C, false);

        int x120 = sepX + mc.font.width("|") + 4;
        context.text(mc.font, label120, x120, y, 0xFF6E6A7C, false);
        x120 += mc.font.width(label120);
        context.text(mc.font, longText, x120, y, longColor, false);
    }

    private static void drawRoundedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + 1, color);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
        context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, color);
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, color);
    }

    private void renderHud(GuiGraphicsExtractor drawContext) {
        if (entries.isEmpty()) {
            SkyblockCounterConfig config = SkyblockCounterService.getConfig();

            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "treasure_hoarder_head"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleonite Boss", "boss_corleone_head"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "zealot_enderman_head"));

            config.save();

            SkyblockCounterService.reloadMobTextures();
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) {
            return;
        }

        int xPos = hudX;
        int yPos = hudY;

        drawKillsAt(drawContext, xPos, yPos);
    }

    /** Draws the kill counter (head icon + count) at the given screen position. */
    public static void drawKillsAt(GuiGraphicsExtractor drawContext, int xPos, int yPos) {
        Minecraft mc = Minecraft.getInstance();

        int displayKills = countingMode.isLocal() ? localSessionKills : currentKills;

        if (debugMode && displayKills < 0) {
            // Debug mode: no API data available in a singleplayer test world,
            // so show a plausible placeholder instead of "-1".
            displayKills = 0;
        }

        String text = "" + displayKills;

        if (countingMode.isLocal() && showCorleonitePercentage && displayKills > 0) {
            int gainedCorleonite = getLocalCorleoniteCount() - localSessionStartCorleonite;
            gainedCorleonite = Math.max(0, gainedCorleonite);
            double percentage = (gainedCorleonite * 100.0) / displayKills;
            text = displayKills + " | " + String.format("%.2f%%", percentage);
        } else if (!countingMode.isLocal() && showCorleonitePercentage && displayKills > 0) {
            double percentage = (cachedCorleoniteCount * 100.0) / displayKills;
            text = displayKills + " | " + String.format("%.2f%%", percentage);
        }

        Identifier headTexture = MOB_TEXTURES.getOrDefault(currentMobId, TREASURE_HOARDER_HEAD);
        drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, headTexture, xPos, yPos, HEAD_SIZE, HEAD_SIZE);

        drawContext.text(mc.font, text, xPos + HEAD_SIZE + 4, yPos + (HEAD_SIZE / 4), 0xFFFFFFFF, false);
    }

    /** Width the kill counter occupies, used to size the drag target. */
    public static int killHudWidth() {
        Minecraft mc = Minecraft.getInstance();
        int displayKills = countingMode.isLocal() ? localSessionKills : currentKills;
        if (displayKills < 0) {
            displayKills = 0;
        }
        return HEAD_SIZE + 4 + Math.max(14, mc.font.width("" + displayKills) + 12);
    }

    /** Height of the kill counter row. */
    public static int killHudHeight() {
        return HEAD_SIZE;
    }

    private static CompletableFuture<Integer> retrieveBestiaryKillsAsync(String uuid, String mobId) {
        return CompletableFuture.supplyAsync(() -> {
            String spec = "https://api.hypixel.net/v2/skyblock/profiles?uuid=" + uuid;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(spec);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("API-Key", API_KEY);
                LOGGER.info("[DEBUG] Hypixel API Key: " + API_KEY);

                int responseCode = connection.getResponseCode();
                LOGGER.info("[DEBUG] Hypixel API Response Code: " + responseCode);

                if (responseCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }

                        String jsonRaw = response.toString();

                        JSONObject data = new JSONObject(jsonRaw);
                        if (!data.optBoolean("success", false)) {
                            LOGGER.warn("[DEBUG] API-Aufruf war laut JSON 'success: false'.");
                            return -1;
                        }

                        JSONArray profiles = data.optJSONArray("profiles");
                        if (profiles == null || profiles.length() == 0) {
                            LOGGER.warn("[DEBUG] Keine Profile ('profiles') im JSON gefunden oder Array leer.");
                            return -1;
                        }

                        String pureUuid = uuid.replace("-", "");

                        for (int i = 0; i < profiles.length(); i++) {
                            JSONObject profile = profiles.getJSONObject(i);

                            if (profile.optBoolean("selected", false)) {
                                JSONObject members = profile.optJSONObject("members");
                                if (members != null) {
                                    String targetKey = members.has(pureUuid) ? pureUuid : (members.has(uuid) ? uuid : null);

                                    if (targetKey != null) {
                                        JSONObject memberData = members.getJSONObject(targetKey);
                                        JSONObject bestiary = memberData.optJSONObject("bestiary");
                                        if (bestiary != null) {
                                            JSONObject kills = bestiary.optJSONObject("kills");
                                            if (kills != null) {
                                                int mobKills = kills.optInt(mobId, 0);
                                                LOGGER.info("[DEBUG] Gefunden im selektierten Profil! Kills für " + mobId + ": " + mobKills);
                                                return mobKills;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LOGGER.info("[DEBUG] Kein 'selected' Profil gefunden, versuche erstes Profil in der Liste...");
                        JSONObject firstProfile = profiles.getJSONObject(0);
                        JSONObject members = firstProfile.optJSONObject("members");
                        if (members != null) {
                            String targetKey = members.has(pureUuid) ? pureUuid : (members.has(uuid) ? uuid : null);
                            if (targetKey != null) {
                                JSONObject memberData = members.getJSONObject(targetKey);
                                JSONObject bestiary = memberData.optJSONObject("bestiary");
                                if (bestiary != null) {
                                    JSONObject kills = bestiary.optJSONObject("kills");
                                    if (kills != null) {
                                        int mobKills = kills.optInt(mobId, 0);
                                        LOGGER.info("[DEBUG] Gefunden im ersten Profil! Kills für " + mobId + ": " + mobKills);
                                        return mobKills;
                                    }
                                }
                            }
                        }
                        LOGGER.warn("[DEBUG] Struktur 'members -> uuid -> bestiary -> kills' wurde im JSON nicht wie erwartet gefunden.");
                    }
                } else {
                    try (BufferedReader err = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder errResponse = new StringBuilder();
                        String line;
                        while ((line = err.readLine()) != null) {
                            errResponse.append(line);
                        }
                        LOGGER.error("[DEBUG] API Fehler-Antwort: " + errResponse.toString());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[DEBUG] Ausnahme beim API-Aufruf oder JSON-Parsing:", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return -1;
        });
    }

    private void updateGameInterface(int kills) {
        currentKills = kills;
        LOGGER.info("Aktuelle " + currentMobName + " Kills aktualisiert: " + kills);
    }

    public static boolean isGuiVisible() {
        return guiVisible;
    }

    public static void setGuiVisible(boolean visible) {
        guiVisible = visible;
        config.setHudVisible(visible);
        config.save();
    }

    public static int getCurrentKills() {
        return currentKills;
    }

    public static String getCurrentMobName() {
        return currentMobName;
    }

    public static int getHudX() {
        return hudX;
    }

    public static int getHudY() {
        return hudY;
    }

    public static void setHudPosition(int x, int y) {
        hudX = x;
        hudY = y;
        config.setHudX(x);
        config.setHudY(y);
        config.save();
        LOGGER.info("HUD Position geändert und gespeichert: X=" + x + ", Y=" + y);
    }

    public static CountingMode getCountingMode() {
        return countingMode;
    }

    /** True for any mode that counts locally instead of reading the Hypixel API. */
    public static boolean isLocalCounting() {
        return countingMode.isLocal();
    }

    /** Kept for callers that only care whether a local counter is active. */
    public static boolean isShowSessionKills() {
        return countingMode.isLocal();
    }

    /**
     * Switches the counting mode. Local modes restart their counter from zero (except the
     * persistent one, which restores its stored value), and the Corleonite baseline is
     * re-captured so the drop rate belongs to the new mode.
     */
    public static void setCountingMode(CountingMode mode) {
        if (mode == null || mode == countingMode) {
            return;
        }

        countingMode = mode;
        config.setCountingMode(mode.id());

        switch (mode) {
            case LOCAL_PERSISTENT -> {
                localSessionKills = config.getStoredKills();
                localSessionStartCorleonite = Math.max(0, config.getStoredCorleonite());
            }
            case LOCAL_SESSION, LOCAL_PER_WORLD -> {
                localSessionKills = 0;
                localSessionStartCorleonite = getLocalCorleoniteCount();
            }
            case API -> {
                // Nothing local is used; leave currentKills to the API poller.
            }
        }

        sessionStartTime = System.currentTimeMillis();
        config.save();
        LOGGER.info("Counting mode set to " + mode.id() + " (local kills: " + localSessionKills + ")");
    }

    public static CountingMode cycleCountingMode() {
        setCountingMode(countingMode.next());
        return countingMode;
    }

    /** Clears the local counter (and, for the persistent mode, the stored value). */
    public static void resetLocalCounting() {
        localSessionKills = 0;
        localSessionStartCorleonite = getLocalCorleoniteCount();
        sessionStartTime = System.currentTimeMillis();
        if (countingMode.isPersistent()) {
            config.setStoredKills(0);
            config.setStoredCorleonite(localSessionStartCorleonite);
        }
        config.save();
    }

    /** Called when the player changes world; only the per-world mode resets. */
    public static void onWorldChanged() {
        if (countingMode.resetsOnWorldChange()) {
            localSessionKills = 0;
            localSessionStartCorleonite = getLocalCorleoniteCount();
            sessionStartTime = System.currentTimeMillis();
            LOGGER.info("World changed - per-world kill counter reset to 0");
        }
    }

    public static int getSessionKills() {
        return countingMode.isLocal() ? localSessionKills : 0;
    }

    public static boolean isShowCorleonitePercentage() {
        return showCorleonitePercentage;
    }

    public static boolean isShowRespawnTimer() {
        return showRespawnTimerHud;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static boolean isTimerSoundEnabled() {
        return config.isTimerSoundEnabled();
    }

    /** Switches the mod UI language and persists the choice. */
    public static void setModLanguage(de.maexle.skyblockcounter.i18n.ModLang.Language language) {
        de.maexle.skyblockcounter.i18n.ModLang.setCurrent(language);
        config.setLanguage(language.id());
        config.save();
        LOGGER.info("Mod language set to " + language.id());
    }

    public static de.maexle.skyblockcounter.i18n.ModLang.Language getModLanguage() {
        return de.maexle.skyblockcounter.i18n.ModLang.current();
    }

    public static void setTimerSoundEnabled(boolean enabled) {
        config.setTimerSoundEnabled(enabled);
        config.save();
        LOGGER.info("Respawn timer sound " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isClearWaypointsOnWorldChange() {
        return config.isClearWaypointsOnWorldChange();
    }

    public static void setClearWaypointsOnWorldChange(boolean clear) {
        config.setClearWaypointsOnWorldChange(clear);
        config.save();
        LOGGER.info("Clear waypoints on world change " + (clear ? "enabled" : "disabled"));
    }

    /**
     * Debug mode makes both HUD elements render outside of Skyblock (for example in a
     * singleplayer test world) without needing a Hypixel API key or a real Corleone kill.
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        config.setDebugMode(enabled);
        config.save();
        LOGGER.info("Debug mode " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean toggleDebugMode() {
        setDebugMode(!debugMode);
        return debugMode;
    }

    public static void setShowRespawnTimer(boolean show) {
        showRespawnTimerHud = show;
        config.setShowRespawnTimer(show);
        config.save();
    }

    public static int getTimerX() {
        return timerX;
    }

    public static int getTimerY() {
        return timerY;
    }

    public static void setTimerPosition(int x, int y) {
        timerX = x;
        timerY = y;
        config.setTimerX(x);
        config.setTimerY(y);
        config.save();
    }

    public static void setShowCorleonitePercentage(boolean show) {
        showCorleonitePercentage = show;
        config.setShowCorleonitePercentage(show);
        config.save();
        if (show) {
            getCorleoniteCount().thenAccept(count -> {
                cachedCorleoniteCount = count;
                LOGGER.info("Corleonite count cached: " + count);
            });
        }
    }

    public static int getCachedCorleoniteCount() {
        return cachedCorleoniteCount;
    }

    public static void setStartSessionKills(int kills) {
        localSessionKills = kills;
    }

    public static void addSessionKills(int kills) {
        localSessionKills += kills;
    }

    public static void removeSessionKills(int kills) {
        if ((localSessionKills - kills) >= 0){
            localSessionKills -= kills;
        }
    }

    public static void setApiKey(String key) {
        API_KEY = key;
    }

    public static void setUndashedUuid(String uuid) {
        undashedUuid = uuid;
    }

    public static void switchMob(String mobId, String mobName) {
        currentMobId = mobId;
        currentMobName = mobName;
        config.setLastMobId(mobId);
        config.setLastMobName(mobName);
        config.setAPI_KEY(config.getAPI_KEY());
        config.setundashedUuid(config.getundashedUuid());
        config.save();
        currentKills = -1;
        LOGGER.info("Switched to mob: " + mobName + " (" + mobId + ") und gespeichert");

        retrieveBestiaryKillsAsync(undashedUuid, mobId)
                .thenAccept(kills -> {
                    if (kills >= 0) {
                        currentKills = kills;
                        LOGGER.info("Updated " + mobName + " Kills: " + kills);
                        if (showCorleonitePercentage) {
                            getCorleoniteCount().thenAccept(count -> {
                                cachedCorleoniteCount = count;
                                LOGGER.info("Corleonite count updated: " + count);
                            });
                        }
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Fehler bei asynchroner Datenverarbeitung (switchMob): " + ex.getMessage());
                    return null;
                });
    }

    public static SkyblockCounterConfig getConfig() {
        return config;
    }

    public static void saveConfig() {
        config.save();
    }

    public static void reloadMobTextures() {
        MOB_TEXTURES.clear();
        for (SkyblockCounterConfig.MobEntry entry : config.getMobEntries()) {
            if (entry.texture != null) {
                MOB_TEXTURES.put(entry.id, Identifier.fromNamespaceAndPath("skyblockcounter", entry.texture));
            }
        }
        LOGGER.info("Mob-Textures neu geladen: " + MOB_TEXTURES.size() + " Mobs");
    }

    /**
     * Corleonite collected this session, sourced from Hypixel's [Sacks] messages.
     *
     * <p>Sacks swallow the item immediately, so scanning the player inventory would always
     * report zero for them. The value is still topped up by whatever is currently held in the
     * inventory, in case a sack message was missed while the item was collected manually.
     */
    public static int getLocalCorleoniteCount() {
        return sackCorleoniteTotal + countCorleoniteInInventory();
    }

    /** Corleonite currently sitting in the player inventory. */
    private static int countCorleoniteInInventory() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;

        int count = 0;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getHoverName().getString().contains("Corleonite")) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int getSackCorleoniteTotal() {
        return sackCorleoniteTotal;
    }

    public static void resetSackCorleoniteTotal() {
        sackCorleoniteTotal = 0;
    }

    /**
     * Manually adjusts the Corleonite counter, for cases where a [Sacks] message was missed.
     */
    public static void adjustSackCorleonite(int delta) {
        sackCorleoniteTotal = Math.max(0, sackCorleoniteTotal + delta);
        LOGGER.info("[Sacks] Corleonite manually adjusted by " + delta + " -> " + sackCorleoniteTotal);
    }

    public static CompletableFuture<Integer> getCorleoniteCount() {
        return CompletableFuture.supplyAsync(() -> {
            String spec = "https://api.hypixel.net/v2/skyblock/profiles?uuid=" + undashedUuid;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(spec);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("API-Key", API_KEY);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }

                        JSONObject data = new JSONObject(response.toString());
                        if (!data.optBoolean("success", false)) {
                            return -1;
                        }

                        JSONArray profiles = data.optJSONArray("profiles");
                        if (profiles == null || profiles.length() == 0) {
                            return -1;
                        }

                        for (int i = 0; i < profiles.length(); i++) {
                            JSONObject profile = profiles.getJSONObject(i);
                            if (profile.optBoolean("selected", false)) {
                                JSONObject members = profile.optJSONObject("members");
                                if (members != null) {
                                    String targetKey = members.has(undashedUuid) ? undashedUuid : (members.has(undashedUuid.replace("-", "")) ? undashedUuid.replace("-", "") : null);
                                    if (targetKey != null) {
                                        JSONObject memberData = members.getJSONObject(targetKey);
                                        return countCorleoniteInInventory(memberData);
                                    }
                                }
                            }
                        }

                        JSONObject firstProfile = profiles.getJSONObject(0);
                        JSONObject members = firstProfile.optJSONObject("members");
                        if (members != null) {
                            for (String key : members.keySet()) {
                                JSONObject memberData = members.getJSONObject(key);
                                int count = countCorleoniteInInventory(memberData);
                                if (count >= 0) {
                                    return count;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Fehler beim Abrufen des Inventars: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return -1;
        });
    }

    public static int countCorleoniteInInventory(JSONObject memberData) {
        try {
            LOGGER.info("[DEBUG] countCorleoniteInInventory called");
            LOGGER.info("[DEBUG] MemberData keys: " + memberData.keySet());
            int corleoniteCount = 0;

            String[] inventoryKeys = {"inventory"};

            for (String key : inventoryKeys) {
                JSONObject inventory = memberData.optJSONObject(key);
                LOGGER.info("[DEBUG] Inventory " + key + " exists: " + (inventory != null));
                if (inventory != null) {
                    LOGGER.info("[DEBUG] Inventory " + key + " keys: " + inventory.keySet());
                    String data = inventory.optString("data", "");
                    LOGGER.info("[DEBUG] Inventory " + key + " has data field: " + !data.isEmpty());

                    if (!data.isEmpty()) {
                        LOGGER.info("[DEBUG] Checking inventory: " + key);
                        int count = countCorleoniteInNBT(data);
                        LOGGER.info("[DEBUG] Found " + count + " Corleonite in " + key);
                        corleoniteCount += count;
                    } else {
                        for (String itemKey : inventory.keySet()) {
                            LOGGER.info("[DEBUG] Checking field: " + itemKey);
                            Object itemValue = inventory.get(itemKey);

                            if (itemValue instanceof JSONObject) {
                                JSONObject nestedObj = (JSONObject) itemValue;
                                String nestedData = nestedObj.optString("data", "");
                                if (!nestedData.isEmpty()) {
                                    LOGGER.info("[DEBUG] Field " + itemKey + " has data, trying to parse");
                                    int count = countCorleoniteInNBT(nestedData);
                                    LOGGER.info("[DEBUG] Found " + count + " Corleonite in " + itemKey);
                                    corleoniteCount += count;
                                } else {
                                    LOGGER.info("[DEBUG] Field " + itemKey + " has no data field");
                                }
                            } else if (itemValue instanceof String) {
                                String itemStr = (String) itemValue;
                                if (!itemStr.isEmpty() && itemStr.length() > 10) {
                                    LOGGER.info("[DEBUG] Field " + itemKey + " might contain data, trying to parse");
                                    int count = countCorleoniteInNBT(itemStr);
                                    LOGGER.info("[DEBUG] Found " + count + " Corleonite in " + itemKey);
                                    corleoniteCount += count;
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("Corleonite im Inventar gefunden: " + corleoniteCount);
            return corleoniteCount;

        } catch (Exception e) {
            LOGGER.error("Fehler in countCorleoniteInInventory: " + e.getMessage(), e);
            return -1;
        }
    }

    private static int countCorleoniteInNBT(String base64Data) {
        int count = 0;
        try {
            String cleanedData = base64Data.replace("\\u003d", "=");

            byte[] decoded = Base64.getDecoder().decode(cleanedData);

            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            GZIPInputStream gzis = new GZIPInputStream(bais);
            DataInputStream dis = new DataInputStream(gzis);
            CompoundTag nbt = NbtIo.read(dis);
            dis.close();

            if (nbt != null && nbt.contains("i")) {
                var itemsList = nbt.getList("i");
                if (itemsList.isPresent()) {
                    LOGGER.info("[DEBUG] NBT contains " + itemsList.get().size() + " items");
                    for (Tag itemTag : itemsList.get()) {
                        if (itemTag instanceof CompoundTag) {
                            CompoundTag item = (CompoundTag) itemTag;
                            if (item.contains("tag")) {
                                var tagOpt = item.getCompound("tag");
                                if (tagOpt.isPresent()) {
                                    CompoundTag tag = tagOpt.get();
                                    if (tag.contains("display")) {
                                        var displayOpt = tag.getCompound("display");
                                        if (displayOpt.isPresent()) {
                                            CompoundTag display = displayOpt.get();
                                            if (display.contains("Name")) {
                                                var nameOpt = display.getString("Name");
                                                if (nameOpt.isPresent()) {
                                                    String itemName = nameOpt.get();
                                                    LOGGER.info("[DEBUG] Found item: " + itemName);
                                                    if (itemName.contains("Corleonite")) {
                                                        LOGGER.info("[DEBUG] Found Corleonite item!");
                                                        if (item.contains("Count")) {
                                                            var countOpt = item.getInt("Count");
                                                            if (countOpt.isPresent()) {
                                                                count += countOpt.get();
                                                                LOGGER.info("[DEBUG] Corleonite count: " + countOpt.get());
                                                            } else {
                                                                count += 1;
                                                                LOGGER.info("[DEBUG] Corleonite count: 1 (default)");
                                                            }
                                                        } else {
                                                            count += 1;
                                                            LOGGER.info("[DEBUG] Corleonite count: 1 (no Count tag)");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LOGGER.info("[DEBUG] NBT is null or does not contain 'i' key");
            }
        } catch (IOException e) {
            LOGGER.error("Fehler beim Parsen von NBT-Daten: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Dekodieren der Inventarsdaten: " + e.getMessage(), e);
        }
        return count;
    }

    public static String getCorleoniteDropPercentage() {
        int corleoniteCount = getLocalCorleoniteCount();

        if (corleoniteCount < 0) {
            return "Fehler beim Abrufen der Corleonite-Anzahl";
        }

        int sessionKills = getSessionKills();
        if (sessionKills <= 0) {
            return "Session-Kills sind 0 oder Session-Modus nicht aktiv";
        }

        double percentage = (corleoniteCount * 100.0) / sessionKills;
        if (!showCorleonitePercentage) {
            showCorleonitePercentage = true;
        } else {
            showCorleonitePercentage = false;
        }
        cachedCorleoniteCount = corleoniteCount;
        return String.format("Corleonite: %d | Session-Kills: %d | Drop-Rate: %.2f%%", corleoniteCount, sessionKills, percentage);
    }

    public static CompletableFuture<String> getCorleoniteDropPercentageAPI() {
        return getCorleoniteCount().thenApply(corleoniteCount -> {
            if (corleoniteCount < 0) {
                return "Fehler beim Abrufen der Corleonite-Anzahl";
            }

            int sessionKills = getSessionKills();
            if (sessionKills <= 0) {
                return "Session-Kills sind 0 oder Session-Modus nicht aktiv";
            }

            double percentage = (corleoniteCount * 100.0) / sessionKills;
            if (!showCorleonitePercentage) {
                showCorleonitePercentage = true;
            } else {
                showCorleonitePercentage = false;
            }
            cachedCorleoniteCount = corleoniteCount;
            return String.format("Corleonite: %d | Session-Kills: %d | Drop-Rate: %.2f%%", corleoniteCount, sessionKills, percentage);
        });
    }
}