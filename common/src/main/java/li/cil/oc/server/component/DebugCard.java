package li.cil.oc.server.component;

import com.google.common.base.Strings;
import dev.architectury.fluid.FluidStack;
import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.common.platform.ComponentPlatform;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.network.DebugNetwork;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static li.cil.oc.util.ResultWrapper.result;

public class DebugCard extends AbstractManagedEnvironment implements DebugNetwork.DebugNode {
    private final EnvironmentHost host;

    public final ComponentConnector node;

    // Used to detect disconnects.
    private Optional<Node> remoteNode = Optional.empty();

    // Used for delayed connecting to remote node again after loading.
    private Optional<BlockPos> remoteNodePosition = Optional.empty();

    // Player this card is bound to (if any) to use for permissions.
    public Optional<AccessContext> access = Optional.empty();

    private final Object commandLock = new Object();

    private Optional<String> CommandMessages = Optional.empty();

    public DebugCard(EnvironmentHost host) {
        this.host = host;
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Neighbors).
                withComponent("debug").
                withConnector().
                create();
        setNode(node);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    public Optional<String> player() {
        return access.map(a -> a.player);
    }

    private CommandSourceStack createCommandSourceStack() {
        final CommandSource sender = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                CommandMessages = Optional.of(CommandMessages.map(m -> m + "\n").orElse("") + message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return true;
            }
        };
        final ServerLevel world = (ServerLevel) host.world();
        final MinecraftServer server = world.getServer();
        final ServerPlayer sourcePlayer = player()
                .map(name -> server.getPlayerList().getPlayerByName(name))
                .orElse(null);
        final ServerPlayer actualPlayer = sourcePlayer != null ? sourcePlayer : ComponentPlatform.fakePlayer(world, Settings.get().fakePlayerProfile);
        final int permLevel = server.getProfilePermissions(actualPlayer.getGameProfile());
        return new CommandSourceStack(sender, new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), Vec2.ZERO, world,
                permLevel, actualPlayer.getName().getString(), actualPlayer.getDisplayName(), server, actualPlayer);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(value:number):number -- Changes the component network's energy buffer by the specified delta.")
    public Object[] changeBuffer(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result(node.changeBuffer(args.checkDouble(0)));
    }

    @Callback(doc = "function():number -- Get the container's X position in the world.")
    public Object[] getX(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result(host.xPosition());
    }

    @Callback(doc = "function():number -- Get the container's Y position in the world.")
    public Object[] getY(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result(host.yPosition());
    }

    @Callback(doc = "function():number -- Get the container's Z position in the world.")
    public Object[] getZ(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result(host.zPosition());
    }

    private static Level levelForLegacyId(int id) {
        final MinecraftServer server = GameInstance.getServer();
        return switch (id) {
            case 0 -> server.overworld();
            case -1 -> server.getLevel(Level.NETHER);
            case 1 -> server.getLevel(Level.END);
            default -> null;
        };
    }

    @Deprecated
    @Callback(doc = "function([id:number]):userdata -- Get the world object for the specified dimension ID, or the container's.")
    public Object[] getWorld(Context context, Arguments args) throws Exception {
        checkAccess(access);
        if (args.count() > 0) {
            return result(new WorldValue(levelForLegacyId(args.checkInteger(0)), access));
        } else return result(new WorldValue(host.world(), access));
    }

    @Deprecated
    @Callback(doc = "function():table -- Get a list of all world IDs, loaded and unloaded.")
    public Object[] getWorlds(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result((Object) new int[]{0, -1, 1});
    }

    @Callback(doc = "function(name:string):userdata -- Get the entity of a player.")
    public Object[] getPlayer(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result(new PlayerValue(args.checkString(0), access));
    }

    @Callback(doc = "function():table -- Get a list of currently logged-in players.")
    public Object[] getPlayers(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result((Object) GameInstance.getServer().getPlayerNames());
    }

    @Callback(doc = "function():userdata -- Get the scoreboard object for the world")
    public Object[] getScoreboard(Context context, Arguments args) throws Exception {
        checkAccess(access);
        return result(new ScoreboardValue(Optional.ofNullable(host.world()), access));
    }

    @Deprecated
    @Callback(doc = "function(x: number, y: number, z: number[, worldId: number]):boolean, string, table -- returns contents at the location in world by id (default host world)")
    public Object[] scanContentsAt(Context context, Arguments args) throws Exception {
        checkAccess(access);
        final int x = args.checkInteger(0);
        final int y = args.checkInteger(1);
        final int z = args.checkInteger(2);
        final Level world = args.count() > 3 ? levelForLegacyId(args.checkInteger(3)) : host.world();

        final BlockPosition position = new BlockPosition(x, y, z, Optional.ofNullable(world));
        final ServerPlayer fakePlayer = ComponentPlatform.fakePlayer((ServerLevel) world, Settings.get().fakePlayerProfile);
        fakePlayer.setPos(position.x + 0.5, position.y + 0.5, position.z + 0.5);

        final List<Entity> candidates = world.getEntitiesOfClass(Entity.class, position.bounds(), e -> true);
        final Optional<Entity> closest = candidates.stream().min(Comparator.comparingDouble(fakePlayer::distanceToSqr));
        if (closest.isPresent() && closest.get() instanceof LivingEntity living) {
            return result(true, "EntityLiving", living);
        } else if (closest.isPresent() && closest.get() instanceof Minecart minecart) {
            return result(true, "EntityMinecart", minecart);
        } else {
            final BlockPos pos = position.toBlockPos();
            final BlockState state = world.getBlockState(pos);
            final Block block = state.getBlock();
            if (state.isAir()) {
                return result(false, "air", block);
            }
            // Note: the 1.16 code had an inverted fluid check here (every non-air block was "liquid").
            else if (block instanceof LiquidBlock) {
                return result(!PlatformHooks.canBreakBlock((ServerLevel) world, pos, fakePlayer), "liquid", block);
            } else if (state.canBeReplaced()) {
                return result(!PlatformHooks.canBreakBlock((ServerLevel) world, pos, fakePlayer), "replaceable", block);
            } else if (state.getCollisionShape(world, pos, CollisionContext.empty()).isEmpty()) {
                return result(true, "passable", block);
            } else {
                return result(true, "solid", block);
            }
        }
    }

    @Callback(doc = "function(name:string):boolean -- Get whether a mod or API is loaded.")
    public Object[] isModLoaded(Context context, Arguments args) throws Exception {
        checkAccess(access);
        final String name = args.checkString(0);
        return result(Platform.isModLoaded(name));
    }

    @Callback(doc = "function(command:string):number -- Runs an arbitrary command using a fake player.")
    public Object[] runCommand(Context context, Arguments args) throws Exception {
        checkAccess(access);
        final Collection<?> commands =
                args.isTable(0) ? args.checkTable(0).values() : List.of(args.checkString(0));

        final CommandSourceStack source = createCommandSourceStack();
        synchronized (commandLock) {
            CommandMessages = Optional.empty();
            int value = 0;
            for (Object command : commands) {
                value = GameInstance.getServer().getCommands().performPrefixedCommand(source, String.valueOf(command));
            }
            return result(value, CommandMessages.orElse(null));
        }
    }

    @Callback(doc = "function(x:number, y:number, z:number):boolean -- Add a component block at the specified coordinates to the computer network.")
    public Object[] connectToBlock(Context context, Arguments args) throws Exception {
        checkAccess(access);
        final int x = args.checkInteger(0);
        final int y = args.checkInteger(1);
        final int z = args.checkInteger(2);
        final Optional<Node> found = findNode(BlockPosition.apply(x, y, z));
        if (found.isPresent()) {
            remoteNode.ifPresent(node::disconnect);
            remoteNode = found;
            remoteNodePosition = Optional.of(new BlockPos(x, y, z));
            node.connect(found.get());
            return result(true);
        } else return result(null, "no node found at this position");
    }

    private Optional<Node> findNode(BlockPosition position) {
        final Level world = host.world();
        if (ExtendedWorld.blockExists(world, position)) {
            final BlockEntity blockEntity = ExtendedWorld.getBlockEntity(world, position);
            if (blockEntity instanceof SidedEnvironment env) {
                for (Direction side : Direction.values()) {
                    final Node sided = env.sidedNode(side);
                    if (sided != null) return Optional.of(sided);
                }
                return Optional.empty();
            } else if (blockEntity instanceof Environment env) {
                return Optional.ofNullable(env.node());
            }
        }
        return Optional.empty();
    }

    @Callback(doc = "function():userdata -- Test method for user-data and general value conversion.")
    public Object[] test(Context context, Arguments args) throws Exception {
        checkAccess(access);

        final Map<Object, Object> v1 = new HashMap<>();
        v1.put("a", true);
        v1.put("b", "test");
        final Map<Object, Object> v2 = new HashMap<>();
        v2.put(10, "zxc");
        v2.put(false, v1);
        v1.put("c", v2);

        return result(v2, new TestValue(), host.world());
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(player:string, text:string) -- Sends text to the specified player's clipboard if possible.")
    public Object[] sendToClipboard(Context context, Arguments args) throws Exception {
        checkAccess(access);
        final ServerPlayer player = GameInstance.getServer().getPlayerList().getPlayerByName(args.checkString(0));
        if (player != null) {
            PacketSender.sendClipboard(player, args.checkString(1));
            return result(true);
        } else return result(false, "no such player");
    }

    @Callback(doc = "function(address:string, data...) -- Sends data to the debug card with the specified address.")
    public Object[] sendToDebugCard(Context context, Arguments args) throws Exception {
        checkAccess(access);
        final String destination = args.checkString(0);
        DebugNetwork.getEndpoint(destination).filter(endpoint -> endpoint != this).ifPresent(endpoint -> {
            final Object[] all = args.toArray();
            final Packet packet = Network.newPacket(node.address(), destination, 0, Arrays.copyOfRange(all, 1, all.length));
            endpoint.receivePacket(packet);
        });
        return new Object[0];
    }

    @Override
    public void receivePacket(Packet packet) {
        final int distance = 0;
        final Object[] data = packet.data();
        final Object[] signal = new Object[4 + data.length];
        signal[0] = "debug_message";
        signal[1] = packet.source();
        signal[2] = packet.port();
        signal[3] = (double) distance;
        System.arraycopy(data, 0, signal, 4, data.length);
        node.sendToReachable("computer.signal", signal);
    }

    @Override
    public String address() {
        return node != null ? node.address() : "debug";
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            DebugNetwork.add(this);
            if (remoteNodePosition.isPresent()) {
                final BlockPos pos = remoteNodePosition.get();
                remoteNode = findNode(BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ()));
                if (remoteNode.isPresent()) node.connect(remoteNode.get());
                else remoteNodePosition = Optional.empty();
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            DebugNetwork.remove(this);
            remoteNode.ifPresent(other -> other.disconnect(node));
        } else if (remoteNode.isPresent() && remoteNode.get() == node) {
            remoteNode = Optional.empty();
            remoteNodePosition = Optional.empty();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        access = AccessContext.loadData(nbt);
        if (nbt.contains(Settings.namespace + "remoteX")) {
            final int x = nbt.getInt(Settings.namespace + "remoteX");
            final int y = nbt.getInt(Settings.namespace + "remoteY");
            final int z = nbt.getInt(Settings.namespace + "remoteZ");
            remoteNodePosition = Optional.of(new BlockPos(x, y, z));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        access.ifPresent(a -> a.saveData(nbt));
        remoteNodePosition.ifPresent(pos -> {
            nbt.putInt(Settings.namespace + "remoteX", pos.getX());
            nbt.putInt(Settings.namespace + "remoteY", pos.getY());
            nbt.putInt(Settings.namespace + "remoteZ", pos.getZ());
        });
    }

    // ----------------------------------------------------------------------- //

    public static void checkAccess(Optional<AccessContext> ctx) throws Exception {
        final Optional<String> msg = Settings.get().debugCardAccess.checkAccess(ctx);
        if (msg.isPresent()) throw new Exception(msg.get());
    }

    /** Formerly a Scala case class; {@code player} and {@code nonce} are public fields. */
    public static final class AccessContext {
        public final String player;
        public final String nonce;

        public AccessContext(String player, String nonce) {
            this.player = player;
            this.nonce = nonce;
        }

        public static void remove(CompoundTag nbt) {
            nbt.remove(Settings.namespace + "player");
            nbt.remove(Settings.namespace + "accessNonce");
        }

        public static Optional<AccessContext> loadData(CompoundTag nbt) {
            if (nbt.contains(Settings.namespace + "player"))
                return Optional.of(new AccessContext(
                        nbt.getString(Settings.namespace + "player"),
                        nbt.getString(Settings.namespace + "accessNonce")
                ));
            else
                return Optional.empty();
        }

        public void saveData(CompoundTag nbt) {
            nbt.putString(Settings.namespace + "player", player);
            nbt.putString(Settings.namespace + "accessNonce", nonce);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AccessContext other && Objects.equals(player, other.player) && Objects.equals(nonce, other.nonce);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player, nonce);
        }

        @Override
        public String toString() {
            return "AccessContext(" + player + "," + nonce + ")";
        }
    }

    private static Item checkItem(String id) {
        final ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
            throw new IllegalArgumentException("invalid item id");
        }
        return BuiltInRegistries.ITEM.get(location);
    }

    public static class PlayerValue extends AbstractValue {
        public String name;
        public Optional<AccessContext> ctx;

        public PlayerValue(String name, Optional<AccessContext> ctx) {
            this.name = name;
            this.ctx = ctx;
        }

        public PlayerValue() {
            this("", Optional.empty()); // For loading.
        }

        // ----------------------------------------------------------------------- //

        public Object[] withPlayer(Function<ServerPlayer, Object[]> f) throws Exception {
            checkAccess(ctx);
            final ServerPlayer player = GameInstance.getServer().getPlayerList().getPlayerByName(name);
            if (player != null) return f.apply(player);
            else return result(null, "player is offline");
        }

        @Callback(doc = "function():userdata -- Get the player's world object.")
        public Object[] getWorld(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(new WorldValue(player.level(), ctx)));
        }

        @Callback(doc = "function():string -- Get the player's game type.")
        public Object[] getGameType(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(player.gameMode.getGameModeForPlayer().getName()));
        }

        @Callback(doc = "function(gametype:string) -- Set the player's game type (survival, creative, adventure).")
        public Object[] setGameType(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                final String gametype = args.checkString(0);
                player.setGameMode(GameType.byName(gametype, GameType.SURVIVAL));
                return null;
            });
        }

        @Callback(doc = "function():number, number, number -- Get the player's position.")
        public Object[] getPosition(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(player.getX(), player.getY(), player.getZ()));
        }

        @Callback(doc = "function(x:number, y:number, z:number) -- Set the player's position.")
        public Object[] setPosition(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                player.teleportTo(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2));
                return null;
            });
        }

        @Callback(doc = "function():number -- Get the player's health.")
        public Object[] getHealth(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(player.getHealth()));
        }

        @Callback(doc = "function():number -- Get the player's max health.")
        public Object[] getMaxHealth(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(player.getMaxHealth()));
        }

        @Callback(doc = "function(health:number) -- Set the player's health.")
        public Object[] setHealth(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                player.setHealth((float) args.checkDouble(0));
                return null;
            });
        }

        @Callback(doc = "function():number -- Get the player's level")
        public Object[] getLevel(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(player.experienceLevel));
        }

        @Callback(doc = "function():number -- Get the player's total experience")
        public Object[] getExperienceTotal(Context context, Arguments args) throws Exception {
            return withPlayer(player -> result(player.totalExperience));
        }

        @Callback(doc = "function(level:number) -- Add a level to the player's experience level")
        public Object[] addExperienceLevel(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                player.giveExperienceLevels(args.checkInteger(0));
                return null;
            });
        }

        @Callback(doc = "function(level:number) -- Remove a level from the player's experience level")
        public Object[] removeExperienceLevel(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                player.giveExperienceLevels(-args.checkInteger(0));
                return null;
            });
        }

        @Callback(doc = "function() -- Clear the players inventory")
        public Object[] clearInventory(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                player.getInventory().clearContent();
                return null;
            });
        }

        @Deprecated
        @Callback(doc = "function(id:string, amount:number, meta:number[, nbt:string]):number -- Adds the item stack to the players inventory")
        public Object[] insertItem(Context context, Arguments args) throws Exception {
            return withPlayer(player -> {
                final Item item = checkItem(args.checkString(0));
                final int amount = args.checkInteger(1);
                args.checkInteger(2); // meta
                final String tagJson = args.checkString(3);
                final CompoundTag tag;
                try {
                    tag = Strings.isNullOrEmpty(tagJson) ? null : TagParser.parseTag(tagJson);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
                final ItemStack stack = new ItemStack(item, amount);
                stack.setTag(tag);
                InventoryUtils.addToPlayerInventory(stack, player);
                return result((Object) null);
            });
        }

        // ----------------------------------------------------------------------- //

        private static final String NameTag = "name";

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            ctx = AccessContext.loadData(nbt);
            name = nbt.getString(NameTag);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            ctx.ifPresent(c -> c.saveData(nbt));
            nbt.putString(NameTag, name);
        }
    }

    public static class ScoreboardValue extends AbstractValue {
        public Optional<AccessContext> ctx;
        public Scoreboard scoreboard;
        public ResourceLocation dimension;

        public ScoreboardValue(Optional<Level> world, Optional<AccessContext> ctx) {
            this.ctx = ctx;
            this.scoreboard = world.map(Level::getScoreboard).orElse(null);
            this.dimension = world.map(Level::dimension).orElse(Level.OVERWORLD).location();
        }

        public ScoreboardValue() {
            this(Optional.empty(), Optional.empty()); // For loading.
        }

        @Callback(doc = "function(team:string) - Add a team to the scoreboard")
        public Object[] addTeam(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String team = args.checkString(0);
            scoreboard.addPlayerTeam(team);
            return null;
        }

        @Callback(doc = "function(teamName: string) - Remove a team from the scoreboard")
        public Object[] removeTeam(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String teamName = args.checkString(0);
            final PlayerTeam team = scoreboard.getPlayersTeam(teamName);
            scoreboard.removePlayerTeam(team);
            return null;
        }

        @Callback(doc = "function(player:string, team:string):boolean - Add a player to a team")
        public Object[] addPlayerToTeam(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String player = args.checkString(0);
            final String teamName = args.checkString(1);
            final PlayerTeam team = scoreboard.getPlayersTeam(teamName);
            return result(scoreboard.addPlayerToTeam(player, team));
        }

        @Callback(doc = "function(player:string):boolean - Remove a player from their team")
        public Object[] removePlayerFromTeams(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String player = args.checkString(0);
            return result(scoreboard.removePlayerFromTeam(player));
        }

        @Callback(doc = "function(player:string, team:string):boolean - Remove a player from a specific team")
        public Object[] removePlayerFromTeam(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String player = args.checkString(0);
            final String teamName = args.checkString(1);
            final PlayerTeam team = scoreboard.getPlayersTeam(teamName);
            scoreboard.removePlayerFromTeam(player, team);
            return null;
        }

        @Callback(doc = "function(objectiveName:string, objectiveCriteria:string) - Create a new objective for the scoreboard")
        public Object[] addObjective(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String objName = args.checkString(0);
            final String objType = args.checkString(1);
            final ObjectiveCriteria criteria = ObjectiveCriteria.byName(objType).orElseThrow(() -> new IllegalArgumentException("invalid criterion"));
            scoreboard.addObjective(objName, criteria, Component.literal(objName), ObjectiveCriteria.RenderType.INTEGER);
            return null;
        }

        @Callback(doc = "function(objectiveName:string) - Remove an objective from the scoreboard")
        public Object[] removeObjective(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String objName = args.checkString(0);
            final Objective objective = scoreboard.getObjective(objName);
            scoreboard.removeObjective(objective);
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Sets the score of a player for a certain objective")
        public Object[] setPlayerScore(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String name = args.checkString(0);
            final Objective objective = scoreboard.getObjective(args.checkString(1));
            final int scoreVal = args.checkInteger(2);
            final Score score = scoreboard.getOrCreatePlayerScore(name, objective);
            score.setScore(scoreVal);
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string):int - Gets the score of a player for a certain objective")
        public Object[] getPlayerScore(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String name = args.checkString(0);
            final Objective objective = scoreboard.getObjective(args.checkString(1));
            final Score score = scoreboard.getOrCreatePlayerScore(name, objective);
            return result(score.getScore());
        }

        @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Increases the score of a player for a certain objective")
        public Object[] increasePlayerScore(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String name = args.checkString(0);
            final Objective objective = scoreboard.getObjective(args.checkString(1));
            final int scoreVal = args.checkInteger(2);
            final Score score = scoreboard.getOrCreatePlayerScore(name, objective);
            score.add(scoreVal);
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Decrease the score of a player for a certain objective")
        public Object[] decreasePlayerScore(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final String name = args.checkString(0);
            final Objective objective = scoreboard.getObjective(args.checkString(1));
            final int scoreVal = args.checkInteger(2);
            final Score score = scoreboard.getOrCreatePlayerScore(name, objective);
            score.add(-scoreVal);
            return null;
        }

        // ----------------------------------------------------------------------- //

        private static final String DimensionTag = "dimension";

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            ctx = AccessContext.loadData(nbt);
            dimension = new ResourceLocation(nbt.getString(DimensionTag));
            final ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimension);
            scoreboard = GameInstance.getServer().getLevel(dimKey).getScoreboard();
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            ctx.ifPresent(c -> c.saveData(nbt));
            nbt.putString(DimensionTag, dimension.toString());
        }
    }

    public static class WorldValue extends AbstractValue {
        public Level world;
        public Optional<AccessContext> ctx;

        public WorldValue(Level world, Optional<AccessContext> ctx) {
            this.world = world;
            this.ctx = ctx;
        }

        public WorldValue() {
            this(null, Optional.empty()); // For loading.
        }

        // ----------------------------------------------------------------------- //

        @Deprecated
        @Callback(doc = "function():number -- Gets the numeric id of the current dimension.")
        public Object[] getDimensionId(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final ResourceKey<Level> dimension = world.dimension();
            if (dimension == Level.OVERWORLD) return result(0);
            else if (dimension == Level.NETHER) return result(-1);
            else if (dimension == Level.END) return result(1);
            else throw new Error("deprecated");
        }

        @Deprecated
        @Callback(doc = "function():string -- Gets the name of the current dimension.")
        public Object[] getDimensionName(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.dimension().location().toString());
        }

        @Callback(doc = "function():string -- Gets the resource location of the current dimension.")
        public Object[] getDimension(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.dimension().location().toString());
        }

        @Callback(doc = "function():number -- Gets the seed of the world.")
        public Object[] getSeed(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(((ServerLevel) world).getSeed());
        }

        @Callback(doc = "function():boolean -- Returns whether it is currently raining.")
        public Object[] isRaining(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.isRaining());
        }

        @Callback(doc = "function(value:boolean) -- Sets whether it is currently raining.")
        public Object[] setRaining(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            world.getLevelData().setRaining(args.checkBoolean(0));
            return null;
        }

        @Callback(doc = "function():boolean -- Returns whether it is currently thundering.")
        public Object[] isThundering(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.isThundering());
        }

        @Callback(doc = "function(value:boolean) -- Sets whether it is currently thundering.")
        public Object[] setThundering(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            ((ServerLevelData) world.getLevelData()).setThundering(args.checkBoolean(0));
            return null;
        }

        @Callback(doc = "function():number -- Get the current world time.")
        public Object[] getTime(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.getDayTime());
        }

        @Callback(doc = "function(value:number) -- Set the current world time.")
        public Object[] setTime(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            ((ServerLevel) world).setDayTime((long) args.checkDouble(0));
            return null;
        }

        @Callback(doc = "function():number, number, number -- Get the current spawn point coordinates.")
        public Object[] getSpawnPoint(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.getLevelData().getXSpawn(), world.getLevelData().getYSpawn(), world.getLevelData().getZSpawn());
        }

        @Callback(doc = "function(x:number, y:number, z:number) -- Set the spawn point coordinates.")
        public Object[] setSpawnPoint(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final int x = args.checkInteger(0);
            final int y = args.checkInteger(1);
            final int z = args.checkInteger(2);
            ((ServerLevel) world).setDefaultSpawnPos(new BlockPos(x, y, z), world.getLevelData().getSpawnAngle());
            return null;
        }

        @Callback(doc = "function(x:number, y:number, z:number, sound:string, range:number) -- Play a sound at the specified coordinates.")
        public Object[] playSoundAt(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final int x = args.checkInteger(0);
            final int y = args.checkInteger(1);
            final int z = args.checkInteger(2);
            final String sound = args.checkString(3);
            final int range = args.checkInteger(4);
            PacketSender.sendSound(world, x, y, z, new ResourceLocation(sound), SoundSource.MASTER, range);
            return null;
        }

        // ----------------------------------------------------------------------- //

        private static BlockPos checkPos(Arguments args, int start) {
            return new BlockPos(args.checkInteger(start), args.checkInteger(start + 1), args.checkInteger(start + 2));
        }

        @Deprecated
        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.")
        public Object[] getBlockId(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final Block block = world.getBlockState(checkPos(args, 0)).getBlock();
            return result(BuiltInRegistries.BLOCK.getId(block));
        }

        @Deprecated
        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates.")
        public Object[] getMetadata(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            args.checkInteger(0);
            args.checkInteger(1);
            args.checkInteger(2);
            return result(0);
        }

        @Deprecated
        @Callback(doc = "function(x:number, y:number, z:number[, actualState:boolean=false]) - gets the block state for the block at the specified position, optionally getting additional display related data")
        public Object[] getBlockState(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final BlockState state = world.getBlockState(checkPos(args, 0));
            args.optBoolean(3, false); // actualState
            return result(state);
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.")
        public Object[] isLoaded(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.isLoaded(checkPos(args, 0)));
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates has a tile entity.")
        public Object[] hasTileEntity(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final BlockState state = world.getBlockState(checkPos(args, 0));
            return result(state.hasBlockEntity());
        }

        @Callback(doc = "function(x:number, y:number, z:number):table -- Get the NBT of the block at the specified coordinates.")
        public Object[] getTileNBT(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final BlockEntity blockEntity = world.getBlockEntity(checkPos(args, 0));
            if (blockEntity != null) return result(ExtendedNBT.toTypedMap(blockEntity.saveWithFullMetadata()));
            else return null;
        }

        @Callback(doc = "function(x:number, y:number, z:number, nbt:table):boolean -- Set the NBT of the block at the specified coordinates.")
        public Object[] setTileNBT(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final BlockPos blockPos = checkPos(args, 0);
            final BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity != null) {
                final Tag nbt = ExtendedNBT.typedMapToNbt(args.checkTable(3));
                if (nbt instanceof CompoundTag compound) {
                    blockEntity.load(compound);
                    blockEntity.setChanged();
                    ExtendedWorld.notifyBlockUpdate(world, blockPos);
                    return result(true);
                } else return result(null, "nbt tag COMPOUND expected, got '" + nbt.getType().getName() + "'");
            } else return result(null, "no tile entity");
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.")
        public Object[] getLightOpacity(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final BlockPos pos = checkPos(args, 0);
            final BlockState state = world.getBlockState(pos);
            return result(state.getLightBlock(world, pos));
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.")
        public Object[] getLightValue(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.getLightEmission(checkPos(args, 0)));
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.")
        public Object[] canSeeSky(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            return result(world.canSeeSkyFromBelowWater(checkPos(args, 0)));
        }

        @Deprecated
        private static BlockState getStateFromMeta(Block block, int meta) {
            final List<BlockState> states = block.getStateDefinition().getPossibleStates();
            if (meta >= 0 && meta < states.size()) return states.get(meta);
            else return block.defaultBlockState();
        }

        private static Block checkBlock(Arguments args, int index) {
            if (args.isInteger(index)) return BuiltInRegistries.BLOCK.byId(args.checkInteger(index));
            else return BuiltInRegistries.BLOCK.get(new ResourceLocation(args.checkString(index)));
        }

        @Deprecated
        @Callback(doc = "function(x:number, y:number, z:number, id:number or string, meta:number):number -- Set the block at the specified coordinates.")
        public Object[] setBlock(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final Block block = checkBlock(args, 3);
            final int metadata = args.checkInteger(4);
            return result(world.setBlockAndUpdate(checkPos(args, 0), getStateFromMeta(block, metadata)));
        }

        @Deprecated
        @Callback(doc = "function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number or string, meta:number):number -- Set all blocks in the area defined by the two corner points (x1, y1, z1) and (x2, y2, z2).")
        public Object[] setBlocks(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final int xMin = args.checkInteger(0), yMin = args.checkInteger(1), zMin = args.checkInteger(2);
            final int xMax = args.checkInteger(3), yMax = args.checkInteger(4), zMax = args.checkInteger(5);
            // Note: the 1.16 code read the block from argument 3 (x2); the documented position is 6.
            final Block block = checkBlock(args, 6);
            final int metadata = args.checkInteger(7);
            final BlockState state = getStateFromMeta(block, metadata);
            for (int x = Math.min(xMin, xMax); x <= Math.max(xMin, xMax); x++) {
                for (int y = Math.min(yMin, yMax); y <= Math.max(yMin, yMax); y++) {
                    for (int z = Math.min(zMin, zMax); z <= Math.max(zMin, zMax); z++) {
                        world.setBlockAndUpdate(new BlockPos(x, y, z), state);
                    }
                }
            }
            return null;
        }

        // ----------------------------------------------------------------------- //

        @Deprecated
        @Callback(doc = "function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean - Insert an item stack into the inventory at the specified location. NBT tag is expected in JSON format.")
        public Object[] insertItem(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final Item item = checkItem(args.checkString(0));
            final int count = args.checkInteger(1);
            final int damage = args.checkInteger(2);
            final String tagJson = args.optString(3, "");
            final CompoundTag tag = Strings.isNullOrEmpty(tagJson) ? null : TagParser.parseTag(tagJson);
            final BlockPosition position = BlockPosition.apply(args.checkDouble(4), args.checkDouble(5), args.checkDouble(6), world);
            final Direction side = ExtendedArguments.checkSideAny(args, 7);
            final Optional<ItemHandler> inventory = InventoryUtils.inventoryAt(position, side);
            if (inventory.isPresent()) {
                final ItemStack stack = new ItemStack(item, count);
                stack.setTag(tag);
                stack.setDamageValue(damage);
                return result(InventoryUtils.insertIntoInventory(stack, inventory.get()));
            } else return result(null, "no inventory");
        }

        @Callback(doc = "function(x:number, y:number, z:number, slot:number[, count:number]):number - Reduce the size of an item stack in the inventory at the specified location.")
        public Object[] removeItem(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final BlockPosition position = BlockPosition.apply(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2), world);
            final Optional<ItemHandler> inventory = InventoryUtils.anyInventoryAt(position);
            if (inventory.isPresent()) {
                final int slot = ExtendedArguments.checkSlot(args, inventory.get(), 3);
                final int count = args.optInteger(4, 64);
                final ItemStack removed = inventory.get().extractItem(slot, count, false);
                if (removed.isEmpty()) return result(0);
                else return result(removed.getCount());
            } else return result(null, "no inventory");
        }

        private Optional<FluidHandler> fluidHandlerAt(BlockPosition position, Direction side) {
            final BlockPos pos = position.toBlockPos();
            if (world.getBlockEntity(pos) instanceof FluidHandler handler) return Optional.of(handler);
            // Formerly only block entities implementing Forge's IFluidHandler directly were supported.
            return Optional.ofNullable(PlatformHooks.getFluidHandler(world, pos, side));
        }

        @Callback(doc = "function(id:string, amount:number, x:number, y:number, z:number, side:number):boolean - Insert some fluid into the tank at the specified location.")
        public Object[] insertFluid(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final ResourceLocation fluidId = ResourceLocation.tryParse(args.checkString(0));
            if (fluidId == null || !BuiltInRegistries.FLUID.containsKey(fluidId)) {
                throw new IllegalArgumentException("invalid fluid id");
            }
            final Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            final int amount = args.checkInteger(1);
            final BlockPosition position = BlockPosition.apply(args.checkDouble(2), args.checkDouble(3), args.checkDouble(4), world);
            final Direction side = ExtendedArguments.checkSideAny(args, 5);
            final Optional<FluidHandler> handler = fluidHandlerAt(position, side);
            if (handler.isPresent()) return result(handler.get().fill(FluidStack.create(fluid, amount), false));
            else return result(null, "no tank");
        }

        @Callback(doc = "function(amount:number, x:number, y:number, z:number, side:number):boolean - Remove some fluid from a tank at the specified location.")
        public Object[] removeFluid(Context context, Arguments args) throws Exception {
            checkAccess(ctx);
            final int amount = args.checkInteger(0);
            final BlockPosition position = BlockPosition.apply(args.checkDouble(1), args.checkDouble(2), args.checkDouble(3), world);
            final Direction side = ExtendedArguments.checkSideAny(args, 4);
            final Optional<FluidHandler> handler = fluidHandlerAt(position, side);
            if (handler.isPresent()) return result(handler.get().drain(amount, false));
            else return result(null, "no tank");
        }

        // ----------------------------------------------------------------------- //

        private static final String DimensionTag = "dimension";

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            ctx = AccessContext.loadData(nbt);
            final ResourceLocation dimension = new ResourceLocation(nbt.getString(DimensionTag));
            final ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimension);
            world = GameInstance.getServer().getLevel(dimKey);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            ctx.ifPresent(c -> c.saveData(nbt));
            nbt.putString(DimensionTag, world.dimension().location().toString());
        }
    }

    public static class TestValue extends AbstractValue {
        public String value = "hello";

        @Override
        public Object apply(Context context, Arguments arguments) {
            OpenComputers.log.info("TestValue.apply(" + joined(arguments) + ")");
            return value;
        }

        @Override
        public void unapply(Context context, Arguments arguments) {
            OpenComputers.log.info("TestValue.unapply(" + joined(arguments) + ")");
            value = arguments.checkString(1);
        }

        @Override
        public Object[] call(Context context, Arguments arguments) {
            OpenComputers.log.info("TestValue.call(" + joined(arguments) + ")");
            return result(arguments.toArray());
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            OpenComputers.log.info("TestValue.dispose()");
        }

        private static String joined(Arguments arguments) {
            final List<String> parts = new ArrayList<>();
            for (Object o : arguments.toArray()) parts.add(String.valueOf(o));
            return String.join(", ", parts);
        }

        private static final String ValueTag = "value";

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            value = nbt.getString(ValueTag);
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            nbt.putString(ValueTag, value);
        }
    }
}
