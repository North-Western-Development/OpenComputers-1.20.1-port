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
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.tileentity.BrewingStandBlockEntity
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity

object DriverBrewingStand extends DriverSidedBlockEntity {
  override def getBlockEntityClass: Class[_] = classOf[BrewingStandBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[BrewingStandBlockEntity])

  final class Environment(tileEntity: BrewingStandBlockEntity) extends ManagedBlockEntityEnvironment[BrewingStandBlockEntity](tileEntity, "brewing_stand") with NamedBlock {
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
