package li.cil.oc.server.driver;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompoundBlockEnvironment implements ManagedEnvironment {
    public final String name;
    public final List<Pair<String, ManagedEnvironment>> environments;

    // Block drivers with visibility < network usually won't make much sense,
    // but let's play it safe and use the least possible visibility based on
    // the drivers we encapsulate.
    public final Component node;

    public final List<ManagedEnvironment> updatingEnvironments;

    public CompoundBlockEnvironment(String name, List<Pair<String, ManagedEnvironment>> environments) {
        this.name = name;
        this.environments = Collections.unmodifiableList(new ArrayList<>(environments));

        Visibility visibility = Visibility.None;
        for (Pair<String, ManagedEnvironment> entry : this.environments) {
            final Node n = entry.getRight().node();
            if (n != null && n.reachability().compareTo(visibility) > 0) visibility = n.reachability();
        }
        this.node = Network.newNode(this, visibility).withComponent(name).create();

        final List<ManagedEnvironment> updating = new ArrayList<>();
        for (Pair<String, ManagedEnvironment> entry : this.environments) {
            if (entry.getRight().canUpdate()) updating.add(entry.getRight());
        }
        this.updatingEnvironments = updating;

        // Force all wrapped components to be neighbor visible, since we as their
        // only neighbor will take care of all component-related interaction.
        for (Pair<String, ManagedEnvironment> entry : this.environments) {
            if (entry.getRight().node() instanceof Component component) {
                component.setVisibility(Visibility.Neighbors);
            }
        }
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public boolean canUpdate() {
        for (Pair<String, ManagedEnvironment> entry : environments) {
            if (entry.getRight().canUpdate()) return true;
        }
        return false;
    }

    @Override
    public void update() {
        for (ManagedEnvironment environment : updatingEnvironments) {
            environment.update();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            for (Pair<String, ManagedEnvironment> entry : environments) {
                if (entry.getRight().node() != null) node.connect(entry.getRight().node());
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node) {
            for (Pair<String, ManagedEnvironment> entry : environments) {
                if (entry.getRight().node() != null) entry.getRight().node().remove();
            }
        }
    }

    private static final String TypeHashTag = "typeHash";

    @Override
    public void loadData(CompoundTag nbt) {
        // Ignore existing data if the underlying type is different.
        if (nbt.contains(TypeHashTag) && nbt.getLong(TypeHashTag) != typeHash()) return;
        node.loadData(nbt);
        for (Pair<String, ManagedEnvironment> entry : environments) {
            final String driver = entry.getLeft();
            final ManagedEnvironment environment = entry.getRight();
            if (nbt.contains(driver)) {
                try {
                    environment.loadData(nbt.getCompound(driver));
                } catch (Throwable e) {
                    OpenComputers.log.warn("A block component of type '" + environment.getClass().getName() + "' (provided by driver '" + driver + "') threw an error while loading.", e);
                }
            }
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        nbt.putLong(TypeHashTag, typeHash());
        node.saveData(nbt);
        for (Pair<String, ManagedEnvironment> entry : environments) {
            final String driver = entry.getLeft();
            final ManagedEnvironment environment = entry.getRight();
            try {
                ExtendedNBT.setNewCompoundTag(nbt, driver, environment::saveData);
            } catch (Throwable e) {
                OpenComputers.log.warn("A block component of type '" + environment.getClass().getName() + "' (provided by driver '" + driver + "') threw an error while saving.", e);
            }
        }
    }

    private long typeHash() {
        final Hasher hash = Hashing.sha256().newHasher();
        final List<String> names = new ArrayList<>();
        for (Pair<String, ManagedEnvironment> entry : environments) names.add(entry.getRight().getClass().getName());
        Collections.sort(names);
        for (String name : names) hash.putString(name, Charset.defaultCharset());
        return hash.hash().asLong();
    }
}
