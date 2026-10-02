package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.HoverBootsData;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.ItemColorizer;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Hover boots. This cannot extend {@link SimpleItem} (it must be an {@link ArmorItem}),
 * so the bits of SimpleItem it used (description id, tooltip) are replicated here.
 * <p>
 * Client hooks: Forge's {@code getArmorTexture} / armor model are provided by the item
 * created in {@code li.cil.oc.common.platform.forge.ItemPlatformImpl#createHoverBoots}, which
 * uses {@link #ARMOR_TEXTURE} and {@link #armorModel}; Fabric's client registers an
 * {@code ArmorRenderer} itself.
 */
public class HoverBoots extends ArmorItem implements Chargeable {
    /** Formerly {@code HoverBootRenderer.texture}. */
    public static final ResourceLocation ARMOR_TEXTURE = new ResourceLocation(Settings.resourceDomain, "textures/model/drone.png");

    /**
     * Client hook: returns the {@code HumanoidModel} to render the boots with for the given
     * stack (the client sets the light color from {@link ItemColorizer} / 0x66DD55 like the old
     * {@code getArmorModel} did). Null on the server.
     */
    public static volatile Function<ItemStack, Object> armorModel;

    @Deprecated
    protected String unlocalizedName = "hoverboots"; // Explicit, the Forge item is an anonymous subclass.

    public HoverBoots(Properties props) {
        super(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, props);
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return "item.oc." + unlocalizedName;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        // SimpleItem tooltip (tooltipName = unlocalizedName, no data).
        for (String curr : Tooltip.get(unlocalizedName)) {
            tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
        }
        SimpleItem.appendNodeAddress(stack, tooltip);
    }

    // ----------------------------------------------------------------------- //
    // Chargeable

    @Override
    public double maxCharge(ItemStack stack) {
        return Settings.get().bufferHoverBoots;
    }

    @Override
    public double getCharge(ItemStack stack) {
        return new HoverBootsData(stack).charge;
    }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        final HoverBootsData data = new HoverBootsData(stack);
        data.charge = Math.min(maxCharge(stack), Math.max(0, amount));
        data.saveData(stack);
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        final HoverBootsData data = new HoverBootsData(stack);
        return Chargeable.applyCharge(amount, data.charge, Settings.get().bufferHoverBoots, used -> {
            if (!simulate) {
                data.charge += used;
                data.saveData(stack);
            }
        });
    }

    // ----------------------------------------------------------------------- //

    /** Formerly Forge's {@code getArmorTexture}; used by the Forge item subclass. */
    @Nullable
    public String armorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String subType) {
        if (entity.level().isClientSide) return ARMOR_TEXTURE.toString();
        else return null;
    }

    // Formerly Forge's onArmorTick: vanilla ticks armor stacks through inventoryTick too.
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof Player player && player.getItemBySlot(EquipmentSlot.FEET) == stack) {
            if (!Settings.get().ignorePower && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null && getCharge(stack) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
            }
        }
    }

    /**
     * Formerly {@code onEntityItemUpdate} (dropped boots in a water cauldron lost their color).
     * Vanilla has no item entity tick hook, so this is now a water cauldron interaction
     * (like washing leather armor). Registered by {@code Items} once the item exists.
     * TODO(port): dropped (item entity) washing is not supported anymore.
     */
    public static void registerCauldronInteraction(HoverBoots item) {
        CauldronInteraction.WATER.put(item, (state, level, pos, player, hand, stack) -> {
            if (!ItemColorizer.hasColor(stack)) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ItemColorizer.removeColor(stack);
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    // Formerly showDurabilityBar / getDurabilityForDisplay (always show energy bar).
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        final HoverBootsData data = new HoverBootsData(stack);
        return 1 - data.charge / Settings.get().bufferHoverBoots;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return SimpleItem.barWidth(getDurabilityForDisplay(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return SimpleItem.barColor(getDurabilityForDisplay(stack));
    }

    // This avoids actual damage value changing.
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    // Formerly Item.Properties#setNoRepair (Forge).
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairStack) {
        return false;
    }

    // TODO(port): Forge's getMaxDamage(stack) / isDamaged(stack) / setDamage(stack, damage) overrides
    //  (taking damage drained energy instead) have no vanilla equivalent.
}
