package li.cil.oc.common.item.traits;

/**
 * Marker for items that show their tier in advanced tooltips. The tooltip line itself
 * (formerly a stacked {@code appendHoverText} override) is added by
 * {@link SimpleItem#appendHoverText} for every {@link SimpleItem} implementing this.
 */
public interface ItemTier {
}
