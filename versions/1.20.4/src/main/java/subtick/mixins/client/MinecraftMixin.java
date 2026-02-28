package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import subtick.client.ClientTickHandler;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;animateTick(III)V"))
    private boolean animateTick(ClientLevel instance, int i, int j, int k) {
        return ClientTickHandler.shouldTick();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
    private boolean tickParticles(ParticleEngine instance) {
        return ClientTickHandler.shouldTick();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tick()V"))
    private boolean tickTextureManager(TextureManager instance) {
        return ClientTickHandler.shouldTick();
    }
}
