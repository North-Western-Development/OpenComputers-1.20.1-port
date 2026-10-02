package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.client.KeyBindings;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RobotProxy extends RedstoneAware implements StateAware {
    public final VoxelShape shape = Shapes.box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);

    /**
     * Set by a robot while it moves, so the proxy block entity created for the new
     * position wraps the moving robot instead of creating a new one.
     */
    public final ThreadLocal<Optional<li.cil.oc.common.tileentity.Robot>> moving = ThreadLocal.withInitial(Optional::empty);

    public RobotProxy(Properties props) {
        super(props);
    }

    // Note: the Scala version overrode getDescriptionId with "robot" (only used for tooltip keys below);
    // the block now uses the regular block.opencomputers.robot translation key.

    // ----------------------------------------------------------------------- //

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy) {
            return proxy.robot.info.copyItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy) {
            li.cil.oc.common.tileentity.Robot robot = proxy.robot;
            if (robot.isAnimatingMove()) {
                double remaining = robot.animationTicksLeft / (double) robot.animationTicksTotal;
                BlockPos blockPos = robot.moveFrom.get();
                BlockPos vec = robot.getBlockPos();
                BlockPos delta = new BlockPos(blockPos.getX() - vec.getX(), blockPos.getY() - vec.getY(), blockPos.getZ() - vec.getZ());
                return shape.move(delta.getX() * remaining, delta.getY() * remaining, delta.getZ() * remaining);
            }
            return shape;
        }
        return super.getShape(state, world, pos, ctx);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipHead(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        super.tooltipHead(stack, world, tooltip, advanced);
        addLines(stack, tooltip);
    }

    @Override
    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        addLines(tooltip, Tooltip.get("robot"));
    }

    @Override
    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, world, tooltip, flag);
        if (KeyBindings.showExtendedTooltips()) {
            RobotData info = new RobotData(stack);
            List<ItemStack> components = new ArrayList<>(Arrays.asList(info.containers));
            components.addAll(Arrays.asList(info.components));
            if (!components.isEmpty()) {
                addLines(tooltip, Tooltip.get("server.Components"));
                for (ItemStack component : components) {
                    if (!component.isEmpty()) {
                        tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                    }
                }
            }
        }
    }

    private void addLines(ItemStack stack, List<Component> tooltip) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains(Settings.namespace + "xp")) {
                double xp = tag.getDouble(Settings.namespace + "xp");
                int level = Math.min((int) (Math.pow(xp - Settings.get().baseXpToLevel, 1 / Settings.get().exponentialXpGrowth) / Settings.get().constantXpGrowth), 30);
                if (level > 0) {
                    addLines(tooltip, Tooltip.get("robot_level", level));
                }
            }
            if (tag.contains(Settings.namespace + "storedEnergy")) {
                int energy = tag.getInt(Settings.namespace + "storedEnergy");
                if (energy > 0) {
                    addLines(tooltip, Tooltip.get("robot_storedenergy", energy));
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        Optional<li.cil.oc.common.tileentity.Robot> robot = moving.get();
        if (robot.isPresent()) {
            return new li.cil.oc.common.tileentity.RobotProxy(TileEntityTypes.ROBOT.get(), pos, state, robot.get());
        }
        return TileEntityTypes.ROBOT.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Superspecial hack... usually this will not work, because Minecraft calls
        // this method *after* the block has already been destroyed. Meaning we
        // won't have access to the tile entity.
        // However! Some mods with block breakers, specifically AE2's annihilation
        // plane, will call *only* this method (don't use a fake player to call
        // removedByPlayer), but call it *before* the block was destroyed. So in
        // general it *should* be safe to generate the item here if the tile entity
        // still exists, and always spawn the stack in removedByPlayer... if some
        // mod calls this before the block is broken *and* calls removedByPlayer
        // this will lead to dupes, but in some initial testing this wasn't the
        // case anywhere (TE autonomous activator, CC turtles).
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof li.cil.oc.common.tileentity.RobotProxy proxy) {
            builder = builder.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f -> {
                li.cil.oc.common.tileentity.Robot robot = proxy.robot;
                if (robot.node() != null) {
                    // Update: even more special hack! As discussed here http://git.io/IcNAyg
                    // some mods call this even when they're not about to actually break the
                    // block... soooo we need a whitelist to know when to generate a *proper*
                    // drop (i.e. with file systems closed / open handles not saved, e.g.).
                    if (gettingDropsForActualDrop()) {
                        robot.node().remove();
                        robot.saveComponents();
                    }
                    f.accept(robot.info.createItemStack());
                }
            });
        }
        return super.getDrops(state, builder);
    }

    private static final Set<String> getDropForRealDropCallers = Set.of(
        "appeng.parts.automation.PartAnnihilationPlane.EatBlock"
    );

    private static boolean gettingDropsForActualDrop() {
        for (StackTraceElement element : new Exception().getStackTrace()) {
            if (getDropForRealDropCallers.contains(element.getClassName() + "." + element.getMethodName())) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (!player.isCrouching()) {
            if (!world.isClientSide) {
                // We only send slot changes to nearby players, so if there was no slot
                // change since this player got into range he might have the wrong one,
                // so we send him the current one just in case.
                if (player instanceof ServerPlayer srvPlr && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy
                    && proxy.robot.node().network() != null) {
                    PacketSender.sendRobotSelectedSlotChange(proxy.robot);
                    if (proxy.stillValid(player)) {
                        ContainerTypes.openRobotGui(srvPlr, proxy.robot);
                    }
                }
            }
            return true;
        }
        else if (heldItem.isEmpty()) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy
                && !proxy.machine().isRunning() && proxy.stillValid(player)) {
                proxy.machine().start();
            }
            return true;
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        if (world.isClientSide) return;
        if (!(world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy)) return;
        String owner;
        UUID uuid;
        if (entity instanceof li.cil.oc.server.agent.Player player) {
            owner = player.agent.ownerName();
            uuid = player.agent.ownerUUID();
        }
        else if (entity instanceof Player player) {
            owner = player.getName().getString();
            uuid = player.getGameProfile().getId();
        }
        else return;
        li.cil.oc.common.tileentity.Robot robot = proxy.robot;
        robot.ownerName = owner;
        robot.ownerUUID = li.cil.oc.server.agent.Player.determineUUID(Optional.ofNullable(uuid));
        robot.info.loadData(stack);
        li.cil.oc.api.network.Connector botNode = (li.cil.oc.api.network.Connector) robot.bot.node();
        botNode.changeBuffer(robot.info.robotEnergy - botNode.localBuffer());
        robot.updateInventorySize();
    }

    @Override
    public boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.RobotProxy proxy) {
            li.cil.oc.common.tileentity.Robot robot = proxy.robot;
            // Only allow breaking creative tier robots by allowed users.
            // Unlike normal robots, griefing isn't really a valid concern
            // here, because to get a creative robot you need creative
            // mode in the first place.
            if (robot.isCreative() && (!player.isCreative() || !robot.canInteract(player.getName().getString()))) return false;
            if (!world.isClientSide) {
                if (robot.player() == player) return false;
                robot.node().remove();
                robot.saveComponents();
                if (player.isCreative()) InventoryUtils.spawnStackInWorld(BlockPosition.apply(pos, world), robot.info.createItemStack());
            }
            robot.moveFrom.ifPresent(fromPos -> {
                if (world.getBlockState(fromPos).getBlock() == Items.get(Constants.BlockName.RobotAfterimage).block()) {
                    world.setBlock(fromPos, Blocks.AIR.defaultBlockState(), 1);
                }
            });
        }
        return super.removedByPlayer(state, world, pos, player);
    }
}
