package li.cil.oc.integration.cofh.tileentity

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModCoFHBlockEntity extends ModProxy {
  override def getMod = Mods.CoFHCore

  override def initialize() {
    Driver.add(new DriverRedstoneControl)
    Driver.add(new DriverSecureTile)
  }
}