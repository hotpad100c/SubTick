package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import subtick.client.LevelRenderer;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public class BlockFeatureRendererMixin {
    @WrapMethod(method = "tesselateBlock")
    private void render(BlockAndTintGetter blockAndTintGetter, List<BlockModelPart> list, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, int i, Operation<Void> original
    ) {
        if (LevelRenderer.hlBe.containsKey(blockPos)) {
            OutlineBufferSource outlineBufferSource = Minecraft.getInstance().renderBuffers().outlineBufferSource();
            outlineBufferSource.setColor(LevelRenderer.color.get());
            original.call(blockAndTintGetter, list, blockState, blockPos, poseStack, vertexConsumer, bl, i);
            outlineBufferSource.setColor(-1);
        }else {
            original.call(blockAndTintGetter, list, blockState, blockPos, poseStack, vertexConsumer, bl, i);
        }
    }
}
