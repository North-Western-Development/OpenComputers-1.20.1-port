package li.cil.oc;

import li.cil.oc.util.ItemUtils;

public final class Constants {
    private Constants() {
    }

    public static final class BlockName {
        private BlockName() {
        }

        public static final String Adapter = "adapter";
        public static final String Assembler = "assembler";
        public static final String Cable = "cable";
        public static final String Capacitor = "capacitor";
        public static final String CarpetedCapacitor = "carpetedcapacitor";
        public static final String CaseCreative = "casecreative";
        public static final String CaseTier1 = "case1";
        public static final String CaseTier2 = "case2";
        public static final String CaseTier3 = "case3";
        public static final String ChameliumBlock = "chameliumblock";
        public static final String Charger = "charger";
        public static final String Disassembler = "disassembler";
        public static final String DiskDrive = "diskdrive";
        public static final String Endstone = "endstone";
        public static final String Geolyzer = "geolyzer";
        public static final String HologramTier1 = "hologram1";
        public static final String HologramTier2 = "hologram2";
        public static final String Keyboard = "keyboard";
        public static final String Microcontroller = "microcontroller";
        public static final String MotionSensor = "motionsensor";
        public static final String NetSplitter = "netsplitter";
        public static final String PowerConverter = "powerconverter";
        public static final String PowerDistributor = "powerdistributor";
        public static final String Print = "print";
        public static final String Printer = "printer";
        public static final String Raid = "raid";
        public static final String Redstone = "redstone";
        public static final String Relay = "relay";
        public static final String Robot = "robot";
        public static final String RobotAfterimage = "robotafterimage";
        public static final String ScreenTier1 = "screen1";
        public static final String ScreenTier2 = "screen2";
        public static final String ScreenTier3 = "screen3";
        public static final String Rack = "rack";
        public static final String Transposer = "transposer";
        public static final String Waypoint = "waypoint";

        public static String Case(int tier) {
            return ItemUtils.caseNameWithTierSuffix("case", tier);
        }
    }

    public static final class ItemName {
        private ItemName() {
        }

        public static final String AbstractBusCard = "abstractbuscard";
        public static final String Acid = "acid";
        public static final String Alu = "alu";
        public static final String Analyzer = "analyzer";
        public static final String AngelUpgrade = "angelupgrade";
        public static final String APUCreative = "apucreative";
        public static final String APUTier1 = "apu1";
        public static final String APUTier2 = "apu2";
        public static final String ArrowKeys = "arrowkeys";
        public static final String BatteryUpgradeTier1 = "batteryupgrade1";
        public static final String BatteryUpgradeTier2 = "batteryupgrade2";
        public static final String BatteryUpgradeTier3 = "batteryupgrade3";
        public static final String ButtonGroup = "buttongroup";
        public static final String Card = "card";
        public static final String CardContainerTier1 = "cardcontainer1";
        public static final String CardContainerTier2 = "cardcontainer2";
        public static final String CardContainerTier3 = "cardcontainer3";
        public static final String Chamelium = "chamelium";
        public static final String ChipTier1 = "chip1";
        public static final String ChipTier2 = "chip2";
        public static final String ChipTier3 = "chip3";
        public static final String ChunkloaderUpgrade = "chunkloaderupgrade";
        public static final String CircuitBoard = "circuitboard";
        public static final String ComponentBusTier1 = "componentbus1";
        public static final String ComponentBusTier2 = "componentbus2";
        public static final String ComponentBusTier3 = "componentbus3";
        public static final String ComponentBusCreative = "componentbuscreative";
        public static final String CPUTier1 = "cpu1";
        public static final String CPUTier2 = "cpu2";
        public static final String CPUTier3 = "cpu3";
        public static final String CraftingUpgrade = "craftingupgrade";
        public static final String ControlUnit = "cu";
        public static final String CuttingWire = "cuttingwire";
        public static final String DatabaseUpgradeTier1 = "databaseupgrade1";
        public static final String DatabaseUpgradeTier2 = "databaseupgrade2";
        public static final String DatabaseUpgradeTier3 = "databaseupgrade3";
        public static final String DataCardTier1 = "datacard1";
        public static final String DataCardTier2 = "datacard2";
        public static final String DataCardTier3 = "datacard3";
        public static final String DebugCard = "debugcard";
        public static final String Debugger = "debugger";
        public static final String DiamondChip = "chipdiamond";
        public static final String Disk = "disk";
        public static final String DiskDriveMountable = "diskdrivemountable";
        public static final String Drone = "drone";
        public static final String DroneCaseCreative = "dronecasecreative";
        public static final String DroneCaseTier1 = "dronecase1";
        public static final String DroneCaseTier2 = "dronecase2";
        public static final String EEPROM = "eeprom";
        public static final String ExperienceUpgrade = "experienceupgrade";
        public static final String Floppy = "floppy";
        public static final String GeneratorUpgrade = "generatorupgrade";
        public static final String GraphicsCardTier1 = "graphicscard1";
        public static final String GraphicsCardTier2 = "graphicscard2";
        public static final String GraphicsCardTier3 = "graphicscard3";
        public static final String HDDTier1 = "hdd1";
        public static final String HDDTier2 = "hdd2";
        public static final String HDDTier3 = "hdd3";
        public static final String HoverBoots = "hoverboots";
        public static final String HoverUpgradeTier1 = "hoverupgrade1";
        public static final String HoverUpgradeTier2 = "hoverupgrade2";
        public static final String InkCartridgeEmpty = "inkcartridgeempty";
        public static final String InkCartridge = "inkcartridge";
        public static final String InternetCard = "internetcard";
        public static final String Interweb = "interweb";
        public static final String InventoryControllerUpgrade = "inventorycontrollerupgrade";
        public static final String InventoryUpgrade = "inventoryupgrade";
        public static final String LeashUpgrade = "leashupgrade";
        public static final String LinkedCard = "linkedcard";
        public static final String LuaBios = "luabios";
        public static final String MFU = "mfu";
        public static final String Manual = "manual";
        public static final String MicrocontrollerCaseCreative = "microcontrollercasecreative";
        public static final String MicrocontrollerCaseTier1 = "microcontrollercase1";
        public static final String MicrocontrollerCaseTier2 = "microcontrollercase2";
        public static final String Nanomachines = "nanomachines";
        public static final String NavigationUpgrade = "navigationupgrade";
        public static final String NetworkCard = "lancard";
        public static final String NumPad = "numpad";
        public static final String OpenOS = "openos";
        public static final String PistonUpgrade = "pistonupgrade";
        public static final String StickyPistonUpgrade = "stickypistonupgrade";
        public static final String Present = "present";
        public static final String PrintedCircuitBoard = "printedcircuitboard";
        public static final String RAMTier1 = "ram1";
        public static final String RAMTier2 = "ram2";
        public static final String RAMTier3 = "ram3";
        public static final String RAMTier4 = "ram4";
        public static final String RAMTier5 = "ram5";
        public static final String RAMTier6 = "ram6";
        public static final String RawCircuitBoard = "rawcircuitboard";
        public static final String RedstoneCardTier1 = "redstonecard1";
        public static final String RedstoneCardTier2 = "redstonecard2";
        public static final String ServerCreative = "servercreative";
        public static final String ServerTier1 = "server1";
        public static final String ServerTier2 = "server2";
        public static final String ServerTier3 = "server3";
        public static final String SignUpgrade = "signupgrade";
        public static final String SolarGeneratorUpgrade = "solargeneratorupgrade";
        public static final String Tablet = "tablet";
        public static final String TabletCaseCreative = "tabletcasecreative";
        public static final String TabletCaseTier1 = "tabletcase1";
        public static final String TabletCaseTier2 = "tabletcase2";
        public static final String TankControllerUpgrade = "tankcontrollerupgrade";
        public static final String TankUpgrade = "tankupgrade";
        public static final String Terminal = "terminal";
        public static final String TerminalServer = "terminalserver";
        public static final String TexturePicker = "texturepicker";
        public static final String TractorBeamUpgrade = "tractorbeamupgrade";
        public static final String TradingUpgrade = "tradingupgrade";
        public static final String Transistor = "transistor";
        public static final String UpgradeContainerTier1 = "upgradecontainer1";
        public static final String UpgradeContainerTier2 = "upgradecontainer2";
        public static final String UpgradeContainerTier3 = "upgradecontainer3";
        public static final String WirelessNetworkCardTier1 = "wlancard1";
        public static final String WirelessNetworkCardTier2 = "wlancard2";
        public static final String WorldSensorCard = "worldsensorcard";
        public static final String Wrench = "wrench";

        public static String DroneCase(int tier) {
            return ItemUtils.caseNameWithTierSuffix("dronecase", tier);
        }

        public static String MicrocontrollerCase(int tier) {
            return ItemUtils.caseNameWithTierSuffix("microcontrollercase", tier);
        }

        public static String TabletCase(int tier) {
            return ItemUtils.caseNameWithTierSuffix("tabletcase", tier);
        }
    }

    public static final class DeviceInfo {
        private DeviceInfo() {
        }

        public static final String DefaultVendor = "MightyPirates GmbH & Co. KG";
        public static final String Scummtech = "Scummtech, Inc.";

    }

}
