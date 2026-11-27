package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SeismicScanPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerCorePayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.chunk.LevelChunk;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?}

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//? if neoforge {
@EventBusSubscriber(modid = HammerMod.MOD_ID)
//?}
public final class SeismicScanner {
    public static final int RADIUS = 24;
    public static final int HIGHLIGHT_TICKS = 100;
    public static final int CHARGE_COST = 16;
    public static final int COOLDOWN_TICKS = 160;
    public static final int MAX_DEPOSITS = 24;
    private static final int WORK_PER_TICK = 8192;
    private static final int SLICE = 512;
    private static final long TIME_BUDGET_NS = 1_500_000;
    private static final ArrayDeque<Scan> queue = new ArrayDeque<>();
    private static final Set<UUID> pending = new HashSet<>();
    private static final LinkedHashMap<UUID, Long> cooldowns = new LinkedHashMap<>();

    private record Scan(UUID owner, ServerLevel level, BlockPos origin, OreDepositSearch search) {}

    private SeismicScanner() {}

    public static boolean activate(ServerPlayer player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!HammerAction.SCAN.availableOn(stack) || !player.isAlive() || player.isSpectator() || !player.onGround()) return false;
        long now = player.level().getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (pending.contains(id) || cooldowns.getOrDefault(id, 0L) > now) {
            player.sendOverlayMessage(Component.translatable("message.hammer.scan.cooldown"));
            return false;
        }
        int charge = HammerItem.charge(stack);
        if (charge < CHARGE_COST) {
            player.sendOverlayMessage(Component.translatable("message.hammer.scan.charge", CHARGE_COST));
            return false;
        }
        stack.set(HammerRegistry.HAMMER_CHARGE.get(), charge - CHARGE_COST);
        cooldowns.putLast(id, now + COOLDOWN_TICKS);
        pending.add(id);
        var origin = player.blockPosition().immutable();
        var level = player.level();
        queue.addLast(new Scan(id, level, origin, new OreDepositSearch(origin, RADIUS, MAX_DEPOSITS)));
        HammerCorePayload.send(player, hand, HammerCorePayload.Cue.SCAN);
        HammerNetwork.sendToPlayer(player, new SeismicScanPayload(level.dimension().identifier(), origin, true, List.of()));
        return true;
    }

    //? if neoforge {
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        stop();
    }
    //?}

    static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        while (!cooldowns.isEmpty() && cooldowns.firstEntry().getValue() <= now) cooldowns.pollFirstEntry();
        if (queue.isEmpty()) return;
        long started = System.nanoTime();
        for (int work = 0; work < WORK_PER_TICK && !queue.isEmpty(); work += SLICE) {
            Scan scan = queue.removeFirst();
            ServerPlayer owner = server.getPlayerList().getPlayer(scan.owner);
            if (owner == null || owner.level() != scan.level || !owner.isAlive()) {
                pending.remove(scan.owner);
            } else if (scan.search.advance(SLICE, new LoadedOres(scan.level))) {
                pending.remove(scan.owner);
                var markers = scan.search.markers();
                HammerNetwork.sendToPlayer(owner, new SeismicScanPayload(scan.level.dimension().identifier(),
                        scan.origin, false, markers));
                owner.sendOverlayMessage(markers.isEmpty() ? Component.translatable("message.hammer.scan.empty")
                        : Component.translatable("message.hammer.scan.found", markers.size()));
            } else {
                queue.addLast(scan);
            }
            if (System.nanoTime() - started >= TIME_BUDGET_NS) break;
        }
    }

    /** Cache only within one slice so an unloaded chunk can never be retained across ticks. */
    static final class LoadedOres implements OreDepositSearch.Sampler {
        private final ServerLevel level;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
        private LevelChunk chunk;

        LoadedOres(ServerLevel level) {
            this.level = level;
        }

        @Override
        public boolean isOre(int x, int y, int z) {
            if (level.isOutsideBuildHeight(y)) return false;
            if (x >> 4 != chunkX || z >> 4 != chunkZ) {
                chunkX = x >> 4;
                chunkZ = z >> 4;
                chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            }
            return chunk != null && chunk.getBlockState(cursor.set(x, y, z)).is(OreTags.ORES);
        }
    }

    static void stop() {
        queue.clear();
        pending.clear();
        cooldowns.clear();
    }
}
