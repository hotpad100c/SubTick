package subtick.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
//#if MC >= 11900
//$$ import org.joml.Matrix4f;
//#else
import com.mojang.math.Matrix4f;
//#endif

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;

//#if MC < 12003
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import carpet.fakes.MinecraftClientInferface;
import net.minecraft.client.Minecraft;
import subtick.client.ClientTickHandler;
//#endif
//#if MC >= 12101
//$$ import net.minecraft.client.DeltaTracker;
//#endif
//#if MC >= 12006
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//#endif

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
  @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", ordinal = 1))
  private void onRenderWorldLastNormal(
          //#if MC >= 12101
          //$$ DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12006
          //$$ float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#else
          PoseStack poseStack, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightTexture lightTexture, Matrix4f projMatrix, CallbackInfo ci
          //#endif
  )
  {
    subtick.client.LevelRenderer.render(poseStack);
  }

  // Everything below this point is yoinked from carpet

  //#if MC < 12003
  @Shadow @Final private Minecraft minecraft;
  float initial = -1234.0f;

  @ModifyVariable(method = "renderLevel", argsOnly = true, require = 0, ordinal = 0, at = @At(
    value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"
  ))
  private float changeTickPhase(float previous)
  {
    initial = previous;
    if(ClientTickHandler.frozen)
      return ((MinecraftClientInferface)minecraft).getPausedTickDelta();
    return previous;
  }

  @ModifyVariable(method = "renderLevel", argsOnly = true, require = 0, ordinal = 0 ,at = @At(
    value = "INVOKE",
    target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
    shift = At.Shift.BEFORE
  ))
  private float changeTickPhaseBack(float previous)
  {
    return initial == -1234.0f ? previous : initial;
  }
  //#endif
}
