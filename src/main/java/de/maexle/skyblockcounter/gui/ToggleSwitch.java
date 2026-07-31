package de.maexle.skyblockcounter.gui;

import net.minecraft.client.gui.DrawContext;


public final class ToggleSwitch {
    public static final int WIDTH = 42;
    public static final int HEIGHT = 20;

    private float thumbPosition;

    public ToggleSwitch(boolean enabled) {
        thumbPosition = enabled ? 1.0F : 0.0F;
    }

    public void render(DrawContext context, int x, int y, boolean enabled, boolean hovered, float tickDelta) {
        float target = enabled ? 1.0F : 0.0F;
        thumbPosition += (target - thumbPosition) * Math.min(1.0F, 0.30F + tickDelta * 0.15F);

        int trackColor = enabled ? 0xFF8E58D8 : 0xFF4A4858;
        if (hovered) {
            trackColor = enabled ? 0xFFA66AF3 : 0xFF5B586A;
        }

        drawRoundedRect(context, x, y, WIDTH, HEIGHT, trackColor);
        drawRoundedRect(context, x + 1, y + 1, WIDTH - 2, HEIGHT - 2, 0xFF272530);

        int travel = WIDTH - HEIGHT;
        int thumbX = x + 2 + Math.round(travel * thumbPosition);
        int thumbColor = enabled ? 0xFFE8D7FF : 0xFFE3E1E8;
        drawRoundedRect(context, thumbX, y + 2, HEIGHT - 4, HEIGHT - 4, thumbColor);
        context.fill(thumbX + 3, y + 4, thumbX + HEIGHT - 5, y + 5, enabled ? 0xFFFFFFFF : 0xFFCBC8D0);
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    private static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + 1, color);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
        context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, color);
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, color);
    }
}
