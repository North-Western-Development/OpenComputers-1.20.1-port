package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.authlib.GameProfile;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.platform.ComponentPlatform;

import java.util.UUID;

/**
 * Operator commands for testing machines without a player, e.g. on a headless
 * dedicated server: {@code /oc_debug (start|stop|status) <pos>},
 * {@code /oc_debug place <pos> <item>} (a fake player uses the item on the top
 * face of the block below {@code pos}, e.g. to place a robot or a drone) and
 * {@code /oc_debug drones (start|stop|status)} (all loaded drones).
 * <p>
 * Only registered when the JVM is started with {@code -Dopencomputers.debugCommands=true}.
 */
public final class DebugCommands {
    public static final String PROPERTY = "opencomputers.debugCommands";

    private DebugCommands() {
    }

    public static void register() {
        if (!Boolean.getBoolean(PROPERTY)) return;
        OpenComputers.log.info("Registering OpenComputers debug commands.");
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher, registry));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registry) {
        dispatcher.register(Commands.literal("oc_debug")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("start").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "start"))))
            .then(Commands.literal("stop").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "stop"))))
            .then(Commands.literal("status").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "status"))))
            .then(Commands.literal("place").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("item", ItemArgument.item(registry))
                    .executes(DebugCommands::place))))
            .then(Commands.literal("drones")
                .then(Commands.literal("start").executes(context -> drones(context, "start")))
                .then(Commands.literal("stop").executes(context -> drones(context, "stop")))
                .then(Commands.literal("status").executes(context -> drones(context, "status")))));
    }

    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("0c0de000-0c0d-4e00-8000-0c0de0000000"), "[OC-Debug]");

    private static int place(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        final ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        final ServerLevel level = context.getSource().getLevel();
        final ServerPlayer player = ComponentPlatform.fakePlayer(level, PROFILE);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() - 1.5);
        player.setYRot(0);
        player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos below = pos.below();
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
        final String result;
        try {
            result = String.valueOf(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        final String placed = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        context.getSource().sendSuccess(() -> Component.literal("[oc_debug] place: " + result + " block=" + placed), true);
        return 1;
    }

    private static int drones(CommandContext<CommandSourceStack> context, String action) {
        final ServerLevel level = context.getSource().getLevel();
        int count = 0;
        for (Drone drone : level.getEntities(EntityTypes.DRONE.get(), drone -> true)) {
            count++;
            final Machine machine = drone.machine();
            final String result = switch (action) {
                case "start" -> "start: " + drone.start();
                case "stop" -> "stop: " + machine.stop();
                default -> "pos=" + String.format("%.2f,%.2f,%.2f", drone.getX(), drone.getY(), drone.getZ()) +
                    " running=" + machine.isRunning() + " components=" + machine.componentCount() +
                    " lastError=" + machine.lastError();
            };
            context.getSource().sendSuccess(() -> Component.literal("[oc_debug] drone " + result), true);
        }
        if (count == 0) context.getSource().sendFailure(Component.literal("No drones"));
        return count;
    }

    private static int run(CommandContext<CommandSourceStack> context, String action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        final BlockEntity blockEntity = context.getSource().getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof MachineHost host) || host.machine() == null) {
            context.getSource().sendFailure(Component.literal("No machine at " + pos.toShortString()));
            return 0;
        }
        final Machine machine = host.machine();
        final String result = switch (action) {
            case "start" -> "start: " + machine.start();
            case "stop" -> "stop: " + machine.stop();
            default -> "running=" + machine.isRunning() + " paused=" + machine.isPaused() +
                " components=" + machine.componentCount() + " lastError=" + machine.lastError();
        };
        context.getSource().sendSuccess(() -> Component.literal("[oc_debug] " + result), true);
        return 1;
    }
}
