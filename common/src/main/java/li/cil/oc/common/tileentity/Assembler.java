package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.StateAware;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

public class Assembler extends TileEntity implements Environment, PowerAcceptor, li.cil.oc.common.tileentity.traits.Inventory, SidedEnvironment, StateAware, Tickable, DeviceInfo, MenuProvider {
    public final ComponentConnector node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("assembler").
        withConnector(Settings.get().bufferConverter).
        create();

    public ItemStack output = ItemStack.EMPTY;

    public double totalRequiredEnergy = 0.0;

    public double requiredEnergy = 0.0;

    private Map<String, String> deviceInfo;

    private static final String OutputTag = Settings.namespace + "output";
    private static final String OutputTagCompat = Settings.namespace + "robot";
    private static final String TotalTag = Settings.namespace + "total";
    private static final String RemainingTag = Settings.namespace + "remaining";

    public Assembler(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Assembler",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Factorizer R1D1"
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public Node sidedNode(Direction side) {
        return side != Direction.UP ? node : null;
    }

    @Override
    public boolean hasConnector(Direction side) {
        return canConnect(side);
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(side != Direction.UP ? node : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().assemblerRate;
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (isAssembling()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        else if (canAssemble()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        else return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    // ----------------------------------------------------------------------- //

    public boolean canAssemble() {
        final Optional<AssemblerTemplates.Template> template = AssemblerTemplates.select(getItem(0));
        return template.isPresent() && !isAssembling() && output.isEmpty() && template.get().validate(this).getLeft();
    }

    public boolean isAssembling() {
        return requiredEnergy > 0;
    }

    public double progress() {
        return (1 - requiredEnergy / totalRequiredEnergy) * 100;
    }

    public int timeRemaining() {
        return (int) (requiredEnergy / Settings.get().assemblerTickAmount / 20);
    }

    // ----------------------------------------------------------------------- //

    public boolean start(boolean finishImmediately) {
        synchronized (this) {
            final Optional<AssemblerTemplates.Template> selected = AssemblerTemplates.select(getItem(0));
            if (selected.isPresent() && !isAssembling() && output.isEmpty() && selected.get().validate(this).getLeft()) {
                final AssemblerTemplates.Template template = selected.get();
                for (int slot = 0; slot < getContainerSize(); slot++) {
                    final ItemStack stack = getItem(slot);
                    if (!stack.isEmpty() && !canPlaceItem(slot, stack)) return false;
                }
                final Pair<ItemStack, Double> assembled = template.assemble(this);
                output = assembled.getLeft() == null ? ItemStack.EMPTY : assembled.getLeft();
                if (finishImmediately) {
                    totalRequiredEnergy = 0;
                } else {
                    totalRequiredEnergy = Math.max(1, assembled.getRight());
                }
                requiredEnergy = totalRequiredEnergy;
                PacketSender.sendRobotAssembling(this, true);

                for (int slot = 0; slot < getContainerSize(); slot++) updateItems(slot, ItemStack.EMPTY);
                setChanged();

                return true;
            }
            return false;
        }
    }

    public boolean start() {
        return start(false);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(): string, number or boolean -- The current state of the assembler, `busy' or `idle', followed by the progress or template validity, respectively.")
    public Object[] status(Context context, Arguments args) {
        if (isAssembling()) return result("busy", progress());
        final Optional<AssemblerTemplates.Template> template = AssemblerTemplates.select(getItem(0));
        if (template.isPresent() && template.get().validate(this).getLeft()) return result("idle", true);
        return result("idle", false);
    }

    @Callback(doc = "function():boolean -- Start assembling, if possible. Returns whether assembly was started or not.")
    public Object[] start(Context context, Arguments args) {
        return result(start());
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!output.isEmpty() && getLevel().getGameTime() % Settings.get().tickFrequency == 0) {
            final double want = Math.max(1, Math.min(requiredEnergy, Settings.get().assemblerTickAmount * Settings.get().tickFrequency));
            final double have = want + (Settings.get().ignorePower ? 0 : node.changeBuffer(-want));
            requiredEnergy -= have;
            if (requiredEnergy <= 0) {
                setItem(0, output);
                output = ItemStack.EMPTY;
                requiredEnergy = 0;
            }
            PacketSender.sendRobotAssembling(this, have > 0.5 && !output.isEmpty());
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        if (nbt.contains(OutputTag)) {
            output = ItemStack.of(nbt.getCompound(OutputTag));
        } else if (nbt.contains(OutputTagCompat)) {
            output = ItemStack.of(nbt.getCompound(OutputTagCompat));
        }
        totalRequiredEnergy = nbt.getDouble(TotalTag);
        requiredEnergy = nbt.getDouble(RemainingTag);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, OutputTag, output::save);
        nbt.putDouble(TotalTag, totalRequiredEnergy);
        nbt.putDouble(RemainingTag, requiredEnergy);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        requiredEnergy = nbt.getDouble(RemainingTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        nbt.putDouble(RemainingTag, requiredEnergy);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getContainerSize() {
        return 22;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) {
            return !isAssembling() && AssemblerTemplates.select(stack).isPresent();
        }
        final Optional<AssemblerTemplates.Template> selected = AssemblerTemplates.select(getItem(0));
        if (selected.isEmpty()) return false;
        final AssemblerTemplates.Template template = selected.get();
        final AssemblerTemplates.Slot tplSlot;
        if (slot >= 1 && slot < 4) tplSlot = template.containerSlots[slot - 1];
        else if (slot >= 4 && slot < 13) tplSlot = template.upgradeSlots[slot - 4];
        else if (slot >= 13 && slot < 21) tplSlot = template.componentSlots[slot - 13];
        else tplSlot = AssemblerTemplates.NoSlot;
        return tplSlot.validate(this, slot, stack);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Assembler(ContainerTypes.ASSEMBLER.get(), id, playerInventory, this);
    }
}
