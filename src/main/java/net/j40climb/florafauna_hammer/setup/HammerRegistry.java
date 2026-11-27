package net.j40climb.florafauna_hammer.setup;

import com.mojang.serialization.Codec;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

import static net.j40climb.florafauna_hammer.common.item.hammer.HammerItem.HAMMER_MATERIAL;

public final class HammerRegistry extends PlatformRegistration {
    public static final ResourceKey<EntityType<?>> THROWN_ITEM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "thrown_item"));

    public static final Supplier<EntityType<ThrownItemEntity>> THROWN_ITEM = entity(THROWN_ITEM_KEY,
            () -> EntityType.Builder.<ThrownItemEntity>of(ThrownItemEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(THROWN_ITEM_KEY));

    public static final Supplier<Item> RESONANCE_CORE = item("resonance_core", Item::new);
    public static final Supplier<Item> TEMPEST_CORE = item("tempest_core", Item::new);

    public static final Supplier<DataComponentType<HammerStage>> HAMMER_STAGE =
            component("hammer_stage",
                    builder -> builder.persistent(HammerStage.CODEC).networkSynchronized(HammerStage.STREAM_CODEC));

    public static final Supplier<DataComponentType<MiningModeData>> MULTI_BLOCK_MINING =
            component("multi_block_mining",
                    builder -> builder.persistent(MiningModeData.CODEC).networkSynchronized(MiningModeData.STREAM_CODEC).ignoreSwapAnimation());

    public static final Supplier<DataComponentType<ToolConfig>> TOOL_CONFIG =
            component("tool_config",
                    builder -> builder.persistent(ToolConfig.CODEC).networkSynchronized(ToolConfig.STREAM_CODEC));

    public static final Supplier<DataComponentType<Unit>> LIGHTNING_ABILITY =
            component("lightning_ability",
                    builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final Supplier<DataComponentType<Unit>> TELEPORT_SURFACE_ABILITY =
            component("teleport_surface_ability",
                    builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final Supplier<DataComponentType<Integer>> HAMMER_CHARGE =
            component("hammer_charge", b -> b.persistent(Codec.intRange(0, HammerStage.TEMPEST.capacity()))
                    .networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation());

    public static final Supplier<DataComponentType<ThrowableAbilityData>> THROWABLE_ABILITY =
            component("throwable_ability",
                    builder -> builder.persistent(ThrowableAbilityData.CODEC).networkSynchronized(ThrowableAbilityData.STREAM_CODEC));

    public static final Supplier<DataComponentType<RightClickAction>> RIGHT_CLICK_ACTION =
            component("right_click_action",
                    builder -> builder.persistent(RightClickAction.CODEC).networkSynchronized(RightClickAction.STREAM_CODEC));

    public static final Supplier<DataComponentType<HammerAction>> HAMMER_ACTION =
            component("hammer_action",
                    builder -> builder.persistent(HammerAction.CODEC).networkSynchronized(HammerAction.STREAM_CODEC).ignoreSwapAnimation());

    public static final Supplier<DataComponentType<MultiToolAbilityData>> MULTI_TOOL_ABILITY =
            component("multi_tool_ability",
                    builder -> builder.persistent(MultiToolAbilityData.CODEC).networkSynchronized(MultiToolAbilityData.STREAM_CODEC));

    public static final Supplier<Item> HAMMER = item("hammer", properties ->
            new HammerItem(properties.tool(HAMMER_MATERIAL, BlockTags.MINEABLE_WITH_PICKAXE, 8, -2.4f, 0)));

    /** Initializes all declarations before the loader finishes registry setup. */
    public static void initialize() {}

    private HammerRegistry() {}
}
