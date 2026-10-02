package li.cil.oc.server.component;

import com.google.common.hash.Hashing;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import static li.cil.oc.util.ResultWrapper.result;

public abstract class DataCard extends AbstractManagedEnvironment implements DeviceInfo {
    public final ComponentConnector node;

    protected DataCard() {
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Neighbors).
                withComponent("data", Visibility.Neighbors).
                withConnector().
                create();
        setNode(node);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    // ----------------------------------------------------------------------- //

    protected byte[] checkCost(Context context, Arguments args, double baseCost, double byteCost) throws Exception {
        final byte[] data = args.checkByteArray(0);
        if (data.length > Settings.get().dataCardHardLimit) throw new IllegalArgumentException("data size limit exceeded");
        final double cost = baseCost + data.length * byteCost;
        if (!node.tryChangeBuffer(-cost)) throw new Exception("not enough energy");
        if (data.length > Settings.get().dataCardSoftLimit) context.pause(Settings.get().dataCardTimeout);
        return data;
    }

    protected void checkCost(double baseCost) throws Exception {
        if (!node.tryChangeBuffer(-baseCost)) throw new Exception("not enough energy");
    }

    protected byte[] trivialCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardTrivial, Settings.get().dataCardTrivialByte);
    }

    protected byte[] simpleCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardSimple, Settings.get().dataCardSimpleByte);
    }

    protected byte[] complexCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardComplex, Settings.get().dataCardComplexByte);
    }

    protected byte[] asymmetricCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardAsymmetric, Settings.get().dataCardComplexByte);
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():number -- The maximum size of data that can be passed to other functions of the card.")
    public Object[] getLimit(Context context, Arguments args) {
        return result(Settings.get().dataCardHardLimit);
    }

    // ----------------------------------------------------------------------- //

    public static final ThreadLocal<SecureRandom> SecureRandomInstance = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    private static Map<String, String> deviceInfo(String product) {
        final Map<String, String> info = new HashMap<>();
        info.put(DeviceAttribute.Class, DeviceClass.Processor);
        info.put(DeviceAttribute.Description, "Data processor card");
        info.put(DeviceAttribute.Vendor, "S.C. Ltd.");
        info.put(DeviceAttribute.Product, product);
        return info;
    }

    public static class Tier1 extends DataCard {
        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) deviceInfo = DataCard.deviceInfo("SC01D H45h3r");
            return deviceInfo;
        }

        // ----------------------------------------------------------------------- //

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Applies base64 encoding to the data.")
        public Object[] encode64(Context context, Arguments args) throws Exception {
            return result((Object) Base64.encodeBase64(trivialCost(context, args)));
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Applies base64 decoding to the data.")
        public Object[] decode64(Context context, Arguments args) throws Exception {
            return result((Object) Base64.decodeBase64(trivialCost(context, args)));
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Applies deflate compression to the data.")
        public Object[] deflate(Context context, Arguments args) throws Exception {
            final byte[] data = complexCost(context, args);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            final DeflaterOutputStream deos = new DeflaterOutputStream(baos);
            deos.write(data);
            deos.finish();
            return result((Object) baos.toByteArray());
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Applies inflate decompression to the data.")
        public Object[] inflate(Context context, Arguments args) throws Exception {
            final byte[] data = complexCost(context, args);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            final InflaterOutputStream inos = new InflaterOutputStream(baos);
            inos.write(data);
            inos.finish();
            return result((Object) baos.toByteArray());
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Computes CRC-32 hash of the data. Result is binary data.")
        public Object[] crc32(Context context, Arguments args) throws Exception {
            final byte[] data = trivialCost(context, args);
            return result((Object) Hashing.crc32().hashBytes(data).asBytes());
        }

        @SuppressWarnings("deprecation")
        @Callback(direct = true, limit = 8, doc = "function(data:string):string -- Computes MD5 hash of the data. Result is binary data.")
        public Object[] md5(Context context, Arguments args) throws Exception {
            final byte[] data = simpleCost(context, args);
            return result((Object) Hashing.md5().hashBytes(data).asBytes());
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Computes SHA2-256 hash of the data. Result is binary data.")
        public Object[] sha256(Context context, Arguments args) throws Exception {
            final byte[] data = complexCost(context, args);
            return result((Object) Hashing.sha256().hashBytes(data).asBytes());
        }
    }

    public static class Tier2 extends Tier1 {
        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) deviceInfo = DataCard.deviceInfo("SC02D Cryptic");
            return deviceInfo;
        }

        // ----------------------------------------------------------------------- //

        @Override
        @Callback(direct = true, limit = 8, doc = "function(data:string[, hmacKey:string]):string -- Computes MD5 hash of the data. Result is binary data.")
        public Object[] md5(Context context, Arguments args) throws Exception {
            if (args.count() > 1) {
                final byte[] data = simpleCost(context, args);
                final byte[] key = args.checkByteArray(1);
                return hash(data, key, "MD5", "HmacMD5");
            } else return super.md5(context, args);
        }

        @Override
        @Callback(direct = true, limit = 4, doc = "function(data:string[, hmacKey:string]):string -- Computes SHA2-256 hash of the data. Result is binary data.")
        public Object[] sha256(Context context, Arguments args) throws Exception {
            if (args.count() > 1) {
                final byte[] data = complexCost(context, args);
                final byte[] key = args.checkByteArray(1);
                return hash(data, key, "SHA-256", "HmacSHA256");
            } else return super.sha256(context, args);
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, key: string, iv:string):string -- Encrypt data with AES. Result is binary data.")
        public Object[] encrypt(Context context, Arguments args) throws Exception {
            return crypt(context, args, Cipher.ENCRYPT_MODE);
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, key:string, iv:string):string -- Decrypt data with AES.")
        public Object[] decrypt(Context context, Arguments args) throws Exception {
            return crypt(context, args, Cipher.DECRYPT_MODE);
        }

        @Callback(direct = true, limit = 4, doc = "function(len:number):string -- Generates secure random binary data.")
        public Object[] random(Context context, Arguments args) throws Exception {
            final int len = args.checkInteger(0);

            if (len <= 0 || len > 1024)
                throw new IllegalArgumentException("length must be in range [1..1024]");

            checkCost(Settings.get().dataCardComplex + Settings.get().dataCardComplexByte * len);
            final byte[] target = new byte[len];
            SecureRandomInstance.get().nextBytes(target);
            return result((Object) target);
        }

        // ----------------------------------------------------------------------- //

        private Object[] crypt(Context context, Arguments args, int mode) throws Exception {
            final byte[] data = simpleCost(context, args);

            final byte[] key = args.checkByteArray(1);
            if (key.length != 16)
                throw new IllegalArgumentException("expected a 128-bit AES key");

            final byte[] iv = args.checkByteArray(2);
            if (iv.length != 16)
                throw new IllegalArgumentException("expected a 128-bit AES IV");

            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return result((Object) cipher.doFinal(data));
        }

        private Object[] hash(byte[] data, byte[] key, String mode, String hmacMode) throws Exception {
            final Mac hmac = Mac.getInstance(hmacMode);
            hmac.init(new SecretKeySpec(key, hmacMode));
            return result((Object) hmac.doFinal(data));
        }
    }

    public static class Tier3 extends Tier2 {
        private Map<String, String> deviceInfo;

        @Override
        public Map<String, String> getDeviceInfo() {
            if (deviceInfo == null) deviceInfo = DataCard.deviceInfo("SC03D Signer");
            return deviceInfo;
        }

        // ----------------------------------------------------------------------- //

        @Callback(direct = true, limit = 1, doc = "function([bitLen:number]):userdata, userdata -- Generates key pair. Returns: public, private keys. Allowed key lengths: 256, 384 bits.")
        public Object[] generateKeyPair(Context context, Arguments args) throws Exception {
            checkCost(Settings.get().dataCardAsymmetric);

            final int bitLen = args.optInteger(0, 384);
            if (bitLen != 256 && bitLen != 384)
                throw new IllegalArgumentException("invalid key length, must be 256 or 384");

            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(bitLen, SecureRandomInstance.get());
            final KeyPair kp = kpg.generateKeyPair();

            return result(new ECUserdata(kp.getPublic()), new ECUserdata(kp.getPrivate()));
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, type:string):userdata -- Restores key from its string representation.")
        public Object[] deserializeKey(Context context, Arguments args) throws Exception {
            final byte[] data = simpleCost(context, args);
            final String t = args.checkString(1);

            return result(new ECUserdata(ECUserdata.deserializeKey(t, data)));
        }

        @Callback(direct = true, limit = 1, doc = "function(priv:userdata, pub:userdata):string -- Generates a shared key. ecdh(a.priv, b.pub) == ecdh(b.priv, a.pub)")
        public Object[] ecdh(Context context, Arguments args) throws Exception {
            checkCost(Settings.get().dataCardAsymmetric);
            final Key privKey = checkUserdata(args, 0, Optional.of(false)).value;
            final Key pubKey = checkUserdata(args, 1, Optional.of(true)).value;

            final KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(privKey);
            ka.doPhase(pubKey, true);
            return result((Object) ka.generateSecret());
        }

        @Callback(direct = true, limit = 1, doc = "function(data:string, key:userdata[, sig:string]):string or boolean -- Signs or verifies data.")
        public Object[] ecdsa(Context context, Arguments args) throws Exception {
            final byte[] data = asymmetricCost(context, args);
            final ECUserdata key = checkUserdata(args, 1, Optional.empty());
            final byte[] sig = args.optByteArray(2, null);

            final Signature sign = Signature.getInstance("SHA256withECDSA");
            if (sig != null) {
                // Verify mode
                if (key.value instanceof PublicKey publicKey) {
                    sign.initVerify(publicKey);
                    sign.update(data);
                    return result(sign.verify(sig));
                } else throw new IllegalArgumentException("public key expected");
            } else {
                // Sign mode
                if (key.value instanceof PrivateKey k) {
                    sign.initSign(k);
                    sign.update(data);
                    return result((Object) sign.sign());
                } else throw new IllegalArgumentException("private key expected");
            }
        }

        // ----------------------------------------------------------------------- //

        private ECUserdata checkUserdata(Arguments args, int i, Optional<Boolean> isPublic) {
            final Object value = args.checkAny(i);
            if (value instanceof ECUserdata userdata) {
                if (isPublic.map(p -> p == userdata.isPublic()).orElse(true)) return userdata;
                else throw new IllegalArgumentException(
                        (isPublic.get() ? "public" : "private") + " key expected at " + (i + 1));
            } else if (value == null) {
                throw new IllegalArgumentException("bad argument #" + (i + 1) + " (userdata expected, got no value)");
            } else {
                throw new IllegalArgumentException("bad argument #" + (i + 1) + " (userdata expected, got " + value.getClass().getName() + ")");
            }
        }
    }

    public static class ECUserdata extends AbstractValue {
        public static final String PrivateTypeName = "ec-private";
        public static final String PublicTypeName = "ec-public";

        public Key value;

        public ECUserdata(Key value) {
            this.value = value;
        }

        // Empty constructor for deserialization.
        public ECUserdata() {
            this(null);
        }

        public boolean isPublic() {
            return value instanceof ECPublicKey;
        }

        public String keyType() {
            return isPublic() ? PublicTypeName : PrivateTypeName;
        }

        // ----------------------------------------------------------------------- //

        @Callback(direct = true, doc = "function():boolean -- Returns whether key is public.")
        public Object[] isPublic(Context context, Arguments args) {
            return result(isPublic());
        }

        @Callback(direct = true, doc = "function():string -- Returns type of key.")
        public Object[] keyType(Context context, Arguments args) {
            return result(keyType());
        }

        @Callback(direct = true, limit = 4, doc = "function():string -- Returns string representation of key. Result is binary data.")
        public Object[] serialize(Context context, Arguments args) {
            return result((Object) value.getEncoded());
        }

        // ----------------------------------------------------------------------- //

        private static final String TypeTag = "Type";
        private static final String DataTag = "Data";

        @Override
        public void loadData(CompoundTag nbt) {
            final String keyType = nbt.getString(TypeTag);
            final byte[] data = nbt.getByteArray(DataTag);
            try {
                value = deserializeKey(keyType, data);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void saveData(CompoundTag nbt) {
            nbt.putString(TypeTag, keyType());
            nbt.putByteArray(DataTag, value.getEncoded());
        }

        public static Key deserializeKey(String typeName, byte[] data) throws Exception {
            if (PrivateTypeName.equals(typeName)) return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(data));
            else if (PublicTypeName.equals(typeName)) return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data));
            else throw new IllegalArgumentException("invalid key type, must be ec-public or ec-private");
        }
    }
}
