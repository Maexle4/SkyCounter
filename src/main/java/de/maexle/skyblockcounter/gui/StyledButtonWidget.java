package de.maexle.skyblockcounter.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class StyledButtonWidget extends AbstractWidget {
    private boolean activeHighlight;
    private final PressAction onPress;

    @FunctionalInterface
    public interface PressAction {
        void onPress(StyledButtonWidget button);
    }

    public StyledButtonWidget(int x, int y, int width, int height, Component message, PressAction onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public void setActiveHighlight(boolean activeHighlight) {
        this.activeHighlight = activeHighlight;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        ConfigPanelHelper.drawButton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), activeHighlight);
        int textColor = activeHighlight ? ConfigPanelHelper.TEXT_PRIMARY : (isHovered() ? ConfigPanelHelper.TEXT_PRIMARY : ConfigPanelHelper.TEXT_SECONDARY);

        context.centeredText(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible && event.button() == 0) {
            if (this.isMouseOver(event.x(), event.y())) {
                this.onClick(event, doubleClick);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (onPress != null) {
            onPress.onPress(this);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}