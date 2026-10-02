package li.cil.oc;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import dev.architectury.platform.Platform;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.DebugCard;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Settings {
    public final Config config;


    // ----------------------------------------------------------------------- //
    // client
    public final double screenTextFadeStartDistance;
    public final double maxScreenTextRenderDistance;
    public final boolean textLinearFiltering;
    public final boolean textAntiAlias;
    public final boolean robotLabels;
    public final float soundVolume;
    public final double fontCharScale;
    public final double hologramFadeStartDistance;
    public final double hologramRenderDistance;
    public final double hologramFlickerFrequency;
    public final int monochromeColor;
    public final String fontRenderer;
    public final int beepSampleRate;
    public final int beepAmplitude;
    public final float beepRadius;
    public final Pair<Double, Double> nanomachineHudPos;
    public final boolean enableNanomachinePfx;

    // ----------------------------------------------------------------------- //
    // computer
    public final int threads;
    public final double timeout;
    public final double startupDelay;
    public final int eepromSize;
    public final int eepromDataSize;
    public final int[] cpuComponentSupport;
    public final double[] callBudgets;
    public final boolean canComputersBeOwned;
    public final int maxUsers;
    public final int maxUsernameLength;
    public final boolean eraseTmpOnReboot;
    public final int executionDelay;

    // computer.lua
    public final boolean allowBytecode;
    public final boolean allowGC;
    public final boolean enableLua53;
    public final boolean defaultLua53;
    public final int[] ramSizes;
    public final double ramScaleFor64Bit;
    public final int maxTotalRam;

    // ----------------------------------------------------------------------- //
    // robot
    public final boolean allowActivateBlocks;
    public final boolean allowUseItemsWithDuration;
    public final boolean canAttackPlayers;
    public final int limitFlightHeight;
    public final boolean screwCobwebs;
    public final double swingRange;
    public final double useAndPlaceRange;
    public final double itemDamageRate;
    public final String nameFormat;
    public final String uuidFormat;
    public final int[] upgradeFlightHeight;

    // robot.xp
    public final double baseXpToLevel;
    public final double constantXpGrowth;
    public final double exponentialXpGrowth;
    public final double robotActionXp;
    public final double robotExhaustionXpRate;
    public final double robotOreXpRate;
    public final double bufferPerLevel;
    public final double toolEfficiencyPerLevel;
    public final double harvestSpeedBoostPerLevel;

    // ----------------------------------------------------------------------- //
    // robot.delays

    // Note: all delays are reduced by one tick to account for the tick they are
    // performed in (since all actions are delegated to the server thread).
    public final double turnDelay;
    public final double moveDelay;
    public final double swingDelay;
    public final double useDelay;
    public final double placeDelay;
    public final double dropDelay;
    public final double suckDelay;
    public final double harvestRatio;

    // ----------------------------------------------------------------------- //
    // power
    public final boolean ignorePower;
    public final double tickFrequency;
    public final double chargeRateExternal;
    public final double chargeRateTablet;
    public final double generatorEfficiency;
    public final double solarGeneratorEfficiency;
    public final double assemblerTickAmount;
    public final double disassemblerTickAmount;
    public final double printerTickAmount;
    public final List<String> powerModBlacklist;

    // power.carpetedCapacitors
    public final double sheepPower;
    public final double ocelotPower;
    public final double carpetDamageChance;

    // power.buffer
    public final double bufferCapacitor;
    public final double bufferCapacitorAdjacencyBonus;
    public final double bufferComputer;
    public final double bufferRobot;
    public final double bufferConverter;
    public final double bufferDistributor;
    public final double[] bufferCapacitorUpgrades;
    public final double bufferTablet;
    public final double bufferAccessPoint;
    public final double bufferDrone;
    public final double bufferMicrocontroller;
    public final double bufferHoverBoots;
    public final double bufferNanomachines;

    // power.cost
    public final double computerCost;
    public final double microcontrollerCost;
    public final double robotCost;
    public final double droneCost;
    public final double sleepCostFactor;
    public final double screenCost;
    public final double hologramCost;
    public final double hddReadCost;
    public final double hddWriteCost;
    public final double gpuSetCost;
    public final double gpuFillCost;
    public final double gpuClearCost;
    public final double gpuCopyCost;
    public final double robotTurnCost;
    public final double robotMoveCost;
    public final double robotExhaustionCost;
    public final double[] wirelessCostPerRange;
    public final double abstractBusPacketCost;
    public final double geolyzerScanCost;
    public final double robotBaseCost;
    public final double robotComplexityCost;
    public final double microcontrollerBaseCost;
    public final double microcontrollerComplexityCost;
    public final double tabletBaseCost;
    public final double tabletComplexityCost;
    public final double droneBaseCost;
    public final double droneComplexityCost;
    public final double disassemblerItemCost;
    public final double chunkloaderCost;
    public final double pistonCost;
    public final double eepromWriteCost;
    public final double printCost;
    public final double hoverBootJump;
    public final double hoverBootAbsorb;
    public final double hoverBootMove;
    public final double dataCardTrivial;
    public final double dataCardTrivialByte;
    public final double dataCardSimple;
    public final double dataCardSimpleByte;
    public final double dataCardComplex;
    public final double dataCardComplexByte;
    public final double dataCardAsymmetric;
    public final double transposerCost;
    public final double nanomachineCost;
    public final double nanomachineReconfigureCost;
    public final double mfuCost;

    // power.rate
    public final double accessPointRate;
    public final double assemblerRate;
    public final double[] caseRate;
    // Creative case.
    public final double chargerRate;
    public final double disassemblerRate;
    public final double powerConverterRate;
    public final double serverRackRate;

    // power.value
    private final double valueAppliedEnergistics2;
    private final double valueFactorization;
    private final double valueGalacticraft;
    private final double valueIndustrialCraft2;
    private final double valueMekanism;
    private final double valuePowerAdvantage;
    private final double valueRedstoneFlux;
    private final double valueRotaryCraft;
    private final double valueForgeEnergy;

    private final int valueInternal;

    public final double ratioAppliedEnergistics2;
    public final double ratioFactorization;
    public final double ratioGalacticraft;
    public final double ratioIndustrialCraft2;
    public final double ratioMekanism;
    public final double ratioPowerAdvantage;
    public final double ratioRedstoneFlux;
    public final double ratioRotaryCraft;
    public final double ratioForgeEnergy;

    // ----------------------------------------------------------------------- //
    // filesystem
    public final int fileCost;
    public final boolean bufferChanges;
    public final int[] hddSizes;
    public final int[] hddPlatterCounts;
    public final int floppySize;
    public final int tmpSize;
    public final int maxHandles;
    public final int maxReadBuffer;
    public final int sectorSeekThreshold;
    public final double sectorSeekTime;

    // ----------------------------------------------------------------------- //
    // internet
    public final boolean httpEnabled;
    public final boolean httpHeadersEnabled;
    public final boolean tcpEnabled;
    public final AddressValidator[] httpHostBlacklist;
    public final AddressValidator[] httpHostWhitelist;
    public final int httpTimeout;
    public final int maxConnections;
    public final int internetThreads;

    // ----------------------------------------------------------------------- //
    // switch
    public final int switchDefaultMaxQueueSize;
    public final int switchQueueSizeUpgrade;
    public final int switchDefaultRelayDelay;
    public final double switchRelayDelayUpgrade;
    public final int switchDefaultRelayAmount;
    public final int switchRelayAmountUpgrade;

    // ----------------------------------------------------------------------- //
    // hologram
    public final double[] hologramMaxScaleByTier;
    public final double[] hologramMaxTranslationByTier;
    public final double hologramSetRawDelay;
    public final boolean hologramLight;

    // ----------------------------------------------------------------------- //
    // misc
    public final int maxScreenWidth;
    public final int maxScreenHeight;
    public final boolean inputUsername;
    public final int maxNetworkPacketSize;
    // Need at least 4 for nanomachine protocol. Because I can!
    public final int maxNetworkPacketParts;
    public final int[] maxOpenPorts;
    public final double[] maxWirelessRange;
    public final int rTreeMaxEntries;
    public final int terminalsPerServer;
    public final boolean updateCheck;
    public final int lootProbability;
    public final boolean lootRecrafting;
    public final int geolyzerRange;
    public final float geolyzerNoise;
    public final boolean disassembleAllTheThings;
    public final double disassemblerBreakChance;
    public final List<String> disassemblerInputBlacklist;
    public final boolean hideOwnPet;
    public final boolean allowItemStackInspection;
    public final int[] databaseEntriesPerTier;
    // Not configurable because of GUI design.
    public final double presentChance;
    public final List<String> assemblerBlacklist;
    public final int threadPriority;
    public final boolean giveManualToNewPlayers;
    public final int dataCardSoftLimit;
    public final int dataCardHardLimit;
    public final double dataCardTimeout;
    public final int serverRackSwitchTier;
    public final double redstoneDelay;
    public final double tradingRange;
    public final int mfuRange;

    // ----------------------------------------------------------------------- //
    // nanomachines
    public final double nanomachineTriggerQuota;
    public final double nanomachineConnectorQuota;
    public final int nanomachineMaxInputs;
    public final int nanomachineMaxOutputs;
    public final int nanomachinesSafeInputsActive;
    public final int nanomachinesMaxInputsActive;
    public final double nanomachinesCommandDelay;
    public final double nanomachinesCommandRange;
    public final double nanomachineMagnetRange;
    public final int nanomachineDisintegrationRange;
    public final List<? extends Object> nanomachinePotionWhitelist;
    public final float nanomachinesHungryDamage;
    public final double nanomachinesHungryEnergyRestored;

    // ----------------------------------------------------------------------- //
    // printer
    public final int maxPrintComplexity;
    public final double printRecycleRate;
    public final boolean chameliumEdible;
    public final int maxPrintLightLevel;
    public final int printCustomRedstone;
    public final int printMaterialValue;
    public final int printInkValue;
    public final boolean printsHaveOpacity;
    public final double noclipMultiplier;

    // chunkloader
    public final List<Integer> chunkloadDimensionBlacklist;
    public final List<Integer> chunkloadDimensionWhitelist;

    // ----------------------------------------------------------------------- //
    // integration
    public final List<String> modBlacklist;
    public final List<String> peripheralBlacklist;
    public final String fakePlayerUuid;
    public final String fakePlayerName;
    public final GameProfile fakePlayerProfile;

    // integration.vanilla
    public final boolean enableInventoryDriver;
    public final boolean enableTankDriver;
    public final boolean enableCommandBlockDriver;
    public final boolean allowItemStackNBTTags;

    // integration.buildcraft
    public final double costProgrammingTable;

    // ----------------------------------------------------------------------- //
    // debug
    public final boolean logLuaCallbackErrors;
    public final boolean forceLuaJ;
    public final boolean allowUserdata;
    public final boolean allowPersistence;
    public final boolean limitMemory;
    public final boolean forceCaseInsensitive;
    public final boolean logFullLibLoadErrors;
    public final String forceNativeLib;
    public final boolean logOpenGLErrors;
    public final boolean logHexFontErrors;
    public final boolean alwaysTryNative;
    public final boolean debugPersistence;
    public final boolean nativeInTmpDir;
    public final boolean periodicallyForceLightUpdate;
    public final boolean insertIdsInConverters;

    public final DebugCardAccess debugCardAccess;

    public final boolean registerLuaJArchitecture;
    public final boolean disableLocaleChanging;

    // >= 1.7.4
    public final int maxSignalQueueSize;

    // >= 1.7.6
    public final double[] vramSizes;

    public final double bitbltCost;

    public Settings(Config config) {
        this.config = config;

        // ----------------------------------------------------------------------- //
        // client
        screenTextFadeStartDistance = config.getDouble("client.screenTextFadeStartDistance");
        maxScreenTextRenderDistance = config.getDouble("client.maxScreenTextRenderDistance");
        textLinearFiltering = config.getBoolean("client.textLinearFiltering");
        textAntiAlias = config.getBoolean("client.textAntiAlias");
        robotLabels = config.getBoolean("client.robotLabels");
        soundVolume = Math.min(Math.max((float) config.getDouble("client.soundVolume"), 0f), 2f);
        fontCharScale = Math.min(Math.max(config.getDouble("client.fontCharScale"), 0.5), 2);
        hologramFadeStartDistance = Math.max(config.getDouble("client.hologramFadeStartDistance"), 0);
        hologramRenderDistance = Math.max(config.getDouble("client.hologramRenderDistance"), 0);
        hologramFlickerFrequency = Math.max(config.getDouble("client.hologramFlickerFrequency"), 0);
        monochromeColor = Integer.decode(config.getString("client.monochromeColor"));
        fontRenderer = config.getString("client.fontRenderer");
        beepSampleRate = config.getInt("client.beepSampleRate");
        beepAmplitude = Math.min(Math.max(config.getInt("client.beepVolume"), 0), Byte.MAX_VALUE);
        beepRadius = Math.min(Math.max((float) config.getDouble("client.beepRadius"), 1f), 32f);
        nanomachineHudPos = hudPos(config.getDoubleList("client.nanomachineHudPos"));
        enableNanomachinePfx = config.getBoolean("client.enableNanomachinePfx");

        // ----------------------------------------------------------------------- //
        // computer
        threads = Math.max(config.getInt("computer.threads"), 1);
        timeout = Math.max(config.getDouble("computer.timeout"), 0);
        startupDelay = Math.max(config.getDouble("computer.startupDelay"), 0.05);
        eepromSize = Math.max(config.getInt("computer.eepromSize"), 0);
        eepromDataSize = Math.max(config.getInt("computer.eepromDataSize"), 0);
        cpuComponentSupport = intArray(config.getIntList("computer.cpuComponentCount"), 4, "Bad number of CPU component counts, ignoring.", new int[]{8, 12, 16, 1024});
        callBudgets = doubleArray(config.getDoubleList("computer.callBudgets"), 3, "Bad number of call budgets, ignoring.", new double[]{0.5, 1.0, 1.5});
        canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned");
        maxUsers = Math.max(config.getInt("computer.maxUsers"), 0);
        maxUsernameLength = Math.max(config.getInt("computer.maxUsernameLength"), 0);
        eraseTmpOnReboot = config.getBoolean("computer.eraseTmpOnReboot");
        executionDelay = Math.max(config.getInt("computer.executionDelay"), 0);

        // computer.lua
        allowBytecode = config.getBoolean("computer.lua.allowBytecode");
        allowGC = config.getBoolean("computer.lua.allowGC");
        enableLua53 = config.getBoolean("computer.lua.enableLua53");
        defaultLua53 = config.getBoolean("computer.lua.defaultLua53");
        ramSizes = intArray(config.getIntList("computer.lua.ramSizes"), 6, "Bad number of RAM sizes, ignoring.", new int[]{192, 256, 384, 512, 768, 1024});
        ramScaleFor64Bit = Math.max(config.getDouble("computer.lua.ramScaleFor64Bit"), 1);
        maxTotalRam = Math.max(config.getInt("computer.lua.maxTotalRam"), 0);

        // ----------------------------------------------------------------------- //
        // robot
        allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks");
        allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration");
        canAttackPlayers = config.getBoolean("robot.canAttackPlayers");
        limitFlightHeight = Math.max(config.getInt("robot.limitFlightHeight"), 0);
        screwCobwebs = config.getBoolean("robot.notAfraidOfSpiders");
        swingRange = config.getDouble("robot.swingRange");
        useAndPlaceRange = config.getDouble("robot.useAndPlaceRange");
        itemDamageRate = Math.min(Math.max(config.getDouble("robot.itemDamageRate"), 0), 1);
        nameFormat = config.getString("robot.nameFormat");
        uuidFormat = config.getString("robot.uuidFormat");
        upgradeFlightHeight = intArray(config.getIntList("robot.upgradeFlightHeight"), 2, "Bad number of hover flight height counts, ignoring.", new int[]{64, 256});

        // robot.xp
        baseXpToLevel = Math.max(config.getDouble("robot.xp.baseValue"), 0);
        constantXpGrowth = Math.max(config.getDouble("robot.xp.constantGrowth"), 1);
        exponentialXpGrowth = Math.max(config.getDouble("robot.xp.exponentialGrowth"), 1);
        robotActionXp = Math.max(config.getDouble("robot.xp.actionXp"), 0);
        robotExhaustionXpRate = Math.max(config.getDouble("robot.xp.exhaustionXpRate"), 0);
        robotOreXpRate = Math.max(config.getDouble("robot.xp.oreXpRate"), 0);
        bufferPerLevel = Math.max(config.getDouble("robot.xp.bufferPerLevel"), 0);
        toolEfficiencyPerLevel = Math.max(config.getDouble("robot.xp.toolEfficiencyPerLevel"), 0);
        harvestSpeedBoostPerLevel = Math.max(config.getDouble("robot.xp.harvestSpeedBoostPerLevel"), 0);

        // ----------------------------------------------------------------------- //
        // robot.delays

        // Note: all delays are reduced by one tick to account for the tick they are
        // performed in (since all actions are delegated to the server thread).
        turnDelay = Math.max((config.getDouble("robot.delays.turn") - 0.06), 0.05);
        moveDelay = Math.max((config.getDouble("robot.delays.move") - 0.06), 0.05);
        swingDelay = Math.max((config.getDouble("robot.delays.swing") - 0.06), 0);
        useDelay = Math.max((config.getDouble("robot.delays.use") - 0.06), 0);
        placeDelay = Math.max((config.getDouble("robot.delays.place") - 0.06), 0);
        dropDelay = Math.max((config.getDouble("robot.delays.drop") - 0.06), 0);
        suckDelay = Math.max((config.getDouble("robot.delays.suck") - 0.06), 0);
        harvestRatio = Math.max(config.getDouble("robot.delays.harvestRatio"), 0);

        // ----------------------------------------------------------------------- //
        // power
        ignorePower = config.getBoolean("power.ignorePower");
        tickFrequency = Math.max(config.getDouble("power.tickFrequency"), 1);
        chargeRateExternal = config.getDouble("power.chargerChargeRate");
        chargeRateTablet = config.getDouble("power.chargerChargeRateTablet");
        generatorEfficiency = config.getDouble("power.generatorEfficiency");
        solarGeneratorEfficiency = config.getDouble("power.solarGeneratorEfficiency");
        assemblerTickAmount = Math.max(config.getDouble("power.assemblerTickAmount"), 1);
        disassemblerTickAmount = Math.max(config.getDouble("power.disassemblerTickAmount"), 1);
        printerTickAmount = Math.max(config.getDouble("power.printerTickAmount"), 1);
        powerModBlacklist = config.getStringList("power.modBlacklist");

        // power.carpetedCapacitors
        sheepPower = Math.max(config.getDouble("power.carpetedCapacitors.sheepPower"), 0);
        ocelotPower = Math.max(config.getDouble("power.carpetedCapacitors.ocelotPower"), 0);
        carpetDamageChance = Math.min(Math.max(config.getDouble("power.carpetedCapacitors.damageChance"), 0), 1.0);

        // power.buffer
        bufferCapacitor = Math.max(config.getDouble("power.buffer.capacitor"), 0);
        bufferCapacitorAdjacencyBonus = Math.max(config.getDouble("power.buffer.capacitorAdjacencyBonus"), 0);
        bufferComputer = Math.max(config.getDouble("power.buffer.computer"), 0);
        bufferRobot = Math.max(config.getDouble("power.buffer.robot"), 0);
        bufferConverter = Math.max(config.getDouble("power.buffer.converter"), 0);
        bufferDistributor = Math.max(config.getDouble("power.buffer.distributor"), 0);
        bufferCapacitorUpgrades = doubleArray(config.getDoubleList("power.buffer.batteryUpgrades"), 3, "Bad number of battery upgrade buffer sizes, ignoring.", new double[]{10000.0, 15000.0, 20000.0});
        bufferTablet = Math.max(config.getDouble("power.buffer.tablet"), 0);
        bufferAccessPoint = Math.max(config.getDouble("power.buffer.accessPoint"), 0);
        bufferDrone = Math.max(config.getDouble("power.buffer.drone"), 0);
        bufferMicrocontroller = Math.max(config.getDouble("power.buffer.mcu"), 0);
        bufferHoverBoots = Math.max(config.getDouble("power.buffer.hoverBoots"), 1);
        bufferNanomachines = Math.max(config.getDouble("power.buffer.nanomachines"), 0);

        // power.cost
        computerCost = Math.max(config.getDouble("power.cost.computer"), 0);
        microcontrollerCost = Math.max(config.getDouble("power.cost.microcontroller"), 0);
        robotCost = Math.max(config.getDouble("power.cost.robot"), 0);
        droneCost = Math.max(config.getDouble("power.cost.drone"), 0);
        sleepCostFactor = Math.max(config.getDouble("power.cost.sleepFactor"), 0);
        screenCost = Math.max(config.getDouble("power.cost.screen"), 0);
        hologramCost = Math.max(config.getDouble("power.cost.hologram"), 0);
        hddReadCost = Math.max(config.getDouble("power.cost.hddRead"), 0) / 1024;
        hddWriteCost = Math.max(config.getDouble("power.cost.hddWrite"), 0) / 1024;
        gpuSetCost = Math.max(config.getDouble("power.cost.gpuSet"), 0) / Settings.basicScreenPixels();
        gpuFillCost = Math.max(config.getDouble("power.cost.gpuFill"), 0) / Settings.basicScreenPixels();
        gpuClearCost = Math.max(config.getDouble("power.cost.gpuClear"), 0) / Settings.basicScreenPixels();
        gpuCopyCost = Math.max(config.getDouble("power.cost.gpuCopy"), 0) / Settings.basicScreenPixels();
        robotTurnCost = Math.max(config.getDouble("power.cost.robotTurn"), 0);
        robotMoveCost = Math.max(config.getDouble("power.cost.robotMove"), 0);
        robotExhaustionCost = Math.max(config.getDouble("power.cost.robotExhaustion"), 0);
        wirelessCostPerRange = doubleArray(config.getDoubleList("power.cost.wirelessCostPerRange"), 2, "Bad number of wireless card energy costs, ignoring.", new double[]{0.05, 0.05}, 0.0);
        abstractBusPacketCost = Math.max(config.getDouble("power.cost.abstractBusPacket"), 0);
        geolyzerScanCost = Math.max(config.getDouble("power.cost.geolyzerScan"), 0);
        robotBaseCost = Math.max(config.getDouble("power.cost.robotAssemblyBase"), 0);
        robotComplexityCost = Math.max(config.getDouble("power.cost.robotAssemblyComplexity"), 0);
        microcontrollerBaseCost = Math.max(config.getDouble("power.cost.microcontrollerAssemblyBase"), 0);
        microcontrollerComplexityCost = Math.max(config.getDouble("power.cost.microcontrollerAssemblyComplexity"), 0);
        tabletBaseCost = Math.max(config.getDouble("power.cost.tabletAssemblyBase"), 0);
        tabletComplexityCost = Math.max(config.getDouble("power.cost.tabletAssemblyComplexity"), 0);
        droneBaseCost = Math.max(config.getDouble("power.cost.droneAssemblyBase"), 0);
        droneComplexityCost = Math.max(config.getDouble("power.cost.droneAssemblyComplexity"), 0);
        disassemblerItemCost = Math.max(config.getDouble("power.cost.disassemblerPerItem"), 0);
        chunkloaderCost = Math.max(config.getDouble("power.cost.chunkloaderCost"), 0);
        pistonCost = Math.max(config.getDouble("power.cost.pistonPush"), 0);
        eepromWriteCost = Math.max(config.getDouble("power.cost.eepromWrite"), 0);
        printCost = Math.max(config.getDouble("power.cost.printerModel"), 0);
        hoverBootJump = Math.max(config.getDouble("power.cost.hoverBootJump"), 0);
        hoverBootAbsorb = Math.max(config.getDouble("power.cost.hoverBootAbsorb"), 0);
        hoverBootMove = Math.max(config.getDouble("power.cost.hoverBootMove"), 0);
        dataCardTrivial = Math.max(config.getDouble("power.cost.dataCardTrivial"), 0);
        dataCardTrivialByte = Math.max(config.getDouble("power.cost.dataCardTrivialByte"), 0);
        dataCardSimple = Math.max(config.getDouble("power.cost.dataCardSimple"), 0);
        dataCardSimpleByte = Math.max(config.getDouble("power.cost.dataCardSimpleByte"), 0);
        dataCardComplex = Math.max(config.getDouble("power.cost.dataCardComplex"), 0);
        dataCardComplexByte = Math.max(config.getDouble("power.cost.dataCardComplexByte"), 0);
        dataCardAsymmetric = Math.max(config.getDouble("power.cost.dataCardAsymmetric"), 0);
        transposerCost = Math.max(config.getDouble("power.cost.transposer"), 0);
        nanomachineCost = Math.max(config.getDouble("power.cost.nanomachineInput"), 0);
        nanomachineReconfigureCost = Math.max(config.getDouble("power.cost.nanomachinesReconfigure"), 0);
        mfuCost = Math.max(config.getDouble("power.cost.mfuRelay"), 0);

        // power.rate
        accessPointRate = Math.max(config.getDouble("power.rate.accessPoint"), 0);
        assemblerRate = Math.max(config.getDouble("power.rate.assembler"), 0);
        caseRate = appendCreative(doubleArray(config.getDoubleList("power.rate.case"), 3, "Bad number of computer case conversion rates, ignoring.", new double[]{5.0, 10.0, 20.0}), 9001.0);
        // Creative case.
        chargerRate = Math.max(config.getDouble("power.rate.charger"), 0);
        disassemblerRate = Math.max(config.getDouble("power.rate.disassembler"), 0);
        powerConverterRate = Math.max(config.getDouble("power.rate.powerConverter"), 0);
        serverRackRate = Math.max(config.getDouble("power.rate.serverRack"), 0);

        // power.value
        valueAppliedEnergistics2 = config.getDouble("power.value.AppliedEnergistics2");
        valueFactorization = config.getDouble("power.value.Factorization");
        valueGalacticraft = config.getDouble("power.value.Galacticraft");
        valueIndustrialCraft2 = config.getDouble("power.value.IndustrialCraft2");
        valueMekanism = config.getDouble("power.value.Mekanism");
        valuePowerAdvantage = config.getDouble("power.value.PowerAdvantage");
        valueRedstoneFlux = config.getDouble("power.value.RedstoneFlux");
        valueRotaryCraft = config.getDouble("power.value.RotaryCraft") / 11256.0;
        valueForgeEnergy = config.hasPath("power.value.ForgeEnergy") ? config.getDouble("power.value.ForgeEnergy") : valueRedstoneFlux;

        valueInternal = 1000;

        ratioAppliedEnergistics2 = valueAppliedEnergistics2 / valueInternal;
        ratioFactorization = valueFactorization / valueInternal;
        ratioGalacticraft = valueGalacticraft / valueInternal;
        ratioIndustrialCraft2 = valueIndustrialCraft2 / valueInternal;
        ratioMekanism = valueMekanism / valueInternal;
        ratioPowerAdvantage = valuePowerAdvantage / valueInternal;
        ratioRedstoneFlux = valueRedstoneFlux / valueInternal;
        ratioRotaryCraft = valueRotaryCraft / valueInternal;
        ratioForgeEnergy = valueForgeEnergy / valueInternal;

        // ----------------------------------------------------------------------- //
        // filesystem
        fileCost = Math.max(config.getInt("filesystem.fileCost"), 0);
        bufferChanges = config.getBoolean("filesystem.bufferChanges");
        hddSizes = intArray(config.getIntList("filesystem.hddSizes"), 3, "Bad number of HDD sizes, ignoring.", new int[]{1024, 2048, 4096});
        hddPlatterCounts = intArray(config.getIntList("filesystem.hddPlatterCounts"), 3, "Bad number of HDD platter counts, ignoring.", new int[]{2, 4, 6});
        floppySize = Math.max(config.getInt("filesystem.floppySize"), 0);
        tmpSize = Math.max(config.getInt("filesystem.tmpSize"), 0);
        maxHandles = Math.max(config.getInt("filesystem.maxHandles"), 0);
        maxReadBuffer = Math.max(config.getInt("filesystem.maxReadBuffer"), 0);
        sectorSeekThreshold = config.getInt("filesystem.sectorSeekThreshold");
        sectorSeekTime = config.getDouble("filesystem.sectorSeekTime");

        // ----------------------------------------------------------------------- //
        // internet
        httpEnabled = config.getBoolean("internet.enableHttp");
        httpHeadersEnabled = config.getBoolean("internet.enableHttpHeaders");
        tcpEnabled = config.getBoolean("internet.enableTcp");
        httpHostBlacklist = config.getStringList("internet.blacklist").stream().map(AddressValidator::new).toArray(AddressValidator[]::new);
        httpHostWhitelist = config.getStringList("internet.whitelist").stream().map(AddressValidator::new).toArray(AddressValidator[]::new);
        httpTimeout = Math.max(config.getInt("internet.requestTimeout"), 0) * 1000;
        maxConnections = Math.max(config.getInt("internet.maxTcpConnections"), 0);
        internetThreads = Math.max(config.getInt("internet.threads"), 1);

        // ----------------------------------------------------------------------- //
        // switch
        switchDefaultMaxQueueSize = Math.max(config.getInt("switch.defaultMaxQueueSize"), 1);
        switchQueueSizeUpgrade = Math.max(config.getInt("switch.queueSizeUpgrade"), 0);
        switchDefaultRelayDelay = Math.max(config.getInt("switch.defaultRelayDelay"), 1);
        switchRelayDelayUpgrade = Math.max(config.getDouble("switch.relayDelayUpgrade"), 0);
        switchDefaultRelayAmount = Math.max(config.getInt("switch.defaultRelayAmount"), 1);
        switchRelayAmountUpgrade = Math.max(config.getInt("switch.relayAmountUpgrade"), 0);

        // ----------------------------------------------------------------------- //
        // hologram
        hologramMaxScaleByTier = doubleArray(config.getDoubleList("hologram.maxScale"), 2, "Bad number of hologram max scales, ignoring.", new double[]{3.0, 4.0}, 1.0);
        hologramMaxTranslationByTier = doubleArray(config.getDoubleList("hologram.maxTranslation"), 2, "Bad number of hologram max translations, ignoring.", new double[]{0.25, 0.5}, 0.0);
        hologramSetRawDelay = Math.max(config.getDouble("hologram.setRawDelay"), 0);
        hologramLight = config.getBoolean("hologram.emitLight");

        // ----------------------------------------------------------------------- //
        // misc
        maxScreenWidth = Math.max(config.getInt("misc.maxScreenWidth"), 1);
        maxScreenHeight = Math.max(config.getInt("misc.maxScreenHeight"), 1);
        inputUsername = config.getBoolean("misc.inputUsername");
        maxNetworkPacketSize = Math.max(config.getInt("misc.maxNetworkPacketSize"), 0);
        // Need at least 4 for nanomachine protocol. Because I can!
        maxNetworkPacketParts = Math.max(config.getInt("misc.maxNetworkPacketParts"), 4);
        maxOpenPorts = intArray(config.getIntList("misc.maxOpenPorts"), 3, "Bad number of max open ports, ignoring.", new int[]{16, 1, 16}, 0);
        maxWirelessRange = doubleArray(config.getDoubleList("misc.maxWirelessRange"), 2, "Bad number of wireless card max ranges, ignoring.", new double[]{16.0, 400.0}, 0.0);
        rTreeMaxEntries = 10;
        terminalsPerServer = 4;
        updateCheck = config.getBoolean("misc.updateCheck");
        lootProbability = config.getInt("misc.lootProbability");
        lootRecrafting = config.getBoolean("misc.lootRecrafting");
        geolyzerRange = config.getInt("misc.geolyzerRange");
        geolyzerNoise = Math.max((float) config.getDouble("misc.geolyzerNoise"), 0f);
        disassembleAllTheThings = config.getBoolean("misc.disassembleAllTheThings");
        disassemblerBreakChance = Math.min(Math.max(config.getDouble("misc.disassemblerBreakChance"), 0), 1);
        disassemblerInputBlacklist = config.getStringList("misc.disassemblerInputBlacklist");
        hideOwnPet = config.getBoolean("misc.hideOwnSpecial");
        allowItemStackInspection = config.getBoolean("misc.allowItemStackInspection");
        databaseEntriesPerTier = new int[]{9, 25, 81};
        // Not configurable because of GUI design.
        presentChance = Math.min(Math.max(config.getDouble("misc.presentChance"), 0), 1);
        assemblerBlacklist = config.getStringList("misc.assemblerBlacklist");
        threadPriority = config.getInt("misc.threadPriority");
        giveManualToNewPlayers = config.getBoolean("misc.giveManualToNewPlayers");
        dataCardSoftLimit = Math.max(config.getInt("misc.dataCardSoftLimit"), 0);
        dataCardHardLimit = Math.max(config.getInt("misc.dataCardHardLimit"), 0);
        dataCardTimeout = Math.max(config.getDouble("misc.dataCardTimeout"), 0);
        serverRackSwitchTier = Math.min(Math.max(config.getInt("misc.serverRackSwitchTier") - 1, Tier.None), Tier.Three);
        redstoneDelay = Math.max(config.getDouble("misc.redstoneDelay"), 0);
        tradingRange = Math.max(config.getDouble("misc.tradingRange"), 0);
        mfuRange = Math.min(Math.max(config.getInt("misc.mfuRange"), 0), 128);

        // ----------------------------------------------------------------------- //
        // nanomachines
        nanomachineTriggerQuota = Math.max(config.getDouble("nanomachines.triggerQuota"), 0);
        nanomachineConnectorQuota = Math.max(config.getDouble("nanomachines.connectorQuota"), 0);
        nanomachineMaxInputs = Math.max(config.getInt("nanomachines.maxInputs"), 1);
        nanomachineMaxOutputs = Math.max(config.getInt("nanomachines.maxOutputs"), 1);
        nanomachinesSafeInputsActive = Math.max(config.getInt("nanomachines.safeInputsActive"), 0);
        nanomachinesMaxInputsActive = Math.max(config.getInt("nanomachines.maxInputsActive"), 0);
        nanomachinesCommandDelay = Math.max(config.getDouble("nanomachines.commandDelay"), 0);
        nanomachinesCommandRange = Math.max(config.getDouble("nanomachines.commandRange"), 0);
        nanomachineMagnetRange = Math.max(config.getDouble("nanomachines.magnetRange"), 0);
        nanomachineDisintegrationRange = Math.max(config.getInt("nanomachines.disintegrationRange"), 0);
        nanomachinePotionWhitelist = config.getAnyRefList("nanomachines.potionWhitelist");
        nanomachinesHungryDamage = Math.max((float) config.getDouble("nanomachines.hungryDamage"), 0f);
        nanomachinesHungryEnergyRestored = Math.max(config.getDouble("nanomachines.hungryEnergyRestored"), 0);

        // ----------------------------------------------------------------------- //
        // printer
        maxPrintComplexity = config.getInt("printer.maxShapes");
        printRecycleRate = config.getDouble("printer.recycleRate");
        chameliumEdible = config.getBoolean("printer.chameliumEdible");
        maxPrintLightLevel = Math.min(Math.max(config.getInt("printer.maxBaseLightLevel"), 0), 15);
        printCustomRedstone = Math.max(config.getInt("printer.customRedstoneCost"), 0);
        printMaterialValue = Math.max(config.getInt("printer.materialValue"), 0);
        printInkValue = Math.max(config.getInt("printer.inkValue"), 0);
        printsHaveOpacity = config.getBoolean("printer.printsHaveOpacity");
        noclipMultiplier = Math.max(config.getDouble("printer.noclipMultiplier"), 0);

        // chunkloader
        chunkloadDimensionBlacklist = Settings.getIntList(config, "chunkloader.dimBlacklist");
        chunkloadDimensionWhitelist = Settings.getIntList(config, "chunkloader.dimWhitelist");

        // ----------------------------------------------------------------------- //
        // integration
        modBlacklist = config.getStringList("integration.modBlacklist");
        peripheralBlacklist = config.getStringList("integration.peripheralBlacklist");
        fakePlayerUuid = config.getString("integration.fakePlayerUuid");
        fakePlayerName = config.getString("integration.fakePlayerName");
        fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName);

        // integration.vanilla
        enableInventoryDriver = config.getBoolean("integration.vanilla.enableInventoryDriver");
        enableTankDriver = config.getBoolean("integration.vanilla.enableTankDriver");
        enableCommandBlockDriver = config.getBoolean("integration.vanilla.enableCommandBlockDriver");
        allowItemStackNBTTags = config.getBoolean("integration.vanilla.allowItemStackNBTTags");

        // integration.buildcraft
        costProgrammingTable = Math.max(config.getDouble("integration.buildcraft.programmingTableCost"), 0);

        // ----------------------------------------------------------------------- //
        // debug
        logLuaCallbackErrors = config.getBoolean("debug.logCallbackErrors");
        forceLuaJ = config.getBoolean("debug.forceLuaJ");
        allowUserdata = !config.getBoolean("debug.disableUserdata");
        allowPersistence = !config.getBoolean("debug.disablePersistence");
        limitMemory = !config.getBoolean("debug.disableMemoryLimit");
        forceCaseInsensitive = config.getBoolean("debug.forceCaseInsensitiveFS");
        logFullLibLoadErrors = config.getBoolean("debug.logFullNativeLibLoadErrors");
        forceNativeLib = config.getString("debug.forceNativeLibWithName");
        logOpenGLErrors = config.getBoolean("debug.logOpenGLErrors");
        logHexFontErrors = config.getBoolean("debug.logHexFontErrors");
        alwaysTryNative = config.getBoolean("debug.alwaysTryNative");
        debugPersistence = config.getBoolean("debug.verbosePersistenceErrors");
        nativeInTmpDir = config.getBoolean("debug.nativeInTmpDir");
        periodicallyForceLightUpdate = config.getBoolean("debug.periodicallyForceLightUpdate");
        insertIdsInConverters = config.getBoolean("debug.insertIdsInConverters");

        debugCardAccess = parseDebugCardAccess(config.getValue("debug.debugCardAccess").unwrapped());

        registerLuaJArchitecture = config.getBoolean("debug.registerLuaJArchitecture");
        disableLocaleChanging = config.getBoolean("debug.disableLocaleChanging");

        // >= 1.7.4
        maxSignalQueueSize = Math.min(config.hasPath("computer.maxSignalQueueSize") ? config.getInt("computer.maxSignalQueueSize") : 256, 256);

        // >= 1.7.6
        vramSizes = doubleArray(config.getDoubleList("gpu.vramSizes"), 3, "Bad number of VRAM sizes (expected 3), ignoring.", new double[]{1, 2, 3});

        bitbltCost = config.hasPath("gpu.bitbltCost") ? config.getDouble("gpu.bitbltCost") : 0.5;
    }

    // ----------------------------------------------------------------------- //
    // Helpers for the array settings (Scala: `config.getXList(..).asScala.toArray match { ... }`).

    private static int[] intArray(List<Integer> values, int expected, String warning, int[] defaults) {
        if (values.size() == expected) {
            return values.stream().mapToInt(Integer::intValue).toArray();
        }
        OpenComputers.log.warn(warning);
        return defaults;
    }

    private static int[] intArray(List<Integer> values, int expected, String warning, int[] defaults, int min) {
        if (values.size() == expected) {
            return values.stream().mapToInt(v -> Math.max(v, min)).toArray();
        }
        OpenComputers.log.warn(warning);
        return defaults;
    }

    private static double[] doubleArray(List<Double> values, int expected, String warning, double[] defaults) {
        if (values.size() == expected) {
            return values.stream().mapToDouble(Double::doubleValue).toArray();
        }
        OpenComputers.log.warn(warning);
        return defaults;
    }

    private static double[] doubleArray(List<Double> values, int expected, String warning, double[] defaults, double min) {
        if (values.size() == expected) {
            return values.stream().mapToDouble(v -> Math.max(v, min)).toArray();
        }
        OpenComputers.log.warn(warning);
        return defaults;
    }

    // Creative case.
    private static double[] appendCreative(double[] values, double creative) {
        final double[] result = Arrays.copyOf(values, values.length + 1);
        result[values.length] = creative;
        return result;
    }

    private static Pair<Double, Double> hudPos(List<Double> values) {
        if (values.size() == 2) {
            return Pair.of(values.get(0), values.get(1));
        }
        OpenComputers.log.warn("Bad number of HUD coordiantes, ignoring.");
        return Pair.of(-1.0, -1.0);
    }

    private static DebugCardAccess parseDebugCardAccess(Object value) {
        if ("true".equals(value) || "allow".equals(value) || Boolean.TRUE.equals(value)) {
            return DebugCardAccess.Allowed.INSTANCE;
        } else if ("false".equals(value) || "deny".equals(value) || Boolean.FALSE.equals(value)) {
            return DebugCardAccess.Forbidden.INSTANCE;
        } else if ("whitelist".equals(value)) {
            final File wlFile = Platform.getConfigFolder().resolve("opencomputers").resolve("debug_card_whitelist.txt").toFile();
            return new DebugCardAccess.Whitelist(wlFile);
        } else { // Fallback to most secure configuration
            OpenComputers.log.warn("Unknown debug card access type, falling back to `deny`. Allowed values: `allow`, `deny`, `whitelist`.");
            return DebugCardAccess.Forbidden.INSTANCE;
        }
    }

    // ----------------------------------------------------------------------- //

    public static final String resourceDomain = OpenComputers.ID;
    public static final String namespace = "oc:";
    public static final String savePath = "opencomputers/";
    public static final String scriptPath = "/assets/" + resourceDomain + "/lua/";
    @SuppressWarnings("unchecked")
    public static final Pair<Integer, Integer>[] screenResolutionsByTier = new Pair[]{Pair.of(50, 16), Pair.of(80, 25), Pair.of(160, 50)};
    public static final TextBuffer.ColorDepth[] screenDepthsByTier = new TextBuffer.ColorDepth[]{TextBuffer.ColorDepth.OneBit, TextBuffer.ColorDepth.FourBit, TextBuffer.ColorDepth.EightBit};
    public static final int[] deviceComplexityByTier = new int[]{12, 24, 32, 9001};
    public static boolean rTreeDebugRenderer = false;
    public static int blockRenderId = -1;

    public static int basicScreenPixels() {
        return screenResolutionsByTier[0].getLeft() * screenResolutionsByTier[0].getRight();
    }

    private static Settings settings;

    public static Settings get() {
        return settings;
    }

    public static void load(File file) {
        final String EOL = System.lineSeparator();
        // typesafe config's internal method for loading the reference.conf file
        // seems to fail on some systems (as does their parseResource method), so
        // we'll have to load the default config manually. This was reported on the
        // Minecraft Forums, I could not reproduce the issue, but this version has
        // reportedly fixed the problem.
        final Config defaults;
        try (InputStream in = Settings.class.getResourceAsStream("/application.conf")) {
            if (in == null) throw new IOException("Missing /application.conf");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            final String plain = reader.lines().map(line -> line + EOL).collect(Collectors.joining());
            defaults = ConfigFactory.parseString(plain);
        } catch (IOException e) {
            throw new IllegalStateException("Failed loading default configuration.", e);
        }
        Config config;
        try {
            final String plain = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).stream().map(line -> line + EOL).collect(Collectors.joining());
            config = patchConfig(ConfigFactory.parseString(plain), defaults).withFallback(defaults);
            settings = new Settings(config.getConfig("opencomputers"));
        } catch (Throwable e) {
            if (file.exists()) {
                OpenComputers.log.warn("Failed loading config, using defaults.", e);
            }
            settings = new Settings(defaults.getConfig("opencomputers"));
            config = defaults;
        }
        try {
            final ConfigRenderOptions renderSettings = ConfigRenderOptions.defaults().setJson(false).setOriginComments(false);
            final String nl = System.getProperty("line.separator");
            final Pattern indent = Pattern.compile("^(\\s*)");
            file.getParentFile().mkdirs();
            try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
                out.write(config.root().render(renderSettings).lines().
                        // Indent two spaces instead of four.
                        map(line -> {
                            final Matcher m = indent.matcher(line);
                            return m.replaceAll(r -> Matcher.quoteReplacement(r.group(1).replace("  ", " ")));
                        }).
                        // Finalize the string.
                        filter(line -> !line.isEmpty()).collect(Collectors.joining(nl)).
                        // Newline after values.
                        replaceAll("((?:\\s*#.*" + nl + ")(?:\\s*[^#\\s].*" + nl + ")+)", "$1" + nl));
            }
        } catch (Throwable e) {
            OpenComputers.log.warn("Failed saving config.", e);
        }
    }

    // Usage: VersionRange.createFromVersionSpec("[0.0,1.5)") -> Array("computer.ramSizes") will
    // re-set the value of `computer.ramSizes` if a config saved with a version < 1.5 is loaded.
    private static final List<Pair<VersionRange, String[]>> configPatches = Arrays.asList(
            // Upgrading to version 1.5.20, changed relay delay default.
            Pair.of(versionRange("[0.0, 1.5.20)"), new String[]{
                    "switch.relayDelayUpgrade"
            }),
            // Potion whitelist was fixed in 1.6.2.
            Pair.of(versionRange("[0.0, 1.6.2)"), new String[]{
                    "nanomachines.potionWhitelist"
            }),
            // Upgrading past version 1.7.1, changed wireless card stuff for t1 card.
            Pair.of(versionRange("[0.0, 1.7.2)"), new String[]{
                    "power.cost.wirelessCostPerRange",
                    "misc.maxWirelessRange",
                    "misc.maxOpenPorts",
                    "computer.cpuComponentCount"
            })
    );

    private static VersionRange versionRange(String spec) {
        try {
            return VersionRange.createFromVersionSpec(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Checks the config version (i.e. the version of the mod the config was
    // created by) against the current version to see if some hard changes
    // were made. If so, the new default values are copied over.
    private static Config patchConfig(Config config, Config defaults) {
        final DefaultArtifactVersion modVersion = new DefaultArtifactVersion(OpenComputers.version());
        final String prefix = "opencomputers.";
        final DefaultArtifactVersion configVersion = new DefaultArtifactVersion(config.hasPath(prefix + "version") ? config.getString(prefix + "version") : "0.0.0");
        Config patched = config;
        if (configVersion.compareTo(modVersion) != 0) {
            OpenComputers.log.info("Updating config from version '" + configVersion + "' to '" + defaults.getString(prefix + "version") + "'.");
            patched = patched.withValue(prefix + "version", defaults.getValue(prefix + "version"));
            for (Pair<VersionRange, String[]> patch : configPatches) {
                if (!patch.getLeft().containsVersion(configVersion)) continue;
                for (String path : patch.getRight()) {
                    final String fullPath = prefix + path;
                    OpenComputers.log.info("Updating setting '" + fullPath + "'. ");
                    if (defaults.hasPath(fullPath)) {
                        patched = patched.withValue(fullPath, defaults.getValue(fullPath));
                    } else {
                        patched = patched.withoutPath(fullPath);
                    }
                }
            }
        }
        return patched;
    }

    public static final Pattern cidrPattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:/(\\d{1,2}))");

    public static class AddressValidator {
        public final String value;
        public final BiPredicate<InetAddress, String> validator;

        public AddressValidator(String value) {
            this.value = value;
            this.validator = createValidator(value);
        }

        private static BiPredicate<InetAddress, String> createValidator(String value) {
            try {
                final Matcher matcher = cidrPattern.matcher(value);
                if (matcher.find()) {
                    final String address = matcher.group(1);
                    final String prefix = matcher.group(2);
                    final int addr = InetAddresses.coerceToInteger(InetAddresses.forString(address));
                    final int mask = 0xFFFFFFFF << (32 - Integer.parseInt(prefix));
                    final int min = addr & mask;
                    final int max = min | ~mask;
                    return (inetAddress, host) -> {
                        if (inetAddress instanceof Inet4Address v4) {
                            final int numeric = InetAddresses.coerceToInteger(v4);
                            return min <= numeric && numeric <= max;
                        }
                        return true; // Can't check IPv6 addresses so we pass them.
                    };
                } else {
                    final InetAddress address = InetAddress.getByName(value);
                    return (inetAddress, host) -> host.equals(value) || address.equals(inetAddress);
                }
            } catch (Throwable t) {
                OpenComputers.log.warn("Invalid entry in internet blacklist / whitelist: " + value, t);
                return (inetAddress, host) -> true;
            }
        }

        public boolean apply(InetAddress inetAddress, String host) {
            return validator.test(inetAddress, host);
        }
    }

    public interface DebugCardAccess {
        Optional<String> checkAccess(Optional<DebugCard.AccessContext> ctx);

        final class Forbidden implements DebugCardAccess {
            public static final Forbidden INSTANCE = new Forbidden();

            private Forbidden() {
            }

            @Override
            public Optional<String> checkAccess(Optional<DebugCard.AccessContext> ctx) {
                return Optional.of("debug card is disabled");
            }
        }

        final class Allowed implements DebugCardAccess {
            public static final Allowed INSTANCE = new Allowed();

            private Allowed() {
            }

            @Override
            public Optional<String> checkAccess(Optional<DebugCard.AccessContext> ctx) {
                return Optional.empty();
            }
        }

        final class Whitelist implements DebugCardAccess {
            public final File noncesFile;
            private final Map<String, String> values = new HashMap<>();
            private final SecureRandom rng;

            public Whitelist(File noncesFile) {
                this.noncesFile = noncesFile;
                try {
                    this.rng = SecureRandom.getInstance("SHA1PRNG");
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
                load();
            }

            public void save() {
                final File noncesDir = noncesFile.getParentFile();
                try {
                    if (!noncesDir.exists() && !noncesDir.mkdirs())
                        throw new IOException("Cannot create nonces directory: " + noncesDir.getCanonicalPath());

                    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(noncesFile), StandardCharsets.UTF_8), false)) {
                        for (Map.Entry<String, String> entry : values.entrySet())
                            writer.println(entry.getKey() + " " + entry.getValue());
                    }
                } catch (IOException e) {
                    // Scala let this propagate unchecked.
                    throw new java.io.UncheckedIOException(e);
                }
            }

            public void load() {
                values.clear();

                if (!noncesFile.exists())
                    return;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(noncesFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String[] parts = line.split(" ", 2);
                        if (parts.length == 2) {
                            values.put(parts[0], parts[1]);
                        }
                    }
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }

            private String generateNonce() {
                final byte[] buf = new byte[16];
                rng.nextBytes(buf);
                return new String(Hex.encodeHex(buf, true));
            }

            public Optional<String> nonce(String player) {
                return Optional.ofNullable(values.get(player.toLowerCase()));
            }

            public boolean isWhitelisted(String player) {
                return values.containsKey(player.toLowerCase());
            }

            public Set<String> whitelist() {
                return values.keySet();
            }

            public void add(String player) {
                if (!values.containsKey(player.toLowerCase())) {
                    values.put(player.toLowerCase(), generateNonce());
                    save();
                }
            }

            public void remove(String player) {
                if (values.remove(player.toLowerCase()) != null)
                    save();
            }

            public void invalidate(String player) {
                if (values.containsKey(player.toLowerCase())) {
                    values.put(player.toLowerCase(), generateNonce());
                    save();
                }
            }

            @Override
            public Optional<String> checkAccess(Optional<DebugCard.AccessContext> ctxOpt) {
                if (ctxOpt.isPresent()) {
                    final DebugCard.AccessContext ctx = ctxOpt.get();
                    final String x = values.get(ctx.player.toLowerCase());
                    if (x != null) {
                        if (x.equals(ctx.nonce)) return Optional.empty();
                        else return Optional.of("debug card is invalidated, please re-bind it to yourself");
                    } else {
                        return Optional.of("you are not whitelisted to use debug card");
                    }
                } else {
                    return Optional.of("debug card is whitelisted, Shift+Click with it to bind card to yourself");
                }
            }
        }
    }

    public static List<Integer> getIntList(Config config, String path, Optional<List<Integer>> defaultValue) {
        if (config.hasPath(path))
            return config.getIntList(path);
        else
            return defaultValue.orElseGet(LinkedList::new);
    }

    public static List<Integer> getIntList(Config config, String path) {
        return getIntList(config, path, Optional.empty());
    }
}
