package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.server.component.LinkedCard;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class ConverterLinkedCard implements Converter {
    public static final ConverterLinkedCard INSTANCE = new ConverterLinkedCard();

    private ItemInfo linkedCard;

    private ConverterLinkedCard() {
    }

    public ItemInfo linkedCard() {
        if (linkedCard == null) linkedCard = Items.get(Constants.ItemName.LinkedCard);
        return linkedCard;
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack && Items.get(stack) == linkedCard()) {
            // Note: like on 1.16.5 this reports the default tunnel of a fresh card.
            final LinkedCard card = new LinkedCard();
            output.put("linkChannel", card.tunnel);
        }
    }
}
