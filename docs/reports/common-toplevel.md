# common top-level (Proxy, IMC, EventHandler, SaveHandler, Loot, ...) — wave 2 report

- Tier, Slot: static constants. InventorySlots: static arrays of InventorySlot (fields slot, tier); `switch` → `switchSlots`.
- IMC.INSTANCE implements IMCAPI; helpers handleMessage, getStaticMethod, tryInvokeStatic(Method, default, Object...), tryInvokeStaticVoid.
- ToolDurabilityProviders.getDurability(stack) → Optional<Double>.
- Sound: SOUNDS registry + init(); ComputerRunning, FloppyAccess, FloppyEject, FloppyInsert, HddAccess.
- ComponentTracker (abstract): add/remove/get(Optional)/onWorldUnload(LevelAccessor).
- EventHandler: register(), registerClient(); scheduleServer(BlockEntity|Runnable|(Runnable,int)), scheduleClient, scheduleWirelessRedstone, onRobotStart/Stopped, addKeyboard, scheduleClose/unscheduleClose(Machine), isItTime(). Mixin ChunkMapAccessor.
- SaveHandler: register(); savePath()/statePath() methods; scheduleSave(byte[]|Consumer<CompoundTag>); static savingForClients.
- Loot: register(), init(); disksForCycling(); randomDisk(RandomSource) → Optional.
- Proxy.preInit: TileEntityTypes, ContainerTypes, EntityTypes, RecipeSerializers, Sound, server.loot.LootFunctions init; server.PacketHandler.registerServerReceiver(); registers EventHandler, SaveHandler, Loot, server ComponentTracker, and event handlers' register(); API impls; API.imc; IMC.processPending(). init(): CreativeTab.instance, Loot/Achievement init, Mods.init(), isPowerEnabled. postInit(): lock driver registry. initClient(): EventHandler.registerClient().

## Assumed names other areas must provide
- server.PacketHandler.registerServerReceiver() (static)
- server.network.Network.INSTANCE, server.fs.FileSystem.INSTANCE, common.nanomachines.Nanomachines.INSTANCE, common.init.Items.INSTANCE (registerStack(stack, id))
- LuaStateFactory.isAvailableStatic(), luajRequested(), include52(), include53(), includeLuaJ()
- PacketSender.sendSound(Level,x,y,z,ResourceLocation,SoundSource,double), sendPetVisibility(Optional<String>, Optional<ServerPlayer>), sendLootDisks(ServerPlayer)
- client.Sound.startLoop(BlockEntity,String,float,long)/stopLoop; PetRenderer.isInitialized/hidden static fields
- Robot.machine(), isCreative(), canInteract(String); RobotProxy.robot field; traits.TileEntity.dispose(); item data `components`/`items` as ItemStack[] fields
- Event handlers with static register(): AngelUpgradeHandler, ChunkloaderUpgradeHandler, ExperienceUpgradeHandler, FileSystemAccessHandler, HoverBootsHandler, NetworkActivityHandler, RobotCommonHandler, WirelessNetworkCardHandler, NanomachinesHandler.Common

## Requests
- Integration: ModOpenComputers must not double-register handlers Proxy registers.
- Client: client.Proxy calls super.initClient(); hook client.ComponentTracker.INSTANCE.onWorldUnload; register NanomachinesHandler.Client, RackMountableRenderHandler.
- Entity: Drone closes machine on removal UNLOADED_TO_CHUNK via EventHandler.scheduleClose.

## TODO(port)
- Item charging via Chargeable capability dropped; missing-mapping remaps dropped; loot disks in dungeon chests (was disabled in 1.16.5); event priorities lost.
