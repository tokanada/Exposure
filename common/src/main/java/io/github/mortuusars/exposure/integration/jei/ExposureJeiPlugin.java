package io.github.mortuusars.exposure.integration.jei;

import com.google.common.collect.ImmutableList;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.gui.screen.ItemRenameScreen;
import io.github.mortuusars.exposure.gui.screen.album.AlbumScreen;
import io.github.mortuusars.exposure.integration.jei.category.PhotographPrintingCategory;
import io.github.mortuusars.exposure.integration.jei.category.PhotographStackingCategory;
import io.github.mortuusars.exposure.integration.jei.recipe.NbtTransferringShapelessExtension;
import io.github.mortuusars.exposure.integration.jei.recipe.PhotographPrintingJeiRecipe;
import io.github.mortuusars.exposure.integration.jei.recipe.PhotographStackingJeiRecipe;
import io.github.mortuusars.exposure.recipe.FilmDevelopingRecipe;
import io.github.mortuusars.exposure.recipe.PhotographAgingRecipe;
import io.github.mortuusars.exposure.recipe.PhotographCopyingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
// mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension; // Not directly used in this signature
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
// java.util.function.Function; // Not directly used with method references here

@JeiPlugin
public class ExposureJeiPlugin implements IModPlugin {
    public static final RecipeType<PhotographPrintingJeiRecipe> PHOTOGRAPH_PRINTING_RECIPE_TYPE =
            RecipeType.create(Exposure.ID, "photograph_printing", PhotographPrintingJeiRecipe.class);
    public static final RecipeType<PhotographStackingJeiRecipe> PHOTOGRAPH_STACKING_RECIPE_TYPE =
            RecipeType.create(Exposure.ID, "photograph_stacking", PhotographStackingJeiRecipe.class);

    private static final ResourceLocation ID = Exposure.resource("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new PhotographPrintingCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PhotographStackingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Exposure.Items.LIGHTROOM.get()), PHOTOGRAPH_PRINTING_RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Exposure.Items.STACKED_PHOTOGRAPHS.get()), PHOTOGRAPH_STACKING_RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registration.addRecipes(PHOTOGRAPH_PRINTING_RECIPE_TYPE, ImmutableList.of(
                new PhotographPrintingJeiRecipe(FilmType.BLACK_AND_WHITE),
                new PhotographPrintingJeiRecipe(FilmType.COLOR)
        ));

        registration.addRecipes(PHOTOGRAPH_STACKING_RECIPE_TYPE, ImmutableList.of(
                new PhotographStackingJeiRecipe(PhotographStackingJeiRecipe.STACKING),
                new PhotographStackingJeiRecipe(PhotographStackingJeiRecipe.REMOVING)
        ));

        if (Config.Client.SHOW_JEI_INFORMATION.get()) {
            registration.addItemStackInfo(new ItemStack(Exposure.Items.PHOTOGRAPH_FRAME.get()),
                    Component.translatable("exposure.jei.info.photograph_frame"));
        }

        if (PlatformHelper.isModLoaded("create")) {
            addSequencedDevelopingRecipes(registration);
        }
    }

    @ExpectPlatform
    public static void addSequencedDevelopingRecipes(@NotNull IRecipeRegistration registration) {
        // This is an @ExpectPlatform method, its implementation is platform-specific.
        throw new AssertionError("This should be implemented by the platform-specific module.");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        // This assumes NbtTransferringShapelessExtension is generic and correctly implements ICraftingCategoryExtension<R>:
        // public record NbtTransferringShapelessExtension<R extends AbstractNbtTransferringRecipe>(R recipe)
        //       implements ICraftingCategoryExtension<R>
        //
        // Using method references for the factory function.
        registration.getCraftingCategory()
                .addExtension(FilmDevelopingRecipe.class, new NbtTransferringShapelessExtension());
        registration.getCraftingCategory()
                .addExtension(PhotographCopyingRecipe.class, new NbtTransferringShapelessExtension());
        registration.getCraftingCategory()
                .addExtension(PhotographAgingRecipe.class, new NbtTransferringShapelessExtension());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AlbumScreen.class, new IGuiContainerHandler<AlbumScreen>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull AlbumScreen containerScreen) {
                return List.of(new Rect2i(0, 0,
                        Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                        Minecraft.getInstance().getWindow().getGuiScaledHeight())); // Corrected to getGuiHeight
            }
        });

        registration.addGenericGuiContainerHandler(ItemRenameScreen.class, new IGuiContainerHandler<ItemRenameScreen>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull ItemRenameScreen containerScreen) {
                return List.of(new Rect2i(0, 0,
                        Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                        Minecraft.getInstance().getWindow().getGuiScaledHeight())); // Corrected to getGuiHeight
            }
        });
    }
}
