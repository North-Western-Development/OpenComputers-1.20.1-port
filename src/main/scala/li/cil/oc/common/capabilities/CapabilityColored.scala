package li.cil.oc.common.capabilities

import li.cil.oc.api.internal.Colored
import li.cil.oc.integration.Mods
import net.minecraft.core.Direction
import net.minecraft.nbt.{IntTag, Tag}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier

object CapabilityColored {
  final val ProviderColored = new ResourceLocation(Mods.IDs.OpenComputers, "colored")

  class Provider(val tileEntity: BlockEntity with Colored) extends ICapabilityProvider with NonNullSupplier[Provider] with Colored {
    private val wrapper = LazyOptional.of(this)

    def get = this

    def invalidate() = wrapper.invalidate

    override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
      if (capability == Capabilities.ColoredCapability) wrapper.cast[T]
      else LazyOptional.empty[T]
    }

    override def getColor = tileEntity.getColor

    override def setColor(value: Int) = tileEntity.setColor(value)

    override def controlsConnectivity = tileEntity.controlsConnectivity
  }

  class DefaultImpl extends Colored {
    var color = 0

    override def getColor = color

    override def setColor(value: Int): Unit = color = value

    override def controlsConnectivity = false
  }

  class DefaultStorage extends Capability.Storage[Colored] {
    override def writeTag(capability: Capability[Colored], t: Colored, Direction: Direction): Tag = {
      val color = t.getColor
      IntTag.valueOf(color)
    }

    override def readTag(capability: Capability[Colored], t: Colored, Direction: Direction, nbtBase: Tag): Unit = {
      nbtBase match {
        case nbt: IntTag =>
          t.setColor(nbt.getAsInt)
        case _ =>
      }
    }
  }

}
