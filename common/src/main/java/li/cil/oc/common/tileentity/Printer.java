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
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.StateAware;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Printer extends TileEntity implements Environment, li.cil.oc.common.tileentity.traits.Inventory, Rotatable, SidedEnvironment, StateAware, Tickable, WorldlyContainer, DeviceInfo, MenuProvider {
    public final ComponentConnector node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("printer3d").
        withConnector(Settings.get().bufferConverter).
        create();

    public final int maxAmountMaterial = 256000;
    public int amountMaterial = 0;
    public final int maxAmountInk = 100000;
    public int amountInk = 0;

    public PrintData data = new PrintData();
    public boolean isActive = false;
    public int limit = 0;
    public ItemStack output = ItemStack.EMPTY;
    public double totalRequiredEnergy = 0.0;
    public double requiredEnergy = 0.0;

    public final int slotMaterial = 0;
    public final int slotInk = 1;
    public final int slotOutput = 2;

    private Map<String, String> deviceInfo;

    private static final String AmountMaterialTag = Settings.namespace + "amountMaterial";
    private static final String AmountInkTag = Settings.namespace + "amountInk";
    private static final String DataTag = Settings.namespace + "data";
    private static final String IsActiveTag = Settings.namespace + "active";
    private static final String LimitTag = Settings.namespace + "limit";
    private static final String OutputTag = Settings.namespace + "output";
    private static final String TotalTag = Settings.namespace + "total";
    private static final String RemainingTag = Settings.namespace + "remaining";

    public Printer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
                DeviceAttribute.Class, DeviceClass.Printer,
                DeviceAttribute.Description, "3D Printer",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Omni-Materializer T6.1"
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
    public ComponentConnector sidedNode(Direction side) {
        return side != Direction.UP ? node : null;
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (isPrinting()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        else if (canPrint()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        else return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    // ----------------------------------------------------------------------- //

    public boolean canPrint() {
        return !data.stateOff.isEmpty() && data.stateOff.size() <= Settings.get().maxPrintComplexity && data.stateOn.size() <= Settings.get().maxPrintComplexity;
    }

    public boolean isPrinting() {
        return !output.isEmpty();
    }

    public double progress() {
        return (1 - requiredEnergy / totalRequiredEnergy) * 100;
    }

    public int timeRemaining() {
        return (int) (requiredEnergy / Settings.get().assemblerTickAmount / 20);
    }

    private static Optional<String> take(String value, int count) {
        if (value == null) return Optional.empty();
        return Optional.of(value.length() > count ? value.substring(0, count) : value);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function() -- Resets the configuration of the printer and stop printing (current job will finish).")
    public Object[] reset(Context context, Arguments args) {
        data = new PrintData();
        isActive = false; // Needs committing.
        return null;
    }

    @Callback(doc = "function(value:string) -- Set a label for the block being printed.")
    public Object[] setLabel(Context context, Arguments args) {
        data.label = take(args.optString(0, null), 24);
        if (data.label.map(String::isEmpty).orElse(false)) data.label = Optional.empty();
        isActive = false; // Needs committing.
        return null;
    }

    @Callback(doc = "function():string -- Get the current label for the block being printed.")
    public Object[] getLabel(Context context, Arguments args) {
        return result(data.label.orElse(null));
    }

    @Callback(doc = "function(value:string) -- Set a tooltip for the block being printed.")
    public Object[] setTooltip(Context context, Arguments args) {
        data.tooltip = take(args.optString(0, null), 128);
        if (data.tooltip.map(String::isEmpty).orElse(false)) data.tooltip = Optional.empty();
        isActive = false; // Needs committing.
        return null;
    }

    @Callback(doc = "function():string -- Get the current tooltip for the block being printed.")
    public Object[] getTooltip(Context context, Arguments args) {
        return result(data.tooltip.orElse(null));
    }

    @Callback(doc = "function(value:number) -- Set what light level the printed block should have.")
    public Object[] setLightLevel(Context context, Arguments args) {
        data.lightLevel = Math.min(Math.max(args.checkInteger(0), 0), Settings.get().maxPrintLightLevel);
        isActive = false; // Needs committing.
        return null;
    }

    @Callback(doc = "function():number -- Get which light level the printed block should have.")
    public Object[] getLightLevel(Context context, Arguments args) {
        return result(data.lightLevel);
    }

    @Callback(doc = "function(value:boolean or number) -- Set whether the printed block should emit redstone when in its active state.")
    public Object[] setRedstoneEmitter(Context context, Arguments args) {
        if (args.isBoolean(0)) data.redstoneLevel = args.checkBoolean(0) ? 15 : 0;
        else data.redstoneLevel = Math.min(Math.max(args.checkInteger(0), 0), 15);
        isActive = false; // Needs committing.
        return null;
    }

    @Callback(doc = "function():boolean, number -- Get whether the printed block should emit redstone when in its active state.")
    public Object[] isRedstoneEmitter(Context context, Arguments args) {
        return result(data.emitRedstone(), data.redstoneLevel);
    }

    @Callback(doc = "function(value:boolean) -- Set whether the printed block should automatically return to its off state.")
    public Object[] setButtonMode(Context context, Arguments args) {
        data.isButtonMode = args.checkBoolean(0);
        isActive = false; // Needs committing.
        return null;
    }

    @Callback(doc = "function():boolean -- Get whether the printed block should automatically return to its off state.")
    public Object[] isButtonMode(Context context, Arguments args) {
        return result(data.isButtonMode);
    }

    @Callback(doc = "function(collideOff:boolean, collideOn:boolean) -- Set whether the printed block should be collidable or not.")
    public Object[] setCollidable(Context context, Arguments args) {
        final boolean collideOff = args.checkBoolean(0);
        final boolean collideOn = args.checkBoolean(1);
        data.noclipOff = !collideOff;
        data.noclipOn = !collideOn;
        return null;
    }

    @Callback(doc = "function():boolean, boolean -- Get whether the printed block should be collidable or not.")
    public Object[] isCollidable(Context context, Arguments args) {
        return result(!data.noclipOff, !data.noclipOn);
    }

    @Callback(doc = "function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean=false][,tint:number]) -- Adds a shape to the printers configuration, optionally specifying whether it is for the off or on state.")
    public Object[] addShape(Context context, Arguments args) {
        if (data.stateOff.size() > Settings.get().maxPrintComplexity || data.stateOn.size() > Settings.get().maxPrintComplexity) {
            return result(ResultWrapper.unit, "model too complex");
        }
        final float minX = Math.min(Math.max(args.checkInteger(0), 0), 16) / 16f;
        final float minY = Math.min(Math.max(args.checkInteger(1), 0), 16) / 16f;
        final float minZ = (16 - Math.min(Math.max(args.checkInteger(2), 0), 16)) / 16f;
        final float maxX = Math.min(Math.max(args.checkInteger(3), 0), 16) / 16f;
        final float maxY = Math.min(Math.max(args.checkInteger(4), 0), 16) / 16f;
        final float maxZ = (16 - Math.min(Math.max(args.checkInteger(5), 0), 16)) / 16f;
        final String texture = take(args.checkString(6), 64).orElse("");
        final boolean state = args.isBoolean(7) && args.checkBoolean(7);
        final Optional<Integer> tint = args.isInteger(7) ? Optional.of(args.checkInteger(7)) : args.isInteger(8) ? Optional.of(args.checkInteger(8)) : Optional.empty();

        if (minX == maxX) throw new IllegalArgumentException("empty block");
        if (minY == maxY) throw new IllegalArgumentException("empty block");
        if (minZ == maxZ) throw new IllegalArgumentException("empty block");

        final Set<PrintData.Shape> list = state ? data.stateOn : data.stateOff;
        list.add(new PrintData.Shape(new AABB(
            Math.min(minX, maxX),
            Math.min(minY, maxY),
            Math.min(minZ, maxZ),
            Math.max(maxX, minX),
            Math.max(maxY, minY),
            Math.max(maxZ, minZ)),
            texture, tint));
        isActive = false; // Needs committing.

        final BlockState blockState = getLevel().getBlockState(getBlockPos());
        getLevel().sendBlockUpdated(getBlockPos(), blockState, blockState, 3);

        return result(true);
    }

    @Callback(doc = "function():number -- Get the number of shapes in the current configuration.")
    public Object[] getShapeCount(Context context, Arguments args) {
        return result(data.stateOff.size(), data.stateOn.size());
    }

    @Callback(doc = "function():number -- Get the maximum allowed number of shapes.")
    public Object[] getMaxShapeCount(Context context, Arguments args) {
        return result(Settings.get().maxPrintComplexity);
    }

    @Callback(doc = "function([count:number]):boolean -- Commit and begin printing the current configuration.")
    public Object[] commit(Context context, Arguments args) {
        if (!canPrint()) {
            return result(ResultWrapper.unit, "model invalid");
        }
        limit = (int) Math.min(Math.max(args.optDouble(0, 1), 0), Integer.MAX_VALUE);
        isActive = limit > 0;
        return result(true);
    }

    @Callback(doc = "function(): string, number or boolean -- The current state of the printer, `busy' or `idle', followed by the progress or model validity, respectively.")
    public Object[] status(Context context, Arguments args) {
        if (isPrinting()) return result("busy", progress());
        else if (canPrint()) return result("idle", true);
        else return result("idle", false);
    }

    // ----------------------------------------------------------------------- //

    private boolean canMergeOutput() {
        final ItemStack presentStack = getItem(slotOutput);
        final ItemStack outputStack = data.createItemStack();
        return presentStack.isEmpty() || ItemStack.isSameItemSameTags(presentStack, outputStack);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (isClient()) {
            return;
        }

        if (isActive && output.isEmpty() && canMergeOutput()) {
            final Optional<Pair<Integer, Integer>> costs = PrintData.computeCosts(data);
            if (costs.isPresent()) {
                final int materialRequired = costs.get().getLeft();
                final int inkRequired = costs.get().getRight();
                totalRequiredEnergy = Settings.get().printCost;
                requiredEnergy = totalRequiredEnergy;

                if (amountMaterial >= materialRequired && amountInk >= inkRequired) {
                    amountMaterial -= materialRequired;
                    amountInk -= inkRequired;
                    limit -= 1;
                    output = data.createItemStack();
                    if (limit < 1) isActive = false;
                    PacketSender.sendPrinting(this, true);
                }
            } else {
                isActive = false;
                data = new PrintData();
            }
        }

        if (!output.isEmpty()) {
            final double want = Math.max(1, Math.min(requiredEnergy, Settings.get().printerTickAmount));
            final double have = want + (Settings.get().ignorePower ? 0 : node.changeBuffer(-want));
            requiredEnergy -= have;
            if (requiredEnergy <= 0) {
                final ItemStack result = getItem(slotOutput);
                if (result.isEmpty()) {
                    setItem(slotOutput, output);
                } else if (result.getCount() < result.getMaxStackSize() && canMergeOutput() /* Should never fail, but just in case... */) {
                    result.grow(1);
                    setChanged();
                } else {
                    return;
                }
                requiredEnergy = 0;
                output = ItemStack.EMPTY;
            }
            PacketSender.sendPrinting(this, have > 0.5 && !output.isEmpty());
        }

        final int inputValue = PrintData.materialValue(getItem(slotMaterial));
        if (inputValue > 0 && maxAmountMaterial - amountMaterial >= inputValue) {
            final ItemStack material = removeItem(slotMaterial, 1);
            if (material != null) {
                amountMaterial += inputValue;
            }
        }

        final int inkValue = PrintData.inkValue(getItem(slotInk));
        if (inkValue > 0 && maxAmountInk - amountInk >= inkValue) {
            final ItemStack material = removeItem(slotInk, 1);
            if (material != null) {
                amountInk += inkValue;
                final ItemStack remainder = PlatformHooks.getCraftingRemainder(material);
                if (!remainder.isEmpty()) {
                    setItem(slotInk, remainder);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        amountMaterial = nbt.getInt(AmountMaterialTag);
        amountInk = nbt.getInt(AmountInkTag);
        data.loadData(nbt.getCompound(DataTag));
        isActive = nbt.getBoolean(IsActiveTag);
        limit = nbt.getInt(LimitTag);
        if (nbt.contains(OutputTag)) {
            output = ItemStack.of(nbt.getCompound(OutputTag));
        } else {
            output = ItemStack.EMPTY;
        }
        totalRequiredEnergy = nbt.getDouble(TotalTag);
        requiredEnergy = nbt.getDouble(RemainingTag);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        nbt.putInt(AmountMaterialTag, amountMaterial);
        nbt.putInt(AmountInkTag, amountInk);
        ExtendedNBT.setNewCompoundTag(nbt, DataTag, data::saveData);
        nbt.putBoolean(IsActiveTag, isActive);
        nbt.putInt(LimitTag, limit);
        if (!output.isEmpty()) ExtendedNBT.setNewCompoundTag(nbt, OutputTag, output::save);
        nbt.putDouble(TotalTag, totalRequiredEnergy);
        nbt.putDouble(RemainingTag, requiredEnergy);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        data.loadData(nbt.getCompound(DataTag));
        requiredEnergy = nbt.getDouble(RemainingTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, DataTag, data::saveData);
        nbt.putDouble(RemainingTag, requiredEnergy);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == slotMaterial)
            return PrintData.materialValue(stack) > 0;
        else if (slot == slotInk)
            return PrintData.inkValue(stack) > 0;
        else return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Printer(ContainerTypes.PRINTER.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{slotMaterial, slotInk, slotOutput};
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !canPlaceItem(slot, stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot != slotOutput;
    }
}
