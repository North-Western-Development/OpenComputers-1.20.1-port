package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraftforge.items.IItemHandler

trait InventoryLevelControlMk2 extends InventoryAware with LevelAware with SideRestricted {
  @Callback(doc = """function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Drops the selected item stack into the specified slot of an inventory.""")
  def dropIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optItemCount(2)
    val fromSide = args.optSideAny(3, facing.getOpposite)
    val stack = inventory.getItem(selectedSlot)
    if (!stack.isEmpty && stack.getCount > 0) {
      withInventory(position.offset(facing), fromSide, inventory => {
        val slot = args.checkSlot(inventory, 1)
        if (!InventoryUtils.insertIntoInventorySlot(stack, inventory, slot, count)) {
          // Cannot drop into that inventory.
          return result(false, "inventory full/invalid slot")
        }
        else if (stack.getCount == 0) {
          // Dropped whole stack.
          this.inventory.setItem(selectedSlot, ItemStack.EMPTY)
        }
        else {
          // Dropped partial stack.
          this.inventory.setChanged()
        }

        context.pause(Settings.get.dropDelay)

        result(true)
      })
    }
    else result(false)
  }

  @Callback(doc = """function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Sucks items from the specified slot of an inventory.""")
  def suckFromSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optItemCount(2)
    val fromSide = args.optSideAny(3, facing.getOpposite)
    withInventory(position.offset(facing), fromSide, inventory => {
      val slot = args.checkSlot(inventory, 1)
      val extracted = InventoryUtils.extractFromInventorySlot((is, sim) => InventoryUtils.insertIntoInventory(is, InventoryUtils.asItemHandler(this.inventory), slots = Option(insertionSlots), simulate = sim), inventory, slot, count)
      if (extracted > 0) {
        context.pause(Settings.get.suckDelay)
        result(extracted)
      }
      else result(false)
    })
  }

  private def withInventory(blockPos: BlockPosition, fromSide: Direction, f: IItemHandler => Array[AnyRef]) =
    InventoryUtils.inventorySourceAt(blockPos, fromSide) match {
      case Some(inventory) if mayInteract(inventory) => f(inventory.inventory)
      case _ => result((), "no inventory")
    }
}
