package li.cil.oc.integration.appeng;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/** An {@link IActionSource} for actions performed by OC on behalf of an AE2 machine. */
public class MachineSource implements IActionSource {
    public final IActionHost via;

    public MachineSource(IActionHost via) {
        this.via = via;
    }

    @Override
    public Optional<Player> player() {
        return Optional.empty();
    }

    @Override
    public Optional<IActionHost> machine() {
        return Optional.of(via);
    }

    @Override
    public <T> Optional<T> context(Class<T> key) {
        return Optional.empty();
    }
}
