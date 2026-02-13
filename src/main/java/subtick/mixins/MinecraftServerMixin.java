package subtick.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
//#if MC < 12110
import net.minecraft.server.level.progress.ChunkProgressListener;
//#endif
import subtick.ITickHandleable;
import subtick.TickHandler;
import subtick.TickPhase;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements ITickHandleable
{
  @Unique private TickHandler tickHandler;

  @Override
  public TickHandler tickHandler()
  {
    return tickHandler;
  }

  @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0))
  private void onOverworldAdded(
          //#if MC < 12110
          ChunkProgressListener chunkProgressListener,
          //#endif
          CallbackInfo ci, @Local ServerLevel serverLevel)
  {
    tickHandler = new TickHandler();
    TickPhase.reset();
    TickPhase.addDimension(serverLevel);
  }

  @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1))
  private void onDimensionAdded(
          //#if MC < 12110
          ChunkProgressListener chunkProgressListener,
          //#endif
          CallbackInfo ci, @Local(ordinal = 1) ServerLevel serverLevel2)
  {
    TickPhase.addDimension(serverLevel2);
  }
}
