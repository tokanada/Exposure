// Path: io/github/mortuusars/exposure/integration/jei/recipe/NbtTransferringShapelessExtension.java
package io.github.mortuusars.exposure.integration.jei.recipe;

import io.github.mortuusars.exposure.recipe.AbstractNbtTransferringRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
// This is the generic interface from JEI
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient; // Make sure this is imported
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class NbtTransferringShapelessExtension implements ICraftingCategoryExtension<AbstractNbtTransferringRecipe> {
//
//    @Override
//    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ICraftingGridHelper craftingGridHelper, @NotNull IFocusGroup focuses) {
//        // Ensure 'recipe' (which is of type R) is used here.
//        List<List<ItemStack>> allInputsForDisplay = this.recipe.getIngredients().stream()
//                .map(ingredient -> List.of(ingredient.getItems()))
//                .collect(Collectors.toList());
//
//        ItemStack resultItem = this.recipe.getDefinedResultItem();
//
//        // For shapeless, width/height 0,0 usually lets JEI auto-arrange
//        craftingGridHelper.createAndSetInputs(builder, allInputsForDisplay, 0, 0);
//        craftingGridHelper.createAndSetOutputs(builder, List.of(resultItem));
//    }
}