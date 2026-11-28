package net.j40climb.florafauna_hammer.common.item.hammer;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.j40climb.florafauna_hammer.common.item.abilities.SeismicScanner;
import net.j40climb.florafauna_hammer.common.item.abilities.RightClickActionHandler;
import net.minecraft.world.item.Item;
//? if <26.3 {
import net.minecraft.world.item.ItemInstance;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
//? if neoforge && <26.3 {
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
//?}

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/**
 * A multi-mode mining tool that can mine blocks with different mining speeds and patterns.
 * This hammer supports multiple mining modes and speed settings configurable via data components.
 */
public class HammerItem extends Item {
    private static final Identifier THROW_ABILITY = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "throwable_ability");
    public static final int MAX_CHARGE = HammerStage.TEMPEST.capacity();
    /**
     * The tool material configuration for the Energy Hammer.
     * Note: Durability and enchantability are unused since the hammer is unbreakable and not enchantable.
     * Attack damage and speed are set via .tool() in RegisterItems.
     * Mining speed is controlled by ToolAbilityEventHandlers via the TOOL_CONFIG component.
     */
    public static final ToolMaterial HAMMER_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // What can't be mined (netherite tier)
            1, // Durability (unused - item is unbreakable)
            1.0F, // Base mining speed (modified by ToolAbilityEventHandlers)
            0.0F, // Attack damage bonus (base damage set in .tool())
            1, // Enchantability (minimum required, but ENCHANTABLE component is removed)
            ItemTags.NETHERITE_TOOL_MATERIALS // Repair ingredient (unused - item is unbreakable)
    );

    /**
     * Constructs a new EnergyHammerItem with the specified properties.
     * Automatically adds mining mode and mining speed data components with default values.
     *
     * @param properties the item properties to configure this hammer
     */
    public HammerItem(Properties properties) {
        super(removeEnchantable(properties)
                // Tool ability components (composable - these enable the abilities)
                .component(HammerRegistry.MULTI_BLOCK_MINING.get(), MiningModeData.DEFAULT)
                .component(HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT)
                .delayedComponent(DataComponents.ENCHANTMENTS, registries -> {
                    var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    enchantments.set(registries.getOrThrow(Enchantments.FORTUNE), 3);
                    return enchantments.toImmutable();
                })
                .component(HammerRegistry.LIGHTNING_ABILITY.get(), Unit.INSTANCE)
                .component(HammerRegistry.TELEPORT_SURFACE_ABILITY.get(), Unit.INSTANCE)
                .component(HammerRegistry.HAMMER_STAGE.get(), HammerStage.FORGED)
                .component(HammerRegistry.THROWABLE_ABILITY.get(), ThrowableAbilityData.DEFAULT)
                .component(HammerRegistry.RIGHT_CLICK_ACTION.get(), new RightClickAction(THROW_ABILITY))
                .component(HammerRegistry.MULTI_TOOL_ABILITY.get(), MultiToolAbilityData.DEFAULT)
                .component(HammerRegistry.HAMMER_ACTION.get(), HammerAction.AUTO)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.onGround()) return super.useOn(context);
        var stack = context.getItemInHand();
        var selected = stack.getOrDefault(HammerRegistry.HAMMER_ACTION.get(), HammerAction.AUTO);
        if (selected == HammerAction.SCAN && selected.availableOn(stack)) {
            if (player instanceof ServerPlayer server) SeismicScanner.activate(server, context.getHand());
            return InteractionResult.SUCCESS;
        }
        var fallback = stack.get(HammerRegistry.RIGHT_CLICK_ACTION.get());
        if (selected == HammerAction.AUTO && fallback != null && fallback.abilityId().equals(THROW_ABILITY)
                && stack.has(HammerRegistry.THROWABLE_ABILITY.get())) {
            RightClickActionHandler.beginThrow(player, context.getHand());
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    /**
     * Removes the ENCHANTABLE component from properties to prevent enchanting via table/anvil.
     * This is called before the parent constructor to strip out the enchantability added by .tool().
     *
     * @param properties the item properties
     * @return properties with ENCHANTABLE component removed
     */
    private static Properties removeEnchantable(Properties properties) {
        return properties.component(DataComponents.ENCHANTABLE, null);
    }

    /**
     * Prevents any enchantments from being applied to the Energy Hammer.
     * This blocks the /enchant command and other programmatic enchantment attempts.
     * The hammer has its own built-in Fortune/Silk Touch toggle instead.
     *
     * @param stack the item stack
     * @param enchantment the enchantment being checked
     * @return false to reject all enchantments
     */
    //? if neoforge {
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    //?} else {
    /*@Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment,
                                      net.fabricmc.fabric.api.item.v1.EnchantingContext context) {
        return false;
    }
    *///?}

    //? if neoforge && <26.3 {
    /**
     * Declares which tool actions this item can perform based on its MULTI_TOOL_ABILITY component.
     * This is required for getToolModifiedState to work properly.
     *
     * @param stack the item stack
     * @param itemAbility the tool action being checked
     * @return true if this item can perform the action
     */
    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        MultiToolAbilityData multiTool = stack.get(HammerRegistry.MULTI_TOOL_ABILITY.get());
        if (multiTool == null) {
            return false;
        }
        if (itemAbility == ItemAbilities.AXE_STRIP && multiTool.strip()) {
            return true;
        }
        if (itemAbility == ItemAbilities.SHOVEL_FLATTEN && multiTool.flatten()) {
            return true;
        }
        if (itemAbility == ItemAbilities.HOE_TILL && multiTool.till()) {
            return true;
        }
        return false;
    }

    //?}
    /**
     * Determines if this hammer is the correct tool for mining the given block state.
     * The hammer can mine blocks that are mineable with pickaxe, shovel, axe, hoe, or are sword efficient.
     *
     * @param itemStack the hammer item stack
     * @param blockState the block state being mined
     * @return true if this hammer can properly mine the block, false otherwise
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack itemStack, BlockState blockState) {
        // TODO what is the downside of just returning true?
        return (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) || blockState.is(BlockTags.MINEABLE_WITH_SHOVEL) || blockState.is(BlockTags.MINEABLE_WITH_AXE) || blockState.is(BlockTags.MINEABLE_WITH_HOE) || blockState.is(BlockTags.SWORD_EFFICIENT));
    }

    /**
     * Appends tooltip text to display the current mining mode when hovering over the item.
     *
     * @param itemStack the hammer item stack
     * @param context the tooltip context
     * @param tooltipDisplay the tooltip display configuration
     * @param tooltipComponents consumer to accept tooltip components
     * @param tooltipFlag flags controlling tooltip behavior
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        HammerStage stage = HammerStage.of(itemStack);
        MiningModeData miningModeData = MiningModeData.effective(itemStack);
        tooltipComponents.accept(miningModeData.modeDescription());
        ToolConfig toolConfig = itemStack.getOrDefault(HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT);
        tooltipComponents.accept(toolConfig.withMiningSpeed(toolConfig.effectiveSpeed(stage)).speedDescription());
        if (stage.capacity() > 0) {
            tooltipComponents.accept(Component.translatable("tooltip.hammer.hammer.charge", charge(itemStack), stage.capacity()));
            tooltipComponents.accept(Component.translatable("tooltip.hammer.hammer.ore_strike", Component.keybind("key.use")));
        }
        super.appendHoverText(itemStack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return HammerStage.of(stack).label();
    }

    public static int charge(ItemStack stack) {
        int capacity = HammerStage.of(stack).capacity();
        return Math.clamp(stack.getOrDefault(HammerRegistry.HAMMER_CHARGE.get(), capacity), 0, capacity);
    }

    //? if neoforge {
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !ItemStack.matchesIgnoringComponents(oldStack, newStack, type -> type == HammerRegistry.HAMMER_CHARGE.get());
    }

    //?} else {
    /*@Override
    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return ItemStack.matchesIgnoringComponents(oldStack, newStack, type -> type == HammerRegistry.HAMMER_CHARGE.get());
    }
    *///?}

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        int capacity = HammerStage.of(stack).capacity();
        if (capacity == 0) {
            if (stack.has(HammerRegistry.HAMMER_CHARGE.get())) stack.remove(HammerRegistry.HAMMER_CHARGE.get());
            return;
        }
        int charge = charge(stack);
        if (owner instanceof Player && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
                && owner.tickCount % 20 == 0 && charge < capacity) {
            charge++;
        }
        if (stack.getOrDefault(HammerRegistry.HAMMER_CHARGE.get(), -1) != charge) stack.set(HammerRegistry.HAMMER_CHARGE.get(), charge);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return HammerStage.of(stack).capacity() > 0; }
    @Override public int getBarWidth(ItemStack stack) {
        int capacity = HammerStage.of(stack).capacity();
        int remaining = charge(stack);
        return remaining == 0 ? 0 : Math.max(1, Math.round(13.0f * remaining / capacity));
    }
    @Override public int getBarColor(ItemStack stack) { return 0xAC72ED; }

    /**
     * Determines whether this item should display an enchantment glint effect.
     *
     * @param itemStack the hammer item stack
     * @return false to disable the enchantment glint effect
     */
    @Override
    public boolean isFoil(ItemStack itemStack) {
        return false;
    }
}
