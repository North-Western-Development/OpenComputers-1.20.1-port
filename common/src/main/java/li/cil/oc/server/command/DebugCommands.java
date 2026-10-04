package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
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
 * {@code /oc_debug drones (start|stop|status)} (all loaded drones),
 * {@code /oc_debug use <pos> <player>} (the player right-clicks the block or drone at pos, e.g.
 * to open its GUI) and {@code /oc_debug useitem <player>} (server-side use of the held item).
 * {@code start|stop|status} on a rack act on its first server.
 * {@code /oc_debug type <pos> <text>} pastes a line (plus Enter) into the machine's screen through its
 * keyboard, {@code /oc_debug screen <pos>} prints the non-blank lines of its screen and
 * {@code /oc_debug tablet <player> (start|stop|status|screen|type <text>)} does the same for the tablet
 * in the player's main hand. {@code /oc_debug useitem <player> release} also releases the item
 * right away (a click, e.g. to turn on a tablet). {@code /oc_debug protect|unprotect <pos>} makes the
 * block refuse interaction like a protection mod would; entities tagged {@code oc_protected} do as well.
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
        li.cil.oc.common.platform.DebugPlatform.registerProtectionHooks();
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
            .then(Commands.literal("use").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(DebugCommands::use))))
            .then(Commands.literal("useitem").then(Commands.argument("player", EntityArgument.player())
                .executes(context -> useItem(context, false))
                .then(Commands.literal("release").executes(context -> useItem(context, true)))))
            .then(Commands.literal("type").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(context -> run(context, "type")))))
            .then(Commands.literal("screen").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> run(context, "screen"))))
            .then(Commands.literal("tablet").then(Commands.argument("player", EntityArgument.player())
                .then(Commands.literal("start").executes(context -> tablet(context, "start")))
                .then(Commands.literal("stop").executes(context -> tablet(context, "stop")))
                .then(Commands.literal("status").executes(context -> tablet(context, "status")))
                .then(Commands.literal("screen").executes(context -> tablet(context, "screen")))
                .then(Commands.literal("type").then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(context -> tablet(context, "type"))))))
            .then(Commands.literal("protect").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> protect(context, true))))
            .then(Commands.literal("unprotect").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> protect(context, false))))
            .then(Commands.literal("drones")
                .then(Commands.literal("start").executes(context -> drones(context, "start")))
                .then(Commands.literal("stop").executes(context -> drones(context, "stop")))
                .then(Commands.literal("status").executes(context -> drones(context, "status")))));
    }

    /** Entities with this scoreboard tag (e.g. {@code /tag @e[...] add oc_protected}) refuse interaction. */
    public static final String PROTECTED_TAG = "oc_protected";

    private static final java.util.Set<String> protectedBlocks = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static String blockKey(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.dimension().location() + "@" + pos.asLong();
    }

    /** Whether the test protection hooks deny interacting with the entity. */
    public static boolean isProtected(net.minecraft.world.entity.Entity entity) {
        return entity != null && entity.getTags().contains(PROTECTED_TAG);
    }

    /** Whether the test protection hooks deny interacting with the block ({@code oc_debug protect <pos>}). */
    public static boolean isProtected(net.minecraft.world.level.Level level, BlockPos pos) {
        return !protectedBlocks.isEmpty() && level != null && pos != null && protectedBlocks.contains(blockKey(level, pos));
    }

    private static int protect(CommandContext<CommandSourceStack> context, boolean protect) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        final String key = blockKey(context.getSource().getLevel(), pos);
        if (protect) protectedBlocks.add(key);
        else protectedBlocks.remove(key);
        context.getSource().sendSuccess(() -> Component.literal("[oc_debug] " + (protect ? "protected " : "unprotected ") + pos.toShortString()), true);
        return 1;
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

    /** The player right-clicks the block at pos (or a drone in it), e.g. to open its GUI. */
    private static int use(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        final ServerPlayer player = EntityArgument.getPlayer(context, "player");
        final ServerLevel level = context.getSource().getLevel();
        final String result;
        final java.util.List<Drone> drones = level.getEntities(EntityTypes.DRONE.get(), new net.minecraft.world.phys.AABB(pos), drone -> true);
        if (!drones.isEmpty()) {
            result = String.valueOf(drones.get(0).interact(player, InteractionHand.MAIN_HAND));
        } else {
            final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            result = String.valueOf(level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit));
        }
        context.getSource().sendSuccess(() -> Component.literal("[oc_debug] use: " + result), true);
        return 1;
    }

    /** The player uses the item in their main hand (on the server side only), optionally releasing it right away. */
    private static int useItem(CommandContext<CommandSourceStack> context, boolean release) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = EntityArgument.getPlayer(context, "player");
        final ItemStack stack = player.getMainHandItem();
        String result = String.valueOf(stack.use(player.level(), player, InteractionHand.MAIN_HAND).getResult());
        if (release && player.isUsingItem()) {
            player.releaseUsingItem();
            result += " released";
        }
        final String message = result;
        context.getSource().sendSuccess(() -> Component.literal("[oc_debug] useitem: " + message), true);
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
        BlockEntity blockEntity = context.getSource().getLevel().getBlockEntity(pos);
        Object target = blockEntity;
        if (blockEntity instanceof li.cil.oc.common.tileentity.Rack rack) {
            // The first server (or other machine) mounted in the rack.
            for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                if (rack.getMountable(slot) instanceof MachineHost mounted) {
                    target = mounted;
                    break;
                }
            }
        }
        if (!(target instanceof MachineHost host) || host.machine() == null) {
            context.getSource().sendFailure(Component.literal("No machine at " + pos.toShortString()));
            return 0;
        }
        final ServerPlayer typist = ComponentPlatform.fakePlayer(context.getSource().getLevel(), PROFILE);
        typist.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return machineAction(context, host.machine(), action, typist);
    }

    /** start/stop/status/screen/type for the tablet in the player's main hand. */
    private static int tablet(CommandContext<CommandSourceStack> context, String action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = EntityArgument.getPlayer(context, "player");
        final ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof li.cil.oc.common.item.Tablet)) {
            context.getSource().sendFailure(Component.literal("No tablet in hand"));
            return 0;
        }
        final Machine machine = li.cil.oc.common.item.Tablet.get(stack, player).machine();
        return machineAction(context, machine, action, player);
    }

    private static int machineAction(CommandContext<CommandSourceStack> context, Machine machine, String action, ServerPlayer typist) {
        final StringBuilder result = new StringBuilder();
        switch (action) {
            case "start" -> result.append("start: ").append(machine.start());
            case "stop" -> result.append("stop: ").append(machine.stop());
            case "type", "screen" -> {
                final li.cil.oc.api.internal.TextBuffer buffer = textBuffer(machine);
                if (buffer == null) {
                    context.getSource().sendFailure(Component.literal("No screen"));
                    return 0;
                }
                if (action.equals("type")) {
                    // Like pasting the line and pressing enter in the screen GUI.
                    buffer.clipboard(StringArgumentType.getString(context, "text") + "\n", typist);
                    result.append("type: ok");
                } else {
                    result.append("screen:");
                    for (int row = 0; row < buffer.getViewportHeight(); row++) {
                        final StringBuilder line = new StringBuilder();
                        for (int col = 0; col < buffer.getViewportWidth(); col++) line.appendCodePoint(buffer.getCodePoint(col, row));
                        final String text = line.toString().stripTrailing();
                        if (!text.isEmpty()) result.append(" ").append(row + 1).append("| ").append(text);
                    }
                }
            }
            default -> result.append("running=").append(machine.isRunning()).append(" paused=").append(machine.isPaused())
                .append(" components=").append(machine.componentCount()).append(" lastError=").append(machine.lastError());
        }
        context.getSource().sendSuccess(() -> Component.literal("[oc_debug] " + result), true);
        return 1;
    }

    private static li.cil.oc.api.internal.TextBuffer textBuffer(Machine machine) {
        if (machine == null || machine.node() == null) return null;
        for (li.cil.oc.api.network.Node node : machine.node().reachableNodes()) {
            if (node.host() instanceof li.cil.oc.api.internal.TextBuffer buffer) return buffer;
        }
        return null;
    }
}
