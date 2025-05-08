package io.github.mortuusars.exposure.data.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.util.Color;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class Filter {
    // Default values
    public static final ResourceLocation DEFAULT_GLASS_TEXTURE = Exposure.resource("textures/gui/filter/stained_glass.png");
    public static final int DEFAULT_TINT_COLOR_INT = 0xFFFFFF;
    public static final String DEFAULT_TINT_COLOR_HEX = String.format("#%06X", DEFAULT_TINT_COLOR_INT & 0xFFFFFF);

    private final Ingredient ingredient;
    private final ResourceLocation shader;
    private final ResourceLocation attachmentTexture;
    private final int tintColor;

    public Filter(Ingredient ingredient, ResourceLocation shader, ResourceLocation attachmentTexture, int tintColor) {
        this.ingredient = ingredient;
        this.shader = shader;
        this.attachmentTexture = attachmentTexture;
        this.tintColor = tintColor;
    }

    public boolean matches(ItemStack stack) {
        return ingredient.test(stack);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ResourceLocation getShader() {
        return shader;
    }

    public ResourceLocation getAttachmentTexture() {
        return attachmentTexture;
    }

    public int getTintColor() {
        return tintColor;
    }

    // Codec definition
    public static final Codec<Filter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("item").forGetter(Filter::getIngredient),
            ResourceLocation.CODEC.fieldOf("shader").forGetter(Filter::getShader),
            ResourceLocation.CODEC.optionalFieldOf("attachment_texture", DEFAULT_GLASS_TEXTURE).forGetter(Filter::getAttachmentTexture),
            Codec.STRING.optionalFieldOf("tint_color", DEFAULT_TINT_COLOR_HEX)
                    .xmap(
                            Color::getRGBFromHex,
                            rgbInt -> String.format("#%06X", rgbInt & 0xFFFFFF)
                    ).forGetter(Filter::getTintColor)
    ).apply(instance, Filter::new));
}