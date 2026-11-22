package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;


/** Shared by the server GameTest and the two-session Survival world rehearsal. */
public final class HammerSurvivalFixture {
    private static final Component NAME = Component.literal("East Gallery Hammer");
    private static final MiningModeData MODE = MiningModeData.DEFAULT.withShape(MiningShape.BORE)
            .toggleFavorite(MiningShape.BORE).toggleFavorite(MiningShape.FLAT_3X3);
    private static final ToolConfig SETTINGS = new ToolConfig(false, true, MiningSpeed.STANDARD, true, true);

    private HammerSurvivalFixture() {}

    public static void forge(ServerPlayer player) {
        check(!player.getAbilities().instabuild, "Recipe fixture requires Survival");
        var hammer = craft(player, HammerRegistry.HAMMER.get(),
                Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.STICK, Items.IRON_INGOT,
                null, Items.STICK, null);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        hammer.set(DataComponents.CUSTOM_NAME, NAME);
        hammer.set(HammerRegistry.MULTI_BLOCK_MINING.get(), MODE);
        hammer.set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.TILL);
        UpdateToolConfigPayload.applyToHeldItem(player, SETTINGS);
        checkTool(player, HammerStage.FORGED);
        check(HammerItem.charge(hammer) == 0, "Crafted Forged Hammer has active charge");
    }

    public static void upgrade(ServerPlayer player, HammerStage destination) {
        var previous = player.getMainHandItem();
        ItemStack core;
        if (destination == HammerStage.RESONANT) {
            core = craft(player, HammerRegistry.RESONANCE_CORE.get(),
                    null, Items.AMETHYST_SHARD, null, Items.REDSTONE, Items.DIAMOND, Items.REDSTONE, null, Items.QUARTZ, null);
        } else {
            check(destination == HammerStage.TEMPEST, "Unexpected upgrade");
            core = craft(player, HammerRegistry.TEMPEST_CORE.get(),
                    null, Items.BLAZE_ROD, null, Items.ENDER_EYE, Items.NETHERITE_INGOT, Items.ENDER_EYE, null, Items.BLAZE_ROD, null);
        }

        var table = new SmithingMenu(2, player.getInventory(), access(player));
        int before = HammerItem.charge(previous);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        place(player, table, SmithingMenu.BASE_SLOT, previous);
        place(player, table, SmithingMenu.ADDITIONAL_SLOT, core);
        table.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
        ItemStack result = takeCursor(table);
        check(result.getCount() == 1 && result.is(HammerRegistry.HAMMER.get()), "Smithing did not yield one Hammer");
        for (int i = 0; i <= SmithingMenu.RESULT_SLOT; i++) {
            check(!table.getSlot(i).hasItem(), "Smithing left an input or duplicate result in slot " + i);
        }
        check(HammerItem.charge(result) == (destination == HammerStage.RESONANT ? 64 : before),
                "Unexpected charge after installing " + destination);
        player.setItemInHand(InteractionHand.MAIN_HAND, result);
        checkTool(player, destination);
    }

    public static void checkTool(ServerPlayer player, HammerStage expected) {
        var hammer = player.getMainHandItem();
        check(hammer.is(HammerRegistry.HAMMER.get()) && hammer.getCount() == 1 && HammerStage.of(hammer) == expected,
                "Missing single " + expected + " Hammer");
        check(NAME.equals(hammer.get(DataComponents.CUSTOM_NAME)) && MODE.equals(hammer.get(HammerRegistry.MULTI_BLOCK_MINING.get()))
                && SETTINGS.equals(hammer.get(HammerRegistry.TOOL_CONFIG.get())) && HammerAction.TILL == hammer.get(HammerRegistry.HAMMER_ACTION.get())
                && hammer.getEnchantments().getLevel(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH)) == 1,
                "Lost the Hammer name, mining shape, favorites, settings, selected ability or enchantment at " + expected);
        int total = player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(HammerRegistry.HAMMER.get())).mapToInt(ItemStack::getCount).sum();
        check(total == 1, "Expected one owned Hammer after " + expected + ", found " + total);
    }

    static void reject(ServerPlayer player, Item core) {
        var table = new SmithingMenu(3, player.getInventory(), access(player));
        var hammer = player.getMainHandItem();
        var expected = hammer.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        place(player, table, SmithingMenu.BASE_SLOT, hammer);
        place(player, table, SmithingMenu.ADDITIONAL_SLOT, new ItemStack(core, 2));
        table.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
        check(table.getCarried().isEmpty() && !table.getSlot(SmithingMenu.RESULT_SLOT).hasItem(),
                "Invalid " + core + " upgrade produced a result");
        table.clicked(SmithingMenu.BASE_SLOT, 0, ContainerInput.PICKUP, player);
        var recovered = takeCursor(table);
        check(ItemStack.matches(expected, recovered), "Rejected smithing changed the input Hammer");
        table.clicked(SmithingMenu.ADDITIONAL_SLOT, 0, ContainerInput.PICKUP, player);
        var remainder = takeCursor(table);
        check(remainder.is(core) && remainder.getCount() == 2, "Rejected smithing spent a core");
        player.setItemInHand(InteractionHand.MAIN_HAND, recovered);
    }

    private static ItemStack craft(ServerPlayer player, Item expected, Item... grid) {
        var menu = new CraftingMenu(1, player.getInventory(), access(player));
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null) place(player, menu, i + 1, new ItemStack(grid[i]));
        }
        menu.clicked(CraftingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
        var crafted = takeCursor(menu);
        check(crafted.is(expected) && crafted.getCount() == 1, "Table did not craft exactly one " + expected);
        check(menu.getInputGridSlots().stream().noneMatch(slot -> slot.hasItem()) && !menu.getResultSlot().hasItem(),
                "Crafting left a duplicate result or unconsumed ingredient for " + expected);
        return crafted;
    }

    private static ContainerLevelAccess access(ServerPlayer player) {
        return ContainerLevelAccess.create(player.level(), player.blockPosition());
    }

    private static void place(ServerPlayer player, AbstractContainerMenu menu, int slot, ItemStack source) {
        check(menu.getCarried().isEmpty() && !menu.getSlot(slot).hasItem(), "Occupied destination");
        menu.setCarried(source);
        menu.clicked(slot, 0, ContainerInput.PICKUP, player);
        check(menu.getCarried().isEmpty() && menu.getSlot(slot).hasItem(), "Vanilla menu rejected input slot " + slot);
    }

    private static ItemStack takeCursor(AbstractContainerMenu menu) {
        var result = menu.getCarried();
        menu.setCarried(ItemStack.EMPTY);
        return result;
    }

    private static void check(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }
}
