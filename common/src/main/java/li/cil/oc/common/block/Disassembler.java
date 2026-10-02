package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class Disassembler extends SimpleBlock implements PowerAcceptor, StateAware, GUI {
    public Disassembler(Properties props) {
        super(props);
    }

    @Override
    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        addLines(tooltip, Tooltip.get(getClass().getSimpleName().toLowerCase(), String.valueOf((int) (Settings.get().disassemblerBreakChance * 100))));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double energyThroughput() {
        return Settings.get().disassemblerRate;
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Disassembler te) {
            ContainerTypes.openDisassemblerGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.DISASSEMBLER.get().create(pos, state);
    }
}
