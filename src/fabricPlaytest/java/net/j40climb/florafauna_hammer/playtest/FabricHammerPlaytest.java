package net.j40climb.florafauna_hammer.playtest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockOutlineRenderer;

public final class FabricHammerPlaytest implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> Playtest.tick());
        LevelRenderEvents.END_MAIN.register(context ->
                HammerPlaytest.captureOutline(context.levelState(), MultiBlockOutlineRenderer.lastRenderedTarget()));
    }
}
