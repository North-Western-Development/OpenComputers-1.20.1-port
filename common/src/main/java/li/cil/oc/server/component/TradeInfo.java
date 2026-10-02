package li.cil.oc.server.component;

import dev.architectury.utils.GameInstance;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.tileentity.RobotProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;

/**
 * Was declared in Trade.scala.
 */
public class TradeInfo {
    public Optional<EnvironmentHost> host;
    public WeakReference<Merchant> merchant;
    public int recipeID;
    public int merchantID;

    public TradeInfo(Optional<EnvironmentHost> host, WeakReference<Merchant> merchant, int recipeID, int merchantID) {
        this.host = host;
        this.merchant = merchant;
        this.recipeID = recipeID;
        this.merchantID = merchantID;
    }

    public TradeInfo() {
        this(Optional.empty(), new WeakReference<>(null), -1, -1);
    }

    public TradeInfo(EnvironmentHost host, Merchant merchant, int recipeID, int merchantID) {
        this(Optional.ofNullable(host), new WeakReference<>(merchant), recipeID, merchantID);
    }

    public Optional<MerchantOffer> recipe() {
        final Merchant m = merchant.get();
        if (m == null) return Optional.empty();
        final MerchantOffers offers = m.getOffers();
        return recipeID >= 0 && recipeID < offers.size() ? Optional.ofNullable(offers.get(recipeID)) : Optional.empty();
    }

    public Optional<Container> inventory() {
        if (host.isPresent() && host.get() instanceof Agent agent) return Optional.ofNullable(agent.mainInventory());
        return Optional.empty();
    }

    private static final String HostIsEntityTag = "hostIsEntity";
    private static final String MerchantUUIDMostTag = "merchantUUIDMost";
    private static final String MerchantUUIDLeastTag = "merchantUUIDLeast";
    private static final String DimensionIDTag = "dimensionID";
    private static final String HostUUIDMost = "hostUUIDMost";
    private static final String HostUUIDLeast = "hostUUIDLeast";
    private static final String HostXTag = "hostX";
    private static final String HostYTag = "hostY";
    private static final String HostZTag = "hostZ";
    private static final String RecipeID = "recipeID";
    private static final String MerchantID = "merchantID";

    public void loadData(CompoundTag nbt) {
        final boolean isEntity = nbt.getBoolean(HostIsEntityTag);
        // If drone we find it again by its UUID, if Robot we know the X/Y/Z of the TileEntity.
        host = isEntity ? loadHostEntity(nbt) : loadHostTileEntity(nbt);
        final Optional<Entity> merchantEntity = loadEntity(nbt, new UUID(nbt.getLong(MerchantUUIDMostTag), nbt.getLong(MerchantUUIDLeastTag)));
        merchant = new WeakReference<>(merchantEntity.isPresent() && merchantEntity.get() instanceof Merchant m ? m : null);
        recipeID = nbt.getInt(RecipeID);
        merchantID = nbt.contains(MerchantID) ? nbt.getInt(MerchantID) : -1;
    }

    public void saveData(CompoundTag nbt) {
        final EnvironmentHost h = host.orElse(null);
        if (h instanceof Entity entity) {
            nbt.putBoolean(HostIsEntityTag, true);
            nbt.putString(DimensionIDTag, entity.level().dimension().location().toString());
            nbt.putLong(HostUUIDLeast, entity.getUUID().getLeastSignificantBits());
            nbt.putLong(HostUUIDMost, entity.getUUID().getMostSignificantBits());
        }
        else if (h instanceof BlockEntity tileEntity) {
            nbt.putBoolean(HostIsEntityTag, false);
            nbt.putString(DimensionIDTag, tileEntity.getLevel().dimension().location().toString());
            nbt.putInt(HostXTag, tileEntity.getBlockPos().getX());
            nbt.putInt(HostYTag, tileEntity.getBlockPos().getY());
            nbt.putInt(HostZTag, tileEntity.getBlockPos().getZ());
        }
        // else: Welp!
        if (merchant.get() instanceof Entity entity) {
            nbt.putLong(MerchantUUIDLeastTag, entity.getUUID().getLeastSignificantBits());
            nbt.putLong(MerchantUUIDMostTag, entity.getUUID().getMostSignificantBits());
        }
        nbt.putInt(RecipeID, recipeID);
        nbt.putInt(MerchantID, merchantID);
    }

    private static Optional<ServerLevel> loadWorld(CompoundTag nbt) {
        final ResourceLocation dimension = new ResourceLocation(nbt.getString(DimensionIDTag));
        final ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimension);
        final MinecraftServer server = GameInstance.getServer();
        return server == null ? Optional.empty() : Optional.ofNullable(server.getLevel(dimKey));
    }

    private Optional<Entity> loadEntity(CompoundTag nbt, UUID uuid) {
        return loadWorld(nbt).map(world -> world.getEntity(uuid));
    }

    private Optional<EnvironmentHost> loadHostEntity(CompoundTag nbt) {
        final Optional<Entity> entity = loadEntity(nbt, new UUID(nbt.getLong(HostUUIDMost), nbt.getLong(HostUUIDLeast)));
        if (entity.isPresent() && entity.get() instanceof Agent && entity.get() instanceof EnvironmentHost host) {
            return Optional.of(host);
        }
        return Optional.empty();
    }

    private Optional<EnvironmentHost> loadHostTileEntity(CompoundTag nbt) {
        final Optional<ServerLevel> world = loadWorld(nbt);
        if (world.isEmpty()) return Optional.empty();

        final int x = nbt.getInt(HostXTag);
        final int y = nbt.getInt(HostYTag);
        final int z = nbt.getInt(HostZTag);

        final BlockEntity tileEntity = world.get().getBlockEntity(new BlockPos(x, y, z));
        if (tileEntity instanceof RobotProxy robotProxy) return Optional.of(robotProxy.robot);
        else if (tileEntity instanceof Agent agent) return Optional.of(agent);
        else return Optional.empty();
    }
}
