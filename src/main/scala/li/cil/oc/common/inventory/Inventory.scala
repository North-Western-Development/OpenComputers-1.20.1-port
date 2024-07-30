package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.{CompoundTag, Tag}
import net.minecraft.network.chat.TranslatableComponent

trait Inventory extends SimpleInventory {
  def items: Array[ItemStack]

  def updateItems(slot: Int, stack: ItemStack): Unit = items(slot) = StackOption(stack).orEmpty

  // ----------------------------------------------------------------------- //

  override def getItem(slot: Int): ItemStack =
    if (slot >= 0 && slot < getContainerSize) items(slot)
    else ItemStack.EMPTY

  override def setItem(slot: Int, stack: ItemStack): Unit = {
    if (slot >= 0 && slot < getContainerSize) {
      if (stack.isEmpty && items(slot).isEmpty) {
        return
      }
      if (items(slot) == stack) {
        return
      }

      val oldStack = items(slot)
      updateItems(slot, ItemStack.EMPTY)
      if (!oldStack.isEmpty) {
        onItemRemoved(slot, oldStack)
      }
      if (!stack.isEmpty && stack.getCount >= getInventoryStackRequired) {
        if (stack.getCount > getMaxStackSize) {
          stack.setCount(getMaxStackSize)
        }
        updateItems(slot, stack)
      }

      if (!items(slot).isEmpty) {
        onItemAdded(slot, items(slot))
      }

      setChanged()
    }
  }

  override def getName: TranslatableComponent = new TranslatableComponent(Settings.namespace + "container." + inventoryName)

  protected def inventoryName: String = getClass.getSimpleName.toLowerCase

  override def isEmpty: Boolean = items.forall(_.isEmpty)

  // ----------------------------------------------------------------------- //

  private final val ItemsTag = Settings.namespace + "items"
  private final val SlotTag = "slot"
  private final val ItemTag = "item"

  def loadData(nbt: CompoundTag) {
    nbt.getList(ItemsTag, Tag.TAG_COMPOUND).foreach((tag: CompoundTag) => {
      if (tag.contains(SlotTag)) {
        val slot = tag.getByte(SlotTag)
        if (slot >= 0 && slot < items.length) {
          updateItems(slot, ItemStack.of(tag.getCompound(ItemTag)))
        }
      }
    })
  }

  def saveData(nbt: CompoundTag) {
    nbt.setNewTagList(ItemsTag,
      items.zipWithIndex collect {
        case (stack, slot) if !stack.isEmpty => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new CompoundTag()
          slotNbt.putByte(SlotTag, slot.toByte)
          slotNbt.setNewCompoundTag(ItemTag, stack.save)
      })
  }

  // ----------------------------------------------------------------------- //

  protected def onItemAdded(slot: Int, stack: ItemStack) {}

  protected def onItemRemoved(slot: Int, stack: ItemStack) {}
}
