package li.cil.oc.integration.appeng;

import appeng.api.ids.AEBlockIds;
import appeng.api.ids.AEPartIds;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.automation.ImportBusPart;
import appeng.parts.misc.InterfacePart;
import dev.architectury.fluid.FluidStack;
import li.cil.oc.server.driver.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Helpers for the AE2 15.x (1.20.1) API.
 * <p>
 * In contrast to the 1.16 version there is no {@code IAppEngApi} instance anymore: AE2's API is
 * static now (grid services via {@link IGrid#getService}, stacks as {@link AEKey}s), so the
 * {@code @AEAddon} entry point is gone. Block entities and parts implement
 * {@link appeng.api.networking.IInWorldGridNodeHost} / {@link IActionHost} directly on both
 * loaders, so no loader-specific lookup is needed.
 */
public final class AEUtil {
    private AEUtil() {
    }

    // ----------------------------------------------------------------------- //

    public static Class<?> controllerClass() {
        return ControllerBlockEntity.class;
    }

    public static Class<?> interfaceClass() {
        return InterfaceBlockEntity.class;
    }

    // ----------------------------------------------------------------------- //

    private static boolean isItem(ItemStack stack, ResourceLocation id) {
        return stack != null && !stack.isEmpty() && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isController(ItemStack stack) {
        return isItem(stack, AEBlockIds.CONTROLLER);
    }

    public static boolean isExportBus(ItemStack stack) {
        return isItem(stack, AEPartIds.EXPORT_BUS);
    }

    public static boolean isImportBus(ItemStack stack) {
        return isItem(stack, AEPartIds.IMPORT_BUS);
    }

    public static boolean isBlockInterface(ItemStack stack) {
        return isItem(stack, AEBlockIds.INTERFACE);
    }

    public static boolean isPartInterface(ItemStack stack) {
        return isItem(stack, AEPartIds.INTERFACE);
    }

    public static boolean isExportBus(@Nullable IPart part) {
        return part instanceof ExportBusPart;
    }

    public static boolean isImportBus(@Nullable IPart part) {
        return part instanceof ImportBusPart;
    }

    public static boolean isPartInterface(@Nullable IPart part) {
        return part instanceof InterfacePart;
    }

    /** Whether any side of the part host at the position holds a part matching the filter. */
    public static boolean hasPart(Level world, BlockPos pos, Predicate<IPart> filter) {
        if (world.getBlockEntity(pos) instanceof IPartHost host) {
            for (Direction side : Direction.values()) {
                if (filter.test(host.getPart(side))) return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    /**
     * Resolves the grid node of an AE2 machine: the node of the part on {@code partSide} if that is
     * given and the block is a part host, otherwise the block entity's actionable node.
     */
    @Nullable
    public static IGridNode nodeAt(@Nullable Level world, BlockPos pos, @Nullable Direction partSide) {
        if (world == null || !world.isLoaded(pos)) return null;
        final BlockEntity be = world.getBlockEntity(pos);
        if (be == null || be.isRemoved()) return null;
        if (partSide != null && be instanceof IPartHost host) {
            final IPart part = host.getPart(partSide);
            return part != null ? part.getGridNode() : null;
        }
        if (be instanceof IActionHost host) {
            return host.getActionableNode();
        }
        return null;
    }

    @Nullable
    public static IGrid gridOf(@Nullable IGridNode node) {
        return node != null ? node.getGrid() : null;
    }

    public static IStorageService getGridStorage(IGrid grid) {
        return grid.getStorageService();
    }

    public static ICraftingService getGridCrafting(IGrid grid) {
        return grid.getCraftingService();
    }

    public static IEnergyService getGridEnergy(IGrid grid) {
        return grid.getEnergyService();
    }

    // ----------------------------------------------------------------------- //

    /**
     * Number of AE2 fluid units per millibucket. Read at runtime: AE2's
     * {@code AEFluidKey.AMOUNT_BUCKET} is a compile-time constant that differs per loader
     * (1000 on Forge, 81000 droplets on Fabric), so it must not be inlined into common code.
     */
    public static long fluidUnitsPerBucket(AEKey key) {
        return key.getAmountPerUnit();
    }

    /** Converts an AE2 fluid amount to millibuckets. */
    public static long fluidToMillibuckets(AEFluidKey key, long amount) {
        final long perBucket = fluidUnitsPerBucket(key);
        return perBucket == 1000 ? amount : amount * 1000 / perBucket;
    }

    /** Converts millibuckets to an AE2 fluid amount. */
    public static long millibucketsToFluid(AEFluidKey key, long millibuckets) {
        final long perBucket = fluidUnitsPerBucket(key);
        return perBucket == 1000 ? millibuckets : millibuckets * perBucket / 1000;
    }

    /** OC-side representation of a key: an {@link ItemStack} for items, a {@link FluidStack} (mB) for fluids. */
    @Nullable
    public static Object toStack(@Nullable AEKey key, long amount) {
        if (key instanceof AEItemKey item) {
            return item.toStack((int) Math.max(1, Math.min(amount, Integer.MAX_VALUE)));
        } else if (key instanceof AEFluidKey fluid) {
            return FluidStack.create(fluid.getFluid(), fluidToMillibuckets(fluid, amount), fluid.getTag());
        }
        return null;
    }

    /**
     * Converts a key to a Lua-friendly table using the registered converters (item stack converter
     * for items, fluid stack converter for fluids); other key types get name and label only.
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> convert(AEKey key, long amount) {
        final Map<Object, Object> hash = new HashMap<>();
        final Object stack = toStack(key, amount);
        if (stack != null) {
            final Object converted = Registry.INSTANCE.convert(new Object[]{stack})[0];
            if (converted instanceof Map<?, ?> map) {
                hash.putAll((Map<Object, Object>) map);
            }
        } else {
            hash.put("name", key.getId().toString());
            hash.put("label", key.getDisplayName().getString());
        }
        return hash;
    }
}
