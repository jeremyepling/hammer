package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerCorePayload;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

final class HammerLightning {
    private HammerLightning() {}

    static BlockPos targetFromView(ServerPlayer player) {
        return BlockPos.containing(player.pick(10.0, 1.0F, false).getLocation());
    }

    static void spawnLightningBolt(ServerPlayer player, InteractionHand hand, BlockPos targetPos) {
        ServerLevel serverLevel = player.level();
        if (!HammerAction.LIGHTNING.availableOn(player.getItemInHand(hand)) || !serverLevel.hasChunkAt(targetPos)) return;
        //? if <26.3 {
        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        //?} else {
        /*LightningBolt lightningBolt = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        *///?}
        if (lightningBolt != null) {
            lightningBolt.setPos(Vec3.atBottomCenterOf(targetPos));
            if (serverLevel.addFreshEntity(lightningBolt) && player.getItemInHand(hand).is(HammerRegistry.HAMMER.get())) {
                HammerCorePayload.send(player, hand, HammerCorePayload.Cue.LIGHTNING);
            }
        }
    }
}
