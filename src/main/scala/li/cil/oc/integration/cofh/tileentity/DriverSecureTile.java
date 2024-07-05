package li.cil.oc.integration.cofh.tileentity;

import cofh.lib.util.control.ISecurable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.lang3.text.WordUtils;

public final class DriverSecureTile extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return ISecurable.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        ISecurable tileEntity = (ISecurable) world.getBlockEntity(pos);
        if (!tileEntity.isSecurable()) return null;
        return new Environment(tileEntity);
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<ISecurable> {
        public Environment(final ISecurable tileEntity) {
            super(tileEntity, "secure_tile");
        }

        @Callback(doc = "function(name:string):boolean --  Returns whether the player with the given name can access the component")
        public Object[] canPlayerAccess(final Context context, final Arguments args) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer player = server.getPlayerList().getPlayerByName(args.checkString(0));
            return new Object[]{player != null && tileEntity.canAccess(player)};
        }

        @Callback(doc = "function():string --  Returns the type of the access.")
        public Object[] getAccess(final Context context, final Arguments args) {
            return new Object[]{WordUtils.capitalize(tileEntity.getAccess().name())};
        }

        @Callback(doc = "function():string --  Returns the name of the owner.")
        public Object[] getOwnerName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOwnerName()};
        }
    }
}
