package li.cil.oc.common.tileentity.traits

import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

// Used to get notifications from containers when a player changes a slot in
// this inventory. Normally the player causing a setItem is
// unavailable. Using this we gain access to the causing player, allowing for
// some player-specific logic, such as the disassembler working instantaneously
// when used by a player in creative mode.
trait PlayerInputAware extends Container {
  def onSetInventorySlotContents(player: Player, slot: Int, stack: ItemStack): Unit
}
