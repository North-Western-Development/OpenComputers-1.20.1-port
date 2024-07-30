package li.cil.oc.common.container

import li.cil.oc.api.Driver
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToScala._

abstract class ComponentSlot(inventory: Container, index: Int, x: Int, y: Int, host: Class[_ <: EnvironmentHost]) extends Slot(inventory, index, x, y) {
  def agentContainer: li.cil.oc.common.container.Player

  def slot: String

  def tier: Int

  @OnlyIn(Dist.CLIENT)
  def tierIcon: ResourceLocation

  var changeListener: Option[Slot => Unit] = None

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  def hasBackground = getBackgroundLocation != null

  @OnlyIn(Dist.CLIENT)
  def getBackgroundLocation: ResourceLocation = null

  @OnlyIn(Dist.CLIENT)
  override def isActive = slot != common.Slot.None && tier != common.Tier.None && super.isActive

  override def mayPlace(stack: ItemStack): Boolean = {
    if (!inventory.canPlaceItem(getSlotIndex, stack)) return false
    if (slot == common.Slot.None || tier == common.Tier.None) return false
    if (slot == common.Slot.Any && tier == common.Tier.Any) return true
    // Special case: tool slots fit everything.
    if (slot == common.Slot.Tool) return true
    Option(Driver.driverFor(stack, host)) match {
      case Some(driver) => {
        val slotOk = (slot == common.Slot.Any || driver.slot(stack) == slot)
        val tierOk = (tier == common.Tier.Any || driver.tier(stack) <= tier)
        slotOk && tierOk
      }
      case _ => false
    }
  }

  override def onTake(player: net.minecraft.world.entity.player.Player, stack: ItemStack) = {
    for (slot <- agentContainer.slots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(player)
      case _ =>
    }
    super.onTake(player, stack)
  }

  override def set(stack: ItemStack): Unit = {
    super.set(stack)
    inventory match {
      case playerAware: common.tileentity.traits.PlayerInputAware =>
        playerAware.onSetInventorySlotContents(agentContainer.playerInventory.player, getSlotIndex, stack)
      case _ =>
    }
  }

  override def setChanged() {
    super.setChanged()
    for (slot <- agentContainer.slots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(agentContainer.playerInventory.player)
      case _ =>
    }
    changeListener.foreach(_(this))
  }

  protected def clearIfInvalid(player: net.minecraft.world.entity.player.Player) {}
}