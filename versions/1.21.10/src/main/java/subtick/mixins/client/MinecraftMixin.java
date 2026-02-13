package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import subtick.client.ClientTickHandler;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickBlockEntities()V"))
    private boolean tickBlockEntities(ClientLevel instance) {
        return !ClientTickHandler.skip_block_entities && ClientTickHandler.shouldTick();
    }
}
