package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ToolDurabilityProviders;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;

public class Robot extends AbstractManagedEnvironment implements Agent, DeviceInfo {
    public final li.cil.oc.common.tileentity.Robot agent;

    public final Optional<ManagedEnvironment> romRobot;

    public Robot(li.cil.oc.common.tileentity.Robot agent) {
        this.agent = agent;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("robot").
            withConnector(Settings.get().bufferRobot).
            create());
        romRobot = Optional.ofNullable(FileSystem.asManagedEnvironment(FileSystem.
            fromResource(new ResourceLocation(Settings.resourceDomain, "lua/component/robot")), "robot"));
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    @Override
    public li.cil.oc.common.tileentity.Robot agent() {
        return agent;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Robot",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Caterpillar",
                DeviceAttribute.Capacity, String.valueOf(agent.getContainerSize())
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public Direction checkSideForAction(Arguments args, int n) {
        return agent.toGlobal(ExtendedArguments.checkSideForAction(args, n));
    }

    @Override
    public void onWorldInteraction(Context context, double duration) {
        Agent.super.onWorldInteraction(context, duration);
        agent.animateSwing(duration);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():number -- Get the current color of the activity light as an integer encoded RGB value (0xRRGGBB).")
    public Object[] getLightColor(Context context, Arguments args) {
        return ResultWrapper.result(agent.info.lightColor);
    }

    @Callback(doc = "function(value:number):number -- Set the color of the activity light to the specified integer encoded RGB value (0xRRGGBB).")
    public Object[] setLightColor(Context context, Arguments args) {
        agent.setLightColor(args.checkInteger(0));
        context.pause(0.1);
        return ResultWrapper.result(agent.info.lightColor);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():number -- Get the durability of the currently equipped tool.")
    public Object[] durability(Context context, Arguments args) {
        final ItemStack item = agent.equipmentInventory().getItem(0);
        if (!item.isEmpty()) {
            final Optional<Double> durability = ToolDurabilityProviders.getDurability(item);
            if (durability.isPresent()) return ResultWrapper.result(durability.get());
            else return ResultWrapper.result(ResultWrapper.unit, "tool cannot be damaged");
        }
        else return ResultWrapper.result(ResultWrapper.unit, "no tool equipped");
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(direction:number):boolean -- Move in the specified direction.")
    public Object[] move(Context context, Arguments args) {
        final Direction direction = agent.toGlobal(ExtendedArguments.checkSideForMovement(args, 0));
        if (agent.isAnimatingMove()) {
            // This shouldn't really happen due to delays being enforced, but just to
            // be on the safe side...
            return ResultWrapper.result(ResultWrapper.unit, "already moving");
        }
        else {
            final Pair<Boolean, String> content = blockContent(direction);
            final boolean something = content.getLeft();
            final String what = content.getRight();
            if (something) {
                context.pause(0.4);
                PacketSender.sendParticleEffect(BlockPosition.apply(agent), ParticleTypes.CRIT, 8, 0.25, Optional.of(direction));
                return ResultWrapper.result(ResultWrapper.unit, what);
            }
            else {
                if (!node().tryChangeBuffer(-Settings.get().robotMoveCost)) {
                    return ResultWrapper.result(ResultWrapper.unit, "not enough energy");
                }
                else if (agent.move(direction)) {
                    context.pause(Settings.get().moveDelay);
                    return ResultWrapper.result(true);
                }
                else {
                    node().changeBuffer(Settings.get().robotMoveCost);
                    context.pause(0.4);
                    PacketSender.sendParticleEffect(BlockPosition.apply(agent), ParticleTypes.CRIT, 8, 0.25, Optional.of(direction));
                    return ResultWrapper.result(ResultWrapper.unit, "impossible move");
                }
            }
        }
    }

    @Callback(doc = "function(clockwise:boolean):boolean -- Rotate in the specified direction.")
    public Object[] turn(Context context, Arguments args) {
        final boolean clockwise = args.checkBoolean(0);
        if (node().tryChangeBuffer(-Settings.get().robotTurnCost)) {
            if (clockwise) agent.rotate(Direction.UP);
            else agent.rotate(Direction.DOWN);
            agent.animateTurn(clockwise, Settings.get().turnDelay);
            context.pause(Settings.get().turnDelay);
            return ResultWrapper.result(true);
        }
        else {
            return ResultWrapper.result(ResultWrapper.unit, "not enough energy");
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            romRobot.ifPresent(fs -> {
                ((Component) fs.node()).setVisibility(Visibility.Network);
                node.connect(fs.node());
            });
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("network.message".equals(message.name()) && message.source() != agent.node()) {
            final Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof Packet packet) {
                agent.proxy.node().sendToReachable(message.name(), packet);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String RomRobotTag = "romRobot";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        romRobot.ifPresent(fs -> fs.loadData(nbt.getCompound(RomRobotTag)));
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        romRobot.ifPresent(fs -> ExtendedNBT.setNewCompoundTag(nbt, RomRobotTag, fs::saveData));
    }
}
