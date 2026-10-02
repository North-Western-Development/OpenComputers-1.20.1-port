package li.cil.oc.server.component;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.transfer.FluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Map;

/**
 * Formerly a Forge {@code IFluidTank} wrapping a Forge {@code FluidTank}; now a single-tank
 * {@link FluidHandler} (amounts in millibuckets) with the old IFluidTank-style accessors.
 * Persists in the same NBT format as Forge's {@code FluidTank.writeToNBT}.
 */
public class UpgradeTank extends AbstractManagedEnvironment implements FluidHandler, DeviceInfo {
    public final EnvironmentHost owner;

    public final int capacity;

    private FluidStack fluid = FluidStack.empty();

    public UpgradeTank(EnvironmentHost owner, int capacity) {
        this.owner = owner;
        this.capacity = capacity;
        setNode(Network.newNode(this, Visibility.None).create());
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Tank upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Superblubb V10",
                DeviceAttribute.Capacity, String.valueOf(capacity)
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    private static final String FluidNameTag = "FluidName";
    private static final String AmountTag = "Amount";
    private static final String TagTag = "Tag";
    private static final String EmptyTag = "Empty";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        fluid = FluidStack.empty();
        if (nbt.contains(FluidNameTag)) {
            final ResourceLocation id = ResourceLocation.tryParse(nbt.getString(FluidNameTag));
            final Fluid f = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
            final long amount = nbt.getInt(AmountTag);
            if (f != Fluids.EMPTY && amount > 0) {
                fluid = FluidStack.create(f, amount, nbt.contains(TagTag) ? nbt.getCompound(TagTag) : null);
            }
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        if (fluid.isEmpty()) {
            nbt.putString(EmptyTag, "");
        }
        else {
            nbt.putString(FluidNameTag, BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            nbt.putInt(AmountTag, (int) fluid.getAmount());
            if (fluid.hasTag()) {
                nbt.put(TagTag, fluid.getTag().copy());
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // IFluidTank-style accessors.

    public FluidStack getFluid() {
        return fluid;
    }

    public long getFluidAmount() {
        return fluid.getAmount();
    }

    public long getCapacity() {
        return capacity;
    }

    public boolean isFluidValid(FluidStack stack) {
        return true;
    }

    // ----------------------------------------------------------------------- //
    // FluidHandler

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluid;
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public long fill(FluidStack resource, boolean simulate) {
        final long amount = fillInternal(resource, simulate);
        if (!simulate && amount > 0) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), (int) amount);
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        final FluidStack amount = resource == null || resource.isEmpty() || !resource.isFluidStackEqual(fluid)
            ? FluidStack.empty()
            : drainInternal(resource.getAmount(), simulate);
        if (!simulate && amount != null && amount.getAmount() > 0) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), (int) -amount.getAmount());
        }
        return amount;
    }

    @Override
    public FluidStack drain(long maxDrain, boolean simulate) {
        final FluidStack amount = drainInternal(maxDrain, simulate);
        if (!simulate && amount != null && amount.getAmount() > 0) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), (int) -amount.getAmount());
        }
        return amount;
    }

    // Same semantics as Forge's FluidTank.
    private long fillInternal(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        if (simulate) {
            if (fluid.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!fluid.isFluidStackEqual(resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            fluid = resource.copyWithAmount(Math.min(capacity, resource.getAmount()));
            return fluid.getAmount();
        }
        if (!fluid.isFluidStackEqual(resource)) {
            return 0;
        }
        long filled = capacity - fluid.getAmount();
        if (resource.getAmount() < filled) {
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        }
        else {
            fluid.setAmount(capacity);
        }
        return filled;
    }

    private FluidStack drainInternal(long maxDrain, boolean simulate) {
        long drained = maxDrain;
        if (fluid.getAmount() < drained) {
            drained = fluid.getAmount();
        }
        if (drained <= 0 || fluid.isEmpty()) {
            return FluidStack.empty();
        }
        final FluidStack stack = fluid.copyWithAmount(drained);
        if (!simulate) {
            fluid.shrink(drained);
            if (fluid.getAmount() <= 0) {
                fluid = FluidStack.empty();
            }
        }
        return stack;
    }

    private int tankIndex() {
        if (owner instanceof Agent agent && agent.tank() != null) {
            int index = -1;
            for (int i = 0; i < agent.tank().tankCount(); i++) {
                if (agent.tank().getFluidTank(i) == this) {
                    index = i;
                    break;
                }
            }
            return Math.max(index, 0) + 1;
        }
        return 1;
    }
}
