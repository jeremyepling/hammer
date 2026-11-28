package net.j40climb.florafauna_hammer.setup;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.GroundSlam;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

class PlatformRegistration {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HammerMod.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, HammerMod.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HammerMod.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HammerMod.MOD_ID);

    public static final Supplier<AttachmentType<ChargedThrowPayload.Hold>> THROW_HOLD = ATTACHMENTS.register("throw_hold",
            () -> AttachmentType.builder(() -> new ChargedThrowPayload.Hold(ItemStack.EMPTY, InteractionHand.MAIN_HAND, -1, Level.OVERWORLD, 0)).build());
    public static final Supplier<AttachmentType<GroundSlam.Pending>> PENDING_SLAM = ATTACHMENTS.register("pending_slam",
            () -> AttachmentType.builder(() -> new GroundSlam.Pending(ItemStack.EMPTY, InteractionHand.MAIN_HAND, Level.OVERWORLD, 0)).build());

    static Supplier<Item> item(String path, Function<Item.Properties, Item> factory) {
        return ITEMS.registerItem(path, factory);
    }

    static <E extends Entity> Supplier<EntityType<E>> entity(ResourceKey<EntityType<?>> key, Supplier<EntityType<E>> factory) {
        return ENTITIES.register(key.identifier().getPath(), factory);
    }

    static <T> Supplier<DataComponentType<T>> component(String path, UnaryOperator<DataComponentType.Builder<T>> factory) {
        return COMPONENTS.registerComponentType(path, factory);
    }

    public static ChargedThrowPayload.@Nullable Hold throwHold(ServerPlayer player) {
        return player.getExistingDataOrNull(THROW_HOLD);
    }

    public static void setThrowHold(ServerPlayer player, ChargedThrowPayload.@Nullable Hold hold) {
        if (hold == null) player.removeData(THROW_HOLD);
        else player.setData(THROW_HOLD, hold);
    }

    public static GroundSlam.@Nullable Pending pendingSlam(ServerPlayer player) {
        return player.getExistingDataOrNull(PENDING_SLAM);
    }

    public static void setPendingSlam(ServerPlayer player, GroundSlam.@Nullable Pending slam) {
        if (slam == null) player.removeData(PENDING_SLAM);
        else player.setData(PENDING_SLAM, slam);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        ENTITIES.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
    }
}
