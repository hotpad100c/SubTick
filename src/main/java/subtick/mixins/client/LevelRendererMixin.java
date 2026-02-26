package subtick.mixins.client;

import subtick.client.ClientTickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC >= 12002
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArgs;
//$$ import net.minecraft.world.entity.player.Player;
//#if MC < 12111
//$$ import org.apache.http.util.Args;
//#endif
//$$ import net.minecraft.world.entity.Entity;
//#endif
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
import net.minecraft.client.Minecraft;
//#if MC < 12002
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import carpet.fakes.MinecraftClientInferface;
//#endif
//#if MC >= 12003
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//#endif

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
  //#if MC < 12101
  @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", ordinal = 1))
  private void onRenderWorldLastNormal(
          //#if MC >= 12006
          //$$ float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#else
          PoseStack poseStack, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightTexture lightTexture, Matrix4f projMatrix, CallbackInfo ci
          //#endif
  )
  {
    subtick.client.LevelRenderer.render(poseStack);
  }
  //#endif


  // Everything below this point is yoinked from carpet

  //#if MC < 12002
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
  //#elseif MC < 12104
  //$$ @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
  //$$ ))
  //$$ private void modifyDelta(LevelRenderer instance, Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, Operation<Void> original) {
  //$$     tickDelta = shouldUsePausedDelta(entity) ? 1.0F : tickDelta;
  //$$     original.call(instance, entity, cameraX, cameraY, cameraZ, tickDelta, matrices, vertexConsumers);
  //$$}
  //#elseif MC < 12109
  //$$ @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
  //$$ ))
  //$$ private void modifyDelta(LevelRenderer instance, Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, Operation<Void> original) {
  //$$     tickDelta = shouldUsePausedDelta(entity) ? 1.0F : tickDelta;
  //$$     original.call(instance, entity, cameraX, cameraY, cameraZ, tickDelta, matrices, vertexConsumers);
  //$$}
  //#elseif MC <= 12111
  //$$ @ModifyArgs(method = "extractEntity", at = @At(                value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
  //$$ ))
  //$$ public void modifyDelta(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
  //$$   Entity entity = args.get(0);
  //$$   float tickDelta = args.get(1);
  //$$   tickDelta = shouldUsePausedDelta(entity) ? 1.0F : tickDelta;
  //$$   args.set(1, tickDelta);
  //$$ }
  //#endif
  //#if MC >= 12002
  //$$ @Unique
  //$$ private boolean shouldUsePausedDelta(Entity entity) {
////$$   if (Minecraft.getInstance().getSingleplayerServer() != null) if (isReplayEnvironment(Minecraft.getInstance().getSingleplayerServer().getClass())) return false;
  //$$   return ClientTickHandler.frozen && !canTick(entity);
  //$$ }
  //$$ @Unique
  //$$ private boolean canTick(Entity entity) {
  //$$  if (entity instanceof Player) return true;
  //$$  return entity.getPassengers().stream().flatMap(Entity::getSelfAndPassengers).anyMatch(entity1 -> entity1 instanceof Player);
  //$$ }
  //#endif
}
