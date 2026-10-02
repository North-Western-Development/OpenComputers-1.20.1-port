package li.cil.oc.common.entity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.Machine;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.ComponentInventory;
import li.cil.oc.common.inventory.Inventory;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// internal.Rotatable is also in internal.Drone, but it wasn't since the start
// so this is to ensure it is implemented here, in the very unlikely case that
// someone decides to ship that specific version of the API.
public class Drone extends Entity implements MachineHost, li.cil.oc.api.internal.Drone, li.cil.oc.api.internal.Rotatable, Analyzable, Context {
    public static final EntityDataAccessor<Boolean> DataRunning = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DataTargetX = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DataTargetY = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DataTargetZ = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DataMaxAcceleration = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DataSelectedSlot = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DataCurrentEnergy = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DataMaxEnergy = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> DataStatusText = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> DataInventorySize = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DataLightColor = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);

    // Some basic constants.
    public final float gravity = 0.05f;
    // low for slow fall (float down)
    public final float drag = 0.8f;
    public final float maxAcceleration = 0.1f;
    public final float maxVelocity = 0.4f;
    public final int maxInventorySize = 8;

    // Rendering stuff, purely eyecandy.
    public final float[][] targetFlapAngles = new float[4][2];
    public final float[][] flapAngles = new float[4][2];
    public int nextFlapChange = 0;
    public float bodyAngle = (float) Math.random() * 90;
    public float angularVelocity = 0f;
    public int nextAngularVelocityChange = 0;
    public int lastEnergyUpdate = 0;

    // Logic stuff, components, machine and such.
    public final DroneData info = new DroneData();
    public final li.cil.oc.api.machine.Machine machine;
    public final li.cil.oc.server.component.Drone control;
    public final ComponentInventory components;
    public final Inventory equipmentInventory;
    public final DroneInventory mainInventory;
    public final MultiTank tank;
    public int selectedTank = 0;

    public String ownerName = Settings.get().fakePlayerName;

    public UUID ownerUUID = Settings.get().fakePlayerProfile.getId();

    private li.cil.oc.server.agent.Player player_;

    private boolean isChangingDimension = false;
    private boolean disposed = false;

    // Not implemented in Drone itself because spectators would open this via vanilla Player.openMenu (without extra data).
    public final MenuProvider containerProvider = new MenuProvider() {
        @Override
        public Component getDisplayName() {
            return Component.empty();
        }

        @Override
        public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
            return new li.cil.oc.common.container.Drone(ContainerTypes.DRONE.get(), id, playerInventory, mainInventory, mainInventory.getContainerSize());
        }
    };

    public Drone(EntityType<? extends Drone> selfType, Level world) {
        super(selfType, world);
        if (!world.isClientSide) {
            li.cil.oc.api.machine.Machine m = Machine.create(this);
            ((Connector) m.node()).setLocalBufferSize(0);
            machine = m;
            control = new li.cil.oc.server.component.Drone(this);
        }
        else {
            machine = null;
            control = null;
        }

        components = new ComponentInventory() {
            @Override
            public EnvironmentHost host() {
                return Drone.this;
            }

            @Override
            public ItemStack[] items() {
                return info.components;
            }

            @Override
            public int getContainerSize() {
                return info.components.length;
            }

            @Override
            public void setChanged() {
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return true;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }

            @Override
            public Node node() {
                return machine != null ? machine.node() : null;
            }

            @Override
            public void onConnect(Node node) {
            }

            @Override
            public void onDisconnect(Node node) {
            }

            @Override
            public void onMessage(Message message) {
            }
        };

        equipmentInventory = new Inventory() {
            private final ItemStack[] items = new ItemStack[0];

            @Override
            public ItemStack[] items() {
                return items;
            }

            @Override
            public int getContainerSize() {
                return 0;
            }

            @Override
            public int getMaxStackSize() {
                return 0;
            }

            @Override
            public void setChanged() {
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return false;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };

        mainInventory = new DroneInventory(this) {
            private final ItemStack[] items = createItems();

            private ItemStack[] createItems() {
                ItemStack[] result = new ItemStack[8];
                Arrays.fill(result, ItemStack.EMPTY);
                return result;
            }

            @Override
            public ItemStack[] items() {
                return items;
            }

            @Override
            public int getContainerSize() {
                return inventorySize();
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }

            @Override
            public void setChanged() {
                // TODO update client GUI?
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot >= 0 && slot < getContainerSize();
            }

            @Override
            public boolean stillValid(Player player) {
                return player.distanceToSqr(drone) < 64;
            }
        };

        tank = new MultiTank() {
            private List<FluidHandler> tanks() {
                List<FluidHandler> result = new ArrayList<>();
                for (Optional<ManagedEnvironment> component : components.components()) {
                    if (component.isPresent() && component.get() instanceof FluidHandler tank) result.add(tank);
                }
                return result;
            }

            @Override
            public int tankCount() {
                return tanks().size();
            }

            @Override
            public FluidHandler getFluidTank(int index) {
                return tanks().get(index);
            }
        };
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Level world() {
        return level();
    }

    @Override
    public li.cil.oc.api.machine.Machine machine() {
        return machine;
    }

    @Override
    public Inventory equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public DroneInventory mainInventory() {
        return mainInventory;
    }

    @Override
    public MultiTank tank() {
        return tank;
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public void setSelectedTank(int index) {
        selectedTank = index;
    }

    @Override
    public int tier() {
        return info.tier;
    }

    @Override
    public Player player() {
        li.cil.oc.server.agent.Player player = fakePlayer();
        li.cil.oc.server.agent.Player.updatePositionAndRotation(player, facing(), facing());
        li.cil.oc.server.agent.Player.setPlayerInventoryItems(player);
        return player;
    }

    private li.cil.oc.server.agent.Player fakePlayer() {
        if (player_ == null) player_ = new li.cil.oc.server.agent.Player(this);
        return player_;
    }

    @Override
    public String name() {
        return info.name;
    }

    @Override
    public void setName(String name) {
        info.name = name;
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
    }

    // ----------------------------------------------------------------------- //
    // Forward context stuff to our machine. Interface needed for some components
    // to work correctly (such as the chunkloader upgrade).

    @Override
    public Node node() {
        return machine.node();
    }

    @Override
    public boolean canInteract(String player) {
        return machine.canInteract(player);
    }

    @Override
    public boolean isPaused() {
        return machine.isPaused();
    }

    @Override
    public boolean start() {
        if (level().isClientSide || machine.isRunning()) {
            return false;
        }
        preparePowerUp();
        return machine.start();
    }

    @Override
    public boolean pause(double seconds) {
        return machine.pause(seconds);
    }

    @Override
    public boolean stop() {
        return machine.stop();
    }

    @Override
    public void consumeCallBudget(double callCost) {
        machine.consumeCallBudget(callCost);
    }

    @Override
    public boolean signal(String name, Object... args) {
        return machine.signal(name, args);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Vec3 getTarget() {
        return new Vec3(targetX(), targetY(), targetZ());
    }

    @Override
    public void setTarget(Vec3 value) {
        setTargetX((float) value.x);
        setTargetY((float) value.y);
        setTargetZ((float) value.z);
    }

    @Override
    public Vec3 getVelocity() {
        return getDeltaMovement();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double xPosition() {
        return getX();
    }

    @Override
    public double yPosition() {
        return getY();
    }

    @Override
    public double zPosition() {
        return getZ();
    }

    @Override
    public void markChanged() {
    }

    @Override
    public Vec3 getRopeHoldPosition(float dt) {
        return getPosition(dt).add(0.0, -0.056, 0.0); // Offset: height * 0.85 * 0.7 - 0.25
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Direction facing() {
        return Direction.SOUTH;
    }

    @Override
    public Direction toLocal(Direction value) {
        return value;
    }

    @Override
    public Direction toGlobal(Direction value) {
        return value;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{machine.node()};
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Iterable<ItemStack> internalComponents() {
        return Arrays.asList(info.components);
    }

    @Override
    public int componentSlot(String address) {
        Optional<ManagedEnvironment>[] list = components.components();
        for (int i = 0; i < list.length; i++) {
            if (list[i].isPresent()) {
                ManagedEnvironment env = list[i].get();
                if (env.node() != null && address.equals(env.node().address())) return i;
            }
        }
        return -1;
    }

    @Override
    public void onMachineConnect(Node node) {
    }

    @Override
    public void onMachineDisconnect(Node node) {
    }

    public int computeInventorySize() {
        int acc = 0;
        for (ItemStack stack : info.components) {
            if (stack != null && !stack.isEmpty()) {
                DriverItem driver = Driver.driverFor(stack, getClass());
                if (driver instanceof li.cil.oc.api.driver.item.Inventory inventory) {
                    acc += Math.max(1, inventory.inventoryCapacity(stack) / 4);
                }
            }
        }
        return Math.min(maxInventorySize, acc);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void defineSynchedData() {
        entityData.define(DataRunning, false);
        entityData.define(DataTargetX, 0f);
        entityData.define(DataTargetY, 0f);
        entityData.define(DataTargetZ, 0f);
        entityData.define(DataMaxAcceleration, 0f);
        entityData.define(DataSelectedSlot, 0);
        entityData.define(DataCurrentEnergy, 0);
        entityData.define(DataMaxEnergy, 100);
        entityData.define(DataStatusText, "");
        entityData.define(DataInventorySize, 0);
        entityData.define(DataLightColor, 0x66DD55);
    }

    public void initializeAfterPlacement(ItemStack stack, Player player, Vec3 position) {
        info.loadData(stack);
        Connector controlNode = control.node();
        controlNode.changeBuffer(info.storedEnergy - controlNode.localBuffer());
        wireThingsTogether();
        setInventorySize(computeInventorySize());
        setPos(position.x, position.y, position.z);
    }

    public void preparePowerUp() {
        setTargetX((float) Math.floor(getX()) + 0.5f);
        setTargetY((float) Math.round(getY()) + 0.5f);
        setTargetZ((float) Math.floor(getZ()) + 0.5f);
        setTargetAcceleration(maxAcceleration);

        wireThingsTogether();
    }

    private void wireThingsTogether() {
        Network.joinNewNetwork(machine.node());
        machine.node().connect(control.node());
        machine.setCostPerTick(Settings.get().droneCost);
        components.connectComponents();
    }

    @Override
    public boolean isRunning() {
        return entityData.get(DataRunning);
    }

    public float targetX() {
        return entityData.get(DataTargetX);
    }

    public float targetY() {
        return entityData.get(DataTargetY);
    }

    public float targetZ() {
        return entityData.get(DataTargetZ);
    }

    public float targetAcceleration() {
        return entityData.get(DataMaxAcceleration);
    }

    @Override
    public int selectedSlot() {
        return entityData.get(DataSelectedSlot) & 0xFF;
    }

    public int globalBuffer() {
        return entityData.get(DataCurrentEnergy);
    }

    public int globalBufferSize() {
        return entityData.get(DataMaxEnergy);
    }

    public String statusText() {
        return entityData.get(DataStatusText);
    }

    public int inventorySize() {
        return entityData.get(DataInventorySize) & 0xFF;
    }

    public int lightColor() {
        return entityData.get(DataLightColor);
    }

    public void setRunning(boolean value) {
        entityData.set(DataRunning, value);
    }

    // Round target values to low accuracy to avoid floating point errors accumulating.
    public void setTargetX(float value) {
        entityData.set(DataTargetX, Math.round(value * 4) / 4f);
    }

    public void setTargetY(float value) {
        entityData.set(DataTargetY, Math.round(value * 4) / 4f);
    }

    public void setTargetZ(float value) {
        entityData.set(DataTargetZ, Math.round(value * 4) / 4f);
    }

    public void setTargetAcceleration(float value) {
        entityData.set(DataMaxAcceleration, Math.max(0, Math.min(maxAcceleration, value)));
    }

    @Override
    public void setSelectedSlot(int value) {
        entityData.set(DataSelectedSlot, (int) (byte) value);
    }

    public void setGlobalBuffer(int value) {
        entityData.set(DataCurrentEnergy, value);
    }

    public void setGlobalBufferSize(int value) {
        entityData.set(DataMaxEnergy, value);
    }

    public void setStatusText(@Nullable String value) {
        String text = "";
        if (value != null) {
            StringBuilder sb = new StringBuilder();
            String[] lines = value.lines().limit(2).toArray(String[]::new);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) sb.append('\n');
                sb.append(lines[i], 0, Math.min(10, lines[i].length()));
            }
            text = sb.toString();
        }
        entityData.set(DataStatusText, text);
    }

    public void setInventorySize(int value) {
        entityData.set(DataInventorySize, (int) (byte) value);
    }

    public void setLightColor(int value) {
        entityData.set(DataLightColor, value);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        // Only set exact position if we're too far away from the server's
        // position, otherwise keep interpolating. This removes jitter and
        // is good enough for drones.
        if (!isRunning() || distanceToSqr(x, y, z) > 1) {
            super.absMoveTo(x, y, z, yaw, pitch);
        }
        else {
            setTargetX((float) x);
            setTargetY((float) y);
            setTargetZ((float) z);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (isInWater() || isInLava()) {
                // We're not water-proof!
                machine.stop();
            }
            machine.update();
            components.updateComponents();
            setRunning(machine.isRunning());

            Connector node = (Connector) machine.node();
            int buffer = (int) Math.round(node.globalBuffer());
            if (Math.abs(lastEnergyUpdate - buffer) > 1 || level().getGameTime() % 200 == 0) {
                lastEnergyUpdate = buffer;
                setGlobalBuffer(buffer);
                setGlobalBufferSize((int) node.globalBufferSize());
            }
        }
        else {
            if (isRunning()) {
                // Client side update; occasionally update wing pitch and rotation to
                // make the drones look a bit more dynamic.
                RandomSource rng = level().random;
                nextFlapChange -= 1;
                nextAngularVelocityChange -= 1;

                if (nextFlapChange < 0) {
                    nextFlapChange = 5 + rng.nextInt(10);
                    for (int i = 0; i < 2; i++) {
                        int flap = rng.nextInt(targetFlapAngles.length);
                        targetFlapAngles[flap][0] = (float) Math.toRadians(rng.nextFloat() * 4 - 2);
                        targetFlapAngles[flap][1] = (float) Math.toRadians(rng.nextFloat() * 4 - 2);
                    }
                }

                if (nextAngularVelocityChange < 0) {
                    if (angularVelocity != 0) {
                        angularVelocity = 0;
                        nextAngularVelocityChange = 20;
                    }
                    else {
                        angularVelocity = rng.nextBoolean() ? 0.1f : -0.1f;
                        nextAngularVelocityChange = 100;
                    }
                }

                // Interpolate wing rotations.
                for (int i = 0; i < flapAngles.length; i++) {
                    float[] f = flapAngles[i];
                    float[] t = targetFlapAngles[i];
                    f[0] = f[0] * 0.7f + t[0] * 0.3f;
                    f[1] = f[1] * 0.7f + t[1] * 0.3f;
                }

                // Update body rotation.
                bodyAngle += angularVelocity;
            }
        }

        xo = getX();
        yo = getY();
        zo = getZ();
        noPhysics = !level().noCollision(this);
        if (noPhysics) moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2, getZ());

        if (isRunning()) {
            Vec3 toTarget = new Vec3(targetX() - getX(), targetY() - getY(), targetZ() - getZ());
            double distance = toTarget.length();
            Vec3 velocity = getDeltaMovement();
            if (distance > 0 && (distance > 0.005f || velocity.dot(velocity) > 0.005f)) {
                double acceleration = Math.min(targetAcceleration(), distance) / distance;
                double velocityX = velocity.x + toTarget.x * acceleration;
                double velocityY = velocity.y + toTarget.y * acceleration;
                double velocityZ = velocity.z + toTarget.z * acceleration;
                setDeltaMovement(new Vec3(Math.max(-maxVelocity, Math.min(maxVelocity, velocityX)),
                    Math.max(-maxVelocity, Math.min(maxVelocity, velocityY)),
                    Math.max(-maxVelocity, Math.min(maxVelocity, velocityZ))));
            }
            else {
                setDeltaMovement(Vec3.ZERO);
                setPos(targetX(), targetY(), targetZ());
            }
        }
        else {
            // No power, free fall: engage!
            setDeltaMovement(getDeltaMovement().subtract(0, gravity, 0));
        }

        move(MoverType.SELF, getDeltaMovement());

        // Make sure we don't get infinitely faster.
        if (isRunning()) {
            setDeltaMovement(getDeltaMovement().scale(drag));
        }
        else {
            BlockPos below = blockPosition().below();
            float groundDrag = level().getBlockState(below).getBlock().getFriction() * drag;
            setDeltaMovement(getDeltaMovement().multiply(groundDrag, drag * (onGround() ? -0.5 : 1), groundDrag));
        }
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        if (isRunning()) {
            Vec3 direction = new Vec3(entity.getX() - getX(), entity.getY() + entity.getEyeHeight() - getY(), entity.getZ() - getZ()).normalize();
            if (!level().isClientSide) {
                if (Settings.get().inputUsername)
                    machine.signal("hit", direction.x, direction.z, direction.y, entity.getName().getString());
                else
                    machine.signal("hit", direction.x, direction.z, direction.y);
            }
            setDeltaMovement(getDeltaMovement().subtract(direction).scale(0.5));
        }
        return super.skipAttackInteraction(entity);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!isAlive()) return InteractionResult.PASS;
        if (player.isCrouching()) {
            if (Wrench.isWrench(player.getItemInHand(InteractionHand.MAIN_HAND))) {
                if (!level().isClientSide) {
                    onBelowWorld();
                }
            }
            else if (!level().isClientSide && !machine.isRunning()) {
                start();
            }
        }
        else if (player instanceof ServerPlayer srvPlr && !level().isClientSide) {
            ContainerTypes.openDroneGui(srvPlr, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // No step sounds. Except on that one day.
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (EventHandler.isItTime()) super.playStepSound(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Nullable
    @Override
    public Entity changeDimension(ServerLevel dimension) {
        // Store relative target as target, to allow adding that in our "new self"
        // (entities get re-created after changing dimension).
        setTargetX((float) (targetX() - getX()));
        setTargetY((float) (targetY() - getY()));
        setTargetZ((float) (targetZ() - getZ()));
        try {
            isChangingDimension = true;
            return super.changeDimension(dimension);
        }
        finally {
            isChangingDimension = false;
            // Again, to actually close old machine state after copying it.
            if (isRemoved()) dispose();
        }
    }

    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        // Compute relative target based on old position and update, because our
        // frame of reference most certainly changed (i.e. we'll spawn at different
        // coordinates than the ones we started traveling from, e.g. when porting
        // to the nether it'll be oldpos / 8).
        if (entity instanceof Drone drone) {
            setTargetX((float) (getX() + drone.targetX()));
            setTargetY((float) (getY() + drone.targetY()));
            setTargetZ((float) (getZ() + drone.targetZ()));
        }
        else {
            setTargetX((float) getX());
            setTargetY((float) getY());
            setTargetZ((float) getZ());
        }
    }

    /**
     * Replaces the Scala {@code remove()} override: vanilla {@code setRemoved} is final, so we hook the
     * level callback, which is notified of every removal (kill, discard, dimension change, unload).
     */
    @Override
    public void setLevelCallback(EntityInLevelCallback callback) {
        if (callback == EntityInLevelCallback.NULL) {
            super.setLevelCallback(callback);
            return;
        }
        super.setLevelCallback(new EntityInLevelCallback() {
            @Override
            public void onMove() {
                callback.onMove();
            }

            @Override
            public void onRemove(RemovalReason reason) {
                callback.onRemove(reason);
                onRemoved(reason);
            }
        });
    }

    protected void onRemoved(RemovalReason reason) {
        if (level().isClientSide || isChangingDimension) return;
        if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
            // State was saved with the chunk; just make sure the machine gets closed.
            EventHandler.scheduleClose(machine);
        }
        else {
            dispose();
        }
    }

    private void dispose() {
        if (level().isClientSide || disposed) return;
        disposed = true;
        machine.stop();
        machine.node().remove();
        components.disconnectComponents();
        components.saveComponents();
    }

    @Override
    protected void onBelowWorld() {
        if (!isAlive()) return;
        super.onBelowWorld();
        if (!level().isClientSide) {
            ItemStack stack = Items.get(Constants.ItemName.Drone).createItemStack(1);
            info.storedEnergy = (int) control.node().localBuffer();
            info.saveData(stack);
            ItemEntity entity = new ItemEntity(level(), getX(), getY(), getZ(), stack);
            entity.setPickUpDelay(15);
            level().addFreshEntity(entity);
            InventoryUtils.dropAllSlots(BlockPosition.apply((Entity) this), mainInventory);
        }
    }

    // Note: the default ClientboundAddEntityPacket works for modded entity types on both loaders;
    // the drone needs no extra spawn data (entity data is synced via SynchedEntityData).

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        info.loadData(nbt.getCompound("info"));
        setInventorySize(computeInventorySize());
        if (!level().isClientSide) {
            machine.loadData(nbt.getCompound("machine"));
            control.loadData(nbt.getCompound("control"));
            components.loadData(nbt.getCompound("components"));
            mainInventory.loadData(nbt.getCompound("inventory"));

            wireThingsTogether();
        }
        setTargetX(nbt.getFloat("targetX"));
        setTargetY(nbt.getFloat("targetY"));
        setTargetZ(nbt.getFloat("targetZ"));
        setTargetAcceleration(nbt.getFloat("targetAcceleration"));
        setSelectedSlot(nbt.getByte("selectedSlot") & 0xFF);
        setSelectedTank(nbt.getByte("selectedTank") & 0xFF);
        setStatusText(nbt.getString("statusText"));
        setLightColor(nbt.getInt("lightColor"));
        if (nbt.contains("owner")) {
            ownerName = nbt.getString("owner");
        }
        if (nbt.contains("ownerUuid")) {
            ownerUUID = UUID.fromString(nbt.getString("ownerUuid"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if (level().isClientSide) return;
        components.saveComponents();
        info.storedEnergy = globalBuffer();
        ExtendedNBT.setNewCompoundTag(nbt, "info", info::saveData);
        ExtendedNBT.setNewCompoundTag(nbt, "machine", machine::saveData);
        ExtendedNBT.setNewCompoundTag(nbt, "control", control::saveData);
        ExtendedNBT.setNewCompoundTag(nbt, "components", components::saveData);
        ExtendedNBT.setNewCompoundTag(nbt, "inventory", mainInventory::saveData);
        nbt.putFloat("targetX", targetX());
        nbt.putFloat("targetY", targetY());
        nbt.putFloat("targetZ", targetZ());
        nbt.putFloat("targetAcceleration", targetAcceleration());
        nbt.putByte("selectedSlot", (byte) selectedSlot());
        nbt.putByte("selectedTank", (byte) selectedTank);
        nbt.putString("statusText", statusText());
        nbt.putInt("lightColor", lightColor());
        nbt.putString("owner", ownerName);
        nbt.putString("ownerUuid", ownerUUID.toString());
    }
}
