package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.server.component.Redstone;
import li.cil.oc.server.machine.Machine;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Provide static environment lookup for blocks that are components.
 * This allows showing their documentation in NEI, for example. Not
 * all blocks are present here, because some also serve as upgrades
 * and therefore have item drivers.
 */
public final class EnvironmentProviderBlocks implements EnvironmentProvider {
    public static final EnvironmentProviderBlocks INSTANCE = new EnvironmentProviderBlocks();

    private EnvironmentProviderBlocks() {
    }

    @Override
    public Class<?> getEnvironment(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() != null) {
            final Block block = blockItem.getBlock();
            if (isOneOf(block, Constants.BlockName.Assembler)) return li.cil.oc.common.tileentity.Assembler.class;
            else if (isOneOf(block, Constants.BlockName.CaseTier1, Constants.BlockName.CaseTier2, Constants.BlockName.CaseTier3, Constants.BlockName.CaseCreative, Constants.BlockName.Microcontroller)) return Machine.class;
            else if (isOneOf(block, Constants.BlockName.HologramTier1, Constants.BlockName.HologramTier2)) return li.cil.oc.common.tileentity.Hologram.class;
            else if (isOneOf(block, Constants.BlockName.Printer)) return li.cil.oc.common.tileentity.Printer.class;
            else if (isOneOf(block, Constants.BlockName.Relay)) return li.cil.oc.common.tileentity.Relay.class;
            else if (isOneOf(block, Constants.BlockName.Redstone)) return BundledRedstone.isAvailable() ? Redstone.Bundled.class : Redstone.Vanilla.class;
            else if (isOneOf(block, Constants.BlockName.ScreenTier1)) return li.cil.oc.common.component.TextBuffer.class;
            else if (isOneOf(block, Constants.BlockName.ScreenTier2, Constants.BlockName.ScreenTier3)) return li.cil.oc.common.component.Screen.class;
            else if (isOneOf(block, Constants.BlockName.Robot)) return li.cil.oc.server.component.Robot.class;
            else if (isOneOf(block, Constants.BlockName.Waypoint)) return li.cil.oc.common.tileentity.Waypoint.class;
            else return null;
        } else {
            if (Items.get(stack) == Items.get(Constants.ItemName.Drone)) return li.cil.oc.server.component.Drone.class;
            else return null;
        }
    }

    private static boolean isOneOf(Block block, String... names) {
        for (String name : names) {
            final ItemInfo info = Items.get(name);
            if (info != null && info.block() == block) return true;
        }
        return false;
    }
}
