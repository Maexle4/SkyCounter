package de.maexle.skyblockcounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkyblockCounterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/skyblockcounter.json");

    private int hudX = 10;
    private int hudY = 10;
    private boolean hudVisible = false;
    private boolean showCorleonitePercentage = false;
    private boolean showRespawnTimer = true;
    private boolean debugMode = false;
    private boolean timerSoundEnabled = false;
    private String language = "en_us";
    private String countingMode = "api";
    /** Persisted kill/corleonite counters, only used by the LOCAL_PERSISTENT mode. */
    private int storedKills = 0;
    private int storedCorleonite = 0;
    private boolean clearWaypointsOnWorldChange = false;
    private int timerX = 10;
    private int timerY = 40;
    private String lastMobId = "zealot_enderman_55";
    private String lastMobName = "Zealot";
    public List<MobEntry> mobEntries = new ArrayList<>();

    private String API_KEY = "";
    private String undashedUuid = "";

    public static class MobEntry {
        public String id;
        public String name;
        public String texture;

        public MobEntry(String id, String name, String texture) {
            this.id = id;
            this.name = name;
            this.texture = texture;
        }
    }

    public List<WaypointEntry> waypoints = new ArrayList<>();

    public static class WaypointEntry {
        public String name;
        public int x, y, z;
        public int color;

        public WaypointEntry(String name, int x, int y, int z, int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }

    public static SkyblockCounterConfig load() {
        if (!CONFIG_FILE.exists()) {
            return new SkyblockCounterConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json == null) {
                return new SkyblockCounterConfig();
            }
            SkyblockCounterConfig config = new SkyblockCounterConfig();
            
            if (json.has("hudX")) config.hudX = json.get("hudX").getAsInt();
            if (json.has("hudY")) config.hudY = json.get("hudY").getAsInt();
            if (json.has("hudVisible")) config.hudVisible = json.get("hudVisible").getAsBoolean();
            if (json.has("showCorleonitePercentage")) config.showCorleonitePercentage = json.get("showCorleonitePercentage").getAsBoolean();
            if (json.has("showRespawnTimer")) config.showRespawnTimer = json.get("showRespawnTimer").getAsBoolean();
            if (json.has("debugMode")) config.debugMode = json.get("debugMode").getAsBoolean();
            if (json.has("timerSoundEnabled")) config.timerSoundEnabled = json.get("timerSoundEnabled").getAsBoolean();
            if (json.has("language")) config.language = json.get("language").getAsString();
            if (json.has("countingMode")) config.countingMode = json.get("countingMode").getAsString();
            if (json.has("storedKills")) config.storedKills = json.get("storedKills").getAsInt();
            if (json.has("storedCorleonite")) config.storedCorleonite = json.get("storedCorleonite").getAsInt();
            if (json.has("clearWaypointsOnWorldChange")) config.clearWaypointsOnWorldChange = json.get("clearWaypointsOnWorldChange").getAsBoolean();
            if (json.has("timerX")) config.timerX = json.get("timerX").getAsInt();
            if (json.has("timerY")) config.timerY = json.get("timerY").getAsInt();
            if (json.has("lastMobId")) config.lastMobId = json.get("lastMobId").getAsString();
            if (json.has("lastMobName")) config.lastMobName = json.get("lastMobName").getAsString();
            if (json.has("API_KEY")) config.API_KEY = json.get("API_KEY").getAsString();
            if (json.has("undashedUuid")) config.undashedUuid = json.get("undashedUuid").getAsString();
            if (json.has("mobEntries")) {
                JsonArray entriesArray = json.getAsJsonArray("mobEntries");
                for (int i = 0; i < entriesArray.size(); i++) {
                    JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
                    String id = entryObj.get("id").getAsString();
                    String name = entryObj.get("name").getAsString();
                    String texture = entryObj.has("texture") ? entryObj.get("texture").getAsString() : null;
                    config.mobEntries.add(new MobEntry(id, name, texture));
                }
            }
            
            // Default mobs if list is empty
            if (config.mobEntries.isEmpty()) {
                config.mobEntries.add(new MobEntry("treasure_hoarder_70", "Treasure Hoarder", "treasure_hoarder_head"));
                config.mobEntries.add(new MobEntry("team_treasurite_corleone_200", "Corleone", "boss_corleone_head"));
                config.mobEntries.add(new MobEntry("zealot_enderman_55", "Zealot", "zealot_enderman_head"));
            }

            if (json.has("waypoints")) {
                JsonArray waypointsArray = json.getAsJsonArray("waypoints");
                for (int i = 0; i < waypointsArray.size(); i++) {
                    JsonObject wpObj = waypointsArray.get(i).getAsJsonObject();
                    String name = wpObj.get("name").getAsString();
                    int x = wpObj.get("x").getAsInt();
                    int y = wpObj.get("y").getAsInt();
                    int z = wpObj.get("z").getAsInt();
                    int color = wpObj.has("color") ? wpObj.get("color").getAsInt() : 0xFFFF5555;
                    config.waypoints.add(new WaypointEntry(name, x, y, z, color));
                }
            }

            if(config.undashedUuid.isEmpty() || config.undashedUuid.equals(" ")) {
                Minecraft client = Minecraft.getInstance();
                UUID uuid = client.getUser().getProfileId();
                config.undashedUuid = uuid.toString().replace("-", "");
            }

            return config;
        } catch (IOException e) {
            e.printStackTrace();
            return new SkyblockCounterConfig();
        }
    }
    
    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            JsonObject json = new JsonObject();
            json.addProperty("hudX", hudX);
            json.addProperty("hudY", hudY);
            json.addProperty("hudVisible", hudVisible);
            json.addProperty("showCorleonitePercentage", showCorleonitePercentage);
            json.addProperty("showRespawnTimer", showRespawnTimer);
            json.addProperty("debugMode", debugMode);
            json.addProperty("timerSoundEnabled", timerSoundEnabled);
            json.addProperty("language", language);
            json.addProperty("countingMode", countingMode);
            json.addProperty("storedKills", storedKills);
            json.addProperty("storedCorleonite", storedCorleonite);
            json.addProperty("clearWaypointsOnWorldChange", clearWaypointsOnWorldChange);
            json.addProperty("timerX", timerX);
            json.addProperty("timerY", timerY);
            json.addProperty("lastMobId", lastMobId);
            json.addProperty("lastMobName", lastMobName);

            json.addProperty("API_KEY", API_KEY);
            json.addProperty("undashedUuid", undashedUuid);

            JsonArray entriesArray = new JsonArray();
            for (MobEntry entry : mobEntries) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("id", entry.id);
                entryObj.addProperty("name", entry.name);
                if (entry.texture != null) {
                    entryObj.addProperty("texture", entry.texture);
                }
                entriesArray.add(entryObj);
            }
            json.add("mobEntries", entriesArray);

            JsonArray waypointsArray = new JsonArray();
            for (WaypointEntry wp : waypoints) {
                JsonObject wpObj = new JsonObject();
                wpObj.addProperty("name", wp.name);
                wpObj.addProperty("x", wp.x);
                wpObj.addProperty("y", wp.y);
                wpObj.addProperty("z", wp.z);
                wpObj.addProperty("color", wp.color);
                waypointsArray.add(wpObj);
            }
            json.add("waypoints", waypointsArray);

            if(undashedUuid.isEmpty() || undashedUuid.equals(" ")) {
                Minecraft client = Minecraft.getInstance();
                UUID uuid = client.getUser().getProfileId();
                undashedUuid = uuid.toString().replace("-", "");
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addWaypoint(String name, int x, int y, int z, int color) {
        boolean foundWaypoint = false;
        for(WaypointEntry existingWP : waypoints){
            if(!(existingWP.x == x) && !foundWaypoint) {
                foundWaypoint = false;
            } else {
                foundWaypoint = true;
                break;
            }
        }
        if(!foundWaypoint) {
                waypoints.add(new WaypointEntry(name, x, y, z, color));
                save();
        }
    }

    public void removeWaypoint(String name) {
        waypoints.removeIf(wp -> wp.name.equals(name));
        save();
    }

    public int getHudX() {
        return hudX;
    }
    
    public void setHudX(int hudX) {
        this.hudX = hudX;
    }
    
    public int getHudY() {
        return hudY;
    }
    
    public void setHudY(int hudY) {
        this.hudY = hudY;
    }

    public boolean isHudVisible() {
        return hudVisible;
    }

    public void setHudVisible(boolean hudVisible) {
        this.hudVisible = hudVisible;
    }

    public boolean isShowCorleonitePercentage() {
        return showCorleonitePercentage;
    }

    public void setShowCorleonitePercentage(boolean showCorleonitePercentage) {
        this.showCorleonitePercentage = showCorleonitePercentage;
    }

    public boolean isShowRespawnTimer() {
        return showRespawnTimer;
    }

    public void setShowRespawnTimer(boolean showRespawnTimer) {
        this.showRespawnTimer = showRespawnTimer;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isTimerSoundEnabled() {
        return timerSoundEnabled;
    }

    public String getCountingMode() {
        return countingMode;
    }

    public void setCountingMode(String countingMode) {
        this.countingMode = countingMode;
    }

    public int getStoredKills() {
        return storedKills;
    }

    public void setStoredKills(int storedKills) {
        this.storedKills = storedKills;
    }

    public int getStoredCorleonite() {
        return storedCorleonite;
    }

    public void setStoredCorleonite(int storedCorleonite) {
        this.storedCorleonite = storedCorleonite;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setTimerSoundEnabled(boolean timerSoundEnabled) {
        this.timerSoundEnabled = timerSoundEnabled;
    }

    public boolean isClearWaypointsOnWorldChange() {
        return clearWaypointsOnWorldChange;
    }

    public void setClearWaypointsOnWorldChange(boolean clearWaypointsOnWorldChange) {
        this.clearWaypointsOnWorldChange = clearWaypointsOnWorldChange;
    }

    public int getTimerX() {
        return timerX;
    }

    public void setTimerX(int timerX) {
        this.timerX = timerX;
    }

    public int getTimerY() {
        return timerY;
    }

    public void setTimerY(int timerY) {
        this.timerY = timerY;
    }
    
    public String getLastMobId() {
        return lastMobId;
    }
    
    public void setLastMobId(String lastMobId) {
        this.lastMobId = lastMobId;
    }
    
    public String getAPI_KEY() {
        return API_KEY;
    }
    
    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;
    }

    public String getundashedUuid() {
        return undashedUuid;
    }

    public void setundashedUuid(String undashedUuid) {
        this.undashedUuid = undashedUuid;
    }

    public String getLastMobName() {
        return lastMobName;
    }

    public void setLastMobName(String lastMobName) {
        this.lastMobName = lastMobName;
    }
    
    public List<MobEntry> getMobEntries() {
        return mobEntries;
    }
    
    public void addMobEntry(String id, String name, String texture) {
        mobEntries.add(new MobEntry(id, name, texture));
    }
    
    public void removeMobEntry(String id) {
        mobEntries.removeIf(entry -> entry.id.equals(id));
    }
}
