package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.client.Textures;
import li.cil.oc.common.InventorySlots.InventorySlot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.template.AssemblerTemplates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Function;

public class Assembler extends Player {
    public final Container assembler;

    public Assembler(MenuType<?> selfType, int id, Inventory playerInventory, Container assembler) {
        super(selfType, id, playerInventory, assembler);
        this.assembler = assembler;

        // Computer case.
        {
            int index = slots.size();
            addSlot(new StaticComponentSlot(this, otherInventory, index, 12, 12, getHostClass(), "template", Tier.Any) {
                @Override
                public boolean isActive() {
                    return !isAssembling() && super.isActive();
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!container.canPlaceItem(getContainerSlot(), stack)) return false;
                    if (isAssembling()) return false;
                    return AssemblerTemplates.select(stack).isPresent();
                }

                @Override
                public ResourceLocation getBackgroundLocation() {
                    return isAssembling() ? Textures.Icons.get(Tier.None) : super.getBackgroundLocation();
                }
            });
        }

        // Component containers.
        for (int i = 0; i < 3; i++) {
            addSlotToContainer(34 + i * slotSize, 70, this::slotInfo);
        }

        // Components.
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(34 + (i % 3) * slotSize, 12 + (i / 3) * slotSize, this::slotInfo);
        }

        // Cards.
        for (int i = 0; i < 3; i++) {
            addSlotToContainer(104, 12 + i * slotSize, this::slotInfo);
        }

        // CPU.
        addSlotToContainer(126, 12, this::slotInfo);

        // RAM.
        for (int i = 0; i < 2; i++) {
            addSlotToContainer(126, 30 + i * slotSize, this::slotInfo);
        }

        // Floppy/EEPROM + HDDs.
        for (int i = 0; i < 3; i++) {
            addSlotToContainer(148, 12 + i * slotSize, this::slotInfo);
        }

        // Show the player's inventory.
        addPlayerInventorySlots(8, 110);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Assembler.class;
    }

    private static AssemblerTemplates.Slot templateSlot(AssemblerTemplates.Template template, int index) {
        if (index >= 1 && index < 4) return template.containerSlots[index - 1];
        else if (index >= 4 && index < 13) return template.upgradeSlots[index - 4];
        else if (index >= 13 && index < 21) return template.componentSlots[index - 13];
        else return AssemblerTemplates.NoSlot;
    }

    private InventorySlot slotInfo(DynamicComponentSlot slot) {
        Optional<AssemblerTemplates.Template> template = AssemblerTemplates.select(getSlot(0).getItem());
        if (template.isPresent()) {
            AssemblerTemplates.Slot tplSlot = templateSlot(template.get(), slot.getContainerSlot());
            return new InventorySlot(tplSlot.kind, tplSlot.tier);
        }
        return new InventorySlot(li.cil.oc.common.Slot.None, Tier.None);
    }

    @Override
    public void addSlotToContainer(int x, int y, Function<DynamicComponentSlot, InventorySlot> info) {
        int index = slots.size();
        addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, getHostClass(), info, () -> Tier.One) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!super.mayPlace(stack)) return false;
                Optional<AssemblerTemplates.Template> template = AssemblerTemplates.select(Assembler.this.getSlot(0).getItem());
                if (template.isPresent()) {
                    AssemblerTemplates.Slot tplSlot = templateSlot(template.get(), getContainerSlot());
                    return tplSlot.validate(assembler, getContainerSlot(), stack);
                }
                return false;
            }
        });
    }

    public boolean isAssembling() {
        return synchronizedData.getBoolean("isAssembling");
    }

    public double assemblyProgress() {
        return synchronizedData.getDouble("assemblyProgress");
    }

    public int assemblyRemainingTime() {
        return synchronizedData.getInt("assemblyRemainingTime");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        if (assembler instanceof li.cil.oc.common.tileentity.Assembler te) {
            synchronizedData.putBoolean("isAssembling", te.isAssembling());
            synchronizedData.putDouble("assemblyProgress", te.progress());
            synchronizedData.putInt("assemblyRemainingTime", te.timeRemaining());
        }
        super.detectCustomDataChanges(nbt);
    }
}
