package net.j40climb.florafauna_hammer.test;

import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.j40climb.florafauna_hammer.HammerMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class FabricHammerGameTests {
    public static void initialize() {
        HammerGameTests.registerTests((id, structure, maxTicks, setupTicks, run) ->
                Registry.register(BuiltInRegistries.TEST_FUNCTION, id, run));
        DynamicRegistrySetupCallback.EVENT.register(registries -> registries.getOptional(Registries.TEST_INSTANCE).ifPresent(instances -> {
            net.minecraft.core.Holder<TestEnvironmentDefinition<?>> env = Registry.registerForHolder(registries.getOptional(Registries.TEST_ENVIRONMENT).orElseThrow(),
                    Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "default"), (TestEnvironmentDefinition<?>) new TestEnvironmentDefinition.AllOf());
            HammerGameTests.registerTests((id, structure, maxTicks, setupTicks, run) -> {
                // Fabric supplies its own empty test structure.
                var template = structure.equals(Identifier.withDefaultNamespace("empty"))
                        ? Identifier.fromNamespaceAndPath("fabric-gametest-api-v1", "empty") : structure;
                Registry.register(instances, id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
                        new TestData<>(env, template, maxTicks, setupTicks, true)));
            });
        }));
    }
}
