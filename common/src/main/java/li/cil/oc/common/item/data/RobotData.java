package li.cil.oc.common.item.data;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.integration.opencomputers.DriverScreen;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RobotData extends ItemData {
    public static final String[] names = loadNames();

    private static String[] loadNames() {
        try (InputStream stream = RobotData.class.getResourceAsStream("/assets/" + Settings.resourceDomain + "/robot.names");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            final List<String> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                final int comment = line.indexOf('#');
                final String name = (comment >= 0 ? line.substring(0, comment) : line).trim();
                if (!name.isEmpty()) result.add(name);
            }
            return result.toArray(new String[0]);
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed loading robot name list.", t);
            return new String[0];
        }
    }

    public static String randomName() {
        return names.length > 0 ? names[(int) (Math.random() * names.length)] : "Robot";
    }

    // ----------------------------------------------------------------------- //

    public RobotData() {
        super(Constants.BlockName.Robot);
    }

    public RobotData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public String name = "";

    // Overall energy including components.
    public int totalEnergy = 0;

    // Energy purely stored in robot component - this is what we have to restore manually.
    public int robotEnergy = 0;

    public int tier = 0;

    public ItemStack[] components = new ItemStack[0];

    public ItemStack[] containers = new ItemStack[0];

    public int lightColor = 0xF23030;

    private static final String StoredEnergyTag = Settings.namespace + "storedEnergy";
    private static final String RobotEnergyTag = Settings.namespace + "robotEnergy";
    private static final String TierTag = Settings.namespace + "tier";
    private static final String ComponentsTag = Settings.namespace + "components";
    private static final String ContainersTag = Settings.namespace + "containers";
    private static final String LightColorTag = Settings.namespace + "lightColor";

    @Override
    public void loadData(CompoundTag nbt) {
        name = ItemUtils.getDisplayName(nbt).orElse("");
        if (Strings.isNullOrEmpty(name)) {
            name = randomName();
        }
        totalEnergy = nbt.getInt(StoredEnergyTag);
        robotEnergy = nbt.getInt(RobotEnergyTag);
        tier = nbt.getInt(TierTag);
        components = Arrays.stream(ExtendedNBT.toTagArray(nbt.getList(ComponentsTag, Tag.TAG_COMPOUND), CompoundTag.class))
            .map(ItemStack::of).toArray(ItemStack[]::new);
        containers = Arrays.stream(ExtendedNBT.toTagArray(nbt.getList(ContainersTag, Tag.TAG_COMPOUND), CompoundTag.class))
            .map(ItemStack::of).toArray(ItemStack[]::new);
        if (nbt.contains(LightColorTag)) {
            lightColor = nbt.getInt(LightColorTag);
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        if (!Strings.isNullOrEmpty(name)) {
            ItemUtils.setDisplayName(nbt, name);
        }
        nbt.putInt(StoredEnergyTag, totalEnergy);
        nbt.putInt(RobotEnergyTag, robotEnergy);
        nbt.putInt(TierTag, tier);
        ExtendedNBT.setNewTagList(nbt, ComponentsTag, ExtendedNBT.itemStackIterableToNbt(Arrays.asList(components)));
        ExtendedNBT.setNewTagList(nbt, ContainersTag, ExtendedNBT.itemStackIterableToNbt(Arrays.asList(containers)));
        nbt.putInt(LightColorTag, lightColor);
    }

    public ItemStack copyItemStack() {
        final ItemStack stack = createItemStack();
        // Forget all node addresses and so on. This is used when 'picking' a
        // robot in creative mode.
        final RobotData newInfo = new RobotData(stack);
        for (ItemStack cs : newInfo.components) {
            final DriverItem driver = li.cil.oc.api.Driver.driverFor(cs);
            if (driver != null && driver == DriverScreen.INSTANCE) {
                final CompoundTag nbt = driver.dataTag(cs);
                for (String tagName : new ArrayList<>(nbt.getAllKeys())) {
                    nbt.remove(tagName);
                }
            }
        }
        // Don't show energy info (because it's unreliable) but fill up the
        // internal buffer. This is for creative use only, anyway.
        newInfo.totalEnergy = 0;
        newInfo.robotEnergy = 50000;
        newInfo.saveData(stack);
        return stack;
    }
}
