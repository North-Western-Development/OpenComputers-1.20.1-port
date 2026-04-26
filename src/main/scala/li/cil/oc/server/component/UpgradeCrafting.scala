package li.cil.oc.server.component

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.{Network, internal}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.{AbstractContainerMenu, ResultContainer, ResultSlot}
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.{Container, inventory}

import java.util
import scala.collection.convert.ImplicitConversionsToJava._

class UpgradeCrafting(val host: EnvironmentHost with internal.Robot) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("crafting").
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Assembly controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "MultiCombinator-9S"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  @Callback(doc = """function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.""")
  def craft(context: Context, args: Arguments): Array[AnyRef] = {
    val count = args.optInteger(0, 64) max 0 min 64
    result(CraftingInventory.craft(count): _*)
  }

  private object CraftingInventory extends inventory.CraftingContainer(new AbstractContainerMenu(null, 0) {
    override def stillValid(player: Player) = true
  }, 3, 3) {
    def craft(wantedCount: Int): Seq[_] = {
      val player = host.player
      copyItemsFromHost(player.inventory)
      var countCrafted = 0
      val manager = host.world.getRecipeManager
      val initialCraft = manager.getRecipeFor(RecipeType.CRAFTING, CraftingInventory: inventory.CraftingContainer, host.world)
      if (initialCraft.isPresent) {
        def tryCraft() : Boolean = {
          val craft = manager.getRecipeFor(RecipeType.CRAFTING, CraftingInventory: inventory.CraftingContainer, host.world)
          if (craft != initialCraft) {
            return false
          }

          val craftResult = new ResultContainer
          val craftingSlot = new ResultSlot(player, CraftingInventory, craftResult, 0, 0, 0)
          val craftedResult = craft.get.assemble(this)
          craftResult.setItem(0, craftedResult)
          if (!craftingSlot.hasItem)
            return false

          val stack = craftingSlot.remove(1)
          countCrafted += stack.getCount max 1
          copyItemsToHost(player.inventory)
          craftingSlot.onTake(player, stack)
          copyItemsFromHost(player.inventory)
          true
        }
        while (countCrafted < wantedCount && tryCraft()) {
          //
        }
      }
      Seq(countCrafted > 0, countCrafted)
    }

    def copyItemsFromHost(inventory: Container) {
      for (slot <- 0 until getContainerSize) {
        val stack = inventory.getItem(toParentSlot(slot))
        setItem(slot, stack)
      }
    }

    def copyItemsToHost(inventory: Container) {
      for (slot <- 0 until getContainerSize) {
        inventory.setItem(toParentSlot(slot), getItem(slot))
      }
    }

    private def toParentSlot(slot: Int) = {
      val col = slot % 3
      val row = slot / 3
      row * 4 + col
    }
  }
}
