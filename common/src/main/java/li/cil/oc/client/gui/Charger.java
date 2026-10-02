package li.cil.oc.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Charger extends DynamicGuiContainer<li.cil.oc.common.container.Charger> {
    public Charger(li.cil.oc.common.container.Charger state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }
}
