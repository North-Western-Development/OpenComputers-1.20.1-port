package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.SignChangeEvent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.platform.ComponentPlatform;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Note: 1.20 signs have a front and a back side; the upgrade reads and writes the front text.
 */
public abstract class UpgradeSign extends AbstractManagedEnvironment implements DeviceInfo {
    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Sign upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Labelizer Deluxe"
            );
        }
        return deviceInfo;
    }

    public abstract EnvironmentHost host();

    private static final int LineCount = 4;

    private static String signText(SignBlockEntity sign) {
        final SignText text = sign.getFrontText();
        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < LineCount; i++) {
            lines.add(text.getMessage(i, false).getString());
        }
        return String.join("\n", lines);
    }

    protected Object[] getValue(Optional<SignBlockEntity> tileEntity) {
        if (tileEntity.isPresent()) return ResultWrapper.result(signText(tileEntity.get()));
        else return ResultWrapper.result(ResultWrapper.unit, "no sign");
    }

    protected Object[] setValue(Optional<SignBlockEntity> tileEntity, String text) {
        if (tileEntity.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "no sign");
        final SignBlockEntity sign = tileEntity.get();

        final Player player;
        if (host() instanceof Robot robot) player = robot.player();
        else player = ComponentPlatform.fakePlayer((ServerLevel) host().world(), Settings.get().fakePlayerProfile);

        final List<String> lineList = text.lines().collect(Collectors.toCollection(ArrayList::new));
        while (lineList.size() < LineCount) lineList.add("");
        final String[] lines = lineList.stream().map(line -> line.length() > 15 ? line.substring(0, 15) : line).toArray(String[]::new);

        if (!canChangeSign(player, sign, lines)) {
            return ResultWrapper.result(ResultWrapper.unit, "not allowed");
        }

        SignText signText = sign.getFrontText();
        for (int i = 0; i < LineCount; i++) {
            signText = signText.setMessage(i, Component.literal(lines[i]));
        }
        sign.setText(signText, true);
        ExtendedWorld.notifyBlockUpdate(host().world(), sign.getBlockPos());

        EventBus.INSTANCE.post(new SignChangeEvent.Post(sign, lines));

        return ResultWrapper.result(signText(sign));
    }

    protected Optional<SignBlockEntity> findSign(Direction side) {
        final BlockPosition hostPos = BlockPosition.apply(host());
        final BlockEntity tileEntity = ExtendedWorld.getBlockEntity(host().world(), hostPos);
        if (tileEntity instanceof SignBlockEntity sign) return Optional.of(sign);
        final BlockEntity neighbor = ExtendedWorld.getBlockEntity(host().world(), hostPos.offset(side));
        if (neighbor instanceof SignBlockEntity sign) return Optional.of(sign);
        return Optional.empty();
    }

    private boolean canChangeSign(Player player, SignBlockEntity tileEntity, String[] lines) {
        if (!host().world().mayInteract(player, tileEntity.getBlockPos())) {
            return false;
        }
        if (host().world() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (!PlatformHooks.canBreakBlock(serverLevel, tileEntity.getBlockPos(), serverPlayer)) {
                return false;
            }
        }

        final SignChangeEvent.Pre signEvent = new SignChangeEvent.Pre(tileEntity, lines);
        return !EventBus.INSTANCE.post(signEvent);
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof Machine machine && machine.host() instanceof Tablet) {
            final Object[] data = message.data();
            if (data.length == 8 &&
                data[0] instanceof CompoundTag nbt &&
                data[1] instanceof ItemStack &&
                data[2] instanceof Player &&
                data[3] instanceof BlockPosition blockPos &&
                data[4] instanceof Direction &&
                data[5] instanceof Float &&
                data[6] instanceof Float &&
                data[7] instanceof Float) {
                if (ExtendedWorld.getBlockEntity(host().world(), blockPos) instanceof SignBlockEntity sign) {
                    nbt.putString("signText", signText(sign));
                }
            }
        }
    }
}
