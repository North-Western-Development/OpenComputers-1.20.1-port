# server/component L–Z — wave 2 report

## Cross-area issues
1. Callbacks.java must scan interfaces (sent to machine agent).
2. TankInventoryControl / InventoryWorldControlMk2 fakePlayer() default clash (sent to traits agent).
3. Chunkloader: must only unforce chunks OC forced; fake first ticket ChunkPos(0,0) problem. API: loader.host, loader.node(), field `ticket` (Optional<ChunkPos>), static claimTicket(String), releaseTicket(ServerLevel,String,ChunkPos), updateLoadedChunk(UpgradeChunkloader).

## Shapes
- Components call setNode(...) in ctor; node() overridden with narrower type. Ctor fields public final.
- RedstoneSignaller/RedstoneVanilla/RedstoneBundled abstract classes; wakeThreshold/wakeNeighborsOnly public fields + accessors; RedstoneVanilla.redstone() → RedstoneAware, redstoneHost(); RedstoneWireless interface (onWirelessConnect/Disconnect, load/saveWirelessData); Redstone.* ctors generic <T extends RedstoneAware & EnvironmentHost>.
- PistonTraits package-private; UpgradeStickyPiston own file; mixin PistonBaseBlockInvoker (oc$moveBlocks).
- TradeInfo own file (host Optional<EnvironmentHost>, merchant WeakReference<Merchant>, recipeID, merchantID).
- UpgradeTank implements common.transfer.FluidHandler; MultiTank.getFluidTank(i) should return the UpgradeTank itself.
- UpgradeGenerator.inventory is ItemStack.
- Server implements ServerInventory+ComponentInventory; lazy machine().
- UpgradeSign uses front SignText, ComponentPlatform.fakePlayer, EventBus SignChangeEvent.

## Assumed
- tileentity.Robot: info.lightColor, proxy fields; move, rotate, animateTurn, animateSwing, isAnimatingMove(), setLightColor. RobotProxy.robot field.
- BlockChangeHandler.addListener/removeListener, ChangeListener.onBlockChanged().
- ContainerTypes.openServerGui(ServerPlayer, ServerInventory, int).
