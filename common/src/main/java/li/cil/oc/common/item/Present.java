package li.cil.oc.common.item;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Present extends SimpleItem {
    public Present(Properties props) {
        super(props);
    }

    // Formerly fillItemCategory {}: not in the creative tab (see Items).

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (stack.getCount() > 0) {
            stack.shrink(1);
            if (!world.isClientSide) {
                world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.2f, 1f);
                final ItemStack present = nextPresent(world.getRecipeManager(), world.registryAccess());
                InventoryUtils.addToPlayerInventory(present, player);
            }
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    // ----------------------------------------------------------------------- //

    private static List<ItemStack> presents;

    private static synchronized List<ItemStack> presents(RecipeManager recipeManager, RegistryAccess registryAccess) {
        if (presents == null) {
            final List<ItemStack> result = new ArrayList<>();
            add(result, recipeManager, registryAccess, Constants.ItemName.ArrowKeys, 520);
            add(result, recipeManager, registryAccess, Constants.ItemName.ButtonGroup, 460);
            add(result, recipeManager, registryAccess, Constants.ItemName.NumPad, 410);
            add(result, recipeManager, registryAccess, Constants.ItemName.Disk, 370);
            add(result, recipeManager, registryAccess, Constants.ItemName.Transistor, 350);
            add(result, recipeManager, registryAccess, Constants.ItemName.Floppy, 340);
            add(result, recipeManager, registryAccess, Constants.ItemName.PrintedCircuitBoard, 320);
            add(result, recipeManager, registryAccess, Constants.ItemName.ChipTier1, 290);
            add(result, recipeManager, registryAccess, Constants.ItemName.EEPROM, 250);
            add(result, recipeManager, registryAccess, Constants.ItemName.Interweb, 220);
            add(result, recipeManager, registryAccess, Constants.ItemName.Card, 190);
            add(result, recipeManager, registryAccess, Constants.ItemName.Analyzer, 170);
            add(result, recipeManager, registryAccess, Constants.ItemName.SignUpgrade, 150);
            add(result, recipeManager, registryAccess, Constants.ItemName.InventoryUpgrade, 130);
            add(result, recipeManager, registryAccess, Constants.ItemName.CraftingUpgrade, 110);
            add(result, recipeManager, registryAccess, Constants.ItemName.TankUpgrade, 90);
            add(result, recipeManager, registryAccess, Constants.ItemName.PistonUpgrade, 80);
            add(result, recipeManager, registryAccess, Constants.ItemName.LeashUpgrade, 70);
            add(result, recipeManager, registryAccess, Constants.ItemName.AngelUpgrade, 55);
            add(result, recipeManager, registryAccess, Constants.ItemName.RedstoneCardTier1, 50);
            add(result, recipeManager, registryAccess, Constants.ItemName.RAMTier1, 48);
            add(result, recipeManager, registryAccess, Constants.ItemName.ControlUnit, 46);
            add(result, recipeManager, registryAccess, Constants.ItemName.Alu, 45);
            add(result, recipeManager, registryAccess, Constants.ItemName.BatteryUpgradeTier1, 43);
            add(result, recipeManager, registryAccess, Constants.ItemName.NetworkCard, 38);
            add(result, recipeManager, registryAccess, Constants.ItemName.WirelessNetworkCardTier1, 37);
            add(result, recipeManager, registryAccess, Constants.ItemName.HDDTier1, 36);
            add(result, recipeManager, registryAccess, Constants.ItemName.GeneratorUpgrade, 35);
            add(result, recipeManager, registryAccess, Constants.ItemName.CPUTier1, 31);
            add(result, recipeManager, registryAccess, Constants.ItemName.MicrocontrollerCaseTier1, 30);
            add(result, recipeManager, registryAccess, Constants.ItemName.DroneCaseTier1, 25);
            add(result, recipeManager, registryAccess, Constants.ItemName.UpgradeContainerTier1, 23);
            add(result, recipeManager, registryAccess, Constants.ItemName.CardContainerTier1, 23);
            add(result, recipeManager, registryAccess, Constants.ItemName.GraphicsCardTier1, 19);
            add(result, recipeManager, registryAccess, Constants.ItemName.RedstoneCardTier2, 17);
            add(result, recipeManager, registryAccess, Constants.ItemName.RAMTier2, 15);
            add(result, recipeManager, registryAccess, Constants.ItemName.DatabaseUpgradeTier1, 15);
            add(result, recipeManager, registryAccess, Constants.ItemName.ChipTier2, 15);
            add(result, recipeManager, registryAccess, Constants.ItemName.ComponentBusTier1, 13);
            add(result, recipeManager, registryAccess, Constants.ItemName.BatteryUpgradeTier2, 12);
            add(result, recipeManager, registryAccess, Constants.ItemName.WirelessNetworkCardTier2, 11);
            add(result, recipeManager, registryAccess, Constants.ItemName.RAMTier3, 10);
            add(result, recipeManager, registryAccess, Constants.ItemName.ServerTier1, 10);
            add(result, recipeManager, registryAccess, Constants.ItemName.InternetCard, 9);
            add(result, recipeManager, registryAccess, Constants.ItemName.Terminal, 9);
            add(result, recipeManager, registryAccess, Constants.ItemName.SolarGeneratorUpgrade, 9);
            add(result, recipeManager, registryAccess, Constants.ItemName.HDDTier2, 7);
            add(result, recipeManager, registryAccess, Constants.ItemName.NavigationUpgrade, 7);
            add(result, recipeManager, registryAccess, Constants.ItemName.InventoryControllerUpgrade, 7);
            add(result, recipeManager, registryAccess, Constants.ItemName.TankControllerUpgrade, 7);
            add(result, recipeManager, registryAccess, Constants.ItemName.CPUTier2, 6);
            add(result, recipeManager, registryAccess, Constants.ItemName.MicrocontrollerCaseTier2, 6);
            add(result, recipeManager, registryAccess, Constants.ItemName.ComponentBusTier2, 6);
            add(result, recipeManager, registryAccess, Constants.ItemName.TabletCaseTier1, 5);
            add(result, recipeManager, registryAccess, Constants.ItemName.UpgradeContainerTier2, 5);
            add(result, recipeManager, registryAccess, Constants.ItemName.CardContainerTier2, 5);
            add(result, recipeManager, registryAccess, Constants.ItemName.GraphicsCardTier2, 4);
            add(result, recipeManager, registryAccess, Constants.ItemName.RAMTier4, 4);
            add(result, recipeManager, registryAccess, Constants.ItemName.DroneCaseTier2, 4);
            add(result, recipeManager, registryAccess, Constants.ItemName.DatabaseUpgradeTier2, 4);
            add(result, recipeManager, registryAccess, Constants.ItemName.ServerTier2, 4);
            add(result, recipeManager, registryAccess, Constants.ItemName.ChipTier3, 3);
            add(result, recipeManager, registryAccess, Constants.ItemName.ComponentBusTier3, 3);
            add(result, recipeManager, registryAccess, Constants.ItemName.TractorBeamUpgrade, 3);
            add(result, recipeManager, registryAccess, Constants.ItemName.BatteryUpgradeTier3, 3);
            add(result, recipeManager, registryAccess, Constants.ItemName.ExperienceUpgrade, 2);
            add(result, recipeManager, registryAccess, Constants.ItemName.RAMTier5, 2);
            add(result, recipeManager, registryAccess, Constants.ItemName.UpgradeContainerTier3, 2);
            add(result, recipeManager, registryAccess, Constants.ItemName.CardContainerTier3, 2);
            add(result, recipeManager, registryAccess, Constants.ItemName.TabletCaseTier2, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.HDDTier3, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.ChunkloaderUpgrade, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.CPUTier3, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.GraphicsCardTier3, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.ServerTier3, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.DatabaseUpgradeTier3, 1);
            add(result, recipeManager, registryAccess, Constants.ItemName.RAMTier6, 1);
            presents = result;
        }
        return presents;
    }

    private static void add(List<ItemStack> result, RecipeManager recipeManager, RegistryAccess registryAccess, String name, int weight) {
        final ItemInfo item = li.cil.oc.api.Items.get(name);
        if (item != null) {
            final ItemStack stack = item.createItemStack(1);
            // Only if it can be crafted (wasn't disabled in the config).
            if (ItemUtils.getIngredients(recipeManager, registryAccess, stack).length > 0) {
                for (int i = 0; i < weight; i++) result.add(stack);
            }
        } else {
            OpenComputers.log.warn("Oops, trying to add '" + name + "' as a present even though it doesn't exist!");
        }
    }

    private static final Random rng = new Random();

    private static ItemStack nextPresent(RecipeManager recipeManager, RegistryAccess registryAccess) {
        final List<ItemStack> list = presents(recipeManager, registryAccess);
        return list.get(rng.nextInt(list.size())).copy();
    }
}
