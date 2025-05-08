package io.github.mortuusars.exposure.camera.infrastructure;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement; // Keep for potential interop if needed, but Codec is primary
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.data.Lenses; // Assuming this is for ofStack, not directly Codec related
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class FocalRange implements StringRepresentable {

    public static final int ALLOWED_MIN = 10;
    public static final int ALLOWED_MAX = 300;

    private final int min;
    private final int max;

    public FocalRange(int min, int max) {
        Preconditions.checkArgument(ALLOWED_MIN <= min && min <= ALLOWED_MAX,
                min + " is not in allowed range for 'min' [" + ALLOWED_MIN + "-" + ALLOWED_MAX + "].");
        Preconditions.checkArgument(ALLOWED_MIN <= max && max <= ALLOWED_MAX,
                max + " is not in allowed range for 'max' [" + ALLOWED_MIN + "-" + ALLOWED_MAX + "].");
        Preconditions.checkArgument(min <= max,
                "'min' should not be larger than 'max'. min: " + min + ", max: " + max);
        this.min = min;
        this.max = max;
    }

    public FocalRange(int fixedValue) {
        this(fixedValue, fixedValue); // Delegate to the main constructor for validation
    }

    // --- Network serialization ---
    public static FocalRange fromNetwork(FriendlyByteBuf buffer) {
        int min = buffer.readInt();
        int max = buffer.readInt();
        return new FocalRange(min, max);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(min);
        buffer.writeInt(max);
    }

    // --- Logic ---
    public boolean isPrime() {
        return min == max;
    }

    // --- Static helpers / defaults ---
    public static FocalRange ofStack(ItemStack stack) {
        if (stack.isEmpty())
            return getDefault();

        if (!stack.is(Exposure.Tags.Items.LENSES)) {
            // Exposure.LOGGER.error(stack + " is not a valid lens. Should have '#exposure:lenses' tag."); // Keep logging if desired
            return getDefault();
        }
        return Lenses.getFocalRangeOf(stack).orElse(getDefault());
    }

    public static @NotNull FocalRange getDefault() {
        // Assuming Config.Common.CAMERA_DEFAULT_FOCAL_RANGE.get() returns a string like "55" or "35-100"
        return parse(Config.Common.CAMERA_DEFAULT_FOCAL_RANGE.get());
    }

    // --- StringRepresentable ---
    @Override
    public @NotNull String getSerializedName() {
        return isPrime() ? Integer.toString(min) : min + "-" + max;
    }

    public static FocalRange parse(String value) {
        int dashIndex = value.indexOf("-");
        if (dashIndex == -1) {
            int prime = Integer.parseInt(value);
            return new FocalRange(prime);
        }

        int minVal = Integer.parseInt(value.substring(0, dashIndex));
        int maxVal = Integer.parseInt(value.substring(dashIndex + 1));
        return new FocalRange(minVal, maxVal);
    }

    // --- Codec Definition ---
    // This codec handles the object form: {"min": X, "max": Y}
    // OR the single number form for fixed values.
    private static final Codec<FocalRange> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min").forGetter(FocalRange::min),
            Codec.INT.fieldOf("max").forGetter(FocalRange::max)
    ).apply(instance, FocalRange::new));

    public static final Codec<FocalRange> CODEC = Codec.either(Codec.INT, OBJECT_CODEC)
            .comapFlatMap(
                    either -> either.map(
                            fixedValue -> {
                                try {
                                    return DataResult.success(new FocalRange(fixedValue));
                                } catch (IllegalArgumentException e) {
                                    return DataResult.error(() -> "FocalRange from single int: " + e.getMessage());
                                }
                            },
                            objectFocalRange -> DataResult.success(objectFocalRange)
                    ),
                    focalRange -> {
                        if (focalRange.isPrime()) {
                            return com.mojang.datafixers.util.Either.left(focalRange.min());
                        } else {
                            return com.mojang.datafixers.util.Either.right(focalRange);
                        }
                    }
            );


    // fromJson is now replaced by using FocalRange.CODEC.parse(...)
    // public static FocalRange fromJson(@Nullable JsonElement json) { ... }


    // --- Getters and Object overrides ---
    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FocalRange) obj;
        return this.min == that.min && this.max == that.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        if (isPrime())
            return "FocalRange[fixed=" + min + ']';
        else
            return "FocalRange[min=" + min + ", max=" + max + ']';
    }
}