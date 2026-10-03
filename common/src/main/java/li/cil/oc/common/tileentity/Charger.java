package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.tileentity.traits.ComponentInventory;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.StateAware;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.integration.util.ItemCharge;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Charger extends TileEntity implements Environment, PowerAcceptor, RedstoneAware, Rotatable, ComponentInventory, Tickable, Analyzable, StateAware, DeviceInfo, MenuProvider {
    public final Connector node = li.cil.oc.api.Network.newNode(this, Visibility.None).
        withConnector(Settings.get().bufferConverter).
        create();

    public final Set<Chargeable> connectors = Collections.synchronizedSet(new LinkedHashSet<>());
    public final Set<ItemStack> equipment = Collections.synchronizedSet(Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    public double chargeSpeed = 0.0;

    public boolean hasPower = false;

    public boolean invertSignal = false;

    private Map<String, String> deviceInfo;

    private static final String ChargeSpeedTag = Settings.namespace + "chargeSpeed";
    private static final String ChargeSpeedTagCompat = "chargeSpeed";
    private static final String HasPowerTag = Settings.namespace + "hasPower";
    private static final String HasPowerTagCompat = "hasPower";
    private static final String InvertSignalTag = Settings.namespace + "invertSignal";
    private static final String InvertSignalTagCompat = "invertSignal";

    public Charger(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Charger",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "PowerUpper"
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(side != facing() ? node : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().chargerRate;
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        // TODO Refine to only report working if present robots/drones actually *need* power.
        if (!connectors.isEmpty()) {
            if (hasPower) return EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
            else return EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        } else return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        player.sendSystemMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed));
        return null;
    }

    // ----------------------------------------------------------------------- //

    private void chargeStack(ItemStack stack, double charge) {
        if (!stack.isEmpty() && charge > 0) {
            final double missing = node.changeBuffer(-charge);
            final double surplus = ItemCharge.charge(stack, charge + missing); // missing is negative
            node.changeBuffer(surplus);
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        final Level level = getLevel();

        // Offset by hashcode to avoid all chargers ticking at the same time.
        if ((level.getLevelData().getGameTime() + Math.abs(hashCode())) % 20 == 0) {
            updateConnectors();
        }

        if (isServer() && level.getLevelData().getGameTime() % Settings.get().tickFrequency == 0) {
            boolean canCharge = Settings.get().ignorePower;

            // Charging of external devices.
            {
                final double charge = Settings.get().chargeRateExternal * chargeSpeed * Settings.get().tickFrequency;
                canCharge = canCharge || (charge > 0 && node.globalBuffer() >= charge * 0.5);
                if (canCharge) {
                    for (Chargeable connector : snapshot(connectors)) {
                        final double missing = node.changeBuffer(-charge);
                        final double surplus = connector.changeBuffer(charge + missing); // missing is negative
                        node.changeBuffer(surplus);
                    }
                }
            }

            // Charging of internal devices.
            {
                final double charge = Settings.get().chargeRateTablet * chargeSpeed * Settings.get().tickFrequency;
                canCharge = canCharge || (charge > 0 && node.globalBuffer() >= charge * 0.5);
                if (canCharge) {
                    for (int slot = 0; slot < getContainerSize(); slot++) {
                        chargeStack(getItem(slot), charge);
                    }
                }
            }

            // Charging of equipment
            {
                final double charge = Settings.get().chargeRateTablet * chargeSpeed * Settings.get().tickFrequency;
                canCharge = canCharge || (charge > 0 && node.globalBuffer() >= charge * 0.5);
                if (canCharge) {
                    for (ItemStack stack : snapshot(equipment)) {
                        chargeStack(stack, charge);
                    }
                }
            }

            if (hasPower && !canCharge) {
                hasPower = false;
                PacketSender.sendChargerState(this);
            }
            if (!hasPower && canCharge) {
                hasPower = true;
                PacketSender.sendChargerState(this);
            }
        }

        if (isClient() && chargeSpeed > 0 && hasPower && level.getLevelData().getGameTime() % 10 == 0) {
            for (Chargeable connector : snapshot(connectors)) {
                final Vec3 position = connector.pos();
                final double theta = level.random.nextDouble() * Math.PI;
                final double phi = level.random.nextDouble() * Math.PI * 2;
                final double dx = 0.45 * Math.sin(theta) * Math.cos(phi);
                final double dy = 0.45 * Math.sin(theta) * Math.sin(phi);
                final double dz = 0.45 * Math.cos(theta);
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, position.x + dx, position.y + dz, position.z + dy, 0, 0, 0);
            }
        }
    }

    private static <T> List<T> snapshot(Set<T> set) {
        synchronized (set) {
            return new ArrayList<>(set);
        }
    }

    @Override
    public void onConnect(Node node) {
        ComponentInventory.super.onConnect(node);
        if (node == this.node) {
            onNeighborChanged();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        if (nbt.contains(ChargeSpeedTagCompat))
            chargeSpeed = Math.min(Math.max(nbt.getDouble(ChargeSpeedTagCompat), 0), 1);
        else
            chargeSpeed = Math.min(Math.max(nbt.getDouble(ChargeSpeedTag), 0), 1);
        if (nbt.contains(HasPowerTagCompat))
            hasPower = nbt.getBoolean(HasPowerTagCompat);
        else
            hasPower = nbt.getBoolean(HasPowerTag);
        if (nbt.contains(InvertSignalTagCompat))
            invertSignal = nbt.getBoolean(InvertSignalTagCompat);
        else
            invertSignal = nbt.getBoolean(InvertSignalTag);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        nbt.putDouble(ChargeSpeedTag, chargeSpeed);
        nbt.putBoolean(HasPowerTag, hasPower);
        nbt.putBoolean(InvertSignalTag, invertSignal);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        chargeSpeed = nbt.getDouble(ChargeSpeedTag);
        hasPower = nbt.getBoolean(HasPowerTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        nbt.putDouble(ChargeSpeedTag, chargeSpeed);
        nbt.putBoolean(HasPowerTag, hasPower);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isComponentSlot(int slot, ItemStack stack) {
        if (!ComponentInventory.super.isComponentSlot(slot, stack)) return false;
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.Tablet.equals(driver.slot(stack));
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (slot == 0 && driver != null && Slot.Tablet.equals(driver.slot(stack))) return true;
        return ItemCharge.canCharge(stack);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Charger(ContainerTypes.CHARGER.get(), id, playerInventory, this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateRedstoneInput(Direction side) {
        RedstoneAware.super.updateRedstoneInput(side);
        int max = 0;
        for (int value : getInput()) max = Math.max(max, value);
        final int signal = Math.min(max, 15);

        if (invertSignal) chargeSpeed = (15 - signal) / 15.0;
        else chargeSpeed = signal / 15.0;
        if (isServer()) {
            PacketSender.sendChargerState(this);
        }
    }

    public void onNeighborChanged() {
        checkRedstoneInputChanged();
        updateConnectors();
    }

    public void updateConnectors() {
        final Level level = getLevel();
        final List<Chargeable> newConnectors = new ArrayList<>();
        for (Direction side : Direction.values()) {
            final BlockPosition blockPos = BlockPosition.apply(this).offset(side);
            if (ExtendedWorld.blockExists(level, blockPos)) {
                final BlockEntity tileEntity = level.getBlockEntity(blockPos.toBlockPos());
                if (tileEntity instanceof RobotProxy proxy) {
                    newConnectors.add(new RobotChargeable(proxy.robot));
                }
            }
        }
        final AABB bounds = BlockPosition.apply(this).bounds().inflate(1, 1, 1);
        for (Drone drone : level.getEntitiesOfClass(Drone.class, bounds)) {
            newConnectors.add(new DroneChargeable(drone));
        }

        final List<Player> players = level.getEntitiesOfClass(Player.class, bounds);

        for (Player player : players) {
            if (li.cil.oc.api.Nanomachines.hasController(player)) {
                newConnectors.add(new PlayerChargeable(player));
            }
        }

        // Only update list when we have to, keeps pointless block updates to a minimum.

        synchronized (connectors) {
            final Set<Chargeable> difference = new LinkedHashSet<>(connectors);
            difference.removeAll(newConnectors);
            if (connectors.size() != newConnectors.size() || (!connectors.isEmpty() && !difference.isEmpty())) {
                connectors.clear();
                connectors.addAll(newConnectors);
                level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            }
        }

        // scan players for chargeable equipment
        synchronized (equipment) {
            equipment.clear();
            for (Player player : players) {
                // Main inventory plus worn armour (e.g. hover boots) and the offhand.
                final List<ItemStack> stacks = new java.util.ArrayList<>(player.getInventory().items);
                stacks.addAll(player.getInventory().armor);
                stacks.addAll(player.getInventory().offhand);
                for (ItemStack stack : stacks) {
                    final DriverItem driver = Driver.driverFor(stack, getClass());
                    if ((driver != null && Slot.Tablet.equals(driver.slot(stack))) || ItemCharge.canCharge(stack)) {
                        equipment.add(stack);
                    }
                }
            }
        }
    }

    public interface Chargeable {
        Vec3 pos();

        double changeBuffer(double delta);
    }

    public abstract static class ConnectorChargeable implements Chargeable {
        public final Connector connector;

        protected ConnectorChargeable(Connector connector) {
            this.connector = connector;
        }

        @Override
        public double changeBuffer(double delta) {
            return connector.changeBuffer(delta);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ConnectorChargeable chargeable && chargeable.connector == connector;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(connector);
        }
    }

    public static class RobotChargeable extends ConnectorChargeable {
        public final Robot robot;

        public RobotChargeable(Robot robot) {
            super((Connector) robot.node());
            this.robot = robot;
        }

        @Override
        public Vec3 pos() {
            return BlockPosition.apply(robot).toVec3();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RobotChargeable chargeable && chargeable.robot == robot;
        }

        @Override
        public int hashCode() {
            return robot.hashCode();
        }
    }

    public static class DroneChargeable extends ConnectorChargeable {
        public final Drone drone;

        public DroneChargeable(Drone drone) {
            super((Connector) drone.components.node());
            this.drone = drone;
        }

        @Override
        public Vec3 pos() {
            return new Vec3(drone.getX(), drone.getY(), drone.getZ());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DroneChargeable chargeable && chargeable.drone == drone;
        }

        @Override
        public int hashCode() {
            return drone.hashCode();
        }
    }

    public static class PlayerChargeable implements Chargeable {
        public final Player player;

        public PlayerChargeable(Player player) {
            this.player = player;
        }

        @Override
        public Vec3 pos() {
            return new Vec3(player.getX(), player.getY(), player.getZ());
        }

        @Override
        public double changeBuffer(double delta) {
            final Controller controller = li.cil.oc.api.Nanomachines.getController(player);
            if (controller != null) return controller.changeBuffer(delta);
            else return delta; // Cannot charge.
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PlayerChargeable chargeable && chargeable.player == player;
        }

        @Override
        public int hashCode() {
            return player.hashCode();
        }
    }
}
