package subtick.mixins;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.ITickHandleable;
//#if MC >= 12002
//$$ import net.minecraft.server.network.CommonListenerCookie;
//#endif

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    //#if MC >= 12002
    //$$ private void onPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
    //#else
    private void onPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CallbackInfo ci) {
        //#endif
        //#if MC >= 12108
        //$$ ((ITickHandleable)serverPlayer.level().getServer()).tickHandler().handleLogin(serverPlayer);
        //#else
        ((ITickHandleable)serverPlayer.server).tickHandler().handleLogin(serverPlayer);
        //#endif
    }
}
