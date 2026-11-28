package net.j40climb.florafauna_hammer.client;

import net.j40climb.florafauna_hammer.common.item.abilities.networking.CycleMiningModePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.Minecraft;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.menu.HammerMenuScreen;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SelectMiningModePayload;
import net.minecraft.core.BlockPos;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;

public class KeyInputEvents {

    private static Player throwPlayer;
    private static net.minecraft.world.level.Level throwLevel;
    private static InteractionHand throwHand;
    private static int throwSlot;
    private static boolean waitForUseUp;
    private static boolean waitForThrowUp;

    /** The interaction callback has selected Throw after vanilla and tool interactions. */
    public static void onUseThrow(Player player, InteractionHand hand) {
        if (player != Minecraft.getInstance().player || waitForUseUp) return;
        if (throwHand == null) {
            throwPlayer = player;
            throwLevel = player.level();
            throwSlot = player.getInventory().getSelectedSlot();
            throwHand = hand;
        }
    }

    public static boolean suppressRepeatedUse() {
        return throwHand != null || waitForUseUp;
    }

    public static boolean canUseHammerBinding(Player player) {
        return player != null && (player.getMainHandItem().is(HammerRegistry.HAMMER.get())
                || player.getOffhandItem().is(HammerRegistry.HAMMER.get()));
    }

    private static boolean hammerBindingDown() {
        var key = HammerKey.USE_HAMMER.mapping();
        return !key.isUnbound() && key.isDown();
    }

    public static boolean hammerBindingDown(Player player) {
        return canUseHammerBinding(player) && hammerBindingDown();
    }

    private static void clearThrow() {
        throwPlayer = null;
        throwLevel = null;
        throwHand = null;
    }

    private static void updateThrow(Minecraft mc) {
        var player = mc.player;
        boolean useDown = mc.options.keyUse.isDown() || hammerBindingDown();
        if (!useDown) waitForUseUp = false;
        //? if <26.3 {
        boolean screenOpen = mc.screen != null;
        //?} else {
        /*boolean screenOpen = mc.gui.screen() != null;
        *///?}
        if (throwHand != null && (player != throwPlayer || player.level() != throwLevel || screenOpen || !player.isAlive() || player.isSpectator()
                || player.getInventory().getSelectedSlot() != throwSlot
                || !player.getItemInHand(throwHand).is(HammerRegistry.HAMMER.get()))) {
            if (player != null && mc.getConnection() != null) sendThrowAction(ChargedThrowPayload.CANCEL);
            waitForUseUp = useDown;
            clearThrow();
        }
        if (throwHand != null && !useDown) {
            sendThrowAction(ChargedThrowPayload.RELEASE);
            clearThrow();
        }
    }

    private static void sendThrowAction(int action) {
        ClientSetup.sendToServer(new ChargedThrowPayload(action));
    }

    private static final HammerControlGesture gesture = new HammerControlGesture();
    private static Player gesturePlayer;
    private static int gestureSlot = -1;
    private static boolean wasDown;
    private static boolean waitForRelease;
    private static boolean menuHold;

    // Observe physical edges before another input can arrive in the same tick. Tick polling also
    // supports injected key mappings and fires deadlines while the keyboard is idle.
    public static void onControlKey(net.minecraft.client.input.KeyEvent event, int action) {
        if (HammerKey.CYCLE_MINING_MODE.mapping().matches(event)) controlEdge(action);
    }

    public static void onControlMouse(int button, int action) {
        if (ClientSetup.boundKey(HammerKey.CYCLE_MINING_MODE.mapping()).equals(InputConstants.Type.MOUSE.getOrCreate(button))) controlEdge(action);
    }

    private static void controlEdge(int action) {
        var mc = Minecraft.getInstance();
        if (menuHold && action == InputConstants.RELEASE) {
            releaseMenuHold(mc);
            return;
        }
        // Escape, changing worlds, or another screen may have consumed the old release.
        //? if <26.3 {
        if (menuHold && action == InputConstants.PRESS && mc.screen == null) resetMenuHold();
        //?} else {
        /*if (menuHold && action == InputConstants.PRESS && mc.gui.screen() == null) resetMenuHold();
        *///?}
        updateControl(mc);
    }

    /** Opening a Screen clears KeyMapping.isDown. Its keyboard/mouse release is the authoritative edge. */
    public static void releaseMenuHold(Minecraft mc) {
        //? if <26.3 {
        if (mc.screen instanceof HammerMenuScreen screen) screen.onClose();
        //?} else {
        /*if (mc.gui.screen() instanceof HammerMenuScreen screen) screen.onClose();
        *///?}
        resetMenuHold();
        var key = HammerKey.CYCLE_MINING_MODE.mapping();
        key.setDown(false);
        while (key.consumeClick()) { } // Discard any repeat queued before the screen grabbed input.
    }

    private static void resetMenuHold() {
        menuHold = false;
        wasDown = false;
        waitForRelease = false;
        gesture.reset();
    }

    private static void updateControl(Minecraft mc) {
        var key = HammerKey.CYCLE_MINING_MODE.mapping();
        var player = mc.player;
        boolean down = key.isDown();
        int clicks = 0;
        while (key.consumeClick()) clicks++;
        if (menuHold) {
            if (player != gesturePlayer) {
                releaseMenuHold(mc);
                gesturePlayer = player;
            }
            return;
        }

        int slot = player == null ? -1 : player.getInventory().getSelectedSlot();
        var held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        boolean usable = held.has(HammerRegistry.MULTI_BLOCK_MINING.get()) || held.has(HammerRegistry.HAMMER_ACTION.get())
                || held.has(HammerRegistry.TOOL_CONFIG.get());
        //? if <26.3 {
        if (player != gesturePlayer || slot != gestureSlot || mc.screen != null || !usable) {
        //?} else {
        /*if (player != gesturePlayer || slot != gestureSlot || mc.gui.screen() != null || !usable) {
        *///?}
            gesture.reset();
            //? if <26.3 {
            waitForRelease = down && (wasDown || mc.screen != null || !usable);
            //?} else {
            /*waitForRelease = down && (wasDown || mc.gui.screen() != null || !usable);
            *///?}
            wasDown = false;
            gesturePlayer = player;
            gestureSlot = slot;
        }
        if (!down) waitForRelease = false;
        //? if <26.3 {
        if (mc.screen != null || !usable || waitForRelease) return;
        //?} else {
        /*if (mc.gui.screen() != null || !usable || waitForRelease) return;
        *///?}

        long now = System.nanoTime() / 1_000_000L;
        if (wasDown && !down) dispatch(mc, gesture.release(now));
        // A quick physical press and release can both occur between client ticks.
        if (!down) {
            for (int i = 0; i < clicks; i++) {
                dispatch(mc, gesture.press(now));
                dispatch(mc, gesture.release(now));
            }
        } else if (!wasDown) {
            dispatch(mc, gesture.press(now));
        }
        dispatch(mc, gesture.advance(now));
        wasDown = down;
    }

    private static void dispatch(Minecraft mc, HammerControlGesture.Action action) {
        switch (action) {
            case NONE -> { }
            case CYCLE -> ClientSetup.sendToServer(CycleMiningModePayload.INSTANCE);
            case SINGLE -> ClientSetup.sendToServer(new SelectMiningModePayload(gestureSlot, MiningShape.SINGLE, false));
            case MENU -> {
                menuHold = true;
                //? if <26.3 {
                mc.setScreen(new HammerMenuScreen(gestureSlot, mc.player.getMainHandItem()));
                //?} else {
                /*mc.gui.setScreen(new HammerMenuScreen(gestureSlot, mc.player.getMainHandItem()));
                *///?}
            }
        }
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        updateControl(mc);
        updateThrow(mc);
        var hammerUse = HammerKey.USE_HAMMER.mapping();
        while (hammerUse.consumeClick()) { } // Discard clicks when Minecraft is not handling gameplay input.
        Player player = mc.player;
        if (player == null)
            return;

        ItemStack itemStack = player.getMainHandItem();
        // The key mappings have to be consumed during the event or it will replay the event on each tick. Don't check anything for this outside of player null
        while (HammerKey.SUMMON_LIGHTNING.mapping().consumeClick()) {
            // Component-based check - works with any item that has LIGHTNING_ABILITY
            if (HammerAction.LIGHTNING.availableOn(itemStack)) {
                BlockPos targetPos = SpawnLightningPayload.targetFromView(player);
                ClientSetup.sendToServer(new SpawnLightningPayload(targetPos));
            }
        }
        while (HammerKey.TELEPORT_SURFACE.mapping().consumeClick()) {
            // Component-based check - works with any item that has TELEPORT_SURFACE_ABILITY
            if (HammerAction.TELEPORT.availableOn(itemStack)) {
                ClientSetup.sendToServer(TeleportToSurfacePayload.INSTANCE);
            }
        }
    }
}
