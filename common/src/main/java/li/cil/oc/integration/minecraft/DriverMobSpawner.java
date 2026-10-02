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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverMobSpawner extends DriverSidedTileEntity {
    public static final DriverMobSpawner INSTANCE = new DriverMobSpawner();

    private DriverMobSpawner() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return SpawnerBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((SpawnerBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<SpawnerBlockEntity> implements NamedBlock {
        public Environment(SpawnerBlockEntity tileEntity) {
            super(tileEntity, "mob_spawner");
        }

        @Override
        public String preferredName() {
            return "mob_spawner";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(Context context, Arguments args) {
            // BaseSpawner.nextSpawnData is made accessible by opencomputers.accesswidener.
            final SpawnData spawnData = tileEntity.getSpawner().nextSpawnData;
            if (spawnData == null) return result((Object) null);
            final CompoundTag entity = spawnData.getEntityToSpawn();
            if (entity == null || !entity.contains("id")) return result((Object) null);
            return result(entity.getString("id"));
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.SPAWNER)
                return Environment.class;
            else return null;
        }
    }
}
