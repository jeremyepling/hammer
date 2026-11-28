package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna_hammer.client.KeyInputEvents;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.CycleMiningModePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ThrowItemPayload;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Airborne Hammers slam. Grounded tools use their selected action or Auto's component fallback. */
//? if neoforge {
@EventBusSubscriber(modid = HammerMod.MOD_ID)
//?}
public class RightClickActionHandler {

    /**
     * Tracks the game tick when each player last performed a tool modification.
     * Used to prevent RightClickItem from also triggering an ability in the same tick
     * when RightClickBlock already performed a tool modification.
     */
    private static final Map<UUID, Long> recentToolModifications = new ConcurrentHashMap<>();

    //? if neoforge {
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forgetPlayer(event.getEntity());
    }

    /**
     * Handle right-click on a block.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (handleRightClick(event.getEntity(), event.getHand(), event.getPos(), event.getHitVec())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Handle right-click in air (with item in hand).
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (handleRightClick(event.getEntity(), event.getHand(), null, null)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    //? if <26.3 {
    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (attemptAirborneSlam(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
    //?}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (attemptAirborneSlam(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    //?}

    public static void beginThrow(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            if (player.getItemInHand(hand).is(HammerRegistry.HAMMER.get())) ChargedThrowPayload.begin(server, hand);
            else ThrowItemPayload.throwItem(server, hand);
        } else if (player.level().isClientSide() && player.getItemInHand(hand).is(HammerRegistry.HAMMER.get())) {
            KeyInputEvents.onUseThrow(player, hand);
        }
    }

    static void forgetPlayer(Player player) {
        recentToolModifications.remove(player.getUUID());
    }

    public static boolean attemptAirborneSlam(Player player, InteractionHand hand) {
        if (player.onGround() || !player.getItemInHand(hand).is(HammerRegistry.HAMMER.get())) return false;
        if (player instanceof ServerPlayer serverPlayer) GroundSlam.activate(serverPlayer, hand);
        return true;
    }

    static boolean allowsNeighborUse(Player player, InteractionHand hand, BlockHitResult original, BlockPos pos) {
        //? if fabric {
        /*if (player instanceof ServerPlayer serverPlayer) {
            var hit = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), original.getDirection(), pos, original.isInside());
            return FabricHammerEvents.allowsBlockUse(serverPlayer, hand, hit);
        }
        *///?}
        return true;
    }

    public static boolean handleRightClick(Player player, InteractionHand hand, @Nullable BlockPos clickedPos, @Nullable BlockHitResult hitResult) {
        if (attemptAirborneSlam(player, hand)) return true;
        ItemStack stack = player.getItemInHand(hand);
        HammerAction selected = stack.getOrDefault(HammerRegistry.HAMMER_ACTION.get(), HammerAction.AUTO);
        ServerPlayer serverPlayer = player instanceof ServerPlayer server ? server : null;
        switch (selected) {
            case SCAN -> {
                // HammerItem.useOn runs after the block gets its normal interaction.
                if (clickedPos != null || !selected.availableOn(stack)) return false;
                if (serverPlayer != null) SeismicScanner.activate(serverPlayer, hand);
                return true;
            }
            case THROW -> {
                if (!stack.has(HammerRegistry.THROWABLE_ABILITY.get())) return false;
                beginThrow(player, hand);
                return true;
            }
            case LIGHTNING -> {
                if (!selected.availableOn(stack)) return false;
                if (serverPlayer != null) {
                    BlockPos target = clickedPos != null ? clickedPos : SpawnLightningPayload.targetFromView(player);
                    SpawnLightningPayload.spawnLightningBolt(serverPlayer, hand, target);
                }
                return true;
            }
            case TELEPORT -> {
                if (!selected.availableOn(stack)) return false;
                if (serverPlayer != null) TeleportToSurfacePayload.teleportToSurface(serverPlayer);
                return true;
            }
        }

        long currentTick = player.level().getGameTime();

        // Priority 1: Try multi-tool modifications if targeting a block
        if (clickedPos != null && hitResult != null) {
            MultiToolAbilityData multiTool = stack.get(HammerRegistry.MULTI_TOOL_ABILITY.get());
            if (multiTool != null) {
                var actions = selected.toolActions(multiTool);
                if (actions.hasAnyEnabled() && ToolModifications.apply(player, hand, clickedPos, hitResult, actions, selected != HammerAction.AUTO)) {
                    // Track that we did a tool modification this tick
                    // This prevents RightClickItem from also executing the ability
                    if (serverPlayer != null) {
                        recentToolModifications.put(player.getUUID(), currentTick);
                    }
                    return true;
                }
            }
        }

        if (selected != HammerAction.AUTO) return false;

        // Check if we already did a tool modification this tick
        // (Handles case where RightClickItem fires after RightClickBlock did a tool modification)
        Long lastToolTick = recentToolModifications.get(player.getUUID());
        if (lastToolTick != null && lastToolTick == currentTick) {
            return false; // Don't run ability - tool modification already happened this tick
        }

        // Priority 2: Execute RIGHT_CLICK_ACTION ability
        RightClickAction action = stack.get(HammerRegistry.RIGHT_CLICK_ACTION.get());
        if (action == null || action.equals(RightClickAction.NONE)) {
            return false;
        }

        if (!action.abilityId().getNamespace().equals(HammerMod.MOD_ID)) return false;
        String abilityName = action.abilityId().getPath();
        if (abilityName.equals("throwable_ability") && stack.is(HammerRegistry.HAMMER.get())) {
            // On blocks HammerItem.useOn runs this fallback after the block has had its turn.
            if (clickedPos != null || !stack.has(HammerRegistry.THROWABLE_ABILITY.get())) return false;
            beginThrow(player, hand);
            return true;
        }
        if (serverPlayer == null) return false;
        switch (abilityName) {
            case "throwable_ability" -> {
                if (!stack.has(HammerRegistry.THROWABLE_ABILITY.get())) return false;
                ThrowItemPayload.throwItem(serverPlayer, hand);
            }
            case "multi_block_mining" -> {
                if (!stack.has(HammerRegistry.MULTI_BLOCK_MINING.get())) return false;
                CycleMiningModePayload.cycleMiningMode(serverPlayer, stack);
            }
            case "lightning_ability" -> {
                if (clickedPos == null || !HammerAction.LIGHTNING.availableOn(stack)) return false;
                SpawnLightningPayload.spawnLightningBolt(serverPlayer, hand, clickedPos);
            }
            case "teleport_surface_ability" -> {
                if (!HammerAction.TELEPORT.availableOn(stack)) return false;
                TeleportToSurfacePayload.teleportToSurface(serverPlayer);
            }
            default -> {
                return false; // Unknown ability, don't cancel the event
            }
        }
        return true; // Ability executed, cancel the event
    }
}
