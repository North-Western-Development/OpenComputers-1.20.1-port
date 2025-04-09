package li.cil.oc.integration.tis3d

import li.cil.oc.OpenComputers
import li.cil.oc.integration.{ModProxy, Mods}
import li.cil.tis3d.api.serial.SerialInterfaceProvider
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.RegisterEvent

import java.util.function.Supplier

object ModTIS3D extends ModProxy {
  override def getMod = Mods.TIS3D

  @SubscribeEvent
  def registerSerialInterfaceProviders(e: RegisterEvent) {
    e.register(SerialInterfaceProvider.REGISTRY,
      new ResourceLocation(OpenComputers.ID, "serial_interface"),
      new Supplier[SerialInterfaceProvider] {
        override def get(): SerialInterfaceProvider = SerialInterfaceProviderAdapter
      })
  }

  override def preInitialize(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }
}
