package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.{GuiGraphics, screens}
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class Drive(playerInventory:Inventory, val driveStack: () => ItemStack) extends screens.Screen(Component.empty()) with traits.Window {
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
    managedButton = new ImageButton(leftPos + 11, topPos + 11, 74, 18, new Button.OnPress {
      override def onPress(b: Button) = {
        ClientPacketSender.sendDriveMode(unmanaged = false)
        DriveData.setUnmanaged(driveStack(), unmanaged = false)
      }
    }, Textures.GUI.ButtonDriveMode, text = Component.literal(Localization.Drive.Managed), textColor = 0x608060, canToggle = true)
    unmanagedButton = new ImageButton(leftPos + 91, topPos + 11, 74, 18, new Button.OnPress {
      override def onPress(b: Button) = {
        ClientPacketSender.sendDriveMode(unmanaged = true)
        DriveData.setUnmanaged(driveStack(), unmanaged = true)
      }
    }, Textures.GUI.ButtonDriveMode, text = Component.literal(Localization.Drive.Unmanaged), textColor = 0x608060, canToggle = true)
    lockedButton = new ImageButton(leftPos + 11, topPos + windowHeight - 42, 44, 18, new Button.OnPress {
      override def onPress(b: Button) = {
        ClientPacketSender.sendDriveLock()
        DriveData.lock(driveStack(), playerInventory.player)
      }
    }, Textures.GUI.ButtonDriveMode, text = Component.literal(Localization.Drive.ReadOnlyLock), textColor = 0x608060, canToggle = true)
    addWidget(managedButton)
    addWidget(unmanagedButton)
    addWidget(lockedButton)
    updateButtonStates()
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(guiGraphics, mouseX, mouseY, dt)
    guiGraphics.drawWordWrap(font, Component.literal(Localization.Drive.Warning), leftPos + 11, topPos + 37, imageWidth - 20, 0x404040)
    guiGraphics.drawWordWrap(font, Component.literal(Localization.Drive.LockWarning), leftPos + 61, topPos + windowHeight - 48, imageWidth - 68, 0x404040)
  }
}
