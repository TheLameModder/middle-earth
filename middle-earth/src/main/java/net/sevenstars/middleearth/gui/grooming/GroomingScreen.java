package net.sevenstars.middleearth.gui.grooming;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.sevenstars.middleearth.MiddleEarth;
import net.sevenstars.middleearth.entity.beard.PlayerBeard;
import net.sevenstars.middleearth.gui.utils.CycledSelectionButtonType;
import net.sevenstars.middleearth.gui.utils.widgets.CustomWidget;
import net.sevenstars.middleearth.gui.utils.widgets.CycledSelectionWidget;
import net.sevenstars.middleearth.network.packets.client2server.PacketSetBeard;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * The Dwarven Grooming Kit's screen: the dwarf turning slowly in the window on the left, wearing the beard being
 * chosen, and on the right the beard's style, hair colour and ornaments, each cycled with arrows. Apply keeps it
 * (the server checks again), Cancel leaves the beard as it was.
 */
@Environment(EnvType.CLIENT)
public class GroomingScreen extends Screen {
    private static final String KEY = "screen." + MiddleEarth.MOD_ID + ".grooming";
    private static final Identifier BACKGROUND = Identifier.of(MiddleEarth.MOD_ID, "textures/gui/grooming.png");
    private static final Identifier BUTTONS = Identifier.of(MiddleEarth.MOD_ID, "textures/gui/faction_selection_buttons.png");
    private static final Identifier PREVIEW_BUTTONS = Identifier.of(MiddleEarth.MOD_ID, "textures/gui/widget/npc_preview_widget.png");
    private static final int WIDTH = 220, HEIGHT = 146;
    /** The window the dwarf stands in (inside its sunken border). */
    private static final int PREVIEW_X = 8, PREVIEW_Y = 18, PREVIEW_W = 72, PREVIEW_H = 104;
    /** The column of choices and the Apply / Cancel buttons. */
    private static final int COLUMN_X = 88, ROW_Y = 18, ROW_STEP = 32, BUTTONS_Y = 120;
    private static final int BUTTON_W = 52, BUTTON_H = 18;
    private static final int TEXT_COLOR = 0xFF404040;
    /** The dwarf faces the viewer at this yaw; it turns this many degrees a second until turned by hand. */
    private static final float FRONT = 180, SPIN = 24, STEP = 30;
    private static final Quaternionf VIEW = new Quaternionf().rotationXYZ(0.12F, 0, (float) Math.PI);

    /** The style, colour and ornament chosen: style 0 is none, then {@link PlayerBeard#STYLES}. */
    private int style, colour, ornament;
    private CycledSelectionWidget styleRow, colourRow, ornamentRow;
    private ButtonWidget applyButton, cancelButton, turnLeft, turnRight, turnReset;
    private int x, y;
    private float angle = FRONT;
    private boolean spinning = true;
    private long lastFrame;

    public GroomingScreen() {
        super(Text.translatable(KEY));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        x = (width - WIDTH) / 2;
        y = (height - HEIGHT) / 2;
        if (styleRow == null && client != null && client.player != null) {
            PlayerBeard beard = PlayerBeard.of(client.player);
            if (beard != null) {
                style = PlayerBeard.STYLES.indexOf(beard.style()) + 1;
                colour = PlayerBeard.COLOURS.indexOf(beard.colour());
                ornament = PlayerBeard.ORNAMENTS.indexOf(beard.ornament());
            }
        }
        styleRow = new CycledSelectionWidget(b -> style = cycle(style, -1, PlayerBeard.STYLES.size() + 1),
            b -> style = cycle(style, 1, PlayerBeard.STYLES.size() + 1), null, CycledSelectionButtonType.GOLD);
        colourRow = new CycledSelectionWidget(b -> colour = cycle(colour, -1, PlayerBeard.COLOURS.size()),
            b -> colour = cycle(colour, 1, PlayerBeard.COLOURS.size()), null, CycledSelectionButtonType.SILVER);
        ornamentRow = new CycledSelectionWidget(b -> ornament = cycle(ornament, -1, PlayerBeard.ORNAMENTS.size()),
            b -> ornament = cycle(ornament, 1, PlayerBeard.ORNAMENTS.size()), null, CycledSelectionButtonType.NORMAL);
        for (CycledSelectionWidget row : List.of(styleRow, colourRow, ornamentRow)) {
            row.getButtons().forEach(this::addDrawableChild);
        }

        applyButton = addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + ".apply"), b -> apply())
            .dimensions(x + WIDTH - 8 - BUTTON_W, y + BUTTONS_Y, BUTTON_W, BUTTON_H).build());
        cancelButton = addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + ".cancel"), b -> close())
            .dimensions(x + COLUMN_X, y + BUTTONS_Y, BUTTON_W, BUTTON_H).build());

        int turnY = y + PREVIEW_Y + PREVIEW_H + 6;
        int centre = x + PREVIEW_X + PREVIEW_W / 2;
        turnLeft = addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + ".turn_left"), b -> turn(STEP))
            .dimensions(centre - 5 - 14, turnY, 14, 9).build());
        turnReset = addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + ".turn_reset"), b -> {
            angle = FRONT;
            spinning = true;
        }).dimensions(centre - 3, turnY + 2, 6, 6).build());
        turnRight = addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + ".turn_right"), b -> turn(-STEP))
            .dimensions(centre + 5, turnY, 14, 9).build());
    }

    private static int cycle(int index, int by, int count) {
        return Math.floorMod(index + by, count);
    }

    private void turn(float by) {
        spinning = false;
        angle += by;
    }

    /** The beard being chosen, or null for none: shown on the dwarf here and in the world while this is open. */
    @Nullable
    public PlayerBeard chosenBeard() {
        if (style == 0) {
            return null;
        }
        return new PlayerBeard(PlayerBeard.STYLES.get(style - 1), PlayerBeard.COLOURS.get(colour), PlayerBeard.ORNAMENTS.get(ornament));
    }

    private void apply() {
        PlayerBeard beard = chosenBeard();
        ClientPlayNetworking.send(beard == null ? new PacketSetBeard("", "", "")
            : new PacketSetBeard(beard.style(), beard.colour(), beard.ornament()));
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The widgets are drawn here in Middle-earth's own look, so the vanilla ones are not (no super.render).
        CustomWidget.updateMouse(mouseX, mouseY);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, WIDTH, HEIGHT, 256, 256);
        context.drawText(textRenderer, title, x + (WIDTH - textRenderer.getWidth(title)) / 2, y + 6, TEXT_COLOR, false);

        drawDwarf(context, delta);
        drawPreviewButton(context, turnLeft, 0, mouseX, mouseY, 9);
        drawPreviewButton(context, turnReset, 28, mouseX, mouseY, 6);
        drawPreviewButton(context, turnRight, 14, mouseX, mouseY, 9);

        boolean shaven = style == 0;
        colourRow.enableArrows(!shaven);
        ornamentRow.enableArrows(!shaven);
        int rowX = x + COLUMN_X;
        drawRow(context, styleRow, rowX, y + ROW_Y, "style",
            shaven ? Text.translatable(KEY + ".style.none") : Text.translatable(KEY + ".style." + PlayerBeard.STYLES.get(style - 1)));
        drawRow(context, colourRow, rowX, y + ROW_Y + ROW_STEP, "colour",
            Text.translatable(KEY + ".colour." + PlayerBeard.COLOURS.get(colour)));
        drawRow(context, ornamentRow, rowX, y + ROW_Y + ROW_STEP * 2, "ornament",
            Text.translatable(KEY + ".ornament." + PlayerBeard.ORNAMENTS.get(ornament)));

        // Cancel (the red cross) and Apply (the green check), as on the onboarding screens.
        boolean overCancel = cancelButton.isMouseOver(mouseX, mouseY) || cancelButton.isFocused();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BUTTONS, cancelButton.getX(), cancelButton.getY(),
            overCancel ? 156 : 103, 55, BUTTON_W, BUTTON_H, 256, 256);
        boolean overApply = applyButton.isMouseOver(mouseX, mouseY) || applyButton.isFocused();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BUTTONS, applyButton.getX(), applyButton.getY(),
            103, overApply ? 37 : 19, BUTTON_W, BUTTON_H, 256, 256);
        if (overCancel) {
            context.drawTooltip(textRenderer, cancelButton.getMessage(), mouseX, mouseY);
        } else if (overApply) {
            context.drawTooltip(textRenderer, applyButton.getMessage(), mouseX, mouseY);
        }
    }

    private void drawRow(DrawContext context, CycledSelectionWidget row, int rowX, int rowY, String name, MutableText value) {
        context.drawText(textRenderer, Text.translatable(KEY + "." + name), rowX, rowY, TEXT_COLOR, false);
        row.drawAnchored(context, rowX, rowY + 10, true, value, textRenderer);
    }

    private void drawPreviewButton(DrawContext context, ButtonWidget button, int u, int mouseX, int mouseY, int step) {
        boolean over = button.isMouseOver(mouseX, mouseY) || button.isFocused();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, PREVIEW_BUTTONS, button.getX(), button.getY(), u, over ? step : 0,
            button.getWidth(), button.getHeight(), 256, 256);
    }

    /** The player in the window, from the knees up, turning; wearing the beard chosen here (BeardFeatureRenderer). */
    private void drawDwarf(DrawContext context, float delta) {
        ClientPlayerEntity player = client == null ? null : client.player;
        if (player == null) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        if (spinning && lastFrame != 0) {
            angle += SPIN * (now - lastFrame) / 1000F;
        }
        lastFrame = now;

        EntityRenderer<? super ClientPlayerEntity, ?> renderer = client.getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState state = renderer.getAndUpdateRenderState(player, delta);
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        playerState.bodyYaw = angle;
        playerState.relativeHeadYaw = 0;
        playerState.pitch = 0;
        playerState.displayName = null;
        playerState.hitbox = null;

        float height = player.getHeight();
        float shown = height * 0.72F;
        float scale = (PREVIEW_H - 4) / shown;
        Vector3f lift = new Vector3f(0, height - shown / 2, 0);
        int left = x + PREVIEW_X, top = y + PREVIEW_Y;
        context.addEntity(playerState, scale, lift, VIEW, null, left, top, left + PREVIEW_W, top + PREVIEW_H);
    }
}
