package net.j40climb.florafauna_hammer.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockOutlineRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class FabricHammerOutlines {
    private static final RenderStateDataKey<MultiBlockOutlineRenderer.Preview> PREVIEW =
            RenderStateDataKey.create(() -> "hammer mining preview");

    public static void register() {
        RenderPipelines.register(MultiBlockOutlineRenderer.PREVIEW_PIPELINE);
        LevelRenderEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hit) -> {
            var state = context.levelState();
            if (state.blockOutlineRenderState != null && hit instanceof BlockHitResult block) {
                var preview = MultiBlockOutlineRenderer.extract(context.level(), block, CollisionContext.of(context.camera().entity()));
                state.setData(PREVIEW, preview);
            }
        });
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> {
            var state = context.levelState();
            var preview = state.getData(PREVIEW);
            if (preview != null && preview.target().equals(outline.pos())) {
                //? if <26.3 {
                MultiBlockOutlineRenderer.draw(preview, context.bufferSource(), context.poseStack(), state.cameraRenderState.pos);
                //?} else {
                /*MultiBlockOutlineRenderer.draw(preview, context.submitNodeCollector(), context.poseStack(), state.cameraRenderState.pos, outline.isTranslucent());
                *///?}
            }
            return true;
        });
    }

    private FabricHammerOutlines() {}
}
