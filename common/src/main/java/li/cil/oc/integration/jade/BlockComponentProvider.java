package li.cil.oc.integration.jade;

import li.cil.oc.Localization;
import li.cil.oc.common.tileentity.Assembler;
import li.cil.oc.common.tileentity.Charger;
import li.cil.oc.common.tileentity.Rack;
import li.cil.oc.common.tileentity.Relay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client side of the former WAILA {@code BlockDataProvider}: renders the data collected by
 * {@link BlockDataProvider} into the Jade tooltip body. Only registered from
 * {@link JadePlugin#registerClient}.
 */
public final class BlockComponentProvider implements IBlockComponentProvider {
    public static final BlockComponentProvider INSTANCE = new BlockComponentProvider();

    private BlockComponentProvider() {
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.Uid;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        final CompoundTag tag = accessor.getServerData();
        if (tag == null || tag.isEmpty()) return;
        final BlockEntity tileEntity = accessor.getBlockEntity();
        final int side = accessor.getSide().ordinal();

        if (tileEntity instanceof Relay) {
            final String address = tag.getList("addresses", Tag.TAG_STRING).getString(side);
            final double signalStrength = tag.getDouble("signalStrength");
            if (config.get(JadePlugin.ConfigAddress) && !address.isEmpty()) {
                tooltip.add(Localization.Analyzer.Address(address));
            }
            tooltip.add(Localization.Analyzer.WirelessStrength(signalStrength));
        } else if (tileEntity instanceof Assembler) {
            if (tag.contains("progress")) {
                final double progress = tag.getDouble("progress");
                final String timeRemaining = formatTime(tag.getInt("timeRemaining"));
                tooltip.add(net.minecraft.network.chat.Component.literal(Localization.Assembler.Progress(progress, timeRemaining)));
                if (tag.contains("output")) {
                    final String output = tag.getString("output");
                    tooltip.add(net.minecraft.network.chat.Component.literal("Building: ").append(net.minecraft.network.chat.Component.translatable(output)));
                }
            }
        } else if (tileEntity instanceof Charger) {
            tooltip.add(Localization.Analyzer.ChargerSpeed(tag.getDouble("chargeSpeed")));
        } else if (tileEntity instanceof Rack) {
            // TODO(port): per-server addresses for racks were already disabled in the 1.16.5 version.
        }

        if (tileEntity instanceof li.cil.oc.api.network.SidedEnvironment) {
            final ListTag nodes = tag.getList("nodes", Tag.TAG_COMPOUND);
            if (side < nodes.size()) readNode(tooltip, config, nodes.getCompound(side));
        } else if (tileEntity instanceof li.cil.oc.api.network.Environment) {
            readNode(tooltip, config, tag);
        }
    }

    private static void readNode(ITooltip tooltip, IPluginConfig config, CompoundTag tag) {
        if (config.get(JadePlugin.ConfigAddress) && tag.contains("address")) {
            final String address = tag.getString("address");
            if (!address.isEmpty()) {
                tooltip.add(Localization.Analyzer.Address(address));
            }
        }
        if (config.get(JadePlugin.ConfigEnergy) && tag.contains("buffer") && tag.contains("bufferSize")) {
            final int buffer = tag.getInt("buffer");
            final int bufferSize = tag.getInt("bufferSize");
            if (bufferSize > 0) {
                tooltip.add(Localization.Analyzer.StoredEnergy(buffer + "/" + bufferSize));
            }
        }
        if (config.get(JadePlugin.ConfigComponentName) && tag.contains("componentName")) {
            final String componentName = tag.getString("componentName");
            if (!componentName.isEmpty()) {
                tooltip.add(Localization.Analyzer.ComponentName(componentName));
            }
        }
    }

    private static String formatTime(int seconds) {
        // Assembly times should not / rarely exceed one hour, so this is good enough.
        if (seconds < 60) return String.format("0:%02d", seconds);
        else return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
