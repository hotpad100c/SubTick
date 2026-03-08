package subtick.client;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import subtick.SubTick;
//#if MC >= 12005
//$$ import subtick.network.packet.SubTickPayload;
//#endif

public class SubTickClient implements ClientModInitializer, IInitializationHandler
{
  @Override
  public void onInitializeClient()
  {
    InitializationHandler.getInstance().registerInitializationHandler(this);
    registerNetworkPackReceiver();
  }

  @Override
  public void registerModHandlers()
  {
    ConfigManager.getInstance().registerConfigHandler(SubTick.MOD_ID, new Configs());
  }


  private static void registerNetworkPackReceiver() {
    ClientPlayNetworking.registerGlobalReceiver(
            //#if MC < 12005
            SubTick.SUBTICK_PACKET_ID,
            //#else
            //$$ SubTickPayload.TYPE,
            //#endif
            //#if MC < 12005
            (client, handler, buf, responseSender) -> {
              CompoundTag tag = buf.readNbt();
              client.execute(() -> {
                if (tag != null) {
                  ClientNetworkHandler.handlePacket(tag, client.player);
                }
              });
            }
            //#else
            //$$ (payload, context) -> context.client().execute(() ->
            //$$        ClientNetworkHandler.handlePacket(payload.tag(), context.client().player)
            //$$ )
            //#endif
    );
  }
}
