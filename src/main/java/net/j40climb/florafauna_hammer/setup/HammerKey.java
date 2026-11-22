package net.j40climb.florafauna_hammer.setup;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna_hammer.HammerMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum HammerKey {
    SUMMON_LIGHTNING("key.florafauna.summon_lightning", InputConstants.KEY_R),
    TELEPORT_SURFACE("key.florafauna.teleport_surface", InputConstants.KEY_M),
    THROW_ITEM("key.florafauna.throw_item", InputConstants.KEY_T),
    CYCLE_MINING_MODE("key.florafauna.cycle_mining_mode", InputConstants.KEY_C);

    static final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "hammer");

    private final Supplier<KeyMapping> mapping;

    HammerKey(String translationKey, int defaultKey) {
        mapping = Suppliers.memoize(() -> ClientSetup.createKey(translationKey, defaultKey));
    }

    public KeyMapping mapping() {
        return mapping.get();
    }

    static void registerAll(Consumer<KeyMapping> register) {
        for (HammerKey key : values()) register.accept(key.mapping());
    }
}
