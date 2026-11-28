package net.j40climb.florafauna_hammer.common.item.hammer;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum HammerStage implements StringRepresentable {
    FORGED("forged", 0), RESONANT("resonant", 64), TEMPEST("tempest", 96);

    public static final Codec<HammerStage> CODEC = StringRepresentable.fromEnum(HammerStage::values);
    public static final StreamCodec<ByteBuf, HammerStage> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC, NbtAccounter::defaultQuota);
    private final String name;
    private final int capacity;

    HammerStage(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component label() {
        return Component.translatable("item.hammer.hammer." + name);
    }

    public int capacity() {
        return capacity;
    }

    /** Component-enabled tools from other mods keep unrestricted access to their own components. */
    public static HammerStage of(ItemStack stack) {
        return stack.getItem() instanceof HammerItem
                ? stack.getOrDefault(HammerRegistry.HAMMER_STAGE.get(), FORGED) : TEMPEST;
    }

    public ItemStack createStack() {
        ItemStack stack = new ItemStack(HammerRegistry.HAMMER.get());
        stack.set(HammerRegistry.HAMMER_STAGE.get(), this);
        if (capacity > 0) stack.set(HammerRegistry.HAMMER_CHARGE.get(), capacity);
        return stack;
    }

    public static HammerStage required(MiningShape shape) {
        return switch (shape) {
            case FLAT_5X5, FLAT_7X7, SHAPELESS, LARGE_TUNNEL -> RESONANT;
            case SINGLE, FLAT_3X3, WALKABLE_TUNNEL, BORE, TUNNEL_UP, TUNNEL_DOWN -> FORGED;
        };
    }

    public static HammerStage required(HammerAction action) {
        return switch (action) {
            case SCAN -> RESONANT;
            case LIGHTNING, TELEPORT -> TEMPEST;
            case AUTO, STRIP, PATH, TILL, THROW -> FORGED;
        };
    }

    public static HammerStage required(MiningSpeed speed) {
        return speed == MiningSpeed.INSTABREAK ? TEMPEST : FORGED;
    }

    public boolean allows(MiningShape shape) { return compareTo(required(shape)) >= 0; }
    public boolean allows(HammerAction action) { return compareTo(required(action)) >= 0; }
    public boolean allows(MiningSpeed speed) { return compareTo(required(speed)) >= 0; }
}
