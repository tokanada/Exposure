package io.github.mortuusars.exposure.data.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Filters {
    private static final Map<ResourceLocation, Filter> filters = new HashMap<>();

    public static Map<ResourceLocation, Filter> getFilters() {
        return filters;
    }

    public static Optional<Filter> of(ItemStack stack) {
        for (var filter : filters.values()) {
            if (filter.matches(stack))
                return Optional.of(filter);
        }

        return Optional.empty();
    }

    public static Optional<ResourceLocation> getShaderOf(ItemStack stack) {
        return of(stack).map(Filter::getShader);
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        public static final String DIRECTORY = "filters";

        // Add a constructor to receive RegistryAccess
        private final RegistryAccess registryAccess;

        public Loader(RegistryAccess registryAccess) {
            super(GSON, DIRECTORY);
            this.registryAccess = registryAccess;
        }

        public Loader() {
            this(RegistryAccess.EMPTY); // Provide a default if no RegistryAccess is given
        }


        @Override
        protected void apply(Map<ResourceLocation, JsonElement> content, ResourceManager resourceManager, ProfilerFiller profiler) {
            filters.clear();
            Exposure.LOGGER.info("Loading exposure filters:");

            // Create RegistryOps
            final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, this.registryAccess);

            for (var entry : content.entrySet()) {
                ResourceLocation key = entry.getKey();

                // Lenses should be in data/exposure/filters folder.
                // Excluding other namespaces because it potentially can cause conflicts,
                // if some other mod adds their own type of 'filter'.
                if (!key.getNamespace().equals(Exposure.ID)) {
                    continue;
                }

                JsonElement jsonElement = entry.getValue();

                // Use Filter.CODEC.parse
                Filter.CODEC.parse(ops, jsonElement)
                        .resultOrPartial(errorMsg -> Exposure.LOGGER.error("Filter '{}' was not loaded: {}", key, errorMsg))
                        .ifPresent(filter -> {
                            filters.put(key, filter);
                            Exposure.LOGGER.info("Filter [" + key + ", " + filter.getShader() + "] added.");
                        });
            }

            if (filters.isEmpty()) {
                Exposure.LOGGER.info("No filters have been loaded.");
            }
        }
    }
}