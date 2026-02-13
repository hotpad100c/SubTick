package subtick.mixins.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.SubTick;
import subtick.client.substitute.WorldRenderContext;
import subtick.client.substitute.WorldRenderEvents;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    @Shadow
    @Final
    private LevelTargetBundle targets;
    @Unique
    private PoseStack subtick$posestack;

    @Unique
    private WorldRenderContext context;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        WorldRenderEvents.START.invoker().onStart();
    }

    @WrapOperation(method = "method_62214", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack setMatrixStack(Operation<PoseStack> original) {
        PoseStack result = original.call();
        this.subtick$posestack = result;
        return result;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"))
    private void setContext(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2, CallbackInfo ci, @Local Frustum frustum) {
        this.context = new WorldRenderContext(this.subtick$posestack, deltaTracker, frustum, this.renderBuffers.bufferSource());
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;"))
    private void onAfterTranslucent(CallbackInfo ci, @Local FrameGraphBuilder frameGraphBuilder) {
        FramePass pass = frameGraphBuilder.addPass(SubTick.MOD_ID + ":afterTranslucent");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> WorldRenderEvents.AFTER_TRANSLUCENT.invoker().render(this.context));
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDDZ)V"))
    private void onDebug(CallbackInfo ci) {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.invoker().render(this.context);
    }
}
