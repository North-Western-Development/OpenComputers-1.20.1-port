package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;

public final class ModMinecraft implements ModProxy, BundledRedstone.RedstoneProvider {
    public static final ModMinecraft INSTANCE = new ModMinecraft();

    private ModMinecraft() {
    }

    @Override
    public Mod getMod() {
        return Mods.Minecraft;
    }

    @Override
    public void initialize() {
        Driver.add(DriverBeacon.INSTANCE);
        Driver.add(DriverBrewingStand.INSTANCE);
        Driver.add(DriverComparator.INSTANCE);
        Driver.add(DriverFurnace.INSTANCE);
        Driver.add(DriverMobSpawner.INSTANCE);
        Driver.add(DriverNoteBlock.INSTANCE);
        Driver.add(DriverRecordPlayer.INSTANCE);

        Driver.add(DriverBeacon.Provider.INSTANCE);
        Driver.add(DriverBrewingStand.Provider.INSTANCE);
        Driver.add(DriverComparator.Provider.INSTANCE);
        Driver.add(DriverFurnace.Provider.INSTANCE);
        Driver.add(DriverMobSpawner.Provider.INSTANCE);
        Driver.add(DriverNoteBlock.Provider.INSTANCE);
        Driver.add(DriverRecordPlayer.Provider.INSTANCE);

        if (Settings.get().enableInventoryDriver) {
            Driver.add(new DriverInventory());
        }
        if (Settings.get().enableTankDriver) {
            Driver.add(new DriverFluidHandler());
            Driver.add(new DriverFluidTank());
        }
        if (Settings.get().enableCommandBlockDriver) {
            Driver.add(DriverCommandBlock.INSTANCE);
        }

        Driver.add(ConverterFluidContainerItem.INSTANCE);
        Driver.add(ConverterFluidStack.INSTANCE);
        Driver.add(ConverterFluidTankInfo.INSTANCE);
        Driver.add(ConverterFluidTankProperties.INSTANCE);
        Driver.add(ConverterItemStack.INSTANCE);
        Driver.add(ConverterNBT.INSTANCE);
        Driver.add(ConverterWorld.INSTANCE);

        BundledRedstone.addProvider(this);

        EventHandlerVanilla.register();
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        final Level world = pos.world.get();
        final BlockPosition neighbor = pos.offset(side);
        return Math.max(ExtendedWorld.computeRedstoneSignal(world, pos, side),
                ExtendedWorld.getBlock(world, neighbor) == Blocks.REDSTONE_WIRE ? world.getBlockState(neighbor.toBlockPos()).getValue(RedStoneWireBlock.POWER) : 0);
    }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        return null;
    }
}
