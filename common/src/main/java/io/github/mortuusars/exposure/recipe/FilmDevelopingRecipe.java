package io.github.mortuusars.exposure.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Exposure; // Assuming Exposure.RecipeSerializers is your registry class
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList; // Added for new ArrayList<>()
import java.util.List;
import java.util.Objects;
// import java.util.Objects; // No longer used

public class FilmDevelopingRecipe extends AbstractNbtTransferringRecipe {

    public FilmDevelopingRecipe(CraftingBookCategory category, Ingredient filmIngredient, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(category, filmIngredient, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Exposure.RecipeSerializers.FILM_DEVELOPING.get(); // Ensure this path is correct
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack item = container.getItem(i);
            if (item.getItem() instanceof PotionItem) {
                remainingItems.set(i, new ItemStack(Items.GLASS_BOTTLE));
            }
            // Use item.hasRecipeRemainder() and item.getRecipeRemainder()
            else if (item.getItem().hasCraftingRemainingItem()) {
                remainingItems.set(i, new ItemStack(Objects.requireNonNull(item.getItem().getCraftingRemainingItem()))); // getRecipeRemainder() returns ItemStack
            }
        }
        return remainingItems;
    }

    public static class Serializer implements RecipeSerializer<FilmDevelopingRecipe> {
        private static final Codec<FilmDevelopingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CraftingBookCategory.CODEC.fieldOf("category").forGetter(AbstractNbtTransferringRecipe::category),
                Ingredient.CODEC_NONEMPTY.fieldOf("film_ingredient").forGetter(AbstractNbtTransferringRecipe::getTransferIngredient),
                Ingredient.CODEC_NONEMPTY.listOf().xmap(
                        (List<Ingredient> list) -> { // Explicit conversion from List to NonNullList
                            NonNullList<Ingredient> nnl = NonNullList.create();
                            nnl.addAll(list);
                            return nnl;
                        },
                        (NonNullList<Ingredient> nnList) -> new ArrayList<>(nnList) // Explicit conversion from NonNullList to ArrayList (List)
                ).fieldOf("ingredients").forGetter(recipe -> recipe.recipeIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(AbstractNbtTransferringRecipe::getDefinedResultItem)
        ).apply(instance, FilmDevelopingRecipe::new));

        @Override
        public @NotNull Codec<FilmDevelopingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull FilmDevelopingRecipe fromNetwork(@NotNull FriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient filmIngredient = Ingredient.fromNetwork(buffer);
            int ingredientsCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientsCount, Ingredient.EMPTY);
            for (int i = 0; i < ingredientsCount; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            return new FilmDevelopingRecipe(category, filmIngredient, ingredients, result);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull FilmDevelopingRecipe recipe) {
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
