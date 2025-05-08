package io.github.mortuusars.exposure.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
// JsonObject is no longer needed for direct manipulation here
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Ingredient;
import com.mojang.serialization.JsonOps;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LensesDataLoader extends SimpleJsonResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "lenses";

    // Helper record for the structure of each lens JSON entry
    private record LensJsonEntry(Ingredient item, FocalRange focalRange) {
        public static final Codec<LensJsonEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("item").forGetter(LensJsonEntry::item),
                FocalRange.CODEC.fieldOf("focal_range").forGetter(LensJsonEntry::focalRange)
        ).apply(instance, LensJsonEntry::new));
    }

    private final RegistryAccess registryAccess;

    // Constructor now takes RegistryAccess
    public LensesDataLoader(RegistryAccess registryAccess) {
        super(GSON, DIRECTORY); // GSON is still used by SimpleJsonResourceReloadListener for initial load
        this.registryAccess = registryAccess;
    }

    // Default constructor for convenience, logs a warning.
    public LensesDataLoader() {
        this(RegistryAccess.EMPTY);
        if (this.registryAccess == RegistryAccess.EMPTY) {
            Exposure.LOGGER.warn("LensesDataLoader initialized with RegistryAccess.EMPTY. Ingredient tags might not be fully resolved.");
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> content, ResourceManager resourceManager, ProfilerFiller profiler) {
        ConcurrentMap<Ingredient, FocalRange> loadedLenses = new ConcurrentHashMap<>();

        Exposure.LOGGER.info("Loading exposure lenses:");

        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, this.registryAccess);

        for (var entry : content.entrySet()) {
            ResourceLocation key = entry.getKey();

            if (!key.getNamespace().equals(Exposure.ID)) {
                continue;
            }

            JsonElement jsonElement = entry.getValue();

            LensJsonEntry.CODEC.parse(ops, jsonElement)
                    .resultOrPartial(errorMsg -> Exposure.LOGGER.error("Lens '{}' was not loaded: {}", key, errorMsg))
                    .ifPresent(lensData -> {
                        if (lensData.item().isEmpty()) {
                            Exposure.LOGGER.error("Lens '{}' was not loaded: 'item' ingredient cannot be empty.", key);
                            return;
                        }
                        loadedLenses.put(lensData.item(), lensData.focalRange());
                        Exposure.LOGGER.info("Lens [" + key + ", " + lensData.focalRange().toString() + "] added.");
                    });
        }

        if (loadedLenses.isEmpty()) {
            Exposure.LOGGER.info("No lenses have been loaded.");
        }

        Lenses.reload(loadedLenses); // Assumes Lenses.reload() is correctly implemented
    }
}