package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.tileentity.traits.RotatableTile;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.ExtendedAABB;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Port note: Forge IModelData was dropped; the renderer reads `data` / `state` directly.
public class Print extends TileEntity implements RedstoneAware, RotatableTile {
    public final Optional<Supplier<Boolean>> canToggle;
    public final Optional<Consumer<Integer>> scheduleUpdate;
    public final Optional<Runnable> onStateChange;

    public final PrintData data = new PrintData();

    public VoxelShape shapeOff = Shapes.block();
    public VoxelShape shapeOn = Shapes.block();
    public boolean state = false;

    private static final String DataTag = Settings.namespace + "data";
    @Deprecated
    private static final String DataTagCompat = "data";
    private static final String StateTag = Settings.namespace + "state";
    @Deprecated
    private static final String StateTagCompat = "state";

    public Print(BlockEntityType<?> type, BlockPos pos, BlockState blockState, Optional<Supplier<Boolean>> canToggle, Optional<Consumer<Integer>> scheduleUpdate, Optional<Runnable> onStateChange) {
        super(type, pos, blockState);
        this.canToggle = canToggle;
        this.scheduleUpdate = scheduleUpdate;
        this.onStateChange = onStateChange;
        redstoneAwareState().isOutputEnabled = true;
    }

    public Print(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        this(type, pos, blockState, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Print(BlockEntityType<?> type, BlockPos pos, BlockState blockState, Supplier<Boolean> canToggle, Consumer<Integer> scheduleUpdate, Runnable onStateChange) {
        this(type, pos, blockState, Optional.ofNullable(canToggle), Optional.ofNullable(scheduleUpdate), Optional.ofNullable(onStateChange));
    }

    public VoxelShape shape() {
        return state ? shapeOn : shapeOff;
    }

    public boolean noclip() {
        return state ? data.noclipOn : data.noclipOff;
    }

    public Set<PrintData.Shape> shapes() {
        return state ? data.stateOn : data.stateOff;
    }

    public boolean activate() {
        if (data.hasActiveState()) {
            if (!state || !data.isButtonMode) {
                toggleState();
                return true;
            }
        }
        return false;
    }

    private Map<Object, Object> buildValueSet(int value) {
        final Map<Object, Object> map = new HashMap<>();
        for (Direction side : Direction.values()) {
            map.put(side.ordinal(), value);
        }
        return map;
    }

    public void toggleState() {
        if (canToggle.map(Supplier::get).orElse(true)) {
            final Level level = getLevel();
            state = !state;
            level.playSound(null, getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, state ? 0.6F : 0.5F);
            final BlockState blockState = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), blockState, blockState, 3);
            updateRedstone();
            if (state && data.isButtonMode) {
                final li.cil.oc.common.block.Print block = (li.cil.oc.common.block.Print) li.cil.oc.api.Items.get(Constants.BlockName.Print).block();
                final int delay = block.tickRate(level);
                if (scheduleUpdate.isPresent()) {
                    scheduleUpdate.get().accept(delay);
                } else if (!level.isClientSide) {
                    level.scheduleTick(getBlockPos(), block, delay);
                }
            }
            onStateChange.ifPresent(Runnable::run);
        }
    }

    private VoxelShape convertShape(Set<PrintData.Shape> state) {
        if (!state.isEmpty()) {
            VoxelShape curr = Shapes.empty();
            for (PrintData.Shape s : state) {
                final VoxelShape voxel = Shapes.create(ExtendedAABB.rotateTowards(s.bounds, facing()));
                curr = Shapes.joinUnoptimized(curr, voxel, BooleanOp.OR);
            }
            return curr.optimize();
        } else return Shapes.block();
    }

    public void updateShape() {
        shapeOff = convertShape(data.stateOff);
        shapeOn = convertShape(data.stateOn);
    }

    public void updateRedstone() {
        if (data.emitRedstone()) {
            setOutput(buildValueSet(data.emitRedstone(state) ? data.redstoneLevel : 0));
        }
    }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        final boolean newState = args.newValue > 0;
        if (!data.emitRedstone() && data.hasActiveState() && state != newState) {
            toggleState();
        }
    }

    @Override
    public void onRotationChanged() {
        RotatableTile.super.onRotationChanged();
        updateShape();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        if (nbt.contains(DataTagCompat))
            data.loadData(nbt.getCompound(DataTagCompat));
        else
            data.loadData(nbt.getCompound(DataTag));
        if (nbt.contains(StateTagCompat))
            state = nbt.getBoolean(StateTagCompat);
        else
            state = nbt.getBoolean(StateTag);
        updateShape();
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, DataTag, data::saveData);
        nbt.putBoolean(StateTag, state);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        data.loadData(nbt.getCompound(DataTag));
        state = nbt.getBoolean(StateTag);
        updateShape();
        final Level level = getLevel();
        if (level != null) {
            final BlockState blockState = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), blockState, blockState, 3);
            if (data.emitLight()) level.getLightEngine().checkBlock(getBlockPos());
        }
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, DataTag, data::saveData);
        nbt.putBoolean(StateTag, state);
    }
}
