package subtick.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import subtick.SubTick;
import subtick.TickPhase;

public class ClientNetworkHandler
{
  private static final Map<String, BiConsumer<LocalPlayer, Tag>> dataHandlers = new HashMap<>();

  static {
    dataHandlers.put("TickingState", (p, t) -> ClientTickHandler.setFreeze((CompoundTag)t));
    dataHandlers.put("TickPhase", (p, t) -> ClientTickHandler.setPhase(new TickPhase((CompoundTag)t)));
    dataHandlers.put("TickPlayerActiveTimeout", (p, t) -> ClientTickHandler.scheduleTickStep(((NumericTag)t).getAsInt()));
    dataHandlers.put("Queue", (p, t) -> ClientTickHandler.setQueue((ListTag)t));
    dataHandlers.put("QueueStep", (p, t) -> ClientTickHandler.queueStep((CompoundTag)t));
  }

  public static void handlePacket(CompoundTag tag, LocalPlayer player) {
    for (String key: tag.getAllKeys())
    {
      if (dataHandlers.containsKey(key)) {
        try {
          dataHandlers.get(key).accept(player, tag.get(key));
        }
        catch (Exception exc)
        {
            SubTick.LOGGER.info("Corrupt subtick data for {}", key);
        }
      }
      else
          SubTick.LOGGER.error("Unknown subtick data: {}", key);
    }
  }
}
