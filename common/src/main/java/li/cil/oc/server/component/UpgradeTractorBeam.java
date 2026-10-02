package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class UpgradeTractorBeam {
    private UpgradeTractorBeam() {
    }

    public abstract static class Common extends AbstractManagedEnvironment implements DeviceInfo {
        protected Common() {
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("tractor_beam").
                create());
        }

        @Override
        public Component node() {
            return (Component) super.node();
        }

        private final int pickupRadius = 3;

        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) {
                deviceInfo = Map.of(
                    DeviceAttribute.Class, DeviceClass.Generic,
                    DeviceAttribute.Description, "Tractor beam",
                    DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                    DeviceAttribute.Product, "T313-K1N.3515"
                );
            }
            return deviceInfo;
        }

        protected abstract BlockPosition position();

        protected abstract void collectItem(ItemEntity item);

        private Level world() {
            return position().world.get();
        }

        @Callback(doc = "function():boolean -- Tries to pick up a random item in the robots' vicinity.")
        public Object[] suck(Context context, Arguments args) {
            final List<ItemEntity> items = new ArrayList<>();
            for (ItemEntity item : world().getEntitiesOfClass(ItemEntity.class, position().bounds().inflate(pickupRadius, pickupRadius, pickupRadius))) {
                if (item.isAlive() && !item.hasPickUpDelay()) items.add(item);
            }
            if (!items.isEmpty()) {
                final ItemEntity item = items.get(world().random.nextInt(items.size()));
                final ItemStack stack = item.getItem();
                final int size = stack.getCount();
                collectItem(item);
                if (stack.getCount() < size || !item.isAlive()) {
                    context.pause(Settings.get().suckDelay);
                    world().levelEvent(2003, new BlockPos((int) Math.floor(item.getX()), (int) Math.floor(item.getY()), (int) Math.floor(item.getZ())), 0);
                    return ResultWrapper.result(true);
                }
            }
            return ResultWrapper.result(false);
        }
    }

    public static class Player extends Common {
        public final EnvironmentHost owner;
        public final Supplier<net.minecraft.world.entity.player.Player> player;

        public Player(EnvironmentHost owner, Supplier<net.minecraft.world.entity.player.Player> player) {
            this.owner = owner;
            this.player = player;
        }

        @Override
        protected BlockPosition position() {
            return BlockPosition.apply(owner);
        }

        @Override
        protected void collectItem(ItemEntity item) {
            item.playerTouch(player.get());
        }
    }

    public static class Drone extends Common {
        public final Agent owner;

        public Drone(Agent owner) {
            this.owner = owner;
        }

        @Override
        protected BlockPosition position() {
            return BlockPosition.apply(owner);
        }

        @Override
        protected void collectItem(ItemEntity item) {
            InventoryUtils.insertIntoInventory(item.getItem(), owner.mainInventory(), Optional.empty(), 64, false, Optional.of(insertionSlots()));
        }

        private List<Integer> insertionSlots() {
            final List<Integer> slots = new ArrayList<>();
            final int size = owner.mainInventory().getContainerSize();
            for (int i = owner.selectedSlot(); i < size; i++) slots.add(i);
            for (int i = 0; i < owner.selectedSlot(); i++) slots.add(i);
            return slots;
        }
    }
}
