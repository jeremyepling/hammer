package net.j40climb.florafauna_hammer.client;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockOutlineRenderer;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockVisualFeedback;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerCorePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SeismicScanPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = HammerMod.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeHammerClientEvents {
    @SubscribeEvent
    public static void networking(RegisterClientPayloadHandlersEvent event) {
        event.register(HammerCorePayload.TYPE, (packet, context) -> HammerAbilityFeedback.receive(packet));
        event.register(SeismicScanPayload.TYPE, (packet, context) -> SeismicScanEffects.receive(packet));
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "ore_echoes"), SeismicScanEffects::drawMarkers);
        event.registerAboveAll(Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "mining_preview"), MultiBlockOutlineRenderer::drawHud);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        KeyInputEvents.onClientTick();
        MultiBlockVisualFeedback.onClientTick();
        SeismicScanEffects.tick();
    }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        KeyInputEvents.onControlKey(event.getKeyEvent(), event.getAction());
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Post event) {
        KeyInputEvents.onControlMouse(event.getButton(), event.getAction());
    }

    @SubscribeEvent
    public static void mining(PlayerInteractEvent.LeftClickBlock event) {
        var player = event.getEntity();
        if (!player.level().isClientSide()) return;
        switch (event.getAction()) {
            case START -> MultiBlockVisualFeedback.start(player, event.getPos(), event.getFace());
            case CLIENT_HOLD -> MultiBlockVisualFeedback.hold(player, event.getPos());
            case STOP, ABORT -> MultiBlockVisualFeedback.clear(player);
        }
    }

    @SubscribeEvent
    public static void pipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(MultiBlockOutlineRenderer.PREVIEW_PIPELINE);
    }

    @SubscribeEvent
    public static void outlines(ExtractBlockOutlineRenderStateEvent event) {
        var preview = MultiBlockOutlineRenderer.extract(event.getLevel(), event.getHitResult(), event.getCollisionContext());
        if (preview == null) return;
        //? if <26.3 {
        event.addCustomRenderer((state, buffers, pose, translucent, level) -> {
            if (translucent == state.isTranslucent()) MultiBlockOutlineRenderer.draw(preview, buffers, pose, level.cameraRenderState.pos);
            return false;
        });
        //?} else {
        /*event.addCustomRenderer((state, collector, pose, level) -> {
            MultiBlockOutlineRenderer.draw(preview, collector, pose, level.cameraRenderState.pos, state.isTranslucent());
            return false;
        });
        *///?}
    }

    private NeoForgeHammerClientEvents() {}
}
