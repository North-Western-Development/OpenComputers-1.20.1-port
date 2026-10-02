package li.cil.oc.common.entity;

import li.cil.oc.common.inventory.Inventory;

/**
 * Main inventory of a drone (was declared in Drone.scala).
 */
public abstract class DroneInventory implements Inventory {
    public final Drone drone;

    protected DroneInventory(Drone drone) {
        this.drone = drone;
    }
}
