package li.cil.oc.common.block.traits;

/**
 * Marker for blocks that accept power. SimpleBlock appends the "poweracceptor"
 * tooltip lines in {@code tooltipTail} for blocks implementing this.
 */
public interface PowerAcceptor {
    double energyThroughput();
}
