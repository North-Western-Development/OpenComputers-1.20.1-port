package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

public final class MagnetProvider extends ScalaProvider {
    public static final MagnetProvider INSTANCE = new MagnetProvider();

    private MagnetProvider() {
        super("9324d5ec-71f1-41c2-b51c-406e527668fc");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        return Collections.singletonList(new MagnetBehavior(player));
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new MagnetBehavior(player);
    }

    public static class MagnetBehavior extends AbstractBehavior {
        public MagnetBehavior(Player player) {
            super(player);
        }

        @Override
        public String getNameHint() {
            return "magnet";
        }

        @Override
        public void update() {
            final Level world = player.level();
            if (!world.isClientSide) {
                final double actualRange = Settings.get().nanomachineMagnetRange * Nanomachines.getController(player).getInputCount(this);
                for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(actualRange, actualRange, actualRange))) {
                    if (!item.hasPickUpDelay() && !item.getItem().isEmpty() && hasSpaceFor(item.getItem())) {
                        final double dx = player.getX() - item.getX();
                        final double dy = player.getY() - item.getY();
                        final double dz = player.getZ() - item.getZ();
                        final Vec3 delta = new Vec3(dx, dy, dz).normalize();
                        item.push(delta.x * 0.1, delta.y * 0.1, delta.z * 0.1);
                    }
                }
            }
        }

        private boolean hasSpaceFor(ItemStack item) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize() && ItemStack.isSameItem(stack, item)) return true;
            }
            return false;
        }
    }
}
