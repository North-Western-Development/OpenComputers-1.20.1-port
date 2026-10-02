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
import dev.architectury.utils.GameInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

import static li.cil.oc.util.ResultWrapper.result;

public final class DriverCommandBlock extends DriverSidedTileEntity {
    public static final DriverCommandBlock INSTANCE = new DriverCommandBlock();

    private DriverCommandBlock() {
    }

    @Override
    public Class<?> getTileEntityClass() {
        return CommandBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((CommandBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<CommandBlockEntity> implements NamedBlock {
        public Environment(CommandBlockEntity tileEntity) {
            super(tileEntity, "command_block");
        }

        @Override
        public String preferredName() {
            return "command_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
        public Object[] getCommand(Context context, Arguments args) {
            return result(tileEntity.getCommandBlock().getCommand());
        }

        @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
        public Object[] setCommand(Context context, Arguments args) {
            tileEntity.getCommandBlock().setCommand(args.checkString(0));
            final Level level = tileEntity.getLevel();
            level.sendBlockUpdated(tileEntity.getBlockPos(), level.getBlockState(tileEntity.getBlockPos()), level.getBlockState(tileEntity.getBlockPos()), 3);
            return result(true);
        }

        @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
        public Object[] executeCommand(Context context, Arguments args) {
            context.pause(0.1);
            final MinecraftServer server = GameInstance.getServer();
            if (server == null || !server.isCommandBlockEnabled()) {
                return result(null, "command blocks are disabled");
            } else {
                final BaseCommandBlock commandSender = tileEntity.getCommandBlock();
                commandSender.performCommand(tileEntity.getLevel());
                return result(commandSender.getSuccessCount(), commandSender.getLastOutput().getString());
            }
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.COMMAND_BLOCK)
                return Environment.class;
            else return null;
        }
    }
}
