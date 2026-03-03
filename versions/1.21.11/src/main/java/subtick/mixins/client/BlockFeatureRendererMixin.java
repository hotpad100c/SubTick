package subtick.mixins.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.client.LevelRenderer;

import java.util.List;

@Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V", shift = At.Shift.AFTER))
    private void render(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, BlockRenderDispatcher blockRenderDispatcher, OutlineBufferSource outlineBufferSource, CallbackInfo ci, @Local MovingBlockRenderState movingBlockRenderState, @Local List<BlockModelPart> parts, @Local BlockState blockState, @Local PoseStack poseStack) {
        if (LevelRenderer.b36Flag.get()) {
            outlineBufferSource.setColor(LevelRenderer.color.get());
            blockRenderDispatcher.getModelRenderer().tesselateBlock(
                    movingBlockRenderState,
                    parts,
                    blockState,
                    movingBlockRenderState.blockPos,
                    poseStack,
                    outlineBufferSource.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(blockState)),
                    false,
                    OverlayTexture.NO_OVERLAY
            );
        }
    }
}
