package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//?}

/**
 * Event handlers for tool abilities.
 * These work with any item that has the appropriate DataComponents.
 */
//? if neoforge {
@EventBusSubscriber(modid = HammerMod.MOD_ID)
//?}
public class ToolAbilityEventHandlers {
    //? if neoforge {
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (tool.has(HammerRegistry.TOOL_CONFIG.get())) event.setNewSpeed(breakSpeed(tool, event.getOriginalSpeed()));
    }
    //?}

    public static float breakSpeed(ItemStack tool, float original) {
        ToolConfig config = tool.get(HammerRegistry.TOOL_CONFIG.get());
        if (config == null) return original;
        return switch (config.effectiveSpeed(HammerStage.of(tool))) {
            case STANDARD -> original;
            case EFFICIENCY -> 35.0F;
            case INSTABREAK -> 100.0F;
        };
    }
}
