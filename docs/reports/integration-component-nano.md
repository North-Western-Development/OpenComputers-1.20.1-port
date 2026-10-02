# integration / common.component / nanomachines — wave 2 report

- Mods: static preInit()/init(); proxies ModPlatform/ModMinecraft/ModOpenComputers .INSTANCE; only Mods.Minecraft, Mods.OpenComputers, Mods.IDs.*.
- integration.minecraftforge → integration.platform (ModPlatform, DriverEnergyStorage.INSTANCE, ItemEnergyCharge, PowerAcceptorEnergyHandler(PowerAcceptor, Direction), Power.fromFE/toFE).
- Hook common.platform.IntegrationPlatform: getFakePlayer, canUseBlock, canAttackBlock (+ forge/fabric Impl). AW: BeaconBlockEntity levels.
- integration.minecraft: X.INSTANCE / X.Provider.INSTANCE; EventHandlerVanilla.register().
- integration.opencomputers: Item interface; Item.dataTagStatic(stack), Item.address(stack) → Optional<String>; DriverCPU non-final with INSTANCE; intersection casts for host types.
- common.component: no ComponentPackage; TextBufferProxy.data(); VideoRamDevice.internalBuffers(); VideoRamRasterizer.videoRamDevices(); ClientGpuTextBufferHandler own file; TextBuffer public data, viewport (Pair), proxy, host, static clientBuffers (self-registers client prune); TerminalServer buffer()/keyboard()/sidedKeys()/hasAddress()/address()/static loaded.
- Nanomachines.INSTANCE; ControllerImpl fields uuid, storedEnergy, configuration; damage via DamageSourceWithRandomCause.create(level, key, 3).

## Requests (verify in reconciliation)
- Static idempotent register(): common.item.Tablet, common.item.Analyzer, server.network.Waypoints, server.network.WirelessNetwork.
- DroneTemplate/MicrocontrollerTemplate/RobotTemplate/TabletTemplate .INSTANCE.register(); NavigationUpgradeTemplate/ServerTemplate/TemplateBlacklist static register().
- TextureImageProvider/ItemImageProvider/BlockImageProvider/OreDictImageProvider .INSTANCE; client.Textures.GUI.ManualHome.
- Tablet.Server.INSTANCE.cache, static Tablet.getId; TabletWrapper.player field; UpgradeBattery.tier field; common.item.Terminal; tileentity.Screen.tier, screens Set, invertTouchMode; UpgradeTractorBeam.Player(EnvironmentHost, Supplier<Player>); DatabaseInventory/ServerInventory anonymous-implementable with container() (+ stillValid / rackSlot()).
