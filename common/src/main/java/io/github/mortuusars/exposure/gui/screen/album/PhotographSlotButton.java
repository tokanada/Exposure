package io.github.mortuusars.exposure.gui.screen.album;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.Tesselator;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.render.PhotographRenderProperties;
import io.github.mortuusars.exposure.render.PhotographRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


public class PhotographSlotButton extends ImageButton {
    public static final WidgetSprites PINS_SPRITES = new WidgetSprites(
            Exposure.resource("album/pins"), Exposure.resource("album/pins_highlighted"));
    public static final WidgetSprites PINS_EMPTY_SPRITES = new WidgetSprites(
            Exposure.resource("album/pins_empty"), Exposure.resource("album/pins_empty_highlighted"));

    protected final Rect2i exposureArea;
    protected final OnPress onRightButtonPress;
    protected final Supplier<ItemStack> photograph;
    protected final boolean isEditable;
    protected boolean hasPhotograph;

    public PhotographSlotButton(Rect2i exposureArea, int x, int y, int width, int height, int xTexStart, int yTexStart,
                                int yDiffTex, ResourceLocation resourceLocation, int textureWidth, int textureHeight,
                                OnPress onLeftButtonPress, OnPress onRightButtonPress, Supplier<ItemStack> photographGetter, boolean isEditable) {
        super(x, y, width, height, PINS_EMPTY_SPRITES, onLeftButtonPress, Component.translatable("item.exposure.photograph"));

        this.exposureArea = exposureArea;
        this.onRightButtonPress = onRightButtonPress;
        this.photograph = photographGetter;
        this.isEditable = isEditable;
    }

    public ItemStack getPhotograph() {
        return photograph.get();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ItemStack photograph = getPhotograph();

        if (photograph.getItem() instanceof PhotographItem) {
            hasPhotograph = true;

            PhotographRenderProperties renderProperties = PhotographRenderProperties.get(photograph);

            // Paper
            guiGraphics.blit(renderProperties.getAlbumPaperOverlayTexture(),
                    getX(), getY(), 0, 0, 0, width, height, width, height);

            // Exposure
            guiGraphics.pose().pushPose();
            float scale = exposureArea.getWidth() / (float) ExposureClient.getExposureRenderer().getSize();
            guiGraphics.pose().translate(exposureArea.getX(), exposureArea.getY(), 1);
            guiGraphics.pose().scale(scale, scale, scale);
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            PhotographRenderer.render(photograph, false, false, guiGraphics.pose(),
                    bufferSource, LightTexture.FULL_BRIGHT, 255, 255, 255, 255);
            bufferSource.endBatch();
            guiGraphics.pose().popPose();

            // Paper overlay
            if (renderProperties.hasAlbumPaperOverlayTexture()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 2);
                guiGraphics.blit(renderProperties.getAlbumPaperOverlayTexture(),
                        getX(), getY(), 0, 0, 0, width, height, width, height);
                guiGraphics.pose().popPose();
            }
        }
        else {
            hasPhotograph = false;
        }

        // Album pins
        WidgetSprites sprites = hasPhotograph ? PINS_SPRITES : PINS_EMPTY_SPRITES;
        ResourceLocation resourceLocation = sprites.get(this.isActive(), this.isHoveredOrFocused());
        guiGraphics.blitSprite(resourceLocation, this.getX(), this.getY(), this.width, this.height);
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isEditable && !hasPhotograph) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui.exposure.album.add_photograph"), mouseX, mouseY);
            return;
        }

        ItemStack photograph = getPhotograph();
        if (photograph.isEmpty())
            return;

        List<Component> itemTooltip = Screen.getTooltipFromItem(Minecraft.getInstance(), photograph);
        itemTooltip.add(Component.translatable("gui.exposure.album.left_click_or_scroll_up_to_view"));
        if (isEditable)
            itemTooltip.add(Component.translatable("gui.exposure.album.right_click_to_remove"));

        // Photograph image in tooltip is not rendered here

        if (isFocused()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, Lists.transform(itemTooltip,
                    Component::getVisualOrderText), DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
        }
        else
            guiGraphics.renderTooltip(Minecraft.getInstance().font, itemTooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !clicked(mouseX, mouseY))
            return false;

        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.onPress.onPress(this);
        } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            this.onRightButtonPress.onPress(this);
        } else
            return false;

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && clicked(mouseX, mouseY) && hasPhotograph) {
            this.onPress.onPress(this);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && Screen.hasShiftDown() && CommonInputs.selected(keyCode)) {
            onRightButtonPress.onPress(this);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
