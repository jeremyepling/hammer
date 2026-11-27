package net.j40climb.florafauna_hammer.common.item.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renderer for ThrownItemEntity.
 * Renders the thrown Hammer with its handle toward the thrower.
 */
public class ThrownItemRenderer extends EntityRenderer<ThrownItemEntity, ThrownItemRenderState> {

    private final ItemModelResolver itemModelResolver;

    public ThrownItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15f;
        this.shadowStrength = 0.75f;
    }

    @Override
    public ThrownItemRenderState createRenderState() {
        return new ThrownItemRenderState();
    }

    @Override
    public void extractRenderState(ThrownItemEntity entity, ThrownItemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // The arrow already reverses yaw while homing. Mirror the outbound pitch
        // so the Hammer has the return pose for the opposite direction of travel.
        float pitch = entity.getXRot(partialTick);
        state.xRot = entity.isNoPhysics() ? pitch : -pitch;
        state.yRot = entity.getYRot(partialTick);

        // Extract the item render state
        this.itemModelResolver.updateForNonLiving(state.item, entity.getThrownItem(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraState) {
        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Apply the horizontal heading and flight tilt to the ground model.
        //? if <26.3 {
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot + 90.0f));
        //?} else {
        /*poseStack.rotateDegrees(Axis.YP, state.yRot + 90.0f);
        *///?}
        //? if <26.3 {
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot + 90.0f));
        //?} else {
        /*poseStack.rotateDegrees(Axis.XP, state.xRot + 90.0f);
        *///?}

        // Scale up slightly for visibility
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Render the item
        state.item.submit(poseStack, nodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();
        super.submit(state, poseStack, nodeCollector, cameraState);
    }
}
