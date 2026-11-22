package net.j40climb.florafauna_hammer.common.item.abilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Common ore tags populated by both loaders and ore-providing mods. */
public final class OreTags {
    public static final TagKey<Block> ORES = tag("ores");
    public static final TagKey<Block> STONE = tag("ores_in_ground/stone");
    public static final TagKey<Block> DEEPSLATE = tag("ores_in_ground/deepslate");

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
    }

    private OreTags() {}
}
