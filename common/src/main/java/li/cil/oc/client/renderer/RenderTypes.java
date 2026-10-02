package li.cil.oc.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

/**
 * OpenComputers' custom render types.
 * <p>
 * Vanilla's {@code RenderType.create} is not accessible without access
 * transformers, so render types are built directly from their state shards
 * (which is all a composite render type does anyway).
 * <p>
 * Since 1.17 everything is drawn through shaders, so every type selects the
 * shader matching its vertex format.
 */
public abstract class RenderTypes extends RenderType {
    // ----------------------------------------------------------------------- //
    // Shaders not exposed as constants by vanilla.

    private static final ShaderStateShard POSITION_TEX_COLOR_SHADER = new ShaderStateShard(GameRenderer::getPositionTexColorShader);

    // ----------------------------------------------------------------------- //

    public static final ResourceLocation ROBOT_CHASSIS_TEXTURE = Textures.Model.Robot;

    /**
     * Robot chassis: entity style lighting (diffuse + lightmap), drawn as triangles.
     * Vertex layout: {@link DefaultVertexFormat#NEW_ENTITY} (position, color, uv, overlay, light, normal).
     */
    public static final RenderType ROBOT_CHASSIS = create(OpenComputers.ID + ":robot_chassis",
        DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1024, true, false,
        new TextureStateShard(ROBOT_CHASSIS_TEXTURE, false, false),
        RENDERTYPE_ENTITY_SOLID_SHADER,
        NO_TRANSPARENCY,
        LIGHTMAP,
        OVERLAY);

    /**
     * Robot status light, additive and full-bright.
     * Vertex layout: position, color, uv.
     */
    public static final RenderType ROBOT_LIGHT = create(OpenComputers.ID + ":robot_light",
        DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, false,
        new TextureStateShard(ROBOT_CHASSIS_TEXTURE, false, false),
        POSITION_COLOR_TEX_SHADER,
        LIGHTNING_TRANSPARENCY);

    // Upgrades rendered on robots use plain entity rendering now (the old custom
    // position/tex/normal format has no matching shader). Vertex layout:
    // {@link DefaultVertexFormat#NEW_ENTITY}.
    public static final RenderType UPGRADE_CRAFTING = RenderType.entitySolid(Textures.Model.UpgradeCrafting);

    public static final RenderType UPGRADE_GENERATOR = RenderType.entitySolid(Textures.Model.UpgradeGenerator);

    public static final RenderType UPGRADE_INVENTORY = RenderType.entitySolid(Textures.Model.UpgradeInventory);

    /**
     * Lines drawn on top of everything (multi-tool target). Vertex layout:
     * {@link DefaultVertexFormat#POSITION_COLOR_NORMAL} (normal = line direction).
     */
    public static final RenderType MFU_LINES = create(OpenComputers.ID + ":mfu_lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1024, false, false,
        RENDERTYPE_LINES_SHADER,
        new LineStateShard(OptionalDouble.of(2.0)),
        TRANSLUCENT_TRANSPARENCY,
        NO_DEPTH_TEST,
        NO_CULL,
        TRANSLUCENT_TARGET,
        COLOR_WRITE);

    /**
     * Quads drawn on top of everything (multi-tool target). Vertex layout: position, color.
     */
    public static final RenderType MFU_QUADS = create(OpenComputers.ID + ":mfu_quads",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
        POSITION_COLOR_SHADER,
        TRANSLUCENT_TRANSPARENCY,
        NO_DEPTH_TEST,
        NO_CULL,
        TRANSLUCENT_TARGET,
        COLOR_WRITE);

    /**
     * Additive, full-bright overlays using block atlas sprites (status lights).
     * Vertex layout: position, uv.
     */
    public static final RenderType BLOCK_OVERLAY = create(OpenComputers.ID + ":overlay_block",
        DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1024, false, false,
        BLOCK_SHEET_MIPPED,
        POSITION_TEX_SHADER,
        LIGHTNING_TRANSPARENCY);

    /**
     * Like {@link #BLOCK_OVERLAY} but with per-vertex color (used for fading).
     * Vertex layout: position, color, uv.
     */
    public static final RenderType BLOCK_OVERLAY_COLOR = create(OpenComputers.ID + ":overlay_block_color",
        DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 1024, false, false,
        BLOCK_SHEET_MIPPED,
        POSITION_COLOR_TEX_SHADER,
        LIGHTNING_TRANSPARENCY);

    /**
     * Text buffer backgrounds. Vertex layout: position, color.
     */
    public static final RenderType FONT_QUAD = create(OpenComputers.ID + ":font_quad",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1024, false, false,
        POSITION_COLOR_SHADER,
        COLOR_WRITE);

    // ----------------------------------------------------------------------- //

    /**
     * Texture state for font textures: always nearest magnification, minification
     * depending on the linear filtering setting.
     */
    private static EmptyTextureStateShard fontTexture(ResourceLocation texture, boolean linear) {
        return new EmptyTextureStateShard(() -> {
            final AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            tex.setFilter(false, false);
            final int id = tex.getId();
            RenderSystem.setShaderTexture(0, id);
            GlStateManager._bindTexture(id);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }, () -> {
        });
    }

    /**
     * Render type for a font texture. Vertex layout: position, color, uv.
     */
    public static RenderType createFontTex(String name, ResourceLocation texture, boolean linear) {
        return create(OpenComputers.ID + ":font_stat_" + name,
            DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 1024, false, false,
            fontTexture(texture, linear),
            POSITION_COLOR_TEX_SHADER,
            TRANSLUCENT_TRANSPARENCY);
    }

    /**
     * Render type for a quad with an arbitrary (non atlas) texture. Supported formats:
     * {@link DefaultVertexFormat#POSITION_TEX}, {@link DefaultVertexFormat#POSITION_TEX_COLOR},
     * {@link DefaultVertexFormat#POSITION_COLOR_TEX}.
     */
    public static RenderType createTexturedQuad(String name, ResourceLocation texture, VertexFormat format, boolean additive) {
        final ShaderStateShard shader;
        if (format == DefaultVertexFormat.POSITION_TEX_COLOR) shader = POSITION_TEX_COLOR_SHADER;
        else if (format == DefaultVertexFormat.POSITION_COLOR_TEX) shader = POSITION_COLOR_TEX_SHADER;
        else shader = POSITION_TEX_SHADER;
        return create(OpenComputers.ID + ":tex_quad_" + name,
            format, VertexFormat.Mode.QUADS, 1024, false, false,
            new TextureStateShard(texture, false, false),
            shader,
            additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY);
    }

    // ----------------------------------------------------------------------- //

    private static RenderType create(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                     boolean affectsCrumbling, boolean sortOnUpload, RenderStateShard... shards) {
        return new RenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, () -> {
            for (RenderStateShard shard : shards) shard.setupRenderState();
        }, () -> {
            for (RenderStateShard shard : shards) shard.clearRenderState();
        }) {
        };
    }

    private RenderTypes() {
        super(null, null, null, 0, false, false, null, null);
        throw new Error();
    }
}
