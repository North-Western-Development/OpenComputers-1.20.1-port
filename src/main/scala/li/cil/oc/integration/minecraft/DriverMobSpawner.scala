package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.{EnvironmentProvider, NamedBlock}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.SpawnerBlockEntity
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraftforge.registries.ForgeRegistries

object DriverMobSpawner extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[SpawnerBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[SpawnerBlockEntity])

  final class Environment(tileEntity: SpawnerBlockEntity) extends ManagedTileEntityEnvironment[SpawnerBlockEntity](tileEntity, "mob_spawner") with NamedBlock {
    override def preferredName = "mob_spawner"

    override def priority = 0

    @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
    def getSpawningMobName(context: Context, args: Arguments): Array[AnyRef] = {
      result(ForgeRegistries.ENTITY_TYPES.getKey(tileEntity.getSpawner.getSpawnerEntity.getType))
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && Block.byItem(stack.getItem) == Blocks.SPAWNER)
        classOf[Environment]
      else null
    }
  }

}
