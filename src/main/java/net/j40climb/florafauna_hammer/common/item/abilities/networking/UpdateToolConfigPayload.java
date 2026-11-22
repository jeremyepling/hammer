package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Network packet to update tool configuration.
 * Sent from client to server when the player changes config settings.
 * Works with any item that has the TOOL_CONFIG data component.
 */
public record UpdateToolConfigPayload(int slot, ToolConfig config) implements CustomPacketPayload {

    public static final Type<UpdateToolConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "update_tool_config")
    );

    public static final StreamCodec<ByteBuf, UpdateToolConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdateToolConfigPayload::slot,
            ToolConfig.STREAM_CODEC,
            UpdateToolConfigPayload::config,
            UpdateToolConfigPayload::new
    );

    public static void onServerReceived(final UpdateToolConfigPayload data, final ServerPlayer player) {
        if (player.getInventory().getSelectedSlot() == data.slot) {
            applyToHeldItem(player, data.config);
        }
    }

    public static void applyToHeldItem(Player player, ToolConfig config) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.has(HammerRegistry.TOOL_CONFIG.get()) || !HammerStage.of(stack).allows(config.miningSpeed())) return;
        stack.set(HammerRegistry.TOOL_CONFIG.get(), config);
        var registries = player.level().registryAccess();
        Holder<Enchantment> fortune = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> silk = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> {
            enchantments.set(fortune, config.fortune() ? 3 : 0);
            enchantments.set(silk, config.silkTouch() ? 1 : 0);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
