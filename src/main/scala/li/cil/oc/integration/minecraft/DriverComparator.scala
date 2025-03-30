package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.{EnvironmentProvider, NamedBlock}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.ComparatorBlockEntity

object DriverComparator extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[ComparatorBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[ComparatorBlockEntity])

  final class Environment(tileEntity: ComparatorBlockEntity) extends ManagedTileEntityEnvironment[ComparatorBlockEntity](tileEntity, "comparator") with NamedBlock {
    override def preferredName = "comparator"

    override def priority = 0

    @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
    def getOutputSignal(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getOutputSignal)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && stack.getItem == Items.COMPARATOR)
        classOf[Environment]
      else null
    }
  }

}
