package li.cil.oc.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import org.lwjgl.opengl.GL11;

// This class has evolved into a wrapper for RenderSystem that basically does
// nothing but call the corresponding RenderSystem methods and then also
// forcefully applies whatever that call *should* do. This way the state
// manager's internal state is kept up-to-date but we also avoid issues with
// that state being incorrect causing wrong behavior (I've had too many render
// bugs where textures were not bound correctly or state was not updated
// because the state manager thought it already was in the state to change to,
// so I frankly don't care if this is less performant anymore).
//
// Client only.
public final class RenderState {
    private RenderState() {
    }

    public static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GL11.GL_NO_ERROR:
                return "No error";
            case GL11.GL_INVALID_ENUM:
                return "Enum argument out of range";
            case GL11.GL_INVALID_VALUE:
                return "Numeric argument out of range";
            case GL11.GL_INVALID_OPERATION:
                return "Operation illegal in current state";
            case GL11.GL_STACK_OVERFLOW:
                return "Command would cause a stack overflow";
            case GL11.GL_STACK_UNDERFLOW:
                return "Command would cause a stack underflow";
            case GL11.GL_OUT_OF_MEMORY:
                return "Not enough memory left to execute command";
            default:
                return String.format("Unknown [0x%X]", errorCode);
        }
    }

    public static void checkError(String where) {
        // glGetError forces a sync with the GPU, so don't call it unless errors get logged.
        if (Settings.get().logOpenGLErrors) {
            final int error = GL11.glGetError();
            if (error != 0) {
                OpenComputers.log.warn("GL ERROR @ " + where + ": " + getErrorString(error));
            }
        }
    }

    /**
     * Like {@link PoseStack#scale}, but also correct for negative (mirroring) scale factors: the
     * vanilla normal matrix update uses an inverse cube root approximation that fails for negative
     * values, which breaks lighting of everything rendered afterwards.
     */
    public static void mirrorScale(PoseStack matrix, float sx, float sy, float sz) {
        matrix.last().pose().scale(sx, sy, sz);
        if (sx != sy || sx != sz || sx <= 0) {
            final float isx = 1 / sx;
            final float isy = 1 / sy;
            final float isz = 1 / sz;
            final float invScale = isx * isy * isz;
            float normScale = Mth.fastInvCubeRoot(Math.abs(invScale));
            if (invScale < 0) {
                // Compensate for taking the absolute value of invScale.
                normScale = -normScale;
            }
            matrix.last().normal().scale(isx * normScale, isy * normScale, isz * normScale);
        }
    }

    /**
     * Display lists do not exist in the core profile used since 1.17, so we are
     * never compiling one.
     */
    public static boolean compilingDisplayList() {
        return false;
    }

    // pushAttrib/popAttrib currently breaks the RenderSystem because it doesn't
    // accordingly pushes/pops its cache, so it gets into an illegal state...
    // See https://gist.github.com/fnuecke/9a5b2499835fca9b52419277dc6239ca
    public static void pushAttrib() {
//    RenderSystem.glPushAttrib(mask)
    }

    public static void popAttrib() {
//    RenderSystem.popAttrib()
    }

    public static void disableEntityLighting() {
        // Fixed function lighting / color material no longer exist (1.17+), lighting
        // is done by the shaders of the respective render types.
    }

    public static void enableEntityLighting() {
        // See disableEntityLighting.
    }

    public static void makeItBlend() {
        RenderSystem.enableBlend();
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void disableBlend() {
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void setBlendAlpha(float alpha) {
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    public static void bindTexture(int id) {
        RenderSystem.bindTexture(id);
        RenderSystem.setShaderTexture(0, id);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }
}
