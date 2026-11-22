package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.HammerMod;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class NeoForgeHammerGameTests {
    public static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf());
        HammerGameTests.registerTests((id, structure, maxTicks, setupTicks, run) -> event.registerTest(id,
                new SimpleGameTestInstance(new TestData<>(env, structure, maxTicks, setupTicks, true), run)));
    }
}
