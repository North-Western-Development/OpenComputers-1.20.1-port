package li.cil.oc.client.gui

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.OnPress
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Inventory

class Drive(playerInventory: Inventory, val driveStack: () => ItemStack) extends net.minecraft.client.gui.screens.Screen(TextComponent.EMPTY) with traits.Window {
  override val windowHeight = 120

  override def backgroundImage = Textures.GUI.Drive

  protected var managedButton: ImageButton = _
  protected var unmanagedButton: ImageButton = _
  protected var lockedButton: ImageButton = _

  def updateButtonStates(): Unit = {
    val data = new DriveData(driveStack())
    unmanagedButton.toggled = data.isUnmanaged
    managedButton.toggled = !unmanagedButton.toggled
    lockedButton.toggled = data.isLocked
    lockedButton.active = !data.isLocked
  }

  override protected def init(): Unit = {
    super.init()
    minecraft.mouseHandler.releaseMouse()
    KeyMapping.releaseAll()
    managedButton = new ImageButton(leftPos + 11, topPos + 11, 74, 18, (b: Button) => {
      ClientPacketSender.sendDriveMode(unmanaged = false)
      DriveData.setUnmanaged(driveStack(), unmanaged = false)
    }, Textures.GUI.ButtonDriveMode, text = new TextComponent(Localization.Drive.Managed), textColor = 0x608060, canToggle = true)
    unmanagedButton = new ImageButton(leftPos + 91, topPos + 11, 74, 18, (b: Button) => {
      ClientPacketSender.sendDriveMode(unmanaged = true)
      DriveData.setUnmanaged(driveStack(), unmanaged = true)
    }, Textures.GUI.ButtonDriveMode, text = new TextComponent(Localization.Drive.Unmanaged), textColor = 0x608060, canToggle = true)
    lockedButton = new ImageButton(leftPos + 11, topPos + windowHeight - 42, 44, 18, (b: Button) => {
      ClientPacketSender.sendDriveLock()
      DriveData.lock(driveStack(), playerInventory.player)
    }, Textures.GUI.ButtonDriveMode, text = new TextComponent(Localization.Drive.ReadOnlyLock), textColor = 0x608060, canToggle = true)
    addRenderableWidget(managedButton)
    addRenderableWidget(unmanagedButton)
    addRenderableWidget(lockedButton)
    updateButtonStates()
  }

  override def render(stack: PoseStack, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(stack, mouseX, mouseY, dt)
    font.drawWordWrap(new TextComponent(Localization.Drive.Warning), leftPos + 11, topPos + 37, imageWidth - 20, 0x404040)
    font.drawWordWrap(new TextComponent(Localization.Drive.LockWarning), leftPos + 61, topPos + windowHeight - 48, imageWidth - 68, 0x404040)
  }
}
