package li.cil.oc.util;

import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains the logic used by computers' internal "speakers".
 * It can generate square waves with a specific frequency and duration
 * and will play them through OpenAL, acquiring sources as necessary.
 * Tones that have finished playing are disposed automatically in the
 * tick handler.
 * <p>
 * Client only. {@link #init()} registers the tick handler (formerly
 * {@code MinecraftForge.EVENT_BUS.register(Audio)} in the client proxy).
 */
public final class Audio {
    private Audio() {
    }

    private static int sampleRate() {
        return Settings.get().beepSampleRate;
    }

    private static int amplitude() {
        return Settings.get().beepAmplitude;
    }

    private static float maxDistance() {
        return Settings.get().beepRadius;
    }

    private static final Set<Source> sources = new HashSet<>();

    private static float volume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
    }

    private static boolean disableAudio = false;

    private static boolean initialized = false;

    /**
     * Registers the client tick handler that cleans up finished sources.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(minecraft -> update());
    }

    public static void play(float x, float y, float z, int frequencyInHz, int durationInMilliseconds) {
        play(x, y, z, ".", frequencyInHz, durationInMilliseconds);
    }

    public static void play(float x, float y, float z, String pattern) {
        play(x, y, z, pattern, 1000, 200);
    }

    public static void play(float x, float y, float z, String pattern, int frequencyInHz) {
        play(x, y, z, pattern, frequencyInHz, 200);
    }

    public static void play(float x, float y, float z, String pattern, int frequencyInHz, int durationInMilliseconds) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        final float distanceBasedGain = (float) Math.max(0, 1 - mc.player.position().distanceTo(new Vec3(x, y, z)) / maxDistance());
        final float gain = distanceBasedGain * volume();
        if (gain <= 0 || amplitude() <= 0) return;

        if (disableAudio || !isOpenALAvailable()) {
            // Fallback audio generation, using built-in Minecraft sound. This can be
            // necessary on certain systems with audio cards that do not have enough
            // memory. May still fail, but at least we can say we tried!
            // Valid range is 20-2000Hz, clamp it to that and get a relative value.
            // MC's pitch system supports a minimum pitch of 0.5, however, so up it
            // by that.
            final float clampedFrequency = Math.min(Math.max(frequencyInHz - 20, 0), 1980) / 1980f + 0.5f;
            int delay = 0;
            for (char ch : pattern.toCharArray()) {
                final SimpleSoundInstance record = new SimpleSoundInstance(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, gain, clampedFrequency, SoundInstance.createUnseededRandom(), BlockPos.containing(x, y, z));
                if (delay == 0) mc.getSoundManager().play(record);
                else mc.getSoundManager().playDelayed(record, delay);
                delay += Math.max((ch == '.' ? durationInMilliseconds : 2 * durationInMilliseconds) * 20 / 1000, 1);
            }
        } else {
            final int sampleRate = sampleRate();
            final int amplitude = amplitude();
            final char[] chars = pattern.toCharArray();
            final int[] sampleCounts = new int[chars.length];
            int sampleSum = 0;
            for (int i = 0; i < chars.length; i++) {
                sampleCounts[i] = (chars[i] == '.' ? durationInMilliseconds : 2 * durationInMilliseconds) * sampleRate / 1000;
                sampleSum += sampleCounts[i];
            }
            // 50ms pause between pattern parts.
            final int pauseSampleCount = 50 * sampleRate / 1000;
            final ByteBuffer data = BufferUtils.createByteBuffer(sampleSum + (sampleCounts.length - 1) * pauseSampleCount);
            final float step = frequencyInHz / (float) sampleRate;
            float offset = 0f;
            for (int sampleCount : sampleCounts) {
                for (int sample = 0; sample < sampleCount; sample++) {
                    final double angle = 2 * Math.PI * offset;
                    final int value = ((byte) (Math.signum(Math.sin(angle)) * amplitude)) ^ 0x80;
                    offset += step;
                    if (offset > 1) offset -= 1;
                    data.put((byte) value);
                }
                if (data.hasRemaining()) {
                    for (int sample = 0; sample < pauseSampleCount; sample++) {
                        data.put((byte) 127);
                    }
                }
            }
            data.rewind();

            // Watch out for sound cards running out of memory... this apparently
            // really does happen. I'm assuming this is due to too many sounds being
            // kept loaded, since from what I can see OC's releasing its audio
            // memory as it should.
            try {
                synchronized (sources) {
                    sources.add(new Source(x, y, z, data, gain));
                }
            } catch (OpenALException e) {
                if (e.errorCode == AL10.AL_OUT_OF_MEMORY) {
                    // Well... let's just stop here.
                    OpenComputers.log.info("Couldn't play computer speaker sound because your sound card ran out of memory. Either your sound card is just really low-end, or there are just too many sounds in use already by other mods. Disabling computer speakers to avoid spamming your log file now.");
                    disableAudio = true;
                } else {
                    OpenComputers.log.warn("Error playing computer speaker sound.", e);
                }
            }
        }
    }

    /**
     * Whether Minecraft's sound engine has a current OpenAL context. It does not when the
     * system has no audio device (or the engine failed to start); every AL call would then
     * throw an IllegalStateException.
     */
    private static boolean isOpenALAvailable() {
        try {
            return org.lwjgl.openal.ALC10.alcGetCurrentContext() != 0L;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void update() {
        if (!disableAudio && isOpenALAvailable()) {
            synchronized (sources) {
                sources.removeIf(Source::checkFinished);
            }

            // Clear error stack.
            try {
                AL10.alGetError();
            } catch (UnsatisfiedLinkError | IllegalStateException e) {
                OpenComputers.log.warn("Negotiations with OpenAL broke down, disabling sounds.");
                disableAudio = true;
            }
        }
    }

    private static final class Source {
        final float x;
        final ByteBuffer data;
        final float gain;
        final int source;
        final int buffer;

        Source(float x, float y, float z, ByteBuffer data, float gain) {
            this.x = x;
            this.data = data;
            this.gain = gain;

            // Clear error stack.
            AL10.alGetError();

            final int buffer = AL10.alGenBuffers();
            checkALError();

            try {
                AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, data, sampleRate());
                checkALError();

                final int source = AL10.alGenSources();
                checkALError();

                try {
                    AL10.alSourceQueueBuffers(source, buffer);
                    checkALError();

                    AL10.alSource3f(source, AL10.AL_POSITION, x, y, z);
                    AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, maxDistance());
                    AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, maxDistance());
                    AL10.alSourcef(source, AL10.AL_GAIN, gain * 0.3f);
                    checkALError();

                    AL10.alSourcePlay(source);
                    checkALError();

                    this.source = source;
                    this.buffer = buffer;
                } catch (Throwable t) {
                    AL10.alDeleteSources(source);
                    throw t;
                }
            } catch (Throwable t) {
                AL10.alDeleteBuffers(buffer);
                throw t;
            }
        }

        boolean checkFinished() {
            if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(source);
                AL10.alDeleteBuffers(buffer);
                return true;
            }
            return false;
        }
    }

    // Having the error code in an accessible way is really cool, you know.
    public static class OpenALException extends RuntimeException {
        public final int errorCode;

        public OpenALException(int errorCode) {
            this.errorCode = errorCode;
        }
    }

    // Custom implementation of Util.checkALError() that uses our custom exception.
    public static void checkALError() {
        final int errorCode = AL10.alGetError();
        if (errorCode != AL10.AL_NO_ERROR) {
            throw new OpenALException(errorCode);
        }
    }
}
