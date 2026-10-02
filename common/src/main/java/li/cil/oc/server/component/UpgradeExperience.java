package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Map;

public class UpgradeExperience extends AbstractManagedEnvironment implements DeviceInfo {
    public final int MaxLevel = 30;

    public final Agent host;

    public double experience = 0.0;

    public int level = 0;

    public UpgradeExperience(Agent host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("experience").
            withConnector(30 * Settings.get().bufferPerLevel).
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Knowledge database",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "ERSO (Event Recorder and Self-Optimizer)",
                DeviceAttribute.Capacity, "30"
            );
        }
        return deviceInfo;
    }

    public double xpForNextLevel() {
        return li.cil.oc.util.UpgradeExperience.xpForLevel(level + 1);
    }

    public void addExperience(double value) {
        if (level < MaxLevel) {
            experience = experience + value;
            if (experience >= xpForNextLevel()) {
                updateXpInfo();
            }
            final Level world = this.host.world();
            final BlockPos pos = this.host.player().blockPosition();
            final ExperienceOrb orb = new ExperienceOrb(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, (int) value);
            this.host.player().takeXpDelay = 0;
            orb.playerTouch(this.host.player());
        }
    }

    public void updateXpInfo() {
        // xp(level) = base + (level * const) ^ exp
        // pow(xp(level) - base, 1/exp) / const = level
        final int oldLevel = level;
        level = li.cil.oc.util.UpgradeExperience.calculateLevelFromExperience(experience);
        if (node() != null) {
            if (level != oldLevel) {
                updateClient();
            }
            node().setLocalBufferSize(Settings.get().bufferPerLevel * level);
        }
    }

    @Callback(direct = true, doc = "function():number -- The current level of experience stored in this experience upgrade.")
    public Object[] level(Context context, Arguments args) {
        return ResultWrapper.result(li.cil.oc.util.UpgradeExperience.calculateExperienceLevel(level, experience));
    }

    @Callback(doc = "function():boolean -- Tries to consume an enchanted item to add experience to the upgrade.")
    public Object[] consume(Context context, Arguments args) {
        if (level >= MaxLevel) {
            return ResultWrapper.result(ResultWrapper.unit, "max level");
        }
        final ItemStack stack = host.mainInventory().getItem(host.selectedSlot());
        if (stack.isEmpty()) {
            return ResultWrapper.result(ResultWrapper.unit, "no item");
        }
        int xp = 0;
        if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
            xp += 3 + host.world().random.nextInt(5) + host.world().random.nextInt(5);
        }
        else {
            for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
                if (entry.getKey() != null) {
                    xp += entry.getKey().getMinCost(entry.getValue());
                }
            }
            if (xp <= 0) {
                return ResultWrapper.result(ResultWrapper.unit, "could not extract experience from item");
            }
        }
        final ItemStack consumed = host.mainInventory().removeItem(host.selectedSlot(), 1);
        if (consumed.isEmpty()) {
            return ResultWrapper.result(ResultWrapper.unit, "could not consume item");
        }
        addExperience(xp * Settings.get().constantXpGrowth);
        return ResultWrapper.result(true);
    }

    private void updateClient() {
        if (host instanceof Robot robot) {
            robot.synchronizeSlot(robot.componentSlot(node().address()));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        li.cil.oc.util.UpgradeExperience.setExperience(nbt, experience);
    }

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        experience = li.cil.oc.util.UpgradeExperience.getExperience(nbt);
        updateXpInfo();
    }
}
