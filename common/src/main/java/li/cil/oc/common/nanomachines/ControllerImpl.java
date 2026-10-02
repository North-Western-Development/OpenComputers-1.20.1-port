package li.cil.oc.common.nanomachines;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.NanomachineData;
import li.cil.oc.integration.util.DamageSourceWithRandomCause;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.PlayerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ControllerImpl implements Controller, WirelessEndpoint {
    /**
     * Data-driven damage type (data/opencomputers/damage_type/nanomachines_overload.json).
     */
    public static final ResourceKey<DamageType> OverloadDamageType = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("opencomputers", "nanomachines_overload"));

    public final Player player;

    public ResourceKey<Level> previousDimension;

    private double commandRange = -1;

    public static final int FullSyncInterval = 20 * 60;

    public String uuid = UUID.randomUUID().toString();
    public int responsePort = 0;
    public int commandDelay = 0;
    public Optional<Runnable> queuedCommand = Optional.empty();
    public double storedEnergy = Settings.get().bufferNanomachines * 0.25;
    public boolean hadPower = true;
    public final NeuralNetwork configuration = new NeuralNetwork(this);
    public final Set<Behavior> activeBehaviors = new LinkedHashSet<>();
    public boolean activeBehaviorsDirty = true;
    public boolean hasSentConfiguration = false;

    public ControllerImpl(Player player) {
        this.player = player;
        if (isServer()) Network.joinWirelessNetwork(this);
        this.previousDimension = player.level().dimension();
    }

    public double CommandRange() {
        if (commandRange < 0) {
            commandRange = Settings.get().nanomachinesCommandRange * Settings.get().nanomachinesCommandRange;
        }
        return commandRange;
    }

    private DamageSourceWithRandomCause overloadDamage() {
        return DamageSourceWithRandomCause.create(player.level(), OverloadDamageType, 3);
    }

    @Override
    public Level world() {
        return player.level();
    }

    @Override
    public int x() {
        return BlockPosition.apply(player).x;
    }

    @Override
    public int y() {
        return BlockPosition.apply(player).y;
    }

    @Override
    public int z() {
        return BlockPosition.apply(player).z;
    }

    @Override
    public void receivePacket(Packet packet, WirelessEndpoint sender) {
        if (getLocalBuffer() > 0 && commandDelay < 1 && player.isAlive()) {
            final double dx = (sender.x() + 0.5) - player.getX();
            final double dy = (sender.y() + 0.5) - player.getY();
            final double dz = (sender.z() + 0.5) - player.getZ();
            final double dSquared = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dSquared > CommandRange()) return;
            final Object[] data = packet.data();
            if (data.length == 0 || !(data[0] instanceof byte[] header) || !new String(header, StandardCharsets.UTF_8).equals("nanomachines")) {
                return; // Not for us.
            }
            final Object[] command = new Object[data.length - 1];
            for (int i = 1; i < data.length; i++) {
                command[i - 1] = data[i] instanceof byte[] value ? new String(value, StandardCharsets.UTF_8) : data[i];
            }
            if (command.length == 0 || !(command[0] instanceof String name)) return;

            if (command.length == 2 && name.equals("setResponsePort") && command[1] instanceof Number port) {
                responsePort = Math.min(Math.max(port.intValue(), 0), 0xFFFF);
                respond(sender, "port", responsePort);
            } else if (command.length == 1 && name.equals("getPowerState")) {
                respond(sender, "power", getLocalBuffer(), getLocalBufferSize());
            } else if (command.length == 1 && name.equals("saveConfiguration")) {
                final ItemInfo nanomachines = Items.get(Constants.ItemName.Nanomachines);
                try {
                    final List<ItemStack> items = player.getInventory().items;
                    int index = -1;
                    for (int i = 0; i < items.size(); i++) {
                        final ItemStack stack = items.get(i);
                        if (Items.get(stack) == nanomachines && new NanomachineData(stack).configuration.isEmpty()) {
                            index = i;
                            break;
                        }
                    }
                    if (index >= 0) {
                        final ItemStack stack = player.getInventory().removeItem(index, 1);
                        new NanomachineData(this).saveData(stack);
                        player.getInventory().add(stack);
                        InventoryUtils.spawnStackInWorld(BlockPosition.apply(player), stack);
                        respond(sender, "saved", true);
                    } else respond(sender, "saved", false, "no nanomachines");
                } catch (Throwable t) {
                    respond(sender, "saved", false, "error");
                }
            } else if (command.length == 1 && name.equals("getHealth")) {
                respond(sender, "health", player.getHealth(), player.getMaxHealth());
            } else if (command.length == 1 && name.equals("getHunger")) {
                respond(sender, "hunger", player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
            } else if (command.length == 1 && name.equals("getAge")) {
                respond(sender, "age", (int) (player.getNoActionTime() / 20f));
            } else if (command.length == 1 && name.equals("getName")) {
                respond(sender, "name", player.getDisplayName().getString());
            } else if (command.length == 1 && name.equals("getExperience")) {
                respond(sender, "experience", player.experienceLevel);
            } else if (command.length == 1 && name.equals("getTotalInputCount")) {
                respond(sender, "totalInputCount", getTotalInputCount());
            } else if (command.length == 1 && name.equals("getSafeActiveInputs")) {
                respond(sender, "safeActiveInputs", getSafeActiveInputs());
            } else if (command.length == 1 && name.equals("getMaxActiveInputs")) {
                respond(sender, "maxActiveInputs", getMaxActiveInputs());
            } else if (command.length == 2 && name.equals("getInput") && command[1] instanceof Number index) {
                try {
                    final boolean trigger = getInput(index.intValue() - 1);
                    respond(sender, "input", index.intValue(), trigger);
                } catch (Throwable t) {
                    respond(sender, "input", "error");
                }
            } else if (command.length == 3 && name.equals("setInput") && command[1] instanceof Number index && command[2] instanceof Boolean value) {
                try {
                    if (setInput(index.intValue() - 1, value)) {
                        respond(sender, "input", index.intValue(), getInput(index.intValue() - 1));
                    } else {
                        respond(sender, "input", "too many active inputs");
                    }
                } catch (Throwable t) {
                    respond(sender, "input", "error");
                }
            } else if (command.length == 1 && name.equals("getActiveEffects")) {
                synchronized (configuration) {
                    final List<String> names = new ArrayList<>();
                    for (Behavior behavior : getActiveBehaviors()) {
                        final String hint = behavior.getNameHint();
                        if (!Strings.isNullOrEmpty(hint)) names.add(hint.replace(',', '_').replace('"', '_'));
                    }
                    final String joined = "{" + String.join(",", names) + "}";
                    respond(sender, "effects", joined);
                }
            }
            // else: Ignore.
        }
    }

    public void respond(WirelessEndpoint endpoint, Object... data) {
        queuedCommand = Optional.of(() -> {
            if (responsePort > 0) {
                final double cost = Settings.get().wirelessCostPerRange[Tier.Two] * CommandRange();
                final double epsilon = 0.1;
                if (changeBuffer(-cost) > -epsilon) {
                    final Object[] packetData = new Object[data.length + 1];
                    packetData[0] = "nanomachines";
                    System.arraycopy(data, 0, packetData, 1, data.length);
                    final Packet packet = Network.newPacket(uuid, null, responsePort, packetData);
                    Network.sendWirelessPacket(this, CommandRange(), packet);
                }
            }
        });
        commandDelay = (int) (Settings.get().nanomachinesCommandDelay * 20);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Controller reconfigure() {
        if (isServer()) synchronized (configuration) {
            configuration.reconfigure();
            activeBehaviorsDirty = true;

            if (player instanceof ServerPlayer playerMP && playerMP.connection != null) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 150));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
                changeBuffer(-Settings.get().nanomachineReconfigureCost);

                hasSentConfiguration = false;
            }
            // else: We're still setting up / loading.
        }
        return this;
    }

    @Override
    public int getTotalInputCount() {
        synchronized (configuration) {
            return configuration.triggers.size();
        }
    }

    @Override
    public int getSafeActiveInputs() {
        return Settings.get().nanomachinesSafeInputsActive;
    }

    @Override
    public int getMaxActiveInputs() {
        return Settings.get().nanomachinesMaxInputsActive;
    }

    @Override
    public boolean getInput(int index) {
        synchronized (configuration) {
            return configuration.triggers.get(index).isActive;
        }
    }

    @Override
    public boolean setInput(int index, boolean value) {
        if (!isServer()) return false;
        synchronized (configuration) {
            if (!value || countActiveTriggers() < Settings.get().nanomachinesMaxInputsActive) {
                configuration.triggers.get(index).isActive = value;
                activeBehaviorsDirty = true;
                return true;
            }
            return false;
        }
    }

    private int countActiveTriggers() {
        int count = 0;
        for (NeuralNetwork.TriggerNeuron trigger : configuration.triggers) {
            if (trigger.isActive) count++;
        }
        return count;
    }

    @Override
    public Iterable<Behavior> getActiveBehaviors() {
        synchronized (configuration) {
            cleanActiveBehaviors(DisableReason.InputChanged);
            return activeBehaviors;
        }
    }

    @Override
    public int getInputCount(Behavior behavior) {
        synchronized (configuration) {
            return configuration.inputs(behavior);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double getLocalBuffer() {
        return storedEnergy;
    }

    @Override
    public double getLocalBufferSize() {
        return Settings.get().bufferNanomachines;
    }

    @Override
    public double changeBuffer(double delta) {
        if (isClient()) return delta;
        else if (delta < 0 && (Settings.get().ignorePower || player.isCreative())) return 0.0;
        else {
            final double newValue = storedEnergy + delta;
            storedEnergy = Math.min(Math.max(newValue, 0), getLocalBufferSize());
            return newValue - storedEnergy;
        }
    }

    // ----------------------------------------------------------------------- //

    public void update() {
        if (!player.isAlive()) {
            return;
        }

        if (isServer()) {
            if (commandDelay > 0) {
                commandDelay -= 1;
                if (commandDelay == 0) {
                    queuedCommand.ifPresent(Runnable::run);
                    queuedCommand = Optional.empty();
                }
            }

            // Handle dimension changes, the robust way (because when logging in,
            // load is called while the world is still set to the overworld, but
            // no dimension change event is fired if the player actually logged
            // out in another dimension... yay)
            if (player.level().dimension() != previousDimension) {
                Network.leaveWirelessNetwork(this, previousDimension);
                Network.joinWirelessNetwork(this);
                previousDimension = player.level().dimension();
            } else {
                Network.updateWirelessNetwork(this);
            }
        }

        boolean hasPower = getLocalBuffer() > 0 || Settings.get().ignorePower;
        List<Behavior> active = null; // Wrap once, lazily.

        if (hasPower != hadPower) {
            active = activeSnapshot();
            if (!hasPower) {
                for (Behavior behavior : active) behavior.onDisable(DisableReason.OutOfEnergy); // This may change our energy buffer.
                hasPower = getLocalBuffer() > 0 || Settings.get().ignorePower;
            } else {
                for (Behavior behavior : active) behavior.onEnable();
            }
        }

        if (hasPower) {
            if (active == null) active = activeSnapshot();
            for (Behavior behavior : active) behavior.update();

            final int activeInputs = countActiveTriggers();

            if (isServer()) {
                if (player.level().getGameTime() % Settings.get().tickFrequency == 0) {
                    changeBuffer(-Settings.get().nanomachineCost * Settings.get().tickFrequency * (activeInputs + 0.5));
                    PacketSender.sendNanomachinePower(player);
                }

                final int overload = activeInputs - getSafeActiveInputs();
                if (!player.isCreative() && overload > 0 && player.level().getGameTime() % 20 == 0) {
                    player.hurt(overloadDamage(), overload);
                }
            }

            if (isClient() && Settings.get().enableNanomachinePfx) {
                final double energyRatio = getLocalBuffer() / (getLocalBufferSize() + 1);
                // Note: integer division like on 1.16.5.
                final double triggerRatio = activeInputs / (configuration.triggers.size() + 1);
                final double intensity = (energyRatio + triggerRatio) * 0.25;
                PlayerUtils.spawnParticleAround(player, ParticleTypes.PORTAL, intensity);
            }
        }

        if (isServer()) {
            // Send new power state, if it changed.
            if (hadPower != hasPower) {
                PacketSender.sendNanomachinePower(player);
            }

            // Send a full sync every now and then, e.g. for other players coming
            // closer that weren't there to get the initial info for an enabled
            // input.
            if (!hasSentConfiguration || player.level().getGameTime() % FullSyncInterval == 0) {
                hasSentConfiguration = true;
                PacketSender.sendNanomachineConfiguration(player);
            }
        }

        hadPower = hasPower;
    }

    private List<Behavior> activeSnapshot() {
        final List<Behavior> result = new ArrayList<>();
        for (Behavior behavior : getActiveBehaviors()) result.add(behavior);
        return result;
    }

    public void reset() {
        synchronized (configuration) {
            for (int index = 0; index < getTotalInputCount(); index++) {
                configuration.triggers.get(index).isActive = false;
                activeBehaviorsDirty = true;
            }
            cleanActiveBehaviors(DisableReason.Default);
        }
    }

    public void dispose() {
        reset();
        if (isServer()) {
            Network.leaveWirelessNetwork(this);
        }
    }

    public void debug() {
        if (isServer()) {
            configuration.debug();
            activeBehaviorsDirty = true;
        }
    }

    public void print() {
        if (isServer()) {
            configuration.print(player);
        }
    }

    // ----------------------------------------------------------------------- //

    public void saveData(CompoundTag nbt) {
        synchronized (configuration) {
            nbt.putString("uuid", uuid);
            nbt.putInt("port", responsePort);
            nbt.putDouble("energy", storedEnergy);
            ExtendedNBT.setNewCompoundTag(nbt, "configuration", configuration::saveData);
        }
    }

    public void loadData(CompoundTag nbt) {
        synchronized (configuration) {
            uuid = nbt.getString("uuid");
            responsePort = nbt.getInt("port");
            storedEnergy = nbt.getDouble("energy");
            configuration.loadData(nbt.getCompound("configuration"));
            activeBehaviorsDirty = true;
        }
    }

    // ----------------------------------------------------------------------- //

    private boolean isClient() {
        return world().isClientSide;
    }

    private boolean isServer() {
        return !isClient();
    }

    private void cleanActiveBehaviors(DisableReason reason) {
        if (activeBehaviorsDirty) {
            synchronized (configuration) {
                if (activeBehaviorsDirty) {
                    final Set<Behavior> newBehaviors = new LinkedHashSet<>();
                    for (NeuralNetwork.BehaviorNeuron neuron : configuration.behaviors) {
                        if (neuron.isActive()) newBehaviors.add(neuron.behavior);
                    }
                    final Set<Behavior> addedBehaviors = new LinkedHashSet<>(newBehaviors);
                    addedBehaviors.removeAll(activeBehaviors);
                    final Set<Behavior> removedBehaviors = new HashSet<>(activeBehaviors);
                    removedBehaviors.removeAll(newBehaviors);
                    activeBehaviors.clear();
                    activeBehaviors.addAll(newBehaviors);
                    activeBehaviorsDirty = false;
                    for (Behavior behavior : addedBehaviors) behavior.onEnable();
                    for (Behavior behavior : removedBehaviors) behavior.onDisable(reason);

                    if (isServer()) {
                        PacketSender.sendNanomachineInputs(player);
                    }
                }
            }
        }
    }
}
