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
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity

object DriverBrewingStand extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[BrewingStandBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[BrewingStandBlockEntity])

  final class Environment(tileEntity: BrewingStandBlockEntity) extends ManagedTileEntityEnvironment[BrewingStandBlockEntity](tileEntity, "brewing_stand") with NamedBlock {
    override def preferredName = "brewing_stand"

    override def priority = 0

    @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
    def getBrewTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.brewTime)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && stack.getItem == Items.BREWING_STAND)
        classOf[Environment]
      else null
    }
  }

}
