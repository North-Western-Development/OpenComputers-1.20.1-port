package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Relay extends SimpleBlock implements GUI, PowerAcceptor {
    public Relay(Properties props) {
        super(props);
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Relay te) {
            ContainerTypes.openRelayGui(player, te);
        }
    }

    @Override
    public double energyThroughput() {
        return Settings.get().accessPointRate;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.RELAY.get().create(pos, state);
    }
}
