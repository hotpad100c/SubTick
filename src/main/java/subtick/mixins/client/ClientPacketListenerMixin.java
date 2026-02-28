package subtick.mixins.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.client.ClientTickHandler;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    //#if MC >= 12005
    //#if MC >= 12109
    //$$ @Inject(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/gui/screens/ReceivingLevelScreen$Reason;)V", shift = At.Shift.AFTER))
    //#endif
    //#else
    @Inject(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", shift = At.Shift.AFTER))
    //#endif
    private void onLogin(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo ci) {
        ClientTickHandler.clear();
    }
}
