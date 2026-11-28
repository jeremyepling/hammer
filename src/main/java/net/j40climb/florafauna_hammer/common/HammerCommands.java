package net.j40climb.florafauna_hammer.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.minecraft.resources.Identifier;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HammerCommands {
    /** Permission check: OP level 2 (gamemaster) OR creative mode */
    private static boolean hasDevPermission(CommandSourceStack source) {
        return Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)).test(source) ||
                (source.getEntity() instanceof ServerPlayer p && p.isCreative());
    }

    // 26.4 serializes the command tree. Creative mode depends on the executing player.
    private static Command<CommandSourceStack> devCommand(Command<CommandSourceStack> action) {
        return context -> {
            if (!hasDevPermission(context.getSource())) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }
            return action.run(context);
        };
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(HammerMod.MOD_ID)
                        //? if <26.4 {
                        .requires(HammerCommands::hasDevPermission)
                        //?}
                        .then(Commands.literal("ability")
                                .then(Commands.literal("lightning")
                                        .executes(devCommand(HammerCommands::toggleLightning)))
                                .then(Commands.literal("teleport")
                                        .executes(devCommand(HammerCommands::toggleTeleport)))
                                .then(Commands.literal("throw")
                                        .executes(devCommand(HammerCommands::toggleThrow)))
                                .then(Commands.literal("mining")
                                        .executes(devCommand(HammerCommands::toggleMining)))
                                .then(Commands.literal("config")
                                        .executes(devCommand(HammerCommands::toggleToolConfig)))
                                .then(Commands.literal("all")
                                        .executes(devCommand(HammerCommands::addAllAbilities)))
                                .then(Commands.literal("clear")
                                        .executes(devCommand(HammerCommands::clearAllAbilities)))));
    }

    private static int toggleLightning(CommandContext<CommandSourceStack> context) {
        return toggleAbilityComponent(context, "lightning",
                HammerRegistry.LIGHTNING_ABILITY.get(), Unit.INSTANCE);
    }

    private static int toggleTeleport(CommandContext<CommandSourceStack> context) {
        return toggleAbilityComponent(context, "teleport",
                HammerRegistry.TELEPORT_SURFACE_ABILITY.get(), Unit.INSTANCE);
    }

    private static int toggleThrow(CommandContext<CommandSourceStack> context) {
        return toggleAbilityComponent(context, "throw",
                HammerRegistry.THROWABLE_ABILITY.get(), ThrowableAbilityData.DEFAULT);
    }

    private static int toggleMining(CommandContext<CommandSourceStack> context) {
        return toggleAbilityComponent(context, "mining",
                HammerRegistry.MULTI_BLOCK_MINING.get(), MiningModeData.DEFAULT);
    }

    private static int toggleToolConfig(CommandContext<CommandSourceStack> context) {
        return toggleAbilityComponent(context, "config",
                HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT);
    }

    private static <T> int toggleAbilityComponent(CommandContext<CommandSourceStack> context,
                                                   String abilityName,
                                                   net.minecraft.core.component.DataComponentType<T> componentType,
                                                   T defaultValue) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.hammer.ability.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("command.hammer.ability.no_item"));
            return 0;
        }

        if (stack.has(componentType)) {
            stack.remove(componentType);
            source.sendSuccess(() -> Component.translatable("command.hammer.ability.removed", abilityName)
                    .withStyle(style -> style.withColor(0xE74C3C)), false);
        } else {
            stack.set(componentType, defaultValue);
            source.sendSuccess(() -> Component.translatable("command.hammer.ability.added", abilityName)
                    .withStyle(style -> style.withColor(0x2ECC71)), false);
        }
        return 1;
    }

    private static int addAllAbilities(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.hammer.ability.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("command.hammer.ability.no_item"));
            return 0;
        }

        // Add all abilities with defaults
        stack.set(HammerRegistry.LIGHTNING_ABILITY.get(), Unit.INSTANCE);
        stack.set(HammerRegistry.TELEPORT_SURFACE_ABILITY.get(), Unit.INSTANCE);
        stack.set(HammerRegistry.THROWABLE_ABILITY.get(), ThrowableAbilityData.DEFAULT);
        stack.set(HammerRegistry.MULTI_BLOCK_MINING.get(), MiningModeData.DEFAULT);
        stack.set(HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT);
        stack.set(HammerRegistry.RIGHT_CLICK_ACTION.get(),
                new RightClickAction(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "throwable_ability")));
        stack.set(HammerRegistry.MULTI_TOOL_ABILITY.get(), MultiToolAbilityData.DEFAULT);

        source.sendSuccess(() -> Component.translatable("command.hammer.ability.all_added")
                .withStyle(style -> style.withColor(0x2ECC71)), false);
        return 1;
    }

    private static int clearAllAbilities(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.hammer.ability.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("command.hammer.ability.no_item"));
            return 0;
        }

        // Remove all abilities
        stack.remove(HammerRegistry.LIGHTNING_ABILITY.get());
        stack.remove(HammerRegistry.TELEPORT_SURFACE_ABILITY.get());
        stack.remove(HammerRegistry.THROWABLE_ABILITY.get());
        stack.remove(HammerRegistry.MULTI_BLOCK_MINING.get());
        stack.remove(HammerRegistry.TOOL_CONFIG.get());
        stack.remove(HammerRegistry.RIGHT_CLICK_ACTION.get());
        stack.remove(HammerRegistry.MULTI_TOOL_ABILITY.get());

        source.sendSuccess(() -> Component.translatable("command.hammer.ability.all_cleared")
                .withStyle(style -> style.withColor(0xF39C12)), false);
        return 1;
    }

}
