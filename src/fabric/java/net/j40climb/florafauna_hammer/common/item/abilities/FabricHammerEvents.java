package net.j40climb.florafauna_hammer.common.item.abilities;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

public final class FabricHammerEvents {
    private static final Identifier USE_HAMMER = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "use_hammer");
    private static final ThreadLocal<Boolean> CHECKING_USE = new ThreadLocal<>();

    public static void register() {
        SlamCooldown.initialize();
        UseBlockCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, USE_HAMMER);
        UseBlockCallback.EVENT.register(USE_HAMMER, (player, level, hand, hit) ->
                CHECKING_USE.get() == null && !player.isSpectator()
                        && RightClickActionHandler.handleRightClick(player, hand, hit.getBlockPos(), hit)
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        UseItemCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, USE_HAMMER);
        UseItemCallback.EVENT.register(USE_HAMMER, (player, level, hand) ->
                !player.isSpectator() && RightClickActionHandler.handleRightClick(player, hand, null, null)
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        UseEntityCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, USE_HAMMER);
        UseEntityCallback.EVENT.register(USE_HAMMER, (player, level, hand, entity, hit) ->
                !player.isSpectator() && RightClickActionHandler.attemptAirborneSlam(player, hand)
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                GroundSlam.tick(player);
                ChargedThrowPayload.tick(player);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(SeismicScanner::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SeismicScanner.stop());
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer player) {
                HammerRegistry.setPendingSlam(player, null);
                RightClickActionHandler.forgetPlayer(player);
            }
        });
    }

    /** Ask other block interaction handlers about a generated tool use before changing the world. */
    public static boolean allowsBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (CHECKING_USE.get() != null) return false;
        CHECKING_USE.set(true);
        try {
            return UseBlockCallback.EVENT.invoker().interact(player, player.level(), hand, hit) == InteractionResult.PASS;
        } finally {
            CHECKING_USE.remove();
        }
    }

    private FabricHammerEvents() {}
}
