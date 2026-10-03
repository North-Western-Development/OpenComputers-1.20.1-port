package li.cil.oc.integration.appeng;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.ids.AEItemIds;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.parts.automation.ExportBusPart;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverExportBus implements DriverBlock {
    public static final DriverExportBus INSTANCE = new DriverExportBus();

    private DriverExportBus() {
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return AEUtil.hasPart(world, pos, AEUtil::isExportBus);
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPartHost> implements NamedBlock, PartEnvironmentBase {
        public Environment(IPartHost host) {
            super(host, "me_exportbus");
        }

        @Override
        public IPartHost host() {
            return tileEntity;
        }

        @Override
        public String preferredName() {
            return "me_exportbus";
        }

        @Override
        public int priority() {
            return 2;
        }

        @Callback(doc = "function(side:number, [ slot:number]):boolean -- Get the configuration of the export bus pointing in the specified direction.")
        public Object[] getExportConfiguration(Context context, Arguments args) {
            return getPartConfig(context, args, AEUtil::isExportBus);
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number):boolean -- Configure the export bus pointing in the specified direction to export item stacks matching the specified descriptor.")
        public Object[] setExportConfiguration(Context context, Arguments args) {
            return setPartConfig(context, args, AEUtil::isExportBus);
        }

        /**
         * Moves up to {@code limit} items of the given type from the network into the inventory.
         * Returns the number of items moved.
         */
        private static int doExport(IGrid grid, MEStorage storage, AEItemKey key, ItemHandler inventory, Optional<Integer> targetSlot, int limit, MachineSource source) {
            final long available = StorageHelper.poweredExtraction(grid.getEnergyService(), storage, key, limit, source, Actionable.SIMULATE);
            if (available <= 0) return 0;
            final int amount = (int) Math.min(available, limit);

            // How many fit?
            final ItemStack simulated = key.toStack(amount);
            if (targetSlot.isPresent()) {
                InventoryUtils.insertIntoInventorySlot(simulated, inventory, targetSlot.get(), amount, true);
            } else {
                InventoryUtils.insertIntoInventory(simulated, inventory, amount, true);
            }
            final int fits = amount - simulated.getCount();
            if (fits <= 0) return 0;

            final long extracted = StorageHelper.poweredExtraction(grid.getEnergyService(), storage, key, fits, source, Actionable.MODULATE);
            if (extracted <= 0) return 0;
            final ItemStack stack = key.toStack((int) extracted);
            if (targetSlot.isPresent()) {
                InventoryUtils.insertIntoInventorySlot(stack, inventory, targetSlot.get(), (int) extracted, false);
            } else {
                InventoryUtils.insertIntoInventory(stack, inventory, (int) extracted, false);
            }
            if (stack.getCount() > 0) {
                // Could not insert everything after all: put the rest back.
                storage.insert(key, stack.getCount(), Actionable.MODULATE, source);
            }
            return (int) extracted - stack.getCount();
        }

        @Callback(doc = "function(side:number, [slot:number]):boolean -- Make the export bus facing the specified direction perform a single export operation into the specified slot.")
        public Object[] exportIntoSlot(Context context, Arguments args) {
            final Direction side = ExtendedArguments.checkSideAny(args, 0);
            final IPart part = tileEntity.getPart(side);
            if (!(part instanceof ExportBusPart exportBus)) {
                return result(null, "no export bus");
            }
            final IGridNode node = exportBus.getGridNode();
            final IGrid grid = node != null ? node.getGrid() : null;
            if (grid == null) {
                return result(null, "no ae grid");
            }

            final Level world = tileEntity.getBlockEntity().getLevel();
            final BlockPos target = tileEntity.getBlockEntity().getBlockPos().relative(side);
            final ItemHandler inventory = world != null ? PlatformHooks.getItemHandler(world, target, side.getOpposite()) : null;
            if (inventory == null) {
                return result(null, "no inventory");
            }
            final int slot = ExtendedArguments.optSlot(args, inventory, 1, -1);
            final Optional<Integer> targetSlot = slot < 0 ? Optional.empty() : Optional.of(slot);

            final var config = exportBus.getConfig();
            final MEStorage storage = grid.getStorageService().getInventory();
            final int speedCards = exportBus.getInstalledUpgrades(BuiltInRegistries.ITEM.get(AEItemIds.SPEED_CARD));
            int count = switch (speedCards) {
                case 1 -> 8;
                case 2 -> 32;
                case 3 -> 64;
                case 4 -> 96;
                default -> 1;
            };
            final boolean fuzzy = exportBus.getInstalledUpgrades(BuiltInRegistries.ITEM.get(AEItemIds.FUZZY_CARD)) > 0;
            final FuzzyMode fuzzyMode = exportBus.getConfigManager().getSetting(Settings.FUZZY_MODE);
            final MachineSource source = new MachineSource(exportBus);
            final int potentialWork = count;

            for (int i = 0; i < config.size() && count > 0; i++) {
                final AEKey filter = config.getKey(i);
                if (!(filter instanceof AEItemKey)) continue; // TODO(port): fluids can't go into item inventories.
                final List<AEItemKey> keys = new ArrayList<>();
                if (fuzzy) {
                    final KeyCounter cached = grid.getStorageService().getCachedInventory();
                    for (Object2LongMap.Entry<AEKey> entry : cached.findFuzzy(filter, fuzzyMode)) {
                        if (entry.getKey() instanceof AEItemKey itemKey) keys.add(itemKey);
                    }
                } else {
                    keys.add((AEItemKey) filter);
                }
                for (AEItemKey key : keys) {
                    if (count <= 0) break;
                    final int moved = doExport(grid, storage, key, inventory, targetSlot, count, source);
                    if (moved > 0) {
                        count -= moved;
                        context.pause(0.25);
                    }
                }
            }
            if (potentialWork == count) {
                return result(null, "no items moved");
            }
            return result(potentialWork - count);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return AEUtil.isExportBus(stack) ? Environment.class : null;
        }
    }
}
