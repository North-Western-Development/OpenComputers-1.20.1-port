package li.cil.oc.common.recipe

import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.common.Loot
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.StackOption
import net.minecraft.core.{NonNullList, RegistryAccess}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.{CraftingRecipe, Ingredient}
import net.minecraft.world.level.Level

import scala.collection.immutable

class LootDiskCyclingRecipe(val getId: ResourceLocation) extends CraftingRecipe {
  val ingredients = NonNullList.create[Ingredient]
  ingredients.add(Ingredient.of(Loot.disksForCycling.toArray: _*))
  ingredients.add(Ingredient.of(api.Items.get(Constants.ItemName.Wrench).createItemStack(1)))

  override def matches(crafting: CraftingContainer, world: Level): Boolean = {
    val stacks = collectStacks(crafting).toArray
    stacks.length == 2 && stacks.exists(Loot.isLootDisk) && stacks.exists(Wrench.isWrench)
  }

  override def assemble(crafting: CraftingContainer, registryAccess: RegistryAccess): ItemStack = {
    val lootDiskStacks = Loot.disksForCycling
    collectStacks(crafting).find(Loot.isLootDisk) match {
      case Some(lootDisk) if lootDiskStacks.nonEmpty =>
        val lootFactoryName = getLootFactoryName(lootDisk)
        val oldIndex = lootDiskStacks.indexWhere(s => getLootFactoryName(s) == lootFactoryName)
        val newIndex = (oldIndex + 1) % lootDiskStacks.length
        lootDiskStacks(newIndex).copy()
      case _ => ItemStack.EMPTY
    }
  }

  def getLootFactoryName(stack: ItemStack): String = stack.getTag.getString(Settings.namespace + "lootFactory")

  def collectStacks(crafting: CraftingContainer): immutable.IndexedSeq[ItemStack] = (0 until crafting.getContainerSize).flatMap(i => StackOption(crafting.getItem(i)))

  override def canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

  override def getResultItem(registryAccess: RegistryAccess) = Loot.disksForCycling.headOption match {
    case Some(lootDisk) => lootDisk
    case _ => ItemStack.EMPTY
  }

  override def getRemainingItems(crafting: CraftingContainer): NonNullList[ItemStack] = {
    val result = NonNullList.withSize[ItemStack](crafting.getContainerSize, ItemStack.EMPTY)
    for (slot <- 0 until crafting.getContainerSize) {
      val stack = crafting.getItem(slot)
      if (Wrench.isWrench(stack)) {
        result.set(slot, stack.copy())
        stack.setCount(0)
      }
    }
    result
  }

  override def getIngredients = ingredients

  override def getSerializer = RecipeSerializers.CRAFTING_LOOTDISK_CYCLING
}
