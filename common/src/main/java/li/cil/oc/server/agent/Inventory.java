package li.cil.oc.server.agent;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Inventory extends net.minecraft.world.entity.player.Inventory {
    public final Agent agent;

    public Inventory(Player playerEntity, Agent agent) {
        super(playerEntity);
        this.agent = agent;
    }

    private ItemStack selectedItemStack() {
        return agent.mainInventory().getItem(agent.selectedSlot());
    }

    private List<Integer> inventorySlots() {
        final List<Integer> slots = new ArrayList<>();
        for (int i = agent.selectedSlot(); i < getContainerSize(); i++) slots.add(i);
        for (int i = 0; i < agent.selectedSlot(); i++) slots.add(i);
        return slots;
    }

    @Override
    public ItemStack getSelected() {
        return agent.equipmentInventory().getItem(0);
    }

    @Override
    public int getFreeSlot() {
        if (selectedItemStack().isEmpty()) return agent.selectedSlot();
        for (int slot : inventorySlots()) {
            if (getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    @Override
    public void pickSlot(int direction) {
    }

    @Override
    public int clearOrCountMatchingItems(Predicate<ItemStack> f, int count, Container inv) {
        return 0;
    }

    @Override
    public void tick() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                try {
                    stack.inventoryTick(agent.world(), !agent.world().isClientSide() ? agent.player() : null, slot, slot == 0);
                } catch (NullPointerException ignored) {
                    // Client side item updates that need a player instance...
                }
            }
        }
    }

    @Override
    public boolean add(ItemStack stack) {
        return InventoryUtils.insertIntoInventory(stack, InventoryUtils.asItemHandler(this), 64, false, Optional.of(inventorySlots()));
    }

    @Override
    public float getDestroySpeed(BlockState state) {
        return getSelected().isEmpty() ? 1f : getSelected().getDestroySpeed(state);
    }

    @Override
    public ListTag save(ListTag nbt) {
        return nbt;
    }

    @Override
    public void load(ListTag nbt) {
    }

    @Override
    public ItemStack getArmor(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void hurtArmor(DamageSource source, float damage, int[] slots) {
    }

    @Override
    public void dropAll() {
    }

    @Override
    public boolean contains(ItemStack stack) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final ItemStack item = getItem(slot);
            if (!item.isEmpty() && ItemStack.isSameItem(item, stack)) return true;
        }
        return false;
    }

    @Override
    public void replaceWith(net.minecraft.world.entity.player.Inventory from) {
    }

    // Container

    @Override
    public int getContainerSize() {
        return agent.mainInventory().getContainerSize();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0) return agent.equipmentInventory().getItem(~slot);
        return agent.mainInventory().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0) return agent.equipmentInventory().removeItem(~slot, amount);
        return agent.mainInventory().removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0) return agent.equipmentInventory().removeItemNoUpdate(~slot);
        return agent.mainInventory().removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0) agent.equipmentInventory().setItem(~slot, stack);
        else agent.mainInventory().setItem(slot, stack);
    }

    @Override
    public Component getName() {
        return Component.literal(agent.name());
    }

    @Override
    public int getMaxStackSize() {
        return agent.mainInventory().getMaxStackSize();
    }

    @Override
    public void setChanged() {
        agent.mainInventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return agent.mainInventory().stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0) return agent.equipmentInventory().canPlaceItem(~slot, stack);
        return agent.mainInventory().canPlaceItem(slot, stack);
    }
}
