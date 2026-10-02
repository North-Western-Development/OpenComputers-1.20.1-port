package li.cil.oc.common.recipe;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class ExtendedRecipe {
    private ExtendedRecipe() {
    }

    private static ItemInfo get(String name) {
        return li.cil.oc.api.Items.get(name);
    }

    private static final TagKey<Item> beaconBlocks = TagKey.create(Registries.ITEM, new ResourceLocation("opencomputers", "beacon_base_blocks"));

    public static ItemStack addNBTToResult(Recipe<?> recipe, ItemStack craftedStack, CraftingContainer inventory) {
        final ItemInfo craftedItemName = li.cil.oc.api.Items.get(craftedStack);

        final ItemInfo navigationUpgrade = get(Constants.ItemName.NavigationUpgrade);
        final ItemInfo linkedCard = get(Constants.ItemName.LinkedCard);
        final ItemInfo floppy = get(Constants.ItemName.Floppy);
        final ItemInfo print = get(Constants.BlockName.Print);
        final ItemInfo eeprom = get(Constants.ItemName.EEPROM);
        final List<ItemInfo> hdds = Arrays.asList(
            get(Constants.ItemName.HDDTier1),
            get(Constants.ItemName.HDDTier2),
            get(Constants.ItemName.HDDTier3));
        final List<ItemInfo> cpus = Arrays.asList(
            get(Constants.ItemName.CPUTier1),
            get(Constants.ItemName.CPUTier2),
            get(Constants.ItemName.CPUTier3),
            get(Constants.ItemName.APUTier1),
            get(Constants.ItemName.APUTier2));

        if (craftedItemName == navigationUpgrade) {
            final DriverItem driver = li.cil.oc.api.Driver.driverFor(craftedStack);
            if (driver != null) {
                for (ItemStack stack : getItems(inventory)) {
                    if (stack.getItem() == Items.FILLED_MAP) {
                        // Store information of the map used for crafting in the result.
                        final CompoundTag nbt = driver.dataTag(craftedStack);
                        ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "map", stack::save);
                    }
                }
            }
        }

        if (craftedItemName == linkedCard) {
            if (SideTracker.isServer()) {
                final DriverItem driver = li.cil.oc.api.Driver.driverFor(craftedStack);
                if (driver != null) {
                    final CompoundTag nbt = driver.dataTag(craftedStack);
                    nbt.putString(Settings.namespace + "tunnel", UUID.randomUUID().toString());
                }
            }
        }

        if (craftedItemName != null && cpus.contains(craftedItemName)) {
            LuaStateFactory.setDefaultArch(craftedStack);
        }

        if (craftedItemName != null && (craftedItemName == floppy || hdds.contains(craftedItemName))) {
            final CompoundTag nbt = craftedStack.getOrCreateTag();
            if (recipe.canCraftInDimensions(1, 1)) {
                // Formatting / loot to normal disk conversion, only keep coloring.
                final String colorKey = Settings.namespace + "color";
                for (ItemStack stack : getItems(inventory)) {
                    final ItemInfo info = li.cil.oc.api.Items.get(stack);
                    if (info != null && (info == floppy || "lootDisk".equals(info.name())) && stack.hasTag()) {
                        final CompoundTag oldData = stack.getTag();
                        if (oldData.contains(colorKey) && oldData.getInt(colorKey) != DyeColor.LIGHT_GRAY.getId()) {
                            nbt.put(colorKey, oldData.get(colorKey).copy());
                        }
                    }
                }
                if (nbt.isEmpty()) {
                    craftedStack.setTag(null);
                }
            } else if (getItems(inventory).stream().allMatch(s -> li.cil.oc.api.Items.get(s) == floppy)) {
                // Copy operation.
                for (ItemStack stack : getItems(inventory)) {
                    if (li.cil.oc.api.Items.get(stack) == floppy && stack.hasTag()) {
                        final CompoundTag oldData = stack.getTag();
                        for (String oldTagName : oldData.getAllKeys()) {
                            if (!nbt.contains(oldTagName)) {
                                nbt.put(oldTagName, oldData.get(oldTagName).copy());
                            }
                        }
                    }
                }
            }
        }

        if (craftedItemName != null && craftedItemName == print &&
            recipe.getIngredients().size() == 2) {
            // First, copy old data.
            final PrintData data = new PrintData(craftedStack);
            final List<ItemStack> inputs = getItems(inventory);
            for (ItemStack stack : inputs) {
                if (li.cil.oc.api.Items.get(stack) == print) {
                    data.loadData(stack);
                }
            }

            // Then apply new data.
            for (ItemStack stack : inputs) {
                if (stack.is(beaconBlocks)) {
                    if (data.isBeaconBase) {
                        // Crafting wouldn't change anything, prevent accidental resource loss.
                        return ItemStack.EMPTY;
                    }
                    data.isBeaconBase = true;
                }
                if (stack.is(Items.GLOWSTONE_DUST)) {
                    if (data.lightLevel == 15) {
                        // Crafting wouldn't change anything, prevent accidental resource loss.
                        return ItemStack.EMPTY;
                    }
                    data.lightLevel = Math.min(15, data.lightLevel + 1);
                }
                if (stack.is(Blocks.GLOWSTONE.asItem())) {
                    if (data.lightLevel == 15) {
                        // Crafting wouldn't change anything, prevent accidental resource loss.
                        return ItemStack.EMPTY;
                    }
                    data.lightLevel = Math.min(15, data.lightLevel + 4);
                }
            }

            // Finally apply modified data.
            data.saveData(craftedStack);
        }

        // EEPROM copying.
        if (craftedItemName != null && craftedItemName == eeprom &&
            craftedStack.getCount() == 2 &&
            recipe.getIngredients().size() == 2) {
            for (ItemStack stack : getItems(inventory)) {
                if (li.cil.oc.api.Items.get(stack) == eeprom && stack.hasTag()) {
                    final CompoundTag copy = stack.getTag().copy();
                    // Erase node address, just in case.
                    copy.getCompound(Settings.namespace + "data").getCompound("node").remove("address");
                    craftedStack.setTag(copy);
                    break;
                }
            }
        }

        // Swapping EEPROM in devices.
        recraft(craftedStack, inventory, get(Constants.BlockName.Microcontroller), MCUDataWrapper::new);
        recraft(craftedStack, inventory, get(Constants.ItemName.Drone), DroneDataWrapper::new);
        recraft(craftedStack, inventory, get(Constants.BlockName.Robot), RobotDataWrapper::new);
        recraft(craftedStack, inventory, get(Constants.ItemName.Tablet), TabletDataWrapper::new);

        return craftedStack;
    }

    private static List<ItemStack> getItems(CraftingContainer inventory) {
        final List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            final ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    private static void recraft(ItemStack craftedStack, CraftingContainer inventory, ItemInfo descriptor, Function<ItemStack, ItemDataWrapper> dataFactory) {
        if (descriptor != null && li.cil.oc.api.Items.get(craftedStack) == descriptor) {
            // Find old Microcontroller.
            for (ItemStack oldMcu : getItems(inventory)) {
                if (li.cil.oc.api.Items.get(oldMcu) != descriptor) continue;
                final ItemDataWrapper data = dataFactory.apply(oldMcu);
                final ItemInfo eeprom = get(Constants.ItemName.EEPROM);

                // Remove old EEPROM (Scala's `diff` removed each matching instance once).
                final List<ItemStack> components = new ArrayList<>(Arrays.asList(data.components()));
                components.removeIf(stack -> li.cil.oc.api.Items.get(stack) == eeprom);

                // Insert new EEPROM.
                for (ItemStack stack : getItems(inventory)) {
                    if (li.cil.oc.api.Items.get(stack) == eeprom) {
                        components.add(stack.copy().split(1));
                    }
                }

                data.setComponents(components.toArray(new ItemStack[0]));
                data.save(craftedStack);
                break;
            }
        }
    }

    private interface ItemDataWrapper {
        ItemStack[] components();

        void setComponents(ItemStack[] value);

        void save(ItemStack stack);
    }

    private static final class MCUDataWrapper implements ItemDataWrapper {
        final MicrocontrollerData data;

        MCUDataWrapper(ItemStack stack) {
            data = new MicrocontrollerData(stack);
        }

        @Override
        public ItemStack[] components() {
            return data.components;
        }

        @Override
        public void setComponents(ItemStack[] value) {
            data.components = value;
        }

        @Override
        public void save(ItemStack stack) {
            data.saveData(stack);
        }
    }

    private static final class DroneDataWrapper implements ItemDataWrapper {
        final DroneData data;

        DroneDataWrapper(ItemStack stack) {
            data = new DroneData(stack);
        }

        @Override
        public ItemStack[] components() {
            return data.components;
        }

        @Override
        public void setComponents(ItemStack[] value) {
            data.components = value;
        }

        @Override
        public void save(ItemStack stack) {
            data.saveData(stack);
        }
    }

    private static final class RobotDataWrapper implements ItemDataWrapper {
        final RobotData data;

        RobotDataWrapper(ItemStack stack) {
            data = new RobotData(stack);
        }

        @Override
        public ItemStack[] components() {
            return data.components;
        }

        @Override
        public void setComponents(ItemStack[] value) {
            data.components = value;
        }

        @Override
        public void save(ItemStack stack) {
            data.saveData(stack);
        }
    }

    private static final class TabletDataWrapper implements ItemDataWrapper {
        final TabletData data;

        ItemStack[] components;

        TabletDataWrapper(ItemStack stack) {
            data = new TabletData(stack);
            components = Arrays.stream(data.items).filter(s -> !s.isEmpty()).toArray(ItemStack[]::new);
        }

        @Override
        public ItemStack[] components() {
            return components;
        }

        @Override
        public void setComponents(ItemStack[] value) {
            components = value;
        }

        @Override
        public void save(ItemStack stack) {
            data.items = components.clone();
            data.saveData(stack);
        }
    }
}
