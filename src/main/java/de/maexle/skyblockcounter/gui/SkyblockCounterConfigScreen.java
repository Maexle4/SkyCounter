package de.maexle.skyblockcounter.gui;

import de.maexle.skyblockcounter.SkyblockCounterConfig;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.waypoint.WaypointManager;
import de.maexle.skyblockcounter.command.SkyblockCounterCommand;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
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
    private ToggleSwitch sessionToggle;

    private TextFieldWidget hudXField;
    private TextFieldWidget hudYField;
    private TextFieldWidget apiKeyField;
    private TextFieldWidget uuidField;
    private TextFieldWidget mobIdField;
    private TextFieldWidget mobNameField;
    private TextFieldWidget mobTextureField;

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
        super(Text.literal("SkyCounter Configuration"));
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
        sessionToggle = new ToggleSwitch(SkyblockCounterService.isShowSessionKills());

        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        switch (selectedCategory) {
            case HUD -> addHudWidgets();
            case MOBS -> addMobWidgets();
            case API -> addApiWidgets();
            case TOOLS -> addToolsWidgets();
            case SESSION -> addSessionWidgets();
            default -> {
            }
        }
    }

    private void addHudWidgets() {
        int y = contentAreaTop + 36 - scrollOffset;
        hudXField = createTextField(contentX + 17, y + 65, 80, "X");
        hudXField.setText(String.valueOf(SkyblockCounterService.getHudX()));
        hudXField.setChangedListener(s -> {
        });
        addDrawableChild(hudXField);

        hudYField = createTextField(contentX + 109, y + 65, 80, "Y");
        hudYField.setText(String.valueOf(SkyblockCounterService.getHudY()));
        addDrawableChild(hudYField);

        addDrawableChild(new StyledButtonWidget(contentX + 194, y + 56, 70, 22, Text.literal("Apply"), button -> applyHudPosition()));
    }

    private void addMobWidgets() {
        int formY = contentAreaTop + 36 - scrollOffset + getMobListHeight() + 93;
        mobIdField = createTextField(contentX + 7, formY, contentWidth - 16, "Mob ID");
        mobNameField = createTextField(contentX + 7, formY + 28, contentWidth - 16, "Display name");
        mobTextureField = createTextField(contentX + 7, formY + 56, contentWidth - 16, "Texture path (optional)");
        addDrawableChild(mobIdField);
        addDrawableChild(mobNameField);
        addDrawableChild(mobTextureField);
        addDrawableChild(new StyledButtonWidget(contentX, formY + 84, 80, 22, Text.literal("Add Mob"), button -> addMobFromForm()));
    }

    private void addApiWidgets() {
        int y = contentAreaTop + 36 - scrollOffset;
        apiKeyField = createTextField(contentX + 17, y + 52, contentWidth - 90, "API Key");
        apiKeyField.setMaxLength(128);
        apiKeyField.setText(SkyblockCounterService.getConfig().getAPI_KEY() != null ? "*".repeat(SkyblockCounterService.getConfig().getAPI_KEY().length()) : "");
        addDrawableChild(apiKeyField);
        addDrawableChild(new StyledButtonWidget(contentX + contentWidth - 78, y + 45, 70, 22, Text.literal("Save"), button -> saveApiKey()));

        uuidField = createTextField(contentX + 17, y + 134, contentWidth - 90, "UUID");
        uuidField.setMaxLength(64);
        uuidField.setText(SkyblockCounterService.getConfig().getundashedUuid());
        addDrawableChild(uuidField);
        addDrawableChild(new StyledButtonWidget(contentX + contentWidth - 78, y + 126, 70, 22, Text.literal("Save"), button -> saveUuid()));
    }

    private void addToolsWidgets() {
        int y = contentAreaTop + 36 - scrollOffset + 36;
        addDrawableChild(new StyledButtonWidget(contentX, y, 220, 24, Text.literal("Scan for Corleone Structures"), button -> runCorleoneFinder()));
    }

    private void addSessionWidgets() {
        int y = contentAreaTop + 36 - scrollOffset + 218;
        addDrawableChild(new StyledButtonWidget(contentX, y, 36, 22, Text.literal("-5"), button -> adjustSession(-5)));
        addDrawableChild(new StyledButtonWidget(contentX + 42, y, 36, 22, Text.literal("-1"), button -> adjustSession(-1)));
        addDrawableChild(new StyledButtonWidget(contentX + 84, y, 36, 22, Text.literal("+1"), button -> adjustSession(1)));
        addDrawableChild(new StyledButtonWidget(contentX + 126, y, 36, 22, Text.literal("+5"), button -> adjustSession(5)));
        addDrawableChild(new StyledButtonWidget(contentX + contentWidth - 90, y, 90, 32, Text.literal("Reset"), button -> resetSession()));
    }

    private TextFieldWidget createTextField(int x, int y, int width, String placeholder) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 20, Text.literal(placeholder));
        field.setPlaceholder(Text.literal(placeholder));
        field.setDrawsBackground(false);
        return field;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ConfigPanelHelper.BACKDROP);
        drawPanel(context);
        drawHeader(context);
        drawCategories(context, mouseX, mouseY);

        context.enableScissor(contentX - 4, contentAreaTop - 4, contentX + contentWidth + 4, contentAreaBottom);
        drawContent(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);

        context.disableScissor();

        renderStatus(context);
        context.drawTextWithShadow(textRenderer, "ESC to close", panelX + panelWidth - 94, panelY + panelHeight - 16, ConfigPanelHelper.TEXT_SECONDARY);
    }

    private void drawPanel(DrawContext context) {
        ConfigPanelHelper.drawRoundedRect(context, panelX + 3, panelY + 4, panelWidth, panelHeight, ConfigPanelHelper.PANEL_SHADOW);
        ConfigPanelHelper.drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, ConfigPanelHelper.PANEL_BORDER);
        ConfigPanelHelper.drawRoundedRect(context, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, ConfigPanelHelper.PANEL);
        context.fill(panelX + 1, panelY + 43, panelX + sidebarWidth, panelY + panelHeight - 1, ConfigPanelHelper.SIDEBAR);
        context.fill(panelX + sidebarWidth, panelY + 44, panelX + sidebarWidth + 1, panelY + panelHeight - 1, ConfigPanelHelper.PANEL_BORDER);
    }

    private void drawHeader(DrawContext context) {
        context.fill(panelX + 1, panelY + 42, panelX + panelWidth - 1, panelY + 43, ConfigPanelHelper.PANEL_BORDER);
        context.drawTextWithShadow(textRenderer, "Sky", panelX + 17, panelY + 16, ConfigPanelHelper.TEXT_PRIMARY);
        int skyWidth = textRenderer.getWidth("Sky");
        context.drawTextWithShadow(textRenderer, "Counter", panelX + 17 + skyWidth, panelY + 16, ConfigPanelHelper.ACCENT);
        context.drawTextWithShadow(textRenderer, "CONFIGURATION", panelX + panelWidth - 99, panelY + 17, ConfigPanelHelper.TEXT_SECONDARY);
    }

    private void drawCategories(DrawContext context, int mouseX, int mouseY) {
        context.drawTextWithShadow(textRenderer, "CATEGORIES", panelX + 14, panelY + 57, ConfigPanelHelper.TEXT_SECONDARY);
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
            context.drawTextWithShadow(textRenderer, category.label, panelX + 16, y + 8, selected ? ConfigPanelHelper.TEXT_PRIMARY : ConfigPanelHelper.TEXT_SECONDARY);
            y += 27;
        }
    }

    private void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int rowY = panelY + CONTENT_TOP - scrollOffset;
        context.drawTextWithShadow(textRenderer, selectedCategory.title, contentX, rowY, ConfigPanelHelper.TEXT_PRIMARY);
        context.drawTextWithShadow(textRenderer, selectedCategory.description, contentX, rowY + 15, ConfigPanelHelper.TEXT_SECONDARY);

        switch (selectedCategory) {
            case GENERAL -> drawGeneralContent(context, mouseX, mouseY, delta, rowY + 36);
            case HUD -> drawHudContent(context, mouseX, mouseY, delta, rowY + 36);
            case MOBS -> drawMobsContent(context, mouseX, mouseY, delta, rowY + 36);
            case SESSION -> drawSessionContent(context, mouseX, mouseY, delta, rowY + 36);
            case API -> drawApiContent(context, mouseX, mouseY, rowY + 36);
            case TOOLS -> drawToolsContent(context, mouseX, mouseY, rowY + 36);
        }

        updateMaxScroll();
    }

    private void drawGeneralContent(DrawContext context, int mouseX, int mouseY, float delta, int y) {
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y, contentWidth,
                "Show HUD", "Shows or hides the SkyCounter in-game.",
                SkyblockCounterService.isGuiVisible(), hudToggle);
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y + OPTION_HEIGHT + OPTION_GAP, contentWidth,
                "Corleonite Percentage", "Displays the Corleonite drop rate in the HUD.",
                SkyblockCounterService.isShowCorleonitePercentage(), percentageToggle);
    }

    private void drawHudContent(DrawContext context, int mouseX, int mouseY, float delta, int y) {
        ConfigPanelHelper.drawOptionCard(context, contentX, y, contentWidth, 88, ConfigPanelHelper.isInside(mouseX, mouseY, contentX, y, contentWidth, 88));
        context.drawTextWithShadow(textRenderer, "HUD Position", contentX + 14, y + 13, ConfigPanelHelper.TEXT_PRIMARY);
        context.drawTextWithShadow(textRenderer, "Current: X=" + SkyblockCounterService.getHudX() + ", Y=" + SkyblockCounterService.getHudY(),
                contentX + 14, y + 30, ConfigPanelHelper.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, "Enter coordinates and click Apply.", contentX + 14, y + 44, ConfigPanelHelper.TEXT_SECONDARY);

        drawFieldBackground(context, contentX + 12, y + 58, 80, 20);
        drawFieldBackground(context, contentX + 104, y + 58, 80, 20);
    }

    private void drawMobsContent(DrawContext context, int mouseX, int mouseY, float delta, int y) {
        int presetY = y;
        context.drawTextWithShadow(textRenderer, "Quick Presets", contentX, presetY, ConfigPanelHelper.TEXT_PRIMARY);
        int buttonWidth = (contentWidth - 16) / 3;
        drawPresetButton(context, mouseX, mouseY, contentX, presetY + 18, buttonWidth, 28, "Treasure Hoarder", "treasure_hoarder_70");
        drawPresetButton(context, mouseX, mouseY, contentX + buttonWidth + 8, presetY + 18, buttonWidth, 28, "Corleone", "team_treasurite_corleone_200");
        drawPresetButton(context, mouseX, mouseY, contentX + (buttonWidth + 8) * 2, presetY + 18, buttonWidth, 28, "Zealot", "zealot_enderman_55");

        int listY = presetY + 58;
        context.drawTextWithShadow(textRenderer, "Mob List", contentX, listY, ConfigPanelHelper.TEXT_PRIMARY);
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
            context.drawTextWithShadow(textRenderer, entry.name, contentX + 10, rowY + 8, ConfigPanelHelper.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer, entry.id, contentX + 10 + textRenderer.getWidth(entry.name) + 8, rowY + 8, ConfigPanelHelper.TEXT_SECONDARY);
            ConfigPanelHelper.drawButton(context, contentX + contentWidth - 52, rowY + 4, 44, 16,
                    ConfigPanelHelper.isInside(mouseX, mouseY, contentX + contentWidth - 52, rowY + 4, 44, 16), false);
            context.drawCenteredTextWithShadow(textRenderer, "Remove", contentX + contentWidth - 30, rowY + 8, 0xFFFF8888);
            rowY += 28;
        }

        int formY = rowY + 12;
        context.drawTextWithShadow(textRenderer, "Add Mob", contentX, formY, ConfigPanelHelper.TEXT_PRIMARY);
        drawFieldBackground(context, contentX, formY + 16, contentWidth - 16, 20);
        drawFieldBackground(context, contentX, formY + 44, contentWidth - 16, 20);
        drawFieldBackground(context, contentX, formY + 72, contentWidth - 16, 20);
    }

    private void drawSessionContent(DrawContext context, int mouseX, int mouseY, float delta, int y) {
        drawToggleOption(context, mouseX, mouseY, delta, contentX, y, contentWidth,
                "Session Mode", "Enables separate kill tracking for this session.",
                SkyblockCounterService.isShowSessionKills(), sessionToggle);

        int statsY = y + OPTION_HEIGHT + OPTION_GAP + 8;
        if (SkyblockCounterService.isShowSessionKills()) {
            drawStatRow(context, statsY, "Kills", String.valueOf(SkyblockCounterService.getSessionKills()));
            drawStatRow(context, statsY + 34, "Time Farmed", SkyblockCounterActions.formatTimeFarmed().orElse("--:--:--"));
            drawStatRow(context, statsY + 68, "Corleonite / Hour", SkyblockCounterActions.formatCorleonitePerHour().orElse("0.00"));
            drawStatRow(context, statsY + 102, "Corleonite Stats", SkyblockCounterActions.getCorleoniteStats().orElse("No data"));
        } else {
            ConfigPanelHelper.drawStatCard(context, contentX, statsY, contentWidth, 48);
            context.drawCenteredTextWithShadow(textRenderer, "Enable session mode to view stats", contentX + contentWidth / 2, statsY + 20, ConfigPanelHelper.TEXT_SECONDARY);
        }
    }

    private void drawApiContent(DrawContext context, int mouseX, int mouseY, int y) {
        ConfigPanelHelper.drawOptionCard(context, contentX, y, contentWidth, 72, false);
        context.drawTextWithShadow(textRenderer, "Hypixel API Key", contentX + 14, y + 13, ConfigPanelHelper.TEXT_PRIMARY);
        context.drawTextWithShadow(textRenderer, "Required for bestiary kill counts from the API.", contentX + 14, y + 30, ConfigPanelHelper.TEXT_SECONDARY);
        drawFieldBackground(context, contentX + 12, y + 46, contentWidth - 100, 20);

        ConfigPanelHelper.drawOptionCard(context, contentX, y + 82, contentWidth, 72, false);
        context.drawTextWithShadow(textRenderer, "Player UUID", contentX + 14, y + 95, ConfigPanelHelper.TEXT_PRIMARY);
        context.drawTextWithShadow(textRenderer, "Your Minecraft UUID.", contentX + 14, y + 112, ConfigPanelHelper.TEXT_SECONDARY);
        drawFieldBackground(context, contentX + 12, y + 128, contentWidth - 100, 20);
    }

    private void drawToolsContent(DrawContext context, int mouseX, int mouseY, int y) {
        context.drawTextWithShadow(textRenderer, "Corleone Finder", contentX, y + 4, ConfigPanelHelper.TEXT_PRIMARY);
        context.drawTextWithShadow(textRenderer, "Scans loaded chunks for Corleone boss structures and saves waypoints.", contentX, y + 18, ConfigPanelHelper.TEXT_SECONDARY);

        int resultY = y + 68;
        List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();

        if (waypoints.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No saved waypoints yet.", contentX, resultY, ConfigPanelHelper.TEXT_SECONDARY);
        } else {
            context.drawTextWithShadow(textRenderer, "Saved Waypoints:", contentX, resultY, ConfigPanelHelper.TEXT_PRIMARY);
            int rowY = resultY + 16;
            for (WaypointManager.Waypoint wp : waypoints) {
                boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, contentX, rowY, contentWidth, 24);
                ConfigPanelHelper.drawOptionCard(context, contentX, rowY, contentWidth, 24, hovered);

                context.drawTextWithShadow(textRenderer, wp.label(), contentX + 10, rowY + 8, ConfigPanelHelper.TEXT_PRIMARY);
                String coords = "X: " + wp.pos().getX() + ", Y: " + wp.pos().getY() + ", Z: " + wp.pos().getZ();
                context.drawTextWithShadow(textRenderer, coords, contentX + 15 + textRenderer.getWidth(wp.label()), rowY + 8, ConfigPanelHelper.TEXT_SECONDARY);

                ConfigPanelHelper.drawButton(context, contentX + contentWidth - 52, rowY + 4, 44, 16,
                        ConfigPanelHelper.isInside(mouseX, mouseY, contentX + contentWidth - 52, rowY + 4, 44, 16), false);
                context.drawCenteredTextWithShadow(textRenderer, "Remove", contentX + contentWidth - 30, rowY + 8, 0xFFFF8888);

                rowY += 28;
            }
        }
    }

    private void drawStatRow(DrawContext context, int y, String label, String value) {
        int half = (contentWidth - 8) / 2;
        ConfigPanelHelper.drawStatCard(context, contentX, y, half, 28);
        ConfigPanelHelper.drawStatCard(context, contentX + half + 8, y, half, 28);
        context.drawTextWithShadow(textRenderer, label, contentX + 10, y + 10, ConfigPanelHelper.TEXT_SECONDARY);
        context.drawTextWithShadow(textRenderer, value, contentX + half + 18, y + 10, ConfigPanelHelper.TEXT_PRIMARY);
    }

    private void drawPresetButton(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height, String label, String mobId) {
        boolean active = mobId.equals(SkyblockCounterService.getConfig().getLastMobId());
        boolean hovered = ConfigPanelHelper.isInside(mouseX, mouseY, x, y, width, height);
        ConfigPanelHelper.drawButton(context, x, y, width, height, hovered, active);
        context.drawCenteredTextWithShadow(textRenderer, label, x + width / 2, y + height / 2 - 4, ConfigPanelHelper.TEXT_PRIMARY);
    }

    private void drawToggleOption(DrawContext context, int mouseX, int mouseY, float delta, int x, int y, int optionWidth,
                                  String title, String description, boolean enabled, ToggleSwitch toggle) {
        boolean rowHovered = ConfigPanelHelper.isInside(mouseX, mouseY, x, y, optionWidth, OPTION_HEIGHT);
        ConfigPanelHelper.drawOptionCard(context, x, y, optionWidth, OPTION_HEIGHT, rowHovered);

        context.drawTextWithShadow(textRenderer, title, x + 14, y + 13, ConfigPanelHelper.TEXT_PRIMARY);
        context.drawTextWithShadow(textRenderer, description, x + 14, y + 30, ConfigPanelHelper.TEXT_SECONDARY);

        int toggleX = x + optionWidth - ToggleSwitch.WIDTH - 14;
        int toggleY = y + 20;
        toggle.render(context, toggleX, toggleY, enabled, ToggleSwitch.isHovered(mouseX, mouseY, toggleX, toggleY), delta);
    }

    private void drawFieldBackground(DrawContext context, int x, int y, int width, int height) {
        ConfigPanelHelper.drawRoundedRect(context, x, y, width, height, ConfigPanelHelper.FIELD_BG);
        ConfigPanelHelper.drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, ConfigPanelHelper.CARD_INNER);
    }

    private void renderStatus(DrawContext context) {
        if (statusMessage.isEmpty() || System.currentTimeMillis() > statusExpiry) {
            return;
        }
        context.drawTextWithShadow(textRenderer, statusMessage, panelX + 16, panelY + panelHeight - 16, ConfigPanelHelper.ACCENT);
    }

    private int getMobListHeight() {
        return SkyblockCounterActions.listMobs().size() * 28 + 16;
    }

    private void updateMaxScroll() {
        int contentHeight = switch (selectedCategory) {
            case GENERAL -> 150;
            case HUD -> 130;
            case MOBS -> 58 + getMobListHeight() + 220;
            case SESSION -> SkyblockCounterService.isShowSessionKills() ? 250 : 170;
            case API -> 160;
            case TOOLS -> 120 + (WaypointManager.getWaypoints().size() * 28);
        };
        int visibleHeight = contentAreaBottom - contentAreaTop;
        maxScroll = Math.max(0, contentHeight - visibleHeight + 20);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

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

        return super.mouseClicked(click, doubled);
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
        return false;
    }

    private boolean handleSessionToggleClick(double mouseX, double mouseY, int firstOptionY) {
        if (isToggleClicked(mouseX, mouseY, contentX, firstOptionY, contentWidth)) {
            boolean enabled = SkyblockCounterActions.toggleSession();
            setStatus("Session mode " + (enabled ? "enabled" : "disabled"));
            rebuildWidgets();
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

    private void applyHudPosition() {
        try {
            int x = Integer.parseInt(hudXField.getText().trim());
            int y = Integer.parseInt(hudYField.getText().trim());
            LOGGER.info(x +" " + y);
            SkyblockCounterActions.setHudPosition(x, y);
            setStatus("HUD position set to X: " + x + ", Y: " + y);
            sendChat("HUD position set to X: " + x + ", Y: " + y);
        } catch (NumberFormatException e) {
            setStatus("Invalid HUD coordinates");
        }
    }

    private void addMobFromForm() {
        String id = mobIdField.getText().trim();
        String name = mobNameField.getText().trim();
        String texture = mobTextureField.getText().trim();
        if (id.isEmpty() || name.isEmpty()) {
            setStatus("Mob ID and name are required");
            return;
        }
        SkyblockCounterActions.MobAddResult result = SkyblockCounterActions.addMob(id, name, texture);
        setStatus(result.message());
        mobIdField.setText("");
        mobNameField.setText("");
        mobTextureField.setText("");
        rebuildWidgets();
    }

    private void saveApiKey() {
        SkyblockCounterActions.setApiKey(apiKeyField.getText().trim());
        setStatus("API key saved");
        sendChat("API key was set");
    }

    private void saveUuid() {
        SkyblockCounterActions.setUuid(uuidField.getText().trim());
        setStatus("UUID saved");
        sendChat("UUID was set");
    }

    public void runCorleoneFinder() {
        if (client == null || client.world == null || client.player == null) {
            finderResults = List.of("Must be in a world");
            setStatus("Must be in a world");
            return;
        }
        SkyblockCounterActions.CorleoneFinderResult result = SkyblockCounterActions.findCorleoneStructures(client.world, client.player.getBlockPos());
        finderResults = new ArrayList<>(result.messages());

        List<WaypointManager.Waypoint> currentWaypoints = new ArrayList<>(WaypointManager.getWaypoints());
        int count = currentWaypoints.size() + 1;
        for (String msg : result.messages()) {
            if (msg.contains("at:")) {
                try {
                } catch (Exception ignored) {}
            }
        }
        WaypointManager.setWaypoints(currentWaypoints);

        setStatus(result.messages().isEmpty() ? "No structures found" : result.messages().get(0));
        for (String message : result.messages()) {
            sendChat(message);
        }
        rebuildWidgets();
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
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Category {
        GENERAL("General", "General Settings", "Control the main display options."),
        HUD("HUD", "HUD Position", "Set where the counter appears on screen."),
        MOBS("Mobs", "Mob Tracking", "Switch targets and manage custom mobs."),
        API("API", "API Settings", "Configure Hypixel API access."),
        SESSION("Session", "Session Tracking", "Track kills and view statistics for your current session."),
        TOOLS("Tools", "Tools", "Utility features for Skyblock farming and Corleone waypoints.");

        private final String label;
        private final String title;
        private final String description;

        Category(String label, String title, String description) {
            this.label = label;
            this.title = title;
            this.description = description;
        }
    }
}