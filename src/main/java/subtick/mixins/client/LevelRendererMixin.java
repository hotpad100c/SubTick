package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
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
//#if MC >= 12110
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//$$ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//#endif

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
  //#if MC < 12101
  @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
  private void onRenderWorldLastNormal(
          //#if MC >= 12006
          //$$ float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#else
          PoseStack poseStack, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightTexture lightTexture, Matrix4f projMatrix, CallbackInfo ci
          //#endif
  )
  {
    OutlineBufferSource outlineBufferSource = this.renderBuffers.outlineBufferSource();
    subtick.client.LevelRenderer.render(poseStack, outlineBufferSource);
  }
  //#endif


  // Everything below this point is yoinked from carpet

  //#if MC < 12104
  @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
  private void modifyBlockEntityDelta(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, Operation<Void> original) {
    if (ClientTickHandler.frozen) f = 1.0f;
    original.call(instance, blockEntity, f, poseStack, multiBufferSource);
  }
  //#endif

  //#if MC < 12002
  @Shadow @Final private Minecraft minecraft;
  @Shadow
  @Final
  private RenderBuffers renderBuffers;
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
  //$$ }
  //$$ @WrapOperation(method = "renderBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
  //$$ private void modifyBlockEntityDelta(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, Operation<Void> original) {
  //$$   if (ClientTickHandler.frozen) f = 1.0f;
  //$$   original.call(instance, blockEntity, f, poseStack, multiBufferSource);
  //$$ }
  //#elseif MC <= 12111
  //$$ @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/LevelRenderer;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
  //$$ ))
  //$$ private EntityRenderState modifyDelta(LevelRenderer instance, Entity entity, float v, Operation<EntityRenderState> original) {
  //$$   if (shouldUsePausedDelta(entity)) v = 1.0f;
  //$$   return original.call(instance, entity, v);
  //$$ }
  //$$ @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/LevelRenderState;)V"))
  //$$ private void modifyBlockEntityDelta(LevelRenderer instance, Camera camera, float v, LevelRenderState levelRenderState, Operation<Void> original) {
  //$$   if (ClientTickHandler.frozen) v = 1.0f;
  //$$   original.call(instance, camera, v, levelRenderState);
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
