package io.github.mortuusars.exposure.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer; // Added for context
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for custom crafting recipes that transfer NBT data from a specific ingredient to the result.
 * The recipe matching logic checks for the presence of one transfer ingredient and a list of other standard ingredients.
 */
public abstract class AbstractNbtTransferringRecipe extends CustomRecipe {
    // The ingredient from which NBT data will be transferred.
    protected final Ingredient transferIngredient;
    // A list of additional ingredients required for the recipe.
    protected final NonNullList<Ingredient> recipeIngredients;
    // The resulting ItemStack of the recipe, before NBT transfer.
    protected final ItemStack resultItemStack;

    /**
     * Constructor for AbstractNbtTransferringRecipe.
     * @param category The crafting book category for this recipe.
     * @param transferIngredient The ingredient whose NBT will be transferred.
     * @param ingredients The list of other ingredients for the recipe.
     * @param result The output item stack.
     */
    public AbstractNbtTransferringRecipe(CraftingBookCategory category, Ingredient transferIngredient, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(category);
        this.transferIngredient = transferIngredient;
        this.recipeIngredients = ingredients;
        this.resultItemStack = result;
    }

    /**
     * @return The ingredient designated for NBT transfer.
     */
    public @NotNull Ingredient getTransferIngredient() {
        return transferIngredient;
    }

    /**
     * @return The list of all ingredients for this recipe (transfer ingredient + other recipe ingredients).
     * This is often used for recipe book display and by systems like JEI.
     */
    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> allIngredients = NonNullList.create();
        if (this.transferIngredient != null && !this.transferIngredient.isEmpty()) {
            allIngredients.add(this.transferIngredient);
        }
        allIngredients.addAll(this.recipeIngredients);
        return allIngredients;
    }

    /**
     * Gets the base result item. NBT transfer happens in the assemble method.
     * @param registryAccess Access to registries, can be null if not needed for result determination.
     * @return The result ItemStack, without NBT transfer (a copy).
     */
    @Override
    public @NotNull ItemStack getResultItem(@Nullable RegistryAccess registryAccess) {
        return this.resultItemStack.copy();
    }

    /**
     * A direct getter for the defined result item stack.
     * Used by the serializer/codec.
     * @return The result ItemStack (not a copy).
     */
    public @NotNull ItemStack getDefinedResultItem() {
        return this.resultItemStack;
    }


    /**
     * Determines if the crafting container matches this recipe.
     * It checks for one item matching the transferIngredient and that all other
     * recipeIngredients are also present, irrespective of their order in the grid.
     *
     * @param container The crafting container.
     * @param level The world level.
     * @return True if the recipe matches, false otherwise.
     */
    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        // Handle the special case where the recipe might only require the NBT transfer item
        // and no other ingredients.
        if (this.recipeIngredients.isEmpty()) {
            if (this.transferIngredient.isEmpty()) { // Should not happen for a valid recipe
                return false;
            }
            boolean foundTransferItem = false;
            int itemCount = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stackInSlot = container.getItem(i);
                if (!stackInSlot.isEmpty()) {
                    itemCount++;
                    if (this.transferIngredient.test(stackInSlot)) {
                        if (foundTransferItem) return false; // Only one transfer item allowed
                        foundTransferItem = true;
                    } else {
                        return false; // Extra item present that is not the transfer item
                    }
                }
            }
            // True if exactly one transfer item is found and no other items.
            return foundTransferItem && itemCount == 1;
        }

        // Collect all non-empty items from the crafting grid.
        List<ItemStack> itemsInGrid = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemstack = container.getItem(i);
            if (!itemstack.isEmpty()) {
                itemsInGrid.add(itemstack);
            }
        }

        // Early exit if item count in grid doesn't match expected count
        // (1 transfer ingredient + N other recipeIngredients).
        if (itemsInGrid.size() != 1 + this.recipeIngredients.size()) {
            return false;
        }

        ItemStack transferItemCandidate = null;
        List<ItemStack> remainingItemsForMatching = new ArrayList<>();

        // Attempt to find the transfer ingredient among the items in the grid.
        // All other items are collected into 'remainingItemsForMatching'.
        boolean foundTransferIngredientInGrid = false;
        for (ItemStack stackInSlot : itemsInGrid) {
            if (!foundTransferIngredientInGrid && this.transferIngredient.test(stackInSlot)) {
                transferItemCandidate = stackInSlot; // Found the NBT transfer item
                foundTransferIngredientInGrid = true;
            } else {
                remainingItemsForMatching.add(stackInSlot);
            }
        }

        // If the NBT transfer ingredient wasn't found, or if the count of remaining items
        // doesn't match the count of other recipe ingredients, it's not a match.
        if (transferItemCandidate == null || remainingItemsForMatching.size() != this.recipeIngredients.size()) {
            return false;
        }

        // Now, check if the 'remainingItemsForMatching' can satisfy all 'recipeIngredients'.
        // This is a shapeless-style match for the remaining ingredients.
        List<Ingredient> ingredientsToMatch = new ArrayList<>(this.recipeIngredients);
        for (ItemStack itemInGrid : remainingItemsForMatching) {
            boolean matchedThisItem = false;
            for (int i = 0; i < ingredientsToMatch.size(); i++) {
                if (ingredientsToMatch.get(i).test(itemInGrid)) {
                    ingredientsToMatch.remove(i); // Ingredient satisfied, remove it from the list to match
                    matchedThisItem = true;
                    break;
                }
            }
            if (!matchedThisItem) {
                // An item in the grid couldn't be matched to any of the remaining recipe ingredients.
                return false;
            }
        }

        // If all ingredients in 'ingredientsToMatch' have been removed, it means all were satisfied.
        return ingredientsToMatch.isEmpty();
    }


    /**
     * Assembles the result ItemStack, transferring NBT from the transferIngredient.
     * @param container The crafting container.
     * @param registryAccess Access to registries.
     * @return The final ItemStack with NBT transferred.
     */
    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        ItemStack nbtSourceStack = ItemStack.EMPTY;
        // Find the item in the container that matches the transferIngredient.
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemInSlot = container.getItem(i);
            if (getTransferIngredient().test(itemInSlot)) {
                nbtSourceStack = itemInSlot;
                break; // Found the source for NBT
            }
        }

        ItemStack result = getResultItem(registryAccess).copy(); // Get a fresh copy of the base result

        // If an NBT source was found and it has a tag, transfer (merge) its NBT.
        if (!nbtSourceStack.isEmpty() && nbtSourceStack.hasTag()) {
            CompoundTag nbtToTransfer = nbtSourceStack.getTag().copy(); // Copy NBT from source
            if (result.getTag() != null) {
                result.getTag().merge(nbtToTransfer); // Merge if result already has NBT
            } else {
                result.setTag(nbtToTransfer); // Set NBT if result has none
            }
        }
        return result;
    }

    /**
     * Helper method to transfer NBT from a source stack to a target stack.
     * This can be called directly if needed, for example, in creative crafting scenarios.
     * @param transferIngredientStack The ItemStack to get NBT from.
     * @param recipeResultStack The ItemStack to transfer NBT to.
     * @return The recipeResultStack with NBT transferred.
     */
    public @NotNull ItemStack transferNbt(@NotNull ItemStack transferIngredientStack, @NotNull ItemStack recipeResultStack) {
        @Nullable CompoundTag transferTag = transferIngredientStack.getTag();
        if (transferTag != null) {
            // Ensure we're working with a copy of the tag for merging/setting
            CompoundTag nbtToApply = transferTag.copy();
            if (recipeResultStack.getTag() != null) {
                recipeResultStack.getOrCreateTag().merge(nbtToApply);
            } else {
                recipeResultStack.setTag(nbtToApply);
            }
        }
        return recipeResultStack;
    }

    /**
     * Determines if the recipe can fit in the given crafting grid dimensions.
     * @param width The width of the crafting grid.
     * @param height The height of the crafting grid.
     * @return True if the recipe (transfer ingredient + other ingredients) can fit.
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // Considers 1 transfer ingredient + the list of other ingredients
        int totalIngredients = (getTransferIngredient().isEmpty() ? 0 : 1) + recipeIngredients.size();
        return totalIngredients > 0 && totalIngredients <= width * height;
    }

    // isSpecial() is true for CustomRecipe by default. If this recipe should show up in the recipe book
    // and be unlocked like normal recipes, you might override this to return false.
    // However, for recipes that don't have a standard JSON definition and rely on custom logic
    // (like this one), isSpecial() = true is typically correct.
    // @Override
    // public boolean isSpecial() {
    //     return true;
    // }
}
