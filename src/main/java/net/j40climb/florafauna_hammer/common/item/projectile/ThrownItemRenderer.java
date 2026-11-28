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
import net.minecraft.world.phys.Vec3;

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
        // The untransformed model points head-first along +Y. Aim it away from
        // the thrower on both legs, independently of the arrow's velocity rotation.
        var owner = entity.getOwner();
        Vec3 direction = owner == null
                ? entity.getDeltaMovement().scale(entity.isNoPhysics() ? -1 : 1)
                : entity.getPosition(partialTick).subtract(owner.getEyePosition(partialTick));
        if (direction.lengthSqr() < 1.0e-8 && owner != null) direction = owner.getViewVector(partialTick);
        state.yRot = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        state.xRot = (float) Math.toDegrees(Math.atan2(direction.horizontalDistance(), direction.y));
        this.itemModelResolver.updateForNonLiving(state.item, entity.getThrownItem(), ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraState) {
        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Apply the horizontal heading and flight tilt.
        //? if <26.3 {
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
        //?} else {
        /*poseStack.rotateDegrees(Axis.YP, state.yRot);
        *///?}
        //? if <26.3 {
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        //?} else {
        /*poseStack.rotateDegrees(Axis.XP, state.xRot);
        *///?}

        // Scale up slightly for visibility
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Render the item
        state.item.submit(poseStack, nodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();
        super.submit(state, poseStack, nodeCollector, cameraState);
    }
}
