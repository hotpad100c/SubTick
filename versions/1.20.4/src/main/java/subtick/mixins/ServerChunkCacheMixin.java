package subtick.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//#if MC >= 12005
//$$ import net.minecraft.world.TickRateManager;
//#else
import net.minecraft.server.ServerTickRateManager;
//#endif
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

    //#if MC >= 12005
    //$$ @WrapOperation(method = "tickChunks()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
    //$$  private boolean tickChunks(TickRateManager instance, Operation<Boolean> original) {
    //#else
    @WrapOperation(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;runsNormally()Z"))
    private boolean tickChunks(ServerTickRateManager instance, Operation<Boolean> original) {
        //#endif
        return tickHandler().shouldTick(level, TickPhase.CHUNK) || this.level.getServer().tickRateManager().runsNormally();
    }
}
