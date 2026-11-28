package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}

//? if neoforge {
@EventBusSubscriber(modid = HammerMod.MOD_ID)
//?}
public final class GroundSlam {
    public static final int CHARGE_COST = 12;
    public static final int COOLDOWN_TICKS = 100;
    public static final double RADIUS = 4.5;
    public static final float MAX_DAMAGE = 4.0F;
    public static final double MAX_KNOCKBACK = 1.6;
    private static final double DOWNWARD_SPEED = 1.8;
    private static final int TIMEOUT_TICKS = 200;
    private static final double FALL_CUSHION = 2.0;
    private static final int VIOLET = 0xC674FF;

    public record Pending(ItemStack hammer, InteractionHand hand, ResourceKey<Level> dimension, long start) {
        boolean valid(ServerPlayer player) {
            return hammer == player.getItemInHand(hand) && !hammer.isEmpty() && dimension == player.level().dimension()
                    && HammerStage.of(hammer) == HammerStage.TEMPEST
                    && player.level().getGameTime() - start <= TIMEOUT_TICKS && canDescend(player);
        }
    }

    private GroundSlam() {}

    public static boolean activate(ServerPlayer player, InteractionHand hand) {
        ItemStack hammer = player.getItemInHand(hand);
        if (!hammer.is(HammerRegistry.HAMMER.get()) || HammerStage.of(hammer) != HammerStage.TEMPEST || player.onGround()) return false;
        if (!canDescend(player) || touchesGround(player)) {
            player.sendOverlayMessage(Component.translatable("ability.hammer.slam.invalid"));
            return false;
        }
        ServerLevel level = player.level();
        long now = level.getGameTime();
        if (HammerRegistry.pendingSlam(player) != null || now < SlamCooldown.readyAt(player)) {
            player.sendOverlayMessage(Component.translatable("ability.hammer.slam.cooldown"));
            return false;
        }
        int charge = HammerItem.charge(hammer);
        if (charge < CHARGE_COST) {
            player.sendOverlayMessage(Component.translatable("ability.hammer.slam.charge", CHARGE_COST));
            return false;
        }
        hammer.set(HammerRegistry.HAMMER_CHARGE.get(), charge - CHARGE_COST);
        SlamCooldown.setReadyAt(player, now + COOLDOWN_TICKS);
        HammerRegistry.setPendingSlam(player, new Pending(hammer, hand, level.dimension(), now));
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x * 0.35, Math.min(velocity.y, -DOWNWARD_SPEED), velocity.z * 0.35);
        //? if <26.3 {
        player.hurtMarked = true; // Sync the server impulse to the controlling client.
        player.swing(hand, true);
        //?} else {
        /*player.syncVelocity = true; // Sync the server impulse to the controlling client.
        player.swing(hand, hammer.getAttackAnimation(), true);
        *///?}
        level.sendParticles(new DustParticleOptions(VIOLET, 1.2F), player.getX(), player.getY() + 0.8, player.getZ(),
                5, 0.3, 0.45, 0.3, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1F, 0.65F);
        return true;
    }

    private static boolean canDescend(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isPassenger() && !player.isInWater() && !player.isInLava() && !player.isSwimming() && !player.onClimbable();
    }

    /** A thin probe under the feet also recognizes partial-height surfaces. */
    private static boolean touchesGround(ServerPlayer player) {
        var feet = player.getBoundingBox();
        var probe = feet.setMaxY(feet.minY).expandTowards(0, -0.04, 0);
        return !player.level().noCollision(player, probe);
    }

    //? if neoforge {
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setDistance(fallDistance(player, event.getDistance()));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HammerRegistry.setPendingSlam(player, null);
    }
    //?}

    public static double fallDistance(ServerPlayer player, double distance) {
        var pending = HammerRegistry.pendingSlam(player);
        return pending != null && pending.valid(player) ? cushionedFall(distance) : distance;
    }

    static double cushionedFall(double distance) {
        return Math.max(0, distance - FALL_CUSHION);
    }

    public static void tick(ServerPlayer player) {
        var pending = HammerRegistry.pendingSlam(player);
        if (pending == null) return;
        if (!pending.valid(player)) {
            HammerRegistry.setPendingSlam(player, null);
        } else if (player.onGround() && touchesGround(player)) {
            HammerRegistry.setPendingSlam(player, null);
            impact(player);
        }
    }

    static double strength(double distance) {
        return Math.clamp(1 - distance / RADIUS, 0, 1);
    }

    private static void impact(ServerPlayer player) {
        ServerLevel level = player.level();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS))) {
            if (!(mob instanceof Enemy) || player.isAlliedTo(mob)
                    || mob instanceof OwnableEntity owned && owned.getOwnerReference() != null) continue;
            double force = strength(mob.distanceTo(player));
            if (force <= 0 || !mob.hurtServer(level, player.damageSources().playerAttack(player), (float)(MAX_DAMAGE * force))) continue;
            //? if <26.3 {
            mob.knockback(MAX_KNOCKBACK * force, player.getX() - mob.getX(), player.getZ() - mob.getZ());
            //?} else {
            /*mob.knockback(MAX_KNOCKBACK * force, player.getX() - mob.getX(), player.getZ() - mob.getZ(), player.damageSources().playerAttack(player), (float)(MAX_DAMAGE * force));
            *///?}
        }
        Vec3 center = player.position().add(0, 0.13, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.4F, 0.85F);
        var ground = level.getBlockState(player.getOnPos());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), center.x, center.y, center.z,
                    16, 0.65, 0.12, 0.65, 0.16);
        }
        // Vanilla trail particles interpolate toward their targets on each observing client.
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * 2 * Math.PI / points;
            Vec3 end = center.add(Math.cos(angle) * RADIUS, 0, Math.sin(angle) * RADIUS);
            level.sendParticles(new TrailParticleOption(end, VIOLET, 9), center.x, center.y, center.z, 1, 0, 0, 0, 0);
        }
    }
}
