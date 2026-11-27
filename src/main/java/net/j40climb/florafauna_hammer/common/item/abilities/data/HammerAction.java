package net.j40climb.florafauna_hammer.common.item.abilities.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum HammerAction implements StringRepresentable {
    AUTO("auto"), STRIP("strip"), PATH("path"), TILL("till"), THROW("throw"),
    LIGHTNING("lightning"), TELEPORT("teleport"), SCAN("scan");

    public static final Codec<HammerAction> CODEC = StringRepresentable.fromEnum(HammerAction::values);
    public static final StreamCodec<ByteBuf, HammerAction> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    private final String name;

    HammerAction(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component label() {
        return Component.translatable("gui.hammer.hammer_action." + name);
    }

    public boolean availableOn(ItemStack tool) {
        if (!HammerStage.of(tool).allows(this)) return false;
        return switch (this) {
            case AUTO -> true;
            case STRIP, PATH, TILL -> !toolActions(tool.getOrDefault(HammerRegistry.MULTI_TOOL_ABILITY.get(), MultiToolAbilityData.NONE))
                    .equals(MultiToolAbilityData.NONE);
            case THROW -> tool.has(HammerRegistry.THROWABLE_ABILITY.get());
            case LIGHTNING -> tool.has(HammerRegistry.LIGHTNING_ABILITY.get());
            case TELEPORT -> tool.has(HammerRegistry.TELEPORT_SURFACE_ABILITY.get());
            case SCAN -> tool.is(HammerRegistry.HAMMER.get());
        };
    }

    public MultiToolAbilityData toolActions(MultiToolAbilityData enabled) {
        return switch (this) {
            case AUTO -> enabled;
            case STRIP -> enabled.strip() ? MultiToolAbilityData.AXE_ONLY : MultiToolAbilityData.NONE;
            case PATH -> enabled.flatten() ? MultiToolAbilityData.SHOVEL_ONLY : MultiToolAbilityData.NONE;
            case TILL -> enabled.till() ? MultiToolAbilityData.HOE_ONLY : MultiToolAbilityData.NONE;
            case THROW, LIGHTNING, TELEPORT, SCAN -> MultiToolAbilityData.NONE;
        };
    }
}
