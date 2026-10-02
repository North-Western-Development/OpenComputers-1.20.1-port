package li.cil.oc.common.tileentity.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Base class of all OpenComputers block entities.
 *
 * <h2>How the Scala stackable traits were ported</h2>
 * In Scala every tile entity was {@code TileEntity with traits.Environment with traits.Rotatable ...},
 * where each trait could hold state and stack {@code abstract override}s of the lifecycle methods
 * ({@code updateEntity}, {@code initialize}, {@code dispose}, {@code loadForServer}, ...) on top of
 * each other via {@code super} calls. In Java:
 * <ul>
 *   <li><b>Type names are kept.</b> Every trait ({@code Environment}, {@code Rotatable},
 *   {@code RotatableTile}, {@code Inventory}, {@code ComponentInventory}, {@code Colored},
 *   {@code RedstoneAware}, {@code BundledRedstoneAware}, {@code OpenSides}, {@code Hub},
 *   {@code Computer}, {@code TextBuffer}, {@code PowerInformation}, {@code PowerBalancer},
 *   {@code PowerAcceptor}, {@code power.Common}, {@code Tickable}, {@code StateAware}, ...) is a
 *   Java <b>interface</b> in this package, so {@code instanceof traits.X} and casts keep working.
 *   Only {@code traits.TileEntity} (this class) is an abstract class; all trait interfaces extend
 *   {@link TileEntityTrait}, which re-declares the members of this class they need.</li>
 *   <li><b>Behaviour</b> lives in {@code default} methods with the Scala member names
 *   ({@code protected} members became public, since interfaces cannot have protected methods).
 *   Trait {@code var}s became accessor pairs ({@code relayDelay()} / {@code setRelayDelay(v)},
 *   {@code hasErrored()} / {@code setHasErrored(v)}, {@code openSides()} / {@code setOpenSides(v)},
 *   ...). Tiles override trait methods normally and call the trait's version with
 *   {@code SomeTrait.super.method(...)} (any trait the class directly implements works, also for
 *   methods it merely inherits).</li>
 *   <li><b>State</b> of stateful traits lives in a small {@code final class State} nested in each
 *   trait interface. One instance per trait is created by this base class in its constructor when
 *   {@code this instanceof Trait} and exposed through a public accessor
 *   ({@link #environmentState()}, {@link #redstoneAwareState()}, ...). The trait interfaces
 *   declare the same accessor abstractly, so it is satisfied by this class. A subclass may override
 *   an accessor to share state (e.g. {@code RobotProxy} returns its robot's redstone state, which
 *   is what the Scala code did by overriding {@code _input}/{@code _output}). Accessors return
 *   {@code null} for traits the tile does not implement.</li>
 *   <li><b>Stacked lifecycle overrides are dispatched explicitly</b> from this class:
 *   {@link #updateEntity()}, {@link #initialize()}, {@link #dispose()}, {@link #loadForServer},
 *   {@link #saveForServer}, {@link #loadForClient}, {@link #saveForClient}, {@link #setChanged()}
 *   and {@link #clearRemoved()} call each implemented trait's static hook
 *   ({@code Environment.onUpdateEntity(this)}, ...) in one fixed order that matches the Scala
 *   linearization of all tiles (super-first, base traits before derived ones, e.g.
 *   Environment, Hub, Inventory, ComponentInventory, TextBuffer, OpenSides, RedstoneAware,
 *   BundledRedstoneAware, Computer, PowerInformation, Colored, RotatableTile). A tile overriding one
 *   of these methods calls {@code super.x(...)} exactly where the Scala code called it; not calling
 *   super skips all trait behaviour, as in Scala (see {@code Robot}). The only "before super" trait
 *   hook, {@code Computer.updateEntity}, is run first.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * {@link #updateEntity()} is called every tick on both sides for tiles implementing
 * {@link Tickable} (by the block's {@code getTicker}). {@link #initialize()} runs from
 * {@link #clearRemoved()} (i.e. when the block entity is added to a level, including chunk loads),
 * {@link #dispose()} from {@link #setRemoved()}. In 1.20.1 vanilla chunk unloading calls
 * {@code setRemoved()} on every block entity of the chunk ({@code LevelChunk#clearAllBlockEntities}),
 * on both Forge and Fabric, so no extra chunk-unload hook is needed (Forge additionally calls
 * {@code onChunkUnloaded()} first, which we deliberately do not override, to avoid disposing twice).
 *
 * <h2>Syncing</h2>
 * The world save goes through {@link #saveForServer}/{@link #loadForServer}, the client description
 * (initial chunk data and {@link ClientboundBlockEntityDataPacket}) through
 * {@link #saveForClient}/{@link #loadForClient}. The client receives the description via
 * {@link #load(CompoundTag)}, which tells both apart by the {@code oc:isServerData} flag.
 */
public abstract class TileEntity extends BlockEntity implements TileEntityTrait {
    private static final String IsServerDataTag = Settings.namespace + "isServerData";

    // ----------------------------------------------------------------------- //
    // Trait state (see class comment).

    private final Environment.State environmentState;
    private final Inventory.State inventoryState;
    private final ComponentInventory.State componentInventoryState;
    private final Colored.State coloredState;
    private final RedstoneAware.State redstoneAwareState;
    private final BundledRedstoneAware.State bundledRedstoneAwareState;
    private final OpenSides.State openSidesState;
    private final PowerInformation.State powerInformationState;
    private final PowerBalancer.State powerBalancerState;
    private final RotatableTile.State rotatableTileState;
    private final Hub.State hubState;
    private final Computer.State computerState;
    private final TextBuffer.State textBufferState;

    protected TileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        environmentState = this instanceof Environment ? new Environment.State() : null;
        inventoryState = this instanceof Inventory ? new Inventory.State() : null;
        componentInventoryState = this instanceof ComponentInventory ? new ComponentInventory.State() : null;
        coloredState = this instanceof Colored ? new Colored.State() : null;
        redstoneAwareState = this instanceof RedstoneAware ? new RedstoneAware.State() : null;
        bundledRedstoneAwareState = this instanceof BundledRedstoneAware ? new BundledRedstoneAware.State() : null;
        openSidesState = this instanceof OpenSides ? new OpenSides.State() : null;
        powerInformationState = this instanceof PowerInformation ? new PowerInformation.State() : null;
        powerBalancerState = this instanceof PowerBalancer ? new PowerBalancer.State() : null;
        rotatableTileState = this instanceof RotatableTile ? new RotatableTile.State() : null;
        hubState = this instanceof Hub hub ? new Hub.State(hub) : null;
        computerState = this instanceof Computer ? new Computer.State() : null;
        textBufferState = this instanceof TextBuffer ? new TextBuffer.State() : null;
    }

    public Environment.State environmentState() {
        return environmentState;
    }

    public Inventory.State inventoryState() {
        return inventoryState;
    }

    public ComponentInventory.State componentInventoryState() {
        return componentInventoryState;
    }

    public Colored.State coloredState() {
        return coloredState;
    }

    public RedstoneAware.State redstoneAwareState() {
        return redstoneAwareState;
    }

    public BundledRedstoneAware.State bundledRedstoneAwareState() {
        return bundledRedstoneAwareState;
    }

    public OpenSides.State openSidesState() {
        return openSidesState;
    }

    public PowerInformation.State powerInformationState() {
        return powerInformationState;
    }

    public PowerBalancer.State powerBalancerState() {
        return powerBalancerState;
    }

    public RotatableTile.State rotatableTileState() {
        return rotatableTileState;
    }

    public Hub.State hubState() {
        return hubState;
    }

    public Computer.State computerState() {
        return computerState;
    }

    public TextBuffer.State textBufferState() {
        return textBufferState;
    }

    // ----------------------------------------------------------------------- //
    // TileEntityTrait accessors. These must not share names with BlockEntity's methods:
    // interface methods are not remapped together with Minecraft's, so an interface method
    // called "getLevel" would not be implemented by the remapped BlockEntity#getLevel in
    // production (AbstractMethodError).

    @Override
    public final Level ocLevel() {
        return getLevel();
    }

    @Override
    public final BlockPos ocBlockPos() {
        return getBlockPos();
    }

    @Override
    public final BlockState ocBlockState() {
        return getBlockState();
    }

    @Override
    public final void ocSetChanged() {
        setChanged();
    }

    @Override
    public final boolean ocIsRemoved() {
        return isRemoved();
    }

    @Override
    public int x() {
        return getBlockPos().getX();
    }

    @Override
    public int y() {
        return getBlockPos().getY();
    }

    @Override
    public int z() {
        return getBlockPos().getZ();
    }

    @Override
    public BlockPosition position() {
        return new BlockPosition(x(), y(), z(), getLevel());
    }

    @Override
    public boolean isClient() {
        return !isServer();
    }

    @Override
    public boolean isServer() {
        final Level level = getLevel();
        return level != null ? !level.isClientSide : SideTracker.isServer();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        // Computer's override ran its logic *before* calling super.
        if (this instanceof Computer computer) Computer.onUpdateEntity(computer);

        final Level level = getLevel();
        if (Settings.get().periodicallyForceLightUpdate && level != null && level.getGameTime() % 40 == 0 && getBlockState().getLightEmission() > 0) {
            final BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }

        if (this instanceof Environment environment) Environment.onUpdateEntity(environment);
        if (this instanceof Hub hub) Hub.onUpdateEntity(hub);
        if (this instanceof PowerBalancer balancer) PowerBalancer.onUpdateEntity(balancer);
        if (this instanceof TextBuffer textBuffer) TextBuffer.onUpdateEntity(textBuffer);
        if (this instanceof RedstoneAware redstone) RedstoneAware.onUpdateEntity(redstone);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        initialize();
        if (this instanceof RedstoneAware redstone) RedstoneAware.onClearRemoved(redstone);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        try {
            dispose();
        } catch (Throwable t) {
            OpenComputers.log.error("Failed properly disposing a tile entity, things may leak and or break.", t);
        }
    }

    @Override
    public void initialize() {
        if (this instanceof Environment environment) Environment.onInitialize(environment);
        if (this instanceof ComponentInventory inventory) ComponentInventory.onInitialize(inventory);
    }

    @Override
    public void dispose() {
        if (isClient()) {
            // Note: chunk unload is handled by sound via event handler.
            li.cil.oc.client.Sound.stopLoop(this);
        }
        if (this instanceof Environment environment) Environment.onDispose(environment);
        if (this instanceof ComponentInventory inventory) ComponentInventory.onDispose(inventory);
        if (this instanceof Computer computer) Computer.onDispose(computer);
    }

    @Override
    public void setChanged() {
        // Not super.setChanged(): that uses the final worldPosition field, but the robot moves
        // without being re-created and overrides getBlockPos() instead.
        final Level level = getLevel();
        if (level != null) {
            setChanged(level, getBlockPos(), getBlockState());
        }
        if (this instanceof Computer computer) Computer.onSetChanged(computer);
    }

    // ----------------------------------------------------------------------- //

    public void loadForServer(CompoundTag nbt) {
        if (this instanceof Environment environment) Environment.onLoadForServer(environment, nbt);
        if (this instanceof Hub hub) Hub.onLoadForServer(hub, nbt);
        if (this instanceof Inventory inventory) Inventory.onLoadForServer(inventory, nbt);
        if (this instanceof TextBuffer textBuffer) TextBuffer.onLoadForServer(textBuffer, nbt);
        if (this instanceof OpenSides openSides) OpenSides.onLoadForServer(openSides, nbt);
        if (this instanceof RedstoneAware redstone) RedstoneAware.onLoadForServer(redstone, nbt);
        if (this instanceof BundledRedstoneAware bundled) BundledRedstoneAware.onLoadForServer(bundled, nbt);
        if (this instanceof Computer computer) Computer.onLoadForServer(computer, nbt);
        if (this instanceof Colored colored) Colored.onLoadForServer(colored, nbt);
        if (this instanceof RotatableTile rotatable) RotatableTile.onLoadForServer(rotatable, nbt);
    }

    public void saveForServer(CompoundTag nbt) {
        nbt.putBoolean(IsServerDataTag, true);
        if (this instanceof Environment environment) Environment.onSaveForServer(environment, nbt);
        if (this instanceof Hub hub) Hub.onSaveForServer(hub, nbt);
        if (this instanceof Inventory inventory) Inventory.onSaveForServer(inventory, nbt);
        if (this instanceof TextBuffer textBuffer) TextBuffer.onSaveForServer(textBuffer, nbt);
        if (this instanceof OpenSides openSides) OpenSides.onSaveForServer(openSides, nbt);
        if (this instanceof RedstoneAware redstone) RedstoneAware.onSaveForServer(redstone, nbt);
        if (this instanceof BundledRedstoneAware bundled) BundledRedstoneAware.onSaveForServer(bundled, nbt);
        if (this instanceof Computer computer) Computer.onSaveForServer(computer, nbt);
        if (this instanceof Colored colored) Colored.onSaveForServer(colored, nbt);
        if (this instanceof RotatableTile rotatable) RotatableTile.onSaveForServer(rotatable, nbt);
    }

    /** Client side only. */
    public void loadForClient(CompoundTag nbt) {
        if (this instanceof ComponentInventory inventory) ComponentInventory.onLoadForClient(inventory, nbt);
        if (this instanceof TextBuffer textBuffer) TextBuffer.onLoadForClient(textBuffer, nbt);
        if (this instanceof OpenSides openSides) OpenSides.onLoadForClient(openSides, nbt);
        if (this instanceof RedstoneAware redstone) RedstoneAware.onLoadForClient(redstone, nbt);
        if (this instanceof Computer computer) Computer.onLoadForClient(computer, nbt);
        if (this instanceof PowerInformation power) PowerInformation.onLoadForClient(power, nbt);
        if (this instanceof Colored colored) Colored.onLoadForClient(colored, nbt);
        if (this instanceof RotatableTile rotatable) RotatableTile.onLoadForClient(rotatable, nbt);
    }

    public void saveForClient(CompoundTag nbt) {
        nbt.putBoolean(IsServerDataTag, false);
        if (this instanceof ComponentInventory inventory) ComponentInventory.onSaveForClient(inventory, nbt);
        if (this instanceof TextBuffer textBuffer) TextBuffer.onSaveForClient(textBuffer, nbt);
        if (this instanceof OpenSides openSides) OpenSides.onSaveForClient(openSides, nbt);
        if (this instanceof RedstoneAware redstone) RedstoneAware.onSaveForClient(redstone, nbt);
        if (this instanceof Computer computer) Computer.onSaveForClient(computer, nbt);
        if (this instanceof PowerInformation power) PowerInformation.onSaveForClient(power, nbt);
        if (this instanceof Colored colored) Colored.onSaveForClient(colored, nbt);
        if (this instanceof RotatableTile rotatable) RotatableTile.onSaveForClient(rotatable, nbt);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (isServer() || nbt.getBoolean(IsServerDataTag)) {
            loadForServer(nbt);
        } else {
            try {
                loadForClient(nbt);
            } catch (Throwable e) {
                OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (isServer()) {
            saveForServer(nbt);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag nbt = super.getUpdateTag();

        // See comment on savingForClients variable.
        SaveHandler.savingForClients = true;
        try {
            try {
                saveForClient(nbt);
            } catch (Throwable e) {
                OpenComputers.log.warn("There was a problem writing a TileEntity description packet. Please report this if you see it!", e);
            }
        } finally {
            SaveHandler.savingForClients = false;
        }

        return nbt;
    }
}
