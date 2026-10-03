package li.cil.oc.integration.appeng;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import appeng.me.cells.BasicCellInventory;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import li.cil.oc.api.driver.Converter;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts AE2 storage cells (as item stacks or cell inventories) to tables describing their
 * usage. The 1.16 version only handled item cells; AE2 15 cells are generic (item and fluid
 * cells share {@link BasicCellInventory}), so {@code getAvailableItems} lists all stored keys.
 */
public final class ConverterCellInventory implements Converter {
    public static final ConverterCellInventory INSTANCE = new ConverterCellInventory();

    private ConverterCellInventory() {
    }

    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof BasicCellInventory cell) {
            output.put("storedItemTypes", cell.getStoredItemTypes());
            output.put("storedItemCount", cell.getStoredItemCount());
            output.put("remainingItemCount", cell.getRemainingItemCount());
            output.put("remainingItemTypes", cell.getRemainingItemTypes());

            output.put("getTotalItemTypes", cell.getTotalItemTypes());
            final KeyCounter available = new KeyCounter();
            cell.getAvailableStacks(available);
            final List<Object> items = new ArrayList<>();
            for (Object2LongMap.Entry<AEKey> entry : available) {
                final Map<Object, Object> stack = AEUtil.convert(entry.getKey(), entry.getLongValue());
                stack.put("size", entry.getLongValue());
                items.add(stack);
            }
            output.put("getAvailableItems", items.toArray());

            output.put("totalBytes", cell.getTotalBytes());
            output.put("freeBytes", cell.getFreeBytes());
            output.put("usedBytes", cell.getUsedBytes());
            output.put("unusedItemCount", cell.getUnusedItemCount());
            output.put("canHoldNewItem", cell.canHoldNewItem());

            output.put("fuzzyMode", String.valueOf(cell.getFuzzyMode()));
            output.put("name", cell.getDescription().getString());
        } else if (value instanceof ItemStack stack && stack.getItem() instanceof IBasicCellItem) {
            final StorageCell inventory = StorageCells.getCellInventory(stack, null);
            if (inventory instanceof BasicCellInventory) {
                convert(inventory, output);
            }
        }
    }
}
