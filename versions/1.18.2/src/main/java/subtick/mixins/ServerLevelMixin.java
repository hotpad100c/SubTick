package subtick.mixins;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

import subtick.Queues;
import subtick.TickHandler;
import subtick.TickPhase;
import subtick.ITickHandleable;
import net.minecraft.server.level.ServerLevel;

// world border
import net.minecraft.world.level.border.WorldBorder;
// tile tick
import net.minecraft.world.ticks.LevelTicks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
// raid
import net.minecraft.world.entity.raid.Raids;
// chunk
import net.minecraft.server.level.ServerChunkCache;
import java.util.function.BooleanSupplier;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import com.google.common.collect.Lists;
import java.util.Optional;
import net.minecraft.world.level.chunk.LevelChunk;
// entity
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import java.util.function.Consumer;
//#if MC >= 12002
//$$ import net.minecraft.world.TickRateManager;
//#endif
//#if MC >= 12004
//$$ import net.minecraft.world.entity.PositionMoveRotation;
//$$ import net.minecraft.world.entity.Relative;
//#endif

// entity management
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{

  @Unique private boolean subtick$shouldTickEntityThisTime = true;
  @Unique private final Map<UUID, Vec3> subtick$lastPos = new HashMap<>();
  @Unique private final Map<UUID, Vec2> subtick$lastRot = new HashMap<>();
  @Shadow @Final private MinecraftServer server;

  @Shadow @Final public EntityTickList entityTickList;

  private TickHandler tickHandler()
  {
    return ((ITickHandleable)server).tickHandler();
  }

  @Inject(method = "blockEvent", at = @At("TAIL"))
  private void blockEvent(BlockPos blockPos, Block block, int i, int j, CallbackInfo ci)
  {
    if(tickHandler().frozen())
      tickHandler().queues().onScheduleBlockEvent((ServerLevel)(Object)this, new BlockEventData(blockPos, block, i, j));
  }

  @Unique private boolean tickingWeather = false;

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"))
  private boolean worldBorder(WorldBorder self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.WORLD_BORDER);
  }

  // BEGIN WEATHER --------------------------------------------------------------------------------------------------------------------------
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;advanceWeatherCycle()V"))
  private boolean weather1(ServerLevel self)
  {
    if(tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.WEATHER))
    {
      tickingWeather = true;
      return true;
    }
    tickingWeather = false;
    return false;
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
  private boolean weather2(boolean original)
  {
    return tickingWeather && original;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateSkyBrightness()V"))
  private boolean weather3(ServerLevel self)
  {
    return tickingWeather;
  }
  // END WEATHER --------------------------------------------------------------------------------------------------------------------------

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
  private boolean time(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.TIME);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V", ordinal = 0))
  private boolean blockTick(LevelTicks<Block> self, long l, int i, BiConsumer<BlockPos, Block> biConsumer)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V", ordinal = 1))
  private boolean fluidTick(LevelTicks<Fluid> self, long l, int i, BiConsumer<BlockPos, Fluid> biConsumer)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.FLUID_TICK);
  }
  //#if MC >= 12105
  //$$ @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick(Lnet/minecraft/server/level/ServerLevel;)V"))
  //$$ private boolean blockTick(Raids instance, ServerLevel level)
  //#else
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
  private boolean blockTick(Raids self)
  //#endif
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.RAID);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;tick(Ljava/util/function/BooleanSupplier;Z)V"))
  private boolean chunk(ServerChunkCache self, BooleanSupplier hasTimeLeft, boolean bool)
  {
    if(tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.CHUNK))
      return true;

    // Send chunk updates and entity updates to clients
    for(ChunkHolder holder : Lists.newArrayList(self.chunkMap.
            //#if MC >= 12110
            //$$ visibleChunkMap.values()
            //#else
            getChunks()
            //#endif
    ))
    {
      //#if MC >= 12006
      //$$ holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).ifSuccess(holder::broadcastChanges);
      //#else
      Optional<LevelChunk> optional = holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();
      optional.ifPresent(holder::broadcastChanges);
      //#endif
    }
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private boolean blockEvent(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_EVENT);
  }


  @Inject(method = "tick", at = @At(target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V", value = "INVOKE"))
  private void tickEntities(CallbackInfo ci)
  {
    subtick$shouldTickEntityThisTime = tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY);
  }

  @Inject(method = "method_31420", at = @At(value = "HEAD"), cancellable = true)
  //#if MC >= 12002
  //$$ private void entity(TickRateManager tickRateManager, ProfilerFiller profilerFiller, Entity entity, CallbackInfo ci)
  //#else
  private void entity(ProfilerFiller profilerFiller, Entity entity, CallbackInfo ci)
  //#endif
  {
    if (!(entity instanceof ServerPlayer) && !subtick$shouldTickEntityThisTime && !isPlayerControlled(entity)
    ) {
      ci.cancel();
      return;
    }
    /*
    if (entity instanceof ServerPlayer player) {
      UUID uuid = player.getUUID();
      Vec3 currentPos = player.position();
      Vec2 currentRot = new Vec2(player.getYRot(), player.getXRot());
      Vec3 lastPos = subtick$lastPos.get(uuid);
      Vec2 lastRot = subtick$lastRot.get(uuid);

      if (lastPos != null && lastRot != null) {
        if (!currentPos.equals(lastPos) || !currentRot.equals(lastRot)) {
          subtick$syncPlayerPosition(player, lastPos);
        }
      }
      subtick$lastPos.put(uuid, currentPos);
      subtick$lastRot.put(uuid, currentRot);
    }*/
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickBlockEntities()V"))
  private boolean blockEntity(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;tick()V"))
  private boolean entityManagement(PersistentEntitySectionManager<Entity> self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY_MANAGEMENT);
  }


  @Unique
  private void subtick$syncPlayerPosition(ServerPlayer player, Vec3 lastPos) {
    double deltaX = player.getX() - lastPos.x;
    double deltaY = player.getY() - lastPos.y;
    double deltaZ = player.getZ() - lastPos.z;
    boolean isTooFar = Math.abs(deltaX) > 8 || Math.abs(deltaY) > 8 || Math.abs(deltaZ) > 8;

    if (isTooFar) {
    //#if MC >= 12104
    //$$  ((ServerLevel)player.level()).getChunkSource().broadcast(player, ClientboundTeleportEntityPacket.teleport(
    //$$      player.getId(),
    //$$      new PositionMoveRotation(player.position(),player.getDeltaMovement(),player.getXRot(), player.getYRot()),
    //$$      Relative.union(Relative.DELTA, Relative.ROTATION),
    //$$      player.onGround()
    //$$  ));
    //$$ } else {((ServerLevel)player.level()).getChunkSource().broadcast(player,
    //#elseif MC >= 12002
    //$$  ((ServerLevel)player.level()).getChunkSource().broadcast(player, new ClientboundTeleportEntityPacket(player));
    //$$} else {((ServerLevel)player.level()).getChunkSource().broadcast(player,
    //#else
      ((ServerLevel)player.level).getChunkSource().broadcast(player, new ClientboundTeleportEntityPacket(player));
    } else {((ServerLevel)player.level).getChunkSource().broadcast(player,
    //#endif
              new ClientboundMoveEntityPacket.PosRot(
                      player.getId(),
                      (short)(deltaX * 4096),
                      (short)(deltaY * 4096),
                      (short)(deltaZ * 4096),
                      (byte)(player.getYRot() * 256.0F / 360.0F),
                      (byte)(player.getXRot() * 256.0F / 360.0F),
                      player.isOnGround()
              )
      );
    }
  }

  @Unique
  private boolean isPlayerControlled(Entity entity) {
    return entity.getPassengers().stream().anyMatch(entity1 -> entity1 instanceof Player);
  }
}
