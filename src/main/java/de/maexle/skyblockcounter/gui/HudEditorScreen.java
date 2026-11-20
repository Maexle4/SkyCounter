package de.maexle.skyblockcounter.gui;

import de.maexle.skyblockcounter.RespawnTimer;
import de.maexle.skyblockcounter.SkyblockCounterService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Full-screen HUD editor: the real game world stays visible behind it, and the two HUD
 * elements are drawn at their actual screen positions so they can be dragged with the mouse.
 *
 * <p>This is the in-game placement flow used by most Skyblock HUD mods: open it from the
 * config GUI or with the config key, drag the elements to wherever they should live, then
 * close it. Positions are written to the config while dragging, so nothing is lost on exit.
 */
public class HudEditorScreen extends Screen {

    private static final int PADDING = 2;

    private final Screen parent;

    private DragTarget dragging = null;
    private int dragOffsetX;
    private int dragOffsetY;

    private Button doneButton;
    private Button resetButton;

    /** Which element the cursor is hovering, used to draw the selection outline. */
    private DragTarget hovered = null;

    private enum DragTarget {
        KILLS("Kill Counter"),
        TIMER("Respawn Timer");

        private final String label;

        DragTarget(String label) {
            this.label = label;
        }
    }

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // The regular HUD renderer must stay quiet while the editor draws the elements itself.
        SkyblockCounterService.setHudEditorActive(true);

        doneButton = Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 104, this.height - 28, 100, 20)
                .build();
        resetButton = Button.builder(Component.literal("Reset"), b -> {
            SkyblockCounterService.setHudPosition(10, 10);
            SkyblockCounterService.setTimerPosition(10, 40);
        }).bounds(this.width / 2 + 4, this.height - 28, 100, 20).build();

        addRenderableWidget(doneButton);
        addRenderableWidget(resetButton);
    }

    @Override
    public void removed() {
        SkyblockCounterService.setHudEditorActive(false);
        super.removed();
    }

    @Override
    public boolean isInGameUi() {
        // Makes the screen draw only a light transparent backdrop over the world.
        return true;
    }

    /**
     * Skips the translucent black veil so the world stays fully readable behind the editor
     * (a dimmed world makes it hard to judge whether the HUD overlaps important content).
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Intentionally empty: draw no backdrop at all.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        hovered = null;

        // Kill counter at its real position.
        int killX = SkyblockCounterService.getHudX();
        int killY = SkyblockCounterService.getHudY();
        int killW = SkyblockCounterService.killHudWidth();
        int killH = SkyblockCounterService.killHudHeight();

        if (isInside(mouseX, mouseY, killX, killY, killW, killH)) {
            hovered = DragTarget.KILLS;
        }
        drawOutline(context, killX, killY, killW, killH, hovered == DragTarget.KILLS || dragging == DragTarget.KILLS);
        SkyblockCounterService.drawKillsAt(context, killX, killY);

        // Respawn timer at its real position.
        int timerX = SkyblockCounterService.getTimerX();
        int timerY = SkyblockCounterService.getTimerY();
        int timerW = SkyblockCounterService.TIMER_PANEL_WIDTH;
        int timerH = SkyblockCounterService.timerBlockHeight();

        if (isInside(mouseX, mouseY, timerX, timerY, timerW, timerH)) {
            hovered = DragTarget.TIMER;
        }
        drawOutline(context, timerX, timerY, timerW, timerH, hovered == DragTarget.TIMER || dragging == DragTarget.TIMER);

        if (RespawnTimer.anyVisible()) {
            SkyblockCounterService.drawRespawnTimersAt(context, timerX, timerY);
        } else {
            drawTimerPlaceholder(context, timerX, timerY);
        }

        drawHint(context, mouseX, mouseY);
    }

    /** Placeholder so the timer can be positioned even when no countdown is running. */
    private void drawTimerPlaceholder(GuiGraphicsExtractor context, int x, int y) {
        int w = SkyblockCounterService.TIMER_PANEL_WIDTH;
        int h = SkyblockCounterService.TIMER_PANEL_HEIGHT;

        ConfigPanelHelper.drawRoundedRect(context, x, y, w, h, 0xFF3D3A4C);
        ConfigPanelHelper.drawRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 0xF01B1A23);
        ConfigPanelHelper.drawRoundedRect(context, x + 2, y + 2, 3, h - 4, ConfigPanelHelper.ACCENT);

        context.text(this.font, "Corleone", x + 11, y + 5, 0xFFA7A3B3, false);
        context.text(this.font, "60s --:-- | 120s --:--", x + 11, y + 15, 0xFF6E6A7C, false);
        context.fill(x + 11, y + h - 8, x + w - 9, y + h - 5, 0xFF322F3E);
    }

    private void drawOutline(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean active) {
        int color = active ? ConfigPanelHelper.ACCENT : 0x60FFFFFF;
        context.fill(x - PADDING, y - PADDING, x + w + PADDING, y - PADDING + 1, color);
        context.fill(x - PADDING, y + h + PADDING - 1, x + w + PADDING, y + h + PADDING, color);
        context.fill(x - PADDING, y - PADDING, x - PADDING + 1, y + h + PADDING, color);
        context.fill(x + w + PADDING - 1, y - PADDING, x + w + PADDING, y + h + PADDING, color);
    }

    private void drawHint(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        String hint = dragging != null
                ? "Drag to move  -  release to save"
                : "Drag the outlined elements  -  ESC / Done when finished";
        context.centeredText(this.font, hint, this.width / 2, 12, 0xFFFFFFFF);

        String coords = "Kills: " + SkyblockCounterService.getHudX() + ", " + SkyblockCounterService.getHudY()
                + "     Timer: " + SkyblockCounterService.getTimerX() + ", " + SkyblockCounterService.getTimerY();
        context.centeredText(this.font, coords, this.width / 2, 26, 0xFFA7A3B3);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x - PADDING && mouseX < x + width + PADDING
                && mouseY >= y - PADDING && mouseY < y + height + PADDING;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        // 26.3 uses SDL button ids: the primary button is 1, not 0.
        if (click.button() != 1) {
            return super.mouseClicked(click, doubleClick);
        }

        double mouseX = click.x();
        double mouseY = click.y();

        int timerW = SkyblockCounterService.TIMER_PANEL_WIDTH;
        int timerH = SkyblockCounterService.timerBlockHeight();
        int timerX = SkyblockCounterService.getTimerX();
        int timerY = SkyblockCounterService.getTimerY();

        if (isInside(mouseX, mouseY, timerX, timerY, timerW, timerH)) {
            dragging = DragTarget.TIMER;
            dragOffsetX = (int) (mouseX - timerX);
            dragOffsetY = (int) (mouseY - timerY);
            return true;
        }

        int killW = SkyblockCounterService.killHudWidth();
        int killH = SkyblockCounterService.killHudHeight();
        int killX = SkyblockCounterService.getHudX();
        int killY = SkyblockCounterService.getHudY();

        if (isInside(mouseX, mouseY, killX, killY, killW, killH)) {
            dragging = DragTarget.KILLS;
            dragOffsetX = (int) (mouseX - killX);
            dragOffsetY = (int) (mouseY - killY);
            return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dragX, double dragY) {
        if (dragging == null) {
            return super.mouseDragged(click, dragX, dragY);
        }

        int newX = (int) Math.round(click.x() - dragOffsetX);
        int newY = (int) Math.round(click.y() - dragOffsetY);

        // Keep the element reachable instead of letting it slide off screen.
        newX = Math.max(0, Math.min(newX, this.width - 8));
        newY = Math.max(0, Math.min(newY, this.height - 8));

        if (dragging == DragTarget.KILLS) {
            SkyblockCounterService.setHudPosition(newX, newY);
        } else {
            SkyblockCounterService.setTimerPosition(newX, newY);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }
}
