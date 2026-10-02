package li.cil.oc.common.nanomachines;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.server.PacketSender;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class NeuralNetwork implements Persistable {
    private final ControllerImpl controller;

    public final List<TriggerNeuron> triggers = new ArrayList<>();
    public final List<ConnectorNeuron> connectors = new ArrayList<>();
    public final List<BehaviorNeuron> behaviors = new ArrayList<>();

    public final Map<Behavior, BehaviorNeuron> behaviorMap = new HashMap<>();

    public NeuralNetwork(ControllerImpl controller) {
        this.controller = controller;
    }

    public int inputs(Behavior behavior) {
        final BehaviorNeuron node = behaviorMap.get(behavior);
        if (node == null) return 0;
        int count = 0;
        for (Neuron input : node.inputs) {
            if (input.isActive()) count++;
        }
        return count;
    }

    private List<BehaviorNeuron> createBehaviorNeurons() {
        final List<BehaviorNeuron> result = new ArrayList<>();
        for (BehaviorProvider p : li.cil.oc.api.Nanomachines.getProviders()) {
            final Iterable<Behavior> created = p.createBehaviors(controller.player);
            if (created == null) continue; // Remove null lists..
            for (Behavior b : created) {
                if (b != null) result.add(new BehaviorNeuron(p, b)); // Remove null behaviors.
            }
        }
        return result;
    }

    public void reconfigure() {
        // Rebuild list of valid behaviors.
        behaviors.clear();
        behaviors.addAll(createBehaviorNeurons());

        // Adjust length of trigger list and reset.
        while (triggers.size() > behaviors.size() * Settings.get().nanomachineTriggerQuota) {
            triggers.remove(triggers.size() - 1);
        }
        for (TriggerNeuron trigger : triggers) trigger.isActive = false;
        while (triggers.size() < behaviors.size() * Settings.get().nanomachineTriggerQuota) {
            triggers.add(new TriggerNeuron());
        }

        // Adjust length of connector list and reset.
        while (connectors.size() > behaviors.size() * Settings.get().nanomachineConnectorQuota) {
            connectors.remove(connectors.size() - 1);
        }
        for (ConnectorNeuron connector : connectors) connector.inputs.clear();
        while (connectors.size() < behaviors.size() * Settings.get().nanomachineConnectorQuota) {
            connectors.add(new ConnectorNeuron());
        }

        // Build connections.
        final Random rng = new Random(controller.player.level().random.nextInt());

        // Connect connectors to triggers, then behaviors to connectors and/or remaining triggers.
        final List<Neuron> sourcePool = new ArrayList<>();
        for (int i = 0; i < Settings.get().nanomachineMaxOutputs; i++) sourcePool.addAll(triggers);
        connect(rng, connectors, sourcePool);
        for (int i = 0; i < Settings.get().nanomachineMaxOutputs; i++) sourcePool.addAll(connectors);
        connect(rng, behaviors, sourcePool);

        // Clean up dead nodes.
        final List<ConnectorNeuron> deadConnectors = new ArrayList<>();
        for (ConnectorNeuron connector : connectors) {
            if (connector.inputs.isEmpty()) deadConnectors.add(connector);
        }
        connectors.removeAll(deadConnectors);
        for (BehaviorNeuron behavior : behaviors) behavior.inputs.removeAll(deadConnectors);

        behaviors.removeIf(behavior -> behavior.inputs.isEmpty());

        behaviorMap.clear();
        for (BehaviorNeuron n : behaviors) behaviorMap.put(n.behavior, n);
    }

    private static void connect(Random rng, List<? extends ConnectorNeuron> sinks, List<Neuron> sources) {
        // Shuffle sink list to give each entry the same chance.
        final List<ConnectorNeuron> sinkPool = new ArrayList<>(sinks);
        Collections.shuffle(sinkPool, rng);
        for (ConnectorNeuron sink : sinkPool) {
            if (sources.isEmpty()) continue;
            // Avoid connecting one sink to the same source twice.
            final Set<Neuron> blacklist = new HashSet<>();
            final int count = rng.nextInt(Settings.get().nanomachineMaxInputs);
            for (int n = 0; n <= count; n++) {
                if (sources.isEmpty()) continue;
                final int baseIndex = rng.nextInt(sources.size());
                int sourceIndex = -1;
                for (int i = 0; i < sources.size(); i++) {
                    final Neuron candidate = sources.get((baseIndex + i) % sources.size());
                    if (!blacklist.contains(candidate)) {
                        sourceIndex = i;
                        break;
                    }
                }
                if (sourceIndex >= 0) {
                    final Neuron source = sources.remove((sourceIndex + baseIndex) % sources.size());
                    blacklist.add(source);
                    sink.inputs.add(source);
                }
            }
        }
    }

    // Enter debug configuration, one input -> one behavior, and list mapping in console.
    public void debug() {
        final Consumer<String> log;
        if (controller.player instanceof ServerPlayer playerMP) {
            log = s -> PacketSender.sendClientLog(s, playerMP);
        } else {
            log = s -> OpenComputers.log.info(s);
        }
        log.accept("Creating debug configuration for nanomachines in player " + controller.player.getDisplayName().getString() + ".");

        behaviors.clear();
        behaviors.addAll(createBehaviorNeurons());

        connectors.clear();

        triggers.clear();
        for (int i = 0; i < behaviors.size(); i++) {
            final BehaviorNeuron behavior = behaviors.get(i);
            final TriggerNeuron trigger = new TriggerNeuron();
            triggers.add(trigger);
            behavior.inputs.add(trigger);

            log.accept(i + " -> " + behavior.behavior.getNameHint() + " (" + behavior.behavior.getClass() + ")");
        }
    }

    public void print(Player player) {
        final StringBuilder sb = new StringBuilder();
        for (BehaviorNeuron behavior : behaviors) {
            final String hint = behavior.behavior.getNameHint();
            final String name = hint != null ? hint : behavior.behavior.getClass().getSimpleName();
            colored(sb, name, behavior.isActive());
            sb.append(" <- (");
            boolean first = true;
            for (Neuron input : behavior.inputs) {
                if (first) first = false;
                else sb.append(", ");
                if (input instanceof TriggerNeuron neuron) {
                    colored(sb, triggers.indexOf(neuron) + 1, neuron.isActive());
                } else if (input instanceof ConnectorNeuron neuron) {
                    sb.append("(");
                    first = true;
                    for (Neuron trigger : neuron.inputs) {
                        if (first) first = false;
                        else sb.append(", ");
                        colored(sb, triggers.indexOf(trigger) + 1, trigger.isActive());
                    }
                    first = false;
                    sb.append(")");
                }
            }
            sb.append(")");
            player.sendSystemMessage(Component.literal(sb.toString()));
            sb.setLength(0);
        }
    }

    private static void colored(StringBuilder sb, Object value, boolean enabled) {
        if (enabled) sb.append(ChatFormatting.GREEN);
        else sb.append(ChatFormatting.RED);
        sb.append(value);
        sb.append(ChatFormatting.RESET);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        saveData(nbt, false);
    }

    private static final String TriggersTag = "triggers";
    private static final String IsActiveTag = "isActive";
    private static final String ConnectorsTag = "connectors";
    private static final String BehaviorsTag = "behaviors";
    private static final String BehaviorTag = "behavior";
    private static final String TriggerInputsTag = "triggerInputs";
    private static final String ConnectorInputsTag = "connectorInputs";

    private int[] indicesIn(List<Neuron> inputs, List<? extends Neuron> pool) {
        return inputs.stream().mapToInt(pool::indexOf).filter(i -> i >= 0).toArray();
    }

    public void saveData(CompoundTag nbt, boolean forItem) {
        final ListTag triggersList = new ListTag();
        for (TriggerNeuron t : triggers) {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean(IsActiveTag, t.isActive && !forItem);
            triggersList.add(tag);
        }
        nbt.put(TriggersTag, triggersList);

        final ListTag connectorsList = new ListTag();
        for (ConnectorNeuron c : connectors) {
            final CompoundTag tag = new CompoundTag();
            tag.putIntArray(TriggerInputsTag, indicesIn(c.inputs, triggers));
            connectorsList.add(tag);
        }
        nbt.put(ConnectorsTag, connectorsList);

        final ListTag behaviorsList = new ListTag();
        for (BehaviorNeuron b : behaviors) {
            final CompoundTag tag = new CompoundTag();
            tag.putIntArray(TriggerInputsTag, indicesIn(b.inputs, triggers));
            tag.putIntArray(ConnectorInputsTag, indicesIn(b.inputs, connectors));
            tag.put(BehaviorTag, b.provider.save(b.behavior));
            behaviorsList.add(tag);
        }
        nbt.put(BehaviorsTag, behaviorsList);
    }

    @Override
    public void loadData(CompoundTag nbt) {
        triggers.clear();
        final ListTag triggersList = nbt.getList(TriggersTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < triggersList.size(); i++) {
            final CompoundTag t = triggersList.getCompound(i);
            final TriggerNeuron neuron = new TriggerNeuron();
            neuron.isActive = t.getBoolean(IsActiveTag);
            triggers.add(neuron);
        }

        connectors.clear();
        final ListTag connectorsList = nbt.getList(ConnectorsTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < connectorsList.size(); i++) {
            final CompoundTag t = connectorsList.getCompound(i);
            final ConnectorNeuron neuron = new ConnectorNeuron();
            for (int index : t.getIntArray(TriggerInputsTag)) neuron.inputs.add(triggers.get(index));
            connectors.add(neuron);
        }

        behaviors.clear();
        final ListTag behaviorsList = nbt.getList(BehaviorsTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < behaviorsList.size(); i++) {
            final CompoundTag t = behaviorsList.getCompound(i);
            for (BehaviorProvider p : li.cil.oc.api.Nanomachines.getProviders()) {
                final Behavior b = p.load(controller.player, t.getCompound(BehaviorTag));
                if (b != null) {
                    final BehaviorNeuron neuron = new BehaviorNeuron(p, b);
                    for (int index : t.getIntArray(TriggerInputsTag)) neuron.inputs.add(triggers.get(index));
                    for (int index : t.getIntArray(ConnectorInputsTag)) neuron.inputs.add(connectors.get(index));
                    behaviors.add(neuron);
                    break; // Done.
                }
                // else: Keep looking.
            }
        }

        behaviorMap.clear();
        for (BehaviorNeuron n : behaviors) behaviorMap.put(n.behavior, n);
    }

    public interface Neuron {
        boolean isActive();
    }

    public static class TriggerNeuron implements Neuron {
        public boolean isActive = false;

        @Override
        public boolean isActive() {
            return isActive;
        }
    }

    public static class ConnectorNeuron implements Neuron {
        public final List<Neuron> inputs = new ArrayList<>();

        @Override
        public boolean isActive() {
            for (Neuron input : inputs) {
                if (!input.isActive()) return false;
            }
            return true;
        }
    }

    public static class BehaviorNeuron extends ConnectorNeuron {
        public final BehaviorProvider provider;
        public final Behavior behavior;

        public BehaviorNeuron(BehaviorProvider provider, Behavior behavior) {
            this.provider = provider;
            this.behavior = behavior;
        }
    }
}
