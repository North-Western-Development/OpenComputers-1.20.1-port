package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.NodeData;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UpgradeBattery extends SimpleItem implements ItemTier, Chargeable {
    public final int tier;

    public UpgradeBattery(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }

    @Override
    protected List<Object> tooltipData() {
        return Collections.singletonList((int) Settings.get().bufferCapacitorUpgrades[tier]);
    }

    // Formerly Forge showDurabilityBar / getDurabilityForDisplay.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        final NodeData data = new NodeData(stack);
        return 1 - data.buffer.orElse(0.0) / Settings.get().bufferCapacitorUpgrades[tier];
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return barWidth(getDurabilityForDisplay(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor(getDurabilityForDisplay(stack));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        final NodeData data = new NodeData(stack);
        final double buffer = data.buffer.orElse(0.0);
        return Chargeable.applyCharge(amount, buffer, Settings.get().bufferCapacitorUpgrades[tier], used -> {
            if (!simulate) {
                data.buffer = Optional.of(buffer + used);
                data.saveData(stack);
            }
        });
    }

    @Override
    public double maxCharge(ItemStack stack) {
        return Settings.get().bufferCapacitorUpgrades[tier];
    }

    @Override
    public double getCharge(ItemStack stack) {
        return new NodeData(stack).buffer.orElse(0.0);
    }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        final NodeData data = new NodeData(stack);
        data.buffer = Optional.of(Math.min(Math.max(0.0, amount), maxCharge(stack)));
        data.saveData(stack);
    }

    @Override
    public boolean canExtract(ItemStack stack) {
        return true;
    }
}
