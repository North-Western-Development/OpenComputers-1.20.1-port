package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.traits.Colored;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.TextBuffer;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.Color;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

public class Screen extends TileEntity implements TextBuffer, SidedEnvironment, Rotatable, RedstoneAware, Colored, Analyzable, Comparable<Screen> {
    public int tier;

    /**
     * Check for multi-block screen option in next update. We do this in the
     * update to avoid unnecessary checks on chunk unload.
     */
    public boolean shouldCheckForMultiBlock = true;

    /**
     * On the client we delay connecting screens a little, to avoid glitches
     * when not all tile entity data for a chunk has been received within a
     * single tick (meaning some screens are still "missing").
     */
    public int delayUntilCheckForMultiBlock = 40;

    public int width = 1;
    public int height = 1;

    public Screen origin = this;

    public final Set<Screen> screens = new HashSet<>();

    public boolean hadRedstoneInput = false;

    public Optional<AABB> cachedBounds = Optional.empty();

    public boolean invertTouchMode = false;

    private final Set<Arrow> arrows = new HashSet<>();

    private final Map<Entity, Pair<Integer, Integer>> lastWalked = new WeakHashMap<>();

    private static final String TierTag = Settings.namespace + "tier";
    private static final String HadRedstoneInputTag = Settings.namespace + "hadRedstoneInput";
    private static final String InvertTouchModeTag = Settings.namespace + "invertTouchMode";

    public Screen(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
        super(type, pos, state);
        this.tier = tier;
        // Enable redstone functionality.
        redstoneAwareState().isOutputEnabled = true;
        screens.add(this);
        setColor(Color.rgbValues.get(Color.byTier[tier]));
    }

    public Screen(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 0);
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public Direction[] validFacings() {
        return Direction.values();
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return side != facing();
    }

    // Allow connections from front for keyboards, and keyboards only...
    @Override
    public Node sidedNode(Direction side) {
        if (side != facing()) return node();
        final Level level = getLevel();
        final BlockPos neighbor = getBlockPos().relative(side);
        return level.isLoaded(neighbor) && level.getBlockEntity(neighbor) instanceof Keyboard ? node() : null;
    }

    // ----------------------------------------------------------------------- //

    public boolean isOrigin() {
        return origin == this;
    }

    public Pair<Integer, Integer> localPosition() {
        final BlockPosition lpos = project(this);
        final BlockPosition opos = project(origin);
        return Pair.of(lpos.x - opos.x, lpos.y - opos.y);
    }

    public boolean hasKeyboard() {
        final Level level = getLevel();
        for (Screen screen : new ArrayList<>(screens)) {
            for (Direction side : Direction.values()) {
                final BlockPosition blockPos = BlockPosition.apply(screen).offset(side);
                final BlockEntity tileEntity = ExtendedWorld.blockExists(level, blockPos) ? level.getBlockEntity(blockPos.toBlockPos()) : null;
                if (tileEntity instanceof Keyboard keyboard && keyboard.hasNodeOnSide(side.getOpposite())) return true;
            }
        }
        return false;
    }

    public void checkMultiBlock() {
        shouldCheckForMultiBlock = true;
        width = 1;
        height = 1;
        origin = this;
        screens.clear();
        screens.add(this);
        cachedBounds = Optional.empty();
        invertTouchMode = false;
    }

    public Pair<Boolean, Optional<Pair<Double, Double>>> toScreenCoordinates(double hitX, double hitY, double hitZ) {
        // Compute absolute position of the click on the face, measured in blocks.
        final Direction east = toGlobal(Direction.EAST);
        final Direction up = toGlobal(Direction.UP);
        final double hx = east.getStepX() * hitX + east.getStepY() * hitY + east.getStepZ() * hitZ;
        final double hy = up.getStepX() * hitX + up.getStepY() * hitY + up.getStepZ() * hitZ;
        final double tx = hx < 0 ? 1 + hx : hx;
        final double ty = 1 - (hy < 0 ? 1 + hy : hy);
        final Pair<Integer, Integer> local = localPosition();
        final double ax = local.getLeft() + tx;
        final double ay = height - 1 - local.getRight() + ty;

        // Get the relative position in the *display area* of the face.
        final double border = 2.25 / 16.0;
        if (ax <= border || ay <= border || ax >= width - border || ay >= height - border) {
            return Pair.of(false, Optional.empty());
        }
        if (!getLevel().isClientSide) return Pair.of(true, Optional.empty());

        final double iw = width - border * 2;
        final double ih = height - border * 2;
        final double rx = (ax - border) / iw;
        final double ry = (ay - border) / ih;

        // Make it a relative position in the displayed buffer.
        final li.cil.oc.api.internal.TextBuffer buffer = origin.buffer();
        final int bw = buffer.getViewportWidth();
        final int bh = buffer.getViewportHeight();
        final double bpw = buffer.renderWidth() / iw;
        final double bph = buffer.renderHeight() / ih;
        final double brx;
        final double bry;
        if (bpw > bph) {
            final double rh = bph / bpw;
            brx = rx;
            bry = (ry - (1 - rh) * 0.5) / rh;
        } else if (bph > bpw) {
            final double rw = bpw / bph;
            brx = (rx - (1 - rw) * 0.5) / rw;
            bry = ry;
        } else {
            brx = rx;
            bry = ry;
        }

        final boolean inBounds = bry >= 0 && bry <= 1 && brx >= 0 || brx <= 1;
        return Pair.of(inBounds, Optional.of(Pair.of(brx * bw, bry * bh)));
    }

    public boolean copyToAnalyzer(double hitX, double hitY, double hitZ) {
        final Pair<Boolean, Optional<Pair<Double, Double>>> result = toScreenCoordinates(hitX, hitY, hitZ);
        if (result.getRight().isPresent()) {
            if (origin.buffer() instanceof li.cil.oc.common.component.TextBuffer buffer) {
                buffer.copyToAnalyzer((int) (double) result.getRight().get().getRight(), null);
                return true;
            }
            return false;
        }
        return result.getLeft();
    }

    public boolean click(double hitX, double hitY, double hitZ) {
        final Pair<Boolean, Optional<Pair<Double, Double>>> result = toScreenCoordinates(hitX, hitY, hitZ);
        if (result.getRight().isPresent()) {
            // Send the packet to the server (manually, for accuracy).
            origin.buffer().mouseDown(result.getRight().get().getLeft(), result.getRight().get().getRight(), 0, null);
            return true;
        }
        return result.getLeft();
    }

    public void walk(Entity entity) {
        final Pair<Integer, Integer> local = localPosition();
        final int x = local.getLeft();
        final int y = local.getRight();
        final Pair<Integer, Integer> old = origin.lastWalked.put(entity, local);
        if (old != null && old.getLeft() == x && old.getRight() == y) {
            return; // Ignore
        }
        if (entity instanceof Player player && Settings.get().inputUsername) {
            origin.node().sendToReachable("computer.signal", "walk", x + 1, height - y, player.getName().getString());
        } else {
            origin.node().sendToReachable("computer.signal", "walk", x + 1, height - y);
        }
    }

    public void shot(Arrow arrow) {
        arrows.add(arrow);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        final Level level = getLevel();
        if (shouldCheckForMultiBlock && ((isClient() && isClientReadyForMultiBlockCheck()) || (isServer() && isConnected()))) {
            // Make sure we merge in a deterministic order, to avoid getting
            // different results on server and client due to the update order
            // differing between the two. This also saves us from having to save
            // any multi-block specific state information.
            final TreeSet<Screen> pending = new TreeSet<>();
            pending.add(this);
            final ArrayDeque<Screen> queue = new ArrayDeque<>();
            queue.add(this);
            while (!queue.isEmpty()) {
                final Screen current = queue.poll();
                final BlockPosition lpos = project(current);
                final int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                for (int[] offset : offsets) {
                    final BlockPosition npos = unproject(lpos.x + offset[0], lpos.y + offset[1], lpos.z);
                    if (ExtendedWorld.blockExists(level, npos) && level.getBlockEntity(npos.toBlockPos()) instanceof Screen s
                        && s.pitch() == pitch() && s.yaw() == yaw() && pending.add(s)) {
                        queue.add(s);
                    }
                }
            }
            // Perform actual merges.
            while (!pending.isEmpty()) {
                final Screen current = pending.first();
                while (current.tryMerge()) {
                }
                for (Screen screen : new ArrayList<>(current.screens)) {
                    screen.shouldCheckForMultiBlock = false;
                    pending.remove(screen);
                    queue.add(screen);
                }
            }
            if (isClient()) li.cil.oc.client.ClientHooks.updateMergedScreenModels(this);
            // Update visibility after everything is done, to avoid noise.
            for (Screen screen : queue) {
                final li.cil.oc.api.internal.TextBuffer buffer = screen.buffer();
                if (screen.isOrigin()) {
                    if (isServer()) {
                        ((Component) buffer.node()).setVisibility(Visibility.Network);
                        buffer.setEnergyCostPerTick(Settings.get().screenCost * screen.width * screen.height);
                        buffer.setAspectRatio(screen.width, screen.height);
                    }
                } else {
                    if (isServer()) {
                        ((Component) buffer.node()).setVisibility(Visibility.None);
                        buffer.setEnergyCostPerTick(Settings.get().screenCost);
                    }
                    buffer.setAspectRatio(1, 1);
                    final int w = buffer.getWidth();
                    final int h = buffer.getHeight();
                    buffer.setForegroundColor(0xFFFFFF, false);
                    buffer.setBackgroundColor(0x000000, false);
                    buffer.fill(0, 0, w, h, ' ');
                }
            }
        }
        if (!arrows.isEmpty()) {
            for (Arrow arrow : arrows) {
                final double hitX = arrow.getX() - x();
                final double hitY = arrow.getY() - y();
                final double hitZ = arrow.getZ() - z();
                if (isClient() && li.cil.oc.client.ClientHooks.isLocalPlayer(arrow.getOwner())) {
                    click(hitX, hitY, hitZ);
                }
            }
            arrows.clear();
        }
    }

    private boolean isClientReadyForMultiBlockCheck() {
        if (delayUntilCheckForMultiBlock > 0) {
            delayUntilCheckForMultiBlock -= 1;
            return false;
        } else return true;
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Screen screen : new ArrayList<>(screens)) screen.checkMultiBlock();
        if (isClient()) {
            li.cil.oc.client.ClientHooks.closeScreenGuiFor(buffer());
        }
    }

    @Override
    public void onColorChanged() {
        Colored.super.onColorChanged();
        for (Screen screen : new ArrayList<>(screens)) screen.checkMultiBlock();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        tier = Math.min(Math.max(nbt.getByte(TierTag), 0), 2);
        setColor(Color.rgbValues.get(Color.byTier[tier]));
        super.loadForServer(nbt);
        hadRedstoneInput = nbt.getBoolean(HadRedstoneInputTag);
        invertTouchMode = nbt.getBoolean(InvertTouchModeTag);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        nbt.putByte(TierTag, (byte) tier);
        super.saveForServer(nbt);
        nbt.putBoolean(HadRedstoneInputTag, hadRedstoneInput);
        nbt.putBoolean(InvertTouchModeTag, invertTouchMode);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        tier = Math.min(Math.max(nbt.getByte(TierTag), 0), 2);
        super.loadForClient(nbt);
        invertTouchMode = nbt.getBoolean(InvertTouchModeTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        nbt.putByte(TierTag, (byte) tier);
        super.saveForClient(nbt);
        nbt.putBoolean(InvertTouchModeTag, invertTouchMode);
    }

    // ----------------------------------------------------------------------- //

    /**
     * Client side only. Was a TileEntity override in 1.16; on Forge this method overrides
     * {@code IForgeBlockEntity#getRenderBoundingBox} by signature, elsewhere renderers may call it.
     */
    public AABB getRenderBoundingBox() {
        if ((width == 1 && height == 1) || !isOrigin()) return new AABB(getBlockPos());
        if (cachedBounds.isPresent()) return cachedBounds.get();
        final BlockPosition spos = unproject(width, height, 1);
        final int ox = x() + (spos.x < 0 ? 1 : 0);
        final int oy = y() + (spos.y < 0 ? 1 : 0);
        final int oz = z() + (spos.z < 0 ? 1 : 0);
        final AABB btmp = new AABB(ox, oy, oz, ox + spos.x, oy + spos.y, oz + spos.z);
        final AABB b = new AABB(
            Math.min(btmp.minX, btmp.maxX), Math.min(btmp.minY, btmp.maxY), Math.min(btmp.minZ, btmp.maxZ),
            Math.max(btmp.minX, btmp.maxX), Math.max(btmp.minY, btmp.maxY), Math.max(btmp.minZ, btmp.maxZ));
        cachedBounds = Optional.of(b);
        return b;
    }

    /** Client side only: maximum render distance for the block entity renderer (was a TileEntity override). */
    public double getViewDistance() {
        return isOrigin() ? 64.0 : 0;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{origin.node()};
    }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        RedstoneAware.super.onRedstoneInputChanged(args);
        int maxInput = 0;
        for (Screen screen : new ArrayList<>(screens)) maxInput = Math.max(maxInput, screen.maxInput());
        final boolean hasRedstoneInput = maxInput > 0;
        if (hasRedstoneInput != hadRedstoneInput) {
            hadRedstoneInput = hasRedstoneInput;
            if (hasRedstoneInput) {
                origin.buffer().setPowerState(!origin.buffer().getPowerState());
            }
        }
    }

    @Override
    public void onRotationChanged() {
        Rotatable.super.onRotationChanged();
        for (Screen screen : new ArrayList<>(screens)) screen.checkMultiBlock();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int compareTo(Screen that) {
        if (x() != that.x()) return x() - that.x();
        else if (y() != that.y()) return y() - that.y();
        else return z() - that.z();
    }

    // ----------------------------------------------------------------------- //

    private boolean tryMerge() {
        final BlockPosition opos = project(origin);
        return tryMergeTowards(opos, 0, height) || tryMergeTowards(opos, 0, -1) || tryMergeTowards(opos, width, 0) || tryMergeTowards(opos, -1, 0);
    }

    private boolean tryMergeTowards(BlockPosition opos, int dx, int dy) {
        final Level level = getLevel();
        final BlockPosition npos = unproject(opos.x + dx, opos.y + dy, opos.z);
        if (!ExtendedWorld.blockExists(level, npos)) return false;
        if (level.getBlockEntity(npos.toBlockPos()) instanceof Screen s && s.tier == tier && s.pitch() == pitch() && s.getColor() == getColor() && s.yaw() == yaw() && !screens.contains(s)) {
            final BlockPosition spos = project(s.origin);
            final boolean canMergeAlongX = spos.y == opos.y && s.height == height && s.width + width <= Settings.get().maxScreenWidth;
            final boolean canMergeAlongY = spos.x == opos.x && s.width == width && s.height + height <= Settings.get().maxScreenHeight;
            if (canMergeAlongX || canMergeAlongY) {
                final Screen newOrigin;
                if (canMergeAlongX) {
                    newOrigin = spos.x < opos.x ? s.origin : origin;
                } else {
                    newOrigin = spos.y < opos.y ? s.origin : origin;
                }
                final int newWidth = canMergeAlongX ? width + s.width : width;
                final int newHeight = canMergeAlongX ? height : height + s.height;
                final Set<Screen> newScreens = new HashSet<>(screens);
                newScreens.addAll(s.screens);
                for (Screen screen : newScreens) {
                    screen.width = newWidth;
                    screen.height = newHeight;
                    screen.origin = newOrigin;
                    screen.screens.addAll(newScreens); // It's a set, so there won't be duplicates.
                    screen.cachedBounds = Optional.empty();
                }
                return true;
            } else return false; // Cannot merge.
        }
        return false;
    }

    private BlockPosition project(Screen t) {
        final Direction east = toGlobal(Direction.EAST);
        final Direction up = toGlobal(Direction.UP);
        final Direction south = toGlobal(Direction.SOUTH);
        return new BlockPosition(dot(east, t), dot(up, t), dot(south, t));
    }

    private static int dot(Direction f, Screen s) {
        return f.getStepX() * s.x() + f.getStepY() * s.y() + f.getStepZ() * s.z();
    }

    private BlockPosition unproject(int x, int y, int z) {
        final Direction east = toLocal(Direction.EAST);
        final Direction up = toLocal(Direction.UP);
        final Direction south = toLocal(Direction.SOUTH);
        return new BlockPosition(
            east.getStepX() * x + east.getStepY() * y + east.getStepZ() * z,
            up.getStepX() * x + up.getStepY() * y + up.getStepZ() * z,
            south.getStepX() * x + south.getStepY() * y + south.getStepZ() * z);
    }
}
