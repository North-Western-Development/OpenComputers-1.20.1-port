package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedBlockEntity
import li.cil.oc.integration.ManagedBlockEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.SpawnerBlockEntity

object DriverMobSpawner extends DriverSidedBlockEntity {
  override def getBlockEntityClass: Class[_] = classOf[SpawnerBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[SpawnerBlockEntity])

  final class Environment(tileEntity: SpawnerBlockEntity) extends ManagedBlockEntityEnvironment[SpawnerBlockEntity](tileEntity, "mob_spawner") with NamedBlock {
    override def preferredName = "mob_spawner"

    override def priority = 0

    @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
    def getSpawningMobName(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getSpawner.getSpawnerEntity.getId)
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
