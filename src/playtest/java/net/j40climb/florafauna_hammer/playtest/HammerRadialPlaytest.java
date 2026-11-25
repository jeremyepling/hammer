package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.menu.HammerMenuScreen;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

final class HammerRadialPlaytest {
    private static final String MENU = "gui.florafauna.hammer_menu.";

    static void steps(Playtest t) {
        t.server("Equip a fresh Hammer in the test plot", HammerPlaytest::prepare);
        t.awaitClient("Receive the Hammer", mc -> mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get()));
        HammerPlaytest.openMenu(t);
        t.client("Verify ten mining choices on the left and eight abilities on the right", HammerRadialPlaytest::checkBothWheels);
        t.client("Releasing the default C key leaves the remapped V hold active", mc -> {
            Playtest.screen(mc).keyReleased(Playtest.keyEvent(InputConstants.KEY_C));
            Playtest.require(Playtest.screen(mc) instanceof HammerMenuScreen, "Unbound C closed the wheels");
        });
        t.screenshot("hammer-wheels");
        t.client("Select Walkable with an off-label click in the left wheel", mc -> {
            var wedge = HammerPlaytest.button(mc, shapeLabel(MiningShape.WALKABLE_TUNNEL));
            clickOuterSector(mc, wedge, HammerPlaytest.button(mc, CommonComponents.GUI_DONE));
        });
        t.awaitServer("Apply the left wedge before touching the right wheel", p ->
                p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.WALKABLE_TUNNEL);
        t.client("Select Scan directly on the right wheel", mc -> {
            checkBothWheels(mc);
            HammerPlaytest.click(mc, HammerPlaytest.button(mc, actionLabel(HammerAction.SCAN)));
        });
        t.awaitServer("Keep the selected mining shape while setting the grounded ability", p ->
                p.getMainHandItem().get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.SCAN
                        && p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.WALKABLE_TUNNEL);
        releaseOver(t, "mining then ability", actionLabel(HammerAction.THROW), MiningShape.WALKABLE_TUNNEL, HammerAction.SCAN);

        HammerPlaytest.openMenu(t);
        t.client("Choose Throw then Large during the same hold", mc -> {
            HammerPlaytest.click(mc, HammerPlaytest.button(mc, actionLabel(HammerAction.THROW)));
            select(mc, "shape.large_tunnel");
        });
        releaseOver(t, "ability then mining", shapeLabel(MiningShape.SINGLE), MiningShape.LARGE_TUNNEL, HammerAction.THROW);

        HammerPlaytest.openMenu(t);
        releaseOver(t, "hover only", shapeLabel(MiningShape.BORE), MiningShape.LARGE_TUNNEL, HammerAction.THROW);

        HammerPlaytest.openMenu(t);
        t.client("Choose only Walkable", mc -> select(mc, "shape.walkable_tunnel"));
        releaseOver(t, "mining only", actionLabel(HammerAction.STRIP), MiningShape.WALKABLE_TUNNEL, HammerAction.THROW);

        HammerPlaytest.openMenu(t);
        t.client("Choose only Scan", mc -> HammerPlaytest.click(mc, HammerPlaytest.button(mc, actionLabel(HammerAction.SCAN))));
        releaseOver(t, "ability only", shapeLabel(MiningShape.SINGLE), MiningShape.WALKABLE_TUNNEL, HammerAction.SCAN);
        HammerPlaytest.openMenu(t);
        t.client("Right-click Walkable to favorite it", mc -> favorite(mc, MiningShape.WALKABLE_TUNNEL));
        t.awaitServer("A secondary click favorites Walkable and preserves the selected mode", p -> {
            var mode = p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get());
            return mode.shape() == MiningShape.WALKABLE_TUNNEL && mode.isFavorite(MiningShape.WALKABLE_TUNNEL);
        });
        t.client("Left-click Walkable", mc -> select(mc, "shape.walkable_tunnel"));
        t.awaitServer("Persist Walkable as the mining shape", p ->
                p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.WALKABLE_TUNNEL);
        t.client("Move keyboard focus clockwise and favorite the focused Large wedge", mc -> {
            key(mc, InputConstants.KEY_RIGHT);
            key(mc, InputConstants.KEY_F);
        });
        t.awaitServer("F favorites Large without switching the selected shape", p -> {
            var mode = p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get());
            return mode.shape() == MiningShape.WALKABLE_TUNNEL && mode.isFavorite(MiningShape.LARGE_TUNNEL);
        });
        t.client("Apply the focused Large wedge with Enter", mc -> key(mc, InputConstants.KEY_RETURN));
        t.awaitServer("Persist Large Tunnel from the keyboard", p ->
                p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.LARGE_TUNNEL);
        t.screenshot("hammer-wheels-selected");
        t.client("Open settings from the right hub", mc -> select(mc, "settings"));
        t.screenshot("hammer-wheels-settings");
        t.client("Change the left wheel while Settings stays open", mc -> select(mc, "shape.bore"));
        t.client("Cycle the right wheel's speed setting", mc -> HammerPlaytest.click(mc, HammerPlaytest.button(mc,
                Component.translatable("gui.florafauna.tool_config.speed.efficiency"))));
        t.awaitServer("Keep Bore, Scan and Instabreak together", p -> {
            var tool = p.getMainHandItem();
            return tool.get(HammerRegistry.TOOL_CONFIG.get()).miningSpeed() == MiningSpeed.INSTABREAK
                    && tool.get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.SCAN
                    && tool.get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.BORE;
        });
        t.client("Backspace restores the eight right-hand abilities", mc -> {
            key(mc, InputConstants.KEY_BACKSPACE);
            checkBothWheels(mc);
        });
        t.client("Resize the disposable client to 680 by 480 at GUI scale two", mc -> {
            mc.options.guiScale().set(2);
            mc.getWindow().setWindowed(680, 480);
            mc.resizeGui();
        });
        t.awaitClient("Lay out both wheels on a narrow screen", mc -> Playtest.screen(mc) instanceof HammerMenuScreen
                && Playtest.screen(mc).width <= 350 && Playtest.screen(mc).height >= 240);
        t.client("Keep all choices on their own side in the narrow layout", HammerRadialPlaytest::checkBothWheels);
        t.screenshot("hammer-wheels-narrow");
        t.client("Select Throw at narrow width", mc -> HammerPlaytest.click(mc, HammerPlaytest.button(mc, actionLabel(HammerAction.THROW))));
        t.awaitServer("Apply Throw from the narrow right wheel", p -> p.getMainHandItem().get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.THROW);
        releaseOver(t, "narrow layout", shapeLabel(MiningShape.SINGLE), MiningShape.BORE, HammerAction.THROW);
    }

    private static void releaseOver(Playtest t, String gesture, Component hovered, MiningShape shape, HammerAction action) {
        long[] released = {0};
        t.client("Release over a different wedge after " + gesture, mc -> {
            var wedge = HammerPlaytest.button(mc, hovered);
            Playtest.screen(mc).mouseMoved(wedge.getX() + wedge.getWidth() / 2.0, wedge.getY() + wedge.getHeight() / 2.0);
            HammerPlaytest.releaseWheel(mc);
            released[0] = System.nanoTime();
        });
        t.awaitClient("Keep the wheel closed beyond the tap and hold windows after " + gesture, mc -> {
            Playtest.require(Playtest.screen(mc) == null, "Wheel reopened after " + gesture);
            return System.nanoTime() - released[0] >= 500_000_000L;
        });
        t.server("Preserve exactly the clicked choices after " + gesture, player -> {
            var tool = player.getMainHandItem();
            var actualShape = tool.get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape();
            var actualAction = tool.get(HammerRegistry.HAMMER_ACTION.get());
            Playtest.require(actualShape == shape && actualAction == action,
                    gesture + " expected " + shape + "/" + action + ", got " + actualShape + "/" + actualAction);
        });
    }

    private static void checkBothWheels(Minecraft mc) {
        int middle = Playtest.screen(mc).width / 2;
        for (var shape : MiningShape.values()) {
            var wedge = HammerPlaytest.button(mc, shapeLabel(shape));
            Playtest.require(wedge.getX() >= 0 && wedge.getRight() < middle, "Mining wedge off its side: " + shape);
        }
        for (var action : HammerAction.values()) {
            var wedge = HammerPlaytest.button(mc, actionLabel(action));
            Playtest.require(wedge.getX() > middle && wedge.getRight() <= Playtest.screen(mc).width, "Ability wedge off its side: " + action);
        }
    }

    private static Component shapeLabel(MiningShape shape) {
        return Component.translatable(MENU + "shape." + shape.getSerializedName());
    }

    static Component actionLabel(HammerAction action) {
        return switch (action) {
            case SCAN, TELEPORT -> Component.translatable(MENU + action.getSerializedName());
            default -> action.label();
        };
    }

    static void select(Minecraft mc, String choice) {
        HammerPlaytest.click(mc, HammerPlaytest.button(mc, Component.translatable(MENU + choice)));
    }

    static void favorite(Minecraft mc, MiningShape shape) {
        Button wedge = HammerPlaytest.button(mc, shapeLabel(shape));
        clickAt(mc, wedge.getX() + wedge.getWidth() / 2.0, wedge.getY() + wedge.getHeight() / 2.0, InputConstants.MOUSE_BUTTON_RIGHT);
    }

    private static void clickOuterSector(Minecraft mc, Button wedge, Button center) {
        double cx = center.getX() + center.getWidth() / 2.0;
        double cy = center.getY() + center.getHeight() / 2.0;
        double dx = wedge.getX() + wedge.getWidth() / 2.0 - cx;
        double dy = wedge.getY() + wedge.getHeight() / 2.0 - cy;
        // Probe just beyond the label's rectangular input target along the wedge's radial axis.
        double scale = 1 + Math.min((wedge.getWidth() / 2.0 + 3) / Math.abs(dx),
                (wedge.getHeight() / 2.0 + 3) / Math.abs(dy));
        double x = cx + dx * scale, y = cy + dy * scale;
        Playtest.require(x < wedge.getX() || x >= wedge.getRight() || y < wedge.getY() || y >= wedge.getBottom(),
                "Probe must hit the sector outside its nominal button rectangle");
        clickAt(mc, x, y, InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void clickAt(Minecraft mc, double x, double y, int button) {
        Playtest.screen(mc).mouseMoved(x, y);
        var click = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        Playtest.require(Playtest.screen(mc).mouseClicked(click, false), "Ignored radial mouse button " + button);
        Playtest.screen(mc).mouseReleased(click);
    }

    private static void key(Minecraft mc, int key) {
        var event = Playtest.keyEvent(key);
        Playtest.require(Playtest.screen(mc).keyPressed(event), "Ignored radial keyboard input " + key);
        Playtest.screen(mc).keyReleased(event);
    }
}
