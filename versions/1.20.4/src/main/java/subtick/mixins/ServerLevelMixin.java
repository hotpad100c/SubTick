package subtick.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.ITickHandleable;
import subtick.TickHandler;
import subtick.TickPhase;

import java.util.function.BiConsumer;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{
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

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private boolean blockEvent(ServerLevel self)
  {
    return tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_EVENT);
  }


  @Inject(method = "tick", at = @At(target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V", value = "INVOKE"))
  private void tickEntities(CallbackInfo ci)
  {
    if(this.entityTickList.active.isEmpty()) tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY);
  }

  /*
   * To keep the player and player mounted entities ticking correctly (matching vanilla 1.21 behavior),
   * we have to filter ticks inside the forEach of entityTickList.
   * But there is problem: when a dimension has no entities,
   * or only player entities, tickHandler().shouldTick will never reach phase 8,
   * causing the phase system to get stuck in the stepping state.
   * The above approach is used to solve this issue.
   * It ensures that shouldTick is still executed even when there are no entities.
   */


  @Inject(method = "method_31420", at = @At(value = "HEAD"), cancellable = true)
  private void entity(TickRateManager tickRateManager, ProfilerFiller profilerFiller, Entity entity, CallbackInfo ci)
  {
    boolean shouldTick = tickHandler().shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY);
    if (!(entity instanceof ServerPlayer) && !shouldTick && !isPlayerControlled(entity)) {
      ci.cancel();
    }
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
  private boolean isPlayerControlled(Entity entity) {
    return entity.getPassengers().stream().anyMatch(entity1 -> entity1 instanceof Player);
  }
}