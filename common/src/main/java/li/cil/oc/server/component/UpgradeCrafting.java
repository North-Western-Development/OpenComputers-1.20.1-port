package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Map;
import java.util.Optional;

public class UpgradeCrafting extends AbstractManagedEnvironment implements DeviceInfo {
    public final Robot host;

    private final CraftingInventory craftingInventory = new CraftingInventory();

    public UpgradeCrafting(Robot host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("crafting").
            create());
    }

    @Override
    public Component node() {
        return (Component) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Assembly controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "MultiCombinator-9S"
            );
        }
        return deviceInfo;
    }

    @Callback(doc = "function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.")
    public Object[] craft(Context context, Arguments args) {
        final int count = Math.min(Math.max(args.optInteger(0, 64), 0), 64);
        return craftingInventory.craft(count);
    }

    private static final class DummyMenu extends AbstractContainerMenu {
        DummyMenu() {
            super(null, 0);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private final class CraftingInventory extends TransientCraftingContainer {
        CraftingInventory() {
            super(new DummyMenu(), 3, 3);
        }

        Object[] craft(int wantedCount) {
            final Player player = host.player();
            copyItemsFromHost(player.getInventory());
            final int[] countCrafted = {0};
            final RecipeManager manager = host.world().getRecipeManager();
            final Optional<CraftingRecipe> initialCraft = manager.getRecipeFor(RecipeType.CRAFTING, this, host.world());
            if (initialCraft.isPresent()) {
                while (countCrafted[0] < wantedCount && tryCraft(player, manager, initialCraft, countCrafted)) {
                    //
                }
            }
            return ResultWrapper.result(countCrafted[0] > 0, countCrafted[0]);
        }

        private boolean tryCraft(Player player, RecipeManager manager, Optional<CraftingRecipe> initialCraft, int[] countCrafted) {
            final Optional<CraftingRecipe> craft = manager.getRecipeFor(RecipeType.CRAFTING, this, host.world());
            if (!craft.equals(initialCraft)) {
                return false;
            }

            final ResultContainer craftResult = new ResultContainer();
            final ResultSlot craftingSlot = new ResultSlot(player, this, craftResult, 0, 0, 0);
            final ItemStack craftedResult = craft.get().assemble(this, host.world().registryAccess());
            craftResult.setItem(0, craftedResult);
            if (!craftingSlot.hasItem())
                return false;

            final ItemStack stack = craftingSlot.remove(1);
            countCrafted[0] += Math.max(stack.getCount(), 1);
            // Note: vanilla's ResultSlot.onTake consumes the ingredients and puts the
            // remaining items (RecipeManager.getRemainingItemsFor, which honours the
            // platform's crafting remainders) back into the grid.
            craftingSlot.onTake(player, stack);
            copyItemsToHost(player.getInventory());
            if (stack.getCount() > 0) {
                InventoryUtils.addToPlayerInventory(stack, player);
            }
            copyItemsFromHost(player.getInventory());
            return true;
        }

        void copyItemsFromHost(Container inventory) {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                final ItemStack stack = inventory.getItem(toParentSlot(slot));
                setItem(slot, stack);
            }
        }

        void copyItemsToHost(Container inventory) {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                inventory.setItem(toParentSlot(slot), getItem(slot));
            }
        }

        private int toParentSlot(int slot) {
            final int col = slot % 3;
            final int row = slot / 3;
            return row * 4 + col;
        }
    }
}
