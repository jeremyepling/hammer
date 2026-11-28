package net.j40climb.florafauna_hammer.setup;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.GroundSlam;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

class PlatformRegistration {
    private static final AttachmentType<ChargedThrowPayload.Hold> THROW_HOLD = AttachmentRegistry.create(id("throw_hold"));
    private static final AttachmentType<GroundSlam.Pending> PENDING_SLAM = AttachmentRegistry.create(id("pending_slam"));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, path);
    }

    static Supplier<Item> item(String path, Function<Item.Properties, Item> factory) {
        var key = ResourceKey.create(Registries.ITEM, id(path));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        return () -> item;
    }

    static <E extends Entity> Supplier<EntityType<E>> entity(ResourceKey<EntityType<?>> key, Supplier<EntityType<E>> factory) {
        var entityType = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, factory.get());
        return () -> entityType;
    }

    static <T> Supplier<DataComponentType<T>> component(String path, UnaryOperator<DataComponentType.Builder<T>> factory) {
        var component = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(path), factory.apply(DataComponentType.builder()).build());
        return () -> component;
    }

    public static ChargedThrowPayload.@Nullable Hold throwHold(ServerPlayer player) {
        return player.getAttached(THROW_HOLD);
    }

    public static void setThrowHold(ServerPlayer player, ChargedThrowPayload.@Nullable Hold hold) {
        player.setAttached(THROW_HOLD, hold);
    }

    public static GroundSlam.@Nullable Pending pendingSlam(ServerPlayer player) {
        return player.getAttached(PENDING_SLAM);
    }

    public static void setPendingSlam(ServerPlayer player, GroundSlam.@Nullable Pending slam) {
        player.setAttached(PENDING_SLAM, slam);
    }
}
