package net.j40climb.florafauna_hammer.common.item.abilities.multiblock;

//? if <26.3 {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else {
/*import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
*///?}
//? if >=26.3 && <26.4 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?}
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.Minecraft;
//? if <26.4 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import net.minecraft.client.renderer.pipeline.RenderPipelines;
import net.minecraft.client.renderer.pipeline.variant.LineRenderPipelineVariant;
*///?}
//? if <26.3 {
import net.minecraft.client.renderer.ShapeRenderer;
//?}
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Draws the additional outlines for a multi-block mining tool. */
public class MultiBlockOutlineRenderer {
    // Show planned placements and newly exposed lava even inside the unbroken wall.
    //? if <26.4 {
    public static final RenderPipeline PREVIEW_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "pipeline/hammer_preview"))
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    private static final RenderType PREVIEW_LINES = RenderType.create("florafauna_hammer_preview",
            RenderSetup.builder(PREVIEW_PIPELINE).createRenderSetup());
    //?} else {
    /*private static final LineRenderPipelineVariant PREVIEW_VARIANT = new LineRenderPipelineVariant(
            "florafauna_hammer_preview", new ColorTargetState(BlendFunction.TRANSLUCENT),
            new DepthStencilState(CompareOp.ALWAYS_PASS, false), false);
    private static final RenderType PREVIEW_LINES = RenderType.create("florafauna_hammer_preview",
            RenderSetup.builder(stage -> RenderPipelines.lines(new RenderPipelines.LinesParameters(PREVIEW_VARIANT, stage)))
                    .createRenderSetup());
    *///?}
    private static final int PLACEMENT_COLOR = 0xD92DE7B1;
    private static final int LAVA_COLOR = 0xFFFF780D;
    private static final Map<Direction, VoxelShape> FACE_OUTLINES = Shapes.rotateAll(Shapes.box(0, 0, 0, 1, 1, 0.005));

    private static BlockPos previewTarget;
    private static int previewCount;
    private static long previewTick;

    //? if <26.4 {
    public static void drawHud(GuiGraphicsExtractor gui, DeltaTracker delta) {
    //?} else {
    /*public static void drawHud(GuiGraphicsExtractor gui) {
    *///?}
        var mc = Minecraft.getInstance();
        //? if <26.3 {
        if (mc.options.hideGui || mc.player == null || mc.screen != null
        //?} else {
        /*if (mc.gui.hud.isHidden() || mc.player == null || mc.gui.screen() != null
        *///?}
                || !(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(previewTarget)
                || Math.abs(mc.player.level().getGameTime() - previewTick) > 1 || previewCount == 0) return;
        var stack = mc.player.getMainHandItem();
        if (!stack.has(HammerRegistry.MULTI_BLOCK_MINING.get())) return;
        var mining = MiningModeData.effective(stack);
        var shape = Component.translatable("mining_shape.florafauna." + mining.shape().getSerializedName());
        var key = previewCount == 1 ? "hud.florafauna_hammer.mining_preview.single" : "hud.florafauna_hammer.mining_preview";
        var label = Component.translatable(key, shape, previewCount);
        gui.text(mc.font, label, (gui.guiWidth() - mc.font.width(label)) / 2, gui.guiHeight() / 2 + 17, 0xFFDEC9FF);
    }

    public static MultiBlockPatterns.Plan previewPlan(Player player, BlockPos target, Direction hoveredFace) {
        return MultiBlockPatterns.plan(player, player.getMainHandItem(), target, MultiBlockVisualFeedback.previewFace(target, hoveredFace));
    }

    public record Preview(BlockPos target, List<VoxelShape> outlines, List<VoxelShape> placements, List<LavaExposure> lava) {}

    public static Preview extract(Level level, BlockHitResult hit, CollisionContext collision) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().has(HammerRegistry.MULTI_BLOCK_MINING.get())) {
            return null;
        }

        BlockPos target = hit.getBlockPos();
        var plan = previewPlan(player, target, hit.getDirection());
        previewTarget = target.immutable();
        previewTick = player.level().getGameTime();
        previewCount = plan.blocks().size();
        List<VoxelShape> outlines = plan.blocks().stream()
                .filter(pos -> !pos.equals(target))
                .map(pos -> level.getBlockState(pos)
                        .getShape(level, pos, collision)
                        //? if <26.3 {
                        .move(pos.getX(), pos.getY(), pos.getZ()))
                        //?} else {
                        /*.move(pos.getX() - target.getX(), pos.getY() - target.getY(), pos.getZ() - target.getZ()))
                        *///?}
                .filter(shape -> !shape.isEmpty())
                .toList();
        List<VoxelShape> placements = plan.stairs().stream()
                .map(pos -> plan.stairState().getShape(level, pos)
                        //? if <26.3 {
                        .move(pos.getX(), pos.getY(), pos.getZ()))
                        //?} else {
                        /*.move(pos.getX() - target.getX(), pos.getY() - target.getY(), pos.getZ() - target.getZ()))
                        *///?}
                .toList();
        List<LavaExposure> lava = LavaExposure.find(level, plan);
        return new Preview(target, outlines, placements, lava);
    }

    private static BlockPos renderedTarget;

    public static BlockPos lastRenderedTarget() {
        return renderedTarget;
    }

    //? if <26.3 {
    public static void draw(Preview preview, net.minecraft.client.renderer.MultiBufferSource buffers, PoseStack poseStack, Vec3 camera) {
        var vertices = buffers.getBuffer(RenderTypes.lines());
        for (VoxelShape outline : preview.outlines()) {
            ShapeRenderer.renderShape(poseStack, vertices, outline, -camera.x, -camera.y, -camera.z, 0xADBCA0E8, 2.0F);
        }
        if (!preview.placements().isEmpty() || !preview.lava().isEmpty()) {
            var previewVertices = buffers.getBuffer(PREVIEW_LINES);
            for (VoxelShape stair : preview.placements()) {
                ShapeRenderer.renderShape(poseStack, previewVertices, stair, -camera.x, -camera.y, -camera.z, PLACEMENT_COLOR, 2.5F);
            }
            for (LavaExposure exposure : preview.lava()) {
                BlockPos pos = exposure.pos();
                ShapeRenderer.renderShape(poseStack, previewVertices, FACE_OUTLINES.get(exposure.face()),
                        pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, LAVA_COLOR, 3.0F);
            }
        }
        renderedTarget = preview.target();
    }
    //?} else {
    /*public static void draw(Preview preview, net.minecraft.client.renderer.SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera, boolean translucent) {
        var target = preview.target();
        poseStack.pushPose();
        poseStack.translate(target.getX() - camera.x, target.getY() - camera.y, target.getZ() - camera.z);
        for (VoxelShape outline : preview.outlines()) {
            collector.submitShapeOutline(poseStack, outline, RenderTypes.linesTranslucentNoDepthWrite(),
                    0xADBCA0E8, 2.0F, translucent);
        }
        for (VoxelShape stair : preview.placements()) {
            collector.submitShapeOutline(poseStack, stair, PREVIEW_LINES, PLACEMENT_COLOR, 2.5F, translucent);
        }
        for (LavaExposure exposure : preview.lava()) {
            BlockPos pos = exposure.pos();
            var shape = FACE_OUTLINES.get(exposure.face()).move(pos.getX() - target.getX(),
                    pos.getY() - target.getY(), pos.getZ() - target.getZ());
            collector.submitShapeOutline(poseStack, shape, PREVIEW_LINES, LAVA_COLOR, 3.0F, translucent);
        }
        poseStack.popPose();
        renderedTarget = target;
    }
    *///?}
}
