package li.cil.oc.integration.tis3d

import li.cil.oc.OpenComputers
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.tis3d.api.serial.SerialInterfaceProvider
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.RegisterEvent
import net.minecraftforge.registries.RegisterEvent.RegisterHelper

import java.util.function.Consumer

object ModTIS3D extends ModProxy {
  override def getMod = Mods.TIS3D

  @SubscribeEvent
  def registerSerialInterfaceProviders(e: RegisterEvent) {
    e.register(
      SerialInterfaceProvider.REGISTRY,
      new Consumer[RegisterHelper[SerialInterfaceProvider]] {
        override def accept(helper: RegisterHelper[SerialInterfaceProvider]): Unit = {
          helper.register(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "serial_port"),
            SerialInterfaceProviderAdapter
          )
        }
      }
    )
  }

  override def preInitialize(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }
}
