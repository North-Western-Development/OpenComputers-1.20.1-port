package li.cil.oc.server.machine.luac;

import com.google.common.base.Strings;
import com.google.common.io.PatternFilenameFilter;
import dev.architectury.platform.Platform;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Factory singleton used to spawn new LuaState instances.
 * <p>
 * This is realized as a singleton so that we only have to resolve shared
 * library references once during initialization and can then re-use the
 * already loaded ones.
 */
public abstract class LuaStateFactory {
    // ----------------------------------------------------------------------- //
    // Former companion object.
    // ----------------------------------------------------------------------- //

    private static final Object LOCK = new Object();

    public static boolean isAvailableStatic() {
        // Force initialization of both.
        final boolean lua52 = Lua52.INSTANCE.isAvailable();
        final boolean lua53 = Lua53.INSTANCE.isAvailable();
        return lua52 || lua53;
    }

    public static boolean luajRequested() {
        return Settings.get().forceLuaJ || Settings.get().registerLuaJArchitecture;
    }

    public static boolean includeLuaJ() {
        return !isAvailableStatic() || luajRequested();
    }

    public static boolean include52() {
        return Lua52.INSTANCE.isAvailable() && !Settings.get().forceLuaJ;
    }

    public static boolean include53() {
        return Lua53.INSTANCE.isAvailable() && Settings.get().enableLua53 && !Settings.get().forceLuaJ;
    }

    public static boolean default53() {
        return include53() && Settings.get().defaultLua53;
    }

    public static ItemStack setDefaultArch(ItemStack stack) {
        if (default53()) {
            final DriverItem driver = Driver.driverFor(stack);
            if (driver instanceof MutableProcessor processor) {
                processor.setArchitecture(stack, NativeLua53Architecture.class);
            }
        }
        return stack;
    }

    public static final class Lua52 extends LuaStateFactory {
        public static final Lua52 INSTANCE = new Lua52();

        private Lua52() {
            super();
        }

        @Override
        public String version() {
            return "52";
        }

        @Override
        protected LuaState create(Optional<Integer> maxMemory) {
            return maxMemory.isPresent() ? new LuaState(maxMemory.get()) : new LuaState();
        }

        @Override
        protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.BIT32);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.pop(8);
        }
    }

    public static final class Lua53 extends LuaStateFactory {
        public static final Lua53 INSTANCE = new Lua53();

        private Lua53() {
            super();
        }

        @Override
        public String version() {
            return "53";
        }

        @Override
        protected LuaState create(Optional<Integer> maxMemory) {
            return maxMemory.isPresent() ? new LuaStateFiveThree(maxMemory.get()) : new LuaStateFiveThree();
        }

        @Override
        protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.openLib(LuaState.Library.UTF8);
            state.pop(8);
        }
    }

    // ----------------------------------------------------------------------- //
    // Instance part.
    // ----------------------------------------------------------------------- //

    public abstract String version();

    // ----------------------------------------------------------------------- //
    // Initialization
    // ----------------------------------------------------------------------- //

    /**
     * Set to true in initialization code below if available.
     */
    private boolean haveNativeLibrary = false;

    private String currentLib = "";

    private final String libraryName;

    protected LuaStateFactory() {
        libraryName = computeLibraryName();

        init();

        if (!haveNativeLibrary) {
            OpenComputers.log.warn("Unsupported platform, you won't be able to host games with persistent computers.");
        }
    }

    private String computeLibraryName() {
        if (!Strings.isNullOrEmpty(Settings.get().forceNativeLib)) return Settings.get().forceNativeLib;

        final String libExtension;
        if (SystemUtils.IS_OS_MAC) libExtension = ".dylib";
        else if (SystemUtils.IS_OS_WINDOWS) libExtension = ".dll";
        else libExtension = ".so";

        final String systemName;
        if (SystemUtils.IS_OS_FREE_BSD) systemName = "freebsd";
        else if (SystemUtils.IS_OS_NET_BSD) systemName = "netbsd";
        else if (SystemUtils.IS_OS_OPEN_BSD) systemName = "openbsd";
        else if (SystemUtils.IS_OS_SOLARIS) systemName = "solaris";
        else if (SystemUtils.IS_OS_LINUX) systemName = "linux";
        else if (SystemUtils.IS_OS_MAC) systemName = "darwin";
        else if (SystemUtils.IS_OS_WINDOWS) systemName = "windows";
        else systemName = "unknown";

        final String archName;
        if (Architecture.IS_OS_ARM64) archName = "aarch64";
        else if (Architecture.IS_OS_ARM) archName = "arm";
        else if (Architecture.IS_OS_X64) archName = "x86_64";
        else if (Architecture.IS_OS_X86) archName = "x86";
        else archName = "unknown";

        final String platformName = systemName + "-" + archName;

        return "libjnlua" + version() + "-" + platformName + libExtension;
    }

    protected abstract LuaState create(Optional<Integer> maxMemory);

    protected LuaState create() {
        return create(Optional.empty());
    }

    protected abstract void openLibs(LuaState state);

    // ----------------------------------------------------------------------- //

    public boolean isAvailable() {
        return haveNativeLibrary;
    }

    // Since we use native libraries we have to do some work. This includes
    // figuring out what we're running on, so that we can load the proper shared
    // libraries compiled for that system. It also means we have to unpack the
    // shared libraries somewhere so that we can load them, because we cannot
    // load them directly from a JAR.
    private void init() {
        if (libraryName == null) {
            return;
        }

        if (SystemUtils.IS_OS_WINDOWS && !Settings.get().alwaysTryNative) {
            if (SystemUtils.IS_OS_WINDOWS_XP) {
                OpenComputers.log.warn("Sorry, but Windows XP isn't supported. I'm afraid you'll have to use a newer Windows. I very much recommend upgrading your Windows, anyway, since Microsoft has stopped supporting Windows XP in April 2014.");
                return;
            }

            if (SystemUtils.IS_OS_WINDOWS_2003) {
                OpenComputers.log.warn("Sorry, but Windows Server 2003 isn't supported. I'm afraid you'll have to use a newer Windows.");
                return;
            }
        }

        // Natives are packaged as resources in the mod jar.
        URL libraryUrl = Machine.class.getResource("/assets/" + Settings.resourceDomain + "/lib/" + libraryName);
        if (libraryUrl == null) {
            libraryUrl = Machine.class.getResource("/assets/" + Settings.resourceDomain + "/lib//" + libraryName);
        }
        if (libraryUrl == null) {
            // On module-based loaders (Forge) Class#getResource only searches the class' own
            // module; in development the natives live in a separate library jar.
            final String path = "assets/" + Settings.resourceDomain + "/lib/" + libraryName;
            final ClassLoader[] loaders = {Machine.class.getClassLoader(), Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader()};
            for (ClassLoader loader : loaders) {
                if (loader == null) continue;
                libraryUrl = loader.getResource(path);
                if (libraryUrl != null) break;
            }
        }
        if (libraryUrl == null) {
            OpenComputers.log.warn("Native library with name '" + version() + "/" + libraryName + "' not found.");
            return;
        }

        final String tmpLibName = "OpenComputersMod-" + OpenComputers.version() + "-" + version() + "-" + libraryName;
        final String tmpBasePath;
        if (Settings.get().nativeInTmpDir) {
            final String path = System.getProperty("java.io.tmpdir");
            if (path == null) tmpBasePath = "";
            else if (path.endsWith("/") || path.endsWith("\\")) tmpBasePath = path;
            else tmpBasePath = path + "/";
        } else {
            // Formerly "./", i.e. the game directory.
            final String path = Platform.getGameFolder().toAbsolutePath().toString();
            tmpBasePath = path.endsWith("/") || path.endsWith("\\") ? path : path + "/";
        }
        final File tmpLibFile = new File(tmpBasePath + tmpLibName);

        // Clean up old library files when not in tmp dir.
        if (!Settings.get().nativeInTmpDir) {
            final File libDir = new File(tmpBasePath);
            if (libDir.isDirectory()) {
                final File[] files = libDir.listFiles(new PatternFilenameFilter("^" + Pattern.quote("OpenComputersMod-") + ".*" + Pattern.quote("-" + libraryName) + "$"));
                if (files != null) {
                    for (File file : files) {
                        if (file.compareTo(tmpLibFile) != 0) {
                            file.delete();
                        }
                    }
                }
            }
        }

        // If the file, already exists, make sure it's the same we need, if it's
        // not disable use of the natives.
        if (tmpLibFile.exists()) {
            boolean matching = true;
            try (InputStream inCurrent = new java.io.BufferedInputStream(libraryUrl.openStream());
                 InputStream inExisting = new java.io.BufferedInputStream(new FileInputStream(tmpLibFile))) {
                int inCurrentByte;
                int inExistingByte;
                do {
                    inCurrentByte = inCurrent.read();
                    inExistingByte = inExisting.read();
                    if (inCurrentByte != inExistingByte) {
                        matching = false;
                        inCurrentByte = -1;
                        inExistingByte = -1;
                    }
                }
                while (inCurrentByte != -1 && inExistingByte != -1);
            } catch (Throwable t) {
                matching = false;
            }
            if (!matching) {
                // Try to delete an old instance of the library, in case we have an update
                // and deleteOnExit fails (which it regularly does on Windows it seems).
                // Note that this should only ever be necessary for dev-builds, where the
                // version number didn't change (since the version number is part of the name).
                try {
                    tmpLibFile.delete();
                } catch (Throwable t) {
                    // Ignore.
                }
                if (tmpLibFile.exists()) {
                    OpenComputers.log.warn("Could not update native library '" + tmpLibFile.getName() + "'!");
                }
            }
        }

        // Copy the file contents to the temporary file.
        try (ReadableByteChannel in = Channels.newChannel(libraryUrl.openStream());
             FileOutputStream outStream = new FileOutputStream(tmpLibFile);
             FileChannel out = outStream.getChannel()) {
            out.transferFrom(in, 0, Long.MAX_VALUE);
            tmpLibFile.deleteOnExit();
            // Set file permissions more liberally for multi-user+instance servers.
            tmpLibFile.setReadable(true, false);
            tmpLibFile.setWritable(true, false);
            tmpLibFile.setExecutable(true, false);
        } catch (Throwable t) {
            // Java (or Windows?) locks the library file when opening it, so any
            // further tries to update it while another instance is still running
            // will fail. We still want to try each time, since the files may have
            // been updated.
            // Alternatively, the file could not be opened for reading/writing.
        }
        // Try to load the lib.
        currentLib = tmpLibFile.getAbsolutePath();
        try {
            synchronized (LOCK) {
                System.load(currentLib);
                create().close();
            }
            OpenComputers.log.info("Found a compatible native library: '" + tmpLibFile.getName() + "'.");
            haveNativeLibrary = true;
        } catch (Throwable t) {
            if (Settings.get().logFullLibLoadErrors) {
                OpenComputers.log.warn("Could not load native library '" + tmpLibFile.getName() + "'.", t);
            } else {
                OpenComputers.log.trace("Could not load native library '" + tmpLibFile.getName() + "'.");
            }
            tmpLibFile.delete();
        }
    }

    // ----------------------------------------------------------------------- //
    // Factory
    // ----------------------------------------------------------------------- //

    public Optional<LuaState> createState() {
        if (!haveNativeLibrary) return Optional.empty();

        try {
            final LuaState state;
            synchronized (LOCK) {
                System.load(currentLib);
                if (Settings.get().limitMemory) state = create(Optional.of(Integer.MAX_VALUE));
                else state = create();
            }
            try {
                // Load all libraries.
                openLibs(state);

                if (!Settings.get().disableLocaleChanging) {
                    state.openLib(LuaState.Library.OS);
                    state.getField(-1, "setlocale");
                    state.pushString("C");
                    state.call(1, 0);
                    state.pop(1);
                }

                // Prepare table for os stuff.
                state.newTable();
                state.setGlobal("os");

                // Kill compat entries.
                state.pushNil();
                state.setGlobal("unpack");
                state.pushNil();
                state.setGlobal("loadstring");
                state.getGlobal("math");
                state.pushNil();
                state.setField(-2, "log10");
                state.pop(1);
                state.getGlobal("table");
                state.pushNil();
                state.setField(-2, "maxn");
                state.pop(1);

                // Remove some other functions we don't need and are dangerous.
                state.pushNil();
                state.setGlobal("dofile");
                state.pushNil();
                state.setGlobal("loadfile");

                state.getGlobal("math");

                // We give each Lua state it's own randomizer, since otherwise they'd
                // use the good old rand() from C. Which can be terrible, and isn't
                // necessarily thread-safe.
                final Random random = new Random();
                ExtendedLuaState.pushScalaFunction(state, lua -> {
                    final double r = random.nextDouble();
                    switch (lua.getTop()) {
                        case 0:
                            lua.pushNumber(r);
                            break;
                        case 1: {
                            final double u = lua.checkNumber(1);
                            lua.checkArg(1, 1 <= u, "interval is empty");
                            lua.pushNumber(Math.floor(r * u) + 1);
                            break;
                        }
                        case 2: {
                            final double l = lua.checkNumber(1);
                            final double u = lua.checkNumber(2);
                            lua.checkArg(2, l <= u, "interval is empty");
                            lua.pushNumber(Math.floor(r * (u - l + 1)) + l);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("wrong number of arguments");
                    }
                    return 1;
                });
                state.setField(-2, "random");

                ExtendedLuaState.pushScalaFunction(state, lua -> {
                    random.setSeed(lua.checkInteger(1));
                    return 0;
                });
                state.setField(-2, "randomseed");

                // Pop the math table.
                state.pop(1);

                return Optional.of(state);
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed creating Lua state.", t);
                state.close();
            }
        } catch (UnsatisfiedLinkError e) {
            OpenComputers.log.error("Failed loading the native libraries.");
        } catch (Throwable t) {
            OpenComputers.log.warn("Failed creating Lua state.", t);
        }
        return Optional.empty();
    }

    // Inspired by org.apache.commons.lang3.SystemUtils
    public static final class Architecture {
        private Architecture() {
        }

        public static final String OS_ARCH = osArch();

        public static final boolean IS_OS_ARM = isOSArchMatch("arm");

        public static final boolean IS_OS_ARM64 = isOSArchMatch("aarch64");

        public static final boolean IS_OS_X86 = isOSArchMatch("x86") || isOSArchMatch("i386");

        public static final boolean IS_OS_X64 = isOSArchMatch("x86_64") || isOSArchMatch("amd64");

        private static String osArch() {
            try {
                return System.getProperty("os.arch");
            } catch (SecurityException ex) {
                return null;
            }
        }

        private static boolean isOSArchMatch(String archPrefix) {
            return OS_ARCH != null && OS_ARCH.startsWith(archPrefix);
        }
    }
}
