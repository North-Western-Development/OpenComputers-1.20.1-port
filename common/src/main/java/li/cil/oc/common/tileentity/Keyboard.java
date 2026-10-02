package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// TODO(port): integration - the Immibis microblocks marker trait (traits.ImmibisMicroblock) was dropped.
public class Keyboard extends TileEntity implements Environment, Rotatable, SidedEnvironment, Analyzable {
    public final ManagedEnvironment keyboard;

    private static final String KeyboardTag = Settings.namespace + "keyboard";

    public Keyboard(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        final ItemStack keyboardItem = li.cil.oc.api.Items.get(Constants.BlockName.Keyboard).createItemStack(1);
        keyboard = li.cil.oc.api.Driver.driverFor(keyboardItem, getClass()).createEnvironment(keyboardItem, this);
    }

    @Override
    public Direction[] validFacings() {
        return Direction.values();
    }

    @Override
    public Node node() {
        return keyboard.node();
    }

    public boolean hasNodeOnSide(Direction side) {
        return side != facing() && (isOnWall() || side.getOpposite() != forward());
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return hasNodeOnSide(side);
    }

    @Override
    public Node sidedNode(Direction side) {
        return hasNodeOnSide(side) ? node() : null;
    }

    // Override automatic analyzer implementation for sided environments.
    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{node()};
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        if (isServer()) {
            keyboard.loadData(nbt.getCompound(KeyboardTag));
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        if (isServer()) {
            ExtendedNBT.setNewCompoundTag(nbt, KeyboardTag, keyboard::saveData);
        }
    }

    // ----------------------------------------------------------------------- //

    private boolean isOnWall() {
        return facing() != Direction.UP && facing() != Direction.DOWN;
    }

    private Direction forward() {
        return isOnWall() ? Direction.UP : yaw();
    }
}
