package de.maexle.skyblockcounter.gui;

import net.minecraft.client.gui.DrawContext;

public final class ConfigPanelHelper {
    public static final int BACKDROP = 0xB8000000;
    public static final int PANEL = 0xFF24232E;
    public static final int SIDEBAR = 0xFF1B1A23;
    public static final int PANEL_BORDER = 0xFF3D3A4C;
    public static final int PANEL_SHADOW = 0x7A000000;
    public static final int ACCENT = 0xFFB16EFF;
    public static final int ACCENT_DARK = 0xFF583B79;
    public static final int TEXT_PRIMARY = 0xFFF1EFF7;
    public static final int TEXT_SECONDARY = 0xFFA7A3B3;
    public static final int CARD_BG = 0xFF2A2935;
    public static final int CARD_BG_HOVER = 0xFF302E3D;
    public static final int CARD_INNER = 0xFF25242E;
    public static final int BUTTON_BG = 0xFF3A3550;
    public static final int BUTTON_BG_HOVER = 0xFF4A4068;
    public static final int BUTTON_BG_ACTIVE = 0xFF583B79;
    public static final int FIELD_BG = 0xFF1E1D26;

    private ConfigPanelHelper() {
    }

    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + 1, color);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
        context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, color);
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, color);
    }

    public static void drawOptionCard(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        drawRoundedRect(context, x, y, width, height, hovered ? CARD_BG_HOVER : CARD_BG);
        drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, CARD_INNER);
    }

    public static void drawStatCard(DrawContext context, int x, int y, int width, int height) {
        drawRoundedRect(context, x, y, width, height, 0xFF2E2C3A);
        drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, 0xFF23222C);
    }

    public static void drawButton(DrawContext context, int x, int y, int width, int height, boolean hovered, boolean active) {
        int color = active ? BUTTON_BG_ACTIVE : (hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        drawRoundedRect(context, x, y, width, height, color);
        if (active) {
            context.fill(x, y + 2, x + 2, y + height - 2, ACCENT);
        }
    }

    public static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
