package li.cil.oc.integration.cofh.foundation

import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModThermalFoundation extends ModProxy {
  override def getMod = Mods.ThermalFoundation

  override def initialize() {
    //Driver.add(new DriverEnergyInfo) // FIXME
    api.IMC.registerWrenchTool("li.cil.oc.integration.cofh.foundation.EventHandlerFoundation.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.cofh.foundation.EventHandlerFoundation.isWrench")
  }
}