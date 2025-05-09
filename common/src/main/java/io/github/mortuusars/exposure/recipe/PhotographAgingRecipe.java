package io.github.mortuusars.exposure.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Exposure; // Assuming Exposure.RecipeSerializers is your registry class
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList; // Added for new ArrayList<>()
import java.util.List;

public class PhotographAgingRecipe extends AbstractNbtTransferringRecipe {

    public PhotographAgingRecipe(CraftingBookCategory category, Ingredient transferIngredient,
                                 NonNullList<Ingredient> ingredients, ItemStack result) {
        super(category, transferIngredient, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Exposure.RecipeSerializers.PHOTOGRAPH_AGING.get(); // Ensure this path is correct
    }

    public static class Serializer implements RecipeSerializer<PhotographAgingRecipe> {
        private static final Codec<PhotographAgingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CraftingBookCategory.CODEC.fieldOf("category").forGetter(AbstractNbtTransferringRecipe::category),
                Ingredient.CODEC_NONEMPTY.fieldOf("photograph_ingredient").forGetter(AbstractNbtTransferringRecipe::getTransferIngredient),
                Ingredient.CODEC_NONEMPTY.listOf().xmap(
                        (List<Ingredient> list) -> { // Explicit conversion from List to NonNullList
                            NonNullList<Ingredient> nnl = NonNullList.create();
                            nnl.addAll(list);
                            return nnl;
                        },
                        (NonNullList<Ingredient> nnList) -> new ArrayList<>(nnList) // Explicit conversion from NonNullList to ArrayList (List)
                ).fieldOf("ingredients").forGetter(recipe -> recipe.recipeIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(AbstractNbtTransferringRecipe::getDefinedResultItem)
        ).apply(instance, PhotographAgingRecipe::new));

        @Override
        public @NotNull Codec<PhotographAgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull PhotographAgingRecipe fromNetwork(@NotNull FriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient photographIngredient = Ingredient.fromNetwork(buffer);
            int ingredientsCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientsCount, Ingredient.EMPTY);
            for (int i = 0; i < ingredientsCount; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            return new PhotographAgingRecipe(category, photographIngredient, ingredients, result);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull PhotographAgingRecipe recipe) {
            buffer.writeEnum(recipe.category());
            recipe.getTransferIngredient().toNetwork(buffer);
            buffer.writeVarInt(recipe.recipeIngredients.size());
            for (Ingredient ingredient : recipe.recipeIngredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.getDefinedResultItem());
        }
    }
}
