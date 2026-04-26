package li.cil.oc.common.capabilities

import li.cil.oc.api.internal.Colored
import li.cil.oc.integration.Mods
import net.minecraft.core.Direction
import net.minecraft.nbt.IntTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.{Capability, ICapabilitySerializable}
import net.minecraftforge.common.util.{LazyOptional, NonNullSupplier}

object CapabilityColored {
  final val ProviderColored = new ResourceLocation(Mods.IDs.OpenComputers, "colored")

  class Provider(val tileEntity: BlockEntity with Colored) extends ICapabilitySerializable[IntTag] with NonNullSupplier[Provider] with Colored {
    private val wrapper = LazyOptional.of(this)

    def get = this

    def invalidate() = wrapper.invalidate

    override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
      if (capability == Capabilities.ColoredCapability) wrapper.cast[T]
      else LazyOptional.empty[T]
    }

    override def getColor: Int = tileEntity.getColor

    override def setColor(value: Int): Unit = tileEntity.setColor(value)

    override def controlsConnectivity: Boolean = tileEntity.controlsConnectivity

    override def serializeNBT(): IntTag = IntTag.valueOf(tileEntity.getColor)

    override def deserializeNBT(nbt: IntTag): Unit = {
      nbt match {
        case nbt: IntTag =>
          tileEntity.setColor(nbt.getAsInt)
        case _ =>
      }
    }
  }

  class DefaultImpl extends Colored {
    var color = 0

    override def getColor = color

    override def setColor(value: Int): Unit = color = value

    override def controlsConnectivity = false
  }
}
