package net.sevenstars.middleearth.client.renderer.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.sevenstars.middleearth.client.TexturedRenderLayersME;
import net.sevenstars.middleearth.entity.EntityModelLayersME;
import net.sevenstars.middleearth.entity.beard.PlayerBeard;
import net.sevenstars.middleearth.gui.grooming.GroomingScreen;
import net.sevenstars.middleearth.utils.ItemTagsME;
import net.sevenstars.middleearth.registries.AtlasesME;

/**
 * A dwarf player's beard ({@link PlayerBeard}), drawn as the dwarf NPCs' are: the beard on the face and chin over
 * the head, and the long beard hanging over the chest (and any armour) on the NPCs' hair model, turning with the
 * head. Closed helms hide the hanging beard, as they do the NPCs' (the character_helmet_should_hide_beard tag).
 * While the beard screen is open, the player's own beard shows the one being chosen.
 */
@Environment(EnvType.CLIENT)
public class BeardFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    /**
     * The face beard sits just outside the skin's outer (hat) layer, at 0.5: its chin part a little out from that and
     * its outer part a little further, so no skin's own stubble, mouth or mask on that layer shows through it. Both
     * stay inside helmets (1.0).
     */
    private static final Dilation FACE_DILATION = new Dilation(0.55F), FACE_OUTER_DILATION = new Dilation(0.6F);

    /** A head with the skin's two layers (uv 0,0 and 32,0), turned and placed as the player's head each frame. */
    private final ModelPart faceHead;
    private final ModelPart hairRoot;
    private final ModelPart hair;

    public BeardFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, LoadedEntityModels loader) {
        super(context);
        ModelData face = new ModelData();
        face.getRoot().addChild("head", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8, 8, 8, FACE_DILATION)
            .uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8, 8, 8, FACE_OUTER_DILATION), ModelTransform.NONE);
        this.faceHead = TexturedModelData.of(face, 64, 64).createModel().getChild("head");
        this.hairRoot = loader.getModelPart(EntityModelLayersME.NPC_ENTITY_HAIR);
        this.hair = hairRoot.getChild("hair");
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state,
                       float limbAngle, float limbDistance) {
        if (state.invisible) {
            return;
        }
        PlayerBeard beard = beardOf(state.id);
        if (beard == null) {
            return;
        }
        SpriteAtlasTexture atlas = AtlasesME.getAtlasFromPath(TexturedRenderLayersME.CHARACTER_ATLAS_TEXTURES);
        VertexConsumer buffer = vertexConsumers.getBuffer(TexturedRenderLayersME.getCharacterTexturesRenderLayer());
        int overlay = LivingEntityRenderer.getOverlay(state, 0.0F);

        faceHead.copyTransform(getContextModel().head);
        Sprite face = atlas.getSprite(beard.faceTexture());
        faceHead.render(matrices, face.getTextureSpecificVertexConsumer(buffer), light, overlay);

        Identifier hanging = beard.hangingTexture();
        if (hanging != null && !state.equippedHeadStack.isIn(ItemTagsME.CHARACTER_HELMET_HIDE_BEARD)) {
            hair.copyTransform(getContextModel().head);
            Sprite sprite = atlas.getSprite(hanging);
            hairRoot.render(matrices, sprite.getTextureSpecificVertexConsumer(buffer), light, overlay);
        }
    }

    /** The beard to draw on this player: the one being chosen on the beard screen for oneself, else their own. */
    private static PlayerBeard beardOf(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getId() == id && client.currentScreen instanceof GroomingScreen screen) {
            return screen.chosenBeard();
        }
        if (client.world == null) {
            return null;
        }
        Entity entity = client.world.getEntityById(id);
        return entity instanceof PlayerEntity player ? PlayerBeard.of(player) : null;
    }
}
