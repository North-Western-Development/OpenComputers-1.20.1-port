package li.cil.oc.common.tileentity.traits.power;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.tileentity.traits.TileEntityTrait;
import net.minecraft.core.Direction;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Common extends TileEntityTrait {
    /** Client side only. */
    default boolean hasConnector(Direction side) {
        return false;
    }

    default Optional<Connector> connector(Direction side) {
        return Optional.empty();
    }

    // ----------------------------------------------------------------------- //

    double energyThroughput();

    default void tryAllSides(BiFunction<Double, Direction, Double> provider, Function<Double, Double> fromOther, Function<Double, Double> toOther) {
        // We make sure to only call this every `Settings.get().tickFrequency` ticks,
        // but our throughput is per tick, so multiply this up for actual budget.
        double budget = energyThroughput() * Settings.get().tickFrequency;
        for (Direction side : Direction.values()) {
            final double demand = toOther.apply(Math.min(budget, globalDemand(side)));
            if (demand > 1) {
                final double energy = fromOther.apply(provider.apply(demand, side));
                if (energy > 0) {
                    budget -= tryChangeBuffer(side, energy);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    default boolean canConnectPower(Direction side) {
        return !Settings.get().ignorePower && (isClient() ? hasConnector(side) : connector(side).isPresent());
    }

    /**
     * Tries to inject the specified amount of energy into the buffer via the specified side.
     *
     * @param side      the side to change the buffer through.
     * @param amount    the amount to change the buffer by.
     * @param doReceive whether to actually inject energy or only simulate it.
     * @return the amount of energy that was actually injected.
     */
    default double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        if (isClient() || Settings.get().ignorePower) return 0;
        final Optional<Connector> connector = connector(side);
        if (connector.isPresent()) {
            final double cappedAmount = Math.max(0, Math.min(Math.min(energyThroughput(), amount), globalDemand(side)));
            if (doReceive) return cappedAmount - connector.get().changeBuffer(cappedAmount);
            else return cappedAmount;
        }
        return 0;
    }

    default double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    default double globalBuffer(Direction side) {
        if (isClient()) return 0;
        return connector(side).map(Connector::globalBuffer).orElse(0.0);
    }

    default double globalBufferSize(Direction side) {
        if (isClient()) return 0;
        return connector(side).map(Connector::globalBufferSize).orElse(0.0);
    }

    default double globalDemand(Direction side) {
        return Math.max(0, Math.min(energyThroughput(), globalBufferSize(side) - globalBuffer(side)));
    }
}
