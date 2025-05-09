package io.github.mortuusars.exposure.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Exposure; // Assuming Exposure.RecipeSerializers is your registry class
import io.github.mortuusars.exposure.item.PhotographItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList; // Added for new ArrayList<>()
import java.util.List;
// import java.util.Objects; // No longer used by this class directly

public class PhotographCopyingRecipe extends AbstractNbtTransferringRecipe {

    // Constructor now takes CraftingBookCategory
    public PhotographCopyingRecipe(CraftingBookCategory category, Ingredient transferIngredient, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(category, transferIngredient, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Exposure.RecipeSerializers.PHOTOGRAPH_CLONING.get(); // Ensure this path is correct
    }

    @Override
    public @NotNull ItemStack transferNbt(@NotNull ItemStack photographStack, @NotNull ItemStack recipeResultStack) {
        // Ensure the photographStack is the one being processed and has valid data
        if (photographStack.getItem() instanceof PhotographItem && photographStack.hasTag()) {
            // Check generation for copying books, apply similar logic if needed for photographs
            int currentGeneration = 0; // Default if no generation tag
            if (photographStack.getTag().contains(WrittenBookItem.TAG_GENERATION, CompoundTag.TAG_INT)) {
                currentGeneration = photographStack.getTag().getInt(WrittenBookItem.TAG_GENERATION);
            }

            if (currentGeneration < 2) { // Max 2 copies (original, gen 1, gen 2)
                ItemStack result = super.transferNbt(photographStack, recipeResultStack.copy()); // Work on a copy of the result
                CompoundTag resultTag = result.getOrCreateTag();
                resultTag.putInt(WrittenBookItem.TAG_GENERATION, currentGeneration + 1);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

//    @Override
//    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer container) {
//        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
//
//        for(int i = 0; i < remainingItems.size(); ++i) {
//            ItemStack itemstack = container.getItem(i);
//            // Use itemstack.hasRecipeRemainder() and itemstack.getRecipeRemainder()
//            if (itemstack.hasRecipeRemainder()) {
//                remainingItems.set(i, itemstack.getRecipeRemainder().copy()); // getRecipeRemainder() returns ItemStack
//            } else if (itemstack.getItem() instanceof PhotographItem) {
//                // If it's a photograph being copied, it's consumed unless it's the original that stays.
//                // This logic makes the original photograph (the one being copied) also a remaining item.
//                ItemStack photographCopy = itemstack.copy();
//                photographCopy.setCount(1);
//                remainingItems.set(i, photographCopy);
//            }
//        }
//        return remainingItems;
//    }

    public static class Serializer implements RecipeSerializer<PhotographCopyingRecipe> {
        // Define the Codec for PhotographCopyingRecipe
        private static final Codec<PhotographCopyingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CraftingBookCategory.CODEC.fieldOf("category").forGetter(AbstractNbtTransferringRecipe::category),
                Ingredient.CODEC_NONEMPTY.fieldOf("photograph_ingredient").forGetter(AbstractNbtTransferringRecipe::getTransferIngredient),
                Ingredient.CODEC_NONEMPTY.listOf().xmap(
                        (List<Ingredient> list) -> { // Explicit conversion from List to NonNullList
                            NonNullList<Ingredient> nnl = NonNullList.create();
                            nnl.addAll(list);
                            return nnl;
                        },
                        (NonNullList<Ingredient> nnList) -> new ArrayList<>(nnList) // Explicit conversion from NonNullList to ArrayList (List)
                ).fieldOf("ingredients").forGetter(recipe -> recipe.recipeIngredients), // Access protected field
                ItemStack.CODEC.fieldOf("result").forGetter(AbstractNbtTransferringRecipe::getDefinedResultItem)
        ).apply(instance, PhotographCopyingRecipe::new));

        @Override
        public @NotNull Codec<PhotographCopyingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull PhotographCopyingRecipe fromNetwork(@NotNull FriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient photographIngredient = Ingredient.fromNetwork(buffer);
            int ingredientsCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientsCount, Ingredient.EMPTY);
            for (int i = 0; i < ingredientsCount; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            return new PhotographCopyingRecipe(category, photographIngredient, ingredients, result);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull PhotographCopyingRecipe recipe) {
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
