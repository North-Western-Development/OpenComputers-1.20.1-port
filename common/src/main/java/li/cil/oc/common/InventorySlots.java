package li.cil.oc.common;

public final class InventorySlots {
    public static final InventorySlot[][] computer = {
      {
        new InventorySlot(Slot.Card, Tier.One),
        new InventorySlot(Slot.Card, Tier.One),
        new InventorySlot(Slot.Memory, Tier.One),
        new InventorySlot(Slot.HDD, Tier.One),
        new InventorySlot(Slot.CPU, Tier.One),
        new InventorySlot(Slot.Memory, Tier.One),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      },

      {
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.Card, Tier.One),
        new InventorySlot(Slot.Memory, Tier.Two),
        new InventorySlot(Slot.Memory, Tier.Two),
        new InventorySlot(Slot.HDD, Tier.Two),
        new InventorySlot(Slot.HDD, Tier.One),
        new InventorySlot(Slot.CPU, Tier.Two),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      },

      {
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Two),
        new InventorySlot(Slot.Floppy, Tier.One),
        new InventorySlot(Slot.CPU, Tier.Three),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      },

      {
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.Floppy, Tier.One),
        new InventorySlot(Slot.CPU, Tier.Three),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      }
    };

    public static final InventorySlot[][] server = {
      {
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.CPU, Tier.Two),
        new InventorySlot(Slot.ComponentBus, Tier.Two),
        new InventorySlot(Slot.Memory, Tier.Two),
        new InventorySlot(Slot.Memory, Tier.Two),
        new InventorySlot(Slot.HDD, Tier.Two),
        new InventorySlot(Slot.HDD, Tier.Two),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      },

      {
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.CPU, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      },

      {
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.CPU, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.Card, Tier.Two),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      },

      {
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.CPU, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.ComponentBus, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.Memory, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.HDD, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.Card, Tier.Three),
        new InventorySlot(Slot.EEPROM, Tier.Any)
      }
    };

    public static final InventorySlot[] relay = {
      new InventorySlot(Slot.CPU, Tier.Three),
      new InventorySlot(Slot.Memory, Tier.Three),
      new InventorySlot(Slot.HDD, Tier.Three),
      new InventorySlot(Slot.Card, Tier.Three)
    };

    // Renamed from `switch` (Java keyword).
    public static final InventorySlot[] switchSlots = {
      new InventorySlot(Slot.CPU, Tier.Three),
      new InventorySlot(Slot.Memory, Tier.Three),
      new InventorySlot(Slot.HDD, Tier.Three)
    };

    public static final class InventorySlot {
        public final String slot;
        public final int tier;

        public InventorySlot(String slot, int tier) {
            this.slot = slot;
            this.tier = tier;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof InventorySlot other && other.tier == tier && other.slot.equals(slot);
        }

        @Override
        public int hashCode() {
            return slot.hashCode() * 31 + tier;
        }

        @Override
        public String toString() {
            return "InventorySlot(" + slot + "," + tier + ")";
        }
    }

    private InventorySlots() {
    }
}
