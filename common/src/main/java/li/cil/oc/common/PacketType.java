package li.cil.oc.common;

/**
 * Packet type ids of OpenComputers' own packet format. The id of a packet type
 * is its ordinal and is written as the first byte of the (possibly
 * compressed) packet payload, see {@link PacketBuilder}.
 */
public enum PacketType {
    // Server -> Client
    AdapterState,
    Analyze,
    ChargerState,
    ClientLog,
    ColorChange,
    ComputerState,
    ComputerUserList,
    ContainerUpdate,
    DisassemblerActiveChange,
    FileSystemActivity,
    FloppyChange,
    HologramArea,
    HologramClear,
    HologramColor,
    HologramPowerChange,
    HologramRotation,
    HologramRotationSpeed,
    HologramScale,
    HologramTranslation,
    HologramValues,
    LootDisk,
    CyclingDisk,
    NanomachinesConfiguration,
    NanomachinesInputs,
    NanomachinesPower,
    NetSplitterState,
    NetworkActivity,
    ParticleEffect,
    PetVisibility, // Goes both ways.
    PowerState,
    PrinterState,
    RackInventory,
    RackMountableData,
    RaidStateChange,
    RedstoneState,
    RobotAnimateSwing,
    RobotAnimateTurn,
    RobotAssemblingState,
    RobotInventoryChange,
    RobotLightChange,
    RobotMove,
    RobotNameChange,
    RobotSelectedSlotChange,
    RotatableState,
    SwitchActivity,
    TextBufferInit, // Goes both ways.
    TextBufferMulti,
    TextBufferRamInit,
    TextBufferBitBlt,
    TextBufferRamDestroy,
    TextBufferMultiColorChange,
    TextBufferMultiCopy,
    TextBufferMultiDepthChange,
    TextBufferMultiFill,
    TextBufferMultiPaletteChange,
    TextBufferMultiResolutionChange,
    TextBufferMultiViewportResolutionChange,
    TextBufferMultiMaxResolutionChange,
    TextBufferMultiSet,
    TextBufferMultiRawSetText,
    TextBufferMultiRawSetBackground,
    TextBufferMultiRawSetForeground,
    TextBufferPowerChange,
    ScreenTouchMode,
    SoundEffect,
    Sound,
    SoundPattern,
    TransposerActivity,
    WaypointLabel, // Goes both ways.

    // Client -> Server
    ComputerPower,
    CopyToAnalyzer,
    DriveLock,
    DriveMode,
    DronePower,
    KeyDown,
    KeyUp,
    TextInput,
    Clipboard,
    MachineItemStateRequest,
    MachineItemStateResponse,
    MouseClickOrDrag,
    MouseScroll,
    MouseUp,
    MultiPartPlace,
    RackMountableMapping,
    RackRelayState,
    RobotAssemblerStart,
    RobotStateRequest,
    ServerPower,

    EndOfList;

    private static final PacketType[] VALUES = values();

    public int id() {
        return ordinal();
    }

    /**
     * Equivalent of the Scala {@code PacketType(id)} lookup.
     */
    public static PacketType byId(int id) {
        if (id < 0 || id >= VALUES.length) throw new IllegalArgumentException("Invalid packet type id " + id);
        return VALUES[id];
    }
}
