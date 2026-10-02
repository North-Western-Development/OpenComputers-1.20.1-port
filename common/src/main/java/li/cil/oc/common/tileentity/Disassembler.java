package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.PlayerInputAware;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.StateAware;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Disassembler extends TileEntity implements Environment, PowerAcceptor, li.cil.oc.common.tileentity.traits.Inventory, StateAware, PlayerInputAware, Tickable, DeviceInfo, MenuProvider {
    public final Connector node = li.cil.oc.api.Network.newNode(this, Visibility.None).
        withConnector(Settings.get().bufferConverter).
        create();

    public boolean isActive = false;

    public final List<ItemStack> queue = new ArrayList<>();

    public double totalRequiredEnergy = 0.0;

    public double buffer = 0.0;

    public boolean disassembleNextInstantly = false;

    private Map<String, String> deviceInfo;

    private static final String QueueTag = Settings.namespace + "queue";
    private static final String BufferTag = Settings.namespace + "buffer";
    private static final String TotalTag = Settings.namespace + "total";
    private static final String IsActiveTag = Settings.namespace + "isActive";

    public Disassembler(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public double progress() {
        return queue.isEmpty() ? 0.0 : (1 - (queue.size() * Settings.get().disassemblerItemCost - buffer) / totalRequiredEnergy) * 100;
    }

    private void setActive(boolean value) {
        if (value != isActive) {
            isActive = value;
            PacketSender.sendDisassemblerActive(this, isActive);
            getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Disassembler",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Break.3R-100"
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasConnector(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(side != Direction.UP ? node : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().disassemblerRate;
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (isActive) return EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        else if (!queue.isEmpty()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        else return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer() && getLevel().getGameTime() % Settings.get().tickFrequency == 0) {
            if (queue.isEmpty()) {
                final boolean instant = disassembleNextInstantly; // Is reset via removeItem
                disassemble(removeItem(0, 1), instant);
                setActive(!queue.isEmpty());
            } else {
                if (buffer < Settings.get().disassemblerItemCost) {
                    final double want = Settings.get().disassemblerTickAmount;
                    final boolean success = node.tryChangeBuffer(-want);
                    setActive(success); // If energy is insufficient indicate it visually.
                    if (success) {
                        buffer += want;
                    }
                }
                while (buffer >= Settings.get().disassemblerItemCost && !queue.isEmpty()) {
                    buffer -= Settings.get().disassemblerItemCost;
                    final ItemStack stack = queue.remove(0);
                    if (disassembleNextInstantly || getLevel().random.nextDouble() >= Settings.get().disassemblerBreakChance) {
                        drop(stack);
                    }
                }
            }
            disassembleNextInstantly = !queue.isEmpty(); // If we have nothing left to do, stop being creative.
        }
    }

    public void disassemble(ItemStack stack, boolean instant) {
        // Validate the item, never trust Minecraft / other Mods on anything!
        if (canPlaceItem(0, stack)) {
            final ItemStack[] ingredients = ItemUtils.getIngredients(getLevel().getRecipeManager(), getLevel().registryAccess(), stack);
            final Optional<DisassemblerTemplates.Template> template = DisassemblerTemplates.select(stack);
            if (template.isPresent()) {
                final Pair<Optional<ItemStack[]>, Optional<ItemStack[]>> result = template.get().disassemble(stack, ingredients);
                result.getLeft().ifPresent(stacks -> queue.addAll(Arrays.asList(stacks)));
                result.getRight().ifPresent(drops -> {
                    for (ItemStack drop : drops) drop(drop);
                });
            } else {
                queue.addAll(Arrays.asList(ingredients));
            }
            totalRequiredEnergy = queue.size() * Settings.get().disassemblerItemCost;
            if (instant) {
                buffer = totalRequiredEnergy;
            }
        } else {
            drop(stack);
        }
    }

    public void disassemble(ItemStack stack) {
        disassemble(stack, false);
    }

    private void drop(ItemStack stack) {
        if (!stack.isEmpty()) {
            for (Direction side : Direction.values()) {
                if (stack.getCount() <= 0) break;
                InventoryUtils.insertIntoInventoryAt(stack, BlockPosition.apply(this).offset(side), Optional.of(side.getOpposite()));
            }
            if (stack.getCount() > 0) {
                spawnStackInWorld(stack, Optional.of(Direction.UP));
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        queue.clear();
        queue.addAll(ExtendedNBT.<CompoundTag, ItemStack>map(nbt.getList(QueueTag, Tag.TAG_COMPOUND), ItemStack::of));
        buffer = nbt.getDouble(BufferTag);
        totalRequiredEnergy = nbt.getDouble(TotalTag);
        isActive = !queue.isEmpty();
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        ExtendedNBT.setNewTagList(nbt, QueueTag, ExtendedNBT.itemStackIterableToNbt(queue));
        nbt.putDouble(BufferTag, buffer);
        nbt.putDouble(TotalTag, totalRequiredEnergy);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        isActive = nbt.getBoolean(IsActiveTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        nbt.putBoolean(IsActiveTag, isActive);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack stack) {
        return allowDisassembling(stack) &&
            (((Settings.get().disassembleAllTheThings || li.cil.oc.api.Items.get(stack) != null) && ItemUtils.getIngredients(getLevel().getRecipeManager(), getLevel().registryAccess(), stack).length > 0) ||
                DisassemblerTemplates.select(stack).isPresent());
    }

    private boolean allowDisassembling(ItemStack stack) {
        return !stack.isEmpty() && (!stack.hasTag() || !stack.getTag().getBoolean(Settings.namespace + "undisassemblable"));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        li.cil.oc.common.tileentity.traits.Inventory.super.setItem(slot, stack);
        if (!getLevel().isClientSide) {
            disassembleNextInstantly = false;
        }
    }

    @Override
    public void onSetInventorySlotContents(Player player, int slot, ItemStack stack) {
        if (!getLevel().isClientSide) {
            disassembleNextInstantly = !stack.isEmpty() && slot == 0 && player.isCreative();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.container.Disassembler(ContainerTypes.DISASSEMBLER.get(), id, playerInventory, this);
    }
}
