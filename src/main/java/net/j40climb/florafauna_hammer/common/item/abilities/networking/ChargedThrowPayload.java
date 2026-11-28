package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record ChargedThrowPayload(int action) implements CustomPacketPayload {
    public static final int PRESS = 0, RELEASE = 1, CANCEL = 2;
    public static final int HOLD_TICKS = 20;
    public static final Type<ChargedThrowPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "charged_throw"));
    public static final StreamCodec<ByteBuf, ChargedThrowPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ChargedThrowPayload::action, ChargedThrowPayload::new);

    public record Hold(ItemStack hammer, InteractionHand hand, int slot, ResourceKey<Level> dimension, long start) {
        boolean valid(ServerPlayer player) {
            return player.isAlive() && !player.isSpectator() && dimension == player.level().dimension()
                    && slot == player.getInventory().getSelectedSlot() && hammer == player.getItemInHand(hand)
                    && hammer.has(HammerRegistry.THROWABLE_ABILITY.get());
        }
    }

    public static void handle(ChargedThrowPayload request, ServerPlayer player) {
        key(player, request.action);
    }

    public static void key(ServerPlayer player, int action) {
        if (action == PRESS) {
            begin(player, InteractionHand.MAIN_HAND);
        } else if (action == CANCEL || action == RELEASE) {
            var hold = HammerRegistry.throwHold(player);
            HammerRegistry.setThrowHold(player, null);
            if (action == RELEASE && hold != null && hold.valid(player)) {
                boolean charged = HammerStage.of(hold.hammer) != HammerStage.FORGED
                        && player.level().getGameTime() - hold.start >= HOLD_TICKS;
                ThrowItemPayload.throwItem(player, hold.hand, charged);
            }
        }
    }

    public static void begin(ServerPlayer player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (HammerRegistry.throwHold(player) == null && player.isAlive() && !player.isSpectator()
                && stack.is(HammerRegistry.HAMMER.get()) && stack.has(HammerRegistry.THROWABLE_ABILITY.get())) {
            HammerRegistry.setThrowHold(player, new Hold(stack, hand, player.getInventory().getSelectedSlot(),
                    player.level().dimension(), player.level().getGameTime()));
        }
    }

    public static void tick(ServerPlayer player) {
        var hold = HammerRegistry.throwHold(player);
        if (hold == null) return;
        if (!hold.valid(player)) {
            HammerRegistry.setThrowHold(player, null);
            return;
        }
        if (HammerStage.of(hold.hammer) == HammerStage.FORGED) return;
        long age = player.level().getGameTime() - hold.start;
        if (age > 0 && age % 4 == 0) HammerCorePayload.send(player, hold.hand, HammerCorePayload.Cue.CHARGE);
        if (age == HOLD_TICKS) {
            player.sendOverlayMessage(Component.translatable("ability.hammer.ore_strike.ready"));
            HammerCorePayload.send(player, hold.hand, HammerCorePayload.Cue.READY);
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
