# server machine / driver / agent / loot — wave 2 report

- Registry.INSTANCE (instance members convert, blacklistHost, blacklist, locked...). Machine statics add/architectures/getArchitectureName/create/checked; Machine.INSTANCE is MachineAPI. Machine.State enum + `state` ArrayDeque public; Machine.Signal(name,args); tryClose(), addComponent/removeComponent, isExecuting(), lastError().
- Callbacks.apply(host)/clear()/fromClass(cls) → Map<String, Callbacks.Callback>; scans interfaces (class methods win). CallbackCall own file. ArgumentsImpl(List<Object>|Object[]).
- LuaStateFactory statics isAvailableStatic(), luajRequested(), include52/53(), includeLuaJ(), default53(), setDefaultArch(stack); Lua52.INSTANCE/Lua53.INSTANCE; createState() → Optional<LuaState>.
- agent.Player extends ServerPlayer (FakeNetworkManager); fields agent, facing, side, instantBreak; closestEntity → Optional; fireRightClickBlock/fireLeftClickBlock/fireRightClickAir → boolean (true = denied).
- ActivationType enum; LootFunctions.SET_COLOR/COPY_COLOR RegistrySuppliers, LootFunctions.init().
- New hook common.platform.AgentPlatform (+ forge/fabric Impl); mixins ServerPlayerGameModeAccessor, BlockPopExperienceMixin; client/GamePause.
- Natives from /assets/opencomputers/lib/<name> (fallback lib/); extracted to Platform.getGameFolder().
- Done: PlatformHooksImpl.isFakePlayer includes agent.Player.
- TODO(port): fake player item-handler caps dropped.
