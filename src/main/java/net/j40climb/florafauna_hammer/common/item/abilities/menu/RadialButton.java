package net.j40climb.florafauna_hammer.common.item.abilities.menu;

//? if <26.3 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else if <26.4 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?}
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//? if <26.4 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.client.gui.GuiPipeline;
*///?}
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** A focusable widget whose pointer target follows its annular sector. */
final class RadialButton extends Button {
    private final int cx, cy, inner, outer;
    private final double direction, halfAngle;
    private final ItemStack icon;
    private final boolean compact;
    private final @Nullable Runnable alternate;
    private @Nullable Component caption;
    private boolean selected, favorite;

    RadialButton(int cx, int cy, int outer, int inner, int index, int count, Component label,
                 @Nullable Component caption, ItemStack icon, OnPress press, @Nullable Runnable alternate) {
        super(0, 0, 16, 16, label, press, DEFAULT_NARRATION);
        this.cx = cx;
        this.cy = cy;
        this.outer = outer;
        this.inner = inner;
        this.caption = caption;
        this.icon = icon;
        compact = outer < 95 && count >= 7 && !icon.isEmpty();
        this.alternate = alternate;
        direction = -Math.PI / 2 + Math.TAU * index / count;
        halfAngle = Math.PI / count;
        double radius = (outer + inner) / 2.0;
        setPosition(cx + (int)Math.round(radius * Math.cos(direction)) - 8,
                cy + (int)Math.round(radius * Math.sin(direction)) - 8);
    }

    void setSelected(boolean value) { selected = value; }
    void setFavorite(boolean value) { favorite = value; }
    void setCaption(@Nullable Component value) { caption = value; }

    boolean toggleFavorite() {
        if (!isActive() || alternate == null) return false;
        alternate.run();
        return true;
    }

    boolean contains(double x, double y) {
        double dx = x - cx, dy = y - cy;
        double r2 = dx * dx + dy * dy;
        if (r2 < inner * inner || r2 >= outer * outer) return false;
        double angle = Math.IEEEremainder(Math.atan2(dy, dx) - direction, Math.TAU);
        return angle >= -halfAngle && angle < halfAngle;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return isActive() && contains(x, y);
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return super.isValidClickButton(info) || (info.button() == InputConstants.MOUSE_BUTTON_RIGHT && alternate != null);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean twice) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) toggleFavorite();
        else super.onClick(event, twice);
    }

    @Override
    //? if <26.4 {
    protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void extractContents(GuiGraphicsExtractor gui) {
        int mouseX = gui.mouseX(), mouseY = gui.mouseY();
    *///?}
        isHovered = contains(mouseX, mouseY);
        boolean highlighted = active && (isHovered || isFocused());
        int rim = !active ? 0xff514958 : selected ? 0xffffcc72 : highlighted ? 0xffdfb8ff : 0xff79678e;
        int face = !active ? 0xed25232c : highlighted ? 0xf7563c6d : selected ? 0xf548374b : 0xee302836;
        var pose = new Matrix3x2f(gui.pose());
        var bounds = new ScreenRectangle(cx - outer, cy - outer, outer * 2, outer * 2).transformMaxBounds(pose);
        double gap = 0.9 / ((outer + inner) * 0.5);
        ClientSetup.submit(gui, new Sector(pose, bounds, cx, cy, inner, outer,
                direction - halfAngle + gap, direction + halfAngle - gap, face, rim));

        var font = Minecraft.getInstance().font;
        int x = getX() + 8;
        int width = Math.max(30, (int)Math.min(outer - inner + 8,
                (outer + inner) * Math.sin(Math.min(halfAngle, Math.PI / 2)) - 7));
        // A short unbroken name can extend past the conservative wrap width at the label radius.
        String label = getMessage().getString();
        int labelWidth = width;
        if (!label.contains(" ") && font.width(label) <= width + 12) labelWidth = Math.max(width, font.width(label));
        List<FormattedCharSequence> lines = compact ? List.of() : font.split(getMessage(), labelWidth);
        var details = compact || caption == null ? List.<FormattedCharSequence>of() : font.split(caption, width);
        int iconHeight = icon.isEmpty() ? 0 : 18;
        int y = getY() + 8 - (iconHeight + (lines.size() + details.size()) * font.lineHeight) / 2;
        if (!icon.isEmpty()) {
            gui.item(icon, x - 8, y);
            if (!active) {
                // Draw a small closed padlock, also readable at compact GUI scales.
                gui.fill(x + 4, y + 8, x + 10, y + 14, 0xff28232d);
                gui.outline(x + 5, y + 5, 4, 6, 0xffc4bcca);
                gui.fill(x + 4, y + 9, x + 10, y + 14, 0xffc4bcca);
            }
            if (favorite && active) gui.text(font, "★", x + 9, y, 0xffffd778);
            y += iconHeight;
        }
        for (var line : lines) {
            gui.centeredText(font, line, x, y, !active ? 0xff9d95a5 : selected ? 0xffffdfa5 : 0xfffaf3ff);
            y += font.lineHeight;
        }
        for (var line : details) {
            gui.centeredText(font, line, x, y, 0xffdbc6e9);
            y += font.lineHeight;
        }
    }

    static void drawHub(GuiGraphicsExtractor gui, int cx, int cy, int radius, int face, int rim) {
        var pose = new Matrix3x2f(gui.pose());
        var bounds = new ScreenRectangle(cx - radius, cy - radius, radius * 2, radius * 2).transformMaxBounds(pose);
        ClientSetup.submit(gui, new Sector(pose, bounds, cx, cy, 0, radius, 0, Math.TAU, face, rim));
    }

    private record Sector(Matrix3x2fc pose, ScreenRectangle bounds, int cx, int cy, int inner, int outer,
                          double start, double end, int face, int rim) implements GuiElementRenderState {
        //? if <26.4 {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        //?} else {
        /*@Override public GuiPipeline pipeline() { return GuiPipeline.DEFAULT; }
        *///?}
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public @Nullable ScreenRectangle scissorArea() { return null; }

        @Override
        public void buildVertices(VertexConsumer out) {
            int segments = Math.max(2, (int)Math.ceil((end - start) * outer / 5));
            double x = Math.cos(start), y = Math.sin(start);
            for (int i = 1; i <= segments; i++) {
                double angle = start + (end - start) * i / segments;
                double nx = Math.cos(angle), ny = Math.sin(angle);
                band(out, x, y, nx, ny, inner, outer - 2, face);
                band(out, x, y, nx, ny, outer - 2, outer, rim);
                x = nx;
                y = ny;
            }
        }

        private void band(VertexConsumer out, double x, double y, double nx, double ny, int from, int to, int color) {
            vertex(out, x, y, from, color);
            vertex(out, nx, ny, from, color);
            vertex(out, nx, ny, to, color);
            vertex(out, x, y, to, color);
        }

        private void vertex(VertexConsumer out, double x, double y, int radius, int color) {
            out.addVertexWith2DPose(pose, (float)(cx + x * radius), (float)(cy + y * radius)).setColor(color);
        }
    }
}
