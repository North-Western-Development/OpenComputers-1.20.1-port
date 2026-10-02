package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Driver for CPUs; also the base class of {@link DriverAPU} (Scala had both
 * {@code object DriverCPU} and {@code abstract class DriverCPU}).
 */
public class DriverCPU implements Item, MutableProcessor, CallBudget {
    public static final DriverCPU INSTANCE = new DriverCPU();

    private static final String NativeLuaArchitectureClassName = "li.cil.oc.server.machine.luac.NativeLuaArchitecture";

    protected DriverCPU() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.CPUTier1),
                Items.get(Constants.ItemName.CPUTier2),
                Items.get(Constants.ItemName.CPUTier3));
    }

    // Declared here so subclasses implementing HostAware inherit a class method.
    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new li.cil.oc.server.component.CPU(tier(stack));
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.CPU;
    }

    @Override
    public int tier(ItemStack stack) {
        return cpuTier(stack);
    }

    public int cpuTier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.CPU cpu) return cpu.cpuTier();
        return Tier.One;
    }

    @Override
    public int supportedComponents(ItemStack stack) {
        return Settings.get().cpuComponentSupport[cpuTier(stack)];
    }

    @Override
    public Collection<Class<? extends Architecture>> allArchitectures() {
        return new ArrayList<>(Machine.architectures());
    }

    @Override
    public Class<? extends Architecture> architecture(ItemStack stack) {
        if (stack.hasTag()) {
            String archClass = stack.getTag().getString(Settings.namespace + "archClass");
            if (NativeLuaArchitectureClassName.equals(archClass)) {
                // Migrate old saved CPUs to new versions (since the class they refer still
                // exists, but is abstract, which would lead to issues).
                archClass = Machine.LuaArchitecture.getName();
            }
            if (!archClass.isEmpty()) {
                try {
                    return Class.forName(archClass).asSubclass(Architecture.class);
                } catch (Throwable t) {
                    OpenComputers.log.warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t);
                    stack.getTag().remove(Settings.namespace + "archClass");
                    stack.getTag().remove(Settings.namespace + "archName");
                }
            }
        }
        final Iterator<Class<? extends Architecture>> it = Machine.architectures().iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Override
    public void setArchitecture(ItemStack stack, Class<? extends Architecture> architecture) {
        if (!worksWith(stack)) throw new IllegalArgumentException("Unsupported processor type.");
        final CompoundTag data = stack.getOrCreateTag();
        data.putString(Settings.namespace + "archClass", architecture.getName());
        data.putString(Settings.namespace + "archName", Machine.getArchitectureName(architecture));
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        return Settings.get().callBudgets[Math.min(Math.max(tier(stack), Tier.One), Tier.Three)];
    }
}
