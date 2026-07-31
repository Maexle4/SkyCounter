package de.maexle.skyblockcounter.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;

public class StyledButtonWidget extends ClickableWidget {
    private boolean activeHighlight;
    private final PressAction onPress;

    @FunctionalInterface
    public interface PressAction {
        void onPress(StyledButtonWidget button);
    }

    public StyledButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public void setActiveHighlight(boolean activeHighlight) {
        this.activeHighlight = activeHighlight;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ConfigPanelHelper.drawButton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), activeHighlight);
        int textColor = activeHighlight ? ConfigPanelHelper.TEXT_PRIMARY : (isHovered() ? ConfigPanelHelper.TEXT_PRIMARY : ConfigPanelHelper.TEXT_SECONDARY);
        context.drawCenteredTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.active && this.visible && click.button() == 0) {
            if (this.isMouseOver(click.x(), click.y())) {
                this.onClick(click.x(), click.y());
                return true;
            }
        }
        return false;
    }

    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) {
            onPress.onPress(this);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getMessage());
    }
}