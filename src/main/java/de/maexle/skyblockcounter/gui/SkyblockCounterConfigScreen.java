package de.maexle.skyblockcounter.gui;

import de.maexle.skyblockcounter.SkyblockCounterConfig;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.RespawnTimer;
import de.maexle.skyblockcounter.waypoint.WaypointManager;
import de.maexle.skyblockcounter.command.SkyblockCounterCommand;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SkyblockCounterConfigScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("skyblockcounter");

    private static final int CONTENT_TOP = 70;
    private static final int CONTENT_BOTTOM_PADDING = 28;
    private static final int OPTION_HEIGHT = 60;
    private static final int OPTION_GAP = 10;
    private static final long STATUS_DURATION_MS = 3000L;

    private final Screen parent;
    private Category selectedCategory = Category.GENERAL;
    private ToggleSwitch hudToggle;
    private ToggleSwitch percentageToggle;
    private ToggleSwitch timerToggle;
    private ToggleSwitch debugToggle;
    private ToggleSwitch clearWaypointsToggle;
    private ToggleSwitch timerSoundToggle;

    private EditBox apiKeyField;
    private EditBox uuidField;
    private EditBox mobIdField;
    private EditBox mobNameField;
    private EditBox mobTextureField;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int contentX;
    private int contentWidth;
    private int contentAreaTop;
    private int contentAreaBottom;
    private int scrollOffset;
    private int maxScroll;

    private String statusMessage = "";
    private long statusExpiry;
    private List<String> finderResults = List.of();

    public SkyblockCounterConfigScreen(Screen parent) {
        super(Component.literal("SkyCounter Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(720, Math.max(420, width - 28));
        panelHeight = Math.min(420, Math.max(320, height - 34));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        sidebarWidth = Math.min(134, panelWidth / 4);
        contentX = panelX + sidebarWidth + 22;
        contentWidth = panelWidth - sidebarWidth - 44;
        contentAreaTop = panelY + CONTENT_TOP;
        contentAreaBottom = panelY + panelHeight - CONTENT_BOTTOM_PADDING;

        hudToggle = new ToggleSwitch(SkyblockCounterService.isGuiVisible());
        percentageToggle = new ToggleSwitch(SkyblockCounterService.isShowCorleonitePercentage());
        timerToggle = new ToggleSwitch(SkyblockCounterService.isShowRespawnTimer());
        debugToggle = new ToggleSwitch(SkyblockCounterService.isDebugMode());
        clearWaypointsToggle = new ToggleSwitch(SkyblockCounterService.isClearWaypointsOnWorldChange());
        timerSoundToggle = new ToggleSwitch(SkyblockCounterService.isTimerSoundEnabled());

        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        switch (selectedCategory) {
            case HUD -> addHudWidgets();
            case MOBS -> addMobWidgets();
            case API -> addApiWidgets();
            case TOOLS -> addToolsWidgets();
            case SESSION -> addSessionWidgets();
            case LANGUAGE -> addLanguageWidgets();
            default -> {
            }
        }
    }

    private void addHudWidgets() {
        int y = contentAreaTop + 36 - scrollOffset;
        addRenderableWidget(new StyledButtonWidget(contentX, y + 118, 200, 26,
                Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("hud.edit")), button -> openHudEditor()));
        addRenderableWidget(new StyledButtonWidget(contentX, y + 152, 100, 22,
                Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("session.reset")), button -> resetPositions()));
    }

    /** Opens the full-screen in-game editor where the HUD is dragged over the live world. */
    private void openHudEditor() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new HudEditorScreen(this));
        }
    }

    private void addLanguageWidgets() {
        int y = contentAreaTop + 36 - scrollOffset;
        int bx = contentX;
        for (de.maexle.skyblockcounter.i18n.ModLang.Language lang
                : de.maexle.skyblockcounter.i18n.ModLang.Language.values()) {
            boolean active = SkyblockCounterService.getModLanguage() == lang;
            addRenderableWidget(new StyledButtonWidget(bx, y + 4, 120, 24,
                    Component.literal(lang.displayName() + (active ? "  ✓" : "")),
                    button -> changeLanguage(lang)));
            bx += 128;
        }
    }

    private void changeLanguage(de.maexle.skyblockcounter.i18n.ModLang.Language language) {
        SkyblockCounterService.setModLanguage(language);
        rebuildWidgets();
        setStatus(language.displayName());
    }

    private void drawLanguageContent(GuiGraphicsExtractor context, int mouseX, int mouseY, int y) {
        // The category header above already shows the title and description, so this tab
        // only needs the language buttons (added in addLanguageWidgets).
    }

    private void addMobWidgets() {
        int formY = contentAreaTop + 36 - scrollOffset + getMobListHeight() + 93;
        mobIdField = createTextField(contentX + 7, formY, contentWidth - 16, de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.id"));
        mobNameField = createTextField(contentX + 7, formY + 28, contentWidth - 16, de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.name"));
        mobTextureField = createTextField(contentX + 7, formY + 56, contentWidth - 16, de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.texture"));
        addRenderableWidget(mobIdField);
        addRenderableWidget(mobNameField);
        addRenderableWidget(mobTextureField);
        addRenderableWidget(new StyledButtonWidget(contentX, formY + 84, 80, 22, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.add")), button -> addMobFromForm()));
    }

    private void addApiWidgets() {
        int y = contentAreaTop + 36 - scrollOffset;
        apiKeyField = createTextField(contentX + 17, y + 52, contentWidth - 90, "API Key");
        apiKeyField.setMaxLength(128);
        apiKeyField.setValue(SkyblockCounterService.getConfig().getAPI_KEY() != null ? "*".repeat(SkyblockCounterService.getConfig().getAPI_KEY().length()) : "");
        addRenderableWidget(apiKeyField);
        addRenderableWidget(new StyledButtonWidget(contentX + contentWidth - 78, y + 45, 70, 22, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("api.save")), button -> saveApiKey()));

        uuidField = createTextField(contentX + 17, y + 134, contentWidth - 90, "UUID");
        uuidField.setMaxLength(64);
        uuidField.setValue(SkyblockCounterService.getConfig().getundashedUuid());
        addRenderableWidget(uuidField);
        addRenderableWidget(new StyledButtonWidget(contentX + contentWidth - 78, y + 126, 70, 22, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("api.save")), button -> saveUuid()));
    }

    private void addToolsWidgets() {
        int y = contentAreaTop + 36 - scrollOffset + 36;
        addRenderableWidget(new StyledButtonWidget(contentX, y, 220, 24, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("tools.scan")), button -> runCorleoneFinder()));
    }

    /** Layout constants shared by the session widgets and their rendering code. */
    private static final int SESSION_MODE_CARD_STEP = 38;
    private static final int SESSION_STAT_STEP = 34;

    private int sessionContentTop() {
        return contentAreaTop + 36 - scrollOffset;
    }

    /** Y where the stats block starts, right after the four mode cards. */
    private int sessionStatsTop() {
        return sessionContentTop() + 16
                + SESSION_MODE_CARD_STEP * de.maexle.skyblockcounter.CountingMode.values().length
                + 8;
    }

    private void addSessionWidgets() {
        // Placed below the stats block so the buttons never cover the numbers.
        int statsY = sessionStatsTop();
        int controlsY = statsY + SESSION_STAT_STEP * 4 + 10;
        boolean local = SkyblockCounterService.isLocalCounting();

        addRenderableWidget(new StyledButtonWidget(contentX + 96, controlsY, 32, 20,
                Component.literal("-5"), b -> adjustSession(-5), local));
        addRenderableWidget(new StyledButtonWidget(contentX + 132, controlsY, 32, 20,
                Component.literal("-1"), b -> adjustSession(-1), local));
        addRenderableWidget(new StyledButtonWidget(contentX + 168, controlsY, 32, 20,
                Component.literal("+1"), b -> adjustSession(1), local));
        addRenderableWidget(new StyledButtonWidget(contentX + 204, controlsY, 32, 20,
                Component.literal("+5"), b -> adjustSession(5), local));

        int dropY = controlsY + 26;
        addRenderableWidget(new StyledButtonWidget(contentX + 132, dropY, 32, 20,
                Component.literal("-1"), b -> adjustCorleonite(-1), local));
        addRenderableWidget(new StyledButtonWidget(contentX + 168, dropY, 32, 20,
                Component.literal("+1"), b -> adjustCorleonite(1), local));

        addRenderableWidget(new StyledButtonWidget(contentX + contentWidth - 90, controlsY, 90, 22,
                Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("session.reset")),
                b -> resetSession(), local));
    }

    private EditBox createTextField(int x, int y, int width, String placeholder) {
        EditBox field = new EditBox(this.font, x, y, width, 20, Component.literal(placeholder));
        field.setHint(Component.literal(placeholder));
        field.setBordered(false);
        return field;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(0, 0, width, height, ConfigPanelHelper.BACKDROP);
        drawPanel(guiGraphics);
        drawHeader(guiGraphics);
        drawCategories(guiGraphics, mouseX, mouseY);

        guiGraphics.enableScissor(contentX - 4, contentAreaTop - 4, contentX + contentWidth + 4, contentAreaBottom);
        drawContent(guiGraphics, mouseX, mouseY, delta);

        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.disableScissor();

        renderStatus(guiGraphics);
        guiGraphics.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("footer.esc")), panelX + panelWidth - 94, panelY + panelHeight - 16, 999, ConfigPanelHelper.TEXT_SECONDARY);
    }

    private void drawPanel(GuiGraphicsExtractor context) {
        ConfigPanelHelper.drawRoundedRect(context, panelX + 3, panelY + 4, panelWidth, panelHeight, ConfigPanelHelper.PANEL_SHADOW);
        ConfigPanelHelper.drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, ConfigPanelHelper.PANEL_BORDER);
        ConfigPanelHelper.drawRoundedRect(context, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, ConfigPanelHelper.PANEL);
        context.fill(panelX + 1, panelY + 43, panelX + sidebarWidth, panelY + panelHeight - 1, ConfigPanelHelper.SIDEBAR);
        context.fill(panelX + sidebarWidth, panelY + 44, panelX + sidebarWidth + 1, panelY + panelHeight - 1, ConfigPanelHelper.PANEL_BORDER);
    }

    private void drawHeader(GuiGraphicsExtractor context) {
        context.fill(panelX + 1, panelY + 42, panelX + panelWidth - 1, panelY + 43, ConfigPanelHelper.PANEL_BORDER);
        context.textWithBackdrop(this.font, Component.literal("Sky"), panelX + 17, panelY + 16, 50, ConfigPanelHelper.TEXT_PRIMARY);
        int skyWidth = this.font.width("Sky");
        context.textWithBackdrop(this.font, Component.literal("Counter"), panelX + 17 + skyWidth, panelY + 16, 80, ConfigPanelHelper.ACCENT);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("header.config")), panelX + panelWidth - 99, panelY + 17, 120, ConfigPanelHelper.TEXT_SECONDARY);
    }

    private void drawCategories(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("header.categories")), panelX + 14, panelY + 57, sidebarWidth, ConfigPanelHelper.TEXT_SECONDARY);
        int y = panelY + 75;
        for (Category category : Category.values()) {
            boolean selected = selectedCategory == category;
            boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, panelX + 7, y, sidebarWidth - 14, 23);
            if (selected || hovered) {
                ConfigPanelHelper.drawRoundedRect(context, panelX + 7, y, sidebarWidth - 14, 23, selected ? ConfigPanelHelper.ACCENT_DARK : 0xFF302E3C);
                if (selected) {
                    context.fill(panelX + 7, y + 4, panelX + 9, y + 19, ConfigPanelHelper.ACCENT);
                }
            }
            context.textWithBackdrop(this.font, Component.literal(category.label()), panelX + 16, y + 8, sidebarWidth - 20, selected ? ConfigPanelHelper.TEXT_PRIMARY : ConfigPanelHelper.TEXT_SECONDARY);
            y += 27;
        }
    }

    private void drawContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int rowY = panelY + CONTENT_TOP - scrollOffset;
        context.textWithBackdrop(this.font, Component.literal(selectedCategory.title()), contentX, rowY, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        context.textWithBackdrop(this.font, Component.literal(selectedCategory.description()), contentX, rowY + 15, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);

        switch (selectedCategory) {
            case GENERAL -> drawGeneralContent(context, mouseX, mouseY, delta, rowY + 36);
            case HUD -> drawHudContent(context, mouseX, mouseY, delta, rowY + 36);
            case MOBS -> drawMobsContent(context, mouseX, mouseY, delta, rowY + 36);
            case SESSION -> drawSessionContent(context, mouseX, mouseY, delta, rowY + 36);
            case API -> drawApiContent(context, mouseX, mouseY, rowY + 36);
            case TOOLS -> drawToolsContent(context, mouseX, mouseY, rowY + 36);
            case LANGUAGE -> drawLanguageContent(context, mouseX, mouseY, rowY + 36);
        }

        updateMaxScroll();
    }

    private void drawGeneralContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, int y) {
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y, contentWidth,
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.showHud"), de.maexle.skyblockcounter.i18n.ModLang.tr("opt.showHud.desc"),
                SkyblockCounterService.isGuiVisible(), hudToggle);
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y + OPTION_HEIGHT + OPTION_GAP, contentWidth,
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.percentage"), de.maexle.skyblockcounter.i18n.ModLang.tr("opt.percentage.desc"),
                SkyblockCounterService.isShowCorleonitePercentage(), percentageToggle);
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y + (OPTION_HEIGHT + OPTION_GAP) * 2, contentWidth,
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.timer"), de.maexle.skyblockcounter.i18n.ModLang.tr("opt.timer.desc"),
                SkyblockCounterService.isShowRespawnTimer(), timerToggle);
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y + (OPTION_HEIGHT + OPTION_GAP) * 3, contentWidth,
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.debug"), de.maexle.skyblockcounter.i18n.ModLang.tr("opt.debug.desc"),
                SkyblockCounterService.isDebugMode(), debugToggle);
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y + (OPTION_HEIGHT + OPTION_GAP) * 4, contentWidth,
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.clearWaypoints"),
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.clearWaypoints.desc"),
                SkyblockCounterService.isClearWaypointsOnWorldChange(), clearWaypointsToggle);
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y + (OPTION_HEIGHT + OPTION_GAP) * 5, contentWidth,
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.timerSound"),
                de.maexle.skyblockcounter.i18n.ModLang.tr("opt.timerSound.desc"),
                SkyblockCounterService.isTimerSoundEnabled(), timerSoundToggle);
    }

    private void drawHudContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, int y) {
        ConfigPanelHelper.drawOptionCard(context, contentX, y, contentWidth, 106,
                ConfigPanelHelper.isInside(mouseX, mouseY, contentX, y, contentWidth, 106));
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("cat.hud.title")),
                contentX + 14, y + 12, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        context.textWithBackdrop(this.font,
                Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("hud.explain1")),
                contentX + 14, y + 30, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
        context.textWithBackdrop(this.font,
                Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("hud.explain2")),
                contentX + 14, y + 42, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
        context.textWithBackdrop(this.font,
                Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("hud.explain3")),
                contentX + 14, y + 56, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);

        int currentY = y + 76;
        context.textWithBackdrop(this.font,
                Component.literal("Kills: X=" + SkyblockCounterService.getHudX() + ", Y=" + SkyblockCounterService.getHudY()
                        + "    Timer: X=" + SkyblockCounterService.getTimerX() + ", Y=" + SkyblockCounterService.getTimerY()),
                contentX + 14, currentY, contentWidth, ConfigPanelHelper.ACCENT_DARK);
    }

    private void drawMobsContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, int y) {
        int presetY = y;
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.presets")), contentX, presetY, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        int buttonWidth = (contentWidth - 16) / 3;
        drawPresetButton(context, mouseX, mouseY, contentX, presetY + 18, buttonWidth, 28, "Treasure Hoarder", "treasure_hoarder_70");
        drawPresetButton(context, mouseX, mouseY, contentX + buttonWidth + 8, presetY + 18, buttonWidth, 28, "Corleone", "team_treasurite_corleone_200");
        drawPresetButton(context, mouseX, mouseY, contentX + (buttonWidth + 8) * 2, presetY + 18, buttonWidth, 28, "Zealot", "zealot_enderman_55");

        int listY = presetY + 58;
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.list")), contentX, listY, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        List<SkyblockCounterConfig.MobEntry> mobs = SkyblockCounterActions.listMobs();
        int rowY = listY + 16;
        String currentMobId = SkyblockCounterService.getConfig().getLastMobId();
        for (SkyblockCounterConfig.MobEntry entry : mobs) {
            boolean selected = entry.id.equals(currentMobId);
            boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, contentX, rowY, contentWidth, 24);
            ConfigPanelHelper.drawOptionCard(context, contentX, rowY, contentWidth, 24, hovered || selected);
            if (selected) {
                context.fill(contentX, rowY + 4, contentX + 2, rowY + 20, ConfigPanelHelper.ACCENT);
            }
            context.textWithBackdrop(this.font, Component.literal(entry.name), contentX + 10, rowY + 8, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
            context.textWithBackdrop(this.font, Component.literal(entry.id), contentX + 10 + this.font.width(entry.name) + 8, rowY + 8, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
            ConfigPanelHelper.drawButton(context, contentX + contentWidth - 52, rowY + 4, 44, 16,
                    ConfigPanelHelper.isInside(mouseX, mouseY, contentX + contentWidth - 52, rowY + 4, 44, 16), false);
            context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.remove")), contentX + contentWidth - 48 , rowY + 8, 44, 0xFFFF8888);
            rowY += 28;
        }

        int formY = rowY + 12;
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.add")), contentX, formY, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        drawFieldBackground(context, contentX, formY + 16, contentWidth - 16, 20);
        drawFieldBackground(context, contentX, formY + 44, contentWidth - 16, 20);
        drawFieldBackground(context, contentX, formY + 72, contentWidth - 16, 20);
    }

    private void drawSessionContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, int y) {
        de.maexle.skyblockcounter.CountingMode current = SkyblockCounterService.getCountingMode();

        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("session.modeLabel")),
                contentX, y, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);

        // Four selectable mode cards, one per row.
        int cardY = y + 16;
        for (de.maexle.skyblockcounter.CountingMode mode : de.maexle.skyblockcounter.CountingMode.values()) {
            boolean selected = mode == current;
            boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, contentX, cardY, contentWidth, 34);
            ConfigPanelHelper.drawOptionCard(context, contentX, cardY, contentWidth, 34, hovered || selected);
            if (selected) {
                context.fill(contentX, cardY + 4, contentX + 3, cardY + 30, ConfigPanelHelper.ACCENT);
            }
            context.textWithBackdrop(this.font, Component.literal(mode.displayName()),
                    contentX + 12, cardY + 6, contentWidth - 24,
                    selected ? ConfigPanelHelper.TEXT_PRIMARY : ConfigPanelHelper.TEXT_SECONDARY);
            context.textWithBackdrop(this.font, Component.literal(mode.description()),
                    contentX + 12, cardY + 19, contentWidth - 24, ConfigPanelHelper.TEXT_SECONDARY);
            cardY += 38;
        }

        int statsY = cardY + 8;
        if (current.isLocal()) {
            drawStatRow(context, statsY, de.maexle.skyblockcounter.i18n.ModLang.tr("session.kills"), String.valueOf(SkyblockCounterService.getSessionKills()));
            drawStatRow(context, statsY + 34, de.maexle.skyblockcounter.i18n.ModLang.tr("session.timeFarmed"), SkyblockCounterActions.formatTimeFarmed().orElse("--:--:--"));
            drawStatRow(context, statsY + 68, de.maexle.skyblockcounter.i18n.ModLang.tr("session.cph"), SkyblockCounterActions.formatCorleonitePerHour().orElse("0.00"));
            drawStatRow(context, statsY + 102, de.maexle.skyblockcounter.i18n.ModLang.tr("session.stats"), SkyblockCounterActions.getCorleoniteStats().orElse(de.maexle.skyblockcounter.i18n.ModLang.tr("session.noData")));
        } else {
            ConfigPanelHelper.drawStatCard(context, contentX, statsY, contentWidth, 48);
            context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("session.enableHint")),
                    contentX + 7, statsY + 20, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
        }

        // Row labels for the adjustment buttons added in addSessionWidgets().
        int controlsY = statsY + SESSION_STAT_STEP * 4 + 10;
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("session.kills")),
                contentX + 4, controlsY + 6, 88, ConfigPanelHelper.TEXT_SECONDARY);
        context.textWithBackdrop(this.font, Component.literal("Corleonite"),
                contentX + 4, controlsY + 32, 120, ConfigPanelHelper.TEXT_SECONDARY);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("session.adjustHint")),
                contentX + 4, controlsY + 58, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
    }

    private void drawApiContent(GuiGraphicsExtractor context, int mouseX, int mouseY, int y) {
        ConfigPanelHelper.drawOptionCard(context, contentX, y, contentWidth, 72, false);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("api.key")), contentX + 14, y + 13, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("api.key.desc")), contentX + 14, y + 30, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
        drawFieldBackground(context, contentX + 12, y + 46, contentWidth - 100, 20);

        ConfigPanelHelper.drawOptionCard(context, contentX, y + 82, contentWidth, 72, false);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("api.uuid")), contentX + 14, y + 95, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("api.uuid.desc")), contentX + 14, y + 112, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
        drawFieldBackground(context, contentX + 12, y + 128, contentWidth - 100, 20);
    }

    private void drawToolsContent(GuiGraphicsExtractor context, int mouseX, int mouseY, int y) {
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("tools.finder")), contentX, y + 4, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
        context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("tools.finder.desc")), contentX, y + 18, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);

        int resultY = y + 68;
        List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();

        if (waypoints.isEmpty()) {
            context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("tools.noWaypoints")), contentX, resultY, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);
        } else {
            context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("tools.savedWaypoints")), contentX, resultY, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
            int rowY = resultY + 16;
            for (WaypointManager.Waypoint wp : waypoints) {
                boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, contentX, rowY, contentWidth, 24);
                ConfigPanelHelper.drawOptionCard(context, contentX, rowY, contentWidth, 24, hovered);

                context.textWithBackdrop(this.font, Component.literal(wp.label()), contentX + 10, rowY + 8, contentWidth, ConfigPanelHelper.TEXT_PRIMARY);
                String coords = "X: " + wp.pos().getX() + ", Y: " + wp.pos().getY() + ", Z: " + wp.pos().getZ();
                context.textWithBackdrop(this.font, Component.literal(coords), contentX + 15 + this.font.width(wp.label()), rowY + 8, contentWidth, ConfigPanelHelper.TEXT_SECONDARY);

                ConfigPanelHelper.drawButton(context, contentX + contentWidth - 52, rowY + 4, 44, 16,
                        ConfigPanelHelper.isInside(mouseX, mouseY, contentX + contentWidth - 52, rowY + 4, 44, 16), false);
                context.textWithBackdrop(this.font, Component.literal(de.maexle.skyblockcounter.i18n.ModLang.tr("mobs.remove")), contentX + contentWidth - 48, rowY + 8, 44, 0xFFFF8888);

                rowY += 28;
            }
        }
    }

    private void drawStatRow(GuiGraphicsExtractor context, int y, String label, String value) {
        int half = (contentWidth - 8) / 2;
        ConfigPanelHelper.drawStatCard(context, contentX, y, half, 28);
        ConfigPanelHelper.drawStatCard(context, contentX + half + 8, y, half, 28);
        context.textWithBackdrop(this.font, Component.literal(label), contentX + 10, y + 10, half, ConfigPanelHelper.TEXT_SECONDARY);
        context.textWithBackdrop(this.font, Component.literal(value), contentX + half + 18, y + 10, half, ConfigPanelHelper.TEXT_PRIMARY);
    }

    private void drawPresetButton(GuiGraphicsExtractor context, int mouseX, int mouseY, int x, int y, int width, int height, String label, String mobId) {
        boolean active = mobId.equals(SkyblockCounterService.getConfig().getLastMobId());
        boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, x, y, width, height);
        ConfigPanelHelper.drawButton(context, x, y, width, height, hovered, active);
        context.textWithBackdrop(this.font, Component.literal(label), x + 7, y + height / 2 - 4, width, ConfigPanelHelper.TEXT_PRIMARY);
    }

    private void drawToggleOption(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, int x, int y, int optionWidth,
                                  String title, String description, boolean enabled, ToggleSwitch toggle) {
        boolean rowHovered = ConfigPanelHelper.isInside(mouseX, mouseY, x, y, optionWidth, OPTION_HEIGHT);
        ConfigPanelHelper.drawOptionCard(context, x, y, optionWidth, OPTION_HEIGHT, rowHovered);

        context.textWithBackdrop(this.font, Component.literal(title), x + 14, y + 13, optionWidth, ConfigPanelHelper.TEXT_PRIMARY);
        context.textWithBackdrop(this.font, Component.literal(description), x + 14, y + 30, optionWidth, ConfigPanelHelper.TEXT_SECONDARY);

        int toggleX = x + optionWidth - ToggleSwitch.WIDTH - 14;
        int toggleY = y + 20;
        toggle.render(context, toggleX, toggleY, enabled, ToggleSwitch.isHovered(mouseX, mouseY, toggleX, toggleY), delta);
    }

    private void drawFieldBackground(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        ConfigPanelHelper.drawRoundedRect(context, x, y, width, height, ConfigPanelHelper.FIELD_BG);
        ConfigPanelHelper.drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, ConfigPanelHelper.CARD_INNER);
    }

    private void renderStatus(GuiGraphicsExtractor context) {
        if (statusMessage.isEmpty() || System.currentTimeMillis() > statusExpiry) {
            return;
        }
        context.textWithBackdrop(this.font, Component.literal(statusMessage), panelX + 16, panelY + panelHeight - 16, 300, ConfigPanelHelper.ACCENT);
    }

    private int getMobListHeight() {
        return SkyblockCounterActions.listMobs().size() * 28 + 16;
    }

    private void updateMaxScroll() {
        int contentHeight = switch (selectedCategory) {
            case GENERAL -> 500;
            case HUD -> 260;
            case MOBS -> 58 + getMobListHeight() + 220;
            case SESSION -> SkyblockCounterService.isLocalCounting() ? 560 : 480;
            case API -> 160;
            case TOOLS -> 120 + (WaypointManager.getWaypoints().size() * 28);
            case LANGUAGE -> 160;
        };
        int visibleHeight = contentAreaBottom - contentAreaTop;
        maxScroll = Math.max(0, contentHeight - visibleHeight + 20);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        // Minecraft 26.3 uses SDL input ids, where the primary (left) button is 1.
        if (click.button() != 1) {
            return super.mouseClicked(click, doubleClick);
        }

        double mouseX = click.x();
        double mouseY = click.y();

        if (handleCategoryClick(mouseX, mouseY)) {
            return true;
        }

        int rowY = panelY + CONTENT_TOP - scrollOffset;

        if (selectedCategory == Category.GENERAL) {
            if (handleGeneralToggleClick(mouseX, mouseY, rowY + 36)) {
                return true;
            }
        } else if (selectedCategory == Category.SESSION) {
            if (handleSessionToggleClick(mouseX, mouseY, rowY + 36)) {
                return true;
            }
        } else if (selectedCategory == Category.MOBS) {
            if (handleMobsClick(mouseX, mouseY, rowY + 36)) {
                return true;
            }
        } else if (selectedCategory == Category.TOOLS) {
            if (handleToolsClick(mouseX, mouseY, rowY + 36)) {
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    private void resetPositions() {
        SkyblockCounterService.setHudPosition(10, 10);
        SkyblockCounterService.setTimerPosition(10, 40);
        setStatus("HUD positions reset");
    }

    private boolean handleCategoryClick(double mouseX, double mouseY) {
        int categoryY = panelY + 75;
        for (Category category : Category.values()) {
            if (ConfigPanelHelper.isInside(mouseX, mouseY, panelX + 7, categoryY, sidebarWidth - 14, 23)) {
                if (selectedCategory != category) {
                    selectedCategory = category;
                    scrollOffset = 0;
                    rebuildWidgets();
                }
                return true;
            }
            categoryY += 27;
        }
        return false;
    }

    private boolean handleGeneralToggleClick(double mouseX, double mouseY, int firstOptionY) {
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY, contentWidth)) {
            SkyblockCounterService.setGuiVisible(!SkyblockCounterService.isGuiVisible());
            setStatus("HUD " + (SkyblockCounterService.isGuiVisible() ? "enabled" : "disabled"));
            return true;
        }
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY + OPTION_HEIGHT + OPTION_GAP, contentWidth)) {
            SkyblockCounterService.setShowCorleonitePercentage(!SkyblockCounterService.isShowCorleonitePercentage());
            setStatus("Corleonite percentage " + (SkyblockCounterService.isShowCorleonitePercentage() ? "enabled" : "disabled"));
            return true;
        }
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY + (OPTION_HEIGHT + OPTION_GAP) * 2, contentWidth)) {
            SkyblockCounterService.setShowRespawnTimer(!SkyblockCounterService.isShowRespawnTimer());
            setStatus("Respawn timer " + (SkyblockCounterService.isShowRespawnTimer() ? "enabled" : "disabled"));
            return true;
        }
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY + (OPTION_HEIGHT + OPTION_GAP) * 3, contentWidth)) {
            SkyblockCounterService.toggleDebugMode();
            setStatus("Debug mode " + (SkyblockCounterService.isDebugMode() ? "enabled" : "disabled"));
            return true;
        }
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY + (OPTION_HEIGHT + OPTION_GAP) * 4, contentWidth)) {
            SkyblockCounterService.setClearWaypointsOnWorldChange(
                    !SkyblockCounterService.isClearWaypointsOnWorldChange());
            setStatus("Clear waypoints on world change "
                    + (SkyblockCounterService.isClearWaypointsOnWorldChange() ? "enabled" : "disabled"));
            return true;
        }
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY + (OPTION_HEIGHT + OPTION_GAP) * 5, contentWidth)) {
            SkyblockCounterService.setTimerSoundEnabled(!SkyblockCounterService.isTimerSoundEnabled());
            setStatus("Respawn timer sound "
                    + (SkyblockCounterService.isTimerSoundEnabled() ? "enabled" : "disabled"));
            return true;
        }
        return false;
    }

    /** Maps a click on the mode cards to the corresponding mode. */
    private void selectCountingModeAt(double mouseY) {
        int rowY = panelY + CONTENT_TOP - scrollOffset;
        int cardY = rowY + 36 + 16;

        for (de.maexle.skyblockcounter.CountingMode mode : de.maexle.skyblockcounter.CountingMode.values()) {
            if (mouseY >= cardY && mouseY < cardY + 34) {
                SkyblockCounterActions.setCountingMode(mode);
                setStatus(mode.displayName());
                rebuildWidgets();
                return;
            }
            cardY += 38;
        }
    }

    private boolean handleSessionToggleClick(double mouseX, double mouseY, int firstOptionY) {
        // Clicking anywhere in the mode-card column selects that mode.
        if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX, firstOptionY + 16,
                contentWidth, 38 * de.maexle.skyblockcounter.CountingMode.values().length)) {
            selectCountingModeAt(mouseY);
            return true;
        }
        return false;
    }

    private boolean handleMobsClick(double mouseX, double mouseY, int y) {
        int buttonWidth = (contentWidth - 16) / 3;
        if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX, y + 18, buttonWidth, 28)) {
            SkyblockCounterActions.switchToTreasureHoarder();
            setStatus("Switched to Treasure Hoarder");
            sendChat("Switched to Treasure Hoarder");
            return true;
        }
        if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX + buttonWidth + 8, y + 18, buttonWidth, 28)) {
            SkyblockCounterActions.switchToCorleone();
            setStatus("Switched to Corleone");
            sendChat("Switched to Corleone");
            return true;
        }
        if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX + (buttonWidth + 8) * 2, y + 18, buttonWidth, 28)) {
            SkyblockCounterActions.switchToZealot();
            setStatus("Switched to Zealot");
            sendChat("Switched to Zealot Lv.55");
            return true;
        }

        int rowY = y + 74;
        for (SkyblockCounterConfig.MobEntry entry : SkyblockCounterActions.listMobs()) {
            if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX + contentWidth - 52, rowY + 4, 44, 16)) {
                setStatus(SkyblockCounterActions.removeMob(entry.id));
                rebuildWidgets();
                return true;
            }
            if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX, rowY, contentWidth - 56, 24)) {
                SkyblockCounterActions.MobSwitchResult result = SkyblockCounterActions.switchToMob(entry.id);
                setStatus(result.message());
                sendChat(result.message());
                return true;
            }
            rowY += 28;
        }
        return false;
    }

    private boolean handleToolsClick(double mouseX, double mouseY, int y) {
        int resultY = y + 68;
        int rowY = resultY + 16;
        List<WaypointManager.Waypoint> waypoints = new ArrayList<>(WaypointManager.getWaypoints());

        for (WaypointManager.Waypoint wp : waypoints) {
            if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX + contentWidth - 52, rowY + 4, 44, 16)) {
                waypoints.remove(wp);
                SkyblockCounterActions.removeWaypoint(wp.label());
                WaypointManager.setWaypoints(waypoints);
                setStatus("Waypoint removed");
                rebuildWidgets();
                return true;
            }
            rowY += 28;
        }
        return false;
    }

    private boolean isToggleClicked(double mouseX, double mouseY, int optionX, int optionY, int optionWidth) {
        int toggleX = optionX + optionWidth - ToggleSwitch.WIDTH - 14;
        return ToggleSwitch.isHovered(mouseX, mouseY, toggleX, optionY + 20);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ConfigPanelHelper.isInside(mouseX, mouseY, contentX, contentAreaTop, contentWidth, contentAreaBottom - contentAreaTop)) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 16));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void addMobFromForm() {
        String id = mobIdField.getValue().trim();
        String name = mobNameField.getValue().trim();
        String texture = mobTextureField.getValue().trim();
        if (id.isEmpty() || name.isEmpty()) {
            setStatus("Mob ID and name are required");
            return;
        }
        SkyblockCounterActions.MobAddResult result = SkyblockCounterActions.addMob(id, name, texture);
        setStatus(result.message());
        mobIdField.setValue("");
        mobNameField.setValue("");
        mobTextureField.setValue("");
        rebuildWidgets();
    }

    private void saveApiKey() {
        SkyblockCounterActions.setApiKey(apiKeyField.getValue().trim());
        setStatus("API key saved");
        sendChat("API key was set");
    }

    private void saveUuid() {
        SkyblockCounterActions.setUuid(uuidField.getValue().trim());
        setStatus("UUID saved");
        sendChat("UUID was set");
    }

    public void runCorleoneFinder() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            finderResults = List.of("Must be in a world");
            setStatus("Must be in a world");
            System.out.println("CorleoneFinder aborted: level or player is null!");
            return;
        }

        System.out.println("Triggering CorleoneFinder from GUI...");
        SkyblockCounterActions.CorleoneFinderResult result = SkyblockCounterActions.findCorleoneStructures(mc.level, mc.player.blockPosition());
        finderResults = new ArrayList<>(result.messages());

        List<WaypointManager.Waypoint> currentWaypoints = new ArrayList<>(WaypointManager.getWaypoints());
        WaypointManager.setWaypoints(currentWaypoints);

        setStatus(result.messages().isEmpty() ? "No structures found" : result.messages().get(0));
        for (String message : result.messages()) {
            sendChat(message);
        }
        rebuildWidgets();
    }

    /** Manual Corleonite adjustment, for when a [Sacks] message was missed. */
    private void adjustCorleonite(int amount) {
        SkyblockCounterService.adjustSackCorleonite(amount);
        setStatus("Corleonite: " + SkyblockCounterService.getSackCorleoniteTotal());
    }

    private void adjustSession(int amount) {
        if (amount > 0) {
            SkyblockCounterActions.addSessionKills(amount);
        } else {
            SkyblockCounterActions.removeSessionKills(-amount);
        }
        setStatus("Session kills: " + SkyblockCounterService.getSessionKills());
    }

    private void resetSession() {
        SkyblockCounterActions.resetSession();
        setStatus("Session kills reset");
    }

    private void setStatus(String message) {
        statusMessage = message;
        statusExpiry = System.currentTimeMillis() + STATUS_DURATION_MS;
    }

    private void sendChat(String message) {
        SkyblockCounterCommand.sendPlayerMessage(message);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Category {
        GENERAL("general"),
        HUD("hud"),
        MOBS("mobs"),
        API("api"),
        SESSION("session"),
        TOOLS("tools"),
        LANGUAGE("language");

        private final String key;

        Category(String key) {
            this.key = key;
        }

        String label() {
            return de.maexle.skyblockcounter.i18n.ModLang.tr("cat." + key + ".label");
        }

        String title() {
            return de.maexle.skyblockcounter.i18n.ModLang.tr("cat." + key + ".title");
        }

        String description() {
            return de.maexle.skyblockcounter.i18n.ModLang.tr("cat." + key + ".desc");
        }
    }
}