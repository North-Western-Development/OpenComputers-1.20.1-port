package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.Window;
import li.cil.oc.common.item.data.DriveData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class Drive extends Window {
    private final Inventory playerInventory;
    public final Supplier<ItemStack> driveStack;

    protected ImageButton managedButton;
    protected ImageButton unmanagedButton;
    protected ImageButton lockedButton;

    public Drive(Inventory playerInventory, Supplier<ItemStack> driveStack) {
        super(Component.empty());
        this.playerInventory = playerInventory;
        this.driveStack = driveStack;
    }

    @Override
    public int windowHeight() {
        return 120;
    }

    @Override
    public ResourceLocation backgroundImage() {
        return Textures.GUI.Drive;
    }

    public void updateButtonStates() {
        final DriveData data = new DriveData(driveStack.get());
        unmanagedButton.toggled = data.isUnmanaged;
        managedButton.toggled = !unmanagedButton.toggled;
        lockedButton.toggled = data.isLocked();
        lockedButton.active = !data.isLocked();
    }

    @Override
    protected void init() {
        super.init();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
        managedButton = new ImageButton(leftPos + 11, topPos + 11, 74, 18, b -> {
            PacketSender.sendDriveMode(false);
            DriveData.setUnmanaged(driveStack.get(), false);
        }, Textures.GUI.ButtonDriveMode, Component.literal(Localization.Drive.Managed()), true, 0x608060, 0xA0A0A0, 0xFFFFA0, -1);
        unmanagedButton = new ImageButton(leftPos + 91, topPos + 11, 74, 18, b -> {
            PacketSender.sendDriveMode(true);
            DriveData.setUnmanaged(driveStack.get(), true);
        }, Textures.GUI.ButtonDriveMode, Component.literal(Localization.Drive.Unmanaged()), true, 0x608060, 0xA0A0A0, 0xFFFFA0, -1);
        lockedButton = new ImageButton(leftPos + 11, topPos + windowHeight() - 42, 44, 18, b -> {
            PacketSender.sendDriveLock();
            DriveData.lock(driveStack.get(), playerInventory.player);
        }, Textures.GUI.ButtonDriveMode, Component.literal(Localization.Drive.ReadOnlyLock()), true, 0x608060, 0xA0A0A0, 0xFFFFA0, -1);
        addRenderableWidget(managedButton);
        addRenderableWidget(unmanagedButton);
        addRenderableWidget(lockedButton);
        updateButtonStates();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        super.render(graphics, mouseX, mouseY, dt);
        graphics.drawWordWrap(font, Component.literal(Localization.Drive.Warning()), leftPos + 11, topPos + 37, imageWidth - 20, 0x404040);
        graphics.drawWordWrap(font, Component.literal(Localization.Drive.LockWarning()), leftPos + 61, topPos + windowHeight() - 48, imageWidth - 68, 0x404040);
    }
}
