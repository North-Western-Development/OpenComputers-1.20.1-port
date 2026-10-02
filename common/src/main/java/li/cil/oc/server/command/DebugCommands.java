package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Operator commands for testing machines without a player, e.g. on a headless
 * dedicated server: {@code /oc_debug (start|stop|status) <pos>}.
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
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oc_debug")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("start").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "start"))))
            .then(Commands.literal("stop").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "stop"))))
            .then(Commands.literal("status").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "status")))));
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
