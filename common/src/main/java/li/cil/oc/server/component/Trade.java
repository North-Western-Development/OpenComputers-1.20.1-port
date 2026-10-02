package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.common.EventHandler;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

public class Trade extends AbstractValue {
    public final TradeInfo info;

    public Trade(TradeInfo info) {
        this.info = info;
    }

    public Trade() {
        this(new TradeInfo());
    }

    public Trade(UpgradeTrading upgrade, Merchant merchant, int recipeID, int merchantID) {
        this(new TradeInfo(upgrade.host, merchant, recipeID, merchantID));
    }

    public double maxRange() {
        return Settings.get().tradingRange;
    }

    public boolean isInRange() {
        final Merchant merchant = info.merchant.get();
        if (merchant instanceof Entity entity && info.host.isPresent()) {
            final EnvironmentHost host = info.host.get();
            return entity.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) < maxRange() * maxRange();
        }
        return false;
    }

    // Queue the load because when load is called we can't access the world yet
    // and we need to access it to get the Robot's TileEntity / Drone's Entity.
    @Override
    public void loadData(CompoundTag nbt) {
        EventHandler.scheduleServer(() -> info.loadData(nbt));
    }

    @Override
    public void saveData(CompoundTag nbt) {
        info.saveData(nbt);
    }

    @Callback(doc = "function():number -- Returns a sort index of the merchant that provides this trade")
    public Object[] getMerchantId(Context context, Arguments arguments) {
        return ResultWrapper.result(info.merchantID);
    }

    @Callback(doc = "function():table, table -- Returns the items the merchant wants for this trade.")
    public Object[] getInput(Context context, Arguments arguments) {
        final Optional<MerchantOffer> recipe = info.recipe();
        return ResultWrapper.result(recipe.map(r -> r.getCostA().copy()).orElse(null),
            recipe.isPresent() && !recipe.get().getCostB().isEmpty() ? recipe.get().getCostB().copy() : null);
    }

    @Callback(doc = "function():table -- Returns the item the merchant offers for this trade.")
    public Object[] getOutput(Context context, Arguments arguments) {
        return ResultWrapper.result(info.recipe().map(r -> r.getResult().copy()).orElse(null));
    }

    @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
    public Object[] isEnabled(Context context, Arguments arguments) {
        // Make sure merchant is neither dead/gone nor the recipe has been disabled.
        return ResultWrapper.result(info.merchant.get() != null && !info.recipe().map(MerchantOffer::isOutOfStock).orElse(false));
    }

    @Callback(doc = "function():boolean, string -- Returns true when trade succeeds and nil, error when not.")
    public Object[] trade(Context context, Arguments arguments) {
        // Make sure we can access an inventory.
        final Optional<Container> inventory = info.inventory();
        if (inventory.isEmpty()) {
            return ResultWrapper.result(false, "trading requires an inventory upgrade to be installed");
        }
        // Make sure merchant hasn't died, it somehow gone or moved out of range and still wants to trade this.
        if (info.merchant.get() instanceof Entity merchant && merchant.isAlive() && isInRange()) {
            if (!merchant.isAlive()) {
                return ResultWrapper.result(false, "trader died");
            }
            else if (!isInRange()) {
                return ResultWrapper.result(false, "out of range");
            }
            else {
                final Optional<MerchantOffer> recipe = info.recipe();
                if (recipe.isPresent()) {
                    if (recipe.get().isOutOfStock()) {
                        return ResultWrapper.result(false, "trade is disabled");
                    }
                    else if (!hasRoomForRecipe(inventory.get(), recipe.get())) {
                        return ResultWrapper.result(false, "not enough inventory space to trade");
                    }
                    else if (completeTrade(inventory.get(), recipe.get(), true) || completeTrade(inventory.get(), recipe.get(), false)) {
                        return ResultWrapper.result(true);
                    }
                    else {
                        return ResultWrapper.result(false, "not enough items to trade");
                    }
                }
                else return ResultWrapper.result(false, "trade has become invalid");
            }
        }
        else return ResultWrapper.result(false, "trade has become invalid");
    }

    public boolean hasRoomForRecipe(Container inventory, MerchantOffer recipe) {
        final ItemStack remainder = recipe.getResult().copy();
        InventoryUtils.insertIntoInventory(remainder, InventoryUtils.asItemHandler(inventory), remainder.getCount(), true);
        return remainder.getCount() == 0;
    }

    public boolean completeTrade(Container inventory, MerchantOffer recipe, boolean exact) {
        // Now we'll check if we have enough items to perform the trade, caching first
        final Merchant merchant = info.merchant.get();
        if (merchant == null) return false;

        final ItemStack firstInputStack = recipe.getCostA();
        final Optional<ItemStack> secondInputStack = !recipe.getCostB().isEmpty() ? Optional.of(recipe.getCostB()) : Optional.empty();

        // Check if we have enough to perform the trade.
        if (!containsAccumulativeItemStack(inventory, firstInputStack, exact) ||
            !secondInputStack.map(stack -> containsAccumulativeItemStack(inventory, stack, exact)).orElse(true))
            return false;

        // Now we need to check if we have enough inventory space to accept the item we get for the trade.
        final ItemStack outputStack = recipe.getResult().copy();

        // We established that out inventory allows to perform the trade, now actually do the trade.
        InventoryUtils.extractFromInventory(firstInputStack, InventoryUtils.asItemHandler(inventory), false, exact);
        secondInputStack.ifPresent(stack -> InventoryUtils.extractFromInventory(stack, InventoryUtils.asItemHandler(inventory), false, exact));
        InventoryUtils.insertIntoInventory(outputStack, InventoryUtils.asItemHandler(inventory), outputStack.getCount());

        // Tell the merchant we used the recipe, so MC can disable it and/or enable more recipes.
        merchant.notifyTrade(recipe);
        return true;
    }

    private static boolean containsAccumulativeItemStack(Container inventory, ItemStack stack, boolean exact) {
        return InventoryUtils.extractFromInventory(stack, inventory, null, true, exact).getCount() == 0;
    }
}
