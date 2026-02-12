package subtick.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import subtick.ITickHandleable;
import subtick.TickHandler;
import subtick.TickPhase;

import java.util.function.Consumer;

@Mixin(EntityTickList.class)
public class EntityTickListMixin {
    @WrapWithCondition(method = "forEach", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private boolean entity(Consumer<?> instance, Object t){
        if (t instanceof Player || isPlayerControlled((LivingEntity) t)) {
            return true;
        }
        if (!(((EntityAccessor) t).getLevel() instanceof ServerLevel serverLevel)) return false;
        TickHandler tickHandler = tickHandler((LivingEntity) t);
        if (tickHandler == null) return false;
        return tickHandler.shouldTick(serverLevel, TickPhase.ENTITY);
    }
    @Unique
    private boolean isPlayerControlled(LivingEntity entity) {
        Entity controller = entity.isPassenger() ? entity.getVehicle() : null;
        return controller instanceof net.minecraft.world.entity.player.Player;
    }
    @Unique
    private TickHandler tickHandler(LivingEntity entity)
    {
        if (entity.getServer() != null) return ((ITickHandleable) entity.getServer()).tickHandler();
        return null;
    }
}
