package net.j40climb.florafauna_hammer.common.item.abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record MiningModeData(MiningShape shape, int radius, int maxBlocksToBreak, boolean ignoreToolRestrictions, int favorites) {
    private static final int ALL_SHAPES = (1 << MiningShape.values().length) - 1;

    public MiningModeData(MiningShape shape, int radius, int maxBlocksToBreak, boolean ignoreToolRestrictions) {
        this(shape, radius, maxBlocksToBreak, ignoreToolRestrictions, 0);
    }
    // Normalize existing saved and networked components before iterating their bounds.
    public MiningModeData {
        favorites &= ALL_SHAPES;
        radius = Math.clamp(radius, 0, 3);
        maxBlocksToBreak = Math.clamp(maxBlocksToBreak, 1, 64);
    }

    public static final Codec<MiningModeData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    MiningShape.CODEC.fieldOf("shape").forGetter(MiningModeData::shape),
                    Codec.INT.fieldOf("radius").forGetter(MiningModeData::radius),
                    Codec.INT.fieldOf("maxBlocksToBreak").forGetter(MiningModeData::maxBlocksToBreak),
                    Codec.BOOL.optionalFieldOf("ignoreToolRestrictions", true).forGetter(MiningModeData::ignoreToolRestrictions),
                    Codec.INT.optionalFieldOf("favorites", 0).forGetter(MiningModeData::favorites)
            ).apply(builder, MiningModeData::new));

    public static final StreamCodec<ByteBuf, MiningModeData> STREAM_CODEC = StreamCodec.composite(
            MiningShape.STREAM_CODEC, MiningModeData::shape,
            ByteBufCodecs.INT, MiningModeData::radius,
            ByteBufCodecs.INT, MiningModeData::maxBlocksToBreak,
            ByteBufCodecs.BOOL, MiningModeData::ignoreToolRestrictions,
            ByteBufCodecs.VAR_INT, MiningModeData::favorites,
            MiningModeData::new
    );

    public static final MiningModeData DEFAULT = new MiningModeData(MiningShape.SINGLE, 0, 64, true);

    /** Apply stage limits at use time too, including for stacks configured by commands. */
    public static MiningModeData effective(ItemStack stack) {
        MiningModeData mode = stack.getOrDefault(HammerRegistry.MULTI_BLOCK_MINING.get(), DEFAULT);
        HammerStage stage = HammerStage.of(stack);
        if (!stage.allows(mode.shape)) return mode.withShape(MiningShape.SINGLE);
        if (stage == HammerStage.FORGED && mode.shape == MiningShape.FLAT_3X3 && mode.radius > mode.shape.getRadius()) {
            return mode.withShape(mode.shape);
        }
        return mode;
    }

    public MiningModeData getNextMode() {
        return getNextMode(HammerStage.TEMPEST);
    }

    public MiningModeData getNextMode(HammerStage stage) {
        var shapes = MiningShape.values();
        int index = shape.ordinal();
        MiningShape firstAvailable = null;
        for (int step = 0; step < shapes.length; step++) {
            index = (index + 1) % shapes.length;
            MiningShape candidate = shapes[index];
            if (!stage.allows(candidate)) continue;
            if (favorites == 0 || isFavorite(candidate)) return withShape(candidate);
            if (firstAvailable == null) firstAvailable = candidate;
        }
        // All stored favorites are locked. Cycle available shapes until a favorite unlocks.
        return withShape(firstAvailable);
    }

    public MiningModeData withShape(MiningShape selected) {
        return new MiningModeData(selected, selected.getRadius(), maxBlocksToBreak, ignoreToolRestrictions, favorites);
    }

    public boolean isFavorite(MiningShape candidate) {
        return (favorites & (1 << candidate.id())) != 0;
    }

    public MiningModeData toggleFavorite(MiningShape candidate) {
        return new MiningModeData(shape, radius, maxBlocksToBreak, ignoreToolRestrictions, favorites ^ (1 << candidate.id()));
    }

    public Component modeDescription() {
        return Component.translatable("tooltip.florafauna.hammer.mode",
                Component.translatable("mining_shape.florafauna." + shape.getSerializedName()));
    }
}
