package net.j40climb.florafauna_hammer.client;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerCorePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerCorePayload.Cue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HammerAbilityFeedback {
    private static final DustParticleOptions VIOLET = new DustParticleOptions(0xBF87FA, 0.6F);
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private HammerAbilityFeedback() {}

    public static void receive(HammerCorePayload packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(packet.playerId()) instanceof Player player)) return;
        switch (packet.cue()) {
            case READY, RELEASE, MODE, SCAN, LIGHTNING -> burst(level, player, packet.hand(), packet.cue());
            default -> { } // Older servers also sent cues for the animated item model.
        }
    }

    private static Vec3 handPosition(Player p, InteractionHand hand) {
        boolean right = (p.getMainArm() == HumanoidArm.RIGHT) == (hand == InteractionHand.MAIN_HAND);
        var mc = Minecraft.getInstance();
        //? if <26.3 {
        var camera = mc.gameRenderer.getMainCamera();
        //?} else {
        /*var camera = mc.gameRenderer.mainCamera();
        *///?}
        if (camera.entity() == p && mc.options.getCameraType().isFirstPerson()) {
            // Particles live in world space; project the held Hammer's screen offset onto a near plane.
            double depth = 0.85;
            double halfHeight = depth * Math.tan(Math.toRadians(camera.getFov() * 0.5));
            return camera.position().add(new Vec3(camera.forwardVector()).scale(depth))
                    .add(new Vec3(camera.leftVector()).scale((right ? -1.04 : 1.04) * halfHeight))
                    .add(new Vec3(camera.upVector()).scale(-0.28 * halfHeight));
        }
        Vec3 across = p.calculateViewVector(0, p.getYRot()).cross(UP);
        return p.getEyePosition().add(p.getViewVector(1).scale(0.54)).add(across.scale(right ? 0.36 : -0.36)).add(0, -0.36, 0);
    }

    private static void burst(ClientLevel level, Player player, InteractionHand hand, Cue cue) {
        Vec3 pos = handPosition(player, hand);
        if (cue == Cue.MODE) {
            if (player == Minecraft.getInstance().player) {
                level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.16F, 1.5F, false);
            }
            return;
        }
        if (cue == Cue.READY) {
            level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.48F, 1.4F, false);
        }
        Vec3 forward = player.getViewVector(1);
        int count = cue == Cue.RELEASE ? 6 : 3;
        for (int i = 0; i < count; i++) {
            Vec3 at = pos.add(forward.scale((cue == Cue.READY ? 0 : 0.15) * i));
            Vec3 drift = forward.scale(0.05 + i * 0.016);
            level.addParticle(cue == Cue.LIGHTNING || cue == Cue.READY ? ParticleTypes.ELECTRIC_SPARK : VIOLET,
                    at.x, at.y + level.getRandom().nextDouble() * 0.07, at.z, drift.x, drift.y + 0.02, drift.z);
        }
    }

}
