package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.HammerMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

@EventBusSubscriber(modid = HammerMod.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeHammerPlaytest {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Playtest.tick();
    }

    @SubscribeEvent
    public static void captureOutline(ExtractLevelRenderStateEvent event) {
        var state = event.getRenderState();
        var outline = state.blockOutlineRenderState;
        HammerPlaytest.captureOutline(state, outline != null && !outline.customRenderers().isEmpty() ? outline.pos() : null);
    }
}
