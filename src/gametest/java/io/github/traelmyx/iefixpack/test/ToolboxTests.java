package io.github.traelmyx.iefixpack.test;

import blusunrize.immersiveengineering.common.gui.ToolboxMenu;
import blusunrize.immersiveengineering.common.register.IEItems;
import blusunrize.immersiveengineering.common.register.IEMenuTypes;
import io.github.traelmyx.iefixpack.IEFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Immersive Engineering #6177, as in the report: a toolbox held in the hand has 32 oak logs in one slot. The player
 * shift-clicks 1 more oak log into it. Afterwards the toolbox must hold 33.
 */
@GameTestHolder(WorkbenchTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class ToolboxTests {
	private static int logsIn(IItemHandler handler) {
		int n = 0;
		for (int i = 0; i < handler.getSlots(); i++) if (handler.getStackInSlot(i).is(Items.OAK_LOG)) n += handler.getStackInSlot(i).getCount();
		return n;
	}

	private static void run(GameTestHelper helper, boolean fixed, Check check) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		ServerPlayer player = WorkbenchTests.player(helper, fixed ? "test-toolbox" : "test-toolbox-v");
		ItemStack toolbox = new ItemStack(IEItems.Tools.TOOLBOX.get());
		player.getInventory().setItem(0, toolbox);
		player.getInventory().selected = 0;
		IItemHandler stored = player.getMainHandItem().getCapability(Capabilities.ItemHandler.ITEM);
		helper.assertTrue(stored != null, "the toolbox has no inventory");
		// slot 16 and up take anything
		stored.insertItem(16, new ItemStack(Items.OAK_LOG, 32), false);
		player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG, 1));
		ToolboxMenu menu = IEMenuTypes.TOOLBOX.create(1, player.getInventory(), EquipmentSlot.MAINHAND, player.getMainHandItem());
		int hotbar1 = -1;
		for (int i = 0; i < menu.slots.size(); i++)
			if (menu.slots.get(i).container == player.getInventory() && menu.slots.get(i).getContainerSlot() == 1) hotbar1 = i;
		helper.assertTrue(hotbar1 >= 0, "the toolbox menu has no slot for the player's hotbar");
		player.containerMenu = menu;
		IEFixPack.TOOLBOX_NO_SHIFT_CLICK_LOSS.enabled = fixed;
		menu.quickMoveStack(player, hotbar1);
		menu.broadcastChanges();
		IEFixPack.TOOLBOX_NO_SHIFT_CLICK_LOSS.enabled = true;
		helper.runAfterDelay(2, () -> {
			int inBox = logsIn(player.getMainHandItem().getCapability(Capabilities.ItemHandler.ITEM));
			int inHand = player.getInventory().getItem(1).is(Items.OAK_LOG) ? player.getInventory().getItem(1).getCount() : 0;
			player.containerMenu = player.inventoryMenu;
			player.getInventory().clearContent();
			helper.getLevel().getServer().getPlayerList().remove(player);
			System.out.printf("[ie_fix_pack] toolbox, fix %s: %d logs in the toolbox, %d left in the inventory (33 exist)%n", fixed ? "on" : "off", inBox, inHand);
			check.check(inBox + inHand);
			helper.succeed();
		});
	}

	private interface Check {
		void check(int total);
	}

	@GameTest(template = "empty", timeoutTicks = 40, batch = "toolbox_fixed")
	public static void heldToolboxKeepsShiftClickedItems(GameTestHelper helper) {
		run(helper, true, total -> helper.assertTrue(total == 33, total + " logs after the shift-click, 33 existed"));
	}

	@GameTest(template = "empty", timeoutTicks = 40, batch = "toolbox_control")
	public static void toolboxControlReproducesBug(GameTestHelper helper) {
		// without the fix logs must be lost (or duplicated), or this test can't tell the fix works
		run(helper, false, total -> helper.assertTrue(total != 33, "no logs were lost without the fix: the bug did not reproduce"));
	}
}
