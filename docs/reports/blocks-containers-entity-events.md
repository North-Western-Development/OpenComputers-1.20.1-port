# common block / container / inventory / entity / event — wave 2 report

- Block traits GUI (openGui(ServerPlayer, Level, BlockPos)), PowerAcceptor (energyThroughput()), StateAware are markers; behaviour in SimpleBlock. SimpleBlock.getTicker shared ticker → updateEntity() for Tickable traits.TileEntity.
- SimpleBlock.removedByPlayer(state, level, pos, player) veto via BlockBreakHandler (Architectury BlockEvent.BREAK) — now registered in common.Proxy.
- RedstoneAware.canConnectRedstone / Adapter.onNeighborChange keep Forge signatures (Forge-only).
- Print blockstate property Print.Light (0–15); Print.updateLightLevel(level, pos, value).
- Containers: container.Player extends AbstractContainerMenu, ctor (MenuType<?>, id, Inventory, Container, ...); RobotInfo top-level (readRobotInfo/writeRobotInfo); DynamicComponentSlot.containerTierGetter IntSupplier; ContainerTypes RegistrySuppliers via MenuRegistry.ofExtended + init(); openXGui via openExtendedMenu.
- Inventory interfaces; ComponentInventory.componentState() (weak-map default; override with field), isSizeInventoryReady()/setIsSizeInventoryReady(v); ItemStackInventory.items() (ItemsHolder). Implement getMaxStackSize() when mixing ComponentInventory + ItemStackInventory/ServerInventory.
- Drone: setTargetX/Y/Z, setTargetAcceleration, setGlobalBuffer, setGlobalBufferSize, setStatusText, setInventorySize, setLightColor; DroneInventory own class; containerProvider/ownerName/ownerUUID fields; disposal via setLevelCallback.
- ChunkloaderUpgradeHandler: setChunkForced + SavedData opencomputers_chunkloaders (owner UUID → centre chunk); TODO: overlapping /forceload chunks may be unforced.
- Client registrations NanomachinesHandler.Client.register() and RackMountableRenderHandler.register() — now in client.Proxy.initClient().
- Mixins: LivingEntityHoverBootsMixin, PlayerDataStorageMixin.
