package li.cil.oc.integration.computercraft

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IPeripheralProvider
import li.cil.oc.common.tileentity.Relay
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier

object PeripheralProvider extends IPeripheralProvider {
  def init() {
    ComputerCraftAPI.registerPeripheralProvider(this)
  }

  override def getPeripheral(world: Level, blockPos: BlockPos, side: Direction): LazyOptional[IPeripheral] = world.getBlockEntity(blockPos) match {
    case relay: Relay => LazyOptional.of(new NonNullSupplier[IPeripheral] {
      override def get = new RelayPeripheral(relay)
    })
    case _ => LazyOptional.empty[IPeripheral]
  }
}
