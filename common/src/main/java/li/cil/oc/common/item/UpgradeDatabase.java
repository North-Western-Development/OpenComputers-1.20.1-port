package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.DatabaseInventory;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UpgradeDatabase extends SimpleItem implements ItemTier {
    public final int tier;

    public UpgradeDatabase(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }

    @Override
    protected List<Object> tooltipData() {
        return Collections.singletonList(Settings.get().databaseEntriesPerTier[tier]);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (!player.isCrouching()) {
            if (!world.isClientSide && player instanceof ServerPlayer srvPlr) {
                ContainerTypes.openDatabaseGui(srvPlr, new DatabaseInventory() {
                    @Override
                    public ItemStack container() {
                        return stack;
                    }

                    @Override
                    public boolean stillValid(Player player) {
                        return player == srvPlr;
                    }
                });
            }
            player.swing(InteractionHand.MAIN_HAND);
        } else {
            stack.removeTagKey(Settings.namespace + "items");
            player.swing(InteractionHand.MAIN_HAND);
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }
}
