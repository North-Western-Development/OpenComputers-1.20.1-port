# server network / fs / packets — wave 2 report

- Channel common.PacketHandler.CHANNEL = opencomputers:main; payload = PacketBuilder bytes (compression flag, PacketType id, data).
- C2S: common.PacketHandler.registerServerReceiver() (Proxy calls server.PacketHandler.registerServerReceiver() — works via inheritance). S2C: common.PacketHandler.registerClientReceiver(client.PacketHandler.INSTANCE).
- common.PacketHandler abstract: world(Player, ResourceLocation) → Optional<Level>, dispatch(PacketParser). PacketParser implements DataInput, unchecked; readBlockEntity(Class)/readEntity(Class)/getBlockEntity(Class, dim,x,y,z) → Optional; readDirection() → Optional; PacketType enum id()/byId().
- PacketBuilder implements DataOutput unchecked; SimplePacketBuilder/CompressedPacketBuilder/PacketBuilderBase top-level. writeRegistryEntry writes key string.
- server.PacketSender static; Optional for Option; default args → overloads.
- server.ComponentTracker.INSTANCE; PetVisibility.hidden synchronized Set.
- Network.INSTANCE (NetworkAPI); Node/Component/Connector/ComponentConnector are interfaces with defaults, impls Network.NodeImpl/ComponentImpl/ConnectorImpl/ComponentConnectorImpl; setters setAddress, setNetwork, setVisibilityState, callbacks(), hosts(), setLocalBuffer, setLocalBufferSizeRaw, distributor()/setDistributor(Optional); Distributor setGlobalBuffer/Size.
- WirelessNetwork.register(), Waypoints.register().
- FileSystem.INSTANCE (FileSystemAPI); fromResource via Platform.getMod(ns).findResource + PathInputStreamFileSystem; fs traits → abstract classes; Capacity interface + Capacity.Tracker.

## Assumed members
Robot: proxy, animationTicksTotal, turnAxis, info.lightColor, name(), selectedSlot(); Hologram public fields; Rack lastData[], isRelayEnabled, connect(int,int,Optional<Direction>); Charger chargeSpeed/hasPower; NetSplitter isInverted; Screen isOrigin(); DroneInventory.drone; Drone.machine field; RobotAfterimage.findMovingRobot; ControllerImpl.saveData(CompoundTag).

## TODO(port)
Immibis check dropped; wireless/waypoint cleanup must happen in BE dispose(); Network.newPacket(nbt) inverted dest check kept (pre-existing bug).
