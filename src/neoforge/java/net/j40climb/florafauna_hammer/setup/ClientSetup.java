package net.j40climb.florafauna_hammer.setup;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?}

public final class ClientSetup {

    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "hammer"));

    public static final Lazy<KeyMapping> SUMMON_LIGHTNING_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.summon_lightning",
            KeyConflictContext.IN_GAME,
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            *///?}
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> TELEPORT_SURFACE_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.teleport_surface",
            KeyConflictContext.IN_GAME,
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_M,
            *///?}
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> THROW_ITEM_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.throw_item",
            KeyConflictContext.IN_GAME,
            //? if <26.3 {
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            //?} else {
            /*InputConstants.Type.KEYBOARD,
            InputConstants.KEY_T,
            *///?}
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> CYCLE_MINING_MODE_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.cycle_mining_mode",
            KeyConflictContext.IN_GAME,
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

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HammerRegistry.THROWN_ITEM.get(), ThrownItemRenderer::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        event.register(SUMMON_LIGHTNING_KEY.get());
        event.register(TELEPORT_SURFACE_KEY.get());
        event.register(THROW_ITEM_KEY.get());
        event.register(CYCLE_MINING_MODE_KEY.get());
    }
}
