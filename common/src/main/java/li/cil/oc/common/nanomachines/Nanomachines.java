package li.cil.oc.common.nanomachines;

import li.cil.oc.Settings;
import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.PlayerUtils;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Nanomachines implements NanomachinesAPI {
    public static final Nanomachines INSTANCE = new Nanomachines();

    private Nanomachines() {
    }

    public final Set<BehaviorProvider> providers = Collections.synchronizedSet(new LinkedHashSet<>());

    public final Map<Player, ControllerImpl> serverControllers = Collections.synchronizedMap(new WeakHashMap<>());
    public final Map<Player, ControllerImpl> clientControllers = Collections.synchronizedMap(new WeakHashMap<>());

    public Map<Player, ControllerImpl> controllers(Player player) {
        return player.level().isClientSide ? clientControllers : serverControllers;
    }

    @Override
    public void addProvider(BehaviorProvider provider) {
        providers.add(provider);
    }

    @Override
    public Iterable<BehaviorProvider> getProviders() {
        return providers;
    }

    @Override
    public Controller getController(Player player) {
        if (hasController(player)) return controllers(player).computeIfAbsent(player, ControllerImpl::new);
        else return null;
    }

    @Override
    public boolean hasController(Player player) {
        return PlayerUtils.persistedData(player).getBoolean(Settings.namespace + "hasNanomachines");
    }

    @Override
    public Controller installController(Player player) {
        if (!hasController(player)) {
            PlayerUtils.persistedData(player).putBoolean(Settings.namespace + "hasNanomachines", true);
        }
        return getController(player); // Initialize controller instance.
    }

    @Override
    public void uninstallController(Player player) {
        if (getController(player) instanceof ControllerImpl controller) {
            controller.dispose();
            controllers(player).remove(player);
            PlayerUtils.persistedData(player).remove(Settings.namespace + "hasNanomachines");
            if (!player.level().isClientSide) {
                PacketSender.sendNanomachineConfiguration(player);
            }
        }
        // else: Doesn't have one anyway.
    }
}
