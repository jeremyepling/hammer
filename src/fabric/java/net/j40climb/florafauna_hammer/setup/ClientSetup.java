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
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?}

public final class ClientSetup implements net.fabricmc.api.ClientModInitializer {

    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "hammer"));

    public static final java.util.function.Supplier<KeyMapping> SUMMON_LIGHTNING_KEY = com.google.common.base.Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.summon_lightning",
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            *///?}
            KEY_CATEGORY
    ));

    public static final java.util.function.Supplier<KeyMapping> TELEPORT_SURFACE_KEY = com.google.common.base.Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.teleport_surface",
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_M,
            *///?}
            KEY_CATEGORY
    ));

    public static final java.util.function.Supplier<KeyMapping> THROW_ITEM_KEY = com.google.common.base.Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.throw_item",
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_T,
            *///?}
            KEY_CATEGORY
    ));

    public static final java.util.function.Supplier<KeyMapping> CYCLE_MINING_MODE_KEY = com.google.common.base.Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.cycle_mining_mode",
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_C,
            *///?}
            KEY_CATEGORY
    ));

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
        KeyMappingHelper.registerKeyMapping(SUMMON_LIGHTNING_KEY.get());
        KeyMappingHelper.registerKeyMapping(TELEPORT_SURFACE_KEY.get());
        KeyMappingHelper.registerKeyMapping(THROW_ITEM_KEY.get());
        KeyMappingHelper.registerKeyMapping(CYCLE_MINING_MODE_KEY.get());
        EntityRendererRegistry.register(HammerRegistry.THROWN_ITEM.get(), ThrownItemRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(HammerCorePayload.TYPE, (packet, context) -> HammerAbilityFeedback.receive(packet));
        ClientPlayNetworking.registerGlobalReceiver(SeismicScanPayload.TYPE, (packet, context) -> SeismicScanEffects.receive(packet));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeyInputEvents.onClientTick();
            MultiBlockVisualFeedback.onClientTick();
            SeismicScanEffects.tick();
        });
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "ore_echoes"), SeismicScanEffects::drawMarkers);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "mining_preview"), MultiBlockOutlineRenderer::drawHud);
        FabricHammerOutlines.register();
    }
}
