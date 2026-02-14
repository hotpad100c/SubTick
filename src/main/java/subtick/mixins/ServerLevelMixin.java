package subtick.mixins;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.*;
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

import subtick.TickHandler;
import subtick.TickPhase;
import subtick.ITickHandleable;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.Level;

// world border
import net.minecraft.world.level.border.WorldBorder;
// weather
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
// tile tick
import net.minecraft.world.level.ServerTickList;
// raid
import net.minecraft.world.entity.raid.Raids;
// chunk

import java.util.*;
import java.util.function.BooleanSupplier;
import com.google.common.collect.Lists;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.MinecraftServer;
// entity
import net.minecraft.world.entity.Entity;
// entity management
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{
  @Unique private final Map<UUID, Vec3> subtick$lastPos = new HashMap<>();
  @Unique private final Map<UUID, Vec2> subtick$lastRot = new HashMap<>();
  @Shadow @Final private MinecraftServer server;

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
  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;hasSkyLight()Z"))
  private boolean weather1(boolean original)
  {
    if(tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.WEATHER))
    {
      tickingWeather = true;
      return original;
    }
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/resources/ResourceKey;)V"))
  private boolean weather2(PlayerList self, Packet<?> packet, ResourceKey<Level> key)
  {
    return tickingWeather;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
  private boolean weather3(PlayerList self, Packet<?> packet)
  {
    return tickingWeather;
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
  private boolean weather4(boolean original)
  {
    return tickingWeather && original;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateSkyBrightness()V"))
  private boolean weather5(ServerLevel self)
  {
    if(tickingWeather)
    {
      tickingWeather = false;
      return true;
    }
    return false;
  }
  // END WEATHER

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
  private boolean time(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.TIME);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerTickList;tick()V", ordinal = 0))
  private boolean blockTick(ServerTickList<Block> self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerTickList;tick()V", ordinal = 1))
  private boolean fluidTick(ServerTickList<Fluid> self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.FLUID_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
  private boolean blockTick(Raids self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.RAID);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;tick(Ljava/util/function/BooleanSupplier;)V"))
  private boolean chunk(ServerChunkCache self, BooleanSupplier hasTimeLeft)
  {
    if(tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.CHUNK))
      return true;

    // Send chunk updates and entity updates to clients
    for(ChunkHolder holder : Lists.newArrayList(self.chunkMap.getChunks()))
    {
      Optional<LevelChunk> optional = holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();
      if(optional.isPresent())
        holder.broadcastChanges(optional.get());
    }
    self.chunkMap.tick();
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private boolean blockEvent(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_EVENT);
  }

  @Inject(method = "method_31420", at = @At(value = "HEAD"), cancellable = true)
  private void entity(ProfilerFiller profilerFiller, Entity entity, CallbackInfo ci)
  {
    if (!(entity instanceof ServerPlayer player)) {
      if (!tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY)) {
        if (!isPlayerControlled(entity)) {
          ci.cancel();
        }
      }
      return;
    }

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
  }

  @Unique
  private void subtick$syncPlayerPosition(ServerPlayer player, Vec3 lastPos) {
    double deltaX = player.getX() - lastPos.x;
    double deltaY = player.getY() - lastPos.y;
    double deltaZ = player.getZ() - lastPos.z;
    boolean isTooFar = Math.abs(deltaX) > 8 || Math.abs(deltaY) > 8 || Math.abs(deltaZ) > 8;

    if (isTooFar) {
      ((ServerLevel)player.level).getChunkSource().broadcast(player,
              new ClientboundTeleportEntityPacket(player));
    } else {
      ((ServerLevel)player.level).getChunkSource().broadcast(player,
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
