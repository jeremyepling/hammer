package net.j40climb.florafauna_hammer.common.item.abilities.menu;

import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SelectHammerActionPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SelectMiningModePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.j40climb.florafauna_hammer.client.KeyInputEvents;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?} else {
/*import com.mojang.blaze3d.platform.InputConstants;
*///?}

public final class HammerMenuScreen extends Screen {
    private static final String MENU = "gui.florafauna.hammer_menu.";
    private static final String CONFIG = "gui.florafauna.tool_config.";

    private static final List<MiningShape> SHAPES = List.of(
            MiningShape.SINGLE, MiningShape.FLAT_3X3, MiningShape.FLAT_5X5, MiningShape.FLAT_7X7,
            MiningShape.WALKABLE_TUNNEL, MiningShape.LARGE_TUNNEL, MiningShape.BORE,
            MiningShape.SHAPELESS, MiningShape.TUNNEL_UP, MiningShape.TUNNEL_DOWN);

    private final int slot;
    private final ItemStack tool;
    private final HammerStage stage;
    private final List<RadialButton> leftWheel = new ArrayList<>();
    private final List<RadialButton> rightWheel = new ArrayList<>();
    private final Map<RadialButton, Component> details = new HashMap<>();
    private final Map<RadialButton, Component> hints = new HashMap<>();
    private final EnumMap<MiningShape, RadialButton> shapeButtons = new EnumMap<>(MiningShape.class);
    private final EnumMap<HammerAction, RadialButton> actionButtons = new EnumMap<>(HammerAction.class);
    private MiningModeData mining;
    private HammerAction action;
    private ToolConfig config;
    private boolean settingsOpen;
    private int leftX, rightX, cy, outer, inner;
    private Button rightHub;
    private boolean keyboardNavigation;
    private boolean favoriteKeyDown;
    private double pointerX, pointerY;

    public HammerMenuScreen(int slot, ItemStack stack) {
        super(Component.translatable("item.florafauna.hammer"));
        this.slot = slot;
        tool = stack.copy();
        stage = HammerStage.of(stack);
        mining = stack.has(HammerRegistry.MULTI_BLOCK_MINING.get()) ? MiningModeData.effective(stack) : null;
        action = stack.get(HammerRegistry.HAMMER_ACTION.get());
        config = stack.get(HammerRegistry.TOOL_CONFIG.get());
        if (config != null) config = config.withMiningSpeed(config.effectiveSpeed(stage));
        settingsOpen = action == null && config != null;
    }

    @Override
    protected void init() {
        outer = Math.min(138, Math.min((width - 24) / 4, (height - 90) / 2));
        inner = outer / 3;
        leftX = width / 2 - outer - 5;
        rightX = width / 2 + outer + 5;
        cy = height / 2 - 5;
        leftWheel.clear();
        rightWheel.clear();
        shapeButtons.clear();
        actionButtons.clear();
        details.clear();
        hints.clear();

        for (var shape : SHAPES) {
            var slice = wedge(leftWheel, SHAPES.size(), shortShape(shape), null, shapeIcon(shape),
                    ignored -> chooseShape(shape), () -> favorite(shape));
            slice.active = mining != null && stage.allows(shape);
            slice.setSelected(mining != null && shape == mining.shape());
            slice.setFavorite(mining != null && mining.isFavorite(shape));
            shapeButtons.put(shape, slice);
            details.put(slice, Component.translatable("mining_shape.florafauna." + shape.getSerializedName()));
            if (!stage.allows(shape)) hints.put(slice, requires(HammerStage.required(shape)));
        }
        addRenderableWidget(new HubButton(leftX, CommonComponents.GUI_DONE, tool, ignored -> onClose()));
        if (settingsOpen) {
            setting("enchantment", Items.ENCHANTED_BOOK, () -> config.fortune() ? "enchantment.fortune" : "enchantment.silk_touch",
                    ToolConfig::withToggledEnchantment);
            var speed = setting("mining_speed", Items.GOLDEN_PICKAXE, () -> "speed." + config.miningSpeed().getSerializedName(),
                    current -> {
                        var next = current.withNextMiningSpeed();
                        return stage.allows(next.miningSpeed()) ? next : next.withNextMiningSpeed();
                    });
            if (stage != HammerStage.TEMPEST) details.put(speed, label("speed_locked"));
            var vein = setting("vein_matching", Items.RAW_IRON, () -> config.exactVein() ? "vein.exact" : "vein.same_ore",
                    ToolConfig::withToggledVeinMatching);
            if (stage == HammerStage.FORGED) {
                vein.active = false;
                details.put(vein, requires(HammerStage.RESONANT));
            }
            setting("tunnel_lighting", Items.TORCH, () -> config.tunnelLighting() ? "lighting.on" : "lighting.off",
                    ToolConfig::withToggledTunnelLighting);
        } else {
            for (var choice : HammerAction.values()) {
                var slice = wedge(rightWheel, HammerAction.values().length, shortAction(choice), null, actionIcon(choice),
                        ignored -> chooseAction(choice), null);
                slice.active = action != null && choice.availableOn(tool);
                slice.setSelected(choice == action);
                actionButtons.put(choice, slice);
                details.put(slice, choice.label());
                if (!stage.allows(choice)) hints.put(slice, requires(HammerStage.required(choice)));
                if (choice == HammerAction.THROW && tool.is(HammerRegistry.HAMMER.get())) {
                    hints.put(slice, label(stage == HammerStage.FORGED ? "throw_locked" : "charged_throw"));
                }
            }
        }
        rightHub = addRenderableWidget(new HubButton(rightX, settingsOpen ? CommonComponents.GUI_BACK : label("settings"),
                new ItemStack(settingsOpen ? Items.COMPASS : Items.COMPARATOR), ignored -> toggleSettings()));
        rightHub.active = config != null && (!settingsOpen || action != null);
    }

    private void toggleSettings() {
        settingsOpen = !settingsOpen;
        rebuildWidgets();
        if (keyboardNavigation) setFocused(rightWheel.getFirst());
    }

    private RadialButton wedge(List<RadialButton> wheel, int count, Component label, Component caption, Item icon,
                               Button.OnPress select, Runnable alternate) {
        var slice = new RadialButton(wheel == leftWheel ? leftX : rightX, cy, outer, inner, wheel.size(), count,
                label, caption, new ItemStack(icon), select, alternate);
        wheel.add(addRenderableWidget(slice));
        return slice;
    }

    private void chooseShape(MiningShape shape) {
        mining = mining.withShape(shape);
        shapeButtons.forEach((candidate, button) -> button.setSelected(candidate == shape));
        ClientSetup.sendToServer(new SelectMiningModePayload(slot, shape, false));
    }

    private void favorite(MiningShape shape) {
        mining = mining.toggleFavorite(shape);
        shapeButtons.get(shape).setFavorite(mining.isFavorite(shape));
        ClientSetup.sendToServer(new SelectMiningModePayload(slot, shape, true));
    }

    private void chooseAction(HammerAction choice) {
        action = choice;
        actionButtons.forEach((candidate, button) -> button.setSelected(candidate == choice));
        ClientSetup.sendToServer(new SelectHammerActionPayload(slot, choice));
    }

    private RadialButton setting(String name, Item icon, Supplier<String> value, UnaryOperator<ToolConfig> change) {
        return wedge(rightWheel, 4, Component.translatable(CONFIG + value.get()), Component.translatable(CONFIG + name), icon, button -> {
            config = change.apply(config);
            button.setMessage(Component.translatable(CONFIG + value.get()));
            ClientSetup.sendToServer(new UpdateToolConfigPayload(slot, config));
        }, null);
    }

    private static Item shapeIcon(MiningShape shape) {
        return switch (shape) {
            case SINGLE -> Items.STONE;
            case FLAT_3X3 -> Items.STONE_BRICKS;
            case FLAT_5X5 -> Items.DEEPSLATE_BRICKS;
            case FLAT_7X7 -> Items.POLISHED_BLACKSTONE_BRICKS;
            case WALKABLE_TUNNEL -> Items.LEATHER_BOOTS;
            case LARGE_TUNNEL -> Items.MINECART;
            case BORE -> Items.IRON_PICKAXE;
            case SHAPELESS -> Items.DIAMOND_ORE;
            case TUNNEL_UP -> Items.QUARTZ_STAIRS;
            case TUNNEL_DOWN -> Items.DEEPSLATE_BRICK_STAIRS;
        };
    }

    private Item actionIcon(HammerAction choice) {
        return switch (choice) {
            case AUTO -> Items.COMPASS;
            case STRIP -> Items.IRON_AXE;
            case PATH -> Items.IRON_SHOVEL;
            case TILL -> Items.IRON_HOE;
            case THROW -> tool.getItem();
            //? if <26.3 {
            case LIGHTNING -> Items.LIGHTNING_ROD;
            //?} else {
            /*case LIGHTNING -> Items.LIGHTNING_ROD.weathering().unaffected();
            *///?}
            case TELEPORT -> Items.ENDER_PEARL;
            case SCAN -> Items.ECHO_SHARD;
        };
    }

    private static Component shortShape(MiningShape shape) {
        return Component.translatable(MENU + "shape." + shape.getSerializedName());
    }

    private static Component shortAction(HammerAction choice) {
        return switch (choice) {
            case TELEPORT, SCAN -> Component.translatable(MENU + choice.getSerializedName());
            default -> choice.label();
        };
    }

    private static Component requires(HammerStage required) {
        var core = required == HammerStage.TEMPEST ? HammerRegistry.TEMPEST_CORE.get() : HammerRegistry.RESONANCE_CORE.get();
        return Component.translatable(MENU + "requires", core.getDefaultInstance().getHoverName());
    }

    private static Component label(String key) {
        return Component.translatable(MENU + key);
    }

    private RadialButton pointedWedge(List<RadialButton> wheel, double x, double y) {
        if (keyboardNavigation) {
            return getFocused() instanceof RadialButton focused && wheel.contains(focused) ? focused : null;
        }
        for (var button : wheel) {
            if (button.contains(x, y)) return button;
        }
        return null;
    }

    @Override
    //? if <26.4 {
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    //?} else {
    /*public void extractRenderState(GuiGraphicsExtractor g) {
        super.extractRenderState(g);
        int mouseX = g.mouseX(), mouseY = g.mouseY();
    *///?}
        int headingY = cy - outer - 25;
        int detailY = cy + outer + 7;
        if (tool.is(HammerRegistry.HAMMER.get())) g.centeredText(font, stage.label(), width / 2, headingY - 10, 0xfff4e9ff);
        g.centeredText(font, label("mining"), leftX, headingY, 0xffffe1aa);
        g.centeredText(font, label(settingsOpen ? "settings" : "abilities"), rightX, headingY, 0xffffe1aa);
        if (mining != null) {
            g.centeredText(font, shortShape(mining.shape()), leftX, headingY + 11, 0xffffd17e);
            var pointed = pointedWedge(leftWheel, mouseX, mouseY);
            if (pointed != null) {
                g.centeredText(font, details.get(pointed), leftX, detailY, 0xfffbf0ff);
            }
            g.centeredText(font, hints.getOrDefault(pointed, label("favorites_hint")), leftX, detailY + 12, 0xffe1cbe9);
        }
        if (!settingsOpen && action != null) g.centeredText(font, action.label(), rightX, headingY + 11, 0xffffd17e);
        var pointed = pointedWedge(rightWheel, mouseX, mouseY);
        var release = Component.translatable(MENU + "release_hint", HammerKey.CYCLE_MINING_MODE.mapping().getTranslatedKeyMessage());
        g.centeredText(font, hints.getOrDefault(pointed, release), rightX, detailY + 12, 0xffe1cbe9);
        Component detail = details.get(pointed);
        if (detail == null && !settingsOpen && tool.is(HammerRegistry.HAMMER.get())) {
            detail = label(stage == HammerStage.TEMPEST ? "slam" : "slam_locked");
        }
        if (detail != null) g.centeredText(font, detail, rightX, detailY, 0xfffbf0ff);
    }

    private final class HubButton extends Button {
        private final int centerX;
        private final ItemStack icon;

        HubButton(int x, Component label, ItemStack icon, OnPress press) {
            super(x - inner, cy - inner, 2 * inner, 2 * inner, label, press, DEFAULT_NARRATION);
            this.centerX = x;
            this.icon = icon;
        }

        @Override
        public boolean isMouseOver(double x, double y) {
            return Math.hypot(x - centerX, y - cy) < inner;
        }

        @Override
        //? if <26.4 {
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        //?} else {
        /*protected void extractContents(GuiGraphicsExtractor g) {
            int mouseX = g.mouseX(), mouseY = g.mouseY();
        *///?}
            int radius = inner - 2;
            int edge = isMouseOver(mouseX, mouseY) || isFocused() ? 0xffffdf94 : 0xffa88c69;
            RadialButton.drawHub(g, centerX, cy, radius, 0xff251d2d, edge);
            g.item(icon, centerX - 8, cy - 12);
            g.centeredText(font, getMessage(), centerX, cy + 7, 0xfff8e7cf);
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        keyboardNavigation = false;
        pointerX = x;
        pointerY = y;
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (HammerKey.CYCLE_MINING_MODE.mapping().matchesMouse(event)) return true;
        mouseMoved(event.x(), event.y());
        boolean handled = super.mouseClicked(event, doubleClick);
        // A navigation click may have replaced the widget that just handled it.
        if (getFocused() != null && !children().contains(getFocused())) clearFocus();
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (HammerKey.CYCLE_MINING_MODE.mapping().matchesMouse(event)) {
            KeyInputEvents.releaseMenuHold(minecraft);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // A repeat of the held control must never activate a focused widget, even when remapped to Enter or F.
        if (HammerKey.CYCLE_MINING_MODE.mapping().matches(event)) return true;
        switch (event.key()) {
            //? if <26.3 {
            case GLFW.GLFW_KEY_BACKSPACE -> {
            //?} else {
            /*case InputConstants.KEY_BACKSPACE -> {
            *///?}
                if (settingsOpen && action != null) toggleSettings();
                else onClose();
                return true;
            }
            //? if <26.3 {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> {
            //?} else {
            /*case InputConstants.KEY_RIGHT, InputConstants.KEY_DOWN, InputConstants.KEY_LEFT, InputConstants.KEY_UP -> {
            *///?}
                keyboardNavigation = true;
                //? if <26.3 {
                int step = event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_DOWN ? 1 : -1;
                //?} else {
                /*int step = event.key() == InputConstants.KEY_RIGHT || event.key() == InputConstants.KEY_DOWN ? 1 : -1;
                *///?}
                var wedges = getFocused() == rightHub || rightWheel.contains(getFocused()) ? rightWheel : leftWheel;
                int index = wedges.indexOf(getFocused());
                if (index < 0 && step < 0) index = 0;
                for (int i = 0; i < wedges.size(); i++) {
                    index = Math.floorMod(index + step, wedges.size());
                    if (wedges.get(index).active) {
                        setFocused(wedges.get(index));
                        break;
                    }
                }
                return true;
            }
            //? if <26.3 {
            case GLFW.GLFW_KEY_F -> {
            //?} else {
            /*case InputConstants.KEY_F -> {
            *///?}
                if (mining == null) break;
                if (favoriteKeyDown) return true;
                favoriteKeyDown = true;
                var shape = pointedWedge(leftWheel, pointerX, pointerY);
                if (shape != null) return shape.toggleFavorite();
                return getFocused() instanceof RadialButton button && button.toggleFavorite();
            }
            //? if <26.3 {
            case GLFW.GLFW_KEY_TAB -> keyboardNavigation = true;
            //?} else {
            /*case InputConstants.KEY_TAB -> keyboardNavigation = true;
            *///?}
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (HammerKey.CYCLE_MINING_MODE.mapping().matches(event)) {
            KeyInputEvents.releaseMenuHold(minecraft);
            return true;
        }
        //? if <26.3 {
        if (event.key() == GLFW.GLFW_KEY_F) favoriteKeyDown = false;
        //?} else {
        /*if (event.key() == InputConstants.KEY_F) favoriteKeyDown = false;
        *///?}
        return super.keyReleased(event);
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.player.getInventory().getSelectedSlot() != slot
                || !minecraft.player.getMainHandItem().is(tool.getItem()) || HammerStage.of(minecraft.player.getMainHandItem()) != stage) onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
