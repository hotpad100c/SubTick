package subtick.mixins.lithium;

import java.util.ArrayList;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.TickNextTickData;

@Mixin(targets = "me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler")
@Pseudo
public interface LithiumServerTickSchedulerAccessor<T>
{
  @Accessor(value = "executingTicks", remap = false)
  public ArrayList<me.jellysquid.mods.lithium.common.world.scheduler.TickEntry<T>> getExecutingTicks();

  @Accessor(value = "executingTicksSet", remap = false)
  public ObjectOpenHashSet<me.jellysquid.mods.lithium.common.world.scheduler.TickEntry<T>> getExecutingTicksSet();

  @Accessor(value = "tickConsumer", remap = false)
  public Consumer<TickNextTickData<T>> getTickConsumer();
}
