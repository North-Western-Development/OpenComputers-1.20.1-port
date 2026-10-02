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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverRecordPlayer extends DriverSidedTileEntity {
    public static final DriverRecordPlayer INSTANCE = new DriverRecordPlayer();

    private DriverRecordPlayer() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return JukeboxBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((JukeboxBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<JukeboxBlockEntity> implements NamedBlock {
        public Environment(JukeboxBlockEntity tileEntity) {
            super(tileEntity, "jukebox");
        }

        @Override
        public String preferredName() {
            return "jukebox";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the title of the record currently in the jukebox.")
        public Object[] getRecord(Context context, Arguments args) {
            final ItemStack record = tileEntity.getItem(0);
            if (!record.isEmpty() && record.getItem() instanceof RecordItem recordItem) {
                return result(recordItem.getDisplayName().getString());
            } else return null;
        }

        @Callback(doc = "function() -- Start playing the record currently in the jukebox.")
        public Object[] play(Context context, Arguments args) {
            final ItemStack record = tileEntity.getItem(0);
            if (!record.isEmpty() && record.getItem() instanceof RecordItem) {
                // TODO(port): this only starts the client side sound, like on 1.16.5; the
                //  jukebox block entity's own playing state is not touched.
                tileEntity.getLevel().levelEvent(null, 1010, tileEntity.getBlockPos(), Item.getId(record.getItem()));
                return result(true);
            } else return null;
        }

        @Callback(doc = "function() -- Stop playing the record currently in the jukebox.")
        public Object[] stop(Context context, Arguments args) {
            tileEntity.getLevel().levelEvent(1010, tileEntity.getBlockPos(), 0);
            return null;
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack.getItem() == Blocks.JUKEBOX.asItem())
                return Environment.class;
            else return null;
        }
    }
}
