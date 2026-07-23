package de.maexle.skyblockcounter;

import org.json.JSONArray;
import org.json.JSONObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.sound.SoundEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import java.util.HashSet;
import java.util.Set;

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
    private static volatile boolean showSessionKills = false;
    private static volatile boolean showCorleonitePercentage = false;
    private static volatile int cachedCorleoniteCount = 0;

    private static volatile int localSessionKills = 0;
    private static volatile int localSessionStartCorleonite = 0;

    private static final Identifier TREASURE_HOARDER_HEAD = Identifier.of("skyblockcounter", "textures/gui/sprites/treasure_hoarder_head.png");
    private static final Identifier CORLEONE_HEAD = Identifier.of("skyblockcounter", "textures/gui/sprites/boss_corleone_head.png");
    private static final Identifier ZEALOT_HEAD = Identifier.of("skyblockcounter", "textures/gui/sprites/zealot_enderman_head.png");
    private static final int HEAD_SIZE = 16;

    List<SkyblockCounterConfig.MobEntry> entries = SkyblockCounterService.getConfig().getMobEntries();

    private static final Set<Integer> knownCorleones = new HashSet<>();

    private long lastUnloadTrigger = 0;

    private static final Map<String, Identifier> MOB_TEXTURES = new HashMap<>();
    private static SkyblockCounterConfig config;

    static {
        config = SkyblockCounterConfig.load();
        hudX = config.getHudX();
        API_KEY = config.getAPI_KEY();
        undashedUuid = config.getundashedUuid();
        hudY = config.getHudY();
        currentMobId = config.getLastMobId();
        currentMobName = config.getLastMobName();
        
        // Build MOB_TEXTURES from config entries
        for (SkyblockCounterConfig.MobEntry entry : config.getMobEntries()) {
            if (entry.texture != null) {
                MOB_TEXTURES.put(entry.id, Identifier.of("skyblockcounter", entry.texture));
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
            if (client.world != null) {
                for (Entity entity : client.world.getEntities()) {
                    if (entity.getCustomName() != null) {
                        String name = entity.getCustomName().getString();

                        if (name.contains("Corleone") && !knownCorleones.contains(entity.getId())) {
                            knownCorleones.add(entity.getId());
                            LOGGER.info("[DEBUG] " + name + " hat jetzt einen Namen! Spiele Sound 3x ab...");

                            for (int i = 0; i < 3; i++) {
                                scheduler.schedule(() -> {
                                    client.execute(() -> {
                                        if (client.player != null) {
                                            client.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 20.0F, 1.0F);
                                        }
                                    });
                                }, (long) i * 300, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                }
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

                        if (showSessionKills) {
                            localSessionKills++;
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

        // Alle 30 Sekunden standardmäßig abfragen
        scheduler.scheduleAtFixedRate(this::fetchCurrentBestiaryKills, 0, 30, TimeUnit.SECONDS);
    }

    private void pingForNextCorleone() {
        if(isShowSessionKills()){
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                for (int i = 0; i < 3; i++) {
                    scheduler.schedule(() -> {
                        client.execute(() -> {
                            if (client.player != null) {
                                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 25.0F, 1.0F);
                            }
                        });
                    }, (long) i * 300, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private void fetchCurrentBestiaryKills() {
        if (showSessionKills) return;

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
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (guiVisible) {
                renderHud(drawContext);
            }
        });
    }

    private void renderHud(DrawContext drawContext) {
        if (entries.isEmpty()) {
            SkyblockCounterConfig config = SkyblockCounterService.getConfig();
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleonite Boss", "textures/gui/sprites/boss_corleone_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            config.save();
            SkyblockCounterService.reloadMobTextures();
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }

        int xPos = hudX;
        int yPos = hudY;

        int displayKills = showSessionKills ? localSessionKills : currentKills;
        String text = "" + displayKills;

        if (showSessionKills && showCorleonitePercentage && displayKills > 0) {
            int currentLocalCorleonite = getLocalCorleoniteCount();
            int sessionDrops = Math.max(0, currentLocalCorleonite - localSessionStartCorleonite);

            double percentage = (currentLocalCorleonite * 100.0) / displayKills;
            text = displayKills + " | " + String.format("%.2f%%", percentage);

        } else if (!showSessionKills && showCorleonitePercentage && displayKills > 0) {
            // Fallback für den normalen API-Modus (außerhalb der Session)
            double percentage = (cachedCorleoniteCount * 100.0) / displayKills;
            text = displayKills + " | " + String.format("%.2f%%", percentage);
        }

        Identifier headTexture = MOB_TEXTURES.getOrDefault(currentMobId, TREASURE_HOARDER_HEAD);

        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, headTexture, xPos, yPos, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);

        int textX = xPos + HEAD_SIZE + 4;
        drawContext.drawTextWithShadow(mc.textRenderer, text, textX, yPos + (HEAD_SIZE / 4), 0xFFFFFFFF);
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

    public static boolean isShowSessionKills() {
        return showSessionKills;
    }

    public static void setShowSessionKills(boolean show) {
        if (show && !showSessionKills) {
            localSessionKills = 0;
            localSessionStartCorleonite = getLocalCorleoniteCount();
            LOGGER.info("Session-Kills Modus aktiviert. Lokale Kills: 0, Start-Corleonite: " + localSessionStartCorleonite);
        } else if (!show) {
            LOGGER.info("Session-Kills Modus deaktiviert");
        }
        showSessionKills = show;
    }

    public static int getSessionKills() {
        return showSessionKills ? localSessionKills : 0;
    }

    public static boolean isShowCorleonitePercentage() {
        return showCorleonitePercentage;
    }

    public static void setShowCorleonitePercentage(boolean show) {
        showCorleonitePercentage = show;
        if (show) {
            // Refresh Corleonite count when enabling
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

    public static void addStartSessionKills(int kills) {
        localSessionKills += kills;
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
                        // Update Corleonite count when kills are updated
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
                MOB_TEXTURES.put(entry.id, Identifier.of("skyblockcounter", entry.texture));
            }
        }
        LOGGER.info("Mob-Textures neu geladen: " + MOB_TEXTURES.size() + " Mobs");
    }

    private static int getLocalCorleoniteCount() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;

        int count = 0;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            net.minecraft.item.ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().contains("Corleonite")) {
                count += stack.getCount();
            }
        }
        return count;
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

                        // Fallback to first profile
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

    private static int countCorleoniteInInventory(JSONObject memberData) {
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
            // Handle unicode escapes in base64
            String cleanedData = base64Data.replace("\\u003d", "=");
            
            byte[] decoded = Base64.getDecoder().decode(cleanedData);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            GZIPInputStream gzis = new GZIPInputStream(bais);
            DataInputStream dis = new DataInputStream(gzis);
            NbtCompound nbt = NbtIo.readCompound(dis);
            dis.close();
            
            if (nbt != null && nbt.contains("i")) {
                var itemsList = nbt.getList("i");
                if (itemsList.isPresent()) {
                    LOGGER.info("[DEBUG] NBT contains " + itemsList.get().size() + " items");
                    for (NbtElement itemTag : itemsList.get()) {
                        if (itemTag instanceof NbtCompound) {
                            NbtCompound item = (NbtCompound) itemTag;
                            if (item.contains("tag")) {
                                var tagOpt = item.getCompound("tag");
                                if (tagOpt.isPresent()) {
                                    NbtCompound tag = tagOpt.get();
                                    if (tag.contains("display")) {
                                        var displayOpt = tag.getCompound("display");
                                        if (displayOpt.isPresent()) {
                                            NbtCompound display = displayOpt.get();
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