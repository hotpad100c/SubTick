package subtick.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import subtick.network.packet.SubTickPayload;

public class PacketRegister {
    //#if MC >= 12005
    public static void s2c() {
        //#if MC >= 26.1
        //$$ PayloadTypeRegistry.clientboundPlay().register(SubTickPayload.TYPE, SubTickPayload.CODEC);
        //#else
        PayloadTypeRegistry.playS2C().register(SubTickPayload.TYPE, SubTickPayload.CODEC);
        //#endif
    }
    //#endif
}
