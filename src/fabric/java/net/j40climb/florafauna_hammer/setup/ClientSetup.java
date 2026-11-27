package net.j40climb.florafauna_hammer.setup;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.j40climb.florafauna_hammer.client.*;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockOutlineRenderer;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockVisualFeedback;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerCorePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SeismicScanPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ClientSetup implements net.fabricmc.api.ClientModInitializer {

    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(HammerKey.CATEGORY_ID);

    static KeyMapping createKey(String translationKey, int defaultKey) {
        return new KeyMapping(translationKey, defaultKey, KEY_CATEGORY);
    }

    public static void submit(net.minecraft.client.gui.GuiGraphicsExtractor gui, net.minecraft.client.renderer.state.gui.GuiElementRenderState state) {
        gui.guiRenderState.addGuiElement(state);
    }

    public static InputConstants.Key boundKey(KeyMapping mapping) {
        return KeyMappingHelper.getBoundKeyOf(mapping);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public void onInitializeClient() {
        HammerKey.registerAll(KeyMappingHelper::registerKeyMapping);
        EntityRendererRegistry.register(HammerRegistry.THROWN_ITEM.get(), ThrownItemRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(HammerCorePayload.TYPE, (packet, context) -> HammerAbilityFeedback.receive(packet));
        ClientPlayNetworking.registerGlobalReceiver(SeismicScanPayload.TYPE, (packet, context) -> SeismicScanEffects.receive(packet));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeyInputEvents.onClientTick();
            MultiBlockVisualFeedback.onClientTick();
            SeismicScanEffects.tick();
        });
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "ore_echoes"), SeismicScanEffects::drawMarkers);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "mining_preview"), MultiBlockOutlineRenderer::drawHud);
        FabricHammerOutlines.register();
    }
}
