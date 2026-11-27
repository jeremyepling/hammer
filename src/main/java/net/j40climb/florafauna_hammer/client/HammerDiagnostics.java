package net.j40climb.florafauna_hammer.client;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class HammerDiagnostics {
    public static void append(ItemStack stack, List<String> lines) {
        var stage = HammerStage.of(stack);
        if (stack.is(HammerRegistry.HAMMER.get())) lines.add(stage.label().getString());
        if (stack.has(HammerRegistry.MULTI_BLOCK_MINING.get())) {
            var mining = MiningModeData.effective(stack);
            lines.add("Mining: " + mining.shape().name() + ", radius=" + mining.radius());
        }
        var config = stack.get(HammerRegistry.TOOL_CONFIG.get());
        if (config != null) {
            var enchant = config.fortune() ? "Fortune" : "SilkTouch";
            lines.add("Tool Config: " + enchant + ", " + config.effectiveSpeed(stage).name());
        }
        if (HammerAction.LIGHTNING.availableOn(stack)) lines.add("Lightning: Yes");
        if (HammerAction.TELEPORT.availableOn(stack)) lines.add("Teleport: Yes");
        var throwable = stack.get(HammerRegistry.THROWABLE_ABILITY.get());
        if (throwable != null) {
            lines.add("Throw: dmg=" + throwable.damage() + ", range=" + throwable.maxRange());
            lines.add("  return=" + (throwable.autoReturn() ? "yes" : "no") + ", speed=" + throwable.returnSpeed());
        }
    }

    private HammerDiagnostics() {}
}
