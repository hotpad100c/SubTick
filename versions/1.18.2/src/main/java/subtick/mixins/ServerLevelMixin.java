package subtick.mixins;

import net.minecraft.server.level.ServerPlayer;
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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

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
//#if MC >= 12104
//$$ import net.minecraft.world.entity.PositionMoveRotation;
//$$ import net.minecraft.world.entity.Relative;
//#endif

// entity management
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(value = ServerLevel.class, priority = 1001)
public class ServerLevelMixin
{

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

  //#if MC < 12103
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;tick(Ljava/util/function/BooleanSupplier;Z)V"))
  private boolean chunk(ServerChunkCache self, BooleanSupplier hasTimeLeft, boolean bool)
  {
    if(tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.CHUNK))
      return true;

    for(ChunkHolder holder : Lists.newArrayList(self.chunkMap.getChunks()
    ))
    {
      //#if MC >= 12006
      //$$ holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).ifSuccess(holder::broadcastChanges);
      //#else
      Optional<LevelChunk> optional = holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();
      optional.ifPresent(holder::broadcastChanges);
      //#endif
    }
    self.chunkMap.tick();
    return false;
  }
  //#endif

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private boolean blockEvent(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_EVENT);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
  private boolean entity(EntityTickList instance, Consumer<Entity> consumer)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY);
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
}