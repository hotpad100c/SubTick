package subtick.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import subtick.ITickHandleable;
import subtick.TickHandler;
import subtick.TickPhase;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    private TickHandler tickHandler()
    {
        return ((ITickHandleable)level.getServer()).tickHandler();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/DistanceManager;purgeStaleTickets()V"))
    private boolean purgeStaleTickets(DistanceManager instance) {
        return tickHandler().shouldTick(level, TickPhase.CHUNK);
    }

    @WrapOperation(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;runsNormally()Z"))
    private boolean tickChunk(ServerTickRateManager instance, Operation<Boolean> original) {
        return tickHandler().shouldTick(level, TickPhase.CHUNK);
    }
}
