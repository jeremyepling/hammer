package net.j40climb.florafauna_hammer.setup;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemRenderer;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
//? if <26.4 {
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//?} else {
/*import net.minecraft.client.core.registries.ClientRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemEntity;
import net.neoforged.neoforge.registries.RegisterEvent;
*///?}
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class ClientSetup {

    private static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(HammerKey.CATEGORY_ID);

    static KeyMapping createKey(String translationKey, int defaultKey) {
        //? if <26.3 {
        return new KeyMapping(translationKey, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, KEY_CATEGORY);
        //?} else {
        /*return new KeyMapping(translationKey, KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, defaultKey, KEY_CATEGORY);
        *///?}
    }

    public static void submit(net.minecraft.client.gui.GuiGraphicsExtractor gui, net.minecraft.client.renderer.state.gui.GuiElementRenderState state) {
        gui.submitGuiElementRenderState(state);
    }

    public static InputConstants.Key boundKey(KeyMapping mapping) {
        return mapping.getKey();
    }

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }

    public static void init(IEventBus bus) {
        bus.addListener(ClientSetup::registerKeys);
        bus.addListener(ClientSetup::registerRenderers);
    }

    //? if <26.4 {
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HammerRegistry.THROWN_ITEM.get(), ThrownItemRenderer::new);
    //?} else {
    /*private static void registerRenderers(RegisterEvent event) {
        event.register(ClientRegistries.ENTITY_RENDERER, HammerRegistry.THROWN_ITEM_KEY.identifier(),
                () -> (EntityRendererProvider<ThrownItemEntity>) ThrownItemRenderer::new);
    *///?}
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        HammerKey.registerAll(event::register);
    }
}
