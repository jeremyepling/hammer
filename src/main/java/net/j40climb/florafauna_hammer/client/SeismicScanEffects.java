package net.j40climb.florafauna_hammer.client;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.SeismicScanner;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SeismicScanPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SeismicScanEffects {
    private static final int PULSE_TICKS = 16;
    private static final DustParticleOptions VIOLET = new DustParticleOptions(0xB379FF, 0.85F);
    private static final DustParticleOptions LILAC = new DustParticleOptions(0xE2BEFF, 0.65F);
    private static ClientLevel world;
    private static Vec3 origin = Vec3.ZERO;
    private static long pulseStart;
    private static long markerStart;
    private static List<BlockPos> markers = List.of();

    public static void receive(SeismicScanPayload scan) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().identifier().equals(scan.dimension())) return;
        world = level;
        origin = Vec3.atBottomCenterOf(scan.origin()).add(0, 0.23, 0);
        if (scan.started()) {
            pulseStart = level.getGameTime();
            markers = List.of();
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 1.1F, 0.62F, false);
        } else {
            markerStart = level.getGameTime();
            markers = scan.markers();
        }
    }

    public static void tick() {
        if (world == null) return;
        if (Minecraft.getInstance().level != world) {
            world = null;
            markers = List.of();
            return;
        }
        long now = world.getGameTime();
        long age = now - pulseStart;
        if (now - markerStart >= SeismicScanner.HIGHLIGHT_TICKS) markers = List.of();
        // Sweep the private search radius with a thinning ring.
        if (age < 3 || age >= 3 + PULSE_TICKS || age % 2 == 0) return;
        double radius = (age - 1) * (double) SeismicScanner.RADIUS / PULSE_TICKS;
        for (int i = 0; i < 16; i++) {
            double angle = (i + 0.37 * age) * Math.TAU / 16;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            world.addParticle(i % 4 == 0 ? LILAC : VIOLET, origin.x + radius * dx, origin.y, origin.z + radius * dz,
                    dx * 0.15, 0.015, dz * 0.15);
        }
    }

    public static void drawMarkers(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        //? if <26.3 {
        if (markers.isEmpty() || mc.level != world || mc.options.hideGui) return;
        //?} else {
        /*if (markers.isEmpty() || mc.level != world || mc.gui.hud.isHidden()) return;
        *///?}
        float age = world.getGameTime() - markerStart + delta.getGameTimeDeltaPartialTick(false);
        int alpha = Math.clamp((int) ((1 - age / SeismicScanner.HIGHLIGHT_TICKS) * 225), 0, 225);
        if (alpha < 4) return;
        int color = alpha << 24 | 0xD1A6FF;
        int halo = (alpha / 3) << 24 | 0xA667FC;
        //? if <26.3 {
        var camera = mc.gameRenderer.getMainCamera();
        //?} else {
        /*var camera = mc.gameRenderer.mainCamera();
        *///?}
        var forward = camera.forwardVector();
        for (var block : markers) {
            // The server sends the lower corner of a two-block cell.
            Vec3 point = Vec3.atLowerCornerOf(block).add(1, 1, 1);
            Vec3 offset = point.subtract(camera.position());
            if (offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() <= 0.1) continue;
            Vec3 projected = mc.gameRenderer.projectPointToScreen(point);
            if (Math.abs(projected.x) > 1 || Math.abs(projected.y) > 1) continue;
            int x = (int) ((projected.x + 1) * graphics.guiWidth() / 2);
            int y = (int) ((1 - projected.y) * graphics.guiHeight() / 2);
            int r = 3 + (int) Math.round(Math.sin(age * 0.19));
            graphics.fill(x - 2, y - 2, x + 3, y + 3, halo);
            graphics.fill(x, y, x + 1, y + 1, color);
            graphics.fill(x - 1, y - r, x + 2, y - r + 1, color);
            graphics.fill(x - 1, y + r, x + 2, y + r + 1, color);
            graphics.fill(x - r, y - 1, x - r + 1, y + 2, color);
            graphics.fill(x + r, y - 1, x + r + 1, y + 2, color);
        }
    }

    private SeismicScanEffects() {}
}
