package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverBeacon extends DriverSidedTileEntity {
    public static final DriverBeacon INSTANCE = new DriverBeacon();

    private DriverBeacon() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return BeaconBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((BeaconBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BeaconBlockEntity> implements NamedBlock {
        public Environment(BeaconBlockEntity tileEntity) {
            super(tileEntity, "beacon");
        }

        @Override
        public String preferredName() {
            return "beacon";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the number of levels for this beacon.")
        public Object[] getLevels(Context context, Arguments args) {
            return result(tileEntity.levels);
        }

        @Callback(doc = "function():string -- Get the name of the active primary effect.")
        public Object[] getPrimaryEffect(Context context, Arguments args) {
            return result(getEffectName(tileEntity.primaryPower));
        }

        @Callback(doc = "function():string -- Get the name of the active secondary effect.")
        public Object[] getSecondaryEffect(Context context, Arguments args) {
            return result(getEffectName(tileEntity.secondaryPower));
        }

        private static String getEffectName(MobEffect effect) {
            return effect != null ? String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(effect)) : null;
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.BEACON)
                return Environment.class;
            else return null;
        }
    }
}
