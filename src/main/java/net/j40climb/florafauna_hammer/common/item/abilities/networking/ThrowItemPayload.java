package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemEntity;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * Payload sent from client to server when the throw key is pressed.
 * Spawns a ThrownItemEntity from the held item if it has the THROWABLE_ABILITY component.
 */
public record ThrowItemPayload(boolean mainHand) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ThrowItemPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "throw_item"));

    public static final StreamCodec<ByteBuf, ThrowItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ThrowItemPayload::mainHand,
            ThrowItemPayload::new
    );

    public static void onServerReceived(final ThrowItemPayload data, final ServerPlayer player) {
        InteractionHand hand = data.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        throwItem(player, hand);
    }

    /**
     * Throws the item in the specified hand.
     * Can be called directly from items or via network payload.
     */
    public static void throwItem(ServerPlayer player, InteractionHand hand) {
        throwItem(player, hand, false);
    }

    static void throwItem(ServerPlayer player, InteractionHand hand, boolean charged) {
        ItemStack stack = player.getItemInHand(hand);

        // Check if item has throwable ability
        if (!stack.has(HammerRegistry.THROWABLE_ABILITY.get())) {
            return;
        }

        ThrowableAbilityData abilityData = stack.get(HammerRegistry.THROWABLE_ABILITY.get());
        if (abilityData == null) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Check if player already has a thrown item of this type in the world
        boolean alreadyThrown = level.getEntities(
                HammerRegistry.THROWN_ITEM.get(),
                entity -> entity.getOwner() == player && ItemStack.isSameItem(entity.getThrownItem(), stack)
        ).stream().findAny().isPresent();

        if (alreadyThrown) {
            return; // Can't throw another until the previous one returns
        }

        // Create the thrown entity
        ThrownItemEntity entity = new ThrownItemEntity(level, player, stack, abilityData);

        if (charged) entity.armOreStrike();

        // Launch in direction player is looking
        var aim = player.getLookAngle();
        entity.shoot(aim.x, aim.y, aim.z, 2.5f, 0.0f);

        // Add entity to world
        if (!level.addFreshEntity(entity)) return;
        if (charged) HammerCorePayload.send(player, hand, HammerCorePayload.Cue.RELEASE);
        //? if >=26.3 {
        /*player.swing(hand, stack.getAttackAnimation(), false);
        *///?}

        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Remove item from hand (will return via entity or pickup)
        if (entity.pickup == AbstractArrow.Pickup.ALLOWED) {
            stack.shrink(1);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
