package de.maexle.skyblockcounter.i18n;

import de.maexle.skyblockcounter.SkyblockCounterService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny self-contained translation layer for the mod UI.
 *
 * <p>Vanilla resource-pack language files follow the game's selected language, which the player
 * cannot change per-mod. SkyCounter therefore keeps its own small string table so the mod can be
 * switched between English and Chinese independently of Minecraft.
 *
 * <p>Proper nouns are intentionally left untranslated in both languages: Skyblock item and mob
 * names (Corleone, Corleonite, Treasure Hoarder, Zealot, Jade Gemstone ...), Hypixel-isms
 * (Bestiary, Sacks, Bazaar) and the mod name itself.
 */
public final class ModLang {

    public enum Language {
        ENGLISH("en_us", "English"),
        CHINESE("zh_cn", "中文");

        private final String id;
        private final String displayName;

        Language(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** key -> { english, chinese } */
    private static final Map<String, String[]> TABLE = new LinkedHashMap<>();

    private static Language current = Language.ENGLISH;

    private ModLang() {
    }

    private static void put(String key, String en, String zh) {
        TABLE.put(key, new String[]{en, zh});
    }

    static {
        // ---- window chrome ----
        put("title", "SkyCounter Configuration", "SkyCounter 配置");
        put("header.categories", "CATEGORIES", "分类");
        put("header.config", "CONFIGURATION", "配置");
        put("footer.esc", "ESC to close", "ESC 关闭");
        put("tab.language", "Language", "语言");

        // ---- category labels ----
        put("cat.general.label", "General", "通用");
        put("cat.general.title", "General Settings", "通用设置");
        put("cat.general.desc", "Control the main display options.", "控制主要的显示选项。");

        put("cat.hud.label", "HUD", "HUD");
        put("cat.hud.title", "HUD Placement", "HUD 位置");
        put("cat.hud.desc", "Set where the counter appears on screen.", "设置计数器在屏幕上的位置。");

        put("cat.mobs.label", "Mobs", "怪物");
        put("cat.mobs.title", "Mob Tracking", "怪物追踪");
        put("cat.mobs.desc", "Switch targets and manage custom mobs.", "切换目标并管理自定义怪物。");

        put("cat.api.label", "API", "API");
        put("cat.api.title", "API Settings", "API 设置");
        put("cat.api.desc", "Configure Hypixel API access.", "配置 Hypixel API 访问。");

        put("cat.session.label", "Session", "会话");
        put("cat.session.title", "Session Tracking", "会话统计");
        put("cat.session.desc", "Track kills and view statistics for your current session.", "追踪击杀并查看当前会话的统计数据。");

        put("cat.tools.label", "Tools", "工具");
        put("cat.tools.title", "Tools", "工具");
        put("cat.tools.desc", "Utility features for Skyblock farming.", "Skyblock 刷怪的辅助功能。");

        put("cat.language.label", "Language", "语言");
        put("cat.language.title", "Mod Language", "模组语言");
        put("cat.language.desc", "Changes only this mod's interface. Proper nouns stay untranslated.", "仅修改本模组的界面语言，专有名词不翻译。");

        // ---- general toggles ----
        put("opt.showHud", "Show HUD", "显示 HUD");
        put("opt.showHud.desc", "Shows or hides the SkyCounter in-game.", "显示或隐藏游戏内的 SkyCounter。");

        put("opt.percentage", "Corleonite Percentage", "Corleonite 掉率百分比");
        put("opt.percentage.desc", "Displays the Corleonite drop rate in the HUD.", "在 HUD 中显示 Corleonite 掉率。");

        put("opt.timer", "Corleone Respawn Timer", "Corleone 刷新计时");
        put("opt.timer.desc", "Shows the Corleone respawn countdown on screen.", "在屏幕上显示 Corleone 刷新倒计时。");

        put("opt.debug", "Debug Mode", "调试模式");
        put("opt.debug.desc", "Shows the HUD in any world so it can be tested in singleplayer.", "在任何世界显示 HUD，便于单人测试。");

        put("opt.clearWaypoints", "Clear Waypoints on World Change", "切换世界时清除路径点");
        put("opt.clearWaypoints.desc", "Removes saved Corleone waypoints when you switch island.", "切换岛屿时删除已保存的 Corleone 路径点。");

        put("opt.timerSound", "Respawn Timer Sound", "计时结束提示音");
        put("opt.timerSound.desc", "Plays a chime when the respawn window ends.", "刷新窗口结束时播放提示音。");

        // ---- HUD tab ----
        put("hud.explain1", "Opens a full-screen editor overlay: the game stays visible and", "打开全屏编辑界面：游戏画面保持可见，");
        put("hud.explain2", "you drag the kill counter and respawn timer to where you want them.", "直接拖动击杀计数和刷新计时到想要的位置。");
        put("hud.explain3", "Positions save automatically as you drag.", "拖动时自动保存位置。");
        put("hud.edit", "Edit HUD in Game", "在游戏内编辑 HUD");
        put("hud.reset", "Reset", "重置");

        // ---- mobs tab ----
        put("mobs.presets", "Quick Presets", "快速预设");
        put("mobs.list", "Mob List", "怪物列表");
        put("mobs.add", "Add Mob", "添加怪物");
        put("mobs.remove", "Remove", "删除");
        put("mobs.id", "Mob ID", "怪物 ID");
        put("mobs.name", "Display name", "显示名称");
        put("mobs.texture", "Texture path (optional)", "贴图路径（可选）");
        put("mobs.needIdName", "Mob ID and name are required", "怪物 ID 和名称必填");

        // ---- API tab ----
        put("api.key", "Hypixel API Key", "Hypixel API Key");
        put("api.key.desc", "Required for bestiary kill counts from the API.", "从 API 获取 Bestiary 击杀数所必需。");
        put("api.uuid", "Player UUID", "玩家 UUID");
        put("api.uuid.desc", "Your Minecraft UUID.", "你的 Minecraft UUID。");
        put("api.save", "Save", "保存");
        put("api.keySaved", "API key saved", "API Key 已保存");
        put("api.uuidSaved", "UUID saved", "UUID 已保存");

        // ---- session tab ----
        put("session.mode", "Session Mode", "会话模式");
        put("session.mode.desc", "Enables separate kill tracking for this session.", "启用独立的本次会话击杀统计。");
        put("session.kills", "Kills", "击杀数");
        put("session.timeFarmed", "Time Farmed", "刷怪时长");
        put("session.cph", "Corleonite / Hour", "Corleonite / 小时");
        put("session.stats", "Corleonite Stats", "Corleonite 统计");
        put("session.noData", "No data", "无数据");
        put("session.enableHint", "Enable session mode to view stats", "启用会话模式以查看统计");
        put("session.reset", "Reset", "重置");
        put("session.killsReset", "Session kills reset", "会话击杀已重置");

        // ---- tools tab ----
        put("tools.finder", "Corleone Finder", "Corleone 结构扫描");
        put("tools.finder.desc", "Scans loaded chunks for Corleone boss structures and saves waypoints.", "扫描已加载的区块以寻找 Corleone 结构并保存路径点。");
        put("tools.scan", "Scan for Corleone Structures", "扫描 Corleone 结构");
        put("tools.noWaypoints", "No saved waypoints yet.", "尚未保存任何路径点。");
        put("tools.savedWaypoints", "Saved Waypoints:", "已保存的路径点：");
        put("tools.notInWorld", "Must be in a world", "必须处于世界中");
        put("tools.noneFound", "Neither structure was found in the loaded chunks.", "已加载区块中未找到任何结构。");

        // ---- HUD editor ----
        put("editor.title", "HUD Editor", "HUD 编辑器");
        put("editor.dragging", "Drag to move  -  release to save", "拖动移动 - 松开即保存");
        put("editor.hint", "Drag the outlined elements  -  ESC / Done when finished", "拖动带边框的元素 - 完成后按 ESC / Done");
        put("editor.done", "Done", "完成");
        put("editor.reset", "Reset", "重置");
        put("editor.kills", "Kills", "击杀");
        put("editor.timer", "Timer", "计时");

        // ---- counting modes ----
        put("mode.api", "Hypixel API", "Hypixel API");
        put("mode.api.desc", "Reads the Bestiary total from Hypixel. Nothing stored locally.", "从 Hypixel 读取 Bestiary 总击杀数，不在本地保存。");

        put("mode.local_persistent", "Local (saved)", "本地记录（永久保存）");
        put("mode.local_persistent.desc", "Counts locally and keeps the total across restarts.", "本地累计计数，重启游戏后仍然保留。");

        put("mode.local_session", "This client", "本次客户端");
        put("mode.local_session.desc", "Counts locally; resets when the game is restarted.", "本地计数，重启游戏后归零。");

        put("mode.local_per_world", "This world", "本次记录（换世界清除）");
        put("mode.local_per_world.desc", "Counts locally; resets when you change world or island.", "本地计数，切换世界或岛屿时清零。");

        put("session.modeLabel", "Counting Mode", "计数模式");
        put("session.modeHint", "Click to cycle through the four modes.", "点击可在四种模式间切换。");
        put("session.resetCounter", "Reset Counter", "重置计数");
        put("session.adjustHint", "Adjust manually if a drop was missed.", "如果漏记了掉落可手动调整。");

        // ---- language tab ----
        put("lang.title", "Mod Language", "模组语言");
        put("lang.desc", "Changes only this mod's interface. Proper nouns stay untranslated.", "仅修改本模组的界面语言。专有名词不翻译。");
        put("lang.note", "Minecraft's own language is separate.", "Minecraft 自身语言独立设置。");
    }

    public static Language current() {
        return current;
    }

    public static void setCurrent(Language language) {
        current = language;
        SkyblockCounterService.saveConfig();
    }

    public static Language next() {
        Language[] values = Language.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    /** Resolves a key in the active language, falling back to English then the key itself. */
    public static String tr(String key) {
        String[] entry = TABLE.get(key);
        if (entry == null) {
            return key;
        }
        String value = current == Language.CHINESE ? entry[1] : entry[0];
        return value != null && !value.isEmpty() ? value : entry[0];
    }

    /** Convenience overload with simple placeholder substitution of {0}, {1} ... */
    public static String tr(String key, Object... args) {
        String text = tr(key);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }
}
