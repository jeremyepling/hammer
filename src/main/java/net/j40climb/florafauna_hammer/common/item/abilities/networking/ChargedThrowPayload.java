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

    public record Hold(ItemStack hammer, ResourceKey<Level> dimension, long start) {}

    public static void handle(ChargedThrowPayload request, ServerPlayer player) {
        key(player, request.action);
    }

    public static void key(ServerPlayer player, int action) {
        ItemStack stack = player.getMainHandItem();
        var hold = HammerRegistry.throwHold(player);
        long now = player.level().getGameTime();
        if (action == PRESS) {
            if (hold == null && stack.is(HammerRegistry.HAMMER.get()) && stack.has(HammerRegistry.THROWABLE_ABILITY.get())
                    && player.isAlive() && !player.isSpectator()) {
                HammerRegistry.setThrowHold(player, new Hold(stack, player.level().dimension(), now));
            }
        } else {
            HammerRegistry.setThrowHold(player, null);
            if (action == RELEASE && hold != null && hold.hammer == stack && hold.dimension == player.level().dimension() && player.isAlive() && !player.isSpectator()) {
                boolean charged = HammerStage.of(stack) != HammerStage.FORGED && now - hold.start >= HOLD_TICKS;
                ThrowItemPayload.throwItem(player, InteractionHand.MAIN_HAND, charged);
            }
        }
    }

    public static void tick(ServerPlayer player) {
        var hold = HammerRegistry.throwHold(player);
        if (hold == null) return;
        if (hold.hammer != player.getMainHandItem() || hold.dimension != player.level().dimension() || !player.isAlive() || player.isSpectator()) {
            HammerRegistry.setThrowHold(player, null);
        } else if (HammerStage.of(hold.hammer) != HammerStage.FORGED && player.level().getGameTime() - hold.start == HOLD_TICKS) {
            player.sendOverlayMessage(Component.translatable("ability.hammer.ore_strike.ready"));
            HammerCorePayload.send(player, InteractionHand.MAIN_HAND, HammerCorePayload.Cue.READY);
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
