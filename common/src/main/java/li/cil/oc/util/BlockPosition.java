package li.cil.oc.util;

import com.google.common.hash.Hashing;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BlockPosition {
    public final int x;
    public final int y;
    public final int z;
    public final Optional<Level> world;

    public BlockPosition(int x, int y, int z, Optional<Level> world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public BlockPosition(double x, double y, double z, Optional<Level> world) {
        this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z), world);
    }

    public BlockPosition(double x, double y, double z) {
        this(x, y, z, Optional.empty());
    }

    public BlockPosition(int x, int y, int z) {
        this(x, y, z, Optional.empty());
    }

    public BlockPosition(int x, int y, int z, Level world) {
        this(x, y, z, Optional.ofNullable(world));
    }

    public BlockPosition(double x, double y, double z, Level world) {
        this(x, y, z, Optional.ofNullable(world));
    }

    public BlockPosition(Vec3 v) {
        this(v.x, v.y, v.z, Optional.empty());
    }

    public BlockPosition(Vec3 v, Level world) {
        this(v.x, v.y, v.z, Optional.ofNullable(world));
    }

    public BlockPosition(EnvironmentHost host) {
        this(host.xPosition(), host.yPosition(), host.zPosition(), host.world());
    }

    public BlockPosition(Entity entity) {
        this(entity.getX(), entity.getY(), entity.getZ(), entity.level());
    }

    public BlockPosition(BlockPos pos, Level world) {
        this(pos.getX(), pos.getY(), pos.getZ(), world);
    }

    public BlockPosition(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPosition offset(Direction direction, int n) {
        return new BlockPosition(
                x + direction.getStepX() * n,
                y + direction.getStepY() * n,
                z + direction.getStepZ() * n,
                world);
    }

    public BlockPosition offset(Direction direction) {
        return offset(direction, 1);
    }

    public Vec3 offset(double x, double y, double z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }

    public AABB bounds() {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public Vec3 toVec3() {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockPosition position) {
            return position.x == x && position.y == y && position.z == z && position.world.equals(world);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Hashing.
                goodFastHash(32).
                newHasher(16).
                putInt(x).
                putInt(y).
                putInt(z).
                putInt(world.hashCode()).
                hash().
                asInt();
    }

    // ----------------------------------------------------------------------- //
    // Companion object factories (Scala `BlockPosition(...)`).

    public static BlockPosition apply(int x, int y, int z, Level world) {
        return new BlockPosition(x, y, z, Optional.ofNullable(world));
    }

    public static BlockPosition apply(int x, int y, int z) {
        return new BlockPosition(x, y, z, Optional.empty());
    }

    public static BlockPosition apply(double x, double y, double z, Level world) {
        return new BlockPosition(x, y, z, Optional.ofNullable(world));
    }

    public static BlockPosition apply(double x, double y, double z) {
        return new BlockPosition(x, y, z, Optional.empty());
    }

    public static BlockPosition apply(Vec3 v) {
        return new BlockPosition(v.x, v.y, v.z, Optional.empty());
    }

    public static BlockPosition apply(Vec3 v, Level world) {
        return new BlockPosition(v.x, v.y, v.z, Optional.ofNullable(world));
    }

    public static BlockPosition apply(EnvironmentHost host) {
        return new BlockPosition(host);
    }

    public static BlockPosition apply(Entity entity) {
        return new BlockPosition(entity);
    }

    public static BlockPosition apply(BlockPos pos, Level world) {
        return new BlockPosition(pos, world);
    }

    public static BlockPosition apply(BlockPos pos) {
        return new BlockPosition(pos);
    }
}
