package subtick.queues;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Npc;
import org.apache.commons.lang3.tuple.Triple;

import subtick.QueueElement;
import subtick.TickPhase;
//#if MC >= 12103
//$$ import net.minecraft.world.level.GameRules;
//#endif

public class EntityQueue extends TickingQueue
{
  private Iterator<Entity> entity_iterator;

  public EntityQueue()
  {
    super(TickPhase.ENTITY, "entity", "Entity", "Entities");
  }

  @Override
  public void start(ServerLevel level)
  {
    this.level = level;
    queue.clear();
    for(Entity e : level.entityTickList.active.values())
      queue.add(new QueueElement(e));

    level.entityTickList.iterated = level.entityTickList.active;
    entity_iterator = level.entityTickList.active.values().iterator();
  }

  @Override
  public Triple<Integer, Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    int success_steps = 0;
    while(success_steps < count && entity_iterator.hasNext())
    {
      Entity entity = entity_iterator.next();
      if(entity.isRemoved())
      {
        queue.remove(new QueueElement(entity));
        continue;
      }
      //#if MC >= 12103
      //$$ boolean shouldDiscardEntity = level.server.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || !(entity instanceof Animal) && !(entity instanceof WaterAnimal);
      //#else
      boolean shouldDiscardEntity = level.server.isSpawningAnimals() || !(entity instanceof Animal) && !(entity instanceof WaterAnimal) ? !level.server.areNpcsEnabled() && entity instanceof Npc : true;
      //#endif
      if(shouldDiscardEntity)
      {
        queue.remove(new QueueElement(entity));
        entity.discard();
      }
      else
      {
        entity.checkDespawn();
        Entity entity2 = entity.getVehicle();
        if(entity2 != null)
        {
          if(!entity2.isRemoved() && entity2.hasPassenger(entity))
            continue;

          entity.stopRiding();
        }

        level.guardEntityTick(level::tickNonPassenger, entity);
      }
      if(rangeCheck(entity.blockPosition(), pos, range))
        success_steps ++;
      executed_steps ++;
    }
    return Triple.of(executed_steps, success_steps, exhausted = !entity_iterator.hasNext());
  }

  @Override
  public void end()
  {
    level.entityTickList.iterated = null;
  }
}
