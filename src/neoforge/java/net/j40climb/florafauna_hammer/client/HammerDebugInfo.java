package net.j40climb.florafauna_hammer.client;

import net.j40climb.florafauna_hammer.HammerMod;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.List;
import java.util.function.BiConsumer;

@EventBusSubscriber(modid = HammerMod.MOD_ID, value = Dist.CLIENT)
public final class HammerDebugInfo {
    @SubscribeEvent
    public static void offerDiagnostics(InterModEnqueueEvent event) {
        // The optional FloraFauna debug overlay accepts a callback using only shared game and JDK types.
        InterModComms.sendTo(HammerMod.MOD_ID, "florafauna", "hammer_tool_diagnostics",
                () -> (BiConsumer<ItemStack, List<String>>) HammerDiagnostics::append);
    }

    private HammerDebugInfo() {}
}
